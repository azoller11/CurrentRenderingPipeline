package toolbox;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL40.*;

public class Mesh {

    private final int vaoId;
    private final int vertexCount;
    private float furthestPoint;
    private MeshData meshData;

    public Mesh(int vaoId, int vertexCount) {
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }

    public Mesh(int vao, int numVertices, float furthestDistance) {
        this.vaoId = vao;
        this.vertexCount = numVertices;
        this.furthestPoint = furthestDistance;
    }

    /**
     * New constructor: Create a Mesh from CPU-side MeshData.
     * This constructor creates the VAO, VBO, and uploads the vertex data.
     *
     * The expected vertex layout in MeshData is:
     * - Position: 3 floats
     * - Texture Coordinates (UV): 2 floats
     * - Normal: 3 floats
     * - Tangent: 3 floats
     * (Total: 11 floats per vertex)
     *
     * @param meshData The MeshData containing the interleaved vertex attributes.
     */
    public Mesh(MeshData meshData) {
        // Create a new VAO.
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // Create a new VBO and bind it.
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Convert the finalData array into a FloatBuffer.
        FloatBuffer fb = ByteBuffer
                .allocateDirect(meshData.finalData.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(meshData.finalData).flip();

        // Upload the vertex data to the VBO.
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        // Each vertex consists of 11 floats.
        int stride = 11 * Float.BYTES;

        // Vertex positions (location 0): 3 floats, starting at offset 0.
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        // Texture coordinates (location 1): 2 floats, starting at offset 3 * Float.BYTES.
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Normals (location 2): 3 floats, starting at offset 5 * Float.BYTES.
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        // Tangents (location 3): 3 floats, starting at offset 8 * Float.BYTES.
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);

        // Unbind the VAO to prevent accidental modification.
        glBindVertexArray(0);

        // Store the VAO ID, vertex count, and furthest distance.
        this.vaoId = vao;
        this.vertexCount = meshData.vertexCount;
        this.furthestPoint = meshData.furthestDistance;
        this.meshData = meshData;
    }

    public Mesh(int vao, int numVertices, float furthestDistance, MeshData meshData2) {
    	this.vaoId = vao;
        this.vertexCount = numVertices;
        this.furthestPoint = furthestDistance;
        this.meshData = meshData2;
		// TODO Auto-generated constructor stub
	}

	public int getVaoId() {
        return vaoId;
    }

    public int getVertexCount() {
        return vertexCount;
    }

    public float getFurthestPoint() {
        return furthestPoint;
    }

    public void setFurthestPoint(float furthestPoint) {
        this.furthestPoint = furthestPoint;
    }

	public float[] getVertices() {
		return this.meshData.getVertices();
	}

	public int[] getIndices() {
		// TODO Auto-generated method stub
		return this.meshData.getIndices();
	}
}
