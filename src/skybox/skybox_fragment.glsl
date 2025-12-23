#version 400 core

in vec3 texCoords;
out vec4 fragColor;

// ------------------------------------------------------------
// Fog
// ------------------------------------------------------------
uniform float density  = 0.0005;
uniform float gradient = 2.0;
uniform vec3 fogColor = vec3(0.25, 0.25, 0.25);

uniform float fogHeightStart = -0.5f;   // lower bound (0 = horizon, -1 = below)
uniform float fogHeightEnd = 0.5f;     // upper bound (1 = sky top)

// ------------------------------------------------------------
// Skybox texture mode
// ------------------------------------------------------------
uniform int hasSkybox;          
uniform sampler2D faces[6];     
uniform float skyboxBlend;      

// ------------------------------------------------------------
// Sky gradient colors
// ------------------------------------------------------------
uniform vec3 topColor;
uniform vec3 bottomColor;

// ------------------------------------------------------------
// Sun uniforms
// ------------------------------------------------------------
uniform vec3 sunPosition;
uniform float sunSize;
uniform float sunBloomAmount;

uniform vec3 sunCoreColor;
uniform vec3 sunHaloColor;

// ------------------------------------------------------------
// Moon uniforms
// ------------------------------------------------------------
uniform vec3 moonPosition;
uniform vec3 moonColor;
uniform float moonSize;
uniform float moonBloomAmount;

// ------------------------------------------------------------
// Extra halo lights
// ------------------------------------------------------------
uniform vec3 light1Position;
uniform vec3 light1Color;

uniform vec3 light2Position;
uniform vec3 light2Color;

// ------------------------------------------------------------
// Stars
// ------------------------------------------------------------
uniform mat3 invViewRotation;

uniform float skyboxTextureBrightness;


// ------------------------------------------------------------
// Utility functions
// ------------------------------------------------------------
float computeLightIntensity(vec3 rayDir, vec3 lightDir, float size, float bloom)
{
    float cosAngle = dot(rayDir, lightDir);
    float angle = acos(clamp(cosAngle, -1.0, 1.0));

    float disk = step(angle, size);
    float bloomIntensity = exp(-pow((angle - size) / bloom, 2.0));

    return clamp(disk + bloomIntensity, 0.0, 1.0);
}

vec3 computeStars(vec3 fixedDir, float visibility)
{
    float n = fract(sin(dot(fixedDir, vec3(12.9898, 78.233, 37.719))) * 43758.5453);
    float threshold = 0.998;

    float f = max((n - threshold) / (1.0 - threshold), 0.0);
    float brightness = f * 1.5;

    return vec3(brightness) * visibility;
}

vec4 sampleCubeFaces(vec3 d) {
    vec3 ad = abs(d);

    if (ad.x >= ad.y && ad.x >= ad.z) {
        if (d.x > 0.0) {
            vec2 uv = vec2(-d.z, d.y) / ad.x * 0.5 + 0.5;
            return texture(faces[0], uv);
        } else {
            vec2 uv = vec2(d.z, d.y) / ad.x * 0.5 + 0.5;
            return texture(faces[1], uv);
        }
    }
    else if (ad.y >= ad.x && ad.y >= ad.z) {
        if (d.y > 0.0) {
            vec2 uv = vec2(d.x, -d.z) / ad.y;
            uv = vec2(-uv.y, uv.x);
            uv = uv * 0.5 + 0.5;
            return texture(faces[2], uv);
        } else {
            vec2 uv = vec2(d.x, d.z) / ad.y;
            uv = vec2(uv.y, -uv.x);
            uv = uv * 0.5 + 0.5;
            return texture(faces[3], uv);
        }
    } else {
        if (d.z > 0.0) {
            vec2 uv = vec2(d.x, d.y) / ad.z * 0.5 + 0.5;
            return texture(faces[4], uv);
        } else {
            vec2 uv = vec2(-d.x, d.y) / ad.z * 0.5 + 0.5;
            return texture(faces[5], uv);
        }
    }
}


