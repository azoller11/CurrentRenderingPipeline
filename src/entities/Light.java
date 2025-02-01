package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import shadows.ShadowMapUtils;

import static org.lwjgl.opengl.GL40.*;

public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; 
    
    private boolean castShadows;
    private int shadowMapFBO;
    private int shadowMapTexture;
    
    // For debugging: an array of 6 texture IDs for each cube map face.
    private int[] debugCubeFaceTextures = new int[6];
    
    // For point-light cube map shadow mapping, we store near and far plane values.
    private float nearPlane = 1.0f;
    private float farPlane = 100.0f;

    // You can keep this if you use directional/spot-light shadows.
    private Matrix4f lightSpaceMatrix;

    // Shadow map dimensions (each face of the cube is square)
    public static final int SHADOW_SIZE = 2048;

    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
        this.castShadows = true;
        initializeShadowMapping();
    }

    public Light(Vector3f direction, Vector3f color) {
        // This constructor can be used for directional lights, but here we assume point lights.
        this.position = new Vector3f(direction);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(1, 0, 0);
        this.castShadows = true;
        initializeShadowMapping();
    }

    private void initializeShadowMapping() {
        // Create the depth framebuffer.
        shadowMapFBO = ShadowMapUtils.createDepthFramebuffer(SHADOW_SIZE, SHADOW_SIZE);

        // Create a cube map depth texture.
        shadowMapTexture = ShadowMapUtils.createCubeDepthTexture(SHADOW_SIZE);

        // Bind the framebuffer and attach the cube map texture.
        glBindFramebuffer(GL_FRAMEBUFFER, shadowMapFBO);
        // Attach the entire cube map to the depth attachment.
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, shadowMapTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Shadow Framebuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);

        // For debugging, create 6 2D depth textures (one for each cube face).
        for (int i = 0; i < 6; i++) {
            debugCubeFaceTextures[i] = ShadowMapUtils.createDepthTexture(SHADOW_SIZE, SHADOW_SIZE);
        }

        // For directional or spot lights you might compute a single lightSpaceMatrix.
        // For point lights (cube mapping), you'll compute six view-projection matrices per render.
        // If needed, you can still initialize lightSpaceMatrix here:
        lightSpaceMatrix = new Matrix4f();
    }

    // Getters for the shadow map resources.
    public int getShadowMapFBO() {
        return shadowMapFBO;
    }

    public int getShadowMapTexture() {
        return shadowMapTexture;
    }

    /**
     * Returns the near plane value used for shadow mapping.
     */
    public float getNearPlane() {
        return nearPlane;
    }

    /**
     * Returns the far plane value used for shadow mapping.
     */
    public float getFarPlane() {
        return farPlane;
    }

    // Optionally keep these if you also use directional or spot-light shadows.
    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    public void setLightSpaceMatrix(Matrix4f lightSpaceMatrix) {
        this.lightSpaceMatrix = lightSpaceMatrix;
    }

    // Getter for the debug textures array.
    public int[] getDebugCubeFaceTextures() {
        return debugCubeFaceTextures;
    }

    // Existing getters and setters.
    public Vector3f getPosition() { 
        return position; 
    }

    public Vector3f getColor() { 
        return color; 
    }

    public Vector3f getAttenuation() { 
        return attenuation; 
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition); // Modify in place.
    }

    public void setColor(Vector3f newColor) {
        this.color.set(newColor);
    }

    public boolean isCastShadows() {
        return this.castShadows;
    }
}
