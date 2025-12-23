package renderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.system.MemoryStack;

import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.TexturedModel;
import settings.EngineSettings;
import shaders.ShaderProgram;
import shadows.ShadowRenderer;
import toolbox.Equations;
import toolbox.Frustum;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL40.*;

public class MasterRenderer {
	
    public static final float NEAR_PLANE = 0.1f;
    public static final float FOV = 100;
    public static final float FAR_PLANE = 2000000000;

    private final ShaderProgram shader;
    private final Matrix4f projectionMatrix;
    
    private static int screenWidth;
    private static int screenHeight;
    
    private Frustum frustum;

    public MasterRenderer(int width, int height) {
    	
        MasterRenderer.screenHeight = height;
        MasterRenderer.screenWidth = width;
      
        String vertex       = "src/shaders/vertex.glsl";
        String tess_control = "src/shaders/tess_control.glsl";
        String tess_eval    = "src/shaders/tess_eval.glsl";
        String geometry     = "src/shaders/geometry.glsl";
        String fragment     = "src/shaders/fragment.glsl";
	   
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
        
        shader = new ShaderProgram(
            vertex,
            tess_control,
            tess_eval,
            geometry,
            fragment, 
            additionalVertexShaders,
            additionalFragmentShaders,
            additionalGeometryShaders
        );
        
        // General settings
        glFrontFace(GL_CW);
        glEnable(GL_MULTISAMPLE);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        frustum = new Frustum();

        // Perspective projection
        float aspect = (float) width / height;
        float fovRad = (float) Math.toRadians(FOV);
        float near   = NEAR_PLANE;
        float far    = FAR_PLANE;

        projectionMatrix = new Matrix4f().perspective(fovRad, aspect, near, far);
    }

    public Matrix4f getProjectionMatrix() {
        return projectionMatrix;
    }
    
    public Matrix4f getFlatProjection() {
        return new Matrix4f().ortho2D(0.0f, screenWidth, 0.0f, screenHeight);
    }

    /**
     * Render all entities from the perspective of the camera.
     * @param outsideAmbience value from 0 (night) to 1 (mid-day)
     */
    public void render(List<Entity> entities, List<Light> lights, Camera camera, int shadowMap, float outsideAmbience) {
    	
        shader.bind();
        
        shader.setUniformLights("lights", lights);
        
        shader.setUniform3f(
            "cameraPos",
            camera.getPosition().x,
            camera.getPosition().y,
            camera.getPosition().z
        );

        // View & projection
        Matrix4f view = camera.getViewMatrix();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);

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

        // ------------------------------------------------------------------
        // Global lighting controls driven by outsideAmbience (time of day)
        // ------------------------------------------------------------------
        float dayFactor = clamp01(outsideAmbience); // 0..1
        
        float exposureBase         = 0.7f;
        float ambientBoostBase     = 0.45f;
        float lightBoostBase       = 1.5f;
        float shadowBrightnessBase = -0.25f;
        
        float exposure         = exposureBase         * Equations.lerp(0.9f,  1.05f, dayFactor);
        float ambientBoost     = ambientBoostBase     * Equations.lerp(0.6f,  1.15f, dayFactor);
        float lightBoost       = lightBoostBase       * Equations.lerp(1.0f,  1.1f,  dayFactor);
        float shadowBrightness = shadowBrightnessBase * Equations.lerp(0.6f,  0.85f, dayFactor);

        //shader.setUniform1f("exposure", exposure);
        //shader.setUniform1f("ambientBoost", ambientBoost);
        //shader.setUniform1f("lightBoost", lightBoost);
        //shader.setUniform1f("shadowBrightness", shadowBrightness);
        
       
        
        //System.out.println(dayFactor);
        
        shader.setUniform1f("exposure", 1.25f * dayFactor);
        shader.setUniform1f("ambientBoost", 0 * dayFactor);
        shader.setUniform1f("lightBoost", 0.95f * dayFactor);
        shader.setUniform1f("shadowBrightness", 0.085f * dayFactor);

