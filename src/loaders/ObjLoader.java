package loaders;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import settings.EngineSettings;
import toolbox.Mesh;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

/**
 * OBJ Loader that parses:
 *   v  (positions)
 *   vt (texture coords)
 *   vn (normals) -- but these will be recomputed for smooth shading!
 *   f  (faces) referencing v/t/n indices
 *
 * Produces a Mesh with layout (11 floats/vertex):
 *   [pos.x, pos.y, pos.z,
 *    uv.x, uv.y,
 *    normal.x, normal.y, normal.z,
 *    tangent.x, tangent.y, tangent.z]
 *
 * This version computes smooth normals by averaging the face normals for all vertices
 * sharing the same position.
 */
public class ObjLoader {

    private static final String RES_LOC = "res/";

    // Basic container for each "vertex" in the OBJ (pos/uv/normal).
    // We now also store the original position index for proper normal averaging.
    static class VertexData {
        int posIndex;
        Vector3f position;
        Vector2f uv;
        Vector3f normal;
        // We'll accumulate tangent here
        Vector3f tangent = new Vector3f(0, 0, 0);

        public VertexData(int posIndex, Vector3f pos, Vector2f uv, Vector3f nor) {
            this.posIndex = posIndex;
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
        // Check if the mesh is already loaded and cached
        if (EngineSettings.meshCache.containsKey(objFileName)) {
            //System.out.println("Model \"" + objFileName + "\" retrieved from cache.");
            return EngineSettings.meshCache.get(objFileName);
        }

        File objFile = new File(RES_LOC + objFileName + ".obj");

        List<Vector3f> positions = new ArrayList<>();
        List<Vector2f> texCoords = new ArrayList<>();
        List<Vector3f> normals = new ArrayList<>();

        // Index lists from faces
        List<Integer> indicesPos = new ArrayList<>();
        List<Integer> indicesTex = new ArrayList<>();
        List<Integer> indicesNor = new ArrayList<>();

        float furthestDistanceSquared = 0.0f; // Store squared distance to avoid sqrt computations

        // 1) Parse the OBJ
        try (FileReader fr = new FileReader(objFile);
             BufferedReader reader = new BufferedReader(fr)) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.startsWith("v ")) {
                    Vector3f position = parseVector3f(line);

                    // Update furthest distance
                    float distanceSquared = position.lengthSquared(); // Faster than computing sqrt
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

        // 2) Build an expanded VertexData array for each face vertex.
        // For smooth shading we want to average normals across all vertices that share the same position,
        // so we store the original position index and ignore (override) any normals read from file.
        int numVertices = indicesPos.size(); // 3 per face for a triangle
        VertexData[] vertexDataArray = new VertexData[numVertices];

        for (int i = 0; i < numVertices; i++) {
            int pIndex = indicesPos.get(i) - 1; // OBJ is 1-based
            int tIndex = indicesTex.get(i) - 1;
            // We ignore the file-supplied normals for smooth shading.
            // int nIndex = indicesNor.get(i) - 1;

            Vector3f pos = positions.get(pIndex);

            Vector2f uv = (tIndex >= 0 && tIndex < texCoords.size())
                    ? texCoords.get(tIndex)
                    : new Vector2f(0.0f, 0.0f);

            // Initialize normal to zero vector; it will be computed below.
            Vector3f nor = new Vector3f(0, 0, 0);

            vertexDataArray[i] = new VertexData(pIndex, pos, uv, nor);
        }

        // 3) Compute tangents face-by-face.
        // Each face is 3 consecutive vertices in "vertexDataArray".
        for (int i = 0; i < numVertices; i += 3) {
            VertexData v0 = vertexDataArray[i];
            VertexData v1 = vertexDataArray[i + 1];
            VertexData v2 = vertexDataArray[i + 2];

            computeTangentsForTriangle(v0, v1, v2);
        }

        // 4) Compute smooth normals by averaging the face normals for each unique vertex (by position)
        Vector3f[] smoothNormals = new Vector3f[positions.size()];
        for (int i = 0; i < smoothNormals.length; i++) {
            smoothNormals[i] = new Vector3f(0, 0, 0);
        }

        // Loop over each face (group of 3 vertices)
        for (int i = 0; i < numVertices; i += 3) {
            VertexData v0 = vertexDataArray[i];
            VertexData v1 = vertexDataArray[i + 1];
            VertexData v2 = vertexDataArray[i + 2];

            // Compute the face normal (using cross product of two edges)
            Vector3f edge1 = new Vector3f(v1.position).sub(v0.position);
            Vector3f edge2 = new Vector3f(v2.position).sub(v0.position);
            Vector3f faceNormal = edge1.cross(edge2, new Vector3f()).normalize();

            // Accumulate this face normal into each vertex's smooth normal (using the original position index)
            smoothNormals[v0.posIndex].add(faceNormal);
            smoothNormals[v1.posIndex].add(faceNormal);
            smoothNormals[v2.posIndex].add(faceNormal);
        }

        // Now update each vertex with the normalized smooth normal.
        for (VertexData vd : vertexDataArray) {
            // Use the accumulated normal for the original vertex position and normalize it.
            Vector3f smooth = smoothNormals[vd.posIndex].normalize(new Vector3f());
            vd.normal.set(smooth);
        }

        // 5) Build final float[] with 11 floats per vertex:
        //    (pos.x, pos.y, pos.z, uv.x, uv.y, normal.x, normal.y, normal.z, tangent.x, tangent.y, tangent.z)
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

            // Normalize the tangent (since it was accumulated over faces)
            vd.tangent.normalize();
            finalData[floatIndex++] = vd.tangent.x;
            finalData[floatIndex++] = vd.tangent.y;
            finalData[floatIndex++] = vd.tangent.z;
        }

