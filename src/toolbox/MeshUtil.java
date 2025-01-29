package toolbox;


import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL40.*;

public class MeshUtil {

    /**
     * Create a VAO containing a single rotating cube (positions + colors).
     * Returns the VAO ID. Also returns the VBO ID if you wish to track it.
     */
    public static int createCubeVAO() {
        // 6 faces, 2 triangles each => 12 triangles => 36 vertices
        // Each vertex: position (x,y,z) + color (r,g,b) => 6 floats
        float[] vertices = {
                // front face: red
                -0.5f, -0.5f, +0.5f,  1f,0f,0f,
                 0.5f, -0.5f, +0.5f,  1f,0f,0f,
                 0.5f,  0.5f, +0.5f,  1f,0f,0f,
                -0.5f, -0.5f, +0.5f,  1f,0f,0f,
                 0.5f,  0.5f, +0.5f,  1f,0f,0f,
                -0.5f,  0.5f, +0.5f,  1f,0f,0f,

                // back face: green
                 0.5f, -0.5f, -0.5f,  0f,1f,0f,
                -0.5f, -0.5f, -0.5f,  0f,1f,0f,
                -0.5f,  0.5f, -0.5f,  0f,1f,0f,
                 0.5f, -0.5f, -0.5f,  0f,1f,0f,
                -0.5f,  0.5f, -0.5f,  0f,1f,0f,
                 0.5f,  0.5f, -0.5f,  0f,1f,0f,

                // left face: blue
                -0.5f, -0.5f, -0.5f,  0f,0f,1f,
                -0.5f, -0.5f, +0.5f,  0f,0f,1f,
                -0.5f,  0.5f, +0.5f,  0f,0f,1f,
                -0.5f, -0.5f, -0.5f,  0f,0f,1f,
                -0.5f,  0.5f, +0.5f,  0f,0f,1f,
                -0.5f,  0.5f, -0.5f,  0f,0f,1f,

                // right face: yellow
                 0.5f, -0.5f, +0.5f,  1f,1f,0f,
                 0.5f, -0.5f, -0.5f,  1f,1f,0f,
                 0.5f,  0.5f, -0.5f,  1f,1f,0f,
                 0.5f, -0.5f, +0.5f,  1f,1f,0f,
                 0.5f,  0.5f, -0.5f,  1f,1f,0f,
                 0.5f,  0.5f, +0.5f,  1f,1f,0f,

                // top face: cyan
                -0.5f,  0.5f, +0.5f,  0f,1f,1f,
                 0.5f,  0.5f, +0.5f,  0f,1f,1f,
                 0.5f,  0.5f, -0.5f,  0f,1f,1f,
                -0.5f,  0.5f, +0.5f,  0f,1f,1f,
                 0.5f,  0.5f, -0.5f,  0f,1f,1f,
                -0.5f,  0.5f, -0.5f,  0f,1f,1f,

                // bottom face: magenta
                -0.5f, -0.5f, -0.5f,  1f,0f,1f,
                 0.5f, -0.5f, -0.5f,  1f,0f,1f,
                 0.5f, -0.5f, +0.5f,  1f,0f,1f,
                -0.5f, -0.5f, -0.5f,  1f,0f,1f,
                 0.5f, -0.5f, +0.5f,  1f,0f,1f,
                -0.5f, -0.5f, +0.5f,  1f,0f,1f,
        };

        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Upload data
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(vertices.length);
            fb.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        }

        // Position at location=0
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        // Color at location=1
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        return vao;
    }
    
    
}