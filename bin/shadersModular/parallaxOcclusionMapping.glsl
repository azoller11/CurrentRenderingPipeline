// -----------------------------------------------------------------------------
// Parallax Occlusion Mapping
// -----------------------------------------------------------------------------
vec2 parallaxOcclusionMapping(vec2 baseUV, vec3 viewDirTangent) {
    viewDirTangent.y = -viewDirTangent.y;
    float viewAngle = dot(vec3(0,0,1), viewDirTangent);
    int numSteps = int(mix(float(maxLayers), float(minLayers), viewAngle));

    vec2 deltaUV = (parallaxScale * viewDirTangent.xy) / float(numSteps);
    vec2 currUV  = baseUV;

    float layerDepth = 0.0;
    float layerStep  = 1.0 / float(numSteps);

    for (int i = 0; i < numSteps; i++) {
        float height = texture(heightMap, currUV).r;
        if (layerDepth > height) {
            break;
        }
        currUV -= deltaUV;
        layerDepth += layerStep;
    }

    return currUV;
}