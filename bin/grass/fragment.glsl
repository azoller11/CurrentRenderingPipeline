#version 400 core

// -----------------------------------------------------------------------------
// Structures & Uniforms
// -----------------------------------------------------------------------------
struct Light {
    vec3 position;    // For point lights.
    vec3 color;
    vec3 attenuation; // (constant, linear, quadratic)
    float distance;
};

uniform int numLights;
uniform Light lights[16];

// Directional light shadow mapping
uniform sampler2D shadowMap;
uniform mat4 lightSpaceMatrix;
uniform vec3 directionalLightDir;

// Camera & Material
uniform vec3 camPos;
uniform float roughness; 
uniform float metallic;  
uniform float ao;        

// Varyings (from vertex/geometry shader)
flat in float colorFactor;     // Grass color variation factor
flat in vec3 fragNormal;       // World-space normal (or approximate)
flat in vec3 fragPos;          // World-space position

out vec4 fragColor;

// -----------------------------------------------------------------------------
// Constants & Helper Functions
// -----------------------------------------------------------------------------
const float PI = 3.14159265359;

// GGX normal distribution function
float DistributionGGX(vec3 N, vec3 H, float roughness)
{
    float a     = roughness * roughness;
    float a2    = a * a;
    float NdotH = max(dot(N, H), 0.0);
    float NdotH2= NdotH * NdotH;

    float num   = a2;
    float denom = (NdotH2 * (a2 - 1.0) + 1.0);
    denom       = PI * denom * denom;

    return num / max(denom, 0.0001);
}

// Schlick-GGX geometry function (single term)
float GeometrySchlickGGX(float NdotV, float roughness)
{
    float r     = (roughness + 1.0);
    float k     = (r * r) / 8.0;
    float num   = NdotV;
    float denom = NdotV * (1.0 - k) + k;
    return num / max(denom, 0.0001);
}

// Smith's method for combined geometry
float GeometrySmith(vec3 N, vec3 V, vec3 L, float roughness)
{
    float NdotV = max(dot(N, V), 0.0);
    float NdotL = max(dot(N, L), 0.0);
    float ggx1  = GeometrySchlickGGX(NdotV, roughness);
    float ggx2  = GeometrySchlickGGX(NdotL, roughness);
    return ggx1 * ggx2;
}

// Fresnel term
vec3 fresnelSchlick(float cosTheta, vec3 F0)
{
    return F0 + (1.0 - F0) * pow(clamp(1.0 - cosTheta, 0.0, 1.0), 5.0);
}

// -----------------------------------------------------------------------------
// Directional Light Shadow Calculation
// -----------------------------------------------------------------------------
float calcDirectionalShadow()
{
    // Transform fragment position to light space
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(fragPos, 1.0);
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;

    // If outside the light's projection volume, assume lit
    if (projCoords.z > 1.0) 
        return 1.0;

    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;

    // Read closest depth from shadow map
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    float currentDepth = projCoords.z;

    // Bias to reduce shadow acne (tweak as needed)
    float bias = 0.005;

    // Simple hard shadow test: 0.0 if in shadow, 1.0 if lit
    return (currentDepth - bias > closestDepth) ? 0.0 : 1.0;
}

// -----------------------------------------------------------------------------
// PBR Light Contribution for a Single Light
// -----------------------------------------------------------------------------
vec3 computeLightContribution(
    vec3 lightColor,
    vec3 lightDir,
    float attenuation,
    vec3 N,
    vec3 V,
    vec3 albedo,
    vec3 F0,
    float metallicVal,
    float roughnessVal
){
    // Half vector
    vec3 H = normalize(V + lightDir);

    // Cook-Torrance BRDF
    float NDF = DistributionGGX(N, H, roughnessVal);
    float G   = GeometrySmith(N, V, lightDir, roughnessVal);
    vec3  F   = fresnelSchlick(max(dot(H, V), 0.0), F0);

    vec3 numerator    = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, lightDir), 0.0) + 0.0001;
    vec3 specular     = numerator / denominator;

    // kS is Fresnel
    vec3 kS = F;
    // kD is diffuse reflection
    vec3 kD = vec3(1.0) - kS;
    // Multiply kD by (1 - metallic)
    kD *= (1.0 - metallicVal);

    float NdotL = max(dot(N, lightDir), 0.0);
    // (kD * albedo / PI + specular) * radiance * NdotL
    vec3 diffuseSpec = (kD * albedo / PI + specular) * lightColor * NdotL;

    return diffuseSpec * attenuation;
}

// -----------------------------------------------------------------------------
// Main Fragment Shader
// -----------------------------------------------------------------------------
void main()
{
    // 1. Base albedo: mix two greens by colorFactor
    vec3 albedo = mix(vec3(0.15, 0.7, 0.15),
                      vec3(0.25, 0.85, 0.25),
                      colorFactor);

    // 2. Normal and view direction
    vec3 N = normalize(fragNormal);
    vec3 V = normalize(camPos - fragPos);

    // 3. Fresnel reflectance at normal incidence
    //    If metallic is high, F0 is closer to albedo
    vec3 F0 = mix(vec3(0.04), albedo, metallic);

    // 4. Ambient term
    vec3 ambient = 0.1 * albedo * ao;  // slightly boosted for vegetation

    // 5. Sum contributions from point lights (unshadowed by default)
    vec3 totalLight = ambient;
    for (int i = 0; i < numLights; i++)
    {
        // Skip zero-intensity lights
        if (length(lights[i].color) < 0.0001)
            continue;

        // Distance check
        vec3 lightVec = lights[i].position - fragPos;
        float dist    = length(lightVec);
        if (dist > lights[i].distance)
            continue;

        // Attenuation
        float atten = 1.0 / (lights[i].attenuation.x +
                             lights[i].attenuation.y * dist +
                             lights[i].attenuation.z * dist * dist);

        vec3 L = normalize(lightVec);

        // Add PBR contribution
        totalLight += computeLightContribution(
            lights[i].color,  // lightColor
            L,                // lightDir
            atten,            // attenuation
            N, V,             // normal, view
            albedo,           // base color
            F0,               // reflectance at normal incidence
            metallic,         // metallic
            roughness         // roughness
        );
    }

    // 6. Add directional light with shadow
    //    (We assume the directional light is not in lights[], or is “the first light.”)
    float dirShadow = calcDirectionalShadow();
    {
        vec3 Ld = normalize(-directionalLightDir);

        // Simple "unit color" for directional light
        vec3 dirColor = vec3(1.0);

        // Compute the same PBR term for directional light
        // (no distance attenuation for directional)
        vec3 dirContribution = computeLightContribution(
            dirColor,  // lightColor
            Ld,        // lightDir
            1.0,       // no attenuation
            N, V,
            albedo,
            F0,
            metallic,
            roughness
        );

        // Multiply by shadow factor
        totalLight += dirContribution * dirShadow;
    }

    // 7. Final color: gamma correct
    vec3 finalColor = pow(totalLight, vec3(1.0 / 2.2));

    fragColor = vec4(finalColor, 1.0);
}
