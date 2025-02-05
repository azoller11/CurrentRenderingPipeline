package skybox;

import org.joml.Matrix4f;
import org.joml.Matrix3f;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;
import shaders.ShaderProgram;
import entities.Light;

import static org.lwjgl.opengl.GL40.*;

public class SkyboxRenderer {
    private final int vao, vertexCount;
    private final ShaderProgram shader;

    private Vector3f topColor;
    private Vector3f bottomColor;

    private float sunAngle = 0.27f; // Controls the sun position
    private float scrollSpeed = 0.02f; // Adjusts sun movement per scroll

    // New variables for sun & moon brightness & size
    private float maxSunBrightness = 2.0f;
    private float maxMoonBrightness = 0.35f;
    private float sunSize = 0.025f;
    private float moonSize = 0.02f;
    
    private float sunBloomAmount = 0.00f;
    private float moonBloomAmount = 0.01f;
    
    private boolean loadedSecView = false;

    public SkyboxRenderer(long window) {
        this.shader = new ShaderProgram(
                "src/skybox/skybox_vertex.glsl", null, null, null, "src/skybox/skybox_fragment.glsl");
        SphereMesh sphere = new SphereMesh(50);
        vao = sphere.getVao();
        vertexCount = sphere.getVertexCount();

        // Scroll callback for sun movement
        GLFW.glfwSetScrollCallback(window, (win, xoffset, yoffset) -> {
            sunAngle += yoffset * scrollSpeed;

            // Keep sunAngle looping continuously
            if (sunAngle > 1.0f) {
                sunAngle -= 2.0f;
            } else if (sunAngle < -1.0f) {
                sunAngle += 2.0f;
            }
            //System.out.print(sunAngle);
        });
    }

    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix, Light sun, Light moon, int scale) {
	    shader.bind();
	
	    // Increase the vertical multiplier for higher sun/moon movement
	    float sunHeightFactor = 2000.0f; // Increased from 180.0f
	
	    // ☀️ Compute Sun Position with increased horizontal and vertical distances
	    float angle = sunAngle * (float) Math.PI * 2.0f;
	    float sunX = (float) Math.cos(angle) * 1000.0f;  // Horizontal distance
	    float sunY = (float) Math.sin(angle) * sunHeightFactor; // Vertical distance now higher
	    float sunZ = (float) Math.sin(angle) * 1000.0f;   // Horizontal distance
	
	    sun.setPosition(new Vector3f(sunX, sunY, sunZ));
	    
	    // 🌙 Compute Moon Position (Opposite of Sun)
	    float moonX = -sunX;
	    float moonY = -sunY;
	    float moonZ = -sunZ;
	    moon.setPosition(new Vector3f(moonX, moonY, moonZ));
	
	    // Update sky colors and light intensities based on the sun's height
	    updateSkyColors(sunY);
	    updateLightIntensity(sun, moon);
	
	    // 🏙 Remove translation from view matrix for the skybox
	    Matrix4f skyboxView = new Matrix4f(viewMatrix).scale(scale);
	    skyboxView.m30(0);
	    skyboxView.m31(0);
	    skyboxView.m32(0);
	    
	    // Set uniforms for shaders
	    shader.setUniformMat4("view", skyboxView);
	    shader.setUniformMat4("projection", projectionMatrix);
	    shader.setUniform3f("topColor", topColor);
	    shader.setUniform3f("bottomColor", bottomColor);
	    shader.setUniform3f("sunPosition", sun.getPosition());
	    shader.setUniform3f("sunColor", sun.getColor());
	    shader.setUniform3f("moonPosition", moon.getPosition());
	    shader.setUniform3f("moonColor", moon.getColor());
	
	    // Set both Sun and Moon as light sources
	    shader.setUniform3f("light1Position", sun.getPosition());
	    shader.setUniform3f("light1Color", sun.getColor());
	    shader.setUniform3f("light2Position", moon.getPosition());
	    shader.setUniform3f("light2Color", moon.getColor());
	
	    // Pass in Sun & Moon size
	    shader.setUniform1f("sunSize", sunSize);
	    shader.setUniform1f("moonSize", moonSize);
	    
	    shader.setUniform1f("sunBloomAmount", sunBloomAmount);
	    shader.setUniform1f("moonBloomAmount", moonBloomAmount);
	
	    if (loadedSecView) {
	         Matrix3f invRotation = new Matrix3f();
	         skyboxView.get3x3(invRotation);
	         shader.setUniformMat3("invViewRotation", invRotation);
	         loadedSecView = true;
	    }
	
	    // Render Skybox
	    glBindVertexArray(vao);
	    glEnableVertexAttribArray(0);
	    glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);
	    glDisableVertexAttribArray(0);
	    glBindVertexArray(0);
	
	    shader.unbind();
	}

    // 🔆 Adjust Light Intensity Based on Horizon Position
    private void updateLightIntensity(Light sun, Light moon) {
        // ☀️ Sun fades in/out based on its height
        float sunHeight = sun.getPosition().y;
        float sunIntensity = smoothstep(-50.0f, 100.0f, sunHeight) * maxSunBrightness; // Scale by max brightness

        // 🌙 Moon fades in/out in the opposite way
        float moonHeight = moon.getPosition().y;
        float moonIntensity = smoothstep(-50.0f, 100.0f, moonHeight) * maxMoonBrightness; // Scale by max brightness

        // Set light colors dynamically with gradual fading
        sun.setColor(new Vector3f(1.0f, 0.9f, 0.6f).mul(sunIntensity));
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

        topColor = new Vector3f(nightTop).lerp(sunsetTop, smoothstep(-0.2f, 0.2f, factor))
                                         .lerp(dayTop, smoothstep(0.2f, 0.7f, factor));

        bottomColor = new Vector3f(nightBottom).lerp(sunsetBottom, smoothstep(-0.2f, 0.2f, factor))
                                               .lerp(dayBottom, smoothstep(0.2f, 0.7f, factor));
    }

    // Smoothstep function
    public static float smoothstep(float edge0, float edge1, float x) {
        float t = Math.max(0.0f, Math.min((x - edge0) / (edge1 - edge0), 1.0f));
        return t * t * (3.0f - 2.0f * t);
    }

    public void cleanUp() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
