 #version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform float bloomIntensity;  // Controls how strong the bloom effect is.
void main() {
     vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
     vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;
     fragColor = vec4(sceneColor + bloomColor * bloomIntensity, 1.0);
}