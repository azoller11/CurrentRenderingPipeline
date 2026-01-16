#version 330 core

layout(location = 0) in vec2 position;   // pixel space
layout(location = 1) in vec2 texCoords;

out vec2 vUV;
out vec2 vScreenPx;

uniform mat4 projectionMatrix;

/* Animation */
uniform float time;              // seconds
uniform float waveAmplitudePx;   // e.g. 6.0
uniform float waveFrequency;     // e.g. 8.0 (waves across screen)
uniform float jitterPx;
uniform vec2 screenSize;

float hash12(vec2 p) {
    return fract(sin(dot(p, vec2(12.9898, 78.233))) * 43758.5453);
}

void main() {
    vec2 pos = position;

    /* ---------------------------------
       RuneScape-style sine wave
       y = sin(time + x * frequency)
    --------------------------------- */
    if (waveAmplitudePx > 0.0) {
        // Normalize X so wave spacing is resolution-independent
        float xNorm = pos.x / max(screenSize.x, 1.0);

        // 2π ensures full sine cycles
        float phase = (xNorm * waveFrequency * 6.2831853) + time;

        pos.y += sin(phase) * waveAmplitudePx;
    }

    /* ---------------------------------
       Optional jitter (unchanged)
    --------------------------------- */
    if (jitterPx > 0.0) {
        float r1 = hash12(pos + time);
        float r2 = hash12(pos + time * 2.0 + 17.0);
        pos += (vec2(r1, r2) - 0.5) * 2.0 * jitterPx;
    }

    vUV = texCoords;
    vScreenPx = pos;

    gl_Position = projectionMatrix * vec4(pos, 0.0, 1.0);
}
