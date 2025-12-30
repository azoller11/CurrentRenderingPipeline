#version 400 core

// -----------------------------------------------------------------------------
// Structs & Uniforms
// -----------------------------------------------------------------------------
struct Light {
    vec3 position;    // For point lights, this is the light position.
    vec3 color;
    vec3 attenuation; // (constant, linear, quadratic)
    float distance;
};

uniform int numLights;
uniform Light lights[16];

uniform vec3 cameraPos;

// Parallax
uniform float parallaxScale;
uniform float minLayers; 
uniform float maxLayers;

// Base textures
uniform sampler2D diffuseTexture;
uniform sampler2D normalMap;
uniform sampler2D heightMap;   // for parallax

// PBR textures
uniform sampler2D metallicMap; 
uniform sampler2D roughnessMap;
uniform sampler2D aoMap;

uniform int hasNormal;      // 1 if normalMap is bound, 0 if missing
uniform int hasHeight;      // 1 if heightMap is bound, 0 if missing
uniform int hasMetallic;    // 1 if metallicMap is bound, 0 if missing
uniform int hasRoughness;   // 1 if roughnessMap is bound, 0 if missing
uniform int hasAo;          // 1 if aoMap is bound, 0 if missing



uniform int isVegitation;

uniform int useFakeLighting;

//uniform int isVegitation;

// Shadow mapping uniforms
uniform sampler2D shadowMap;
uniform mat4 lightSpaceMatrix;
uniform vec3 directionalLightDir;

// New uniforms for specular lighting (used when no metallic map is provided)
uniform float shineDamper = 0;
uniform float reflectivity = 0;

uniform float outsideAmbience;

uniform vec3 vegitationColorAdjust = vec3(1.0, 1.0, 1.0);  // RGB multiplier for vegetation
uniform float vegitationAmbiance = 0.5;  // Additional ambient boost for vegetation
uniform float vegitationLightReduction = 0.0f;
uniform float vegitationShadowBrightness = 1.0f;


const float PI = 3.14159265359;

// Debug uniform
uniform int debugMode;

// New Uniform for Opaque Pass
uniform int isOpaquePass;

uniform float exposure = 2.2;           // global brightness
uniform float ambientBoost = 1.4;       // boosts sky ambient
uniform float lightBoost = 3.0;         // boosts artificial lights
uniform float shadowBrightness = 0.35;  // brightness in shadowed regions
uniform float minBrightness = 0.12; 

uniform float density  = 0.0000;         
uniform float gradient  = 0.001;
uniform vec3 fogColor = vec3(0.25f, 0.25f, 0.25f);

uniform float fakeLightingStrength = 1.0f;

// -----------------------------------------------------------------------------
// Inputs and Outputs
// -----------------------------------------------------------------------------
in GS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
    vec3 wBitangent;
} fs_in;

out vec4 outColor;

// -----------------------------------------------------------------------------
// Function Declarations
// -----------------------------------------------------------------------------
vec3 fresnelSchlick(float cosTheta, vec3 F0);
vec3 computeNormal(vec3 worldNormal, vec3 worldTangent, vec2 uv, out mat3 TBN);
vec3 computeLightContribution(Light light, vec3 fragPos, vec3 N, vec3 V, float metallic, float roughness, float ao, vec3 baseColor);
vec2 parallaxMapping(vec2 texCoords, vec3 viewDirTangent);
float calculatePOMShadow(vec3 lightDirTangent, vec2 initialUV, float lightDistance, float maxLightDistance);
float calculatedDirectionalShadows();



// Trowbridge-Reitz GGX normal distribution function.
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a = roughness * roughness;
    float a2 = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2 = NdotH * NdotH;
    
    float num = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom = PI * denom * denom;
    
    return num / max(denom, 0.0001);
}

// Schlick-GGX geometry function for a single direction.
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r = (roughness + 1.0);
    float k = (r * r) / 8.0;
    
    float num = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    return num / max(denom, 0.0001);
}

// Smith's method for combined geometry term.
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx2 = GeometrySchlickGGX(NdotV, roughness);
    float ggx1 = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

