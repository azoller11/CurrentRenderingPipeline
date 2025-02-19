package skybox;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import shaders.ShaderProgram;
import entities.Camera;
import entities.Light;

import static org.lwjgl.opengl.GL40.*;

public class SkyboxRenderer {
    private final int vao, vertexCount;
    private final ShaderProgram shader;

    private Vector3f topColor;
    private Vector3f bottomColor;

    // Controls progression along the orbit (in radians). 
    // When sunAngle == 0, the sun will be overhead (if orbitRotation==0).
    private float sunAngle = 0.5f;
    private float scrollSpeed = 0.02f; // Adjusts sun movement per scroll

    // New variable: Adjusts the rotation (or tilt) of the sun/moon orbit around the Y-axis.
    // When orbitRotation == 0, the orbit’s “zero” position is along the positive Y axis.
    private float orbitRotation = 0.25f; // in radians

    // New variables for sun & moon brightness & size
    private float maxSunBrightness = 3.2f;
    private float maxMoonBrightness = 0.25f;
    private float sunSize = 0.015f;
    private float moonSize = 0.02f;
    
    private float sunBloomAmount = 0.015f; // Adjusted for a smoother halo
    private float moonBloomAmount = 0.01f;
    
    private float currentSunIntensity = 0.0f; // Stores computed intensity for the sun
    
    // Variables for extra sun color and warm tint.
    private Vector3f sunCoreColor;  // The “core” disk color
    private Vector3f sunHaloColor;  // The halo (bloom) color
    private float sunWarmFactor = 0.0f;  // 1 when sun is near horizon, 0 when high in the sky

    private boolean loadedSecView = false;
    
    private boolean isSunOut = false;

