package toolbox;

/**
 * Container for CPU-side mesh data.
 * This data will be prepared asynchronously, and later used on the main thread
 * to create OpenGL resources.
 */
public class MeshData {
    public float[] vertices;
    public float[] normals;
    public float[] texCoords;
    public int[] indices;
    public int[] tangents;
    public float[] finalData;
    // The number of vertices in the mesh.
    public int vertexCount;
    // Optional: furthest distance from the origin (can be used for scaling, etc.)
    public float furthestDistance;
    
    
    
	public float[] getFinalData() {
		return finalData;
	}
	public void setFinalData(float[] finalData) {
		this.finalData = finalData;
	}
	public int getVertexCount() {
		return vertexCount;
	}
	public void setVertexCount(int vertexCount) {
		this.vertexCount = vertexCount;
	}
	public float getFurthestDistance() {
		return furthestDistance;
	}
	public void setFurthestDistance(float furthestDistance) {
		this.furthestDistance = furthestDistance;
	}
	public float[] getVertices() {
		return vertices;
	}
	public void setVertices(float[] vertices) {
		this.vertices = vertices;
	}
	public float[] getNormals() {
		return normals;
	}
	public void setNormals(float[] normals) {
		this.normals = normals;
	}
	public float[] getTexCoords() {
		return texCoords;
	}
	public void setTexCoords(float[] texCoords) {
		this.texCoords = texCoords;
	}
	public int[] getIndices() {
		return indices;
	}
	public void setIndices(int[] indices) {
		this.indices = indices;
	}
	public int[] getTangents() {
		return tangents;
	}
	public void setTangents(int[] tangents) {
		this.tangents = tangents;
	}
    
    
    
    // Additional data (e.g., tangents) can be added here as needed.

    // You can add constructors, setters, or helper methods here if necessary.
}
