package terrain;

import entities.Light;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;

import shaders.ShaderProgram;
import toolbox.Mesh;

import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class TerrainRenderer {

    private final ShaderProgram shader;

    public TerrainRenderer() {
    	
    	
    	 String[] additionalVertexShaders = new String[] {
  			   
  	   };
              
  	   String[] additionalFragmentShaders = new String[] {
  			   "src/terrain/computeLayerNormal.glsl"
  			   
  	   };
  	   
  	   String[] additionalGeometryShaders = new String[] {
  			  
  	   };
    	
        shader = new ShaderProgram(
                "src/terrain/vertex.glsl",
                null,
                null,
                null,
                "src/terrain/fragment.glsl",
                additionalVertexShaders,
                additionalFragmentShaders,
                additionalGeometryShaders
        );
    }

    public void render(
            Terrain terrain,
            Matrix4f projection,
            Matrix4f view,
            List<Light> lights,
            int shadowMapTexture,
            Matrix4f lightSpaceMatrix,
            Vector3f cameraPos, float outsideAmbience
    ) {
        if (terrain == null || terrain.getMesh() == null) {
            return;
        }

        TerrainTextureLayer[] layers = terrain.getTextureLayers();
        if (layers == null || layers.length < 4) {
            return;
        }

        shader.bind();

        // Matrices
        Matrix4f model = new Matrix4f()
                .translate(terrain.getX(), 0.0f, terrain.getZ());

        shader.setUniformMat4("projection", projection);
        shader.setUniformMat4("view", view);
        shader.setUniformMat4("model", model);
        shader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);

        // Camera & lights
        shader.setUniform3f("cameraPos", cameraPos);
        shader.setUniformLights("lights", lights);
        
        
        
        shader.setUniform1f("shadowStrength", 0.5f * outsideAmbience);
        shader.setUniform1f("terrainShadowStrength", 1.55f * outsideAmbience);
        float terrainAmbience = outsideAmbience * 0.4f ;
        if (terrainAmbience < 0.1f) terrainAmbience = 0.1f;
        shader.setUniform1f("ambientStrength", terrainAmbience);  // almost no ambient
        
        
        //Fog
        shader.setUniform1f("density", 0.0000f * 1.0f);
        shader.setUniform1f("gradient", 0.00001f * 1.0f);
        shader.setUniform3f("fogColor", new Vector3f(0.25f, 0.25f, 0.25f));
        
        
        //System.out.println("outsideAmbience: " + outsideAmbience * 0.15f);
       // shader.setUniform1f("outsideAmbience", );

        // --------------------------
        // Blend map
        // --------------------------
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, terrain.getBlendMapTexture());
        shader.setUniformSampler("blendMap", 0);

        // --------------------------
        // Albedo layers (1..4)
        // --------------------------
        glActiveTexture(GL_TEXTURE1);
        glBindTexture(GL_TEXTURE_2D, layers[0].albedoMap);
        shader.setUniformSampler("layer0", 1);

        glActiveTexture(GL_TEXTURE2);
        glBindTexture(GL_TEXTURE_2D, layers[1].albedoMap);
        shader.setUniformSampler("layer1", 2);

        glActiveTexture(GL_TEXTURE3);
        glBindTexture(GL_TEXTURE_2D, layers[2].albedoMap);
        shader.setUniformSampler("layer2", 3);

        glActiveTexture(GL_TEXTURE4);
        glBindTexture(GL_TEXTURE_2D, layers[3].albedoMap);
        shader.setUniformSampler("layer3", 4);

        // Shadow map (5)
        glActiveTexture(GL_TEXTURE5);
        glBindTexture(GL_TEXTURE_2D, shadowMapTexture);
        shader.setUniformSampler("shadowMap", 5);

        // --------------------------
        // Tile scales
        // --------------------------
        float ts0 = layers[0].tileScale == 0 ? 1.0f : layers[0].tileScale;
        float ts1 = layers[1].tileScale == 0 ? 1.0f : layers[1].tileScale;
        float ts2 = layers[2].tileScale == 0 ? 1.0f : layers[2].tileScale;
        float ts3 = layers[3].tileScale == 0 ? 1.0f : layers[3].tileScale;

        shader.setUniform1f("tileScale0", ts0);
        shader.setUniform1f("tileScale1", ts1);
        shader.setUniform1f("tileScale2", ts2);
        shader.setUniform1f("tileScale3", ts3);

        // --------------------------
        // Normal maps (6..9)
        // --------------------------
        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_2D, layers[0].normalMap);
        shader.setUniformSampler("layer0Normal", 6);
        shader.setUniform1i("hasNormal0", layers[0].hasNormal ? 1 : 0);

        glActiveTexture(GL_TEXTURE7);
        glBindTexture(GL_TEXTURE_2D, layers[1].normalMap);
        shader.setUniformSampler("layer1Normal", 7);
        shader.setUniform1i("hasNormal1", layers[1].hasNormal ? 1 : 0);

        glActiveTexture(GL_TEXTURE8);
        glBindTexture(GL_TEXTURE_2D, layers[2].normalMap);
        shader.setUniformSampler("layer2Normal", 8);
        shader.setUniform1i("hasNormal2", layers[2].hasNormal ? 1 : 0);

        glActiveTexture(GL_TEXTURE9);
        glBindTexture(GL_TEXTURE_2D, layers[3].normalMap);
        shader.setUniformSampler("layer3Normal", 9);
        shader.setUniform1i("hasNormal3", layers[3].hasNormal ? 1 : 0);

        // --------------------------
        // Metallic (10..13)
        // --------------------------
        glActiveTexture(GL_TEXTURE10);
        glBindTexture(GL_TEXTURE_2D, layers[0].metallicMap);
        shader.setUniformSampler("layer0Metallic", 10);
        shader.setUniform1i("hasMetallic0", layers[0].hasMetallic ? 1 : 0);

        glActiveTexture(GL_TEXTURE11);
        glBindTexture(GL_TEXTURE_2D, layers[1].metallicMap);
        shader.setUniformSampler("layer1Metallic", 11);
        shader.setUniform1i("hasMetallic1", layers[1].hasMetallic ? 1 : 0);

        glActiveTexture(GL_TEXTURE12);
        glBindTexture(GL_TEXTURE_2D, layers[2].metallicMap);
        shader.setUniformSampler("layer2Metallic", 12);
        shader.setUniform1i("hasMetallic2", layers[2].hasMetallic ? 1 : 0);

        glActiveTexture(GL_TEXTURE13);
        glBindTexture(GL_TEXTURE_2D, layers[3].metallicMap);
        shader.setUniformSampler("layer3Metallic", 13);
        shader.setUniform1i("hasMetallic3", layers[3].hasMetallic ? 1 : 0);

        // --------------------------
        // Roughness (14..17)
        // --------------------------
        glActiveTexture(GL_TEXTURE14);
        glBindTexture(GL_TEXTURE_2D, layers[0].roughnessMap);
        shader.setUniformSampler("layer0Roughness", 14);
        shader.setUniform1i("hasRoughness0", layers[0].hasRoughness ? 1 : 0);

        glActiveTexture(GL_TEXTURE15);
        glBindTexture(GL_TEXTURE_2D, layers[1].roughnessMap);
        shader.setUniformSampler("layer1Roughness", 15);
        shader.setUniform1i("hasRoughness1", layers[1].hasRoughness ? 1 : 0);

        glActiveTexture(GL_TEXTURE16);
        glBindTexture(GL_TEXTURE_2D, layers[2].roughnessMap);
        shader.setUniformSampler("layer2Roughness", 16);
        shader.setUniform1i("hasRoughness2", layers[2].hasRoughness ? 1 : 0);

        glActiveTexture(GL_TEXTURE17);
        glBindTexture(GL_TEXTURE_2D, layers[3].roughnessMap);
        shader.setUniformSampler("layer3Roughness", 17);
        shader.setUniform1i("hasRoughness3", layers[3].hasRoughness ? 1 : 0);

        // --------------------------
        // AO (18..21)
        // --------------------------
        glActiveTexture(GL_TEXTURE18);
        glBindTexture(GL_TEXTURE_2D, layers[0].aoMap);
        shader.setUniformSampler("layer0Ao", 18);
        shader.setUniform1i("hasAo0", layers[0].hasAo ? 1 : 0);

        glActiveTexture(GL_TEXTURE19);
        glBindTexture(GL_TEXTURE_2D, layers[1].aoMap);
        shader.setUniformSampler("layer1Ao", 19);
        shader.setUniform1i("hasAo1", layers[1].hasAo ? 1 : 0);

        glActiveTexture(GL_TEXTURE20);
        glBindTexture(GL_TEXTURE_2D, layers[2].aoMap);
        shader.setUniformSampler("layer2Ao", 20);
        shader.setUniform1i("hasAo2", layers[2].hasAo ? 1 : 0);

        glActiveTexture(GL_TEXTURE21);
        glBindTexture(GL_TEXTURE_2D, layers[3].aoMap);
        shader.setUniformSampler("layer3Ao", 21);
        shader.setUniform1i("hasAo3", layers[3].hasAo ? 1 : 0);

        // --------------------------
        // Draw terrain mesh
        // --------------------------
        Mesh mesh = terrain.getMesh();
        glBindVertexArray(mesh.getVaoId());

        glEnableVertexAttribArray(0); // position
        glEnableVertexAttribArray(1); // uv
        glEnableVertexAttribArray(2); // normal
        glEnableVertexAttribArray(3); // tangent
        
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
        

        glDrawElements(GL_TRIANGLES,
                mesh.getVertexCount(),
                GL_UNSIGNED_INT,
                0L
        );

        glDisableVertexAttribArray(0);
        glDisableVertexAttribArray(1);
        glDisableVertexAttribArray(2);
        glDisableVertexAttribArray(3);
        glBindVertexArray(0);

        shader.unbind();
    }

    public void cleanup() {
        shader.destroy();
    }
}
