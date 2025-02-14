#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;

uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform float bloomIntensity;
uniform float exposure; // Dynamic exposure value passed in

void main() {
    // Sample the scene and bloom colors.
    vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
    vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;
    
    // Combine scene and bloom in HDR.
    vec3 hdrColor = sceneColor + bloomColor * bloomIntensity;
    
    // Apply exposure scaling.
    hdrColor *= exposure;
    
    // Reinhard tone mapping to compress the HDR range.
    vec3 mappedColor = hdrColor / (hdrColor + vec3(1.0));
    
    // Optional gamma correction.
    mappedColor = pow(mappedColor, vec3(1.0 / 2.2));
    
    fragColor = vec4(mappedColor, 1.0);
}
