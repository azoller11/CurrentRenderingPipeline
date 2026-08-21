package toolbox;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;

public class Mesh {

    private final int vaoId;
    private final int vertexCount;
    private float furthestPoint;
    private MeshData meshData;
    private int vboId = 0;
    private int eboId = 0;

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
        this.meshData = meshData;

        // ----- VAO -----
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // ----- VBO (vertex data) -----
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = ByteBuffer
                .allocateDirect(meshData.finalData.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(meshData.finalData).flip();

        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        int stride = 11 * Float.BYTES;

        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);

        // ----- EBO (indices) -----
        int ebo = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ebo);   // MUST stay bound to VAO

        IntBuffer ib = ByteBuffer
                .allocateDirect(meshData.indices.length * Integer.BYTES)
                .order(ByteOrder.nativeOrder())
                .asIntBuffer();
        ib.put(meshData.indices).flip();

        glBufferData(GL_ELEMENT_ARRAY_BUFFER, ib, GL_STATIC_DRAW);

        // ----- store handles -----
        this.vaoId = vao;
        this.vboId = vbo;
        this.eboId = ebo;
        this.vertexCount = meshData.indices.length;
        this.furthestPoint = meshData.furthestDistance;

        // ----- unbind VAO (BUT NEVER EBO) -----
        glBindVertexArray(0); // good
        // DO NOT glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, 0);
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

	// One entry per unique source vertex position (e.g. one per OBJ "v" line), for
	// building compact convex collision hulls. Falls back to the full per-corner
	// array if the mesh source didn't populate this (e.g. procedurally built meshes).
	public float[] getUniquePositions() {
		float[] unique = this.meshData.getUniquePositions();
		return unique != null ? unique : this.meshData.getVertices();
	}

	public int[] getIndices() {
		// TODO Auto-generated method stub
		return this.meshData.getIndices();
	}

	public float[] getTextureCoords() {
		// TODO Auto-generated method stub
		return this.meshData.texCoords;
	}

	public float[] getNormals() {
		// TODO Auto-generated method stub
		return this.meshData.normals;
	}

	public int getIndexCount() {
		// TODO Auto-generated method stub
		return this.meshData.indices.length;
	}

	public void updateVertexData(float[] vertexArray) {
		this.meshData.vertices = vertexArray;
		
	}

	public void updateNormals(float[] normals) {
		this.meshData.normals = normals;

	}

	// Frees the GPU buffers for this mesh. Only safe to call once nothing is still
	// referencing/drawing this Mesh (e.g. right after it has been swapped out for a
	// freshly rebuilt one, as terrain sculpting does).
	public void dispose() {
		if (vboId != 0) glDeleteBuffers(vboId);
		if (eboId != 0) glDeleteBuffers(eboId);
		if (vaoId != 0) glDeleteVertexArrays(vaoId);
	}


}
