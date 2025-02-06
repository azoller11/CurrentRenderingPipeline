package terrain;

import org.joml.Vector2f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryUtil;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class AdaptiveTerrainGenerator {

    private float size;
    private OpenSimplexNoise noise;
    
    // Noise parameters.
    private double frequency = 0.001;
    private double amplitude = 450.0; // Height scaling
    private int octaves = 3;
    private double persistence = 0.3;

    // Resolutions for near and far patches.
    private int highResolution; // e.g. 256
    private int lowResolution;  // e.g. 64

    public AdaptiveTerrainGenerator(float size, int highResolution, int lowResolution) {
        this.size = size;
        this.highResolution = highResolution;
        this.lowResolution = lowResolution;
        noise = new OpenSimplexNoise(123456789L);
    }

    /**
     * Generates adaptive terrain patches based on the camera's position.
     */
    public List<PatchMesh> getPatches(Vector3f cameraPos) {
        return generate(cameraPos);
    }

    /**
     * Generates the adaptive terrain patches.
     */
    public List<PatchMesh> generate(Vector3f cameraPos) {
        float halfSize = size / 2.0f;
        // Four quadrants.
        Vector2f[] origins = new Vector2f[] {
            new Vector2f(0, 0),
            new Vector2f(halfSize, 0),
            new Vector2f(0, halfSize),
            new Vector2f(halfSize, halfSize)
        };

        List<PatchMesh> patches = new ArrayList<>();
        for (Vector2f origin : origins) {
            // Compute patch center.
            float patchCenterX = origin.x + halfSize / 2.0f;
            float patchCenterZ = origin.y + halfSize / 2.0f;
            float dx = cameraPos.x - patchCenterX;
            float dz = cameraPos.z - patchCenterZ;
            float dist = (float) Math.sqrt(dx * dx + dz * dz);
            int res = (dist < halfSize * 0.75f) ? highResolution : lowResolution;
            patches.add(generatePatch(origin, halfSize, res));
        }
        return patches;
    }

    /**
     * Generates a single patch mesh given an origin, patch size, and resolution.
     * This version smooths the computed heights before generating the vertices.
     */
    private PatchMesh generatePatch(Vector2f origin, float patchSize, int resolution) {
        int vertexCount = resolution * resolution;
        float[] vertices = new float[vertexCount * 3]; // x, y, z per vertex
        float[] texCoords = new float[vertexCount * 2];  // u, v per vertex
        float[] blendValues = new float[vertexCount];    // blend factor per vertex
        int[] indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // First, compute raw height values.
        float[] heights = new float[vertexCount];
        int pointer = 0;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                float u = (float) j / (resolution - 1);
                float v = (float) i / (resolution - 1);
                float x = origin.x + u * patchSize;
                float z = origin.y + v * patchSize;
                double y = 0;
                double amp = 1.0;
                double freq = frequency;
                for (int o = 0; o < octaves; o++) {
                    y += noise.eval(x * freq, z * freq) * amp;
                    amp *= persistence;
                    freq *= 2;
                }
                y *= amplitude;
                heights[pointer++] = (float) y;
            }
        }
        // Smooth the heights using a simple box blur.
        heights = smoothHeights(heights, resolution, 3);  // 3 iterations; adjust as needed.

        // Now fill the vertex, texture, and blend arrays.
        pointer = 0;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                float u = (float) j / (resolution - 1);
                float v = (float) i / (resolution - 1);
                float x = origin.x + u * patchSize;
                float z = origin.y + v * patchSize;
                float y = heights[pointer];
                vertices[pointer * 3]     = x;
                vertices[pointer * 3 + 1] = y;
                vertices[pointer * 3 + 2] = z;
                texCoords[pointer * 2]     = u;
                texCoords[pointer * 2 + 1] = v;
                // Compute blend value (for example, lower heights are more grass, higher become rock).
                float blend = (y - 20.0f) / 40.0f;
                blend = Math.max(0.0f, Math.min(1.0f, blend));
                blendValues[pointer] = blend;
                pointer++;
            }
        }

        // Create indices for triangles.
        int indexPointer = 0;
        for (int i = 0; i < resolution - 1; i++) {
            for (int j = 0; j < resolution - 1; j++) {
                int topLeft = i * resolution + j;
                int topRight = topLeft + 1;
                int bottomLeft = (i + 1) * resolution + j;
                int bottomRight = bottomLeft + 1;
                indices[indexPointer++] = topLeft;
                indices[indexPointer++] = bottomLeft;
                indices[indexPointer++] = topRight;
                indices[indexPointer++] = topRight;
                indices[indexPointer++] = bottomLeft;
                indices[indexPointer++] = bottomRight;
            }
        }

        // Create and bind the VAO.
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Positions.
        int posVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
        verticesBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(verticesBuffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // Texture Coordinates.
        int texVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, texVboId);
        FloatBuffer texBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texBuffer.put(texCoords).flip();
        glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(texBuffer);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);

        // Blend Values.
        int blendVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, blendVboId);
        FloatBuffer blendBuffer = MemoryUtil.memAllocFloat(blendValues.length);
        blendBuffer.put(blendValues).flip();
        glBufferData(GL_ARRAY_BUFFER, blendBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(blendBuffer);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);

        // Indices.
        int indexVboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
        indicesBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);

        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        return new PatchMesh(vaoId, indices.length);
    }

    /**
     * Applies a simple box blur smoothing on the heights.
     *
     * @param heights    The original height array.
     * @param res        The resolution (number of vertices per row).
     * @param iterations How many times to apply the smoothing.
     * @return The smoothed height array.
     */
    private float[] smoothHeights(float[] heights, int res, int iterations) {
        float[] smoothed = new float[heights.length];
        for (int iter = 0; iter < iterations; iter++) {
            for (int i = 0; i < res; i++) {
                for (int j = 0; j < res; j++) {
                    float sum = 0.0f;
                    int count = 0;
                    // Loop over a 3x3 neighborhood.
                    for (int di = -1; di <= 1; di++) {
                        for (int dj = -1; dj <= 1; dj++) {
                            int ni = i + di, nj = j + dj;
                            if (ni >= 0 && ni < res && nj >= 0 && nj < res) {
                                sum += heights[ni * res + nj];
                                count++;
                            }
                        }
                    }
                    smoothed[i * res + j] = sum / count;
                }
            }
            // Copy smoothed data back into heights for next iteration.
            System.arraycopy(smoothed, 0, heights, 0, heights.length);
        }
        return heights;
    }
    

    
    /**
     * Helper class representing a patch mesh.
     */
    public static class PatchMesh {
        public int vaoId;
        public int indexCount;
        
        public PatchMesh(int vaoId, int indexCount) {
            this.vaoId = vaoId;
            this.indexCount = indexCount;
        }
    }
}
