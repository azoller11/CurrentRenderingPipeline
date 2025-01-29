#version 400 core

in GS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
} fs_in;

out vec4 outColor;

// -----------------------------------------------------------------------------
// Structs & Uniforms
// -----------------------------------------------------------------------------
struct Light {
    vec3 position;    // If attenuation == (0,0,0), interpret as directional
    vec3 color;
    vec3 attenuation; // (const, linear, quad)
};

uniform int numLights;
uniform Light lights[16];

uniform vec3 cameraPos;

// Parallax
uniform float parallaxScale = 0.14;
uniform int minLayers = 120; 
uniform int maxLayers = 160;

// Base textures
uniform sampler2D diffuseTexture;
uniform sampler2D normalMap;
uniform sampler2D heightMap;   // for parallax

// PBR textures
uniform sampler2D metallicMap; 
uniform sampler2D roughnessMap;
uniform sampler2D aoMap;

// Whether each PBR map is present
uniform int hasMetallic;  // 1 if metallicMap is bound, 0 if missing
uniform int hasRoughness; // 1 if roughnessMap is bound, 0 if missing
uniform int hasAo;        // 1 if aoMap is bound, 0 if missing

// Old fallback uniforms
uniform float reflectivity; // "specular intensity"
uniform float shineDamper;  // "specular exponent"

const float PI = 3.14159265359;


//Debug uniform
uniform int debugMode;

