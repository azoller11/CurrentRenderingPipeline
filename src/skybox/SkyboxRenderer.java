package skybox;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import shaders.ShaderProgram;
import entities.Camera;
import entities.Light;
import settings.EngineSettings;

import static org.lwjgl.opengl.GL40.*;

public class SkyboxRenderer {
	
    public float ambience = 0;

    private final int vao, vertexCount;
    private final ShaderProgram shader;

    private Vector3f topColor;
    private Vector3f bottomColor;

    // Sun orbit controls
    private float sunAngle = 0.01f;
    private float scrollSpeed = 0.02f;
    private float orbitRotation = 0.25f; // Y-axis rotation (tilt)

    // Sun/moon parameters
    private float maxSunBrightness = 3.55f;
    private float maxMoonBrightness = 1.00f;

    private float sunSize = 0.025f;
    private float moonSize = 0.05f;

    private float sunBloomAmount = 0.025f;
    private float moonBloomAmount = 0.001f;

    // Final computed values
    private Vector3f sunCoreColor = new Vector3f();
    private Vector3f sunHaloColor = new Vector3f();
    private float sunWarmFactor = 0.0f;

    private boolean loadedSecView = false;
    private boolean isSunOut = false;

    // Skybox textures (cube-style, 6 faces)
    // Order: +X, -X, +Y, -Y, +Z, -Z
    private boolean hasSkybox = false;
    private int[] skyboxTextures = new int[6];  // OpenGL IDs
    private float skyboxBlend = 1.0f;           // 0 = full procedural, 1 = full textured
    
    private float skyboxTextureBrightness = 1.75f;

    public SkyboxRenderer(long window) {
        shader = new ShaderProgram(
                "src/skybox/skybox_vertex.glsl",
                null, null, null,
                "src/skybox/skybox_fragment.glsl"
        );

        SphereMesh sphere = new SphereMesh(50);
        vao = sphere.getVao();
        vertexCount = sphere.getVertexCount();

        // Mouse wheel scroll – changes time-of-day
        GLFW.glfwSetScrollCallback(window, (win, xo, yo) -> {
            if (EngineSettings.grabMouse) {
                sunAngle += yo * scrollSpeed;
                sunAngle = (float)((sunAngle % (2 * Math.PI) + (2 * Math.PI)) % (2 * Math.PI));
            }
        });
    }

    public void setOrbitRotation(float orbitRotation) {
        this.orbitRotation = orbitRotation;
    }

    public void setSkyboxTextures(int[] tex) {
        if (tex != null && tex.length >= 6) {
            hasSkybox = true;
            // Use first 6 as cube faces: +X, -X, +Y, -Y, +Z, -Z
            for (int i = 0; i < 6; i++) {
                skyboxTextures[i] = tex[i];
            }
        } else {
            hasSkybox = false;
        }
    }

    public void setSkyboxBlend(float blend) {
        skyboxBlend = Math.max(0.0f, Math.min(1.0f, blend));
    }

    public void render(Camera camera, Matrix4f viewMatrix,
                       Matrix4f projectionMatrix,
                       Light sun, Light moon, int scale) {

        shader.bind();

        float sunHeightFactor = 7000.0f;
        float r = sunHeightFactor;

        // --- SUN POSITION ---
        float sunY = (float)Math.cos(sunAngle) * r;
        float horiz = (float)Math.sin(sunAngle) * r;

        float sunX = horiz * (float)Math.cos(orbitRotation);
        float sunZ = horiz * (float)Math.sin(orbitRotation);

        sun.setPosition(new Vector3f(sunX, sunY, sunZ));
        


        // --- MOON POSITION (opposite sun) ---
        moon.setPosition(new Vector3f(-sunX, -sunY, -sunZ));

        // --- SUN ABOVE HORIZON? ---
        isSunOut = (sunY >= 0);

        // --- UPDATE SKY + SUN COLORS ---
        updateSkyColors(sunY);
        updateSunAndMoonIntensity(sun, moon, sunHeightFactor);

        ambience = computeAmbience(sun, sunHeightFactor);

        shader.setUniform1f("density", 0.000020f * 1.0f);
        shader.setUniform1f("gradient", 0.215f * 1.0f);
        shader.setUniform3f("fogColor", new Vector3f(0.4f, 0.4f, 0.4f));
        
        
        // --- SKYBOX VIEW MATRIX (no translation, with optional scale) ---
        Matrix4f skyboxView = new Matrix4f(viewMatrix).scale(scale);
        skyboxView.m30(0);
        skyboxView.m31(0);
        skyboxView.m32(0);

        shader.setUniformMat4("view", skyboxView);
        shader.setUniformMat4("projection", projectionMatrix);

        shader.setUniform3f("topColor", topColor);
        shader.setUniform3f("bottomColor", bottomColor);

        shader.setUniform3f("sunPosition", sun.getPosition());
        shader.setUniform3f("moonPosition", moon.getPosition());

        shader.setUniform3f("light1Position", sun.getPosition());
        shader.setUniform3f("light1Color", sun.getColor());

        shader.setUniform3f("light2Position", moon.getPosition());
        shader.setUniform3f("light2Color", moon.getColor());

        shader.setUniform1f("sunSize", sunSize);
        shader.setUniform1f("moonSize", moonSize);

        shader.setUniform1f("sunBloomAmount", sunBloomAmount);
        shader.setUniform1f("moonBloomAmount", moonBloomAmount);

        shader.setUniform3f("sunCoreColor", sunCoreColor);
        shader.setUniform3f("sunHaloColor", sunHaloColor);

        if (loadedSecView) {
            Matrix3f invRotation = new Matrix3f();
            skyboxView.get3x3(invRotation);
            shader.setUniformMat3("invViewRotation", invRotation);
        }

        // --- SKYBOX TEXTURE UNIFORMS ---
        shader.setUniform1i("hasSkybox", hasSkybox ? 1 : 0);
        shader.setUniform1f("skyboxBlend", skyboxBlend);
        
        
        
        shader.setUniform1f("skyboxTextureBrightness", ambience * (skyboxTextureBrightness * 0.4f));


        if (hasSkybox) {
            for (int i = 0; i < 6; i++) {
                glActiveTexture(GL_TEXTURE0 + i);
                glBindTexture(GL_TEXTURE_2D, skyboxTextures[i]);
                shader.setUniform1i("faces[" + i + "]", i);
            }
        }

        // --- RENDER SKYBOX ---
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);

