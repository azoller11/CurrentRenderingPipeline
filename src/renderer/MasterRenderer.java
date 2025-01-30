package renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import entities.Camera;
import entities.Entity;
import entities.Light;
import settings.EngineSettings;
import shaders.ShaderProgram;
import shadows.ShadowMapRenderer;
import toolbox.Frustum;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL40.*;

public class MasterRenderer {
	
	public static final float NEAR_PLANE = 0.1f;
	public static final float FOV = 60;
	public static final float FAR_PLANE = 10000;

    private final ShaderProgram shader;
    private final Matrix4f projectionMatrix;
    
    private final ShadowMapRenderer shadowMapRenderer;
    
    private int screenWidth, screenHeight;
    
    private Frustum frustum;

    // For each frame, we’ll set up the "view" from the camera.
    // We keep the "projection" in a single place here for simplicity.
    public MasterRenderer(int width, int height) {
    	
    	this.screenHeight = height;
    	this.screenWidth = width;
        // 1) Load/compile/link your pipeline
        shader = new ShaderProgram(
            "src/shaders/vertex.glsl",
            "src/shaders/tess_control.glsl",
            "src/shaders/tess_eval.glsl",
            "src/shaders/geometry.glsl",
            "src/shaders/fragment.glsl"
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
        
     // Initialize the ShadowMapRenderer
        shadowMapRenderer = new ShadowMapRenderer();
    }

    public Matrix4f getProjectionMatrix() {
		return projectionMatrix;
	}
    
    public Matrix4f getFlatProjection() {
        return new Matrix4f().ortho2D(0.0f, screenWidth, 0.0f, screenHeight);
    }

	/**
     * Render all entities from the perspective of the camera.
     */
    public void render(List<Entity> entities, List<Light> lights, Camera camera) {
    	// 1. Render shadow maps for each light
        //shadowMapRenderer.renderShadowMaps(entities, lights);
        
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
     // 2. Reset viewport to window dimensions to prevent distortion
        glViewport(0, 0, this.screenWidth, this.screenHeight);

        // 3. Clear the screen (color and depth buffers)
        glClearColor(0.2f, 0.3f, 0.4f, 1.0f);
        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

        // 2) Use our pipeline
        shader.bind();
        
        shader.setUniform1i("debugMode", EngineSettings.ShaderDebug.getValue());
        
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

       // bindShadowMaps(lights, shader);
        
        frustum.calculateFrustum(projectionMatrix, view);
        // 5) For each entity, build the model matrix and draw
        for (Entity entity : entities) {
        	if (frustum.contains(entity.getPosition(), entity.getMesh().getFurthestPoint()))
        		drawEntity(entity);
        }

        shader.unbind();
    }

    private void drawEntity(Entity entity) {
        // 1) Build model matrix from the entity's transform
        Matrix4f model = new Matrix4f()
            .translate(entity.getPosition())
            .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)
            .scale(entity.getScale());

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
        boolean hasHeightMap = (entity.getNormalMapId() != 0);
        
        
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

        // Now pass these booleans to the shader
        shader.setUniform1i("hasMetallic",  hasMetallic ? 1 : 0);
        shader.setUniform1i("hasRoughness", hasRoughness ? 1 : 0);
        shader.setUniform1i("hasAo",        hasAo ? 1 : 0);
        
        shader.setUniform1i("hasNormal",  hasNormalMap ? 1 : 0);
        shader.setUniform1i("hasHeight", hasHeightMap ? 1 : 0);
        

        // If you still use "shineDamper"/"reflectivity" for older code, you can set them
        shader.setUniform1f("shineDamper", entity.getShineDamper());
        shader.setUniform1f("reflectivity", entity.getReflectivity());
        
    
        if (entity.isHasTransparency()) {
        	GL11.glDisable(GL11.GL_CULL_FACE);
    		//GL11.glCullFace(GL11.GL_BACK);
        } else {
        	GL11.glEnable(GL11.GL_CULL_FACE);
    		GL11.glCullFace(GL11.GL_BACK);
        }
        

        //shader.setUniform1f("shineDamper", entity.getShineDamper());
        //shader.setUniform1f("reflectivity", entity.getReflectivity());
        

        //  Bind the entity's mesh
        Mesh mesh = entity.getMesh();
        glBindVertexArray(mesh.getVaoId());

        // Because we have tessellation in the pipeline, use GL_PATCHES
        glPatchParameteri(GL_PATCH_VERTICES, 3);
        glDrawArrays(GL_PATCHES, 0, mesh.getVertexCount());

        glBindVertexArray(0);
    }

    private void bindShadowMaps(List<Light> lights, ShaderProgram shader) {
        int textureUnit = 10; // Starting texture unit for shadow maps to avoid conflicts
        shader.setUniform1i("numLights", lights.size());

        for (int i = 0; i < lights.size(); i++) {
            Light light = lights.get(i);
            String shadowMapUniform = "shadowMaps[" + i + "]";
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            glBindTexture(GL_TEXTURE_2D, light.getShadowMapTexture());
            shader.setUniform1i(shadowMapUniform, textureUnit);
            
            
            System.out.println(light.getShadowMapTexture());
            
            // Pass light space matrix
            String matrixUniform = "lightSpaceMatrices[" + i + "]";
            try (MemoryStack stack = MemoryStack.stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);
                light.getLightSpaceMatrix().get(fb);
                shader.setUniformMat4(matrixUniform, false, fb);
            }

            textureUnit++;
        }
    }

    /**
     * Cleanup the shader (call at end of program).
     */
    public void cleanup() {
        shader.destroy();
    }
    
   
}
