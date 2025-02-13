float calculatedDirectionalShadows()
{
    // Transform the fragment position to light space.
    vec4 fragPosLightSpace = lightSpaceMatrix * vec4(fs_in.wPosition, 1.0);
    // Perform perspective divide.
    vec3 projCoords = fragPosLightSpace.xyz / fragPosLightSpace.w;
    // Transform from NDC [-1,1] to texture space [0,1].
    projCoords = projCoords * 0.5 + 0.5;
    
    // If the fragment is outside the light's frustum, assume full light.
    if (projCoords.z > 1.0)
        return 1.0;
    
    // Apply a bias to help reduce shadow acne.
    float bias = 0.005;
    
    // Retrieve the closest depth stored in the shadow map.
    float closestDepth = texture(shadowMap, projCoords.xy).r;
    // Current fragment depth from light's perspective.
    float currentDepth = projCoords.z;
    
    // If the current depth is greater than the stored depth (plus bias),
    // then this fragment is in shadow.
    float shadow = currentDepth - bias > closestDepth ? 0.15 : 1.0;
    
    return shadow;
}