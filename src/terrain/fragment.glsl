#version 400 core

// =============================================================
// Lighting structs
// =============================================================
struct Light {
    vec3 position;
    vec3 color;
    vec3 attenuation;
    float distance;
};

uniform int numLights;
uniform Light lights[16];
uniform vec3 cameraPos;

// =============================================================
// Blend & texture sampler uniforms
// =============================================================
uniform sampler2D blendMap;

uniform sampler2D layer0;
uniform sampler2D layer1;
uniform sampler2D layer2;
uniform sampler2D layer3;

uniform sampler2D layer0Normal;
uniform sampler2D layer1Normal;
uniform sampler2D layer2Normal;
uniform sampler2D layer3Normal;

uniform sampler2D layer0Metallic;
uniform sampler2D layer1Metallic;
uniform sampler2D layer2Metallic;
uniform sampler2D layer3Metallic;

uniform sampler2D layer0Roughness;
uniform sampler2D layer1Roughness;
uniform sampler2D layer2Roughness;
uniform sampler2D layer3Roughness;

uniform sampler2D layer0Ao;
uniform sampler2D layer1Ao;
uniform sampler2D layer2Ao;
uniform sampler2D layer3Ao;

uniform bool hasNormal0;
uniform bool hasNormal1;
uniform bool hasNormal2;
uniform bool hasNormal3;

uniform bool hasMetallic0;
uniform bool hasMetallic1;
uniform bool hasMetallic2;
uniform bool hasMetallic3;

uniform bool hasRoughness0;
uniform bool hasRoughness1;
uniform bool hasRoughness2;
uniform bool hasRoughness3;

uniform bool hasAo0;
uniform bool hasAo1;
uniform bool hasAo2;
uniform bool hasAo3;

// Tiling
uniform float tileScale0;
uniform float tileScale1;
uniform float tileScale2;
uniform float tileScale3;

// =============================================================
// Shadows
// =============================================================
uniform sampler2D shadowMap;
uniform float shadowStrength;
uniform float terrainShadowStrength;
uniform float ambientStrength;

uniform float density  = 0.0000;         
uniform float gradient =  0.001;
uniform vec3 fogColor = vec3(0.25, 0.25, 0.25);

// =============================================================
// Inputs from vertex shader
// =============================================================
in VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} fs_in;

out vec4 fragColor;

// =============================================================
// Utility
// =============================================================
const float PI = 3.14159265359;

// =============================================================
// PBR Functions
// =============================================================
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a  = roughness * roughness;
    float a2 = a * a;
    float NdotH  = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;

    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;

    return a2 / max(denom, 0.0000001);
}

float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = roughness + 1.0;
    float k = (r * r) / 8.0;

    float denom = NdotV * (1.0 - k) + k;
    return NdotV / max(denom, 0.0000001);
}

float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.00001);
    float NdotL = max(dot(N, L), 0.00001);
    float ggx1 = GeometrySchlickGGX(NdotV, roughness);
    float ggx2 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

// =============================================================
// Normal blending helper
// =============================================================
vec3 computeLayerNormal(vec3 worldNormal, vec3 worldTangent, vec2 uv, sampler2D normalMap, bool hasNormal);

// =============================================================
// Improved Normal Blending Function
// =============================================================
vec3 blendNormals(vec3 n1, vec3 n2, float weight)
{
    return normalize(mix(n1, n2, weight));
}

