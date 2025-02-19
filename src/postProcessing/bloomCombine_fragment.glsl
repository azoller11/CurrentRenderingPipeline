#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;

uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform float bloomIntensity;

uniform float exposure;        // If 0, disable tone mapping.
uniform float gamma;           // If 0, disable gamma correction.
uniform float vignetteStrength; // If 0, disable vignette effect.

void main() {
    // Sample scene and bloom textures.
    vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
    vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;
    
    // Combine scene and bloom into an HDR color.
    vec3 hdrColor = sceneColor + bloomColor * bloomIntensity;

    // Tone mapping: only apply if exposure > 0.
    vec3 mappedColor;
    if (exposure > 0.0) {
        mappedColor = hdrColor / (hdrColor + vec3(1.0));
        mappedColor *= exposure;
    } else {
        mappedColor = hdrColor;
    }

    // Gamma correction: only apply if gamma > 0.
    vec3 correctedColor;
    if (gamma > 0.0) {
        correctedColor = pow(mappedColor, vec3(1.0 / gamma));
    } else {
        correctedColor = mappedColor;
    }

    // Vignette effect: only apply if vignetteStrength > 0.
    if (vignetteStrength > 0.0) {
        // Compute distance from the center (0.5, 0.5).
        vec2 position = passTexCoords - vec2(0.5);
        float distance = length(position);
        // Compute vignette factor using smoothstep.
        float vignette = smoothstep(0.7, 0.4, distance);
        // Blend the color with the vignetted version.
        correctedColor = mix(correctedColor, correctedColor * vignette, vignetteStrength);
    }

    fragColor = vec4(correctedColor, 1.0);
}
