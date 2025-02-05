#version 400 core

in vec2 TexCoords;
out vec4 FragColor;

uniform sampler2D textureSampler;
uniform vec4 solidColor;
uniform int useTexture;
uniform float brightness;

void main() {
    if (useTexture == 1) {
    	vec2 flippedTexCoords = vec2(TexCoords.x, 1.0 - TexCoords.y);
        vec4 texColor = texture(textureSampler, flippedTexCoords);
        if (texColor.a < 0.1) // Discard fully transparent pixels
            discard;
        FragColor = texColor * vec4(brightness, brightness, brightness, 1.0);
    } else {
        FragColor = solidColor * vec4(brightness, brightness, brightness, 1.0);
    }
}