        // Raw sun ambience 0..1 straight to the shader now
        shader.setUniform1f("outsideAmbience",1);
        
        
        //Vegitation lighting
        float vegDayFactor = (dayFactor * 0.9f);
        if (vegDayFactor < 0.1f)
        	vegDayFactor = 0.1f;
        
        float vegShadowFactor = (dayFactor * -1.4f);
        if (vegShadowFactor < -0.9f)
        	vegShadowFactor = -0.9f;
        
        shader.setUniform1f("vegitationAmbiance", 0.3f * dayFactor);
        shader.setUniform3f("vegitationColorAdjust",new Vector3f(1.4f * vegDayFactor,1.1f * vegDayFactor,1.1f * vegDayFactor));
        shader.setUniform1f("vegitationLightReduction", 0.2f * dayFactor);
        shader.setUniform1f("vegitationShadowBrightness", vegShadowFactor);
        
        // Recalc frustum
        frustum.calculateFrustum(projectionMatrix, view);

        // -------------------------
        // BATCH BY TEXTUREDMODEL
        // -------------------------
        Map<TexturedModel, List<Entity>> batches = new HashMap<>();

        for (Entity entity : entities) {
            if (entity == null || entity.getTexturedModel() == null) {
                continue;
            }
            TexturedModel model = entity.getTexturedModel();
            // Frustum cull early if you want:
            if (!frustum.contains(
                    entity.getPosition(),
                    entity.getTexturedModel().getMesh().getFurthestPoint() * entity.getScale())) {
                continue;
            }
            batches.computeIfAbsent(model, k -> new ArrayList<>()).add(entity);
        }

        // -------------------------
        // DRAW EACH BATCH
        // -------------------------
        for (Map.Entry<TexturedModel, List<Entity>> entry : batches.entrySet()) {
            TexturedModel model = entry.getKey();
            List<Entity> batch  = entry.getValue();

            if (batch.isEmpty()) continue;

            prepareTexturedModel(model, shadowMap, dayFactor);

            Mesh mesh = model.getMesh();
            glBindVertexArray(mesh.getVaoId());
            glPatchParameteri(GL_PATCH_VERTICES, 3);

            for (Entity entity : batch) {
                // Per-entity model matrix
                loadModelMatrix(entity);
                // If you ever add per-entity overrides, do them here
                glDrawArrays(GL_PATCHES, 0, mesh.getVertexCount());
            }

            glBindVertexArray(0);
        }

