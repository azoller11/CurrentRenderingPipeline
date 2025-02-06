package terrain;

import entities.Light;
import shaders.ShaderProgram;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.List;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL40.*;

public class TerrainRenderer {

    private ShaderProgram terrainShader;
    private Map<String, Integer> textureMap;

    public TerrainRenderer() {
        terrainShader = new ShaderProgram(
                "src/terrain/vertex.glsl", 
                "src/terrain/tess_control.glsl", 
                "src/terrain/tess_eval.glsl", 
                null, 
                "src/terrain/fragment.glsl");
        textureMap = new HashMap<>();
    }

    public void addTexture(String uniformName, int textureId) {
        textureMap.put(uniformName, textureId);
    }
    
    public int getTexture(String uniformName) {
        return textureMap.get(uniformName);
    }
    
    public Iterable<String> getTextureUniformNames() {
        return textureMap.keySet();
    }
    
    /**
     * Renders adaptive terrain with lighting.
     *
     * @param adaptiveGen    The adaptive terrain generator.
     * @param projection     The projection matrix.
     * @param view           The view matrix.
     * @param model          The model matrix for the terrain.
     * @param cameraPosition The camera's world-space position.
     * @param lights         List of active lights.
     */
    public void renderAdaptiveTerrain(AdaptiveTerrainGenerator adaptiveGen,
                                      Matrix4f projection,
                                      Matrix4f view,
                                      Matrix4f model,
                                      Vector3f cameraPosition,
                                      List<Light> lights) {
        // Get the adaptive patch meshes.
        List<AdaptiveTerrainGenerator.PatchMesh> patches = adaptiveGen.getPatches(cameraPosition);
        
        terrainShader.bind();
        terrainShader.setUniformMat4("projection", projection);
        terrainShader.setUniformMat4("view", view);
        terrainShader.setUniformMat4("model", model);
        terrainShader.setUniform3f("cameraPosition", cameraPosition);
        terrainShader.setUniform1f("tiling", 512.0f);
        
        // Bind textures.
        int textureUnit = 0;
        for (String uniformName : getTextureUniformNames()) {
            glActiveTexture(GL_TEXTURE0 + textureUnit);
            glBindTexture(GL_TEXTURE_2D, getTexture(uniformName));
            terrainShader.setUniformSampler(uniformName, textureUnit);
            textureUnit++;
        }
        
        // Pass light information to the shader.
        // Make sure you do not exceed MAX_LIGHTS (4 in our shader).
        int numLights = Math.min(lights.size(), 4);
        terrainShader.setUniform1i("numLights", numLights);
        for (int i = 0; i < numLights; i++) {
            Light light = lights.get(i);
            terrainShader.setUniform3f("lightPositions[" + i + "]", light.getPosition());
            terrainShader.setUniform3f("lightColors[" + i + "]", light.getColor());
            terrainShader.setUniform3f("lightAttenuations[" + i + "]", light.getAttenuation());
        }
        
        // Set patch size (3 vertices per patch).
        glPatchParameteri(GL_PATCH_VERTICES, 3);
        
        // Render each adaptive patch.
        for (AdaptiveTerrainGenerator.PatchMesh patch : patches) {
            glBindVertexArray(patch.vaoId);
            glDrawElements(GL_PATCHES, patch.indexCount, GL_UNSIGNED_INT, 0);
            glBindVertexArray(0);
        }
        
        terrainShader.unbind();
    }
    
    public void cleanup() {
        terrainShader.destroy();
    }
    
    public ShaderProgram getShader() {
        return terrainShader;
    }
}
