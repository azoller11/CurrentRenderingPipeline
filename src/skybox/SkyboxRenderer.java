package skybox;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL40;
import shaders.ShaderProgram;

import entities.Light; // Assuming Light is a class representing the sun

import static org.lwjgl.opengl.GL40.*;

public class SkyboxRenderer {
    private final int vao, vertexCount;
    private final ShaderProgram shader;

    public SkyboxRenderer() {
    	this.shader = new ShaderProgram(
        		"src/skybox/skybox_vertex.glsl", null, null, null, "src/skybox/skybox_fragment.glsl");
        SphereMesh sphere = new SphereMesh(50); // Creates a high-detail sphere
        vao = sphere.getVao();
        vertexCount = sphere.getVertexCount();
    }

    public void render(Matrix4f viewMatrix, Matrix4f projectionMatrix, Light sun, int scale) {
        shader.bind();

        // Remove translation components from the view matrix
        Matrix4f skyboxView = new Matrix4f(viewMatrix).scale(scale);
        skyboxView.m30(0);
        skyboxView.m31(0);
        skyboxView.m32(0);

        // Set uniforms
        shader.setUniformMat4("view", skyboxView);
        shader.setUniformMat4("projection", projectionMatrix);
        shader.setUniform3f("topColor", 0.2f, 0.5f, 0.8f);
        shader.setUniform3f("bottomColor", 0.8f, 0.9f, 1.0f);
        
        // Pass sun position and color
        shader.setUniform3f("sunPosition", sun.getPosition());
        shader.setUniform3f("sunColor", sun.getColor());

        // Render skybox
        glBindVertexArray(vao);
        glEnableVertexAttribArray(0);

        glDrawElements(GL_TRIANGLES, vertexCount, GL_UNSIGNED_INT, 0);

        glDisableVertexAttribArray(0);
        glBindVertexArray(0);

        shader.unbind();
    }

    

    public void cleanUp() {
        shader.destroy();
        glDeleteVertexArrays(vao);
    }
}
