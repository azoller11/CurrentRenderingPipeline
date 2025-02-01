#version 400 core

// -----------------------------------------------------------------------------
// Structs & Uniforms
// -----------------------------------------------------------------------------
struct Light {
    vec3 position;    // For point lights, this is the light position.
    vec3 color;
    vec3 attenuation; // (const, linear, quad)
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

// Old fallback uniforms
uniform float reflectivity; // "specular intensity"
uniform float shineDamper;  // "specular exponent"

const float PI = 3.14159265359;

// Debug uniform
uniform int debugMode;

// New Uniform for Opaque Pass
uniform int isOpaquePass;

// Shadow uniforms
const int NUM_SAMPLES = 20;
vec3 sampleOffsetDirections[NUM_SAMPLES] = vec3[](
    vec3( 1,  1,  1), vec3(-1,  1,  1), vec3( 1, -1,  1), vec3(-1, -1,  1),
    vec3( 1,  1, -1), vec3(-1,  1, -1), vec3( 1, -1, -1), vec3(-1, -1, -1),
    vec3( 1,  0,  0), vec3(-1,  0,  0), vec3( 0,  1,  0), vec3( 0, -1,  0),
    vec3( 0,  0,  1), vec3( 0,  0, -1), vec3( 1,  1,  0), vec3(-1,  1,  0),
    vec3( 1, -1,  0), vec3(-1, -1,  0), vec3( 0,  1,  1), vec3( 0, -1, -1)
);
uniform samplerCube shadowCubeMaps[16];
uniform float shadowFarPlane = 1000;

// -----------------------------------------------------------------------------
// Cube Shadow Mapping Function (PCF for Point Lights)
// -----------------------------------------------------------------------------
float calculatePointLightShadow(int lightIndex, vec3 fragPos) {
    // Compute vector from light to fragment
    vec3 fragToLight = fragPos - lights[lightIndex].position;
    float currentDistance = length(fragToLight);
    float bias = 0.05; // Adjust to taste
    float shadow = 0.0;
    int samples = NUM_SAMPLES;
    
    // Compute a sampling disk radius based on view distance
    float viewDistance = length(cameraPos - fragPos);
    float diskRadius = (1.0 + (viewDistance / shadowFarPlane)) / 25.0;
    
    for (int i = 0; i < samples; i++) {
        vec3 sampleVec = fragToLight + sampleOffsetDirections[i] * diskRadius;
        // Sample the cube map depth (stored in [0,1])
        float closestDistance = texture(shadowCubeMaps[lightIndex], sampleVec).r;
        // Scale to actual depth using the far plane
        closestDistance *= shadowFarPlane;
        if (currentDistance - bias > closestDistance) {
            shadow += 1.0;
        }
    }
    shadow /= float(samples);
    return clamp(shadow, 0.0, 1.0);
}

// -----------------------------------------------------------------------------
// Inputs and Outputs
// -----------------------------------------------------------------------------
in GS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} fs_in;

out vec4 outColor;

// -----------------------------------------------------------------------------
// Parallax Occlusion Mapping
// -----------------------------------------------------------------------------
vec2 parallaxOcclusionMapping(vec2 baseUV, vec3 viewDirTangent) {
    viewDirTangent.y = -viewDirTangent.y;
    float viewAngle = dot(vec3(0,0,1), viewDirTangent);
    int numSteps = int(mix(float(maxLayers), float(minLayers), viewAngle));

    vec2 deltaUV = (parallaxScale * viewDirTangent.xy) / float(numSteps);
    vec2 currUV  = baseUV;

    float layerDepth = 0.0;
    float layerStep  = 1.0 / float(numSteps);

    for (int i = 0; i < numSteps; i++) {
        float height = texture(heightMap, currUV).r;
        if (layerDepth > height) {
            break;
        }
        currUV -= deltaUV;
        layerDepth += layerStep;
    }

    return currUV;
}

// -----------------------------------------------------------------------------
// Cook–Torrance Helpers
// -----------------------------------------------------------------------------
vec3 fresnelSchlick(float cosTheta, vec3 F0) {
    return F0 + (1.0 - F0) * pow(1.0 - cosTheta, 5.0);
}

