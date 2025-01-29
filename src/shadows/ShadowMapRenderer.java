package shadows;

import org.joml.Matrix4f;
import org.lwjgl.system.MemoryStack;
import entities.Entity;
import entities.Light;
import shaders.ShaderProgram;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

/**
 * Responsible for rendering shadow maps for multiple lights.
 */
public class ShadowMapRenderer {
    
    private final ShaderProgram shadowShader;
    
    // Shadow map dimensions
    public static final int SHADOW_WIDTH = 2048;
    public static final int SHADOW_HEIGHT = 2048;
    
    /**
     * Initializes the ShadowMapRenderer by loading the shadow shaders.
     */
    public ShadowMapRenderer() {
        // Initialize the shadow shader program with only vertex and fragment shaders
        shadowShader = new ShaderProgram(
            "src/shadows/shadow_vertex.glsl", null, null, null,
            "src/shadows/shadow_fragment.glsl"
        );
    }
    
    /**
     * Renders shadow maps for all shadow-casting lights.
     *
     * @param entities The list of entities to render into the shadow maps.
     * @param lights   The list of lights that cast shadows.
     */
    public void renderShadowMaps(List<Entity> entities, List<Light> lights) {
        for (int i = 0; i < lights.size(); i++) {
            Light light = lights.get(i);
            renderShadowMap(entities, light, i);
        }
    }
    
    /**
     * Renders a single shadow map for a specific light.
     *
     * @param entities  The list of entities to render.
     * @param light     The light for which to render the shadow map.
     * @param lightIndex The index of the light in the lights list.
     */
    private void renderShadowMap(List<Entity> entities, Light light, int lightIndex) {
       /*
    	// Bind the light's framebuffer
        glBindFramebuffer(GL_FRAMEBUFFER, light.getShadowMapFBO());
        glViewport(0, 0, SHADOW_WIDTH, SHADOW_HEIGHT);
        glClear(GL_DEPTH_BUFFER_BIT);
        
        // Use the shadow shader
        shadowShader.bind();
        
        // Pass the light space matrix to the shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            light.getLightSpaceMatrix().get(fb);
            shadowShader.setUniformMat4("lightSpaceMatrix", false, fb);
        }
        
        // Render each entity from the light's perspective
        for (Entity entity : entities) {
            renderEntityForShadow(entity);
        }
        
        shadowShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        */
    }
    
    /**
     * Renders a single entity using the shadow shader.
     *
     * @param entity The entity to render.
     */
    private void renderEntityForShadow(Entity entity) {
        // Build the model matrix
        Matrix4f model = new Matrix4f()
            .translate(entity.getPosition())
            .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)
            .scale(entity.getScale());
        
        // Upload the model matrix to the shadow shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            shadowShader.setUniformMat4("model", false, fb);
        }
        
        // Bind the entity's VAO
        Mesh mesh = entity.getMesh();
        glBindVertexArray(mesh.getVaoId());
        
        // Enable only the position attribute for shadow mapping
        //glEnableVertexAttribArray(0); // Assuming location 0 for positions
        
        // Draw the entity
        glDrawArrays(GL_TRIANGLES, 0, mesh.getVertexCount());
        
        // Disable the position attribute after drawing
        //glDisableVertexAttribArray(0);
        
        // Unbind the VAO
        glBindVertexArray(0);
    }

    
    /**
     * Cleans up the shadow shader program.
     * Call this method when shutting down your application.
     */
    public void cleanup() {
        shadowShader.destroy();
    }
}
