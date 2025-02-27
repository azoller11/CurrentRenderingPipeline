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


// Shadow mapping uniforms
uniform sampler2D shadowMap;
uniform mat4 lightSpaceMatrix;
uniform vec3 directionalLightDir;

// New uniforms for specular lighting (used when no metallic map is provided)
uniform float shineDamper = 0;
uniform float reflectivity = 0;


const float PI = 3.14159265359;

// Debug uniform
uniform int debugMode;

// New Uniform for Opaque Pass
uniform int isOpaquePass;


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
    
    
	// --- Compute view direction in tangent space.
	// Before parallaxMapping call:
	vec3 viewDir = normalize(cameraPos - fs_in.wPosition);
	vec3 viewDirTangent = normalize(transpose(TBN) * viewDir);  // Use transpose instead of direct multiplication
	// --- Compute parallax-adjusted UVs.
	
	
	 vec2 parallaxedUV = fs_in.uv;
    if(distanceToCamera < 3000.0) {
        parallaxedUV = parallaxMapping(fs_in.uv, viewDirTangent);
    }
	
	// --- Compute the normal using the parallaxed UV.
	vec3 normal = computeNormal(fs_in.wNormal, fs_in.wTangent, parallaxedUV, TBN);

	 if (isOpaquePass < 0)
	    return;
	 
    // Sample textures.
    vec4 texColor = texture(diffuseTexture, parallaxedUV);
    //if (texColor.a < 0.1)
    //    discard;
    if (isOpaquePass != 1 && texColor.a < 0.1)
    	discard;  
        
       
        
        
    vec3 baseColor = texColor.rgb;
    
    
    
    // Sample PBR maps.
    float metallic = (hasMetallic == 1) ? texture(metallicMap, parallaxedUV).r : 0.0;
    float roughness = (hasRoughness == 1) ? texture(roughnessMap, parallaxedUV).r : 1.0;
    float ao = (hasAo == 1) ? texture(aoMap, parallaxedUV).r : 1.0;

    // Increase ambient brightness.
  
    
    vec3 ambient = 0.1 * baseColor * ao;  // Increased ambient multiplier.
    
    vec3 lighting = ambient;

     float brightnessFactor = 1.0;
     
      float shadowFactor = calculatedDirectionalShadows();
      
      
     // Loop over each light source.
    for (int i = 0; i < numLights; i++) {
        if (length(lights[i].color) < 0.001) continue;

        // Calculate light direction in world space.
        vec3 lightDir = normalize(lights[i].position - fs_in.wPosition);
        // Compute attenuation based on distance.
        float distance = length(lights[i].position - fs_in.wPosition);
        float attenuation = 1.0 / (lights[i].attenuation.x +
                                   lights[i].attenuation.y * distance +
                                   lights[i].attenuation.z * distance * distance);

        // Calculate light direction in tangent space.
        vec3 lightDirTangent = normalize(transpose(TBN) * lightDir);

        // Calculate shadow factor.
    	float shadow = calculatePOMShadow(lightDirTangent, parallaxedUV, distance, lights[i].distance);
        if (i == 0) {
            shadow *= shadowFactor;
        } else {
            shadow += shadowFactor;
        }
        
        // Base lighting contribution.
       

        // If there's no metallic map, add an extra specular term with attenuation.
        if (hasMetallic == 0 && reflectivity > 0 && shineDamper > 0) {
         	lighting += shadow * brightnessFactor * 
                    computeLightContribution(lights[i], fs_in.wPosition, normal, 
                                              viewDir, metallic, roughness, ao, baseColor) * attenuation;
            vec3 reflectDir = reflect(-lightDir, normal);
            float specAngle = max(dot(viewDir, reflectDir), 0.0);
            float spec = pow(specAngle, shineDamper);
            lighting += spec * reflectivity * lights[i].color * attenuation;
        } else {
         lighting += shadow * brightnessFactor * 
                   computeLightContribution(lights[i], fs_in.wPosition, normal, 
                                          viewDir, metallic, roughness, ao, baseColor);
        
        }
    }
    
    
    
    //This is not working :<
    //Its making a blue outline on transparent objects.
    
	//  if (isOpaquePass == 1) {
	//    // Opaque pass: Ignore the texture alpha.
	//    vec3 finalColor = lighting;
	//    outColor = vec4(finalColor, 1.0);
	//} else {
	//    // Transparent pass: Use premultiplied alpha.
	//    vec3 finalColor = lighting * texColor.a;
	//    outColor = vec4(finalColor, texColor.a);
	//}
		
	vec3 finalColor = lighting;
	    outColor = vec4(finalColor, 1.0);
		


   
}

