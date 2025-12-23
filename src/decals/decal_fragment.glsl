#version 400 core

in vec2 TexCoord;
in vec3 FragPos;
in vec3 Normal;
in vec4 FragPosLightSpace;

out vec4 FragColor;

// Textures
uniform sampler2D decalTexture;
uniform sampler2D shadowMap;
uniform sampler2D normalTexture;

// Lighting
uniform vec3 viewPos;
uniform int numLights;
uniform int useNormalMap;

// Lighting controls
uniform float ambient = 0.4;           // Always add 10% ambient light
uniform float minLight = 0.2;         // Minimum light value (prevents total darkness)
uniform float maxLight = 0.7;          // Maximum light value (prevents overexposure)
uniform float shineDamper = 1.0;       // Control specular intensity (0.0 = no shine, 1.0 = full shine)
uniform float specularPower = 32.0;    // Shininess exponent

struct Light {
    vec3 position;
    vec3 color;
    vec3 attenuation;
    vec3 direction;
    int castShadow;
};

uniform Light lights[16];

// Shadow calculation (keep your existing code)
float ShadowCalculation(vec4 fragPosLightSpace, vec3 normal, vec3 lightDir) {
    // Perform perspective divide
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    
    // Transform to [0,1] range
    projCoords = projCoords * 0.5 + 0.5;
    
    // Get closest depth value from light's perspective
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    
    // Get depth of current fragment from light's perspective
    float currentDepth = projCoords.z;
    
    // Calculate bias (based on normal and light direction)
    float bias = max(0.05 * (1.0 - dot(normal, lightDir)), 0.005);
    
    // Check if current fragment is in shadow
    float shadow = currentDepth - bias > closestDepth ? 1.0 : 0.0;
    
    // PCF (Percentage Closer Filtering) for smoother shadows
    shadow = 0.0;
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    for(int x = -1; x <= 1; ++x) {
        for(int y = -1; y <= 1; ++y) {
            float pcfDepth = texture(shadowMap, projCoords.xy + vec2(x, y) * texelSize).r;
            shadow += currentDepth - bias > pcfDepth ? 1.0 : 0.0;
        }
    }
    shadow /= 9.0;
    
    // Keep shadow at 0.0 when outside the far plane region of the light's frustum
    if(projCoords.z > 1.0)
        shadow = 0.0;
        
    return shadow;
}

void main() {
    // Base color from texture
    vec4 color = texture(decalTexture, TexCoord);
    
    if (color.a < 0.1) {
        discard;
    }
    
    // Normal mapping
    vec3 normal = normalize(Normal);
    
    if (useNormalMap == 1) {
        // Get normal from normal map
        vec3 normalMap = texture(normalTexture, TexCoord).rgb;
        // Transform from [0,1] to [-1,1]
        normalMap = normalize(normalMap * 2.0 - 1.0);
        normal = normalize(normal + normalMap);
    }
    
    // Lighting calculation
    vec3 totalDiffuse = vec3(0.0);
    vec3 totalSpecular = vec3(0.0);
    
    for (int i = 0; i < numLights; i++) {
        Light light = lights[i];
        
        // Calculate light direction and distance
        vec3 lightDir = normalize(light.position - FragPos);
        float distance = length(light.position - FragPos);
        
        // Calculate attenuation
        float att = 1.0 / (light.attenuation.x + 
                          light.attenuation.y * distance + 
                          light.attenuation.z * distance * distance);
        
        // Diffuse lighting
        float diff = max(dot(normal, lightDir), 0.0);
        totalDiffuse += diff * light.color * att;
        
        // Specular lighting (Blinn-Phong)
        vec3 viewDir = normalize(viewPos - FragPos);
        vec3 halfwayDir = normalize(lightDir + viewDir);
        float spec = pow(max(dot(normal, halfwayDir), 0.0), specularPower);
        totalSpecular += spec * light.color * att * shineDamper;
    }
    
    // Apply shadows to the first light that casts shadows
    float shadow = 0.0;
    for (int i = 0; i < min(numLights, 1); i++) {
        if (lights[i].castShadow == 1) {
            vec3 lightDir = normalize(lights[i].position - FragPos);
            shadow = ShadowCalculation(FragPosLightSpace, normal, lightDir);
            break;
        }
    }
    
    // Combine lighting with shadows
    vec3 lighting = totalDiffuse + totalSpecular;
    lighting *= (1.0 - shadow);
    
    // ADD ambient light
    lighting += vec3(ambient);
    
    // CLAMP lighting between minLight and maxLight
    lighting = clamp(lighting, vec3(minLight), vec3(maxLight));
    
    // Final color with lighting
    FragColor = vec4(lighting * color.rgb, color.a);
}