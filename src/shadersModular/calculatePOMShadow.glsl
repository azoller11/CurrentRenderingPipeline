float calculatePOMShadow(vec3 lightDirTangent, vec2 initialUV) {
    if (hasHeight == 0) return 1.0;
    
    // Flip Y-axis and normalize direction
    lightDirTangent.y *= -1.0;
    lightDirTangent = normalize(lightDirTangent);
    
    // Shadow parameters - more aggressive settings for visible results
    float shadowParallaxScale = parallaxScale * 1.2; // Increased scale for visibility
    float shadowMinLayers = minLayers * 0.7;
    float shadowMaxLayers = maxLayers * 0.7;

    // Calculate perspective-correct shadow stepping
    float numLayers = mix(shadowMaxLayers, shadowMinLayers, 
                        abs(dot(vec3(0.0, 0.0, 1.0), lightDirTangent)));
    vec2 P = lightDirTangent.xy * (shadowParallaxScale / lightDirTangent.z);
    vec2 delta = P / numLayers;

    // Convert height to depth (inverted relationship)
    float initialDepth = 1.0 - texture(heightMap, initialUV).r;
    float currentDepth = initialDepth;
    vec2 currentCoords = initialUV;
    
    // Depth-aware shadow marching
    float shadowFactor = 1.0;
    float layerDepth = 1.0 / numLayers;
    
    for(int i = 0; i < int(numLayers); i++) {
        currentCoords += delta;
        float sampledDepth = 1.0 - texture(heightMap, currentCoords).r;
        
        // Depth comparison with bias
        if(sampledDepth < currentDepth - 0.005) { 
            // Calculate soft shadows based on depth difference
            float depthDifference = (currentDepth - sampledDepth);
            shadowFactor *= 1.0 - smoothstep(0.0, 0.1, depthDifference);
        }
        
        currentDepth -= layerDepth;
    }

    return clamp(shadowFactor, 0.3, 1.0);
}