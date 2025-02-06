#version 400 core

// Input direction for the skybox (from vertex shader)
in vec3 texCoords;
out vec4 fragColor;

// Sky gradient colors.
uniform vec3 topColor;
uniform vec3 bottomColor;

// Sun uniforms.
uniform vec3 sunPosition;    // World-space position (direction) of the sun
uniform float sunSize;       // Angular radius in radians
uniform float sunBloomAmount; // Halo width in radians

// Extra sun color uniforms.
uniform vec3 sunCoreColor;   // Core (disk) color computed on Java side
uniform vec3 sunHaloColor;   // Halo color computed on Java side

// New uniform for warm tint factor (0 = no extra warm tint, 1 = full warm tint)
uniform float sunWarmFactor;

// Moon uniforms.
uniform vec3 moonPosition;   // World-space direction for the moon
uniform vec3 moonColor;
uniform float moonSize;      // Angular radius in radians
uniform float moonBloomAmount; // Bloom for the moon

// Extra light uniforms.
uniform vec3 light1Position;
uniform vec3 light1Color;
uniform vec3 light2Position;
uniform vec3 light2Color;

// Inverse of the camera's rotation matrix (3x3) for a fixed star field.
uniform mat3 invViewRotation;

//---------------------------------------------------------------------
// Computes the light intensity (disk plus bloom) for non-sun sources.
float computeLightIntensity(vec3 rayDir, vec3 lightDir, float size, float bloom) {
    float cosAngle = dot(rayDir, lightDir);
    float angle = acos(clamp(cosAngle, -1.0, 1.0));
    float disk = step(angle, size);
    float bloomIntensity = exp(-pow((angle - size) / bloom, 2.0));
    float intensity = disk + bloomIntensity;
    return clamp(intensity, 0.0, 1.0);
}

//---------------------------------------------------------------------
// Computes a procedural star field contribution based on a fixed direction.
vec3 computeStars(vec3 fixedDir, float starVisibility) {
    float starNoise = fract(sin(dot(fixedDir, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    float threshold = 0.998;
    float starFactor = max((starNoise - threshold) / (1.0 - threshold), 0.0);
    float brightness = starFactor * 1.5;
    return vec3(brightness) * starVisibility;
}

void main() {
    vec3 rayDir = normalize(texCoords);
    
    // --- 1. Sky Gradient ---
    float t = rayDir.y * 0.5 + 0.5;
    vec3 skyColor = mix(bottomColor, topColor, t);
    
    // --- 2. Fixed Direction for Stars ---
    vec3 fixedRayDir = normalize(invViewRotation * rayDir);
    
    // --- 3. Normalize light directions ---
    vec3 sunDir    = normalize(sunPosition);
    vec3 moonDir   = normalize(moonPosition);
    vec3 light1Dir = normalize(light1Position);
    vec3 light2Dir = normalize(light2Position);
    
    // --- 4. Compute Moon and extra light intensities.
    float intensityMoon   = computeLightIntensity(rayDir, moonDir, moonSize, moonBloomAmount);
    float intensityLight1 = computeLightIntensity(rayDir, light1Dir, sunSize, sunBloomAmount);
    float intensityLight2 = computeLightIntensity(rayDir, light2Dir, moonSize, moonBloomAmount);
    
    // --- 5. Compute star field ---
    float starVisibility = clamp(1.0 - smoothstep(0.0, 0.1, sunPosition.y), 0.0, 1.0);
    vec3 stars = computeStars(fixedRayDir, starVisibility);
    
    // --- 6. Compute the sun’s contribution with a realistic core & halo ---
    float cosSun = dot(rayDir, sunDir);
    float angleDiff = acos(clamp(cosSun, -1.0, 1.0));
    
    // Compute core and halo factors.
    float coreFactor = smoothstep(sunSize, sunSize - 0.005, angleDiff);
    float haloFactor = exp(-pow((angleDiff - sunSize) / sunBloomAmount, 2.0));
    vec3 sunContribution = mix(sunHaloColor, sunCoreColor, coreFactor) * clamp(coreFactor + haloFactor, 0.0, 1.0);
    
    // --- 7. Apply extra warm tint if the sun is near the horizon.
    // The warm tint is an extra blend of an orange tone.
    vec3 warmTint = vec3(1.0, 0.5, 0.0); // Deep orange.
    sunContribution = mix(sunContribution, sunContribution * warmTint, sunWarmFactor);
    
    // --- 8. Final composition ---
    vec3 finalColor = skyColor;
    finalColor += sunContribution;
    finalColor += moonColor * intensityMoon;
    finalColor += light1Color * intensityLight1;
    finalColor += light2Color * intensityLight2;
    finalColor += stars;
    
    fragColor = vec4(finalColor, 1.0);
}
