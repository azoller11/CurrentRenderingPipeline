#version 400 core

uniform sampler2D sceneTexture;
uniform vec2 resolution;
uniform float fxaaQuality;   // 0.0 → 1.0
uniform int skipFXAA = 1;        // 0 = apply FXAA, 1 = skip FXAA

in vec2 passTexCoords;
out vec4 FragColor;

// Make these adjustable
#define FXAA_REDUCE_MIN (1.0/128.0)
#define FXAA_REDUCE_MUL (1.0/8.0)
#define FXAA_SPAN_MAX 8.0

void main() {

    // ------------------------------------------------------------
    // HARD SKIP FXAA (fast path)
    // ------------------------------------------------------------
    if (skipFXAA != 0) {
        FragColor = texture(sceneTexture, passTexCoords);
        return;
    }

    vec3 rgbNW = texture(sceneTexture, passTexCoords + vec2(-1.0,  1.0) * resolution).rgb;
    vec3 rgbNE = texture(sceneTexture, passTexCoords + vec2( 1.0,  1.0) * resolution).rgb;
    vec3 rgbSW = texture(sceneTexture, passTexCoords + vec2(-1.0, -1.0) * resolution).rgb;
    vec3 rgbSE = texture(sceneTexture, passTexCoords + vec2( 1.0, -1.0) * resolution).rgb;
    vec3 rgbM  = texture(sceneTexture, passTexCoords).rgb;

    vec3 luma = vec3(0.299, 0.587, 0.114);
    float lumaNW = dot(rgbNW, luma);
    float lumaNE = dot(rgbNE, luma);
    float lumaSW = dot(rgbSW, luma);
    float lumaSE = dot(rgbSE, luma);
    float lumaM  = dot(rgbM,  luma);

    float lumaMin = min(lumaM, min(min(lumaNW, lumaNE), min(lumaSW, lumaSE)));
    float lumaMax = max(lumaM, max(max(lumaNW, lumaNE), max(lumaSW, lumaSE)));

    // Early exit for non-edges
    if ((lumaMax - lumaMin) < max(0.03125, lumaMax * 0.125)) {
        FragColor = vec4(rgbM, 1.0);
        return;
    }

    vec2 dir;
    dir.x = -((lumaNW + lumaNE) - (lumaSW + lumaSE));
    dir.y =  ((lumaNW + lumaSW) - (lumaNE + lumaSE));

    float dirReduce = max(
        (lumaNW + lumaNE + lumaSW + lumaSE) * (0.25 * FXAA_REDUCE_MUL),
        FXAA_REDUCE_MIN
    );

    float rcpDirMin = 1.0 / (min(abs(dir.x), abs(dir.y)) + dirReduce);

    float spanMax = mix(4.0, FXAA_SPAN_MAX, fxaaQuality);
    dir = clamp(dir * rcpDirMin, -spanMax, spanMax) * resolution;

    vec3 rgbA = 0.5 * (
        texture(sceneTexture, passTexCoords + dir * (1.0/3.0 - 0.5)).rgb +
        texture(sceneTexture, passTexCoords + dir * (2.0/3.0 - 0.5)).rgb
    );

    vec3 rgbB = rgbA * 0.5 + 0.25 * (
        texture(sceneTexture, passTexCoords + dir * -0.5).rgb +
        texture(sceneTexture, passTexCoords + dir *  0.5).rgb
    );

    float lumaB = dot(rgbB, luma);
    vec3 finalColor = ((lumaB < lumaMin) || (lumaB > lumaMax)) ? rgbA : rgbB;

    // Optional sharpening
    vec3 sharpened = finalColor * 1.1 - rgbM * 0.1;
    finalColor = mix(finalColor, sharpened, 0.25);

    FragColor = vec4(finalColor, 1.0);
}
