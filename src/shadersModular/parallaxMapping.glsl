vec2 parallaxMapping(vec2 texCoords, vec3 viewDirTangent) {
    if (hasHeight == 0) return texCoords;
    
    // Flip Y-axis for OpenGL's texture coordinates
    viewDirTangent.y *= -1.0;
    
    // Calculate perspective-adjusted displacement
    float numLayers = mix(maxLayers, minLayers, abs(dot(vec3(0.0, 0.0, 1.0), viewDirTangent)));
    vec2 P = viewDirTangent.xy * (parallaxScale / viewDirTangent.z); // Critical perspective fix
    vec2 delta = P / numLayers;

    // Depth buffer traversal
    float layerDepth = 1.0 / numLayers;
    float currentLayerDepth = 0.0;
    vec2 currentCoords = texCoords;
    float currentDepthValue = 1.0 - texture(heightMap, currentCoords).r; // Invert height value

    // Depth testing loop
    while(currentLayerDepth < currentDepthValue) {
        currentCoords -= delta;
        currentDepthValue = 1.0 - texture(heightMap, currentCoords).r; // Invert height lookup
        currentLayerDepth += layerDepth;
    }

    // Parallax occlusion interpolation
    vec2 prevCoords = currentCoords + delta; // Step back
    float prevDepth = 1.0 - texture(heightMap, prevCoords).r - (currentLayerDepth - layerDepth);
    float currentDepth = currentDepthValue - currentLayerDepth;
    
    float weight = currentDepth / (currentDepth - prevDepth);
    vec2 finalCoords = mix(currentCoords, prevCoords, weight);

    return finalCoords;
}