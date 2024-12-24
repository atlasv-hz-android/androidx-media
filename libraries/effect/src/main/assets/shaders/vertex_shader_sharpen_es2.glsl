#version 100

attribute vec4 aFramePosition;
uniform float uImageWidth; // 1.0f / width
uniform float uImageHeight; //1.0f/ height
uniform float uSharpen; //from -4.0 to 4.0, with 0.0 as the normal level

varying highp vec2 vTexSamplingCoord;
varying vec2 vLeftTextureCoord;
varying vec2 vRightTextureCoord;
varying vec2 vTopTextureCoord;
varying vec2 vBottomTextureCoord;
varying float vCenterMultiplier;
varying float vEdgeMultiplier;

void main() {
    gl_Position = aFramePosition;
    vTexSamplingCoord = aFramePosition.xy * 0.5 + 0.5;

    mediump vec2 widthStep = vec2(uImageWidth, 0.0);
    mediump vec2 heightStep = vec2(0.0, uImageHeight);

    vLeftTextureCoord = vTexSamplingCoord.xy - widthStep;
    vRightTextureCoord = vTexSamplingCoord.xy + widthStep;
    vTopTextureCoord = vTexSamplingCoord.xy + heightStep;
    vBottomTextureCoord = vTexSamplingCoord.xy - heightStep;

    vCenterMultiplier = 1.0 + 4.0 * uSharpen;
    vEdgeMultiplier = 1.0 * uSharpen ;
}