        shader.unbind();
    }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------
    private void prepareTexturedModel(TexturedModel model, int shadowMap, float outsideAmbience) {
        // Base texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, model.getTextureId());
        shader.setUniform1i("diffuseTexture", 0);

        boolean hasMetallic  = (model.getMetallicMap()   != 0);
        boolean hasRoughness = (model.getRoughnessMap()  != 0);
        boolean hasAo        = (model.getAoMap()         != 0);
        boolean hasNormalMap = (model.getNormalMapId()   != 0);
        boolean hasHeightMap = (model.getHeighMapId()    != 0);

        // Normal map
        if (hasNormalMap) {
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, model.getNormalMapId());
            shader.setUniform1i("normalMap", 1);
        }

        // Height map + parallax
        if (hasHeightMap) {
            glActiveTexture(GL_TEXTURE2);
            glBindTexture(GL_TEXTURE_2D, model.getHeighMapId());
            shader.setUniform1i("heightMap", 2);

            if (model.getParallaxScale() != null) {
                shader.setUniform1f("parallaxScale", model.getParallaxScale().x);
                shader.setUniform1f("minLayers",     model.getParallaxScale().y);
                shader.setUniform1f("maxLayers",     model.getParallaxScale().z);
            }
        }

        // Metallic
        if (hasMetallic) {
            glActiveTexture(GL_TEXTURE3);
            glBindTexture(GL_TEXTURE_2D, model.getMetallicMap());
            shader.setUniform1i("metallicMap", 3);
        }
        // Roughness
        if (hasRoughness) {
            glActiveTexture(GL_TEXTURE4);
            glBindTexture(GL_TEXTURE_2D, model.getRoughnessMap());
            shader.setUniform1i("roughnessMap", 4);
        }
        // AO
        if (hasAo) {
            glActiveTexture(GL_TEXTURE5);
            glBindTexture(GL_TEXTURE_2D, model.getAoMap());
            shader.setUniform1i("aoMap", 5);
        }

        // Shadow map
        glActiveTexture(GL_TEXTURE6);
        glBindTexture(GL_TEXTURE_2D, shadowMap);
        shader.setUniform1i("shadowMap", 6);

        shader.setUniform1i("hasMetallic",  hasMetallic  ? 1 : 0);
        shader.setUniform1i("hasRoughness", hasRoughness ? 1 : 0);
        shader.setUniform1i("hasAo",        hasAo        ? 1 : 0);
        shader.setUniform1i("hasNormal",    hasNormalMap ? 1 : 0);
        shader.setUniform1i("hasHeight",    hasHeightMap ? 1 : 0);

        shader.setUniform1i("isOpaquePass", model.isHasOpaque() ? 1 : 0);

        shader.setUniform1f("shineDamper",  model.getShineDamper());
        shader.setUniform1f("reflectivity", model.getReflectivity());
        
        shader.setUniform1i("useFakeLighting", model.isUseFakeLighting() ? 1 : 0);
        
        
        //shader.setUniform1i("useFakeMetallic",  model.isUseFakeMetallic()  ? 1 : 0);
        //shader.setUniform1i("useFakeRoghness",  model.isUseFakeRoghness()  ? 1 : 0);
        //shader.setUniform1i("useFakeAO",  model.isUseFakeAO()  ? 1 : 0);
        //shader.setUniform1i("useFakeHeight",  model.isUseFakeHeight()  ? 1 : 0);
        //shader.setUniform1i("useFakeNormals",  model.isUseFakeNormals()  ? 1 : 0);
        
        shader.setUniform1i("isVegitation", model.isVegitation() ? 1 : 0);
        

        // Transparency / culling per model
        if (model.isHasTransparency()) {
            glDisable(GL_CULL_FACE);
            glEnable(GL_BLEND);

            if (model.isVegitation()) {
                glBlendFunc(GL_ONE, GL_ONE_MINUS_SRC_ALPHA); // ✅ premultiplied
            } else {
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }
        }
        	else {
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glCullFace(GL11.GL_BACK);
            // blending stays enabled globally as you configured, which is fine
        }

        // If you ever want per-model ambience tweaks:
        shader.setUniform1f("outsideAmbience", outsideAmbience);
    }

    private void loadModelMatrix(Entity entity) {
        Matrix4f model = entity.getModelMatrix();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            shader.setUniformMat4("model", false, fb);
        }
    }
    
    private float clamp01(float v) {
        if (v < 0.0f) return 0.0f;
        if (v > 1.0f) return 1.0f;
        return v;
    }
    
    public static Matrix4f createTransformationMatrix(
            Vector3f translation,
            float rx, float ry, float rz,
            float scale,
            Vector3f pivot
    ) {
        Matrix4f matrix = new Matrix4f().identity();
        
        matrix.translate(translation);
        matrix.translate(pivot);
        matrix
            .rotateX((float)Math.toRadians(rx))
            .rotateY((float)Math.toRadians(ry))
            .rotateZ((float)Math.toRadians(rz));
        matrix.scale(scale);
        matrix.translate(new Vector3f(-pivot.x, -pivot.y, -pivot.z));
        
        return matrix;
    }
    
    public static int getScreenWidth() {
        return screenWidth;
    }

    public void setScreenWidth(int screenWidth) {
        MasterRenderer.screenWidth = screenWidth;
    }

    public static int getScreenHeight() {
        return screenHeight;
    }

    public void setScreenHeight(int screenHeight) {
        MasterRenderer.screenHeight = screenHeight;
    }

    /**
     * Cleanup the shader (call at end of program).
     */
    public void cleanup() {
        shader.destroy();
    }

}
