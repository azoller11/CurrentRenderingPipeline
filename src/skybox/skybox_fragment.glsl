#version 400 core

// Input direction for the skybox (from vertex shader)
in vec3 texCoords;
out vec4 fragColor;

// Sky gradient colors.
uniform vec3 topColor;
uniform vec3 bottomColor;

// Sun uniforms.
uniform vec3 sunPosition;  // Normalized direction vector for the sun (in world space)
uniform vec3 sunColor;
uniform float sunSize;     // Angular radius in radians (e.g., 0.05)

// Moon uniforms.
uniform vec3 moonPosition; // Normalized direction vector for the moon (in world space)
uniform vec3 moonColor;
uniform float moonSize;    // Angular radius in radians (e.g., 0.02)

// Extra light uniforms (for additional effects).
uniform vec3 light1Position;
uniform vec3 light1Color;
uniform vec3 light2Position;
uniform vec3 light2Color;

// Bloom parameter: controls how far (in radians) the glow extends beyond the disk edge.
uniform float bloomAmount; // e.g., 0.03 - 0.05

// New uniform: inverse of the camera's rotation matrix (3x3)
// This transforms a vector from camera space into a fixed world-space orientation.
uniform mat3 invViewRotation;

//---------------------------------------------------------------------
// Computes the light intensity (disk plus bloom) for a given light source.
// 'size' defines the hard disk angular radius, and 'bloom' the extra halo width.
float computeLightIntensity(vec3 rayDir, vec3 lightDir, float size, float bloom) {
    // Calculate the angle between the viewing ray and the light's direction.
    float cosAngle = dot(rayDir, lightDir);
    float angle = acos(clamp(cosAngle, -1.0, 1.0));
    
    // Disk: fully lit (1.0) for angles within the disk.
    float disk = step(angle, size);
    
    // Bloom: an exponential falloff beyond the disk edge.
    float bloomIntensity = exp(-pow((angle - size) / bloom, 2.0));
    
    // Combine the disk and bloom.
    float intensity = disk + 1.0 * bloomIntensity;
    
    return clamp(intensity, 0.0, 1.0);
}

//---------------------------------------------------------------------
// Computes a procedural star field contribution based on a fixed direction.
// The star field is computed in world space (using invViewRotation) so that stars remain static.
vec3 computeStars(vec3 fixedDir, float starVisibility) {
    // Create a pseudo-random value from the fixed (world-space) direction.
    float starNoise = fract(sin(dot(fixedDir, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    
    // Increase the threshold to lower the density of stars.
    // Previous threshold was 0.995; try a higher value like 0.998.
    float threshold = 0.998;
    
    // Compute star factor: only values above the threshold contribute.
    float starFactor = max((starNoise - threshold) / (1.0 - threshold), 0.0);
    
    // Optionally vary the star brightness.
    float brightness = starFactor * 1.5;
    
    return vec3(brightness) * starVisibility;
}

//---------------------------------------------------------------------
// Main function: Composes the sky gradient, sun, moon, extra lights, and stars.
void main() {
    // Normalize the input ray direction.
    vec3 rayDir = normalize(texCoords);
    
    // --- 1. Sky Gradient ---
    float t = rayDir.y * 0.5 + 0.5;
    vec3 skyColor = mix(bottomColor, topColor, t);
    
    // --- 2. Compute Fixed Direction for Stars ---
    // Transform the ray direction by the inverse view rotation to lock the star field in world space.
    vec3 fixedRayDir = normalize(invViewRotation * rayDir);
    
    // --- 3. Normalize Light Directions (in world space) ---
    vec3 sunDir    = normalize(sunPosition);
    vec3 moonDir   = normalize(moonPosition);
    vec3 light1Dir = normalize(light1Position);
    vec3 light2Dir = normalize(light2Position);
    
    // --- 4. Compute Light Intensities with Bloom ---
    float intensitySun    = computeLightIntensity(rayDir, sunDir, sunSize, bloomAmount);
    float intensityMoon   = computeLightIntensity(rayDir, moonDir, moonSize, bloomAmount);
    float intensityLight1 = computeLightIntensity(rayDir, light1Dir, sunSize, bloomAmount);
    float intensityLight2 = computeLightIntensity(rayDir, light2Dir, moonSize, bloomAmount);
    
    // --- 5. Compute Star Visibility ---
    // Instead of a hard cutoff, we use a smooth transition based on the sun's world Y value.
    // When the sun is high (sunPosition.y > 0.1), stars are nearly invisible.
    // As the sun drops (sunPosition.y <= 0.1), stars fade in.
    float starVisibility = clamp(1.0 - smoothstep(0.0, 0.1, sunPosition.y), 0.0, 1.0);
    // For testing, you might temporarily force stars to be visible:
    starVisibility = 0.0;
    
    vec3 stars = computeStars(fixedRayDir, starVisibility);
    
    // --- 6. Final Composition ---
    vec3 finalColor = skyColor;
    finalColor += sunColor    * intensitySun;
    finalColor += moonColor   * intensityMoon;
    finalColor += light1Color * intensityLight1;
    finalColor += light2Color * intensityLight2;
    finalColor += stars;
    
    fragColor = vec4(finalColor, 1.0);
}
