#version 400 core

layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoord;

// IMPORTANT: match VAO type!
// If VAO uses glVertexAttribIPointer for bone IDs, this MUST be ivec4.
layout (location = 5) in ivec4 inBoneIds;
layout (location = 6) in vec4  inBoneWeights;

uniform mat4 model;
uniform mat4 lightSpaceMatrix;

uniform int useBones;
#define MAX_BONES 100
uniform mat4 bones[MAX_BONES];

out vec2 passTexCoord;

void main() {
    vec4 localPos = vec4(inPosition, 1.0);

    if (useBones == 1) {
        ivec4 ids = inBoneIds;
        vec4 w = inBoneWeights;

        float total = w.x + w.y + w.z + w.w;
        if (total > 0.0) w /= total;
        else w = vec4(1,0,0,0);

        mat4 skin = mat4(0.0);
        if (ids.x >= 0 && w.x > 0.0) skin += bones[ids.x] * w.x;
        if (ids.y >= 0 && w.y > 0.0) skin += bones[ids.y] * w.y;
        if (ids.z >= 0 && w.z > 0.0) skin += bones[ids.z] * w.z;
        if (ids.w >= 0 && w.w > 0.0) skin += bones[ids.w] * w.w;

        localPos = skin * localPos;
    }

    gl_Position = lightSpaceMatrix * model * localPos;
    passTexCoord = inTexCoord;
}
