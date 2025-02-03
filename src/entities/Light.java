package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL13;

import java.nio.ByteBuffer;
import static org.lwjgl.opengl.GL40.*;

public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; // (constant, linear, quadratic)
    
    private boolean castShadow;

    // Fields for point light shadow mapping
    private int depthCubeMap;  // OpenGL texture ID for the depth cube map
    private int depthMapFBO;   // Framebuffer object ID

    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
        initShadowMap(1024,1024);
    }

    public Light(Vector3f direction, Vector3f color) {
        this.position = new Vector3f(direction);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(1, 0, 0);
        initShadowMap(1024,1024);
    }

    // Getter and setter methods
    public Vector3f getPosition() { return position; }
    public Vector3f getColor() { return color; }
    public Vector3f getAttenuation() { return attenuation; }
    public void setPosition(Vector3f newPosition) { this.position.set(newPosition); }
    public void setColor(Vector3f lerp) { this.color.set(lerp); }

    /**
     * Initializes the framebuffer and cube map texture used for shadow mapping.
     *
     * @param shadowWidth  The width of each face of the cube map.
     * @param shadowHeight The height of each face of the cube map.
     */
    public void initShadowMap(int shadowWidth, int shadowHeight) {
        // Generate FBO and cube map texture
        depthMapFBO = glGenFramebuffers();
        depthCubeMap = glGenTextures();
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCubeMap);
        for (int i = 0; i < 6; i++) {
            // Create a texture for each face of the cube map
            glTexImage2D(GL_TEXTURE_CUBE_MAP_POSITIVE_X + i, 0, GL_DEPTH_COMPONENT,
                         shadowWidth, shadowHeight, 0, GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        }
        // Set texture parameters
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_CUBE_MAP, GL_TEXTURE_WRAP_R, GL_CLAMP_TO_EDGE);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);

        // Bind the cube map texture to the FBO's depth attachment
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthCubeMap, 0);
        // We are not using color data
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Shadow map framebuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Returns an array of 6 view-projection matrices for the point light’s cube map.
     * These matrices are used to render the scene from the light's point of view in all 6 directions.
     *
     * @param nearPlane The near plane distance for the projection.
     * @param farPlane  The far plane distance for the projection.
     * @return An array of 6 combined view-projection matrices.
     */
    public Matrix4f[] getShadowMatrices(float nearPlane, float farPlane) {
        // 90° field-of-view and aspect ratio 1:1 for cube faces
        Matrix4f projection = new Matrix4f().perspective((float)Math.toRadians(90.0f), 1.0f, nearPlane, farPlane);
        Matrix4f[] shadowTransforms = new Matrix4f[6];

        // +X face
        shadowTransforms[0] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(1.0f, 0.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f))
                        .mul(projection, shadowTransforms[0]);
        // -X face
        shadowTransforms[1] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(-1.0f, 0.0f, 0.0f), new Vector3f(0.0f, -1.0f, 0.0f))
                        .mul(projection, shadowTransforms[1]);
        // +Y face
        shadowTransforms[2] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(0.0f, 1.0f, 0.0f), new Vector3f(0.0f, 0.0f, 1.0f))
                        .mul(projection, shadowTransforms[2]);
        // -Y face
        shadowTransforms[3] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(0.0f, -1.0f, 0.0f), new Vector3f(0.0f, 0.0f, -1.0f))
                        .mul(projection, shadowTransforms[3]);
        // +Z face
        shadowTransforms[4] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(0.0f, 0.0f, 1.0f), new Vector3f(0.0f, -1.0f, 0.0f))
                        .mul(projection, shadowTransforms[4]);
        // -Z face
        shadowTransforms[5] = new Matrix4f();
        new Matrix4f().lookAt(position, new Vector3f(position).add(0.0f, 0.0f, -1.0f), new Vector3f(0.0f, -1.0f, 0.0f))
                        .mul(projection, shadowTransforms[5]);

        return shadowTransforms;
    }
    
    public int getFaceAs2DTexture(int face, int width, int height) {
        // Compute the target for the desired face.
        int faceTarget = GL_TEXTURE_CUBE_MAP_POSITIVE_X + face;

        // Create a new 2D texture.
        int texId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, texId);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        // Allocate storage for the 2D texture.
        // Adjust the internal format as needed; here we assume RGBA8 (8-bit per channel).
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);

        // Bind the cube map texture.
        glBindTexture(GL_TEXTURE_CUBE_MAP, depthCubeMap);
        // Create a buffer to hold the pixel data.
        // (Assuming your custom depth shader outputs RGBA data.)
        ByteBuffer pixels = BufferUtils.createByteBuffer(width * height * 4);
        // Read the pixel data from the cube map face.
        glGetTexImage(faceTarget, 0, GL_RGBA, GL_UNSIGNED_BYTE, pixels);
        // Update the 2D texture with the data from the cube map face.
        glTexSubImage2D(GL_TEXTURE_2D, 0, 0, 0, width, height, GL_RGBA, GL_UNSIGNED_BYTE, pixels);

        // Unbind the textures.
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindTexture(GL_TEXTURE_CUBE_MAP, 0);
        System.out.println(texId);
        return texId;
    }
    

    
    public int getCubeMapFaceTarget(int face) {
        // Valid face indices: 0 to 5.
        return GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face;
    }

    // Getters for the shadow mapping resources
    public int getDepthCubeMap() {
        return depthCubeMap;
    }

    public int getDepthMapFBO() {
        return depthMapFBO;
    }

	public boolean isCastShadow() {
		return castShadow;
	}

	public void setCastShadow(boolean castShadow) {
		this.castShadow = castShadow;
	}
}
