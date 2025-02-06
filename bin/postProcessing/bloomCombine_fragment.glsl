#version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D sceneTexture;
uniform sampler2D bloomTexture;
uniform float bloomIntensity;
//uniform float exposure;  // Add this uniform (default to 1.0)

void main() {
    vec3 sceneColor = texture(sceneTexture, passTexCoords).rgb;
    vec3 bloomColor = texture(bloomTexture, passTexCoords).rgb;
    
    // Combine scene and bloom in HDR
    //vec3 hdrColor = sceneColor + bloomColor * bloomIntensity;
    
    // Apply exposure scaling to HDR values
   // hdrColor *= exposure;
    
    // Reinhard tone mapping (adjusts for overbright areas)
   // vec3 mapped = hdrColor / (hdrColor + vec3(1.0));
    
    // Gamma correction (optional: only apply if your scene isn't already gamma-corrected)
    //mapped = pow(mapped, vec3(1.0 / 2.2));
    
   // fragColor = vec4(mapped, 1.0);
    
    
    sceneColor = texture(sceneTexture, passTexCoords).rgb;
	bloomColor = texture(bloomTexture, passTexCoords).rgb;
	fragColor = vec4(sceneColor + bloomColor * bloomIntensity, 1.0);
}