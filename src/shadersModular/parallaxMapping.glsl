vec2 parallaxMapping(vec2 texCoords, vec3 viewDirTangent) {
    if (hasHeight == 0) return texCoords;
    
    // Calculate the distance from the camera to the fragment.
    float dist = length(cameraPos - fs_in.wPosition);
    
    float effectiveLayers;
    // If the camera is really close, use fewer iterations to improve performance.
    if (dist < 5.0) {
        // When very close, use a simplified approach by choosing only the minimum layers.
        effectiveLayers = minLayers;
    } else {
        // Otherwise, determine effectiveLayers based on distance.
        float distanceFactor = clamp(dist / 100.0, 0.0, 1.0);
        effectiveLayers = mix(maxLayers, minLayers, distanceFactor);
    }
    
    // Flip Y-axis for the view direction
    viewDirTangent.y *= -1.0;
    
    // Pre-calculate the parallax offset and delta per layer.
    float safeZ = max(viewDirTangent.z, 0.15);
    vec2 P = viewDirTangent.xy * (parallaxScale / safeZ);
    vec2 delta = P / effectiveLayers;
    
    float layerDepth = 1.0 / effectiveLayers;
    float currentLayerDepth = 0.0;
    vec2 currentCoords = texCoords;
    float currentDepthValue = 1.0 - texture(heightMap, currentCoords).r;
    
    // Use a fixed iteration loop with early exit.
    int maxIterations = int(effectiveLayers);
    for (int i = 0; i < maxIterations; i++) {
        if (currentLayerDepth >= currentDepthValue)
            break;
        
        // Step the texture coordinate and update depth value.
        currentCoords -= delta;
        currentDepthValue = 1.0 - texture(heightMap, currentCoords).r;
        currentLayerDepth += layerDepth;
    }
    
    // Refine the intersection by interpolating between the last two coordinates.
    vec2 prevCoords = currentCoords + delta;
    float prevDepth = 1.0 - texture(heightMap, prevCoords).r - (currentLayerDepth - layerDepth);
    float currentDiff = currentDepthValue - currentLayerDepth;
    float weight = currentDiff / (currentDiff - prevDepth);
    vec2 finalCoords = mix(currentCoords, prevCoords, clamp(weight, 0.0, 1.0));
    
    return finalCoords;
}
