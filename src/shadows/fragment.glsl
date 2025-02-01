#version 330 core

// Optionally, you can keep these uniforms if you want to use them in more advanced depth calculations.
uniform float farPlane; 

// Output color
out vec4 FragColor;

void main()
{
    // Retrieve the depth value (non-linear, in [0, 1]) from gl_FragCoord.
    // When nothing is rendered, the depth is 1.0 (clear value) and will be red.
    // When geometry is close to the light (in shadow), the depth is near 0.0 and will appear black.
    float depth = gl_FragCoord.z;

    // Output the depth in the red channel.
    FragColor = vec4(depth, 0.0, 0.0, 1.0);
}
