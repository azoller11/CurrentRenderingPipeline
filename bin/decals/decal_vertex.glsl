#version 400 core

layout (location = 0) in vec3 position;
layout (location = 1) in vec2 texCoord;
layout (location = 2) in vec3 normal;  // If you have normals in your mesh

out vec2 TexCoord;
out vec3 FragPos;
out vec3 Normal;
out vec4 FragPosLightSpace;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

void main() {
    TexCoord = texCoord;
    
    // Calculate fragment position in world space
    vec4 worldPos = model * vec4(position, 1.0);
    FragPos = worldPos.xyz;
    
    // Calculate normal in world space
    mat3 normalMatrix = transpose(inverse(mat3(model)));
    Normal = normalMatrix * normal;
    
    // Calculate fragment position in light space for shadow mapping
    FragPosLightSpace = lightSpaceMatrix * worldPos;
    
    gl_Position = projection * view * worldPos;
}