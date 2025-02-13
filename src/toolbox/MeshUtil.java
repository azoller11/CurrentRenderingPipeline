package toolbox;

import org.lwjgl.system.MemoryStack;

import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL40.*;

public class MeshUtil {

    /**
     * Create a VAO containing a single rotating cube (positions + colors).
     * Returns the VAO ID.
     */
    public int createCubeVAO() {
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
    
    // --- Circle Mesh for Picking Lights ---
    // We'll create a circle mesh (as a triangle fan) for rendering lights.
    // The circle is created only once and its VAO and vertex count are stored.
    
    private static int circleVAO = 0;
    private static int circleVertexCount = 0;
    
    /**
     * Returns the VAO ID for a unit circle mesh.
     * The circle is defined in the XY-plane centered at (0,0,0).
     */
    public int getCircleVAO() {
        if (circleVAO == 0) {
            createCircleMesh();
        }
        return circleVAO;
    }
    
    /**
     * Returns the number of vertices in the circle mesh.
     */
    public int getCircleVertexCount() {
        if (circleVAO == 0) {
            createCircleMesh();
        }
        return circleVertexCount;
    }
    
    /**
     * Creates a circle mesh using a triangle fan.
     * The mesh is defined in the XY-plane and centered at the origin.
     * This method initializes the circleVAO and circleVertexCount.
     */
    private static void createCircleMesh() {
        int numSegments = 32;
        // The vertex count is: one center vertex + one per segment + one extra to close the fan.
        circleVertexCount = numSegments + 2;
        
        float[] vertices = new float[circleVertexCount * 3]; // Only positions (x,y,z)
        
        // Center vertex at (0,0,0)
        vertices[0] = 0.0f;
        vertices[1] = 0.0f;
        vertices[2] = 0.0f;
        
        double angleStep = 2.0 * Math.PI / numSegments;
        for (int i = 0; i <= numSegments; i++) {
            double angle = i * angleStep;
            float x = (float) Math.cos(angle);
            float y = (float) Math.sin(angle);
            // Offset by 1 because index 0 is the center.
            int index = (i + 1) * 3;
            vertices[index] = x;
            vertices[index + 1] = y;
            vertices[index + 2] = 0.0f;
        }
        
        circleVAO = glGenVertexArrays();
        glBindVertexArray(circleVAO);
        
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(vertices.length);
            fb.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);
        }
        
        // Set attribute 0 to be the position (3 floats per vertex)
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 3 * Float.BYTES, 0L);
        
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);
    }
}
