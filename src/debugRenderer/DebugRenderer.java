package debugRenderer;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import entities.Camera;
import shaders.ShaderProgram;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class DebugRenderer {
    private final int maxVertices = 500000;
    private final int vaoId;
    private final int vboId;
    private final ShaderProgram shader; 

    private final List<DebugObject> debugObjects = new ArrayList<>();
    private final float[] buffer = new float[maxVertices * 6]; // 6 floats per vertex (position + color)

    public DebugRenderer() {
        // Create VAO and VBO
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        glBufferData(GL_ARRAY_BUFFER, maxVertices * 6 * Float.BYTES, GL_DYNAMIC_DRAW);

        // Define the vertex layout: 3 floats for position, 3 for color
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);

        // Load the debug shader
        shader = new ShaderProgram("src/debugRenderer/debug_vertex.glsl", null, null, null, "src/debugRenderer/debug_fragment.glsl");
    }

    public DebugPoint addPoint(Vector3f position, Vector3f color) {
        DebugPoint point = new DebugPoint(position, color);
        debugObjects.add(point);
        return point;
    }

    public DebugLine addLine(Vector3f start, Vector3f end, Vector3f color) {
        DebugLine line = new DebugLine(start, end, color);
        debugObjects.add(line);
        return line;
    }

    public DebugTriangle addTriangle(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f color) {
        DebugTriangle triangle = new DebugTriangle(v1, v2, v3, color);
        debugObjects.add(triangle);
        return triangle;
    }

    public DebugSphere addSphere(Vector3f center, float radius, Vector3f color) {
        DebugSphere sphere = new DebugSphere(center, radius, color);
        debugObjects.add(sphere);
        return sphere;
    }

    public void removeObject(DebugObject object) {
        debugObjects.remove(object);
    }

    public void render(Camera camera, Matrix4f projectionMatrix, Matrix4f viewMatrix) {
        if (debugObjects.isEmpty()) return;

        // Enable wireframe mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_LINE);

        // Build vertex data
        List<Float> vertexData = new ArrayList<>();
        for (DebugObject object : debugObjects) {
            object.appendVertexData(vertexData);
        }

        // Upload data to the GPU
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        int vertexCount = Math.min(vertexData.size(), maxVertices * 6);
        for (int i = 0; i < vertexCount; i++) {
            buffer[i] = vertexData.get(i);
        }
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);

        // Render using the debug shader
        shader.bind();

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);

            // Set projection matrix
            projectionMatrix.get(fb);
            shader.setUniformMat4("projection", false, fb);

            // Set view matrix
            fb.clear();
            viewMatrix.get(fb);
            shader.setUniformMat4("view", false, fb);
        }

        glBindVertexArray(vaoId);

        // Draw all primitives
        glDrawArrays(GL_LINES, 0, vertexCount / 6);

        glBindVertexArray(0);
        shader.unbind();

        // Restore default polygon mode
        glPolygonMode(GL_FRONT_AND_BACK, GL_FILL);
        
        clear();
    }
    
    public void clear() {
        debugObjects.clear();
    }

    public void cleanup() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        shader.destroy();
    }
}