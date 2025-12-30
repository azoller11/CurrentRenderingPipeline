package decals;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import entities.Camera;
import entities.Light;
import shaders.ShaderProgram;
import toolbox.CubeMesh;
import toolbox.MeshUtil;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL40.*;

public class DecalRenderer {

    private final ShaderProgram shader;
    private int cubeVAO;
    private int vertexCount;
    private int maxLights = 16;  // Support up to 16 lights

    public DecalRenderer() {
        shader = new ShaderProgram(
            "src/decals/decal_vertex.glsl",
            null,
            null,
            null,
            "src/decals/decal_fragment.glsl"
        );
        
        CubeMesh cubeMesh = new CubeMesh();
        cubeVAO = cubeMesh.getVao();
        vertexCount = cubeMesh.getVertexCount();
    }
    
    public void render(List<Decal> decals, Camera camera, Matrix4f projection, 
                      int shadowTexture, List<Light> lights, Matrix4f lightSpaceMatrix) {
        if (decals.isEmpty()) return;

        shader.bind();

        // Camera matrices
        shader.setUniformMat4("view", camera.getViewMatrix());
        shader.setUniformMat4("projection", projection);
        shader.setUniform3f("viewPos", camera.getPosition());
        
        // Light space matrix for shadows
        if (lightSpaceMatrix != null) {
            shader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);
        }
        
        // Set up lights
        int lightCount = Math.min(lights.size(), maxLights);
        shader.setUniform1i("numLights", lightCount);
        
        for (int i = 0; i < lightCount; i++) {
            Light light = lights.get(i);
            String base = "lights[" + i + "].";
            
            shader.setUniform3f(base + "position", light.getPosition());
            shader.setUniform3f(base + "color", light.getColor());
            shader.setUniform3f(base + "attenuation", light.getAttenuation());
            shader.setUniform1i(base + "castShadow", light.isCastShadow() ? 1 : 0);
            
            // For directional lights, we might want to store direction
            Vector3f lightDirection = new Vector3f(light.getPosition()).normalize();
            shader.setUniform3f(base + "direction", lightDirection);
        }
        
        // Set up textures
        glActiveTexture(GL_TEXTURE0);
        shader.setUniform1i("decalTexture", 0);
        
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, shadowTexture);
        shader.setUniform1i("shadowMap", 1);
        
        // Optional: if you have a normal map texture
        // glActiveTexture(GL_TEXTURE2);
        // shader.setUniform1i("normalTexture", 2);

        // Enable depth test but don't write
        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);           // don't write depth
        glDepthFunc(GL_LEQUAL);  
        
        // Enable blending for transparency
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
        
        // Enable culling
        glEnable(GL_CULL_FACE);
        glCullFace(GL_BACK);

        glBindVertexArray(cubeVAO);

        for (Decal decal : decals) {
            shader.setUniformMat4("model", decal.getModelMatrix());
            
            // Bind decal texture
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, decal.getTextureId());
            
            // Bind normal texture if available
            if (decal.getNormalTextureId() > 0) {
                glActiveTexture(GL_TEXTURE2);
                glBindTexture(GL_TEXTURE_2D, decal.getNormalTextureId());
                shader.setUniform1i("useNormalMap", 1);
            } else {
                shader.setUniform1i("useNormalMap", 0);
            }
            
            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        // Restore state
        glDisable(GL_BLEND);
        glDepthMask(true);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);
        
        shader.unbind();
    }
    
    public void cleanup() {
        shader.cleanUp();
    }
}