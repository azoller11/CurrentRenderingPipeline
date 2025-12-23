package toolbox;

import org.lwjgl.BufferUtils;
import java.nio.FloatBuffer;
import static org.lwjgl.opengl.GL40.*;

public class CubeMesh {
    private final int vao;
    private final int vertexCount;

    // Cube vertices with: position (3), texture coordinates (2), normal (3) = 8 floats per vertex
    private static final float[] VERTICES = {
        // Front face (counter-clockwise)
        // Positions           TexCoords  Normals (pointing out)
        -1, -1,  1,  0, 0,   0, 0, 1,   // bottom-left
        -1,  1,  1,  0, 1,   0, 0, 1,   // top-left
         1,  1,  1,  1, 1,   0, 0, 1,   // top-right
         1,  1,  1,  1, 1,   0, 0, 1,   // top-right (duplicate)
         1, -1,  1,  1, 0,   0, 0, 1,   // bottom-right
        -1, -1,  1,  0, 0,   0, 0, 1,   // bottom-left (duplicate)

        // Back face (counter-clockwise when viewed from outside)
         1, -1, -1,  0, 0,   0, 0, -1,  // bottom-right
         1,  1, -1,  0, 1,   0, 0, -1,  // top-right
        -1,  1, -1,  1, 1,   0, 0, -1,  // top-left
        -1,  1, -1,  1, 1,   0, 0, -1,  // top-left (duplicate)
        -1, -1, -1,  1, 0,   0, 0, -1,  // bottom-left
         1, -1, -1,  0, 0,   0, 0, -1,  // bottom-right (duplicate)

        // Right face
         1, -1,  1,  0, 0,   1, 0, 0,   // bottom-front
         1,  1,  1,  0, 1,   1, 0, 0,   // top-front
         1,  1, -1,  1, 1,   1, 0, 0,   // top-back
         1,  1, -1,  1, 1,   1, 0, 0,   // top-back (duplicate)
         1, -1, -1,  1, 0,   1, 0, 0,   // bottom-back
         1, -1,  1,  0, 0,   1, 0, 0,   // bottom-front (duplicate)

        // Left face
        -1, -1, -1,  0, 0,   -1, 0, 0,  // bottom-back
        -1,  1, -1,  0, 1,   -1, 0, 0,  // top-back
        -1,  1,  1,  1, 1,   -1, 0, 0,  // top-front
        -1,  1,  1,  1, 1,   -1, 0, 0,  // top-front (duplicate)
        -1, -1,  1,  1, 0,   -1, 0, 0,  // bottom-front
        -1, -1, -1,  0, 0,   -1, 0, 0,  // bottom-back (duplicate)

        // Top face
        -1,  1,  1,  0, 0,   0, 1, 0,   // front-left
        -1,  1, -1,  0, 1,   0, 1, 0,   // back-left
         1,  1, -1,  1, 1,   0, 1, 0,   // back-right
         1,  1, -1,  1, 1,   0, 1, 0,   // back-right (duplicate)
         1,  1,  1,  1, 0,   0, 1, 0,   // front-right
        -1,  1,  1,  0, 0,   0, 1, 0,   // front-left (duplicate)

        // Bottom face
        -1, -1, -1,  0, 0,   0, -1, 0,  // back-left
        -1, -1,  1,  0, 1,   0, -1, 0,  // front-left
         1, -1,  1,  1, 1,   0, -1, 0,  // front-right
         1, -1,  1,  1, 1,   0, -1, 0,  // front-right (duplicate)
         1, -1, -1,  1, 0,   0, -1, 0,  // back-right
        -1, -1, -1,  0, 0,   0, -1, 0   // back-left (duplicate)
    };

    public CubeMesh() {
        vertexCount = VERTICES.length / 8; // 8 floats per vertex (3 pos + 2 uv + 3 normal)

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(VERTICES.length);
        buffer.put(VERTICES).flip();

        glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        
        // Position attribute (location = 0) - 3 floats
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 8 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);
        
        // Texture coordinate attribute (location = 1) - 2 floats
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 8 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);
        
        // Normal attribute (location = 2) - 3 floats
        glVertexAttribPointer(2, 3, GL_FLOAT, false, 8 * Float.BYTES, 5 * Float.BYTES);
        glEnableVertexAttribArray(2);

        glBindVertexArray(0);
    }

    public int getVao() { return vao; }
    public int getVertexCount() { return vertexCount; }
}