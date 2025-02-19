package renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;
import org.lwjgl.util.vector.Vector2f;

import entities.Camera;
import entities.Entity;
import entities.Light;
import settings.EngineSettings;
import shaders.ShaderProgram;
import shadows.ShadowRenderer;
import toolbox.Equations;
import toolbox.Frustum;
import toolbox.Mesh;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;
import java.util.Random;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL40.*;

public class MasterRenderer {
	
	public static final float NEAR_PLANE = 0.1f;
	public static final float FOV = 90;
	public static final float FAR_PLANE = 2000000000;

    private final ShaderProgram shader;
    private final Matrix4f projectionMatrix;
    
    private static int screenWidth;
	private static int screenHeight;
    
    private Frustum frustum;
    
    // --- New SSAO resources ---
    private int depthFBO;
    private int depthTextureID;
    private int noiseTextureID;
    private final int NOISE_SIZE = 4;
    private final int NUM_SAMPLES = 64;
    private Vector3f[] sampleKernel;

    // For each frame, we’ll set up the "view" from the camera.
    // We keep the "projection" in a single place here for simplicity.
    public MasterRenderer(int width, int height) {
    	
    	this.screenHeight = height;
    	this.screenWidth = width;
        // 1) Load/compile/link your pipeline
    	/*
        shader = new ShaderProgram(
            "src/shaders/vertex.glsl",
            "src/shaders/tess_control.glsl",
            "src/shaders/tess_eval.glsl",
            "src/shaders/geometry.glsl",
            "src/shaders/fragment.glsl"
        );*/
    	
	   String vertex =  "src/shaders/vertex.glsl";
	   String tess_control =  "src/shaders/tess_control.glsl";
	   String tess_eval =  "src/shaders/tess_eval.glsl";
	   String geometry =  "src/shaders/geometry.glsl";
	   String fragment =  "src/shaders/fragment.glsl";
	   
	   String[] additionalVertexShaders = new String[] {
			   
	   };
            
	   String[] additionalFragmentShaders = new String[] {
			   "src/shadersModular/fresnel.glsl",
			   "src/shadersModular/parallaxMapping.glsl",
			   "src/shadersModular/computeNormal.glsl",
			   "src/shadersModular/computeLightContribution.glsl",
			   "src/shadersModular/calculatePOMShadow.glsl",
			   "src/shadersModular/calculatedDirectionalShadows.glsl",
			   
	   };
	   
	   String[] additionalGeometryShaders = new String[] {
			  
	   };
        
	   shader = new ShaderProgram(vertex, tess_control, tess_eval, geometry,fragment, 
			   additionalVertexShaders,
			   additionalFragmentShaders,
			   additionalGeometryShaders
			   
			   );
        
        //General settings
        glFrontFace(GL_CW);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        frustum = new Frustum();


        // 2) Create your perspective projection
        float aspect = (float) width / height;
        float fov = (float) Math.toRadians(FOV);
        float near = NEAR_PLANE;
        float far  = FAR_PLANE;
        
        float aspectRatio = (float) width / (float) height;

  /*      projectionMatrix = new Matrix4f().identity()
            .perspective(
                (float)Math.toRadians(FOV),
                aspectRatio,
                NEAR_PLANE,
                FAR_PLANE
            );
*/
        // We'll use JOML to build the matrix. The final pass to GPU is still float[].
        projectionMatrix = new Matrix4f().perspective(fov, aspect, near, far);
        
        // --- Initialize new SSAO resources ---
        initDepthTexture();
        initNoiseTexture();
        generateSampleKernel();
        
     // Initialize the ShadowMapRenderer
    }

