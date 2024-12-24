package androidx.media3.effect;

import static androidx.media3.common.util.Assertions.checkArgument;

import android.content.Context;
import androidx.annotation.FloatRange;
import androidx.media3.common.VideoFrameProcessingException;

/**
 * yingchang@atlasv.com
 * 2024/12/23
 */
public class Sharpen implements GlEffect {
  private final float sharpen;

  /**
   * Creates a new instance for the given sharpenX and sharpenY value.
   *
   * <p>
   * The sharpen values range from -1 to 1. 0 means to add no sharpen and leaves
   * the frames
   * unchanged.
   */
  public Sharpen(@FloatRange(from = -4, to = 4) float sharpen) {
    checkArgument(-4 <= sharpen && sharpen <= 4, "sharpen needs to be in the interval [-1, 1].");
    this.sharpen = sharpen;
  }

  @Override
  public GlShaderProgram toGlShaderProgram(Context context, boolean useHdr)
      throws VideoFrameProcessingException {
    return new SharpenShaderProgram(context, sharpen, useHdr);
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return sharpen == 0;
  }
}
