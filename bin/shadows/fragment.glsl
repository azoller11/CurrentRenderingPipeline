// File: src/shadows/fragment.glsl
#version 330 core

in vec2 passTexCoord;
out vec4 FragColor;

uniform sampler2D diffuseMap;   // The texture sampler
uniform float alphaThreshold;   // Threshold below which fragments are discarded

void main() {
    // Sample the texture at the given coordinates.
    vec4 texColor = texture(diffuseMap, passTexCoord);
    
    // Discard fragments that are too transparent.
    if(texColor.a < alphaThreshold)
        discard;
    
    // For the shadow map pass you might simply output white (or
    // any constant value) so that the depth is recorded correctly.
    FragColor = vec4(1.0);
}
