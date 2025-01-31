#version 400 core

in vec3 texCoords;
out vec4 fragColor;

uniform vec3 topColor;
uniform vec3 bottomColor;
uniform vec3 sunPosition;
uniform vec3 sunColor;

void main() {
    // Compute sky gradient
    float factor = texCoords.y * 0.5 + 0.5;
    vec3 skyColor = mix(bottomColor, topColor, factor);

    // Normalize sun position and direction
    vec3 sunDir = normalize(sunPosition);
    float sunIntensity = max(dot(sunDir, normalize(texCoords)), 0.0);

    // Apply sun effect (smooth glow effect)
    float sunGlow = exp(-pow(sunIntensity - 1.0, 2.0) * 1000000.0);
    
    // Blend sky color with sun color based on intensity
    vec3 finalColor = mix(skyColor, sunColor, sunGlow);

    fragColor = vec4(finalColor, 1.0);
}
