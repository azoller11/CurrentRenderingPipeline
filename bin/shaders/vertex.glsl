#version 400 core

// MATCHES THE 22-FLOAT VERTEX FORMAT
layout(location = 0) in vec3 inPosition;      // floats 0-2
layout(location = 1) in vec2 inTexCoord;      // floats 3-4
layout(location = 2) in vec3 inNormal;        // floats 5-7
layout(location = 3) in vec3 inTangent;       // floats 8-10
layout(location = 4) in vec3 inBitangent;     // floats 11-13
layout(location = 5) in ivec4 inBoneIds;      // floats 14-17 (stored as floats, read as ints)
layout(location = 6) in vec4 inBoneWeights;   // floats 18-21

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



void main() {
    vec4 localPos = vec4(inPosition, 1.0);
    vec3 localNorm = inNormal;
    vec3 localTan = inTangent;
    vec3 localBitan = inBitangent;

    // --- Skinning ---
    if (useBones == 1) {
        ivec4 boneIds = inBoneIds;
        
        // Initialize skin matrix
        mat4 skin = mat4(0.0);
        float totalWeight = 0.0;
        
        // Sum weighted bone matrices
        if (boneIds.x >= 0 && inBoneWeights.x > 0.0) {
            skin += bones[boneIds.x] * inBoneWeights.x;
            totalWeight += inBoneWeights.x;
        }
        if (boneIds.y >= 0 && inBoneWeights.y > 0.0) {
            skin += bones[boneIds.y] * inBoneWeights.y;
            totalWeight += inBoneWeights.y;
        }
        if (boneIds.z >= 0 && inBoneWeights.z > 0.0) {
            skin += bones[boneIds.z] * inBoneWeights.z;
            totalWeight += inBoneWeights.z;
        }
        if (boneIds.w >= 0 && inBoneWeights.w > 0.0) {
            skin += bones[boneIds.w] * inBoneWeights.w;
            totalWeight += inBoneWeights.w;
        }
        
        // If total weight is less than 1, add identity for remaining
        if (totalWeight < 1.0) {
            skin += mat4(1.0) * (1.0 - totalWeight);
        }
        
        localPos = skin * localPos;
        
        // For normals/tangents
        mat3 skin3 = mat3(skin);
        localNorm = normalize(skin3 * localNorm);
        localTan = normalize(skin3 * localTan);
        localBitan = normalize(skin3 * localBitan);
    }

    // --- Transform to world ---
    vec4 worldPos = model * localPos;
    vs_out.wPosition = worldPos.xyz;

    mat3 normalMatrix = transpose(inverse(mat3(model)));

    vs_out.wNormal = normalize(normalMatrix * localNorm);
    vs_out.wTangent = normalize(normalMatrix * localTan);
    vs_out.wBitangent = normalize(normalMatrix * localBitan);

    vs_out.uv = inTexCoord;
    gl_Position = projection * view * worldPos;
}