// =============================================================
// Advanced Weight Computation with Smooth Transitions
// =============================================================
vec4 computeBlendWeights(vec4 rawBlend)
{
    // Step 1: Apply a small bias to prevent completely black areas
    // This ensures there's always at least a tiny bit of texture visible
    const float MIN_BIAS = 0.02;
    vec4 biasedBlend = rawBlend + vec4(MIN_BIAS);
    
    // Step 2: Clean up tiny values (anti-bleeding)
    const float MIN_THRESHOLD = 0.01;
    vec4 cleanBlend = vec4(
        biasedBlend.r < MIN_THRESHOLD ? 0.0 : biasedBlend.r,
        biasedBlend.g < MIN_THRESHOLD ? 0.0 : biasedBlend.g,
        biasedBlend.b < MIN_THRESHOLD ? 0.0 : biasedBlend.b,
        biasedBlend.a < MIN_THRESHOLD ? 0.0 : biasedBlend.a
    );
    
    // Step 3: Check if we have any color channels (RGB) with significant values
    float colorSum = cleanBlend.r + cleanBlend.g + cleanBlend.b;
    float alphaValue = cleanBlend.a;
    
    // Step 4: NEW - Better alpha channel handling
    // Instead of completely suppressing alpha when colors exist,
    // we blend it more intelligently based on its relative strength
    if (colorSum > 0.001 && alphaValue > 0.001) {
        // If both colors and alpha exist, reduce alpha based on color strength
        // but don't eliminate it completely
        float colorStrength = min(colorSum, 1.0);
        cleanBlend.a *= (1.0 - colorStrength * 0.8); // Reduce but not eliminate
    }
    
    // Step 5: Compute sum for normalization
    float total = cleanBlend.r + cleanBlend.g + cleanBlend.b + cleanBlend.a;
    
    // Step 6: Handle edge cases
    if (total < 0.001) {
        // If everything is near zero, use equal distribution to avoid pure black
        return vec4(0.25, 0.25, 0.25, 0.25);
    }
    
    // Step 7: Apply non-linear response for smoother transitions
    // This helps prevent sudden jumps to black
    const float EXPONENT = 1.2; // Lower exponent for smoother transitions
    vec4 powered = pow(cleanBlend, vec4(EXPONENT));
    float poweredSum = powered.r + powered.g + powered.b + powered.a;
    
    // Step 8: Ensure we don't have pure black by maintaining minimum weights
    vec4 normalized = powered / poweredSum;
    
    // Add a tiny bit of all channels to prevent complete disappearance
    const float MIN_WEIGHT = 0.001;
    normalized = mix(normalized, vec4(0.25), MIN_WEIGHT);
    
    // Re-normalize
    float finalSum = normalized.r + normalized.g + normalized.b + normalized.a;
    return normalized / finalSum;
}
// =============================================================
// Texture Sampling with Seam Reduction
// =============================================================
vec4 sampleTexture(sampler2D tex, vec2 uv, float tileScale)
{
    // Scale the UV coordinates
    vec2 scaledUV = uv * tileScale;
    
    // Better jitter calculation - use world position for consistent patterns
    vec2 jitter = vec2(
        sin(fs_in.worldPos.x * 0.1) * 0.0005,
        cos(fs_in.worldPos.z * 0.1) * 0.0005
    );
    
    return texture(tex, scaledUV + jitter);
}

// =============================================================
// Shadows
// =============================================================
float computeShadow(vec4 lightSpacePos, vec3 N_geom, vec3 lightDir)
{
    // Keep this small for terrain; large kernels cause leaks near walls
    const int K = 1; // 1 => 3x3 PCF, 2 => 5x5 PCF
    const float MIN_BIAS = 0.00002;
    const float MAX_BIAS = 0.0002;

    vec3 proj = lightSpacePos.xyz / lightSpacePos.w;

	// --- RECEIVER OFFSET IN LIGHT SPACE ---
	// This removes the last "floating" without causing acne or swimming
	const float RECEIVER_BIAS = -0.0005;
	proj.z -= RECEIVER_BIAS;
	
	proj = proj * 0.5 + 0.5;

    // Outside shadow map => fully lit
    if (proj.z > 1.0 || proj.x < 0.0 || proj.x > 1.0 || proj.y < 0.0 || proj.y > 1.0)
        return 1.0;

    vec3 Ng = normalize(N_geom);
    vec3 L  = normalize(lightDir);
    float NdotL = clamp(dot(Ng, L), 0.0, 1.0);

    // Small slope bias ONLY (no tan/acos)
    float bias = mix(MAX_BIAS, MIN_BIAS, NdotL);

    vec2 texelSize = 1.0 / vec2(textureSize(shadowMap, 0));

    // Prevent taps going out of bounds (avoids edge artifacts)
    vec2 uv = clamp(proj.xy, texelSize * float(K + 1), vec2(1.0) - texelSize * float(K + 1));

    float currentDepth = proj.z - bias;

    float sum = 0.0;
    int samples = 0;

    for (int x = -K; x <= K; ++x)
    for (int y = -K; y <= K; ++y)
    {
        vec2 offset = vec2(x, y) * texelSize;
        float closestDepth = texture(shadowMap, uv + offset).r;

        // HARD compare (no smoothstep) => stops “light bleeding”
        sum += (currentDepth > closestDepth) ? 0.0 : 1.0;
        samples++;
    }

    float shadow = sum / float(samples);

    // Apply your strength in a clean way:
    shadow = mix(1.0, shadow, shadowStrength);

    return shadow;
}


