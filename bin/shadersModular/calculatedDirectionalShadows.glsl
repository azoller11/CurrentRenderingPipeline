
float calculatedDirectionalShadows()
{

	const float SHADOW_SOFTNESS = 1.0f;


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
    float bias = max(0.0005 * (1.0 - dot(normalize(fs_in.wNormal), normalize(directionalLightDir))), 0.00005);
    
    // Compute the size of one texel in the shadow map.
    vec2 texelSize = 1.0 / textureSize(shadowMap, 0);
    
    // Use PCF to average the shadow result over a 3x3 kernel.
    float shadow = 0.0;
    int samples = 0;
    for (int x = -1; x <= 1; ++x)
    {
        for (int y = -1; y <= 1; ++y)
        {
            // Offset texture coordinates based on the softness constant.
            vec2 offset = vec2(x, y) * texelSize * SHADOW_SOFTNESS;
            float closestDepth = texture(shadowMap, projCoords.xy + offset).r;
            // If the current depth (with bias) is less than the closest depth, then this sample is lit.
            shadow += (projCoords.z - bias > closestDepth) ? 0.0 : 1.0;
            samples++;
        }
    }
    shadow /= float(samples);
    
    return shadow;
}