        shader.unbind();
    }

    public float computeAmbience(Light sun, float heightFactor) {
        float ambienceFalloff = 3.8f;
        float alt = clamp(sun.getPosition().y / heightFactor, -1f, 1f);
        float ambience = smoothstep(-0.25f * ambienceFalloff, 0.15f * ambienceFalloff, alt);
        return ambience;
    }

    // ============================================================
    // REALISTIC SUN + MOON INTENSITY
    // ============================================================
    private void updateSunAndMoonIntensity(Light sun, Light moon, float heightFactor) {

        float y = sun.getPosition().y;
        float alt = y / heightFactor; // -1..1

        // True altitude angle
        float altitudeAngle = (float)Math.asin(clamp(alt, -1f, 1f));

        // Atmospheric extinction — fades sunlight near horizon
        float extinction = (float)Math.exp(-4.0f * Math.max(0.0f, 0.2f - altitudeAngle));

        // Warm tint near horizon  
        sunWarmFactor = 1.0f - clamp(altitudeAngle / 0.5f, 0, 1);

        Vector3f horizonColor = new Vector3f(1.3f, 0.65f, 0.2f);

        // Base noon colors
        Vector3f noonCore = new Vector3f(1.0f, 0.9f, 0.7f);
        Vector3f noonHalo = new Vector3f(1.0f, 0.65f, 0.3f);

        float brightness = (maxSunBrightness - 0.8f) * extinction;

        sunCoreColor = new Vector3f(noonCore)
                .lerp(horizonColor, sunWarmFactor)
                .mul(brightness);

        sunHaloColor = new Vector3f(noonHalo)
                .lerp(horizonColor, sunWarmFactor)
                .mul(brightness * 0.7f);

        sun.setColor(new Vector3f(sunCoreColor));

        // Moon simple lighting
        float moonAlt = moon.getPosition().y / heightFactor;
        float moonIntensity = smoothstep(-0.1f, 0.1f, moonAlt) * maxMoonBrightness;

        moon.setColor(new Vector3f(0.9f, 0.9f, 1.0f).mul(moonIntensity));
    }

    // ============================================================
    // REALISTIC SKY GRADIENT (DAY → SUNSET → NIGHT)
    // ============================================================
    private void updateSkyColors(float sunY) {

        float sunAlt = sunY / 7000.0f;

        Vector3f zenithDay = new Vector3f(0.45f, 0.70f, 1.05f);
        Vector3f horizonDay = new Vector3f(0.85f, 0.95f, 1.10f);

        Vector3f zenithSunset = new Vector3f(1.05f, 0.55f, 0.30f);
        Vector3f horizonSunset = new Vector3f(1.35f, 0.75f, 0.40f);

        Vector3f zenithNight = new Vector3f(0.06f, 0.08f, 0.15f);
        Vector3f horizonNight = new Vector3f(0.10f, 0.12f, 0.20f);

        float dayF    = smoothstep(0.02f, 0.28f, sunAlt);
        float sunsetF = smoothstep(-0.25f, 0.25f, sunAlt);
        float nightF  = 1.0f - dayF;

        topColor = new Vector3f(zenithNight)
                .lerp(zenithSunset, sunsetF)
                .lerp(zenithDay, dayF);

        bottomColor = new Vector3f(horizonNight)
                .lerp(horizonSunset, sunsetF)
                .lerp(horizonDay, dayF);
    }

    // ============================================================
    // Utils
    // ============================================================
    public void setSkyboxTextureBrightness(float brightness) {
        this.skyboxTextureBrightness = Math.max(0.0f, brightness);
    }
    
    private static float clamp(float v, float min, float max) {
        return Math.max(min, Math.min(max, v));
    }

    private static float smoothstep(float edge0, float edge1, float x) {
        float t = clamp((x - edge0) / (edge1 - edge0), 0.0f, 1.0f);
        return t * t * (3.0f - 2.0f * t);
    }

    public void cleanUp() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }

    public boolean isSunOut() {
        return isSunOut;
    }

    public void setSunOut(boolean v) {
        isSunOut = v;
    }
}