        float furthestDistance = (float) Math.sqrt(furthestDistanceSquared);

        // 6) Create VAO, VBO
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        FloatBuffer fb = ByteBuffer.allocateDirect(finalData.length * Float.BYTES)
                .order(ByteOrder.nativeOrder())
                .asFloatBuffer();
        fb.put(finalData).flip();
        glBufferData(GL_ARRAY_BUFFER, fb, GL_STATIC_DRAW);

        // pos => loc=0 (3 floats)
        int stride = 11 * Float.BYTES;
        glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0L);
        glEnableVertexAttribArray(0);

        // uv => loc=1 (2 floats)
        glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3L * Float.BYTES);
        glEnableVertexAttribArray(1);

        // normal => loc=2 (3 floats)
        glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5L * Float.BYTES);
        glEnableVertexAttribArray(2);

        // tangent => loc=3 (3 floats)
        glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8L * Float.BYTES);
        glEnableVertexAttribArray(3);

        glBindVertexArray(0);

        Mesh mesh = new Mesh(vao, numVertices, furthestDistance);

        // Store the loaded mesh in the cache
        EngineSettings.meshCache.put(objFileName, mesh);
        //System.out.println("Model \"" + objFileName + "\" loaded and cached.");

        return mesh;
    }

    // ---------------------------------------------------
    //  Tangent Calculation for a Single Triangle
    // ---------------------------------------------------
    private static void computeTangentsForTriangle(VertexData v0, VertexData v1, VertexData v2) {
        // pos edges
        Vector3f edgePos1 = new Vector3f(v1.position).sub(v0.position);
        Vector3f edgePos2 = new Vector3f(v2.position).sub(v0.position);

        // uv edges
        Vector2f edgeUV1 = new Vector2f(v1.uv).sub(v0.uv);
        Vector2f edgeUV2 = new Vector2f(v2.uv).sub(v0.uv);

        float r = (edgeUV1.x * edgeUV2.y - edgeUV1.y * edgeUV2.x);
        if (Math.abs(r) < 0.0001f) {
            // Avoid division by zero (or handle degenerate UV)
            r = 0.0001f;
        }
        float inv = 1.0f / r;

        Vector3f tangent = new Vector3f(
                inv * (edgePos1.x * edgeUV2.y - edgePos2.x * edgeUV1.y),
                inv * (edgePos1.y * edgeUV2.y - edgePos2.y * edgeUV1.y),
                inv * (edgePos1.z * edgeUV2.y - edgePos2.z * edgeUV1.y)
        );

        // Accumulate the tangent into each vertex
        v0.tangent.add(tangent);
        v1.tangent.add(tangent);
        v2.tangent.add(tangent);
    }

    // ---------------------------------------------------
    //  Helpers
    // ---------------------------------------------------

    private static Vector3f parseVector3f(String line) {
        // e.g. "v 0.1 1.2 2.3" or "vn 0.5 0.6 0.7"
        String[] tokens = line.split("\\s+");
        float x = Float.parseFloat(tokens[1]);
        float y = Float.parseFloat(tokens[2]);
        float z = Float.parseFloat(tokens[3]);
        return new Vector3f(x, y, z);
    }

    private static Vector2f parseVector2f(String line) {
        // e.g. "vt 0.5 0.6"
        String[] tokens = line.split("\\s+");
        float u = Float.parseFloat(tokens[1]);
        float v = Float.parseFloat(tokens[2]);

        // Flip the V to match typical OBJ convention
        v = 1.0f - v;

        return new Vector2f(u, v);
    }

    /**
     * Parse a face line: "f v1/t1/n1 v2/t2/n2 v3/t3/n3"
     * We store the indices in separate lists (positions, texCoords, normals).
     */
    private static void parseFace(String line,
                                  List<Integer> indicesPos,
                                  List<Integer> indicesTex,
                                  List<Integer> indicesNor) {
        // e.g. "f 1/1/1 2/2/2 3/3/3"
        String[] tokens = line.split("\\s+");
        // tokens[0] = "f"

        // For a triangle face, we expect 3 vertices
        for (int i = 1; i <= 3; i++) {
            String[] parts = tokens[i].split("/");
            // parts[0] = posIndex
            // parts[1] = texIndex
            // parts[2] = norIndex

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
}
