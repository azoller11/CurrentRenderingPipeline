vec2 parallaxMapping(vec2 texCoords, vec3 viewDirTangent) {
    if (hasHeight == 0) return texCoords;
    
    // Optionally reduce iterations for distant fragments (assume we pass in a distance factor)
    float distanceFactor = clamp(length(cameraPos - fs_in.wPosition) / 100.0, 0.0, 1.0);
    // Interpolate between min and max layers based on distance (fewer layers for distant fragments)
    float effectiveLayers = mix(maxLayers, minLayers, distanceFactor);
    
    viewDirTangent.y *= -1.0;
    
    vec2 P = viewDirTangent.xy * (parallaxScale / viewDirTangent.z);
    vec2 delta = P / effectiveLayers;
    
    float layerDepth = 1.0 / effectiveLayers;
    float currentLayerDepth = 0.0;
    vec2 currentCoords = texCoords;
    float currentDepthValue = 1.0 - texture(heightMap, currentCoords).r;

    while(currentLayerDepth < currentDepthValue) {
        currentCoords -= delta;
        currentDepthValue = 1.0 - texture(heightMap, currentCoords).r;
        currentLayerDepth += layerDepth;
    }

    vec2 prevCoords = currentCoords + delta;
    float prevDepth = 1.0 - texture(heightMap, prevCoords).r - (currentLayerDepth - layerDepth);
    float currentDepth = currentDepthValue - currentLayerDepth;
    float weight = currentDepth / (currentDepth - prevDepth);
    vec2 finalCoords = mix(currentCoords, prevCoords, weight);

    return finalCoords;
}
