package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL40.*;
import shadows.ShadowMapUtils;

/**
 * Enhanced Light definition with shadow mapping.
 */
public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; // (constant, linear, quadratic)

    // Shadow mapping properties
    private int shadowMapFBO;
    private int shadowMapTexture;
    private Matrix4f lightSpaceMatrix;

    // Shadow map dimensions
    public static final int SHADOW_WIDTH = 2048;
    public static final int SHADOW_HEIGHT = 2048;

    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
        initializeShadowMapping();
    }

    public Light(Vector3f direction, Vector3f color) {
        this.position = new Vector3f(direction);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(1, 0, 0); // indicates directional
        initializeShadowMapping();
    }

    private void initializeShadowMapping() {
        // Create the depth framebuffer and texture using ShadowMapUtils
        shadowMapFBO = ShadowMapUtils.createDepthFramebuffer(SHADOW_WIDTH, SHADOW_HEIGHT);
        shadowMapTexture = ShadowMapUtils.createDepthTexture(SHADOW_WIDTH, SHADOW_HEIGHT);

        // Attach depth texture to framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, shadowMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, shadowMapTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Shadow Framebuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
     // Perspective projection for point lights
        float aspect = (float) SHADOW_WIDTH / SHADOW_HEIGHT;
        Matrix4f lightProjection = new Matrix4f().perspective((float)Math.toRadians(90.0f), aspect, 1.0f, 100.0f);
        Matrix4f lightView = new Matrix4f().lookAt(position, new Vector3f(0, 0, 0), new Vector3f(0, 1, 0));
        lightProjection.mul(lightView, lightSpaceMatrix = new Matrix4f());
/*
        // Initialize light projection and view matrices
        if (isDirectional()) {
            // Orthographic projection for directional lights (e.g., sun)
            Matrix4f lightProjection = new Matrix4f().ortho(-50, 50, -50, 50, 1.0f, 100f);
            Vector3f lightDir = new Vector3f(position).negate(); // assuming 'position' stores direction
            Matrix4f lightView = new Matrix4f().lookAt(
                new Vector3f(0, 0, 0),               // Position the "camera" at origin
                lightDir.add(new Vector3f(0, 0, 0)), // Look in the direction of the light
                new Vector3f(0, 1, 0)                // Up vector
            );
            lightProjection.mul(lightView, lightSpaceMatrix = new Matrix4f());
        } else {
            
        }
        */
    }

    /**
     * Determines if the light is directional based on its attenuation.
     *
     * @return True if directional, false otherwise.
     */
    public boolean isDirectional() {
        return attenuation.x == 1.0f && attenuation.y == 0 && attenuation.z == 0;
    }

    // Getters for shadow mapping
    public int getShadowMapFBO() {
        return shadowMapFBO;
    }

    public int getShadowMapTexture() {
        return shadowMapTexture;
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    // Existing getters
    public Vector3f getPosition() { return position; }
    public Vector3f getColor() { return color; }
    public Vector3f getAttenuation() { return attenuation; }
}
