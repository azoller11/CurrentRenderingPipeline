#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D sceneTexture;
uniform float threshold;  // e.g., 1.0
void main() {
   vec3 color = texture(sceneTexture, passTexCoords).rgb;
    float brightness = dot(color, vec3(0.2126, 0.7152, 0.0722));
    if (brightness > threshold)
        fragColor = vec4(color, 1.0);
   else
          fragColor = vec4(0.0);
 }