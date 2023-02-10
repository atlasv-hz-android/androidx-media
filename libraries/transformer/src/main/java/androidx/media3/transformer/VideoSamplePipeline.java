/*
 * Copyright 2021 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package androidx.media3.transformer;

import static androidx.media3.common.util.Assertions.checkNotNull;
import static androidx.media3.common.util.Assertions.checkState;
import static androidx.media3.common.util.Util.SDK_INT;
import static androidx.media3.transformer.TransformationRequest.HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR;
import static androidx.media3.transformer.TransformationRequest.HDR_MODE_KEEP_HDR;
import static androidx.media3.transformer.TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC;
import static androidx.media3.transformer.TransformationRequest.HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL;

import android.content.Context;
import android.graphics.Bitmap;
import android.media.MediaCodec;
import android.view.Surface;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;
import androidx.media3.common.C;
import androidx.media3.common.ColorInfo;
import androidx.media3.common.DebugViewProvider;
import androidx.media3.common.Effect;
import androidx.media3.common.Format;
import androidx.media3.common.FrameInfo;
import androidx.media3.common.FrameProcessingException;
import androidx.media3.common.FrameProcessor;
import androidx.media3.common.MimeTypes;
import androidx.media3.common.SurfaceInfo;
import androidx.media3.common.util.Consumer;
import androidx.media3.common.util.Log;
import androidx.media3.common.util.Util;
import androidx.media3.decoder.DecoderInputBuffer;
import com.google.common.collect.ImmutableList;
import com.google.common.util.concurrent.MoreExecutors;
import java.nio.ByteBuffer;
import java.util.List;
import org.checkerframework.checker.nullness.qual.MonotonicNonNull;
import org.checkerframework.dataflow.qual.Pure;

/** Pipeline to process, re-encode and mux raw video frames. */
/* package */ final class VideoSamplePipeline extends SamplePipeline {

  private final FrameProcessor frameProcessor;
  private final ColorInfo frameProcessorInputColor;
  private final FrameInfo firstFrameInfo;

  private final EncoderWrapper encoderWrapper;
  private final DecoderInputBuffer encoderOutputBuffer;

  /**
   * The timestamp of the last buffer processed before {@linkplain
   * FrameProcessor.Listener#onFrameProcessingEnded() frame processing has ended}.
   */
  private volatile long finalFramePresentationTimeUs;

  public VideoSamplePipeline(
      Context context,
      Format firstInputFormat,
      long streamStartPositionUs,
      long streamOffsetUs,
      TransformationRequest transformationRequest,
      ImmutableList<Effect> effects,
      FrameProcessor.Factory frameProcessorFactory,
      Codec.EncoderFactory encoderFactory,
      MuxerWrapper muxerWrapper,
      Consumer<TransformationException> errorConsumer,
      FallbackListener fallbackListener,
      DebugViewProvider debugViewProvider)
      throws TransformationException {
    super(firstInputFormat, streamStartPositionUs, muxerWrapper);

    boolean isGlToneMapping = false;
    if (ColorInfo.isTransferHdr(firstInputFormat.colorInfo)) {
      if (transformationRequest.hdrMode == HDR_MODE_EXPERIMENTAL_FORCE_INTERPRET_HDR_AS_SDR) {
        if (SDK_INT < 29) {
          throw TransformationException.createForCodec(
              new IllegalArgumentException("Interpreting HDR video as SDR is not supported."),
              TransformationException.ERROR_CODE_HDR_DECODING_UNSUPPORTED,
              /* isVideo= */ true,
              /* isDecoder= */ true,
              firstInputFormat);
        }
        firstInputFormat =
            firstInputFormat.buildUpon().setColorInfo(ColorInfo.SDR_BT709_LIMITED).build();
      } else if (transformationRequest.hdrMode == HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_OPEN_GL) {
        isGlToneMapping = true;
      }
    }

    finalFramePresentationTimeUs = C.TIME_UNSET;

    encoderOutputBuffer =
        new DecoderInputBuffer(DecoderInputBuffer.BUFFER_REPLACEMENT_MODE_DISABLED);

    encoderWrapper =
        new EncoderWrapper(
            encoderFactory,
            firstInputFormat,
            muxerWrapper.getSupportedSampleMimeTypes(C.TRACK_TYPE_VIDEO),
            transformationRequest,
            fallbackListener);

    ColorInfo encoderInputColor = encoderWrapper.getSupportedInputColor();
    // If not tone mapping using OpenGL, the decoder will output the encoderInputColor,
    // possibly by tone mapping.
    frameProcessorInputColor =
        isGlToneMapping ? checkNotNull(firstInputFormat.colorInfo) : encoderInputColor;
    // For consistency with the Android platform, OpenGL tone mapping outputs colors with
    // C.COLOR_TRANSFER_GAMMA_2_2 instead of C.COLOR_TRANSFER_SDR, and outputs this as
    // C.COLOR_TRANSFER_SDR to the encoder.
    ColorInfo frameProcessorOutputColor =
        isGlToneMapping
            ? new ColorInfo(
                C.COLOR_SPACE_BT709,
                C.COLOR_RANGE_LIMITED,
                C.COLOR_TRANSFER_GAMMA_2_2,
                /* hdrStaticInfo= */ null)
            : encoderInputColor;
    try {
      frameProcessor =
          frameProcessorFactory.create(
              context,
              effects,
              debugViewProvider,
              frameProcessorInputColor,
              frameProcessorOutputColor,
              MimeTypes.getTrackType(firstInputFormat.sampleMimeType),
              /* releaseFramesAutomatically= */ true,
              MoreExecutors.directExecutor(),
              new FrameProcessor.Listener() {
                private long lastProcessedFramePresentationTimeUs;

                @Override
                public void onOutputSizeChanged(int width, int height) {
                  try {
                    checkNotNull(frameProcessor)
                        .setOutputSurfaceInfo(encoderWrapper.getSurfaceInfo(width, height));
                  } catch (TransformationException exception) {
                    errorConsumer.accept(exception);
                  }
                }

                @Override
                public void onOutputFrameAvailable(long presentationTimeUs) {
                  // Frames are released automatically.
                  lastProcessedFramePresentationTimeUs = presentationTimeUs;
                }

                @Override
                public void onFrameProcessingError(FrameProcessingException exception) {
                  errorConsumer.accept(
                      TransformationException.createForFrameProcessingException(
                          exception, TransformationException.ERROR_CODE_FRAME_PROCESSING_FAILED));
                }

                @Override
                public void onFrameProcessingEnded() {
                  VideoSamplePipeline.this.finalFramePresentationTimeUs =
                      lastProcessedFramePresentationTimeUs;
                  try {
                    encoderWrapper.signalEndOfInputStream();
                  } catch (TransformationException exception) {
                    errorConsumer.accept(exception);
                  }
                }
              });
    } catch (FrameProcessingException e) {
      throw TransformationException.createForFrameProcessingException(
          e, TransformationException.ERROR_CODE_FRAME_PROCESSING_FAILED);
    }
    // The decoder rotates encoded frames for display by firstInputFormat.rotationDegrees.
    int decodedWidth =
        (firstInputFormat.rotationDegrees % 180 == 0)
            ? firstInputFormat.width
            : firstInputFormat.height;
    int decodedHeight =
        (firstInputFormat.rotationDegrees % 180 == 0)
            ? firstInputFormat.height
            : firstInputFormat.width;
    firstFrameInfo =
        new FrameInfo.Builder(decodedWidth, decodedHeight)
            .setPixelWidthHeightRatio(firstInputFormat.pixelWidthHeightRatio)
            .setStreamOffsetUs(streamOffsetUs)
            .build();
  }

  @Override
  public void onMediaItemChanged(
      EditedMediaItem editedMediaItem, Format trackFormat, long mediaItemOffsetUs) {
    frameProcessor.setInputFrameInfo(
        new FrameInfo.Builder(firstFrameInfo).setOffsetToAddUs(mediaItemOffsetUs).build());
  }

  @Override
  public void queueInputBitmap(Bitmap inputBitmap, long durationUs, int frameRate) {
    frameProcessor.queueInputBitmap(inputBitmap, durationUs, frameRate);
  }

  @Override
  public Surface getInputSurface() {
    return frameProcessor.getInputSurface();
  }

  @Override
  public ColorInfo getExpectedInputColorInfo() {
    return frameProcessorInputColor;
  }

  @Override
  public void registerVideoFrame() {
    frameProcessor.registerInputFrame();
  }

  @Override
  public int getPendingVideoFrameCount() {
    return frameProcessor.getPendingInputFrameCount();
  }

  @Override
  public void signalEndOfVideoInput() {
    frameProcessor.signalEndOfInput();
  }

  @Override
  public void release() {
    frameProcessor.release();
    encoderWrapper.release();
  }

  @Override
  @Nullable
  protected Format getMuxerInputFormat() throws TransformationException {
    return encoderWrapper.getOutputFormat();
  }

  @Override
  @Nullable
  protected DecoderInputBuffer getMuxerInputBuffer() throws TransformationException {
    encoderOutputBuffer.data = encoderWrapper.getOutputBuffer();
    if (encoderOutputBuffer.data == null) {
      return null;
    }
    MediaCodec.BufferInfo bufferInfo = checkNotNull(encoderWrapper.getOutputBufferInfo());
    if (finalFramePresentationTimeUs != C.TIME_UNSET
        && bufferInfo.size > 0
        && bufferInfo.presentationTimeUs == 0) {
      // Internal ref b/235045165: Some encoder incorrectly set a zero presentation time on the
      // penultimate buffer (before EOS), and sets the actual timestamp on the EOS buffer. Use the
      // last processed frame presentation time instead.
      // bufferInfo.presentationTimeUs should never be 0 because we apply streamOffsetUs to the
      // buffer presentationTimeUs.
      bufferInfo.presentationTimeUs = finalFramePresentationTimeUs;
    }
    encoderOutputBuffer.timeUs = bufferInfo.presentationTimeUs;
    encoderOutputBuffer.setFlags(bufferInfo.flags);
    return encoderOutputBuffer;
  }

  @Override
  protected void releaseMuxerInputBuffer() throws TransformationException {
    encoderWrapper.releaseOutputBuffer(/* render= */ false);
  }

  @Override
  protected boolean isMuxerInputEnded() {
    return encoderWrapper.isEnded();
  }

  /**
   * Creates a {@link TransformationRequest}, based on an original {@code TransformationRequest} and
   * parameters specifying alterations to it that indicate device support.
   *
   * @param transformationRequest The requested transformation.
   * @param hasOutputFormatRotation Whether the input video will be rotated to landscape during
   *     processing, with {@link Format#rotationDegrees} of 90 added to the output format.
   * @param requestedFormat The requested format.
   * @param supportedFormat A format supported by the device.
   * @param supportedHdrMode A {@link TransformationRequest.HdrMode} supported by the device.
   * @return The created instance.
   */
  @Pure
  private static TransformationRequest createSupportedTransformationRequest(
      TransformationRequest transformationRequest,
      boolean hasOutputFormatRotation,
      Format requestedFormat,
      Format supportedFormat,
      @TransformationRequest.HdrMode int supportedHdrMode) {
    // TODO(b/259570024): Consider including bitrate in the revised fallback design.

    TransformationRequest.Builder supportedRequestBuilder = transformationRequest.buildUpon();
    if (transformationRequest.hdrMode != supportedHdrMode) {
      supportedRequestBuilder.setHdrMode(supportedHdrMode);
    }

    if (!Util.areEqual(requestedFormat.sampleMimeType, supportedFormat.sampleMimeType)) {
      supportedRequestBuilder.setVideoMimeType(supportedFormat.sampleMimeType);
    }

    if (hasOutputFormatRotation) {
      if (requestedFormat.width != supportedFormat.width) {
        supportedRequestBuilder.setResolution(/* outputHeight= */ supportedFormat.width);
      }
    } else if (requestedFormat.height != supportedFormat.height) {
      supportedRequestBuilder.setResolution(supportedFormat.height);
    }

    return supportedRequestBuilder.build();
  }

  /**
   * Wraps an {@linkplain Codec encoder} and provides its input {@link Surface}.
   *
   * <p>The encoder is created once the {@link Surface} is {@linkplain #getSurfaceInfo(int, int)
   * requested}. If it is {@linkplain #getSurfaceInfo(int, int) requested} again with different
   * dimensions, the same encoder is used and the provided dimensions stay fixed.
   */
  @VisibleForTesting
  /* package */ static final class EncoderWrapper {
    private static final String TAG = "EncoderWrapper";

    private final Codec.EncoderFactory encoderFactory;
    private final Format inputFormat;
    private final List<String> muxerSupportedMimeTypes;
    private final TransformationRequest transformationRequest;
    private final FallbackListener fallbackListener;
    private final String requestedOutputMimeType;
    private final ImmutableList<String> supportedEncoderNamesForHdrEditing;

    private @MonotonicNonNull SurfaceInfo encoderSurfaceInfo;

    private volatile @MonotonicNonNull Codec encoder;
    private volatile int outputRotationDegrees;
    private volatile boolean releaseEncoder;

    public EncoderWrapper(
        Codec.EncoderFactory encoderFactory,
        Format inputFormat,
        List<String> muxerSupportedMimeTypes,
        TransformationRequest transformationRequest,
        FallbackListener fallbackListener) {
      this.encoderFactory = encoderFactory;
      this.inputFormat = inputFormat;
      this.muxerSupportedMimeTypes = muxerSupportedMimeTypes;
      this.transformationRequest = transformationRequest;
      this.fallbackListener = fallbackListener;

      String inputSampleMimeType = checkNotNull(inputFormat.sampleMimeType);

      if (transformationRequest.videoMimeType != null) {
        requestedOutputMimeType = transformationRequest.videoMimeType;
      } else if (MimeTypes.isImage(inputSampleMimeType)) {
        requestedOutputMimeType = MimeTypes.VIDEO_H265;
      } else {
        requestedOutputMimeType = inputSampleMimeType;
      }
      supportedEncoderNamesForHdrEditing =
          EncoderUtil.getSupportedEncoderNamesForHdrEditing(
              requestedOutputMimeType, inputFormat.colorInfo);
    }

    /** Returns the {@link ColorInfo} expected from the input surface. */
    public ColorInfo getSupportedInputColor() {
      boolean isHdrEditingEnabled =
          transformationRequest.hdrMode == HDR_MODE_KEEP_HDR
              && !supportedEncoderNamesForHdrEditing.isEmpty();
      boolean isInputToneMapped =
          !isHdrEditingEnabled && ColorInfo.isTransferHdr(inputFormat.colorInfo);
      if (isInputToneMapped) {
        // When tone-mapping HDR to SDR is enabled, assume we get BT.709 to avoid having the encoder
        // populate default color info, which depends on the resolution.
        // TODO(b/237674316): Get the color info from the decoder output media format instead.
        return ColorInfo.SDR_BT709_LIMITED;
      }
      if (inputFormat.colorInfo == null) {
        Log.d(TAG, "colorInfo is null. Defaulting to SDR_BT709_LIMITED.");
        return ColorInfo.SDR_BT709_LIMITED;
      }
      return inputFormat.colorInfo;
    }

    @Nullable
    public SurfaceInfo getSurfaceInfo(int requestedWidth, int requestedHeight)
        throws TransformationException {
      if (releaseEncoder) {
        return null;
      }
      if (encoderSurfaceInfo != null) {
        return encoderSurfaceInfo;
      }

      // Encoders commonly support higher maximum widths than maximum heights. This may rotate the
      // frame before encoding, so the encoded frame's width >= height, and sets
      // rotationDegrees in the output Format to ensure the frame is displayed in the correct
      // orientation.
      if (requestedWidth < requestedHeight) {
        int temp = requestedWidth;
        requestedWidth = requestedHeight;
        requestedHeight = temp;
        outputRotationDegrees = 90;
      }

      Format requestedEncoderFormat =
          new Format.Builder()
              .setWidth(requestedWidth)
              .setHeight(requestedHeight)
              .setRotationDegrees(0)
              .setFrameRate(inputFormat.frameRate)
              .setSampleMimeType(requestedOutputMimeType)
              .setColorInfo(getSupportedInputColor())
              .build();

      @Nullable
      String supportedMimeType =
          findSupportedMimeTypeForEncoderAndMuxer(
              requestedOutputMimeType, muxerSupportedMimeTypes, requestedEncoderFormat.colorInfo);
      if (supportedMimeType == null) {
        throw createNoSupportedMimeTypeException(requestedEncoderFormat);
      }

      encoder =
          encoderFactory.createForVideoEncoding(
              requestedEncoderFormat.buildUpon().setSampleMimeType(supportedMimeType).build());

      Format encoderSupportedFormat = encoder.getConfigurationFormat();
      checkState(supportedMimeType.equals(encoderSupportedFormat.sampleMimeType));

      boolean isInputToneMapped =
          ColorInfo.isTransferHdr(inputFormat.colorInfo)
              && !ColorInfo.isTransferHdr(requestedEncoderFormat.colorInfo);
      // HdrMode fallback is only supported from HDR_MODE_KEEP_HDR to
      // HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC.
      @TransformationRequest.HdrMode
      int supportedFallbackHdrMode =
          isInputToneMapped && transformationRequest.hdrMode == HDR_MODE_KEEP_HDR
              ? HDR_MODE_TONE_MAP_HDR_TO_SDR_USING_MEDIACODEC
              : transformationRequest.hdrMode;

      fallbackListener.onTransformationRequestFinalized(
          createSupportedTransformationRequest(
              transformationRequest,
              /* hasOutputFormatRotation= */ outputRotationDegrees != 0,
              requestedEncoderFormat,
              encoderSupportedFormat,
              supportedFallbackHdrMode));

      encoderSurfaceInfo =
          new SurfaceInfo(
              encoder.getInputSurface(),
              encoderSupportedFormat.width,
              encoderSupportedFormat.height,
              outputRotationDegrees);

      if (releaseEncoder) {
        encoder.release();
      }
      return encoderSurfaceInfo;
    }

    public void signalEndOfInputStream() throws TransformationException {
      if (encoder != null) {
        encoder.signalEndOfInputStream();
      }
    }

    @Nullable
    public Format getOutputFormat() throws TransformationException {
      if (encoder == null) {
        return null;
      }
      @Nullable Format outputFormat = encoder.getOutputFormat();
      if (outputFormat != null && outputRotationDegrees != 0) {
        outputFormat = outputFormat.buildUpon().setRotationDegrees(outputRotationDegrees).build();
      }
      return outputFormat;
    }

    @Nullable
    public ByteBuffer getOutputBuffer() throws TransformationException {
      return encoder != null ? encoder.getOutputBuffer() : null;
    }

    @Nullable
    public MediaCodec.BufferInfo getOutputBufferInfo() throws TransformationException {
      return encoder != null ? encoder.getOutputBufferInfo() : null;
    }

    public void releaseOutputBuffer(boolean render) throws TransformationException {
      if (encoder != null) {
        encoder.releaseOutputBuffer(render);
      }
    }

    public boolean isEnded() {
      return encoder != null && encoder.isEnded();
    }

    public void release() {
      if (encoder != null) {
        encoder.release();
      }
      releaseEncoder = true;
    }

    /**
     * Finds a {@linkplain MimeTypes MIME type} that is supported by the encoder and the muxer.
     *
     * <p>HDR editing support is checked if the {@link ColorInfo} is HDR.
     *
     * @param preferredMimeType The preferred {@linkplain MimeTypes MIME type}, returned if
     *     supported.
     * @param muxerSupportedMimeTypes The list of sample {@linkplain MimeTypes MIME types} that the
     *     muxer supports.
     * @param colorInfo The optional encoding {@link ColorInfo}. If a HDR color info is provided,
     *     only encoders that support it will be considered.
     * @return A {@linkplain MimeTypes MIME type} that is supported by an encoder and the muxer, or
     *     {@code null} if no such {@linkplain MimeTypes MIME type} exists.
     */
    @Nullable
    private static String findSupportedMimeTypeForEncoderAndMuxer(
        String preferredMimeType,
        List<String> muxerSupportedMimeTypes,
        @Nullable ColorInfo colorInfo) {
      ImmutableList<String> mimeTypesToCheck =
          new ImmutableList.Builder<String>()
              .add(preferredMimeType)
              .add(MimeTypes.VIDEO_H265)
              .add(MimeTypes.VIDEO_H264)
              .addAll(muxerSupportedMimeTypes)
              .build();

      for (int i = 0; i < mimeTypesToCheck.size(); i++) {
        String mimeType = mimeTypesToCheck.get(i);
        if (mimeTypeAndColorAreSupported(mimeType, muxerSupportedMimeTypes, colorInfo)) {
          return mimeType;
        }
      }
      return null;
    }

    private static boolean mimeTypeAndColorAreSupported(
        String mimeType, List<String> muxerSupportedMimeTypes, @Nullable ColorInfo colorInfo) {
      if (!muxerSupportedMimeTypes.contains(mimeType)) {
        return false;
      }
      if (ColorInfo.isTransferHdr(colorInfo)) {
        return !EncoderUtil.getSupportedEncoderNamesForHdrEditing(mimeType, colorInfo).isEmpty();
      }
      return !EncoderUtil.getSupportedEncoders(mimeType).isEmpty();
    }
  }
}