// -----------------------------------------------------------------------------
// PBR Lighting
// -----------------------------------------------------------------------------
vec3 pbrLight(
    vec3 N, vec3 V, 
    vec3 baseColor, 
    float metallic, 
    float roughness, 
    vec3 F0,
    Light L
) {
    // Compute light direction and attenuation (always treated as point light)
    vec3 fragPos = fs_in.wPosition;
    vec3 toLight = L.position - fragPos;
    float dist   = length(toLight);
    vec3 lightDir = normalize(toLight);

    float denom = L.attenuation.x + L.attenuation.y * dist +
                  L.attenuation.z * (dist * dist);
    if (denom < 0.0001) {
        denom = 0.0001;
    }

    float NdotL = max(dot(N, lightDir), 0.0);
    if (NdotL <= 0.0) return vec3(0.0);

    // Cook–Torrance BRDF
    vec3 H = normalize(V + lightDir);
    float NdotV = max(dot(N, V), 0.0);
    float NdotH = max(dot(N, H), 0.0);
    float VdotH = max(dot(V, H), 0.0);

    float alpha    = roughness * roughness;
    float alphaSqr = alpha * alpha;
    float denomD   = (NdotH * NdotH) * (alphaSqr - 1.0) + 1.0;
    float D        = alphaSqr / (PI * denomD * denomD);

    float k  = (alpha + 1.0) * (alpha + 1.0) / 8.0;
    float G1V = NdotV / (NdotV * (1.0 - k) + k);
    float G1L = NdotL / (NdotL * (1.0 - k) + k);
    float G  = G1V * G1L;

    vec3 F = fresnelSchlick(VdotH, F0);

    float denomSpec = 4.0 * NdotV * NdotL + 0.0001;
    vec3 specular   = (D * G * F) / denomSpec;

    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= (1.0 - metallic);

    vec3 diffuse = (kD * baseColor) / PI;
    vec3 radiance = L.color / denom;

    return (diffuse + specular) * radiance * NdotL;
}

// -----------------------------------------------------------------------------
// Fallback Lighting (Lambert + Phong) for missing PBR
// -----------------------------------------------------------------------------
vec3 fallbackLight(
    vec3 N, vec3 V,
    vec3 baseColor,
    float reflectivity,
    float shineDamper,
    Light L
) {
    // Compute light direction and attenuation (always treated as point light)
    vec3 fragPos = fs_in.wPosition;
    vec3 toLight = L.position - fragPos;
    float dist   = length(toLight);
    vec3 lightDir = normalize(toLight);

    float denom = L.attenuation.x + L.attenuation.y * dist + 
                  L.attenuation.z * (dist * dist);
    if (denom < 0.0001) {
        denom = 0.0001;
    }

    float NdotL = max(dot(N, lightDir), 0.0);
    if (NdotL <= 0.0) return vec3(0.0);

    // Diffuse (Lambert)
    vec3 diff = baseColor * (NdotL / denom) * L.color;

    // Phong Specular
    vec3 R = reflect(-lightDir, N);
    float specFactor = pow(max(dot(R, V), 0.0), shineDamper) * reflectivity;
    vec3 spec = (specFactor / denom) * L.color;

    return diff + spec;
}

