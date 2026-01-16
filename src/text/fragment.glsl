#version 330 core

in vec2 vUV;
in vec2 vScreenPx;
out vec4 FragColor;

uniform sampler2D textAtlas;

/* MUST be set from Java */
uniform int sdfChannel;   // 3 = alpha
uniform int invertSdf;    // 0 or 1

/* Core */
uniform vec4 textColor;

/* Outline (first ring outside glyph) */
uniform vec4 outlineColor;
uniform float outlineWidth; // SDF units

/* Border (second ring outside outline) */
uniform vec4 borderColor;
uniform float borderWidth;      // SDF units (additional thickness)
uniform float borderSoftness;   // 0..2-ish multiplier

/* Shadow */
uniform vec4 shadowColor;       // alpha=0 disables
uniform vec2 shadowOffsetPx;    // pixels
uniform float shadowSoftness;   // 0..2-ish multiplier

/* Glow */
uniform vec4 glowColor;
uniform float glowStrength;     // 0 = off
uniform float glowRadius;       // SDF units

/* Tint */
uniform vec4 tintTop;
uniform vec4 tintBottom;
uniform float tintMix;
uniform vec2 screenSize;

float sampleSdf(vec2 uv) {
    vec4 s = texture(textAtlas, uv);
    if (sdfChannel == 0) return s.r;
    if (sdfChannel == 1) return s.g;
    if (sdfChannel == 2) return s.b;
    return s.a;
}

/* Standard fill alpha: inside is d > 0.5 */
float sdfFill(float d, float w) {
    return smoothstep(0.5 - w, 0.5 + w, d);
}

/* Outside ring between [0.5 - a] and [0.5 - b] (a > b >= 0) */
float sdfOutsideBand(float d, float w, float a, float b, float softnessMul) {
    // a = outer thickness, b = inner thickness
    // outside region uses reversed smoothstep
    float outer = smoothstep(0.5 + w, 0.5 - w, d);
    float w2 = w * max(1.0, 1.0 + softnessMul * 2.0);

    float inner = smoothstep(
        (0.5 - a) + w2,
        (0.5 - a) - w2,
        d
    );

    // If b > 0, carve out inside portion of the band
    if (b > 0.0) {
        float carve = smoothstep(
            (0.5 - b) + w2,
            (0.5 - b) - w2,
            d
        );
        // band is outside up to b, so keep only between b..a
        return clamp((outer - inner) - (outer - carve), 0.0, 1.0);
    }

    // band from edge..a
    return clamp(outer - inner, 0.0, 1.0);
}

void main() {
    // Sample + optional invert
    float d = sampleSdf(vUV);
    if (invertSdf == 1) d = 1.0 - d;

    // Screen-space AA
    float w = max(fwidth(d), 0.0005);

    // Fill
    float fill = sdfFill(d, w);

    // Outline: edge .. outlineWidth
    float outline = 0.0;
    if (outlineWidth > 0.0 && outlineColor.a > 0.0) {
        float outer = smoothstep(0.5 + w, 0.5 - w, d);
        float inner = smoothstep(
            (0.5 - outlineWidth) + w,
            (0.5 - outlineWidth) - w,
            d
        );
        outline = clamp(outer - inner, 0.0, 1.0);
    }

    // Border: outside outline by borderWidth
    float border = 0.0;
    if (borderWidth > 0.0 && borderColor.a > 0.0) {
        float a = outlineWidth + borderWidth; // total outer thickness
        float b = outlineWidth;               // start after outline
        // use a slightly softer edge if requested
        float wB = w * max(1.0, 1.0 + borderSoftness * 2.0);

        float outer = smoothstep(0.5 + wB, 0.5 - wB, d);
        float innerA = smoothstep((0.5 - a) + wB, (0.5 - a) - wB, d);
        float innerB = smoothstep((0.5 - b) + wB, (0.5 - b) - wB, d);

        // band between b..a
        border = clamp((outer - innerA) - (outer - innerB), 0.0, 1.0);
    }

    // Glow: outside edge, extends by glowRadius
    float glow = 0.0;
    if (glowStrength > 0.0 && glowRadius > 0.0 && glowColor.a > 0.0) {
        // outside distance amount: (0.5 - d) is positive outside
        float outside = 0.5 - d;
        float wG = w * 2.0;
        glow = 1.0 - smoothstep(0.0, glowRadius + wG, outside);
        glow *= (1.0 - fill); // don't glow over solid fill
        glow *= glowStrength;
    }

    // Shadow: sample shifted UV using derivatives (stable)
    float shadow = 0.0;
    if (shadowColor.a > 0.001) {
        vec2 uvDx = dFdx(vUV);
        vec2 uvDy = dFdy(vUV);
        vec2 uvPerPx = vec2(length(uvDx), length(uvDy));
        vec2 uvOff = shadowOffsetPx * uvPerPx;

        float ds = sampleSdf(vUV + uvOff);
        if (invertSdf == 1) ds = 1.0 - ds;

        float ws = max(fwidth(ds), 0.0005);
        ws *= max(1.0, 1.0 + shadowSoftness * 2.0);

        float sh = sdfFill(ds, ws);
        shadow = sh * (1.0 - fill); // only behind
    }

    // Tint gradient
    float t = clamp(vScreenPx.y / max(screenSize.y, 1.0), 0.0, 1.0);
    vec4 tint = mix(tintBottom, tintTop, t);

    vec4 fillCol = mix(textColor, textColor * tint, tintMix);
    vec4 outCol  = mix(outlineColor, outlineColor * tint, tintMix);
    vec4 borCol  = mix(borderColor, borderColor * tint, tintMix);
    vec4 gloCol  = mix(glowColor, glowColor * tint, tintMix);
    vec4 shaCol  = mix(shadowColor, shadowColor * tint, tintMix);

    // Coverage determines discard (prevents quad showing)
    float coverage = max(max(fill, outline), max(border, max(glow, shadow)));
    if (coverage < 0.01) discard;

    // Compose back -> front
    vec3 rgb = vec3(0.0);
    float a = 0.0;

    // Glow
    if (glow > 0.0) {
        rgb = mix(rgb, gloCol.rgb, glow * gloCol.a);
        a = max(a, glow * gloCol.a);
    }

    // Shadow
    if (shadow > 0.0) {
        rgb = mix(rgb, shaCol.rgb, shadow * shaCol.a);
        a = max(a, shadow * shaCol.a);
    }

    // Border
    if (border > 0.0) {
        rgb = mix(rgb, borCol.rgb, border);
        a = max(a, border * borCol.a);
    }

    // Outline
    if (outline > 0.0) {
        rgb = mix(rgb, outCol.rgb, outline);
        a = max(a, outline * outCol.a);
    }

    // Fill
    if (fill > 0.0) {
        rgb = mix(rgb, fillCol.rgb, fill);
        a = max(a, fill * fillCol.a);
    }

    FragColor = vec4(rgb, a);
}
