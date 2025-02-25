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
    
    // Number of blur passes.
    private int blurIterations = 12;
    
    public BloomRenderer(int width, int height) {
        this.width = width;
        this.height = height;
        initSceneFBO();
        initBrightFBO();
        initPingPongFBOs();
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
    }
    
    // Bind the scene FBO for rendering your scene.
    public void bindSceneFBO() {
        glBindFramebuffer(GL_FRAMEBUFFER, sceneFBO);
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
    float smoothingFactor = 0.01f; 
    
    public void renderBloom(int windowWidth, int windowHeight, float threshold, float bloomIntensity) {
    // 1. Extract bright areas.
    glDisable(GL_DEPTH_TEST);
    glBindFramebuffer(GL_FRAMEBUFFER, brightFBO);
    glViewport(0, 0, width, height); // Set viewport for bright FBO
    glClear(GL_COLOR_BUFFER_BIT);
    bloomExtractShader.bind();
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, sceneTexture);
    bloomExtractShader.setUniformSampler("sceneTexture", 0);
    bloomExtractShader.setUniform1f("threshold", threshold);
    
    renderQuad();
    bloomExtractShader.unbind();
    glEnable(GL_DEPTH_TEST);
    
    // 2. Blur the bright texture using ping-pong FBOs.
    boolean horizontal = true;
    boolean firstIteration = true;
    blurShader.bind();
    for (int i = 0; i < blurIterations; i++) {
        glBindFramebuffer(GL_FRAMEBUFFER, pingpongFBO[horizontal ? 1 : 0]);
        glViewport(0, 0, width, height); // Set viewport for ping-pong FBO
        glClear(GL_COLOR_BUFFER_BIT);
        blurShader.setUniform2f("blurDirection", horizontal ? 1.0f : 0.0f, horizontal ? 0.0f : 1.0f);
        glActiveTexture(GL_TEXTURE0);
        if (firstIteration) {
            glBindTexture(GL_TEXTURE_2D, brightTexture);
            firstIteration = false;
        } else {
            glBindTexture(GL_TEXTURE_2D, pingpongTexture[horizontal ? 0 : 1]);
        }
        blurShader.setUniformSampler("image", 0);
        renderQuad();
        horizontal = !horizontal;
    }
    blurShader.unbind();
    
    // 3. Combine the original scene with the blurred bloom texture.
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
    glViewport(0, 0, windowWidth, windowHeight); // Reset to window viewport
    glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
    bloomCombineShader.bind();
    glActiveTexture(GL_TEXTURE0);
    glBindTexture(GL_TEXTURE_2D, sceneTexture);
    glGenerateMipmap(GL_TEXTURE_2D); 
    
 // Create a buffer to store one pixel (RGBA floats)
    FloatBuffer pixelBuffer = BufferUtils.createFloatBuffer(4);

    // Read from the smallest mipmap level (assuming level = mipLevels - 1)
    int mipLevel = (int) (Math.log(Math.max(width, height)) / Math.log(2));
    glGetTexImage(GL_TEXTURE_2D, mipLevel, GL_RGBA, GL_FLOAT, pixelBuffer);

 // Retrieve color components
    float r = pixelBuffer.get(0);
    float g = pixelBuffer.get(1);
    float b = pixelBuffer.get(2);
    // Optionally ignore the alpha or use it if needed.

    // Compute luminance using a common formula (adjust weights as needed)
    
    if (r != Float.NaN && r < 1.0 && r > 0.0 &&
    	g != Float.NaN  && g < 1.0 && g > 0.0 &&
    	b != Float.NaN  && b < 1.0 && b > 0.0) {
    	 averageBrightness = r * 0.2126f + g * 0.7152f + b * 0.0722f;
    }
    
   
    //System.out.println("averageBrightness: " + averageBrightness + " " + sceneTexture);
    
    float minExposure = 0.95f; // for bright, outdoor scenes
    float maxExposure = 3.52f; // for dark, indoor scenes
    
    float minGamma = 0.75f;
    float maxGamma = 2.52f;
    
    float brightnessThresholdLow = 0.1f;  // below this, assume it's dark
    float brightnessThresholdHigh = 0.7f; // above this, assume it's bright
    
    float t = (averageBrightness - brightnessThresholdLow) / (brightnessThresholdHigh - brightnessThresholdLow);
	 // Clamp 't' between 0 and 1.
	 if (t < 0.0f) {
	     t = 0.0f;
	 } else if (t > 1.0f) {
	     t = 1.0f;
	 }
	
	 // Linear interpolation function: mix(a, b, t) = a * (1 - t) + b * t.
	 // For exposure: when t is 0 (dark), use maxExposure; when t is 1 (bright), use minExposure.
	 float dynamicExposure = maxExposure * (1.0f - t) + minExposure * t;
	 // For gamma: when t is 0 (dark), use maxGamma; when t is 1 (bright), use minGamma.
	 float dynamicGamma = maxGamma * (1.0f - t) + minGamma * t;
	 
	 
	 currentExposure += (dynamicExposure - currentExposure) * smoothingFactor;
	 currentGamma   += (dynamicGamma - currentGamma) * smoothingFactor;
    
    
    bloomCombineShader.setUniform1f("gamma", 0); //dynamicGamma
    bloomCombineShader.setUniform1f("exposure",currentExposure); //dynamicExposure
    bloomCombineShader.setUniform1f("vignetteStrength", 0.0f);
    
    
    bloomCombineShader.setUniformSampler("sceneTexture", 0);
    glActiveTexture(GL_TEXTURE1);
    glBindTexture(GL_TEXTURE_2D, pingpongTexture[horizontal ? 0 : 1]);
    bloomCombineShader.setUniformSampler("bloomTexture", 1);
    bloomCombineShader.setUniform1f("bloomIntensity", bloomIntensity);
    
    
    
    
    //bloomCombineShader.setUniform1f("exposure", 4.2f);
    renderQuad();
    bloomCombineShader.unbind();
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
    }
}