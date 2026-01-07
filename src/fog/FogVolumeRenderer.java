package fog;

import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import entities.Camera;
import entities.Light;
import shaders.ShaderProgram;
import toolbox.CubeMesh;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL40.*;

public class FogVolumeRenderer {

    private final ShaderProgram shader;

    private final int cubeVAO;
    private final int vertexCount;

    // If you want to render fog at half-res later, keep this; for now render at full res.
    private int screenWidth;
    private int screenHeight;

    public FogVolumeRenderer(int screenWidth, int screenHeight) {
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;

        shader = new ShaderProgram(
            "src/fog/fog_vertex.glsl",
            null,
            null,
            null,
            "src/fog/fog_fragment.glsl"
        );

        CubeMesh cubeMesh = new CubeMesh();
        cubeVAO = cubeMesh.getVao();
        vertexCount = cubeMesh.getVertexCount();
    }

    public void resize(int w, int h) {
        this.screenWidth = w;
        this.screenHeight = h;
    }

    /**
     * Renders bounded fog volumes as proxy cubes.
     *
     * @param volumes            List of fog cubes (each is a volume in world space)
     * @param camera             Your camera
     * @param projection         Projection matrix
     * @param sceneDepthTexture  Depth texture from your main scene (GL_DEPTH_COMPONENT texture)
     *                           Pass 0 if you want to disable depth clipping.
     */
    public float time = 0;
    public void render(List<FogVolume> volumes, Camera camera, Matrix4f projection, int depthTexture, List<Light> lights, float deltaTime) {
        if (volumes == null || volumes.isEmpty()) return;

        // --------- FORCE CORRECT STATE (prevents black opaque cube) ---------
        glDisable(GL_DEPTH_TEST);
        glDepthMask(false);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);


        shader.bind();

        // Common uniforms
        shader.setUniformMat4("projection", projection);
        shader.setUniformMat4("view", camera.getViewMatrix());

        shader.setUniformMat4("invProjection", new Matrix4f(projection).invert());
        shader.setUniformMat4("invView", new Matrix4f(camera.getViewMatrix()).invert());

        shader.setUniform3f("cameraPos", camera.getPosition());

        shader.setUniform2f("screenSize", (float)screenWidth, (float)screenHeight);
        
        shader.setUniformLights("lights", lights);
        shader.setUniform1f("scatteringStrength", 0.4f);
        shader.setUniform1f("anisotropyG", -0.6f);
        
        shader.setUniform1f("stepMultiplier", 5.0f);
        
        time += deltaTime;
        shader.setUniform1f("time", time);
        shader.setUniform3f("windDir", new Vector3f(1.0f, 0.0f, 0.3f).normalize());
        shader.setUniform1f("windSpeed", 0.21f); // start small
        

        // Depth texture
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, depthTexture);
        shader.setUniformSampler("depthTexture", 0);

        //shader.setUniform1i("useDepthClip", 1);

        glBindVertexArray(cubeVAO);

        for (FogVolume v : volumes) {
            Matrix4f model = v.getModelMatrix();
            shader.setUniformMat4("model", model);
            shader.setUniformMat4("invModel", new Matrix4f(model).invert());

            // ✅ THIS IS WHAT PREVENTS "BLACK BOX"
           // shader.setUniform4f("fogColor", v.getFogColor());

            //shader.setUniform1f("density", v.getDensity());
            //shader.setUniform1f("density", 0.2f);
            //shader.setUniform1f("noiseScale", v.getNoiseScale());
            //shader.setUniform1f("noiseStrength", v.getNoiseStrength());
            //shader.setUniform1awwwf("baseOpacity", v.getBaseOpacity());
            
            
            //shader.setUniform1f("density", v.getDensity());
            shader.setUniform1f("density", 0.024f);
            shader.setUniform1f("baseOpacity", 1.0f);
            //shader.setUniform1f("baseOpacity", v.getBaseOpacity());
            float cloudLight = 0.75f;
            shader.setUniform4f("fogColor", new  org.joml.Vector4f(0.245f * cloudLight, 0.173f * cloudLight, 0.104f * cloudLight, 1f));
            //shader.setUniform4f("fogColor", v.getFogColor());
            
            glActiveTexture(GL_TEXTURE1);
            glBindTexture(GL_TEXTURE_2D, v.getNoiseTexture());
            shader.setUniformSampler("noiseMap", 1);

            //shader.setUniform1f("noiseScale", v.getNoiseScale());
            shader.setUniform1f("noiseScale", 1.0f);
            shader.setUniform1f("noiseStrength", 6.0f);
            //shader.setUniform1f("noiseStrength", v.getNoiseStrength());

            //shader.setUniform1f("uniformScale", v.getUniformScale());
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

            glDrawArrays(GL_TRIANGLES, 0, vertexCount);
        }

        glBindVertexArray(0);
        shader.unbind();

        // Restore state
        glDepthMask(true);
        glEnable(GL_CULL_FACE);
        glDisable(GL_BLEND);
    }
    public void cleanup() {
        shader.destroy();
    }
}