float computeMetallic(vec2 uv)
{
     if (hasMetallic == 1) {
        return clamp(texture(metallicMap, uv).r, 0.0, 1.0);
    }

    // Missing metallic → dielectric default
    return 0.0;
}


float computeRoughness(vec2 uv)
{
    if (hasRoughness == 1) {
        return clamp(texture(roughnessMap, uv).r, 0.04, 1.0);
    }

    // Missing roughness → matte surface
    return 0.85;
}

// -----------------------------------------------------------------------------
// Vegitation Lighting Function
// -----------------------------------------------------------------------------
vec3 computeVegitationLighting(vec3 baseColor, float alpha, vec3 worldPos, vec3 worldNormal)
{
    // ---------------------------------------------------------------------
    // COLOR ADJUST (HUE ONLY, NO BRIGHTNESS CHANGE)
    // ---------------------------------------------------------------------
    vec3 colorAdjusted = baseColor * vegitationColorAdjust;
    float luminance = dot(baseColor, vec3(0.2126, 0.7152, 0.0722));
    colorAdjusted = normalize(colorAdjusted + 0.0001) * luminance;

    // ---------------------------------------------------------------------
    // GEOMETRY
    // ---------------------------------------------------------------------
    vec3 N = vec3(0.0,0.75,0.0);
    vec3 V = normalize(cameraPos - worldPos);

    // ---------------------------------------------------------------------
    // AMBIENT (UNAFFECTED BY LIGHT REDUCTION OR SHADOWS)
    // ---------------------------------------------------------------------
    float baseAmbientFloor = 0.12;
    float skyAmbient = mix(0.08, 0.55, clamp(outsideAmbience, 0.0, 1.0));
    skyAmbient *= ambientBoost;

    vec3 ambient = (baseAmbientFloor + skyAmbient) * colorAdjusted;
    vec3 lighting = ambient;

    // ---------------------------------------------------------------------
    // DIRECTIONAL LIGHT (SHADOW-CONTROLLED)
    // ---------------------------------------------------------------------
    vec3 Ls = normalize(-directionalLightDir);
    float NdotL = abs(dot(N, Ls));

    if (NdotL > 0.0)
    {
        float shadowFactor = calculatedDirectionalShadows();

        // Shadow control ONLY here
        float shadowTerm = mix(
		    vegitationShadowBrightness,
		    1.0,
		    smoothstep(0.6, 0.95, shadowFactor)
		);

        vec3 sunColor = vec3(1.0, 0.95, 0.9);
        vec3 diffuse =
            NdotL *
            sunColor *
            colorAdjusted *
            vegitationLightReduction *
            shadowTerm *
            lightBoost;

        lighting += diffuse;

        // ---- Soft vegetation specular (optional)
        if (shineDamper > 0.0 && reflectivity > 0.0)
        {
            vec3 H = normalize(V + Ls);
            float spec = pow(max(dot(N, H), 0.0), shineDamper * 0.1);
            lighting += spec * reflectivity * sunColor * 0.1;
        }
    }

    // ---------------------------------------------------------------------
    // POINT LIGHTS (LIGHT REDUCTION ONLY)
    // ---------------------------------------------------------------------
    for (int i = 0; i < numLights; i++)
    {
        vec3 lightVec = lights[i].position - worldPos;
        float dist = length(lightVec);

        if (dist > lights[i].distance || length(lights[i].color) < 0.001)
            continue;

        vec3 L = normalize(lightVec);

        float att =
            1.0 /
            (lights[i].attenuation.x +
             lights[i].attenuation.y * dist +
             lights[i].attenuation.z * dist * dist);

        float diff = max(dot(N, L), 0.0);

        lighting +=
            diff *
            lights[i].color *
            colorAdjusted *
            att *
            vegitationLightReduction *
            lightBoost;
    }

    // ---------------------------------------------------------------------
    // SUBSURFACE SCATTERING (UNAFFECTED BY SHADOWS)
    // ---------------------------------------------------------------------
    float sss = max(dot(-Ls, V), 0.0) * 0.1;
    lighting += colorAdjusted * vec3(0.8, 1.0, 0.6) * sss;

    // ---------------------------------------------------------------------
    // FINAL VEGETATION AMBIANCE (GLOBAL BRIGHTNESS ONLY)
    // ---------------------------------------------------------------------
    lighting *= (1.0 + vegitationAmbiance);

    return lighting;
}
// -----------------------------------------------------------------------------
// MAIN
// -----------------------------------------------------------------------------
void main()
{


    // --- Compute TBN manually.
    vec3 N = normalize(fs_in.wNormal);
    vec3 T = normalize(fs_in.wTangent);
    vec3 B = normalize(cross(N, T)); // Right-handed tangent space
    mat3 TBN = mat3(T, B, N);

    float distanceToCamera = length(cameraPos - fs_in.wPosition);

    // View direction (world + tangent)
    vec3 V = normalize(cameraPos - fs_in.wPosition);
    vec3 viewDirTangent = normalize(transpose(TBN) * V);

    // Parallax mapping (only when close enough)
    vec2 parallaxedUV = fs_in.uv;
    if (distanceToCamera < 3000.0 && hasHeight == 1) {
        parallaxedUV = parallaxMapping(fs_in.uv, viewDirTangent);
    }

    // Normal map / TBN-based normal
    vec3 normal = computeNormal(fs_in.wNormal, fs_in.wTangent, parallaxedUV, TBN);

    // Pass filter
    if (isOpaquePass < 0)
        return;

    // Base color / alpha
    vec4 texColor = texture(diffuseTexture, parallaxedUV);

    // Only discard in transparent pass, keep opaque geometry solid
    if (isOpaquePass != 1 && texColor.a < 0.1)
        discard;

    vec3 baseColor = texColor.rgb;
    
    // -----------------------------------------------------------------------------
	// VEGITATION MODE
	// -----------------------------------------------------------------------------
	      if (isVegitation == 1) {
        // Get texture color using original UV (not parallaxed)
        vec4 texColor = texture(diffuseTexture, fs_in.uv);
        
        // CRITICAL: Distance-based alpha threshold to fix black edges at distance
        float distance = length(cameraPos - fs_in.wPosition);
        float distanceFactor = clamp(distance / 100.0, 0.0, 1.0);
        float alphaThreshold = mix(0.15, 0.4, distanceFactor); // Higher threshold at distance
        
        if (texColor.a < alphaThreshold) {
            discard;
        }
        
        // Use vegetation lighting with all lights
        vec3 vegLighting = computeVegitationLighting(
            texColor.rgb, 
            texColor.a, 
            fs_in.wPosition, 
            fs_in.wNormal
        );
        
        // Apply exposure (important for brightness matching)
        vec3 exposed = vec3(1.0) - exp(-vegLighting * exposure);
        
        // Micro-contrast tweak (same as main shader)
        exposed = pow(exposed, vec3(0.92));
        
        // Lighter fog for vegetation to prevent darkening at distance
        float fogDistance = distance;
        float vegDensity = density * 0.3; // Much less fog for vegetation
        float fogFactor = 1.0 - exp(-pow(fogDistance * vegDensity, gradient));
        fogFactor = clamp(fogFactor, 0.0, 0.5); // Max 50% fog
        
        // Brighter fog for vegetation
        vec3 vegFogColor = fogColor * 1.5;
        vec3 fogged = mix(exposed, vegFogColor, fogFactor);
        
        // Output with texture alpha
        outColor = vec4(fogged, 1.0);
        return; // Early exit for vegetation
    }

    // --- PBR maps with sane clamps
    float metallic = computeMetallic(parallaxedUV);
    float roughness = computeRoughness(parallaxedUV);
    float ao = (hasAo == 1)? texture(aoMap, parallaxedUV).r: 1.0;

    metallic  = clamp(metallic,  0.0, 1.0);
    roughness = clamp(roughness, 0.04, 1.0);   // avoid 0 roughness fireflies
    ao        = clamp(ao,        0.0, 1.0);

    // -------------------------------------------------------------------------
    // AMBIENT LIGHTING
    // -------------------------------------------------------------------------
    //
    // outsideAmbience is assumed 0 (night) -> 1 (mid-day).
    // We use it to smoothly move between a low and medium ambient, plus a small
    // base floor so shadows never go pure black.
    //
    float baseAmbientFloor = minBrightness; // was 0.03
	float skyAmbientMin    = 0.08;
	float skyAmbientMax    = 0.55; // MUCH brighter sky influence
	
	float skyAmbient = mix(skyAmbientMin, skyAmbientMax, clamp(outsideAmbience, 0.0, 1.0));
	//skyAmbient *= ambientBoost;
	
	float ambientStrength = baseAmbientFloor;
	
	// ambient lighting
	vec3 lighting = ambientStrength * baseColor * ao;

    // -------------------------------------------------------------------------
    // DIRECTIONAL SHADOW FROM SHADOW MAP
    // -------------------------------------------------------------------------
    float dirShadowFactor = calculatedDirectionalShadows(); // 0..1 (1 = fully lit)
    dirShadowFactor = clamp(dirShadowFactor, 0.0, 1.0);

    // We don't apply this directly yet; we combine with POM shadow per-light.

    // -------------------------------------------------------------------------
    // DIRECT LIGHTING LOOP
    // -------------------------------------------------------------------------

    for (int i = 0; i < numLights; i++) {
        // Skip lights with negligible contribution.
        if (length(lights[i].color) < 0.001)
            continue;

        // Vector from fragment to light.
        vec3 lightVec = lights[i].position - fs_in.wPosition;
        float distance = length(lightVec);

        // Skip outside light range.
        if (distance > lights[i].distance)
            continue;

        vec3 L = normalize(lightVec);

        // Tangent-space light direction for POM self-shadow
        vec3 lightDirTangent = normalize(transpose(TBN) * L);

        // Parallax self-shadow (0..1)
        float pomShadow = calculatePOMShadow(lightDirTangent, parallaxedUV,
                                             distance, lights[i].distance);
        pomShadow = clamp(pomShadow, 0.0, 1.0);

        // Directional shadow map only applies to your "sun/moon" light,
        // assuming that is lights[0].
        float dirShadow = (i == 0) ? dirShadowFactor : 1.0;

        // Combine self-shadow + directional shadow (no double-darkness)
		float shadowRaw = min(pomShadow, dirShadow);
		
		// Keep a minimum visible lighting in shadows
		float finalShadow = mix(shadowBrightness, 1.0, shadowRaw);

        // PBR direct light contribution (already does attenuation inside)
        vec3 direct = computeLightContribution(
            lights[i],
            fs_in.wPosition,
            normal,
            V,
            metallic,
            roughness,
            ao,
            baseColor
        );

       // Optional extra Blinn-Phong spec for non-PBR materials
		if (hasMetallic == 0 && reflectivity > 0.0 && shineDamper > 0.0) {
		    vec3 H = normalize(V + L);
		    float specAngle = max(dot(normal, H), 0.0);
		    float spec = pow(specAngle, shineDamper);
		    direct += spec * reflectivity * lights[i].color;
		}
		
		// NOW boost the whole light contribution, equally for PBR and non-PBR
		direct *= lightBoost;
		
		lighting += finalShadow * direct;
    }

    // -------------------------------------------------------------------------
    // FINAL COLOR
    // -------------------------------------------------------------------------
    // For now, output in linear space; you can Gamma-correct in post or here.
    vec3 finalColor = lighting;

    // If you want alpha to matter in transparent pass, you can do:
    // outColor = vec4(finalColor, (isOpaquePass == 1) ? 1.0 : texColor.a);
   // HDR → LDR tone mapping
	vec3 hdr = lighting;

	// exposure mapping
	vec3 mapped = vec3(1.0) - exp(-hdr * exposure);
	
	// micro-contrast tweak
	mapped = pow(mapped, vec3(0.92));
	
	float fogDistance = distanceToCamera;
	
	// exponential fog
	float fogFactor = 1.0 - exp(-pow(fogDistance * density, gradient));
	fogFactor = clamp(fogFactor, 0.0, 1.0);
	
	// apply fog (fogColor comes from uniform)
	vec3 fogged = mix(mapped, fogColor, fogFactor);
	
	// output final
	outColor = vec4(fogged, 1.0);
}