package toolbox;



public class Mesh {

    private final int vaoId;
    private final int vertexCount;
    private float furthestPoint;

    public Mesh(int vaoId, int vertexCount) {
        this.vaoId = vaoId;
        this.vertexCount = vertexCount;
    }

    public Mesh(int vao, int numVertices, float furthestDistance) {
    	this.vaoId = vao;
        this.vertexCount = numVertices;
        this.furthestPoint = furthestDistance;
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
	

  
}
