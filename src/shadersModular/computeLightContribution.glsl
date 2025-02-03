vec3 computeLightContribution(Light light, vec3 fragPos, vec3 N, vec3 V, float metallic, float roughness, float ao, vec3 baseColor)
{
    vec3 L = normalize(light.position - fragPos);
    vec3 H = normalize(V + L);
    
    // Distance attenuation.
    float distance = length(light.position - fragPos);
    float attenuation = 1.0 / (light.attenuation.x + light.attenuation.y * distance + light.attenuation.z * distance * distance);
    vec3 radiance = light.color * attenuation;
    
    // Cook-Torrance BRDF.
    float NDF = DistributionGGX(N, H, roughness);
    float G   = GeometrySmith(N, V, L, roughness);
    vec3 F0 = vec3(0.04); 
    F0 = mix(F0, baseColor, metallic); // Interpolate between dielectric and metal
    vec3 F  = fresnelSchlick(max(dot(H, V), 0.0), F0);
    
    vec3 numerator = NDF * G * F;
    float denominator = 4.0 * max(dot(N, V), 0.0) * max(dot(N, L), 0.0) + 0.001;
    vec3 specular = numerator / denominator;
    
    // kS is the specular reflection ratio, and kD is the diffuse ratio.
    vec3 kS = F;
    vec3 kD = vec3(1.0) - kS;
    // Metals have no diffuse reflection.
    kD *= (1.0 - metallic);
    
    float NdotL = max(dot(N, L), 0.0);
    
    // Return the light’s contribution.
    return (kD * baseColor / PI + specular) * radiance * NdotL;
}