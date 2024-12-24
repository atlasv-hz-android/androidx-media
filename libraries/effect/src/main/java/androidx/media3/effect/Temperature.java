package androidx.media3.effect;

import androidx.media3.common.util.GlUtil;
import androidx.media3.common.VideoFrameProcessingException;
import android.content.Context;
import java.io.IOException;

public final class Temperature implements GlEffect {
  private final float temperature;

  public Temperature(float temperature) {
    this.temperature = temperature;
  }

  @Override
  public GlShaderProgram toGlShaderProgram(Context context, boolean useHdr)
      throws VideoFrameProcessingException {
    return null;
  }

  @Override
  public boolean isNoOp(int inputWidth, int inputHeight) {
    return GlEffect.super.isNoOp(inputWidth, inputHeight);
  }
}