// ------------------------------------------------------------
// MAIN
// ------------------------------------------------------------
void main()
{
    vec3 rayDir = normalize(texCoords);

    // --------------------------------------------------------
    // 1. Procedural Sky Gradient
    // --------------------------------------------------------
    float t = rayDir.y * 0.5 + 0.5;
    vec3 proceduralSky = mix(bottomColor, topColor, t);

    // --------------------------------------------------------
    // 1b. Optional cube-textured skybox
    // --------------------------------------------------------
    vec3 texSky = sampleCubeFaces(rayDir).rgb;
    vec3 skyColor;

    if (hasSkybox == 1) {
        skyColor =
            proceduralSky * (1.0 - skyboxBlend) +
            texSky        * (skyboxBlend * skyboxTextureBrightness);
    } else {
        skyColor = proceduralSky;
    }

    // --------------------------------------------------------
    // 2. Stars
    // --------------------------------------------------------
    vec3 fixedRay = normalize(invViewRotation * rayDir);
    float starVisibility = clamp(1.0 - smoothstep(0.0, 0.1, sunPosition.y), 0.0, 1.0);
    vec3 stars = computeStars(fixedRay, starVisibility);

    // --------------------------------------------------------
    // 3. Normalize Light Directions
    // --------------------------------------------------------
    vec3 sunDir = normalize(sunPosition);
    vec3 moonDir = normalize(moonPosition);

    // --------------------------------------------------------
    // 4. Sun Rendering
    // --------------------------------------------------------
    float cosSun = dot(rayDir, sunDir);
    float sunAngle = acos(clamp(cosSun, -1.0, 1.0));

    float core = smoothstep(sunSize, sunSize * 0.8, sunAngle);
    float halo = exp(-sunAngle * 45.0);

    float horizonBoost = 1.0 - smoothstep(0.0, 0.3, sunDir.y);
    halo *= mix(1.0, 2.5, horizonBoost);

    vec3 sunColorCombined = sunCoreColor * core + sunHaloColor * halo;
    float horizonDarken = smoothstep(0.0, 0.2, sunDir.y);
    sunColorCombined *= horizonDarken;

    // --------------------------------------------------------
    // 5. Moon
    // --------------------------------------------------------
    float moonIntensity = computeLightIntensity(rayDir, moonDir, moonSize, moonBloomAmount);
    vec3 moonContribution = moonColor * moonIntensity;

    // --------------------------------------------------------
    // 6. Extra Lights
    // --------------------------------------------------------
    vec3 light1Dir = normalize(light1Position);
    vec3 light2Dir = normalize(light2Position);

    float l1 = computeLightIntensity(rayDir, light1Dir, sunSize, sunBloomAmount);
    float l2 = computeLightIntensity(rayDir, light2Dir, moonSize, moonBloomAmount);

    vec3 extraLights = light1Color * l1 + light2Color * l2;

    // --------------------------------------------------------
    // 7. Compose Final Sky Color
    // --------------------------------------------------------
    vec3 finalColor = skyColor;
    finalColor += sunColorCombined;
    finalColor += moonContribution;
    finalColor += extraLights;
    finalColor += stars;

    // --------------------------------------------------------
    // 8. Tone Mapping (Reinhard)
    // --------------------------------------------------------
    finalColor = finalColor / (1.0 + finalColor);

	   //------------------------------------------------------------
	// IMPROVED SKYBOX FOG (works with textured cube maps)
	//------------------------------------------------------------
	
	// 1. Horizon factor (0 = sky top, 1 = horizon)
	float h = clamp(1.0 - rayDir.y, 0.0, 1.0);
	
	// 2. Height mask
	float heightMask = smoothstep(fogHeightStart, fogHeightEnd, h);
	
	// 3. Exponential depth falloff
	float fogAmount = 1.0 - exp(-pow(h * density, gradient));
	
	// 4. Combine masks
	fogAmount *= heightMask;
	
	// 5. Strenghten fog for textures (important)
	fogAmount = pow(fogAmount, 1.2);
	
	// 6. Apply fog
	finalColor = mix(finalColor, fogColor, fogAmount);
	fragColor = vec4(finalColor, 1.0);
}
