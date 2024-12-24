#version 100

precision highp float;
uniform sampler2D uTexSampler;

varying highp vec2 vTexSamplingCoord;
varying highp vec2 vLeftTextureCoord;
varying highp vec2 vRightTextureCoord;
varying highp vec2 vTopTextureCoord;
varying highp vec2 vBottomTextureCoord;
varying highp float vCenterMultiplier;
varying highp float vEdgeMultiplier;


void main()
{
    mediump vec4 rawTextureColor = texture2D(uTexSampler, vTexSamplingCoord);
    mediump vec3 textureColor = rawTextureColor.rgb;
    if (rawTextureColor.a == 0.0) {
        gl_FragColor = rawTextureColor;
        return;
    }

    mediump vec3 leftTextureColor = texture2D(uTexSampler, vLeftTextureCoord).rgb;
    mediump vec3 rightTextureColor = texture2D(uTexSampler, vRightTextureCoord).rgb;
    mediump vec3 topTextureColor = texture2D(uTexSampler, vTopTextureCoord).rgb;
    mediump vec3 bottomTextureColor = texture2D(uTexSampler, vBottomTextureCoord).rgb;

    gl_FragColor = vec4((textureColor * vCenterMultiplier - (leftTextureColor * vEdgeMultiplier + rightTextureColor * vEdgeMultiplier + topTextureColor * vEdgeMultiplier + bottomTextureColor * vEdgeMultiplier)), texture2D(uTexSampler, vBottomTextureCoord).w);
}


