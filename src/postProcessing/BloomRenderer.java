package postProcessing;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.FloatBuffer;

import org.lwjgl.BufferUtils;
import org.lwjgl.system.MemoryUtil;
import shaders.ShaderProgram;
import toolbox.Equations;

/**
 * This bloom renderer creates several FBOs for a multi‐pass bloom post‑processing effect.
 * The steps are:
 *   1. Render the scene into a scene FBO.
 *   2. Extract bright areas (bright pass) into a separate texture.
 *   3. Blur the bright areas using two ping–pong FBOs.
 *   4. Combine the blurred (bloom) texture with the original scene.
 *
 * The necessary shader sources for each pass are provided below as comments.
 */
public class BloomRenderer {
    
    // Dimensions
    private int width;
    private int height;
    
    // FBO for the original scene.
    private int sceneFBO;
    private int sceneTexture;
  

	private int sceneDepthRBO;
    
    // FBO for extracting bright areas.
    private int brightFBO;
    private int brightTexture;
    
    // Two ping-pong FBOs for blurring.
    private int[] pingpongFBO = new int[2];
    private int[] pingpongTexture = new int[2];
    
    // Full-screen quad.
    private int quadVAO;
    private int quadVBO;
    
    private int combineFBO;
    private int combineTexture;
    
    // Shaders:
    private ShaderProgram bloomExtractShader;  // Extract bright regions.
    private ShaderProgram blurShader;            // Blur (takes a uniform vec2 "blurDirection").
    private ShaderProgram bloomCombineShader;    // Combine original scene and bloom.
    
    private ShaderProgram fxaaShader;
    
    
    
    // Number of blur passes.
    private int blurIterations = 2;
    
