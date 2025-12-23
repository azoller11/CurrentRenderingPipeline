#version 400 core

layout(quads, fractional_even_spacing) in;

uniform sampler2D heightMap;
uniform float maxHeight;
uniform mat4 projection;
uniform mat4 view;
uniform mat4 model;
uniform mat4 lightSpaceMatrix;

in VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} tes_in[];

out VS_OUT {
    vec2 uv;
    vec2 baseUV;
    vec3 worldPos;
    vec3 normal;
    vec3 tangent;
    vec4 lightSpacePos;
} tes_out;

vec3 interpolate3(vec3 v0, vec3 v1, vec3 v2, vec3 v3)
{
    vec2 uv = gl_TessCoord.xy;
    return mix(mix(v0, v1, uv.x), mix(v2, v3, uv.x), uv.y);
}

vec2 interpolate2(vec2 v0, vec2 v1, vec2 v2, vec2 v3)
{
    vec2 uv = gl_TessCoord.xy;
    return mix(mix(v0, v1, uv.x), mix(v2, v3, uv.x), uv.y);
}

void main()
{
    // Bilinear interpolation of input vertices
    vec3 pos = interpolate3(
        tes_in[0].worldPos, tes_in[1].worldPos,
        tes_in[2].worldPos, tes_in[3].worldPos
    );

    vec2 uv = interpolate2(
        tes_in[0].uv, tes_in[1].uv,
        tes_in[2].uv, tes_in[3].uv
    );

    vec2 baseUV = interpolate2(
        tes_in[0].baseUV, tes_in[1].baseUV,
        tes_in[2].baseUV, tes_in[3].baseUV
    );

    vec3 normal = normalize(interpolate3(
        tes_in[0].normal, tes_in[1].normal,
        tes_in[2].normal, tes_in[3].normal
    ));

    vec3 tangent = normalize(interpolate3(
        tes_in[0].tangent, tes_in[1].tangent,
        tes_in[2].tangent, tes_in[3].tangent
    ));

    // Height-based displacement
    float heightValue = texture(heightMap, uv).r;
    pos.y = heightValue * maxHeight;

    // Apply model transform
    vec4 world = model * vec4(pos, 1.0);

    // Output
    tes_out.uv       = uv;
    tes_out.baseUV   = baseUV;
    tes_out.worldPos = world.xyz;
    tes_out.normal   = normal;
    tes_out.tangent  = tangent;
    tes_out.lightSpacePos = lightSpaceMatrix * world;

    gl_Position = projection * view * world;
}
