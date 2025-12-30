#version 400 core

// Vertex attributes
layout (location = 0) in vec3 inPosition;
layout (location = 1) in vec2 inTexCoord;
layout (location = 5) in ivec4 inBoneIds;      // Bone IDs for animation
layout (location = 6) in vec4 inBoneWeights;   // Bone weights for animation

// Uniforms
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

// Animation uniforms
uniform int useBones;
#define MAX_BONES 100
uniform mat4 bones[MAX_BONES];

// Pass texture coordinates to the fragment shader
out vec2 passTexCoord;

void main() {
    vec4 localPos = vec4(inPosition, 1.0);
    
    // Apply skinning if bones are enabled
    if (useBones == 1) {
        ivec4 boneIds = inBoneIds;
        vec4 weights = inBoneWeights;
        
        // Normalize weights
        float totalWeight = weights.x + weights.y + weights.z + weights.w;
        if (totalWeight > 0.0) {
            weights /= totalWeight;
        }
        
        // Initialize skin matrix
        mat4 skinMatrix = mat4(0.0);
        
        // Sum weighted bone matrices
        if (boneIds.x >= 0 && weights.x > 0.0) {
            skinMatrix += bones[boneIds.x] * weights.x;
        }
        if (boneIds.y >= 0 && weights.y > 0.0) {
            skinMatrix += bones[boneIds.y] * weights.y;
        }
        if (boneIds.z >= 0 && weights.z > 0.0) {
            skinMatrix += bones[boneIds.z] * weights.z;
        }
        if (boneIds.w >= 0 && weights.w > 0.0) {
            skinMatrix += bones[boneIds.w] * weights.w;
        }
        
        localPos = skinMatrix * localPos;
    }
    
    gl_Position = lightSpaceMatrix * model * localPos;
    passTexCoord = inTexCoord;
}