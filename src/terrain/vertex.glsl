#version 400 core

layout (location = 0) in vec3 inPos;
layout (location = 1) in vec2 inUV;
layout (location = 2) in vec3 inNormal;
layout (location = 3) in vec3 inTangent;

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

out VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} vs_out;

void main()
{
    vec4 worldPosition = model * vec4(inPos, 1.0);

    mat3 normalMat = transpose(inverse(mat3(model)));
    vec3 N = normalize(normalMat * inNormal);
    vec3 T = normalize(normalMat * inTangent);

    // Orthogonalize tangent
    T = normalize(T - dot(T, N) * N);

    vs_out.uv            = inUV;
    vs_out.baseUV        = inUV;
    vs_out.worldPos      = worldPosition.xyz;
    vs_out.normal        = N;
    vs_out.tangent       = T;
    vs_out.lightSpacePos = lightSpaceMatrix * worldPosition;

    gl_Position = projection * view * worldPosition;
}
