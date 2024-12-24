package androidx.media3.effect;

import android.content.Context;
import android.opengl.GLES20;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.GlProgram;
import androidx.media3.common.util.GlUtil;
import androidx.media3.common.util.Size;
import java.io.IOException;

public class SharpenShaderProgram extends BaseGlShaderProgram {

  private static final String VERTEX_SHADER_PATH = "shaders/vertex_shader_sharpen_es2.glsl";
  private static final String FRAGMENT_SHADER_PATH = "shaders/fragment_shader_sharpen_es2.glsl";

  private final GlProgram glProgram;
  private final float sharpen;

  public SharpenShaderProgram(Context context, float sharpen, boolean useHdr)
      throws VideoFrameProcessingException {
    super(useHdr, 1);

    this.sharpen = sharpen;

    try {
      glProgram = new GlProgram(context, VERTEX_SHADER_PATH, FRAGMENT_SHADER_PATH);
    } catch (IOException | GlUtil.GlException e) {
      throw new VideoFrameProcessingException(e);
    }

    glProgram.setBufferAttribute(
        "aFramePosition",
        GlUtil.getNormalizedCoordinateBounds(),
        GlUtil.HOMOGENEOUS_COORDINATE_VECTOR_SIZE);
    glProgram.setFloatUniform("uSharpen", sharpen);
  }

  @Override
  public Size configure(int inputWidth, int inputHeight) throws VideoFrameProcessingException {
    return new Size(inputWidth, inputHeight);
  }

  @Override
  public void drawFrame(int inputTexId, long presentationTimeUs)
      throws VideoFrameProcessingException {
    try {
      glProgram.use();
      glProgram.setSamplerTexIdUniform("uTexSampler", inputTexId, 0);
      glProgram.setFloatUniform("uSharpen", sharpen);
      glProgram.setFloatUniform("uImageWidth", 1f / inputWidth);
      glProgram.setFloatUniform("uImageHeight", 1f / inputHeight);
      glProgram.bindAttributesAndUniforms();
      GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4);
    } catch (GlUtil.GlException e) {
      throw new VideoFrameProcessingException(e);
    }
  }

  @Override
  public void release() throws VideoFrameProcessingException {
    super.release();
    try {
      glProgram.delete();
    } catch (GlUtil.GlException e) {
      throw new VideoFrameProcessingException(e);
    }
  }
}