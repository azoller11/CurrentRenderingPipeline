vec3 computeLightContribution(Light light, vec3 fragPos, vec3 N, vec3 V, 
                             float metallic, float roughness, float ao, 
                             vec3 baseColor) {
    float distance = length(light.position - fragPos);
    
    // If the fragment is outside the light's effective range, no contribution.
    if(distance > light.distance) {
        return vec3(0.0);
    }
    
    // Standard attenuation calculation.
    float attenuation = 1.0 / (light.attenuation.x + 
                              light.attenuation.y * distance + 
                              light.attenuation.z * distance * distance);
    
    // Smoothly reduce the light intensity with a wider range for the fade.
    // Falloff begins at 70% of the light's range and fully fades at 100%.
    float rangeFactor = 1.0 - smoothstep(light.distance * 0.7, light.distance, distance);
    attenuation *= rangeFactor;
    
    vec3 radiance = light.color * attenuation;
    
    // Cook-Torrance BRDF calculations.
    vec3 L = normalize(light.position - fragPos);
    vec3 H = normalize(V + L);
    float NDF = DistributionGGX(N, H, roughness);   
    float G   = GeometrySmith(N, V, L, roughness);    
    vec3 F    = fresnelSchlick(max(dot(H, V), 0.0), mix(vec3(0.04), baseColor, metallic));
    
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0);
    vec3 specular = numerator / max(denominator, 0.001);
    
    vec3 kS = F;
    vec3 kD = (vec3(1.0) - kS) * (1.0 - metallic);
    
    float NdotL = max(dot(N, L), 0.0);        
    return (kD * baseColor / PI + specular) * radiance * NdotL;
}
