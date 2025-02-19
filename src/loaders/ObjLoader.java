package loaders;

import org.joml.Vector2f;
import org.joml.Vector3f;
import settings.EngineSettings;
import toolbox.Mesh;
import toolbox.MeshData;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class ObjLoader {

    private static final String RES_LOC = "res/";
    // Toggle debug output for load timing
    private static final boolean DEBUG = true;

    static class VertexData {
        int posIndex;
        Vector3f position;
        Vector2f uv;
        Vector3f normal;
        Vector3f tangent = new Vector3f(0, 0, 0);

        public VertexData(int posIndex, Vector3f pos, Vector2f uv, Vector3f nor) {
            this.posIndex = posIndex;
            this.position = pos;
            this.uv = uv;
            this.normal = nor;
        }

        public VertexData(Vector3f pos, Vector2f uv, Vector3f nor) {
            this.position = pos;
            this.uv = uv;
            this.normal = nor;
        }
    }

    /**
     * Loads an OBJ model. If the model has already been loaded, it returns the cached Mesh.
     *
     * @param objFileName The name of the OBJ file (without the .obj extension)
     * @return The loaded Mesh
     */
    public static Mesh loadObj(String objFileName) {
        long totalStartTime = System.nanoTime();

        // Check cache first
        if (EngineSettings.meshCache.containsKey(objFileName)) {
            return EngineSettings.meshCache.get(objFileName);
        }

        File objFile = new File(RES_LOC + objFileName + ".obj");

        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        List<Integer> indicesPos = new ArrayList<>();
        List<Integer> indicesTex = new ArrayList<>();
        List<Integer> indicesNor = new ArrayList<>();

        float furthestDistanceSquared = 0.0f;

        // STEP 1: Parse the OBJ file.
        long parseStartTime = System.nanoTime();
        try (FileReader fr = new FileReader(objFile);
             BufferedReader reader = new BufferedReader(fr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    Vector3f position = parseVector3f(line);
                    float distanceSquared = position.lengthSquared();
                    if (distanceSquared > furthestDistanceSquared) {
                        furthestDistanceSquared = distanceSquared;
                    }
                    positions.add(position);
                } else if (line.startsWith("vt ")) {
                    texCoords.add(parseVector2f(line));
                } else if (line.startsWith("vn ")) {
                    normals.add(parseVector3f(line));
                } else if (line.startsWith("f ")) {
                    parseFace(line, indicesPos, indicesTex, indicesNor);
                }
            }
        } catch (IOException e) {
            throw new RuntimeException("Error reading OBJ file: " + objFile.getAbsolutePath(), e);
        }
        long parseEndTime = System.nanoTime();

        // STEP 2: Build VertexData array.
        long vertexDataStartTime = System.nanoTime();
        int numVertices = indicesPos.size();
        VertexData[] vertexDataArray = new VertexData[numVertices];
        for (int i = 0; i < numVertices; i++) {
            int pIndex = indicesPos.get(i) - 1;
            int tIndex = indicesTex.get(i) - 1;
            Vector3f pos = positions.get(pIndex);
            Vector2f uv = (tIndex >= 0 && tIndex < texCoords.size())
                    ? texCoords.get(tIndex)
                    : new Vector2f(0.0f, 0.0f);
            vertexDataArray[i] = new VertexData(pIndex, pos, uv, new Vector3f(0, 0, 0));
        }
        long vertexDataEndTime = System.nanoTime();

        // STEP 3: Compute tangents for each triangle.
        long tangentStartTime = System.nanoTime();
        for (int i = 0; i < numVertices; i += 3) {
            computeTangentsForTriangle(vertexDataArray[i],
                                       vertexDataArray[i + 1],
                                       vertexDataArray[i + 2]);
        }
        long tangentEndTime = System.nanoTime();

        // STEP 4: Compute smooth normals.
        long normalsStartTime = System.nanoTime();
        Vector3f[] smoothNormals = new Vector3f[positions.size()];
        for (int i = 0; i < smoothNormals.length; i++) {
            smoothNormals[i] = new Vector3f(0, 0, 0);
        }
        for (int i = 0; i < numVertices; i += 3) {
            VertexData v0 = vertexDataArray[i];
            VertexData v1 = vertexDataArray[i + 1];
            VertexData v2 = vertexDataArray[i + 2];

            Vector3f edge1 = new Vector3f(v1.position).sub(v0.position);
            Vector3f edge2 = new Vector3f(v2.position).sub(v0.position);
            Vector3f faceNormal = edge1.cross(edge2, new Vector3f()).normalize();

            smoothNormals[v0.posIndex].add(faceNormal);
            smoothNormals[v1.posIndex].add(faceNormal);
            smoothNormals[v2.posIndex].add(faceNormal);
        }
        for (VertexData vd : vertexDataArray) {
            vd.normal.set(smoothNormals[vd.posIndex].normalize(new Vector3f()));
        }
        long normalsEndTime = System.nanoTime();

        // STEP 5: Build final float array.
        long finalDataStartTime = System.nanoTime();
        float[] finalData = new float[numVertices * 11];
        int floatIndex = 0;
        for (int i = 0; i < numVertices; i++) {
            VertexData vd = vertexDataArray[i];
            finalData[floatIndex++] = vd.position.x;
            finalData[floatIndex++] = vd.position.y;
            finalData[floatIndex++] = vd.position.z;
            finalData[floatIndex++] = vd.uv.x;
            finalData[floatIndex++] = vd.uv.y;
            finalData[floatIndex++] = vd.normal.x;
            finalData[floatIndex++] = vd.normal.y;
            finalData[floatIndex++] = vd.normal.z;
            vd.tangent.normalize();
            finalData[floatIndex++] = vd.tangent.x;
            finalData[floatIndex++] = vd.tangent.y;
            finalData[floatIndex++] = vd.tangent.z;
        }
        long finalDataEndTime = System.nanoTime();

        float furthestDistance = (float) Math.sqrt(furthestDistanceSquared);

        // STEP 6: Create VAO, VBO and upload data.
        long uploadStartTime = System.nanoTime();
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = ByteBuffer.allocateDirect(finalData.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(finalData).flip();
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

        glBindVertexArray(0);
        long uploadEndTime = System.nanoTime();

        Mesh mesh = new Mesh(vao, numVertices, furthestDistance);
        EngineSettings.meshCache.put(objFileName, mesh);

        long totalEndTime = System.nanoTime();

        if (DEBUG) {
            String debugInfo = String.format("OBJ Load [%s]: parse=%.2f ms, vertexData=%.2f ms, tangents=%.2f ms, normals=%.2f ms, finalData=%.2f ms, GPU upload=%.2f ms, total=%.2f ms",
                    objFileName,
                    (parseEndTime - parseStartTime) / 1_000_000.0,
                    (vertexDataEndTime - vertexDataStartTime) / 1_000_000.0,
                    (tangentEndTime - tangentStartTime) / 1_000_000.0,
                    (normalsEndTime - normalsStartTime) / 1_000_000.0,
                    (finalDataEndTime - finalDataStartTime) / 1_000_000.0,
                    (uploadEndTime - uploadStartTime) / 1_000_000.0,
                    (totalEndTime - totalStartTime) / 1_000_000.0);
            System.out.println(debugInfo);
        }

        return mesh;
    }
    
    public static MeshData parseMeshDataFromLines(List<String> lines) {
        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        List<Integer> indicesPos = new ArrayList<>();
        List<Integer> indicesTex = new ArrayList<>();
        List<Integer> indicesNor = new ArrayList<>();

        float furthestDistanceSquared = 0.0f;

        // STEP 1: Parse the OBJ lines.
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("v ")) {
                Vector3f position = parseVector3f(line);
                float distanceSquared = position.lengthSquared();
                if (distanceSquared > furthestDistanceSquared) {
                    furthestDistanceSquared = distanceSquared;
                }
                positions.add(position);
            } else if (line.startsWith("vt ")) {
                texCoords.add(parseVector2f(line));
            } else if (line.startsWith("vn ")) {
                normals.add(parseVector3f(line));
            } else if (line.startsWith("f ")) {
                parseFace(line, indicesPos, indicesTex, indicesNor);
            }
        }

        // STEP 2: Build VertexData array.
        int numVertices = indicesPos.size();
        VertexData[] vertexDataArray = new VertexData[numVertices];
        for (int i = 0; i < numVertices; i++) {
            int pIndex = indicesPos.get(i) - 1;
            int tIndex = indicesTex.get(i) - 1;
            Vector3f pos = positions.get(pIndex);
            Vector2f uv = (tIndex >= 0 && tIndex < texCoords.size())
                    ? texCoords.get(tIndex)
                    : new Vector2f(0.0f, 0.0f);
            // Create a new VertexData with a placeholder normal (will be computed later)
            vertexDataArray[i] = new VertexData(pIndex, pos, uv, new Vector3f(0, 0, 0));
        }

        // STEP 3: Compute tangents for each triangle.
        for (int i = 0; i < numVertices; i += 3) {
            computeTangentsForTriangle(vertexDataArray[i],
                                       vertexDataArray[i + 1],
                                       vertexDataArray[i + 2]);
        }

        // STEP 4: Compute smooth normals.
        Vector3f[] smoothNormals = new Vector3f[positions.size()];
        for (int i = 0; i < smoothNormals.length; i++) {
            smoothNormals[i] = new Vector3f(0, 0, 0);
        }
        for (int i = 0; i < numVertices; i += 3) {
            VertexData v0 = vertexDataArray[i];
            VertexData v1 = vertexDataArray[i + 1];
            VertexData v2 = vertexDataArray[i + 2];

            Vector3f edge1 = new Vector3f(v1.position).sub(v0.position);
            Vector3f edge2 = new Vector3f(v2.position).sub(v0.position);
            Vector3f faceNormal = edge1.cross(edge2, new Vector3f()).normalize();

            smoothNormals[v0.posIndex].add(faceNormal);
            smoothNormals[v1.posIndex].add(faceNormal);
            smoothNormals[v2.posIndex].add(faceNormal);
        }
        for (VertexData vd : vertexDataArray) {
            vd.normal.set(smoothNormals[vd.posIndex].normalize(new Vector3f()));
        }

        // STEP 5: Build the final interleaved float array.
        // Format: 3 position, 2 uv, 3 normal, 3 tangent = 11 floats per vertex.
        float[] finalData = new float[numVertices * 11];
        int floatIndex = 0;
        for (int i = 0; i < numVertices; i++) {
            VertexData vd = vertexDataArray[i];
            finalData[floatIndex++] = vd.position.x;
            finalData[floatIndex++] = vd.position.y;
            finalData[floatIndex++] = vd.position.z;
            finalData[floatIndex++] = vd.uv.x;
            finalData[floatIndex++] = vd.uv.y;
            finalData[floatIndex++] = vd.normal.x;
            finalData[floatIndex++] = vd.normal.y;
            finalData[floatIndex++] = vd.normal.z;
            vd.tangent.normalize();
            finalData[floatIndex++] = vd.tangent.x;
            finalData[floatIndex++] = vd.tangent.y;
            finalData[floatIndex++] = vd.tangent.z;
        }

        float furthestDistance = (float) Math.sqrt(furthestDistanceSquared);

        // Package the CPU-side mesh data.
        MeshData meshData = new MeshData();
        meshData.finalData = finalData;
        meshData.vertexCount = numVertices;
        meshData.furthestDistance = furthestDistance;

        return meshData;
    }
    
    
      public static Mesh loadMeshFromLines(List<String> lines) {
        long totalStartTime = System.nanoTime();

        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        List<Integer> indicesPos = new ArrayList<>();
        List<Integer> indicesTex = new ArrayList<>();
        List<Integer> indicesNor = new ArrayList<>();

        float furthestDistanceSquared = 0.0f;

        // STEP 1: Parse the lines.
        long parseStartTime = System.nanoTime();
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("v ")) {
                Vector3f position = parseVector3f(line);
                float distanceSquared = position.lengthSquared();
                if (distanceSquared > furthestDistanceSquared) {
                    furthestDistanceSquared = distanceSquared;
                }
                positions.add(position);
            } else if (line.startsWith("vt ")) {
                texCoords.add(parseVector2f(line));
            } else if (line.startsWith("vn ")) {
                normals.add(parseVector3f(line));
            } else if (line.startsWith("f ")) {
                parseFace(line, indicesPos, indicesTex, indicesNor);
            }
        }
        long parseEndTime = System.nanoTime();

        // STEP 2: Build VertexData array.
        long vertexDataStartTime = System.nanoTime();
        int numVertices = indicesPos.size();
        VertexData[] vertexDataArray = new VertexData[numVertices];
        for (int i = 0; i < numVertices; i++) {
            int pIndex = indicesPos.get(i) - 1;
            int tIndex = indicesTex.get(i) - 1;
            Vector3f pos = positions.get(pIndex);
            Vector2f uv = (tIndex >= 0 && tIndex < texCoords.size())
                    ? texCoords.get(tIndex)
                    : new Vector2f(0.0f, 0.0f);
            vertexDataArray[i] = new VertexData(pIndex, pos, uv, new Vector3f(0, 0, 0));
        }
        long vertexDataEndTime = System.nanoTime();

        // STEP 3: Compute tangents for each triangle.
        long tangentStartTime = System.nanoTime();
        for (int i = 0; i < numVertices; i += 3) {
            computeTangentsForTriangle(vertexDataArray[i],
                                       vertexDataArray[i + 1],
                                       vertexDataArray[i + 2]);
        }
        long tangentEndTime = System.nanoTime();

        // STEP 4: Compute smooth normals.
        long normalsStartTime = System.nanoTime();
        Vector3f[] smoothNormals = new Vector3f[positions.size()];
        for (int i = 0; i < smoothNormals.length; i++) {
            smoothNormals[i] = new Vector3f(0, 0, 0);
        }
        for (int i = 0; i < numVertices; i += 3) {
            VertexData v0 = vertexDataArray[i];
            VertexData v1 = vertexDataArray[i + 1];
            VertexData v2 = vertexDataArray[i + 2];

            Vector3f edge1 = new Vector3f(v1.position).sub(v0.position);
            Vector3f edge2 = new Vector3f(v2.position).sub(v0.position);
            Vector3f faceNormal = edge1.cross(edge2, new Vector3f()).normalize();

            smoothNormals[v0.posIndex].add(faceNormal);
            smoothNormals[v1.posIndex].add(faceNormal);
            smoothNormals[v2.posIndex].add(faceNormal);
        }
        for (VertexData vd : vertexDataArray) {
            vd.normal.set(smoothNormals[vd.posIndex].normalize(new Vector3f()));
        }
        long normalsEndTime = System.nanoTime();

        // STEP 5: Build final float array.
        long finalDataStartTime = System.nanoTime();
        float[] finalData = new float[numVertices * 11]; // 3 pos, 2 uv, 3 normal, 3 tangent = 11 floats
        int floatIndex = 0;
        for (int i = 0; i < numVertices; i++) {
            VertexData vd = vertexDataArray[i];
            finalData[floatIndex++] = vd.position.x;
            finalData[floatIndex++] = vd.position.y;
            finalData[floatIndex++] = vd.position.z;
            finalData[floatIndex++] = vd.uv.x;
            finalData[floatIndex++] = vd.uv.y;
            finalData[floatIndex++] = vd.normal.x;
            finalData[floatIndex++] = vd.normal.y;
            finalData[floatIndex++] = vd.normal.z;
            vd.tangent.normalize();
            finalData[floatIndex++] = vd.tangent.x;
            finalData[floatIndex++] = vd.tangent.y;
            finalData[floatIndex++] = vd.tangent.z;
        }
        long finalDataEndTime = System.nanoTime();

        float furthestDistance = (float) Math.sqrt(furthestDistanceSquared);

        // STEP 6: Create VAO, VBO and upload data.
        long uploadStartTime = System.nanoTime();
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = ByteBuffer.allocateDirect(finalData.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(finalData).flip();
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

        glBindVertexArray(0);
        long uploadEndTime = System.nanoTime();

        Mesh mesh = new Mesh(vao, numVertices, furthestDistance);

        long totalEndTime = System.nanoTime();

        if (DEBUG) {
            String debugInfo = String.format("Mesh Load from lines: parse=%.2f ms, vertexData=%.2f ms, tangents=%.2f ms, normals=%.2f ms, finalData=%.2f ms, GPU upload=%.2f ms, total=%.2f ms",
                    (parseEndTime - parseStartTime) / 1_000_000.0,
                    (vertexDataEndTime - vertexDataStartTime) / 1_000_000.0,
                    (tangentEndTime - tangentStartTime) / 1_000_000.0,
                    (normalsEndTime - normalsStartTime) / 1_000_000.0,
                    (finalDataEndTime - finalDataStartTime) / 1_000_000.0,
                    (uploadEndTime - uploadStartTime) / 1_000_000.0,
                    (totalEndTime - totalStartTime) / 1_000_000.0);
            System.out.println(debugInfo);
        }

        return mesh;
    }
      
      private static final Vector3f tempEdge1 = new Vector3f();
      private static final Vector3f tempEdge2 = new Vector3f();
      private static final Vector2f tempUV1 = new Vector2f();
      private static final Vector2f tempUV2 = new Vector2f();
      
      
      private static void computeTangentsForTriangle(VertexData v0, VertexData v1, VertexData v2) {
    	    // Compute position deltas
    	    tempEdge1.set(v1.position).sub(v0.position);
    	    tempEdge2.set(v2.position).sub(v0.position);

    	    // Compute UV deltas
    	    tempUV1.set(v1.uv).sub(v0.uv);
    	    tempUV2.set(v2.uv).sub(v0.uv);

    	    float r = (tempUV1.x * tempUV2.y - tempUV1.y * tempUV2.x);
    	    if (Math.abs(r) < 0.0001f) {
    	        r = 0.0001f;
    	    }
    	    float inv = 1.0f / r;

    	    Vector3f tangent = new Vector3f(
    	            inv * (tempEdge1.x * tempUV2.y - tempEdge2.x * tempUV1.y),
    	            inv * (tempEdge1.y * tempUV2.y - tempEdge2.y * tempUV1.y),
    	            inv * (tempEdge1.z * tempUV2.y - tempEdge2.z * tempUV1.y)
    	    );

    	    v0.tangent.add(tangent);
    	    v1.tangent.add(tangent);
    	    v2.tangent.add(tangent);
    	}

    private static Vector3f parseVector3f(String line) {
        String[] tokens = line.split("\\s+");
        float x = Float.parseFloat(tokens[1]);
        float y = Float.parseFloat(tokens[2]);
        float z = Float.parseFloat(tokens[3]);
        return new Vector3f(x, y, z);
    }

    private static Vector2f parseVector2f(String line) {
        String[] tokens = line.split("\\s+");
        float u = Float.parseFloat(tokens[1]);
        float v = Float.parseFloat(tokens[2]);
        v = 1.0f - v;
        return new Vector2f(u, v);
    }

    private static void parseFace(String line,
                                  List<Integer> indicesPos,
                                  List<Integer> indicesTex,
                                  List<Integer> indicesNor) {
        String[] tokens = line.split("\\s+");
        List<String> faceVertices = new ArrayList<>();
        for (int i = 1; i < tokens.length; i++) {
            faceVertices.add(tokens[i]);
        }
        int numVertices = faceVertices.size();
        if (numVertices < 3) {
            return;
        }
        for (int i = 1; i < numVertices - 1; i++) {
            processFaceVertex(faceVertices.get(0), indicesPos, indicesTex, indicesNor);
            processFaceVertex(faceVertices.get(i), indicesPos, indicesTex, indicesNor);
            processFaceVertex(faceVertices.get(i + 1), indicesPos, indicesTex, indicesNor);
        }
    }

    private static void processFaceVertex(String vertexToken,
                                          List<Integer> indicesPos,
                                          List<Integer> indicesTex,
                                          List<Integer> indicesNor) {
        String[] parts = vertexToken.split("/");
        int posIndex = Integer.parseInt(parts[0]);
        indicesPos.add(posIndex);

        int texIndex = 0;
        if (parts.length > 1 && !parts[1].isEmpty()) {
            texIndex = Integer.parseInt(parts[1]);
        }
        indicesTex.add(texIndex);

        int norIndex = 0;
        if (parts.length > 2 && !parts[2].isEmpty()) {
            norIndex = Integer.parseInt(parts[2]);
        }
        indicesNor.add(norIndex);
    }
}
