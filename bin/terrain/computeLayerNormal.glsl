vec3 computeLayerNormal(
    vec3 worldNormal,
    vec3 worldTangent,
    vec2 uv,
    sampler2D normalMap,
    bool hasNormal)
{
    // Fallback: no normal map
    if (!hasNormal)
        return normalize(worldNormal);

    // Build TBN
    vec3 N = normalize(worldNormal);
    vec3 T = normalize(worldTangent);
    T = normalize(T - dot(T, N) * N); // orthogonalize T
    vec3 B = normalize(cross(N, T));
    mat3 TBN = mat3(T, B, N);

    // Sample tangent-space normal
    vec3 tangentNormal = texture(normalMap, uv).rgb * 2.0 - 1.0;

    // Return world-space normal
    return normalize(TBN * tangentNormal);
}