    public SkyboxRenderer(long window) {
        this.shader = new ShaderProgram(
                "src/skybox/skybox_vertex.glsl", null, null, null, "src/skybox/skybox_fragment.glsl");
        SphereMesh sphere = new SphereMesh(50);
        vao = sphere.getVao();
        vertexCount = sphere.getVertexCount();

        // Scroll callback for sun movement.
        GLFW.glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            // Adjust the sun's progression along the orbit.
            sunAngle += yoffset * scrollSpeed;
            // Keep sunAngle looping continuously over 0 to 2*PI.
            sunAngle = (float) ((sunAngle % (2 * Math.PI) + (2 * Math.PI)) % (2 * Math.PI));
        });
    }
    
    /**
     * Optional: Setter to allow external code (or input) to adjust the orbit rotation.
     * @param orbitRotation The new orbit rotation in radians.
     */
    public void setOrbitRotation(float orbitRotation) {
        this.orbitRotation = orbitRotation;
    }

    public void render(Camera camera, Matrix4f viewMatrix, Matrix4f projectionMatrix, Light sun, Light moon, int scale) {
        shader.bind();
    
        // Increase the vertical multiplier for sun/moon movement.
        float sunHeightFactor = 7000.0f; // Determines the “radius” of the sun's orbit.
    
        // --- Compute Sun Position using spherical coordinates ---
        // We want a full daily cycle:
        //   - When sunAngle == 0: cos(0)==1 so sun is at maximum height.
        //   - When sunAngle == PI/2: sun is at the horizon (y=0).
        //   - When sunAngle == PI: sun is at the nadir.
        float r = sunHeightFactor;
        float sunY = (float) Math.cos(sunAngle) * r;
        float horizontalDistance = (float) Math.sin(sunAngle) * r;
        
        // Apply the orbitRotation to rotate the horizontal (x,z) position.
        float sunX = horizontalDistance * (float) Math.cos(orbitRotation);
        float sunZ = horizontalDistance * (float) Math.sin(orbitRotation);
        sun.setPosition(new Vector3f(sunX, sunY, sunZ));
        
        // --- Compute Moon Position as the opposite of the Sun ---
        // Simply invert the sun's position.
        moon.setPosition(new Vector3f(-sunX, -sunY, -sunZ));
        
        // Update our boolean based on the sun's y-position.
        isSunOut = sun.getPosition().y >= 0;
    
        // Update sky colors and light intensities based on the sun's height.
        updateSkyColors(sunY);
        updateLightIntensity(sun, moon, sunHeightFactor);
    
        // Remove translation from view matrix for the skybox.
        Matrix4f skyboxView = new Matrix4f(viewMatrix).scale(scale);
        skyboxView.m30(0);
        skyboxView.m31(0);
        skyboxView.m32(0);
        
        // Set uniforms for shaders.
        shader.setUniformMat4("view", skyboxView);
        shader.setUniformMat4("projection", projectionMatrix);
        shader.setUniform3f("topColor", topColor);
        shader.setUniform3f("bottomColor", bottomColor);
        shader.setUniform3f("sunPosition", sun.getPosition());
        shader.setUniform3f("moonPosition", moon.getPosition());
    
        // Extra light sources.
        shader.setUniform3f("light1Position", sun.getPosition());
        shader.setUniform3f("light1Color", sun.getColor());
        shader.setUniform3f("light2Position", moon.getPosition());
        shader.setUniform3f("light2Color", moon.getColor());
    
        // Pass in sun and moon size and bloom.
        shader.setUniform1f("sunSize", sunSize);
        shader.setUniform1f("moonSize", moonSize);
        shader.setUniform1f("sunBloomAmount", sunBloomAmount);
        shader.setUniform1f("moonBloomAmount", moonBloomAmount);
    
        // Pass extra sun color uniforms and the warm factor.
        shader.setUniform3f("sunCoreColor", sunCoreColor);
        shader.setUniform3f("sunHaloColor", sunHaloColor);
        shader.setUniform1f("sunWarmFactor", sunWarmFactor);
    
        if (loadedSecView) {
            Matrix3f invRotation = new Matrix3f();
            skyboxView.get3x3(invRotation);
            shader.setUniformMat3("invViewRotation", invRotation);
            loadedSecView = true;
        }
    
        // Render Skybox.
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);
        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
        glDisableVertexAttribArray(0);
        glBindVertexArray(0);
    
        shader.unbind();
    }

    /**
     * Computes light intensity and computes the sun’s color.
     * Uses the sun’s y-position to determine a “warm factor” that is high near the horizon.
     */
    private void updateLightIntensity(Light sun, Light moon, float sunHeightFactor) {
        float sunY = sun.getPosition().y;
        // Compute overall sun intensity (fading in/out as the sun rises/sets).
        currentSunIntensity = smoothstep(-50.0f, 100.0f, sunY) * maxSunBrightness;
        
        // Compute a normalized altitude for the sun.
        // We assume that the horizon corresponds roughly to sunY = 0 and a “low” sun up to 200 units.
        float normalizedAlt = clamp(sunY / 200.0f, 0.0f, 1.0f);
        // Use smoothstep to get a warm factor that is 1 at the horizon and 0 at or above 200 units.
        sunWarmFactor = 1.0f - smoothstep(0.0f, 0.2f, normalizedAlt);
    
        // Define “noon” (default) colors.
        Vector3f noonSunCore  = new Vector3f(1.0f, 0.9f, 0.6f);  // Bright yellowish white.
        Vector3f noonSunHalo  = new Vector3f(1.0f, 0.7f, 0.3f);  // Soft halo.
    
        // Define “warm” colors for sunrise/sunset.
        Vector3f warmSunCore  = new Vector3f(1.0f, 0.4f, 0.0f);  // Deep orange.
        Vector3f warmSunHalo  = new Vector3f(1.0f, 0.3f, 0.0f);  // Intense orange halo.
    
        // Blend the colors based on the warm factor.
        sunCoreColor = new Vector3f(noonSunCore).lerp(warmSunCore, sunWarmFactor).mul(currentSunIntensity);
        sunHaloColor = new Vector3f(noonSunHalo).lerp(warmSunHalo, sunWarmFactor).mul(currentSunIntensity * 0.6f);
    
        // Set the sun’s base color (for extra lights, etc.) using the computed core color.
        sun.setColor(new Vector3f(sunCoreColor));
    
        // For the moon we use a simple smoothstep (inversely to the sun).
        float moonY = moon.getPosition().y;
        float moonIntensity = smoothstep(-50.0f, 100.0f, moonY) * maxMoonBrightness;
        moon.setColor(new Vector3f(0.9f, 0.9f, 1.0f).mul(moonIntensity));
    }
    
    private void updateSkyColors(float sunY) {
        float factor = ((sunY + 50.0f) / 100.0f);

        Vector3f dayTop = new Vector3f(0.2f, 0.5f, 0.8f);
        Vector3f dayBottom = new Vector3f(0.8f, 0.9f, 1.0f);
        Vector3f sunsetTop = new Vector3f(1.0f, 0.3f, 0.0f);
        Vector3f sunsetBottom = new Vector3f(1.0f, 0.6f, 0.3f);
        Vector3f nightTop = new Vector3f(0.02f, 0.02f, 0.1f);
        Vector3f nightBottom = new Vector3f(0.05f, 0.05f, 0.2f);

        topColor = new Vector3f(nightTop)
                        .lerp(sunsetTop, smoothstep(-0.2f, 0.2f, factor))
                        .lerp(dayTop, smoothstep(0.2f, 0.7f, factor));

        bottomColor = new Vector3f(nightBottom)
                        .lerp(sunsetBottom, smoothstep(-0.2f, 0.2f, factor))
                        .lerp(dayBottom, smoothstep(0.2f, 0.7f, factor));
    }

    // Helper: clamp a value between min and max.
    private static float clamp(float val, float min, float max) {
        return Math.max(min, Math.min(max, val));
    }
    
    // Smoothstep function.
    public static float smoothstep(float edge0, float edge1, float x) {
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

    public void setSunOut(boolean isSunOut) {
        this.isSunOut = isSunOut;
    }
}
