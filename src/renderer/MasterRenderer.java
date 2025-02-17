package renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
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

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_NO_ERROR;
import static org.lwjgl.opengl.GL11.glClearColor;
import static org.lwjgl.opengl.GL11.glGetError;
import static org.lwjgl.opengl.GL40.*;

public class MasterRenderer {
	
	public static final float NEAR_PLANE = 0.1f;
	public static final float FOV = 60;
	public static final float FAR_PLANE = 2000000000;

    private final ShaderProgram shader;
    private final Matrix4f projectionMatrix;
    
    private int screenWidth, screenHeight;
    
    private Frustum frustum;

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
    
   
        
        frustum.calculateFrustum(projectionMatrix, view);
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


    /**
     * Cleanup the shader (call at end of program).
     */
    public void cleanup() {
        shader.destroy();
    }
    
   
}
