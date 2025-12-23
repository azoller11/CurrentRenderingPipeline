#version 400 core

in vec2 textureCoords1;
in vec2 textureCoords2;
in float blend;

out vec4 out_Color;

uniform sampler2D particleTexture;

void main(void)
{
    vec4 c1 = texture(particleTexture, textureCoords1);
    vec4 c2 = texture(particleTexture, textureCoords2);
    vec4 color = mix(c1, c2, blend);

    // start lenient so you can SEE something
    if (color.a < 0.05) discard;

    out_Color = color;
}