// -----------------------------------------------------------------------------
// MAIN
// -----------------------------------------------------------------------------
void main() {
    // 1) TBN Matrix
    vec3 N = normalize(fs_in.wNormal);
    vec3 T = normalize(fs_in.wTangent);
    vec3 B = normalize(cross(N, T) * (gl_FrontFacing ? 1.0 : -1.0)); // Flip for correct winding

    mat3 TBN = mat3(T, B, N);

    // 2) View vector in World Space
    vec3 Vworld = normalize(cameraPos - fs_in.wPosition);
    vec3 Vtangent = normalize(TBN * Vworld);

    // 3) Parallax Occlusion Mapping
    vec2 displacedUV;
    if (hasHeight == 1 && parallaxScale > 0.0) {
        displacedUV = parallaxOcclusionMapping(fs_in.uv, Vtangent);
    } else {
        displacedUV = fs_in.uv; // No parallax or height map missing, use original UV
    }

    // 4) Normal Mapping
    vec3 Nworld;
    if (hasNormal == 1) {
        // Sample and transform the normal map
        vec3 mapNormal = texture(normalMap, displacedUV).rgb;
        mapNormal = normalize(mapNormal * 2.0 - 1.0);
        Nworld = normalize(TBN * mapNormal);
    
        // Flip the normal if it's a back face
        if (!gl_FrontFacing) {
            Nworld = -Nworld;
        }
    } else {
        // Use the geometry normal if normal map is missing
        Nworld = gl_FrontFacing ? N : -N;
    }

    // 5) Base Color and Alpha Handling
    vec4 diffuseSample = texture(diffuseTexture, displacedUV);
    vec3 baseColor = diffuseSample.rgb;
    
    float finalAlpha;
    
    if (isOpaquePass == 1) {
        // Opaque Object: Full opacity
        finalAlpha = 1.0;
    } else {
        // Semi-Transparent Object: Use texture's alpha with smoothing
        float alphaThreshold = 0.1;
        float smoothAlpha = smoothstep(alphaThreshold - 0.05, alphaThreshold + 0.05, diffuseSample.a);
        if (smoothAlpha < 0.05) {
            discard;
        }
        finalAlpha = smoothAlpha;
    }

    // 6) Check for PBR Maps
    bool hasAnyPBR = (hasMetallic == 1 || hasRoughness == 1 || hasAo == 1);

    // 7) Prepare PBR or fallback values
    float metallic  = 0.0;
    float roughness = 0.5;
    float ao        = 1.0;

    if (hasAnyPBR) {
        // Metallic
        if (hasMetallic == 1) {
            metallic = texture(metallicMap, displacedUV).r;
            metallic = clamp(metallic, 0.0, 1.0);
        }

        // Roughness
        if (hasRoughness == 1) {
            roughness = texture(roughnessMap, displacedUV).r;
            roughness = clamp(roughness, 0.0, 1.0);
        }

        // Ambient Occlusion
        if (hasAo == 1) {
            ao = texture(aoMap, displacedUV).r;
            ao = clamp(ao, 0.0, 1.0);
        }
    }

    // 8) Lighting Calculation
    vec3 V = normalize(Vworld);
    vec3 finalColor = vec3(0.0);
    
     // Loop over each light
    for (int i = 0; i < numLights; i++) {
        // Compute shadow factor for the current light using cube shadow mapping
        float shadow = calculatePointLightShadow(i, fs_in.wPosition);
        
        if (!hasAnyPBR) {
            //finalColor += fallbackLight(Nworld, V, baseColor, reflectivity, shineDamper, lights[i], shadow);
        } else {
            vec3 F0 = mix(vec3(0.04), baseColor, metallic);
            //finalColor += pbrLight(Nworld, V, baseColor, metallic, roughness, F0, lights[i], shadow);
        }
    }

    if (!hasAnyPBR) {
        // Use fallback lighting model
        for (int i = 0; i < numLights; i++) {
            finalColor += fallbackLight(Nworld, V, baseColor, reflectivity, shineDamper, lights[i]);
        }
    } else {
        // Use PBR lighting model
        for (int i = 0; i < numLights; i++) {
            // Calculate F0 based on metallic property
            vec3 F0 = mix(vec3(0.04), baseColor, metallic);
            finalColor += pbrLight(Nworld, V, baseColor, metallic, roughness, F0, lights[i]);
        }
        // Apply Ambient Occlusion if available
        if (hasAo == 1) {
            finalColor *= ao;
        }
    }

    // 9) Assign the final color with alpha
    outColor = vec4(finalColor, finalAlpha);

    // -----------------------------------------------------------------------------
    // Debug Mode Handling
    // -----------------------------------------------------------------------------
    switch (debugMode) {
        case 1: // Visualize World Normals
            outColor = vec4((Nworld * 0.5) + 0.5, 1.0);
            break;
        case 2: // Visualize Tangent Space Normals
            if (hasNormal == 1) {
                vec3 mapNormal = texture(normalMap, displacedUV).rgb;
                mapNormal = normalize(mapNormal * 2.0 - 1.0);
                outColor = vec4((mapNormal * 0.5) + 0.5, 1.0);
            } else {
                outColor = vec4((N * 0.5) + 0.5, 1.0);
            }
            break;
        case 3: // Visualize UV Coordinates
            outColor = vec4(displacedUV, 0.0, 1.0);
            break;
        case 4: // Visualize Depth
            {
                // Assuming a perspective projection matrix is used
                float depth = gl_FragCoord.z / gl_FragCoord.w;
                outColor = vec4(vec3(depth), 1.0);
            }
            break;
        case 5: // Visualize Material Properties (Metallic)
            if (hasMetallic == 1) {
                outColor = vec4(vec3(texture(metallicMap, displacedUV).r), 1.0);
            } else {
                outColor = vec4(vec3(0.0), 1.0); // Default metallic value
            }
            break;
        case 6: // Visualize Surface Curvature
            {
                // Simple curvature approximation using normal's Y component
                float curvature = abs(Nworld.y);
                outColor = vec4(vec3(curvature), 1.0);
            }
            break;
        case 7: // Visualize View Vector
            {
                float viewIntensity = dot(normalize(Vworld), normalize(Nworld));
                outColor = vec4(vec3(viewIntensity), 1.0);
            }
            break;
        case 8: // Heat Maps for Performance (placeholder for computational intensity)
            {
                // Placeholder: Replace with actual performance metrics if available
                float computeIntensity = 0.5; 
                outColor = vec4(computeIntensity, 0.0, 1.0 - computeIntensity, 1.0);
            }
            break;
        case 9:
            {
                outColor = vec4(fs_in.wPosition * 0.01, 1.0);
            }
            break;
        case 10:
            {
                vec3 lightDirDebug = normalize(lights[0].position - fs_in.wPosition);
                outColor = vec4((lightDirDebug * 0.5) + 0.5, 1.0);
            }
            break;
        default:
            // No debug mode; retain the computed color
            break;
    }
}
