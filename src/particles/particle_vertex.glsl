#version 400 core

layout(location = 0) in vec2 position;

// modelViewMatrix occupies locations 1,2,3,4 (each is vec4)
layout(location = 1) in vec4 mvRow0;
layout(location = 2) in vec4 mvRow1;
layout(location = 3) in vec4 mvRow2;
layout(location = 4) in vec4 mvRow3;

layout(location = 5) in vec4 texOffsets;   // (o1.x, o1.y, o2.x, o2.y)
layout(location = 6) in float blendFactor;

out vec2 textureCoords1;
out vec2 textureCoords2;
out float blend;

uniform mat4 projectionMatrix;
uniform float numberOfRows;

void main(void)
{
    mat4 modelViewMatrix = mat4(mvRow0, mvRow1, mvRow2, mvRow3);

    // quad local coords -> [0..1] atlas coords
    vec2 uv = position + vec2(0.5, 0.5);
    uv.y = 1.0 - uv.y; // if your atlas is top-left origin; remove if not

    uv /= numberOfRows;

    textureCoords1 = uv + texOffsets.xy;
    textureCoords2 = uv + texOffsets.zw;
    blend = blendFactor;

    gl_Position = projectionMatrix * modelViewMatrix * vec4(position, 0.0, 1.0);
}