    public BloomRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        initSceneFBO();
        initBrightFBO();
        initPingPongFBOs();
        initCombineFBO();
        initFullScreenQuad();
        initShaders();
    }
    
    // 1. Create the FBO for rendering the full scene.
    private void initSceneFBO() {
        sceneFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFBO);
        
        // Create the color texture attachment.
        sceneTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        //glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, sceneTexture, 0);
        
        // Create a renderbuffer object for depth.
        sceneDepthRBO = glGenRenderbuffers();
        glBindRenderbuffer(GL_RENDERBUFFER, sceneDepthRBO);
        glRenderbufferStorage(GL_RENDERBUFFER, GL_DEPTH_COMPONENT, width, height);
        glFramebufferRenderbuffer(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_RENDERBUFFER, sceneDepthRBO);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Scene FBO is not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    // 2. Create the FBO for the bright pass.
    private void initBrightFBO() {
        brightFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, brightFBO);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        brightTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, brightTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, brightTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Bright pass FBO is not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    private void initCombineFBO() {
        combineFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, combineFBO);
        
        combineTexture = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, combineTexture);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, combineTexture, 0);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("ERROR: Combine FBO is not complete!");
        }
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    // 3. Create two ping-pong FBOs for blurring.
    private void initPingPongFBOs() {
        for (int i = 0; i < 2; i++) {
            pingpongFBO[i] = glGenFramebuffers();
            pingpongTexture[i] = glGenTextures();
            glBindTexture(GL_TEXTURE_2D, pingpongTexture[i]);
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0, GL_RGBA, GL_FLOAT, (java.nio.ByteBuffer)null);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
            glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
            glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO[i]);
            glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, pingpongTexture[i], 0);
            
            if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
                System.err.println("ERROR: Ping-pong FBO " + i + " is not complete!");
            }
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }
    
    // 4. Create a full-screen quad.
    private void initFullScreenQuad() {
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
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 4 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 4 * Float.BYTES, 2 * Float.BYTES);
        glEnableVertexAttribArray(1);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
    
    // 5. Load and initialize the shaders.
    private void initShaders() {
        // Load the bloom extraction shader.
        bloomExtractShader = new ShaderProgram("src/postProcessing/postProcess_vertex.glsl", 
                                                 null, null, null, 
                                                 "src/postProcessing/bloomExtract_fragment.glsl");
        
        // Load the blur shader.
        blurShader = new ShaderProgram("src/postProcessing/postProcess_vertex.glsl",
                                       null, null, null,
                                       "src/postProcessing/blur_fragment.glsl");
        
        // Load the bloom combine shader.
        bloomCombineShader = new ShaderProgram("src/postProcessing/postProcess_vertex.glsl",
                                                 null, null, null,
                                                 "src/postProcessing/bloomCombine_fragment.glsl");
        
        fxaaShader = new ShaderProgram(
        	    "src/postProcessing/postProcess_vertex.glsl",
        	    null, null, null,
        	    "src/postProcessing/fxaa_fragment.glsl"
        	);
    }
    
    // Bind the scene FBO for rendering your scene.
    public void bindSceneFBO() {
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFBO);
        glClearColor(0.0f, 0.0f, 0.0f, 1.0f); // Ensure black background
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);

        
    }
    
    // Unbind and switch back to default framebuffer.
    public void unbindSceneFBO(int windowWidth, int windowHeight) {
    	glDisable(GL_DEPTH_TEST);
        glDisable(GL_CULL_FACE);
       glBindFramebuffer(GL_FRAMEBUFFER, 0);
       glViewport(0, 0, windowWidth, windowHeight);
    }
    
    /**
     * Perform the full bloom post-processing.
     * First, extract bright regions from the scene into the brightTexture.
     * Then, apply several blur passes using ping-pong FBOs.
     * Finally, combine the blurred bloom with the original scene.
     *
     * @param windowWidth  The final viewport width.
     * @param windowHeight The final viewport height.
     * @param threshold    The brightness threshold for bloom extraction.
     * @param bloomIntensity The intensity for combining bloom.
     */
    float averageBrightness = 0;
    
    private float currentExposure = 1.0f;
    private float currentGamma = 1.0f;
    float smoothingFactor = 0.1f; 
    
    public void renderBloom(int windowWidth, int windowHeight, float threshold, float bloomIntensity) {
        // Disable depth testing for 2D post-processing
        glDisable(GL_DEPTH_TEST);
        
        // 1. Extract bright areas.
        glBindFramebuffer(GL_FRAMEBUFFER, brightFBO);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);
        
        bloomExtractShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        bloomExtractShader.setUniformSampler("sceneTexture", 0);
        bloomExtractShader.setUniform1f("threshold", threshold);
        
        renderQuad();
        bloomExtractShader.unbind();
        
        // 2. Blur the bright texture using ping-pong FBOs.
        boolean horizontal = true;
        boolean firstIteration = true;
        int lastTexture = brightTexture;

        blurShader.bind();
        for (int i = 0; i < blurIterations; i++) {
            int targetFboIndex = horizontal ? 1 : 0;
            int sourceTex = firstIteration ? brightTexture : pingpongTexture[1 - targetFboIndex];

            glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO[targetFboIndex]);
            glViewport(0, 0, width, height);
            glClear(GL_COLOR_BUFFER_BIT);

            blurShader.setUniform2f("blurDirection", horizontal ? 1.0f : 0.0f, horizontal ? 0.0f : 1.0f);

            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, sourceTex);
            blurShader.setUniformSampler("image", 0);

            renderQuad();

            firstIteration = false;
            lastTexture = pingpongTexture[targetFboIndex];
            horizontal = !horizontal;
        }
        blurShader.unbind();
        
        // 3. Calculate auto-exposure BEFORE rendering to combine FBO
        // This prevents modifying the texture while it's being used
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        glGenerateMipmap(GL_TEXTURE_2D);
        
        FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(4);
        int mipLevel = (int) (Math.log(Math.max(width, height)) / Math.log(2));
        glGetTexImage(GL_TEXTURE_2D, mipLevel, GL_RGBA, GL_FLOAT, pixelBuffer);
        
        float r = pixelBuffer.get(0);
        float g = pixelBuffer.get(1);
        float b = pixelBuffer.get(2);
        
        if (!Float.isNaN(r) && !Float.isNaN(g) && !Float.isNaN(b)) {
            averageBrightness = r * 0.2126f + g * 0.7152f + b * 0.0722f;
        }
        
        // Auto-exposure calculation
        float brightnessThresholdLow = 0.05f;
        float brightnessThresholdHigh = 0.9f;
        float t = smoothStep(brightnessThresholdLow, brightnessThresholdHigh, averageBrightness);
        
        float minExposure = 0.5f;
        float maxExposure = 2.8f;
        float dynamicExposure = maxExposure * (1.0f - t) + minExposure * t;
        
        float minGamma = 0.8f;
        float maxGamma = 2.2f;
        float dynamicGamma = maxGamma * (1.0f - t) + minGamma * t;
        
        currentExposure += (dynamicExposure - currentExposure) * smoothingFactor;
        currentGamma += (dynamicGamma - currentGamma) * smoothingFactor;
        
        // Fixed exposure for testing (comment out if you want auto-exposure)
        currentExposure = 3.05f;
        
        
        
        // 4. Combine the original scene with the blurred bloom texture.
        glBindFramebuffer(GL_FRAMEBUFFER, combineFBO);
        glViewport(0, 0, width, height);
        glClear(GL_COLOR_BUFFER_BIT);
        
        bloomCombineShader.bind();
        
        // Bind scene texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, sceneTexture);
        bloomCombineShader.setUniformSampler("sceneTexture", 0);
        
        // Bind bloom texture
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, lastTexture);
        bloomCombineShader.setUniformSampler("bloomTexture", 1);
        
        // Set uniforms
        bloomCombineShader.setUniform1f("gamma", 0); // dynamicGamma if enabled
        bloomCombineShader.setUniform1f("exposure", currentExposure);
        bloomCombineShader.setUniform1f("vignetteStrength", 0.0f);
        bloomCombineShader.setUniform1f("bloomIntensity", bloomIntensity);
        
        renderQuad();
        bloomCombineShader.unbind();
        
        // 5. FXAA PASS - apply anti-aliasing to the combined result
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glViewport(0, 0, windowWidth, windowHeight);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
        fxaaShader.bind();
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, combineTexture);
        
        fxaaShader.setUniform1f("fxaaQuality", 0.0f); // Adjust between 0.0 (sharp) and 1.0 (smooth)

        
        fxaaShader.setUniformSampler("sceneTexture", 0);
        fxaaShader.setUniform2f("resolution", 1.0f / windowWidth, 1.0f / windowHeight);
        
        renderQuad();
        fxaaShader.unbind();
        
        // Re-enable depth testing for 3D rendering
        glEnable(GL_DEPTH_TEST);
    }
    // Utility method to render the full-screen quad.
    private void renderQuad() {
        glBindVertexArray(quadVAO);
        glDrawArrays(GL_TRIANGLES, 0, 6);
        glBindVertexArray(0);
    }
    
    public int getSceneTexture() {
  		return sceneTexture;
  	}

  	public void setSceneTexture(int sceneTexture) {
  		this.sceneTexture = sceneTexture;
  	}
  	
 // Define a smoothstep helper (or use your own math library)
  	private float smoothStep(float edge0, float edge1, float x) {
  	    float t = Math.max(0.0f, Math.min(1.0f, (x - edge0) / (edge1 - edge0)));
  	    return t * t * (3 - 2 * t);
  	}

    
    // Cleanup all resources.
    public void cleanup() {
        glDeleteFramebuffers(sceneFBO);
        glDeleteFramebuffers(brightFBO);
        for (int fbo : pingpongFBO) {
            glDeleteFramebuffers(fbo);
        }
        glDeleteTextures(sceneTexture);
        glDeleteTextures(brightTexture);
        for (int tex : pingpongTexture) {
            glDeleteTextures(tex);
        }
        glDeleteBuffers(quadVBO);
        glDeleteVertexArrays(quadVAO);
        bloomExtractShader.destroy();
        blurShader.destroy();
        bloomCombineShader.destroy();
        fxaaShader.destroy();
        
        glDeleteFramebuffers(combineFBO);
        glDeleteTextures(combineTexture);

    }
}