// =============================================================
// PBR Light Contribution
// =============================================================
vec3 computeLightContribution(
    Light light,
    vec3 fragPos,
    vec3 N,
    vec3 V,
    float metallic,
    float roughness,
    float ao,
    vec3 baseColor)
{
    float dist = length(light.position - fragPos);
    if (dist > light.distance) return vec3(0.0);

    float atten = 1.0 /
        (light.attenuation.x +
         light.attenuation.y * dist +
         light.attenuation.z * dist * dist);

    vec3 L = normalize(light.position - fragPos);
    vec3 H = normalize(V + L);
    vec3 radiance = light.color * atten;

    // PBR
    float NDF = DistributionGGX(N, H, roughness);
    float G   = GeometrySmith(N, V, L, roughness);
    vec3  F   = fresnelSchlick(max(dot(H, V), 0.0), mix(vec3(0.04), baseColor, metallic));

    vec3 numerator = NDF * G * F;
    float NdotV = max(dot(N, V), 0.00001);
    float NdotL = max(dot(N, L), 0.00001);
    float denominator = 4.0 * NdotV * NdotL + 0.00001;

    vec3 specular = numerator / denominator;

    vec3 kS = F;
    vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);

    return (kD * baseColor / PI + specular) * radiance * NdotL;
}



