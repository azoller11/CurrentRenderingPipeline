#version 400 core
in vec2 texCoords; // assume this comes from your quad's vertex shader (range [0,1])
out vec4 fragColor;

uniform samplerCube depthCubeMap;
uniform int faceIndex; // which cube face to display (0 to 5)

// Converts the 2D texture coordinates into a 3D direction vector for the given cube face.
vec3 computeDirection(int face, vec2 uv) {
    // remap uv from [0,1] to [-1,1]
    uv = uv * 2.0 - 1.0;
    if(face == 0) { // +X
        return vec3(1.0, -uv.y, -uv.x);
    } else if(face == 1) { // -X
        return vec3(-1.0, -uv.y, uv.x);
    } else if(face == 2) { // +Y
        return vec3(uv.x, 1.0, uv.y);
    } else if(face == 3) { // -Y
        return vec3(uv.x, -1.0, -uv.y);
    } else if(face == 4) { // +Z
        return vec3(uv.x, -uv.y, 1.0);
    } else if(face == 5) { // -Z
        return vec3(-uv.x, -uv.y, -1.0);
    }
    return vec3(0.0);
}

void main() {
    // Compute the direction based on the face index and UV coordinates.
    vec3 direction = computeDirection(faceIndex, texCoords);
    // Sample the cube map.
    // (If your cube map is a depth texture, the sampled value will be the depth.)
    float depthValue = texture(depthCubeMap, direction).r;
    
    // Optionally, remap the depth value to a visible range.
    // Here, we simply output a grayscale color.
    fragColor = vec4(vec3(depthValue), 1.0);
}
