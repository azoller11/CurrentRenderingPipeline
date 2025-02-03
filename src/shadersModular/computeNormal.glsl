vec3 computeNormal(vec3 worldNormal, vec3 worldTangent, vec2 uv, out mat3 TBN)
{
    vec3 N = normalize(worldNormal);
    vec3 T = normalize(worldTangent);
    vec3 B = normalize(cross(N, T));
    TBN = mat3(T, B, N);

    if (hasNormal == 1) {
        // Sample and convert the normal map value from [0,1] to [-1,1]
        vec3 tangentNormal = texture(normalMap, uv).rgb * 2.0 - 1.0;
        // Transform the tangent-space normal into world space using TBN.
        N = normalize(TBN * tangentNormal);
    }
    return N;
}