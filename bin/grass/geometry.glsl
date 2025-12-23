#version 400 core
layout(points) in;
layout(triangle_strip, max_vertices = 6) out;

uniform float bladeLength;    // Total height of the grass blade
uniform float bladeWidth;     // Width at the base of the blade
uniform float windStrength;   // Amplitude of the wind effect
uniform vec2 windDirection;   // Wind direction (normalized)
uniform float time;           // Time used to animate wind

uniform mat4 view;
uniform mat4 projection;

in vec3 fragPos[];  // Now matches vertex shader output

// Output a flat color factor to the fragment shader for color variation.
flat out float colorFactor;
// Output a flat normal and world-space position for PBR lighting.
flat out vec3 fragNormal;
flat out vec3 fragPosOut;

const float PI = 3.14159265;

// Build a rotation matrix about the Y axis.
mat3 rotationY(float angle) {
    float s = sin(angle);
    float c = cos(angle);
    return mat3(
         c, 0, s,
         0, 1, 0,
        -s, 0, c
    );
}

// Compute the local vertex position along the blade.
// t: vertical fraction [0,1]
// lateral: -0.5 for left edge, +0.5 for right edge; at tip, lateral = 0.
vec3 computeLocalPosition(float t, float lateral) {
    float y = bladeLength * t;
    float halfWidth = mix(bladeWidth * 0.5, 0.0, t);
    vec3 lateralOffset = vec3(lateral * halfWidth, 0.0, 0.0);
    
    // Use time and t to generate a nonlinear wind offset.
    float windFactor = sin(time + t * PI);
    vec3 windOffset = vec3(windDirection, 0.0) * windStrength * windFactor * (t * t);
    
    return vec3(lateralOffset.x, y, 0.0) + windOffset;
}

void main() {
    // Use the incoming world-space position from the vertex shader.
    vec3 basePos = fragPos[0];
    
    // Compute a random rotation angle in [0, 2pi] based on the base position.
    float randomAngle = fract(sin(dot(basePos.xz, vec2(12.9898, 78.233))) * 43758.5453) * 2.0 * PI;
    mat3 rot = rotationY(randomAngle);
    
    // Compute a color variation factor.
    colorFactor = 0.8 + fract(sin(dot(basePos.xz, vec2(93.9898, 67.345))) * 43758.5453) * 0.4;
    
    // Define three vertical positions: base, mid, tip.
    float t0 = 0.0;
    float t1 = 0.5;
    float t2 = 1.0;
    
    // Compute the local positions in blade space, rotate, then add to the base.
    vec3 v0 = basePos + (rot * computeLocalPosition(t0, -0.5));
    vec3 v1 = basePos + (rot * computeLocalPosition(t0,  0.5));
    vec3 v2 = basePos + (rot * computeLocalPosition(t1, -0.5));
    vec3 v3 = basePos + (rot * computeLocalPosition(t1,  0.5));
    vec3 v4 = basePos + (rot * computeLocalPosition(t2, 0.0));
    
    // For lighting, we output an averaged position (here we use basePos) and compute an approximate normal.
    vec3 edge1 = v3 - v2;
    vec3 edge2 = v4 - v2;
    vec3 n = normalize(cross(edge1, edge2));
    
    fragPosOut = basePos; // You can average the vertices if desired.
    fragNormal = n;
    
    // Emit vertices in triangle strip order.
    gl_Position = projection * view * vec4(v0, 1.0);
    EmitVertex();
    
    gl_Position = projection * view * vec4(v1, 1.0);
    EmitVertex();
    
    gl_Position = projection * view * vec4(v2, 1.0);
    EmitVertex();
    
    gl_Position = projection * view * vec4(v3, 1.0);
    EmitVertex();
    
    // Emit the tip twice for a pointed top.
    gl_Position = projection * view * vec4(v4, 1.0);
    EmitVertex();
    
    gl_Position = projection * view * vec4(v4, 1.0);
    EmitVertex();
    
    EndPrimitive();
}
 