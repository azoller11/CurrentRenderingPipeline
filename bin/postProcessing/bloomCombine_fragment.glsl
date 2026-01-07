#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;

uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform sampler2D fogTexture;

uniform float bloomIntensity;

uniform float exposure;         // If 0, disable tone mapping.
uniform float gamma;            // If 0, disable gamma correction.
uniform float vignetteStrength; // If 0, disable vignette effect.

void main() {
    // --------------------------------------------------
    // 1. Sample inputs safely
    // --------------------------------------------------
    vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
    vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;
    vec4 fogSample  = texture(fogTexture,  passTexCoords);

    if (any(isnan(sceneColor)) || any(isinf(sceneColor))) sceneColor = vec3(0.0);
    if (any(isnan(bloomColor)) || any(isinf(bloomColor))) bloomColor = vec3(0.0);
    if (any(isnan(fogSample.rgb)) || any(isinf(fogSample.rgb))) fogSample.rgb = vec3(0.0);
    if (isnan(fogSample.a) || isinf(fogSample.a)) fogSample.a = 0.0;

    float fogAlpha = clamp(fogSample.a, 0.0, 1.0);

    // --------------------------------------------------
    // 2. Apply volumetric fog (PRE-BLOOM)
    //    scene = scene * (1 - fog) + fog
    // --------------------------------------------------
    vec3 foggedScene =
        sceneColor * (1.0 - fogAlpha) +
        fogSample.rgb;

    foggedScene = max(foggedScene, vec3(0.0));

    // --------------------------------------------------
    // 3. Add bloom AFTER fog
    // --------------------------------------------------
    vec3 hdrColor =
        foggedScene +
        bloomColor * max(bloomIntensity, 0.0);

    hdrColor = clamp(hdrColor, vec3(0.0), vec3(100.0));

    // --------------------------------------------------
    // 4. Tone mapping
    // --------------------------------------------------
    vec3 mappedColor;
    if (exposure > 0.0) {
        mappedColor = hdrColor / (hdrColor + vec3(1.0));
        mappedColor *= exposure;
    } else {
        mappedColor = hdrColor;
    }

    // --------------------------------------------------
    // 5. Gamma correction
    // --------------------------------------------------
    vec3 correctedColor = mappedColor;
    if (gamma > 0.01) {
        correctedColor =
            pow(max(mappedColor, vec3(0.0)), vec3(1.0 / gamma));
    }

    // --------------------------------------------------
    // 6. Vignette
    // --------------------------------------------------
    if (vignetteStrength > 0.0) {
        vec2 position = passTexCoords - vec2(0.5);
        float distance = length(position);
        float vignette = smoothstep(0.7, 0.4, distance);
        correctedColor =
            mix(correctedColor,
                correctedColor * vignette,
                vignetteStrength);
    }

    // --------------------------------------------------
    // 7. Final clamp
    // --------------------------------------------------
    fragColor = vec4(clamp(correctedColor, 0.0, 1.0), 1.0);
}
