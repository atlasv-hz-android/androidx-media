#version 100
precision highp float;
uniform sampler2D uTexSampler;
uniform sampler2D uColorLut;
uniform float uColorLutLength;
uniform int uColorLutFormat;
varying vec2 vTexSamplingCoord;

vec3 applyLookup(vec3 color) {
  float redCoord = color.r * (uColorLutLength - 1.0);
  float redCoordLow = clamp(floor(redCoord), 0.0, uColorLutLength - 2.0);
  float lowerY = (0.5 + redCoordLow * uColorLutLength +
                  color.g * (uColorLutLength - 1.0)) /
                 (uColorLutLength * uColorLutLength);
  // The upperY is the same position moved up by one LUT plane.
  float upperY = lowerY + 1.0 / uColorLutLength;

  // The x position is the blue color channel (x-axis in LUT[R][G][B]).
  float x = (0.5 + color.b * (uColorLutLength - 1.0)) / uColorLutLength;

  vec3 lowerRgb = texture2D(uColorLut, vec2(x, lowerY)).rgb;
  vec3 upperRgb = texture2D(uColorLut, vec2(x, upperY)).rgb;

  // Linearly interpolate between lowerRgb and upperRgb based on the
  // distance of the actual in the plane and the lower sampling position.
  return mix(lowerRgb, upperRgb, redCoord - redCoordLow);
}

vec3 applyLookup2(vec3 textureColor) {
  float matchLut = 8.0;
  if (uColorLutLength == 64.0) {
    matchLut = 4.0;
  } else {
    matchLut = 8.0;
  }

  mediump float blueColor = textureColor.b * (pow(matchLut, 2.0)-1.0);
  mediump vec2 quad1;
  quad1.y = floor(floor(blueColor) / matchLut);
  quad1.x = floor(blueColor) - (quad1.y * matchLut);
  mediump vec2 quad2;
  quad2.y = floor(ceil(blueColor) / matchLut);
  quad2.x = ceil(blueColor) - (quad2.y * matchLut);
  highp vec2 texPos1;
  float dimen = pow(matchLut, 3.0);
  texPos1.x = (quad1.x *(1.0/matchLut)) + 0.5/(dimen)+ ((1.0/matchLut - 1.0/dimen) * textureColor.r);
  texPos1.y = (quad1.y *(1.0/matchLut)) + 0.5/(dimen) + ((1.0/matchLut - 1.0/dimen) * textureColor.g);
  highp vec2 texPos2;
  texPos2.x = (quad2.x *(1.0/matchLut)) + 0.5/(dimen) + ((1.0/matchLut - 1.0/dimen) * textureColor.r);
  texPos2.y = (quad2.y *(1.0/matchLut)) + 0.5/(dimen) + ((1.0/matchLut - 1.0/dimen) * textureColor.g);
  lowp vec3 newColor1 = texture2D(uColorLut, texPos1).rgb;
  lowp vec3 newColor2 = texture2D(uColorLut, texPos2).rgb;
  lowp vec3 newColor = mix(newColor1, newColor2, fract(blueColor));
  return newColor;
}

void main() {
  vec4 inputColor = texture2D(uTexSampler, vTexSamplingCoord);

  if (uColorLutFormat == 2) {
    gl_FragColor.rgb = applyLookup2(inputColor.rgb);
  } else {
    gl_FragColor.rgb = applyLookup(inputColor.rgb);
  }
  gl_FragColor.a = inputColor.a;
}
