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
    
    
    
    
	public MeshData(float[] vertices, float[] texCoords, float[] normals, int[] indices, float furthestDistance) {
		super();
		this.vertices = vertices;
		this.normals = normals;
		this.texCoords = texCoords;
		this.indices = indices;
		this.furthestDistance = furthestDistance;
		 // Calculate vertex count (3 floats per vertex position)
        this.vertexCount = vertices.length / 3;
        
        // Generate finalData (interleaved array) and tangents
        generateFinalData();
	}
	
	public MeshData() {
	}
	
	
  

	private void generateFinalData() {
        // Each vertex has: position(3), texCoord(2), normal(3), tangent(3) = 11 floats
        finalData = new float[vertexCount * 11];
        
        // Generate tangents (simplified - all zeros for terrain)
        tangents = new int[vertexCount * 3];
        for (int i = 0; i < tangents.length; i += 3) {
            tangents[i] = 1;   // tangent.x
            tangents[i+1] = 0; // tangent.y
            tangents[i+2] = 0; // tangent.z
        }
        
        // Interleave the data
        for (int i = 0; i < vertexCount; i++) {
            int finalIdx = i * 11;
            int vertIdx = i * 3;
            int texIdx = i * 2;
            int normIdx = i * 3;
            int tanIdx = i * 3;
            
            // Position
            finalData[finalIdx] = vertices[vertIdx];
            finalData[finalIdx + 1] = vertices[vertIdx + 1];
            finalData[finalIdx + 2] = vertices[vertIdx + 2];
            
            // Texture coordinates
            finalData[finalIdx + 3] = texCoords[texIdx];
            finalData[finalIdx + 4] = texCoords[texIdx + 1];
            
            // Normal
            finalData[finalIdx + 5] = normals[normIdx];
            finalData[finalIdx + 6] = normals[normIdx + 1];
            finalData[finalIdx + 7] = normals[normIdx + 2];
            
            // Tangent
            finalData[finalIdx + 8] = tangents[tanIdx];
            finalData[finalIdx + 9] = tangents[tanIdx + 1];
            finalData[finalIdx + 10] = tangents[tanIdx + 2];
        }
    }
	
	
    

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
    
	
    
  
}