// =============================================================
// MAIN - Improved Blending
// =============================================================
void main()
{
    vec3 V = normalize(cameraPos - fs_in.worldPos);
    
    vec3 N_geom = normalize(fs_in.normal);
    vec3 N_smoothed = normalize(mix(N_geom, vec3(0.0, 1.0, 0.0), terrainShadowStrength));
	N_geom = N_smoothed; // Use this smoothed version everywhere

    // ----------------------------
    // IMPROVED: Blend weight computation
    // ----------------------------
    vec4 rawBlend = texture(blendMap, fs_in.uv);
    vec4 blend = computeBlendWeights(rawBlend);
    
    // Apply distance-based blend smoothing to reduce seams
    float blendSmoothing = 0.1; // Adjust as needed
    blend = mix(blend, vec4(0.25), blendSmoothing * (1.0 - max(blend.r, max(blend.g, max(blend.b, blend.a)))));
    
    // Final normalization to ensure weights sum to 1
    float sum = blend.r + blend.g + blend.b + blend.a;
    if (sum > 0.0) {
        blend /= sum;
    } else {
        blend = vec4(0.0, 0.0, 0.0, 1.0); // Fallback to base layer
    }

    // ----------------------------
    // Texture coordinates
    // ----------------------------
    vec2 uv0 = fs_in.baseUV * tileScale0;
    vec2 uv1 = fs_in.baseUV * tileScale1;
    vec2 uv2 = fs_in.baseUV * tileScale2;
    vec2 uv3 = fs_in.baseUV * tileScale3;

    // ----------------------------
    // Albedo blend - SIMPLIFIED (using texture directly)
    // ----------------------------
    vec3 albedo0 = texture(layer0, uv0).rgb;
    vec3 albedo1 = texture(layer1, uv1).rgb;
    vec3 albedo2 = texture(layer2, uv2).rgb;
    vec3 albedo3 = texture(layer3, uv3).rgb;
    
    // Debug: Check if textures are loading correctly
    // Uncomment to see each layer individually
    /*
    if (blend.r > 0.5) {
        fragColor = vec4(albedo0, 1.0);
        return;
    } else if (blend.g > 0.5) {
        fragColor = vec4(albedo1, 1.0);
        return;
    } else if (blend.b > 0.5) {
        fragColor = vec4(albedo2, 1.0);
        return;
    } else {
        fragColor = vec4(albedo3, 1.0);
        return;
    }
    */
    
    vec3 albedo = albedo0 * blend.r + albedo1 * blend.g + 
                  albedo2 * blend.b + albedo3 * blend.a;

    // ----------------------------
    // Improved Normal blending
    // ----------------------------
    vec3 n0 = computeLayerNormal(N_geom, fs_in.tangent, uv0, layer0Normal, hasNormal0);
    vec3 n1 = computeLayerNormal(N_geom, fs_in.tangent, uv1, layer1Normal, hasNormal1);
    vec3 n2 = computeLayerNormal(N_geom, fs_in.tangent, uv2, layer2Normal, hasNormal2);
    vec3 n3 = computeLayerNormal(N_geom, fs_in.tangent, uv3, layer3Normal, hasNormal3);
    
    // Start with base normal and blend in layers
    vec3 N = n3; // Start with base layer
    N = blendNormals(N, n0, blend.r);
    N = blendNormals(N, n1, blend.g);
    N = blendNormals(N, n2, blend.b);
    
    // Ensure normal is normalized
    N = normalize(N);

    // ----------------------------
    // Material properties blend
    // ----------------------------
    float metallic = 0.0;
    float roughness = 0.0;  // Start with 1.0 (fully rough) for layers without roughness maps
    float ao = 0.0;         // Start with 1.0 (no occlusion) for layers without AO maps
    
    float metaillicFactor = 0.3f;
    // Layer 0
    if (hasMetallic0) metallic += texture(layer0Metallic, uv0).r * blend.r * metaillicFactor;
    if (hasRoughness0) roughness += (texture(layer0Roughness, uv0).r - 1.0) * blend.r;
    if (hasAo0) ao += (texture(layer0Ao, uv0).r - 1.0) * blend.r;
    
    // Layer 1
    if (hasMetallic1) metallic += texture(layer1Metallic, uv1).r * blend.g * metaillicFactor;
    if (hasRoughness1) roughness += (texture(layer1Roughness, uv1).r - 1.0) * blend.g;
    if (hasAo1) ao += (texture(layer1Ao, uv1).r - 1.0) * blend.g;
    
    // Layer 2
    if (hasMetallic2) metallic += texture(layer2Metallic, uv2).r * blend.b * metaillicFactor;
    if (hasRoughness2) roughness += (texture(layer2Roughness, uv2).r - 1.0) * blend.b;
    if (hasAo2) ao += (texture(layer2Ao, uv2).r - 1.0) * blend.b;
    
    // Layer 3 (base)
    if (hasMetallic3) metallic += texture(layer3Metallic, uv3).r * blend.a * metaillicFactor;
    if (hasRoughness3) roughness += (texture(layer3Roughness, uv3).r - 1.0) * blend.a;
    if (hasAo3) ao += (texture(layer3Ao, uv3).r - 1.0) * blend.a;
    
    // Clamp values
    metallic = clamp(metallic, 0.0, 1.0);
    roughness = clamp(roughness, 0.01, 1.0);
    ao = clamp(ao, 0.0, 1.0);

    // Debug: Output albedo only (uncomment to check textures)
    // fragColor = vec4(albedo, 1.0);
    // return;

    // ----------------------------
    // Lighting
    // ----------------------------
    vec3 Lsun = normalize(lights[0].position - fs_in.worldPos);
	float shadow = computeShadow(fs_in.lightSpacePos, N_geom, Lsun);

    vec3 lighting = vec3(0.0);
    for (int i = 0; i < numLights; i++) {
        lighting += computeLightContribution(
            lights[i],
            fs_in.worldPos,
            N,
            V,
            metallic,
            roughness,
            ao,
            albedo
        );
    }

    // ----------------------------
    // Final color composition
    // ----------------------------
    vec3 ambient = albedo * ao * ambientStrength;
    vec3 directLighting = lighting * shadow;
    
    // Soft shadow ambient boost
    float shadowAmbientBoost = 0.3 * (1.0 - shadow) * ao;
    vec3 shadowAmbient = albedo * shadowAmbientBoost * ambientStrength;

    vec3 finalColor = ambient + shadowAmbient + directLighting;
    finalColor = max(finalColor, vec3(0.0));

    // Rim lighting for edge definition
    float grazingFactor = 1.0 - max(dot(N, V), 0.0);
    float grazingBoost = grazingFactor * grazingFactor * 0.1 * ambientStrength;
    finalColor += albedo * grazingBoost;

    //-------------------------------------------------------------
	// FOG APPLICATION
	//-------------------------------------------------------------
	float fogDistance = length(cameraPos - fs_in.worldPos);
	
	// exponential fog model
	float fogFactor = 1.0 - exp(-pow(fogDistance * density, gradient));
	fogFactor = clamp(fogFactor, 0.0, 1.0);
	
	// mix final terrain color with fog color
	vec3 foggedColor = mix(finalColor, fogColor, fogFactor);
	
	// output
	fragColor = vec4(foggedColor, 1.0);
}