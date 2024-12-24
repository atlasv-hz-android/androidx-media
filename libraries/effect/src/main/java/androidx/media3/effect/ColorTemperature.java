package androidx.media3.effect;

import static androidx.media3.common.util.Assertions.checkArgument;

import android.content.Context;
import androidx.annotation.FloatRange;
import androidx.media3.common.VideoFrameProcessingException;
import androidx.media3.common.util.UnstableApi;

/**
 * yingchang@atlasv.com
 * 2024/12/24
 */
@UnstableApi
public final class ColorTemperature implements GlEffect {
  private final float temperature;

  /**
   * Creates a new instance for the given color temperature value.
   *
   * <p>Temperature values range from -1 (cold/blue) to 1 (warm/red). 0 means
   * to add no temperature adjustment and leaves the frames unchanged.
   */
  public ColorTemperature(@FloatRange(from = -1, to = 1) float temperature) {
    checkArgument(
        -1 <= temperature && temperature <= 1,
        "Temperature needs to be in the interval [-1, 1].");
    this.temperature = temperature;
  }

  @Override
  public GlShaderProgram toGlShaderProgram(Context context, boolean useHdr)
      throws VideoFrameProcessingException {
    return new ColorTemperatureShaderProgram(context, temperature, useHdr);
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return temperature == 0f;
  }
} 