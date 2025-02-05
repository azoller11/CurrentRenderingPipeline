 #version 400 core
in vec2 passTexCoords;
out vec4 fragColor;
uniform sampler2D image;
uniform vec2 blurDirection;  // (1.0, 0.0) for horizontal, (0.0, 1.0) for vertical.
   void main() {
    float offset = 1.0 / 300.0; // Adjust for texture resolution.
   vec3 result = texture(image, passTexCoords).rgb * 0.227027;
    result += texture(image, passTexCoords + blurDirection * offset * 1.384615).rgb * 0.316216;
   result += texture(image, passTexCoords - blurDirection * offset * 1.384615).rgb * 0.316216;
    result += texture(image, passTexCoords + blurDirection * offset * 3.230769).rgb * 0.070270;
  result += texture(image, passTexCoords - blurDirection * offset * 3.230769).rgb * 0.070270;
  fragColor = vec4(result, 1.0);
 }