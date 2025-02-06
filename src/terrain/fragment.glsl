#version 400 core

// Inputs from tessellation evaluation shader:
in vec2 teTexCoord;
in float teBlend;
in vec3 teWorldPosition;  // <-- Ensure this is passed from the tessellation evaluation shader

out vec4 fragColor;

uniform sampler2D grassTexture;
uniform sampler2D rockTexture;
uniform float tiling;  // Set from Java

// Maximum number of lights supported.
#define MAX_LIGHTS 16

// Number of active lights.
uniform int numLights;

// Light properties arrays. For each light, we supply the world-space position, its color, and attenuation (constant, linear, quadratic).
uniform vec3 lightPositions[MAX_LIGHTS];
uniform vec3 lightColors[MAX_LIGHTS];
uniform vec3 lightAttenuations[MAX_LIGHTS];

void main() {
    // Sample the base textures and blend between them.
    vec4 grassColor = texture(grassTexture, teTexCoord * tiling);
    vec4 rockColor = texture(rockTexture, teTexCoord * tiling);
    vec4 baseColor = mix(grassColor, rockColor, teBlend);

    // Compute an approximate normal from screen-space derivatives of the world position.
    vec3 dPosdx = dFdx(teWorldPosition);
    vec3 dPosdy = dFdy(teWorldPosition);
    vec3 normal = normalize(cross(dPosdx, dPosdy));

    // Accumulate lighting from all lights.
    vec3 lighting = vec3(0.0);
    for (int i = 0; i < numLights; i++) {
        // Compute vector from fragment to light.
        vec3 lightDir = lightPositions[i] - teWorldPosition;
        float distance = length(lightDir);
        lightDir = normalize(lightDir);

        // Attenuation factor: intensity = 1/(a + b*d + c*d^2)
        float intensity = 1.0 / (lightAttenuations[i].x +
                                 lightAttenuations[i].y * distance +
                                 lightAttenuations[i].z * distance * distance);

        // Diffuse lighting using Lambert’s cosine law.
        float diff = max(dot(normal, lightDir), 0.0);
        lighting += lightColors[i] * intensity * diff;
    }

    // Multiply the base color by the computed lighting.
    fragColor = vec4(baseColor.rgb * lighting, baseColor.a);
}
