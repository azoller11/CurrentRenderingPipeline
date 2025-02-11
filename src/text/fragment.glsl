#version 330 core

in vec2 TexCoords;
out vec4 FragColor;

uniform sampler2D textAtlas;
uniform vec4 textColor;
uniform vec4 outlineColor;
uniform float edgeSmoothness;
uniform float outlineWidth;

void main() {    
    float distance = texture(textAtlas, TexCoords).r;
    float alpha = smoothstep(0.5 - edgeSmoothness, 0.5 + edgeSmoothness, distance);
    float outline = smoothstep(0.5 - outlineWidth - edgeSmoothness, 
                            0.5 - outlineWidth + edgeSmoothness, distance);
    
    vec4 color = mix(outlineColor, textColor, alpha);
    color.a = alpha + (outline * (1.0 - alpha));
    
    if (color.a < 0.01) discard;
    FragColor = color;
}