#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D image;
uniform vec2 blurDirection;

void main() {
    vec2 texSize = textureSize(image, 0);
    vec2 texelSize = 1.0 / texSize;
    vec2 offset = blurDirection * texelSize;

    // Gaussian weights for blur
    vec3 result = texture(image, passTexCoords).rgb * 0.227027;
    result += texture(image, passTexCoords + offset * 1.384615).rgb * 0.316216;
    result += texture(image, passTexCoords - offset * 1.384615).rgb * 0.316216;
    result += texture(image, passTexCoords + offset * 3.230769).rgb * 0.070270;
    result += texture(image, passTexCoords - offset * 3.230769).rgb * 0.070270;
    
    fragColor = vec4(result, 1.0);
}