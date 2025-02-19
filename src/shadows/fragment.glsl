#version 330 core
in vec2 passTexCoord;
out vec4 FragColor;

uniform sampler2D diffuseMap;
uniform float alphaThreshold;
uniform bool useTexture; // true if texture might have transparency

void main() {
    vec4 texColor = vec4(1.0); // default opaque white
    if(useTexture) {
        texColor = texture(diffuseMap, passTexCoord);
        if(texColor.a < alphaThreshold)
            discard;
    }
    vec3 shadowColor = texColor.rgb * texColor.a;
    FragColor = vec4(shadowColor, 1.0);
}
