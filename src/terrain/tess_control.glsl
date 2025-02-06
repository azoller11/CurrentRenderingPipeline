#version 400 core

layout(vertices = 3) out;

in vec2 vTexCoord[];
in float vBlend[];
out vec2 tcTexCoord[];
out float tcBlend[];

uniform vec3 cameraPosition;

void main() {
    // Pass through texture coordinates and blend factor.
    tcTexCoord[gl_InvocationID] = vTexCoord[gl_InvocationID];
    tcBlend[gl_InvocationID] = vBlend[gl_InvocationID];

    // Compute the patch center from the three control points.
    vec3 p0 = gl_in[0].gl_Position.xyz;
    vec3 p1 = gl_in[1].gl_Position.xyz;
    vec3 p2 = gl_in[2].gl_Position.xyz;
    vec3 patchCenter = (p0 + p1 + p2) / 3.0;

    // Compute the distance from the camera to the patch center.
    float distance = length(cameraPosition - patchCenter);

    // Piecewise LOD with shorter distance intervals and lower detail for farther patches.
    float tessLevel;
    float distanceLevel = 4;
    if (distance < 5.0 * distanceLevel) {
        tessLevel = 1024.0;  // Extremely high detail when extremely close.
    } else if (distance < 10.0 * distanceLevel) {
        tessLevel = 512.0;
    } else if (distance < 15.0 * distanceLevel) {
        tessLevel = 256.0;
    } else if (distance < 20.0 * distanceLevel) {
        tessLevel = 128.0;
    } else if (distance < 30.0 * distanceLevel) {
        tessLevel = 64.0;
    } else if (distance < 40.0 * distanceLevel) {
        tessLevel = 32.0;
    } else if (distance < 60.0 * distanceLevel) {
        tessLevel = 16.0;
    } else if (distance < 80.0 * distanceLevel) {
        tessLevel = 8.0;
    } else {
        tessLevel = 4.0;     // Minimal tessellation for distant patches.
    }

    // Only one invocation sets the tessellation levels.
    if (gl_InvocationID == 0) {
        gl_TessLevelInner[0] = tessLevel;
        gl_TessLevelOuter[0] = tessLevel;
        gl_TessLevelOuter[1] = tessLevel;
        gl_TessLevelOuter[2] = tessLevel;
    }
    
    // Pass through the vertex position.
    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;
}
