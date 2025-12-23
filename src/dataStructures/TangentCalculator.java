package dataStructures;
import org.lwjgl.util.vector.Vector3f;
import java.util.Arrays;

public class TangentCalculator {

    /**
     * Calculates the tangent vectors for a mesh.
     *
     * @param positions    Vertex positions.
     * @param texCoords    Texture coordinates.
     * @param normals      Vertex normals.
     * @param indices       Indices defining the triangles.
     * @return Tangent vectors for each vertex.
     */
    public static float[] calculateTangents(float[] positions, float[] texCoords, float[] normals, int[] indices) {
        int vertexCount = positions.length / 3;
        float[] tangents = new float[vertexCount * 3];
        float[] tan1 = new float[vertexCount * 3];
        float[] tan2 = new float[vertexCount * 3];

        // Iterate over each triangle
        for (int i = 0; i < indices.length; i += 3) {
            int i1 = indices[i];
            int i2 = indices[i + 1];
            int i3 = indices[i + 2];

            // Positions
            float v1x = positions[i1 * 3];
            float v1y = positions[i1 * 3 + 1];
            float v1z = positions[i1 * 3 + 2];

            float v2x = positions[i2 * 3];
            float v2y = positions[i2 * 3 + 1];
            float v2z = positions[i2 * 3 + 2];

            float v3x = positions[i3 * 3];
            float v3y = positions[i3 * 3 + 1];
            float v3z = positions[i3 * 3 + 2];

            // Texture coordinates
            float w1 = texCoords[i1 * 2];
            float h1 = texCoords[i1 * 2 + 1];

            float w2 = texCoords[i2 * 2];
            float h2 = texCoords[i2 * 2 + 1];

            float w3 = texCoords[i3 * 2];
            float h3 = texCoords[i3 * 2 + 1];

            // Edges of the triangle : position delta
            float x1 = v2x - v1x;
            float y1 = v2y - v1y;
            float z1 = v2z - v1z;

            float x2 = v3x - v1x;
            float y2 = v3y - v1y;
            float z2 = v3z - v1z;

            // UV delta
            float s1 = w2 - w1;
            float t1 = h2 - h1;
            float s2 = w3 - w1;
            float t2 = h3 - h1;

            float r = 1.0f / (s1 * t2 - s2 * t1);
            float sdirx = (t2 * x1 - t1 * x2) * r;
            float sdiry = (t2 * y1 - t1 * y2) * r;
            float sdirz = (t2 * z1 - t1 * z2) * r;

            float tdirx = (s1 * x2 - s2 * x1) * r;
            float tdiry = (s1 * y2 - s2 * y1) * r;
            float tdirz = (s1 * z2 - s2 * z1) * r;

            // Accumulate the tangents and bitangents
            tan1[i1 * 3] += sdirx;
            tan1[i1 * 3 + 1] += sdiry;
            tan1[i1 * 3 + 2] += sdirz;

            tan1[i2 * 3] += sdirx;
            tan1[i2 * 3 + 1] += sdiry;
            tan1[i2 * 3 + 2] += sdirz;

            tan1[i3 * 3] += sdirx;
            tan1[i3 * 3 + 1] += sdiry;
            tan1[i3 * 3 + 2] += sdirz;

            tan2[i1 * 3] += tdirx;
            tan2[i1 * 3 + 1] += tdiry;
            tan2[i1 * 3 + 2] += tdirz;

            tan2[i2 * 3] += tdirx;
            tan2[i2 * 3 + 1] += tdiry;
            tan2[i2 * 3 + 2] += tdirz;

            tan2[i3 * 3] += tdirx;
            tan2[i3 * 3 + 1] += tdiry;
            tan2[i3 * 3 + 2] += tdirz;
        }

        // Calculate the tangents
        for (int i = 0; i < vertexCount; i++) {
            float t1x = tan1[i * 3];
            float t1y = tan1[i * 3 + 1];
            float t1z = tan1[i * 3 + 2];

            float t2x = tan2[i * 3];
            float t2y = tan2[i * 3 + 1];
            float t2z = tan2[i * 3 + 2];

            Vector3f n = new Vector3f(normals[i * 3], normals[i * 3 + 1], normals[i * 3 + 2]);
            Vector3f t = new Vector3f(t1x, t1y, t1z);

            // Gram-Schmidt orthogonalize
            orthoNormalize(n, t);

            // Set the tangent
            tangents[i * 3] = t.x;
            tangents[i * 3 + 1] = t.y;
            tangents[i * 3 + 2] = t.z;
        }

        return tangents;
    }
    
    public static void orthoNormalize(Vector3f normal, Vector3f tangent) {
        // Calculate the dot product of normal and tangent
        float dot = normal.x * tangent.x + normal.y * tangent.y + normal.z * tangent.z;
        
        // Subtract the projection of tangent onto normal
        tangent.x -= normal.x * dot;
        tangent.y -= normal.y * dot;
        tangent.z -= normal.z * dot;
        
        // Normalize the tangent vector
        float length = (float) Math.sqrt(tangent.x * tangent.x + tangent.y * tangent.y + tangent.z * tangent.z);
        if (length != 0.0f) {
            tangent.x /= length;
            tangent.y /= length;
            tangent.z /= length;
        }
    }
    
    /**
     * Calculates the length of a vector.
     *
     * @param v The vector.
     * @return The length of the vector.
     */
    public static float length(Vector3f v) {
        return (float) Math.sqrt(v.x * v.x + v.y * v.y + v.z * v.z);
    }
}

