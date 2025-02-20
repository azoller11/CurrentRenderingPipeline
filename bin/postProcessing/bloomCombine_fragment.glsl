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
    // Sample scene and bloom textures safely
    vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
    vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;

    // Ensure texture fetches return finite numbers
   if (any(isnan(sceneColor)) || any(isinf(sceneColor))) sceneColor = vec3(0.0);
	if (any(isnan(bloomColor)) || any(isinf(bloomColor))) bloomColor = vec3(0.0);


    // Combine scene and bloom, ensuring no negative values
    vec3 hdrColor = max(sceneColor + bloomColor * max(bloomIntensity, 0.0), vec3(0.0));

    // Prevent extreme HDR values that could cause NaN issues
    hdrColor = clamp(hdrColor, vec3(0.0), vec3(100.0));

    // Tone mapping: only apply if exposure > 0 and ensure no division by zero
    vec3 mappedColor;
    if (exposure > 0.0) {
        mappedColor = hdrColor / (hdrColor + vec3(1.0));
        mappedColor *= exposure;
    } else {
        mappedColor = hdrColor;
    }

    // Gamma correction: Ensure gamma is valid before applying `pow()`
    vec3 correctedColor = mappedColor;
    if (gamma > 0.01) { // Prevent division by zero
        correctedColor = pow(max(mappedColor, vec3(0.0)), vec3(1.0 / gamma)); 
    }

    // Vignette effect: only apply if vignetteStrength > 0
    if (vignetteStrength > 0.0) {
        vec2 position = passTexCoords - vec2(0.5);
        float distance = length(position);
        float vignette = smoothstep(0.7, 0.4, distance);
        correctedColor = mix(correctedColor, correctedColor * vignette, vignetteStrength);
    }

    // Ensure final color is finite and in valid range
    fragColor = vec4(clamp(correctedColor, 0.0, 1.0), 1.0);
}
