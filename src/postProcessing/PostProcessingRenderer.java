package postProcessing;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import org.lwjgl.system.MemoryUtil;
import shaders.ShaderProgram;

/**
 * This renderer creates an offscreen FBO with multiple color attachments.
 * It provides methods to bind/unbind the FBO for scene rendering, and then
 * to draw a full-screen quad using a post‐processing shader.
 */
public class PostProcessingRenderer {

    // FBO and attachments
    private int fboId;
    private int depthBufferId;
    private int[] colorTextureIds;
    private int width;
    private int height;
    private int numAttachments;
    
    // Full-screen quad
    private int quadVAO;
    private int quadVBO;
    
    // Shader program for post processing
    private ShaderProgram postProcessShader;
    
    /**
     * Creates a post processing renderer.
     *
     * @param width         The width of the FBO (usually the window width).
     * @param height        The height of the FBO.
     * @param numAttachments The number of color attachments (post‐processing textures) to create.
     */
    public PostProcessingRenderer(int width, int height, int numAttachments) {
        this.width = width;
        this.height = height;
        this.numAttachments = numAttachments;
        initFBO();
        initFullScreenQuad();
        initShader();
    }
    
    /**
     * Creates the framebuffer object and its attachments.
     */
    private void initFBO() {
        // Create the FBO and bind it.
        fboId = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        
        // Create color texture attachments.
        colorTextureIds = new int[numAttachments];
        int[] attachments = new int[numAttachments];
        for (int i = 0; i < numAttachments; i++) {
            colorTextureIds[i] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, colorTextureIds[i]);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F , width, height,
                         0, GL_RGBA, GL_UNSIGNED_BYTE, (java.nio.ByteBuffer) null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            // Attach the texture to the framebuffer.
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0 + i,
                                   GL_TEXTURE_2D, colorTextureIds[i], 0);
            attachments[i] = GL_COLOR_ATTACHMENT0 + i;
        }
        
        // Create and attach a renderbuffer for depth.
        depthBufferId = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, depthBufferId);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, depthBufferId);
        
        // Specify the list of color attachments for rendering.
        glDrawBuffers(attachments);
        
        // Check that the framebuffer is complete.
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Framebuffer is not complete!");
        }
        
        // Unbind the FBO so subsequent rendering uses the default framebuffer.
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    /**
     * Creates a full-screen quad VAO/VBO for drawing the post processed texture.
     */
    private void initFullScreenQuad() {
        // The quad covers the full screen (positions range from -1 to 1)
        // Each vertex: [position.x, position.y, texCoord.x, texCoord.y]
        float[] quadVertices = {
            // positions   // texCoords
            -1.0f,  1.0f,  0.0f, 1.0f,
            -1.0f, -1.0f,  0.0f, 0.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
            
            -1.0f,  1.0f,  0.0f, 1.0f,
             1.0f, -1.0f,  1.0f, 0.0f,
             1.0f,  1.0f,  1.0f, 1.0f
        };
        
        quadVAO = glGenVertexArrays();
        quadVBO = glGenBuffers();
        
        glBindVertexArray(quadVAO);
        glBindBuffer(GL_ARRAY_BUFFER, quadVBO);
        glBufferData(GL_ARRAY_BUFFER, quadVertices, GL_STATIC_DRAW);
        
        // Set up the vertex attributes.
        // Position attribute (location = 0)
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        // Texture coordinate attribute (location = 1)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Unbind the VAO and VBO.
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    /**
     * Initializes the post processing shader using your ShaderProgram class.
     * The shader simply samples from a texture (uniform name "screenTexture").
     */
    private void initShader() {
        // Here we use the simple vertex and fragment shader files we provided.
        // (Pass null for any shader stages not used.)
        postProcessShader = new ShaderProgram("src/postProcessing/postProcess_vertex.glsl", 
                                                null, null, null, 
                                                "src/postProcessing/postProcess_fragment.glsl");
        // (If you need additional shader source files, you can use the other constructor.)
    }
    
    /**
     * Bind the FBO so that all subsequent scene rendering goes into the offscreen textures.
     */
    public void bindFBO() {
        glBindFramebuffer(GL_FRAMEBUFFER, fboId);
        glViewport(0, 0, width, height);
    }
    
    /**
     * Unbind the FBO so that subsequent rendering goes to the default framebuffer.
     *
     * @param windowWidth  The window width (to reset the viewport).
     * @param windowHeight The window height.
     */
    public void unbindFBO(int windowWidth, int windowHeight) {
    	 glDisable(GL_DEPTH_TEST);
         glDisable(GL_CULL_FACE);
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight);
    }
    
    /**
     * Renders the full-screen quad using the post processing shader.
     * By default, it binds the texture from the first color attachment.
     * (You can modify this to choose different attachments or add multi-texturing.)
     */
    public void renderPostProcess() {
        // Disable depth testing so the quad covers the whole screen.
        glDisable(GL_DEPTH_TEST);
        
        postProcessShader.bind();
        glBindVertexArray(quadVAO);
        
        // Bind texture unit 0 to the first color attachment.
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, colorTextureIds[0]);
        postProcessShader.setUniformSampler("screenTexture", 0);
        
        postProcessShader.setUniform1f("gamma", 0);
        postProcessShader.setUniform1f("exposure", 0);
        postProcessShader.setUniform1f("vignetteStrength", 0.0f);
        
        // Draw the full-screen quad (6 vertices).
        glDrawArrays(GL_TRIANGLES, 0, 6);
        
        // Unbind and cleanup.
        glBindTexture(GL_TEXTURE_2D, 0);
        glBindVertexArray(0);
        postProcessShader.unbind();
        
        glEnable(GL_DEPTH_TEST);
    }
    
    /**
     * Returns the texture ID for the given post processing attachment index.
     *
     * @param index The index of the color attachment.
     * @return The OpenGL texture ID.
     */
    public int getColorTexture(int index) {
        if (index < 0 || index >= numAttachments) {
            throw new IllegalArgumentException("Invalid texture attachment index");
        }
        return colorTextureIds[index];
    }
    
    /**
     * Cleanup all OpenGL resources.
     */
    public void cleanup() {
        glDeleteFramebuffers(fboId);
        glDeleteRenderbuffers(depthBufferId);
        for (int tex : colorTextureIds) {
            glDeleteTextures(tex);
        }
        glDeleteBuffers(quadVBO);
        glDeleteVertexArrays(quadVAO);
        postProcessShader.destroy();
    }
}
