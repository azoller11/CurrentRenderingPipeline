package shadows;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.util.List;

import entities.Camera;
import entities.Entity;
import entities.Light;
import renderer.MasterRenderer;
import shaders.ShaderProgram;
import toolbox.Frustum;

public class ShadowRenderer {
    private static int shadowWidth = 0;
    private static int shadowHeight;
    
    // OpenGL handles
    private int depthMapFBO;
    private int depthMap; // This is the depth texture ID (useful for debugging)
    
    // Our simple shader to render depth only
    private ShaderProgram shadowShader;
    
    private Frustum frustum;

    public ShadowRenderer(int shadowWidth, int shadowHeight) {
        this.shadowWidth = shadowWidth;
        this.shadowHeight = shadowHeight;
        this.frustum = new Frustum();
        initShadowFBO();
        initShadowShader();
    }

    // Initialize the framebuffer and attach a depth texture
    private void initShadowFBO() {
        // Generate framebuffer
        depthMapFBO = glGenFramebuffers();

        // Create depth texture
        depthMap = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthMap);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT,
                     shadowWidth, shadowHeight, 0,
                     GL_DEPTH_COMPONENT, GL_FLOAT, (java.nio.ByteBuffer) null);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
        // Set border color (white – meaning “not in shadow”)
        float[] borderColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        // Attach depth texture as FBO's depth buffer
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);
        // We are not going to render any color data
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);
        
        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Error: Shadow framebuffer is not complete!");
        }
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    // Initialize the shadow shader that renders the scene from the light’s perspective.
    private void initShadowShader() {
        // Note: Pass null for shaders you are not using.
        // Make sure the shader files "shadow.vs" and "shadow.fs" exist at the specified paths.
        shadowShader = new ShaderProgram("src/shadows/vertex.glsl", null, null, null, "src/shadows/fragment.glsl");
    }

    /**
     * Renders the scene’s depth (shadow map) from the light’s point of view.
     *
     * @param entities         List of entities to render
     * @param lightSpaceMatrix The transformation matrix (from world to light space)
     */
    public void renderShadowMap(List<Entity> entities, Matrix4f lightSpaceMatrix, Matrix4f viewMatrix, 
            Matrix4f projectionMatrix) {
    	
    	glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glViewport(0, 0, shadowWidth, shadowHeight);
        
        glClear(GL_DEPTH_BUFFER_BIT);
        
        shadowShader.bind();
        // Send the light space matrix uniform to the shader
        shadowShader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);
        
        shadowShader.setUniform1f("alphaThreshold", 0.1f); 

        // Render each entity using its model transform
        frustum.calculateFrustum(projectionMatrix, viewMatrix);
        for (Entity entity : entities) {
        	//if (frustum.contains(entity.getPosition(), entity.getMesh().getFurthestPoint() * entity.getScale())) {
        		 Matrix4f modelMatrix = createModelMatrix(entity);
                 shadowShader.setUniformMat4("model", modelMatrix);
                 
                 
                 shadowShader.setUniform1i("diffuseMap", 0);
                 glActiveTexture(GL_TEXTURE0);
                 glBindTexture(GL_TEXTURE_2D, entity.getTextureId());

                 int vaoID = entity.getMesh().getVaoId();
                 glBindVertexArray(vaoID);
                 glDrawArrays(GL_TRIANGLES, 0, entity.getMesh().getVertexCount());
                 glBindVertexArray(0);
        	//}
           
        }
       
        shadowShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        
        
    }

    // Utility function to build the model matrix from an entity's transform.
    private Matrix4f createModelMatrix(Entity entity) {
    	Matrix4f model = new Matrix4f()
    		    .identity()
    		    .scale(entity.getScale())            // Scale first
    		    .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)         // Rotate next
    		    .translate(entity.getPosition());    // Finally translate

    		model.setTranslation(entity.getPosition());
        return model;
    }
    
    public static Matrix4f createLightSpaceMatrix(Light light, Camera camera) {
        float orthoSize = 400.0f; 
        float near = 1.0f;
        float far = 180000000.0f;

        // Light direction (keep your existing inversion logic)
        Vector3f lightDir = new Vector3f(light.getPosition()).normalize();
        lightDir.z = -lightDir.z;
        lightDir.x = -lightDir.x;

        // NEW: Use camera position as the shadow map center
        Vector3f target = new Vector3f(camera.getPosition()); 

        // Light position relative to CAMERA (not scene center)
        Vector3f lightPos = new Vector3f(target)
                           .sub(new Vector3f(lightDir).mul(orthoSize * 2));

        // Create light view matrix looking at CAMERA POSITION
        Matrix4f lightView = new Matrix4f().lookAt(
            lightPos,
            target,  // Focus on camera
            new Vector3f(0, 1, 0)
        );

        Matrix4f lightProjection = new Matrix4f().ortho(
        	    -orthoSize, orthoSize,
        	    -orthoSize, orthoSize,
        	    near, far
        	);


        Matrix4f lightSpaceMatrix = new Matrix4f();
        lightProjection.mul(lightView, lightSpaceMatrix);
        lightSpaceMatrix.scale(1, -1, 1);  // Keep your existing Y-flip

        // NEW: Snap using CAMERA POSITION in light space
        float texelSize = (2.0f * orthoSize) / shadowWidth;
        Vector4f origin = new Vector4f(target, 1.0f);  // Camera position
        lightSpaceMatrix.transform(origin);
        origin.x = Math.round(origin.x / texelSize) * texelSize;
        origin.y = Math.round(origin.y / texelSize) * texelSize;
        Matrix4f adjust = new Matrix4f().translate(-origin.x, -origin.y, 0);
        lightSpaceMatrix.mul(adjust);

        return lightSpaceMatrix;
    }



    /**
     * Returns the OpenGL texture ID for the depth map.
     * You can use this ID to debug by rendering the texture to a quad.
     *
     * @return the texture ID of the depth (shadow) map.
     */
    public int getDepthMapTexture() {
        return depthMap;
    }

    // Call this method during cleanup
    public void cleanUp() {
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthMap);
        shadowShader.destroy();
    }
}
