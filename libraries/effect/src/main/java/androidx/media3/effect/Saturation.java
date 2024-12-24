package androidx.media3.effect;

import static androidx.media3.common.util.Assertions.checkArgument;

import androidx.annotation.FloatRange;
import androidx.media3.common.util.UnstableApi;

/**
 * yingchang@atlasv.com
 * 2024/12/23
 */
@UnstableApi
public class Saturation implements RgbMatrix {
  private final float saturation;
  private final float[] saturationMatrix;

  /**
   * Creates a new instance for the given saturation value.
   *
   * @param saturation The saturation value. 0.0 is grayscale, 1.0 is identity.
   */
  public Saturation(@FloatRange(from = 0.0f) float saturation) {
    checkArgument(saturation >= 0.0f, "Saturation value must be non-negative.");
    this.saturation = saturation;

    // Luminance weights based on human perception
    float rWeight = 0.3086f;
    float gWeight = 0.6094f;
    float bWeight = 0.0820f;
    saturationMatrix = new float[] {
        (1 - saturation) * rWeight + saturation, (1 - saturation) * rWeight    , (1 - saturation) * rWeight    , 0,
        (1 - saturation) * gWeight    , (1 - saturation) * gWeight + saturation, (1 - saturation) * gWeight    , 0,
        (1 - saturation) * bWeight    , (1 - saturation) * bWeight    , (1 - saturation) * bWeight + saturation, 0,
        0                    , 0                     , 0                     , 1
    };
  }

  @Override
  public float[] getMatrix(long presentationTimeUs, boolean useHdr) {
    return saturationMatrix;
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return saturation == 1f;
  }
} 