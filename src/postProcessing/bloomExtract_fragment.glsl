#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D sceneTexture;
uniform float threshold;

void main() {
    vec3 color = texture(sceneTexture, passTexCoords).rgb;
    float brightness = dot(color, vec3(0.2126, 0.7152, 0.0722));
    
    // Soft threshold with smoothstep
    float softEdge = 0.05; // Adjust for smoother transition
    float factor = smoothstep(threshold - softEdge, threshold + softEdge, brightness);
    fragColor = vec4(color * factor, 1.0);
}