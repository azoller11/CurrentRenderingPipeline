float calculatePOMShadow(vec3 lightDirTangent, vec2 initialUV) {
    if (hasHeight == 0) return 1.0;
    
    // Flip Y-axis and normalize light direction
    lightDirTangent.y *= -1.0;
    lightDirTangent = normalize(lightDirTangent);
    
    // Adjust parallax parameters
    float shadowParallaxScale = parallaxScale * 1.2;
    float shadowMinLayers = minLayers * 0.7;
    float shadowMaxLayers = maxLayers * 0.7;
    
    // Dynamically adjust the number of layers based on the view angle
    float numLayers = mix(shadowMaxLayers, shadowMinLayers, 
                          abs(dot(vec3(0.0, 0.0, 1.0), lightDirTangent)));
    
    // Calculate the stepping offset per layer
    vec2 P = lightDirTangent.xy * (shadowParallaxScale / lightDirTangent.z);
    vec2 delta = P / numLayers;
    
    // Retrieve the initial depth from the height map
    float initialDepth = 1.0 - texture(heightMap, initialUV).r;
    float currentDepth = initialDepth;
    vec2 currentCoords = initialUV;
    
    float shadowFactor = 1.0;
    float layerDepth = 1.0 / numLayers;
    
    // Use a fixed maximum iteration count to allow potential loop unrolling.
    int maxIterations = int(shadowMaxLayers);
    for (int i = 0; i < maxIterations; i++) {
        if (i >= int(numLayers)) break; // Only process the required number of layers
        
        // Step to the next texture coordinate
        currentCoords += delta;
        float sampledDepth = 1.0 - texture(heightMap, currentCoords).r;
        
        // Compute the depth difference with bias and use a branchless factor
        float depthDiff = currentDepth - sampledDepth - 0.005;
        float factor = 1.0 - smoothstep(0.0, 0.1, max(depthDiff, 0.0));
        shadowFactor *= factor;
        
        // Early exit if shadow factor is near the minimum threshold
        if (shadowFactor < 0.31) break;
        
        currentDepth -= layerDepth;
    }
    
    return clamp(shadowFactor, 0.3, 1.0);
}