    public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}
    
    public Matrix4f getFlatProjection() {
        return new Matrix4f().ortho2D(0.0f, screenWidth, 0.0f, screenHeight);
    	//return new Matrix4f().ortho(0, 1, 1, 0, -1, 1);
    }

	/**
     * Render all entities from the perspective of the camera.
     */
    public void render(List<Entity> entities, List<Light> lights, Camera camera, int shadowMap) {
    	
    	
        
     // 2. Reset viewport to window dimensions to prevent distortion
        glViewport(0, 0, this.screenWidth, this.screenHeight);

        // 3. Clear the screen (color and depth buffers)
        glClearColor(0.2f, 0.3f, 0.4f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        
    

        // 2) Use our pipeline
        shader.bind();
        
    	
        
        //shader.setUniform1i("debugMode", EngineSettings.ShaderDebug.getValue());
        
        //Calculate if the lights 
        
        shader.setUniformLights("lights", lights);
        
        shader.setUniform3f("cameraPos",
        	    camera.getPosition().x,
        	    camera.getPosition().y,
        	    camera.getPosition().z
        	);

        // 3) Upload the "view" matrix from the camera
        Matrix4f view = camera.getViewMatrix();

        // 4) Upload the projection matrix
        // We'll do it once at start (you could do it once only if it never changes)
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);

            // setUniformMat4(...) is from ShaderProgram
            projectionMatrix.get(fb);
            shader.setUniformMat4("projection", false, fb);

            fb.clear();
            view.get(fb);
            shader.setUniformMat4("view", false, fb);
            
         
            
        }
        
        shader.setUniformMat4("lightSpaceMatrix", ShadowRenderer.createLightSpaceMatrix(lights.get(0), camera));
        Vector3f lightDir = new Vector3f(lights.get(0).getPosition()).normalize();
        lightDir.z = -lightDir.z;
        lightDir.x = -lightDir.x;
        
        shader.setUniform3f("directionalLightDir", lightDir);
   
        
        frustum.calculateFrustum(projectionMatrix, view);
        
        
        // Bind SSAO resources:
        // Bind depth texture to texture unit 7.
        glActiveTexture(GL_TEXTURE7);
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        shader.setUniform1i("depthTexture", 7);
        // Bind noise texture to texture unit 8.
        glActiveTexture(GL_TEXTURE8);
        glBindTexture(GL_TEXTURE_2D, noiseTextureID);
        shader.setUniform1i("noiseTexture", 8);
        // Upload sample kernel array.
        for (int i = 0; i < NUM_SAMPLES; i++) {
            Vector3f sample = sampleKernel[i];
            shader.setUniform3f("sampleKernel[" + i + "]", sample.x, sample.y, sample.z);
        }
        
        
        
        
        // 5) For each entity, build the model matrix and draw
        for (Entity entity : entities) {
        	if (frustum.contains(entity.getPosition(), entity.getMesh().getFurthestPoint() * entity.getScale()))
        		drawEntity(entity, shadowMap);
        }

        shader.unbind();
       
      
    }

    private void drawEntity(Entity entity, int shadowMap) {
        // 1) Build model matrix from the entity's transform
    	
    	Matrix4f model = new Matrix4f()
    		    .identity()
    		    .scale(entity.getScale())            // Scale first
    		    .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)         // Rotate next
    		    .translate(entity.getPosition());    // Finally translate

    		model.setTranslation(entity.getPosition());
    	 
    	
    	
    	//model.scale(entity.getScale(),entity.getScale(),entity.getScale());
    	
    	
    	
        // 2) Upload "model" uniform
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            shader.setUniformMat4("model", false, fb);
        }
        
        
        
        //upload the texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, entity.getTextureId());
        shader.setUniform1i("diffuseTexture", 0);
        
        
      
        
        
        // --------------------------------------------------
        // 3) Metallic / Roughness / AO with condition checks
        // --------------------------------------------------
        // We'll store booleans for each
        boolean hasMetallic = (entity.getMetallicMap() != 0);
        boolean hasRoughness = (entity.getRoughnessMap() != 0);
        boolean hasAo = (entity.getAoMap() != 0);
        boolean hasNormalMap = (entity.getNormalMapId() != 0);
        boolean hasHeightMap = (entity.getHeighMapId() != 0);
        
        
        if (hasNormalMap) {
        	glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, entity.getNormalMapId());
            shader.setUniform1i("normalMap", 1);
        }
        
        if (hasHeightMap) {
        	 glActiveTexture(GL_TEXTURE2);
             glBindTexture(GL_TEXTURE_2D, entity.getHeighMapId());
             shader.setUniform1i("heightMap", 2);
             //parallax scale
             if (entity.getParallaxScale() != null) {
            	 shader.setUniform1f("parallaxScale", entity.getParallaxScale().x);
                 shader.setUniform1f("minLayers", entity.getParallaxScale().y);
                 shader.setUniform1f("maxLayers", entity.getParallaxScale().z);
             }
             
        }

        // If a texture ID != 0, we bind it. Otherwise skip binding
        if (hasMetallic) {
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, entity.getMetallicMap());
            shader.setUniform1i("metallicMap", 3);
        }
        if (hasRoughness) {
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, entity.getRoughnessMap());
            shader.setUniform1i("roughnessMap", 4);
        }
        if (hasAo) {
            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, entity.getAoMap());
            shader.setUniform1i("aoMap", 5);
        }
        
        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_2D, shadowMap);
        shader.setUniform1i("shadowMap", 6);

        // Now pass these booleans to the shader
        shader.setUniform1i("hasMetallic",  hasMetallic ? 1 : 0);
        shader.setUniform1i("hasRoughness", hasRoughness ? 1 : 0);
        shader.setUniform1i("hasAo",        hasAo ? 1 : 0);
        
        shader.setUniform1i("hasNormal",  hasNormalMap ? 1 : 0);
        shader.setUniform1i("hasHeight", hasHeightMap ? 1 : 0);
        shader.setUniform1f("isOpaquePass", (entity.getNormalMapId() != 0) ? 1 : 0);
        
        // If you still use "shineDamper"/"reflectivity" for older code, you can set them
        shader.setUniform1f("shineDamper", entity.getShineDamper());
        shader.setUniform1f("reflectivity", entity.getReflectivity());
        
    
        if (entity.isHasTransparency()) {
        	GL11.glDisable(GL11.GL_CULL_FACE);
        	glEnable(GL_BLEND);
        	glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
    		//GL11.glCullFace(GL11.GL_BACK);
        } else {
        	GL11.glEnable(GL11.GL_CULL_FACE);
    		GL11.glCullFace(GL11.GL_BACK);
        }
        

        

        //  Bind the entity's mesh
        Mesh mesh = entity.getMesh();
        glBindVertexArray(mesh.getVaoId());

        // Because we have tessellation in the pipeline, use GL_PATCHES
        glPatchParameteri(GL_PATCH_VERTICES, 3);
        glDrawArrays(GL_PATCHES, 0, mesh.getVertexCount());

        glBindVertexArray(0);
    }
    
    
 
    public static Matrix4f createTransformationMatrix(Vector3f translation, float rx, float ry, float rz, float scale, Vector3f pivot) {
        Matrix4f matrix = new Matrix4f().identity();
        
        // 1. Translate to the desired world position.
        matrix.translate(translation);
        
        // 2. Move to the pivot point (so scaling/rotation happens around it)
        matrix.translate(pivot);
        
        // 3. Apply rotation
        matrix.rotateX((float)Math.toRadians(rx))
              .rotateY((float)Math.toRadians(ry))
              .rotateZ((float)Math.toRadians(rz));
        
        // 4. Apply scaling
        matrix.scale(scale);
        
        // 5. Move back from the pivot point
        matrix.translate(new Vector3f(-pivot.x, -pivot.y, -pivot.z));
        
        return matrix;
    }
    
    private void initDepthTexture() {
        // Generate the FBO
        depthFBO = glGenFramebuffers();
        glBindFramebuffer(GL_FRAMEBUFFER, depthFBO);

        // Create depth texture
        depthTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthTextureID);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT, screenWidth, screenHeight, 0,
                GL_DEPTH_COMPONENT, GL_FLOAT, (ByteBuffer) null);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);

        // Attach texture to FBO as depth attachment
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthTextureID, 0);

        // No color buffer is drawn to.
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            System.err.println("Error: Depth framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Generates a small noise texture used to randomize sample directions.
     */
    private void initNoiseTexture() {
        int noiseTextureSize = NOISE_SIZE;
        FloatBuffer noiseBuffer = BufferUtils.createFloatBuffer(noiseTextureSize * noiseTextureSize * 3);
        Random random = new Random();
        for (int i = 0; i < noiseTextureSize * noiseTextureSize; i++) {
            // Generate random xy values in range [-1,1], z = 0.
            float x = random.nextFloat() * 2.0f - 1.0f;
            float y = random.nextFloat() * 2.0f - 1.0f;
            noiseBuffer.put(x).put(y).put(0.0f);
        }
        noiseBuffer.flip();

        noiseTextureID = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, noiseTextureID);
        // Using GL_RGB16F for high precision; adjust if needed.
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGB16F, noiseTextureSize, noiseTextureSize, 0, GL_RGB, GL_FLOAT, noiseBuffer);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        // Tiling the noise texture over the screen.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    /**
     * Generates an array of sample kernel vectors for SSAO.
     */
    private void generateSampleKernel() {
        sampleKernel = new Vector3f[NUM_SAMPLES];
        Random random = new Random();
        for (int i = 0; i < NUM_SAMPLES; i++) {
            // Random vector with x,y in range [-1,1] and z in range [0,1]
            float x = random.nextFloat() * 2.0f - 1.0f;
            float y = random.nextFloat() * 2.0f - 1.0f;
            float z = random.nextFloat();
            Vector3f sample = new Vector3f(x, y, z).normalize();
            // Scale samples so they're more concentrated near the origin.
            float scale = (float) i / NUM_SAMPLES;
            scale = 0.1f + 0.9f * (scale * scale);
            sample.mul(scale);
            sampleKernel[i] = sample;
        }
    }
    
    


    public static int getScreenWidth() {
		return screenWidth;
	}

	public void setScreenWidth(int screenWidth) {
		this.screenWidth = screenWidth;
	}

	public static int getScreenHeight() {
		return screenHeight;
	}

	public void setScreenHeight(int screenHeight) {
		this.screenHeight = screenHeight;
	}

	/**
     * Cleanup the shader (call at end of program).
     */
    public void cleanup() {
        shader.destroy();
    }
    
   
}
