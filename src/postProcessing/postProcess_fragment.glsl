#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;

uniform sampler2D screenTexture;
uniform float exposure;        // If 0, disable tone mapping.
uniform float gamma;           // If 0, disable gamma correction.
uniform float vignetteStrength; // If 0, disable vignette effect.

void main() {
    // Sample the rendered scene.
    vec3 hdrColor = texture(screenTexture, passTexCoords).rgb;

    // Tone mapping: only apply if exposure > 0.
    vec3 mappedColor;
    if (exposure > 0.0) {
        // Reinhard tone mapping operator example:
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
        // Compute the distance from the center (0.5, 0.5).
        vec2 position = passTexCoords - vec2(0.5);
        float distance = length(position);
        // Adjust the edge values (0.7 and 0.4) as needed.
        float vignette = smoothstep(0.7, 0.4, distance);
        // Mix the original corrected color with the vignetted version.
        correctedColor = mix(correctedColor, correctedColor * vignette, vignetteStrength);
    }
    
    correctedColor = clamp(correctedColor, 0.0, 1.0);

    fragColor = vec4(correctedColor, 1.0);
}