// -----------------------------------------------------------------------------
// Parallax Occlusion
// -----------------------------------------------------------------------------
vec2 parallaxOcclusionMapping(vec2 baseUV, vec3 viewDirTangent) {
    viewDirTangent.y = -viewDirTangent.y;
    float viewAngle = dot(vec3(0,0,1), viewDirTangent);
    int numSteps = int(mix(float(maxLayers), float(minLayers), viewAngle));

    vec2 deltaUV = (parallaxScale * viewDirTangent.xy) / float(numSteps);
    vec2 currUV  = baseUV;

    float layerDepth = 0.0;
    float layerStep  = 1.0 / float(numSteps);

    for(int i=0; i<numSteps; i++){
        float height = texture(heightMap, currUV).r;
        if(layerDepth > height) {
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

// PBR lighting
vec3 pbrLight(
    vec3 N, vec3 V, 
    vec3 baseColor, 
    float metallic, 
    float roughness, 
    vec3 F0,
    Light L
) {
    // Light direction + attenuation
    vec3 lightDir;
    float denom = 1.0;

    bool isDirectional = all(lessThanEqual(L.attenuation, vec3(0.000001)));
    vec3 fragPos = fs_in.wPosition;

    if(isDirectional) {
        lightDir = normalize(-L.position);
    } else {
        vec3 toLight = L.position - fragPos;
        float dist   = length(toLight);
        lightDir     = normalize(toLight);

        denom = L.attenuation.x + L.attenuation.y * dist +
                L.attenuation.z * (dist*dist);
        if(denom < 0.0001) denom=0.0001;
    }

    float NdotL = max(dot(N, lightDir), 0.0);
    if(NdotL <= 0.0) return vec3(0.0);

    // Cook–Torrance
    vec3 H = normalize(V + lightDir);
    float NdotV = max(dot(N, V), 0.0);
    float NdotH = max(dot(N, H), 0.0);
    float VdotH = max(dot(V, H), 0.0);

    float alpha    = roughness * roughness;
    float alphaSqr = alpha * alpha;
    float denomD   = (NdotH*NdotH)*(alphaSqr - 1.0) + 1.0;
    float D        = alphaSqr / (PI * denomD * denomD);

    float k  = (alpha + 1.0)*(alpha + 1.0) / 8.0;
    float G1V= NdotV / (NdotV*(1.0 - k) + k);
    float G1L= NdotL / (NdotL*(1.0 - k) + k);
    float G  = G1V * G1L;

    vec3 F = fresnelSchlick(VdotH, F0);

    float denomSpec = 4.0 * NdotV * NdotL + 0.0001;
    vec3 specular   = (D * G * F) / denomSpec;

    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    kD *= (1.0 - metallic);

    vec3 diffuse = (kD * baseColor) / PI;
    vec3 radiance= L.color / denom;

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
){
    // Light direction + attenuation
    vec3 lightDir;
    float denom = 1.0;
    bool isDirectional = all(lessThanEqual(L.attenuation, vec3(0.000001)));
    vec3 fragPos = fs_in.wPosition;

    if(isDirectional) {
        lightDir = normalize(-L.position);
    } else {
        vec3 toLight = L.position - fragPos;
        float dist   = length(toLight);
        lightDir     = normalize(toLight);

        denom = L.attenuation.x + L.attenuation.y * dist 
                + L.attenuation.z * dist*dist;
        if(denom<0.0001) denom=0.0001;
    }

    float NdotL = max(dot(N, lightDir), 0.0);
    if(NdotL<=0.0) return vec3(0.0);

    // Diffuse (Lambert)
    vec3 diff = baseColor * (NdotL/denom) * L.color;

    // Phong Spec
    vec3 R = reflect(-lightDir, N);
    float specFactor = pow(max(dot(R, V), 0.0), shineDamper) * reflectivity;
    vec3 spec = (specFactor / denom)* L.color;

    return diff + spec;
}

// -----------------------------------------------------------------------------
// MAIN
// -----------------------------------------------------------------------------
void main() {
    // 1) TBN
    vec3 N = normalize(fs_in.wNormal);
    vec3 T = normalize(fs_in.wTangent);
    vec3 B = normalize(cross(N, T));
    mat3 TBN = mat3(T, B, N);

    // 2) View vector
    vec3 Vworld = normalize(cameraPos - fs_in.wPosition);
    vec3 Vtangent= normalize(TBN * Vworld);

    // 3) Parallax
    vec2 displacedUV = parallaxOcclusionMapping(fs_in.uv, Vtangent);

    // 4) Normal map
    vec3 mapNormal = texture(normalMap, displacedUV).rgb;
    mapNormal = normalize(mapNormal * 2.0 - 1.0);
    vec3 Nworld  = normalize(TBN * mapNormal);

    // 5) Base color
    vec3 baseColor = texture(diffuseTexture, displacedUV).rgb;

    // 6) Are any PBR maps present?
    //    If no PBR maps at all => fallback
    bool hasAnyPBR = (hasMetallic==1 || hasRoughness==1 || hasAo==1);

    // 7) Prepare PBR or fallback values
    float metallic  = 0.0;
    float roughness = 0.5;
    float ao        = 1.0;

    if (hasAnyPBR) {
        // If we have metallic, read it, else default = 0
        if (hasMetallic==1) {
            metallic = texture(metallicMap, displacedUV).r;
            metallic = clamp(metallic, 0.17, 1.0);
        }
        // If we have roughness, read it, else default = 0.5
        if (hasRoughness==1) {
            roughness = texture(roughnessMap, displacedUV).r;
            roughness = clamp(roughness, 0.17, 1.0);
        }
        // If we have AO, read it, else default=1
        if (hasAo==1) {
            ao = texture(aoMap, displacedUV).r;
            ao = clamp(ao, 0.17, 1.0);
        }
    }

    // 8) Summation
    vec3 V  = normalize(Vworld);
    vec3 finalColor = vec3(0.0);

    if (!hasAnyPBR) {
        // old fallback
        for(int i=0; i<numLights; i++){
            finalColor += fallbackLight(Nworld, V, baseColor, reflectivity, shineDamper, lights[i]);
        }
    }
    else {
        // partial or full PBR
        for(int i=0; i<numLights; i++){
            // F0 = mix(0.04, baseColor) by metallic
            vec3 F0 = mix(vec3(0.04), baseColor, metallic);
            finalColor += pbrLight(Nworld, V, baseColor, metallic, roughness, F0, lights[i]);
        }
        // multiply by AO if we have it
        if (hasAo==1) {
            finalColor *= ao;
        }
    }
    

    outColor = vec4(finalColor, 1.0);
    
    
     // Debug Mode Handling
    switch (debugMode) {
        case 1: // Visualize World Normals
            outColor = vec4((Nworld * 0.5) + 0.5, 1.0);
            break;
        case 2: // Visualize Tangent Space Normals
            outColor = vec4((mapNormal * 0.5) + 0.5, 1.0);
            break;
        case 3: // Visualize UV Coordinates
            outColor = vec4(displacedUV, 0.0, 1.0);
            break;
        case 4: // Visualize Depth
            float depth = gl_FragCoord.z / gl_FragCoord.w;
            outColor = vec4(vec3(depth), 1.0);
            break;
        case 5: // Visualize Material Properties (Metallic)
            outColor = vec4(vec3(texture(metallicMap, displacedUV).r), 1.0);
            break;
        case 6: // Visualize Surface Curvature
            float curvature = dot(normalize(Nworld), vec3(0.0, 0.0, 1.0));
            outColor = vec4(curvature, curvature, curvature, 1.0);
            break;
        case 7: // Visualize View Vector
            float viewIntensity = dot(normalize(Vworld), normalize(Nworld));
            outColor = vec4(vec3(viewIntensity), 1.0);
            break;
        case 8: // Heat Maps for Performance (placeholder for computational intensity)
            float computeIntensity = 0.5; // Placeholder: replace with actual computation
            outColor = vec4(computeIntensity, 0.0, 1.0 - computeIntensity, 1.0);
            break;
        default:
            break;
    }
}
