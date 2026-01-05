#version 400 core

layout(location = 0) in vec3 inPosition;
layout(location = 1) in vec2 inTexCoord;
layout(location = 2) in vec3 inNormal;
layout(location = 3) in vec3 inTangent;
layout(location = 4) in vec3 inBitangent;
layout(location = 5) in ivec4 inBoneIds;
layout(location = 6) in vec4 inBoneWeights;

out VS_OUT {
    vec2 uv;
    vec3 wPosition;
    vec3 wNormal;
    vec3 wTangent;
    vec3 wBitangent;
} vs_out;

uniform mat4 model;
uniform mat4 view;
uniform mat4 projection;

uniform int useBones;

#define MAX_BONES 100
uniform mat4 bones[MAX_BONES];

mat4 buildSkinMatrix(ivec4 ids, vec4 w) {
    mat4 skin = mat4(0.0);
    float total = 0.0;

    if (ids.x >= 0 && w.x > 0.0) { skin += bones[ids.x] * w.x; total += w.x; }
    if (ids.y >= 0 && w.y > 0.0) { skin += bones[ids.y] * w.y; total += w.y; }
    if (ids.z >= 0 && w.z > 0.0) { skin += bones[ids.z] * w.z; total += w.z; }
    if (ids.w >= 0 && w.w > 0.0) { skin += bones[ids.w] * w.w; total += w.w; }

    // Normalize weights defensively (FBX exporters sometimes give sums != 1)
    if (total > 0.0) {
        skin /= total;
    } else {
        skin = mat4(1.0);
    }
    return skin;
}

void main() {
    vec4 localPos = vec4(inPosition, 1.0);

    mat4 skin = mat4(1.0);
    if (useBones == 1) {
        skin = buildSkinMatrix(inBoneIds, inBoneWeights);
    }

    // Position uses full combined transform
    vec4 worldPos = model * (skin * localPos);
    vs_out.wPosition = worldPos.xyz;

    // Normals/tangents must use normal matrix of the SAME combined linear transform
    mat3 M = mat3(model) * mat3(skin);
    mat3 normalMat = transpose(inverse(M));

    vec3 N = normalize(normalMat * inNormal);
    vec3 T = normalize(normalMat * inTangent);
    vec3 B = normalize(normalMat * inBitangent);

    // Orthonormalize TBN (stabilizes normal mapping a lot)
    T = normalize(T - N * dot(N, T));
    vec3 Bcross = normalize(cross(N, T));
    float handed = (dot(Bcross, B) < 0.0) ? -1.0 : 1.0;
    B = Bcross * handed;

    vs_out.wNormal = N;
    vs_out.wTangent = T;
    vs_out.wBitangent = B;

    vs_out.uv = inTexCoord;
    gl_Position = projection * view * worldPos;
}
