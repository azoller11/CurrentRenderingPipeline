package shadows;

import entities.Entity;
import entities.Light;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL32;
import org.lwjgl.system.MemoryStack;
import shaders.ShaderProgram;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL32.*;

public class ShadowRenderer {
    private final int shadowWidth;
    private final int shadowHeight;
    private int depthMapFBO;
    private int depthMapTexture;
    private ShaderProgram shadowShader;
    
    // This matrix stores the light's (sun’s) combined projection and view matrix.
    private Matrix4f lightSpaceMatrix = new Matrix4f();

    /**
     * Creates a ShadowRenderer with the given shadow map resolution.
     *
     * @param shadowWidth              Width of the shadow map texture.
     * @param shadowHeight             Height of the shadow map texture.
     * @param shadowVertexShaderPath   Path to your shadow vertex shader.
     * @param shadowFragmentShaderPath Path to your shadow fragment shader.
     */
    public ShadowRenderer(int shadowWidth, int shadowHeight) {
        this.shadowWidth = shadowWidth;
        this.shadowHeight = shadowHeight;
        initFrameBuffer();
        // Create the shadow shader using your existing ShaderProgram class.
        // (Pass null for tessellation and geometry shaders.)
        shadowShader = new ShaderProgram("src/shadows/shadow_vertex.glsl", null, null, null, "src/shadows/shadow_fragment.glsl");
    }

    /**
     * Initializes the framebuffer and depth texture used for shadow mapping.
     */
    private void initFrameBuffer() {
        // Create framebuffer object.
        depthMapFBO = glGenFramebuffers();
        
        // Create depth texture.
        depthMapTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthMapTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, shadowWidth, shadowHeight,
                     0, GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        // Attach the depth texture as the framebuffer's depth buffer.
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, depthMapTexture, 0);
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Shadow framebuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Renders the shadow map by drawing all entities from the sun’s perspective.
     *
     * @param entities List of entities to render into the shadow map.
     * @param sun      The light representing the sun (directional light).
     * @return The OpenGL texture ID of the shadow map.
     */
    public int renderShadowMap(List<Entity> entities, Light sun) {
        // Compute the light space matrix.
        // Here we use an orthographic projection. Adjust the bounds and near/far planes as needed.
        Matrix4f lightProjection = new Matrix4f().ortho(-20, 20, -20, 20,0.1f, 100000.0f);
        Matrix4f lightView = new Matrix4f().lookAt(
                new Vector3f(sun.getPosition()),
                new Vector3f(0.0f, 0.0f, 0.0f), // target; adjust if needed
                new Vector3f(0.0f, 1.0f, 0.0f)
        );
        lightProjection.mul(lightView, lightSpaceMatrix);

        // Bind the framebuffer and set viewport to shadow map size.
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glViewport(0, 0, shadowWidth, shadowHeight);
        glClear(GL_DEPTH_BUFFER_BIT);
        

        // 3. Clear the screen (color and depth buffers)
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // Bind the shadow shader and upload the light space matrix.
        shadowShader.bind();
        shadowShader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);

        // Render each entity into the depth map.
        for (Entity entity : entities) {
            drawEntityShadow(entity);
        }

        shadowShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        

        return depthMapTexture;
    }

    /**
     * Helper method to render an individual entity for the shadow map.
     * This method sets the entity’s model matrix and draws its mesh.
     *
     * @param entity The entity to render.
     */
    private void drawEntityShadow(Entity entity) {
        Matrix4f model = new Matrix4f()
                .translate(entity.getPosition())
                .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)
                .scale(entity.getScale());
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            shadowShader.setUniformMat4("model", false, fb);
        }
        Mesh mesh = entity.getMesh();
        glBindVertexArray(mesh.getVaoId());
        // Render using triangles (shadow shader typically requires minimal attributes)
        glDrawArrays(GL_TRIANGLES, 0, mesh.getVertexCount());
        glBindVertexArray(0);
        
    }

    /**
     * Returns the light space matrix (sun matrix) computed during the last shadow pass.
     *
     * @return The light space (projection * view) matrix.
     */
    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    /**
     * Cleans up the shadow renderer resources.
     */
    public void cleanup() {
        shadowShader.destroy();
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthMapTexture);
    }
}
