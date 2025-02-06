#version 400 core

layout(triangles, equal_spacing, cw) in;

in vec2 tcTexCoord[];
in float tcBlend[];
out vec2 teTexCoord;
out float teBlend;
out vec3 teWorldPosition;
out vec4 teLightSpacePos;  // <-- NEW: Light-space position

uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat4 lightSpaceMatrix; // NEW: transforms world-space into light-space

void main() {
    // Interpolate position.
    vec3 p0 = gl_in[0].gl_Position.xyz;
    vec3 p1 = gl_in[1].gl_Position.xyz;
    vec3 p2 = gl_in[2].gl_Position.xyz;
    vec3 pos = gl_TessCoord.x * p0 + gl_TessCoord.y * p1 + gl_TessCoord.z * p2;
    
    // Interpolate texture coordinates and blend factor.
    teTexCoord = gl_TessCoord.x * tcTexCoord[0] +
                 gl_TessCoord.y * tcTexCoord[1] +
                 gl_TessCoord.z * tcTexCoord[2];
    teBlend = gl_TessCoord.x * tcBlend[0] +
              gl_TessCoord.y * tcBlend[1] +
              gl_TessCoord.z * tcBlend[2];
    
    // Compute world-space position.
    vec4 worldPos = model * vec4(pos, 1.0);
    teWorldPosition = worldPos.xyz;
    
    // Compute the light-space position for shadow mapping.
    teLightSpacePos = lightSpaceMatrix * worldPos;
    
    gl_Position = projection * view * worldPos;
}
