#version 100

precision highp float;
varying highp vec2 vTexSamplingCoord;
uniform sampler2D uTexSampler;

uniform lowp float uTemperature;//[-1.0, 1.0] 色温调节范围
const lowp vec3 warmFilter = vec3(0.93, 0.54, 0.0);
const mediump mat3 RGBtoYIQ = mat3(0.299, 0.587, 0.114, 0.596, -0.274, -0.322, 0.212, -0.523, 0.311);
const mediump mat3 YIQtoRGB = mat3(1.0, 0.956, 0.621, 1.0, -0.272, -0.647, 1.0, -1.105, 1.702);
const mediump vec3 luminanceWeighting = vec3(0.2125, 0.7154, 0.0721);

void main()
{
    mediump vec4 textureColor = texture2D(uTexSampler, vTexSamplingCoord);
    if (textureColor.a == 0.0) {
        gl_FragColor = textureColor;
        return;
    }

    mediump vec3 yiq = RGBtoYIQ * textureColor.rgb;
    yiq.b = clamp(yiq.b, -0.5226, 0.5226);
    lowp vec3 rgb = YIQtoRGB * yiq;
    lowp float A = (rgb.r < 0.5 ? (2.0 * rgb.r * warmFilter.r) : (1.0 - 2.0 * (1.0 - rgb.r) * (1.0 - warmFilter.r)));
    lowp float B = (rgb.g < 0.5 ? (2.0 * rgb.g * warmFilter.g) : (1.0 - 2.0 * (1.0 - rgb.g) * (1.0 - warmFilter.g)));
    lowp float C =  (rgb.b < 0.5 ? (2.0 * rgb.b * warmFilter.b) : (1.0 - 2.0 * (1.0 - rgb.b) * (1.0 - warmFilter.b)));
    lowp vec3 processed = vec3(A, B, C);

    // Output to screen
    gl_FragColor = vec4(mix(rgb, processed, uTemperature * 0.3 ), textureColor.a);

}