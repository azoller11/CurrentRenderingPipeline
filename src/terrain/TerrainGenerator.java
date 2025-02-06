package terrain;

import org.lwjgl.system.MemoryUtil;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;

public class TerrainGenerator {

    private Mesh terrainMesh;
    private float size;
    private int resolution;
    
    // Noise parameters.
    private OpenSimplexNoise noise;
    private double frequency = 0.001;
    private double amplitude = 250.0; // Height
    private int octaves = 3;
    private double persistence = 0.3;

    /**
     * Creates a mountainous terrain grid on the X-Z plane with texture blend information.
     *
     * @param size       The overall width/length of the terrain.
     * @param resolution The number of vertices per row/column.
     */
    public TerrainGenerator(float size, int resolution) {
        this.size = size;
        this.resolution = resolution;
        // Use a fixed seed (or random seed) for noise.
        noise = new OpenSimplexNoise(123456789L);
        
        // Create arrays for vertex positions, texture coordinates, blend values, and indices.
        int vertexCount = resolution * resolution;
        float[] vertices = new float[vertexCount * 3];    // x, y, z per vertex
        float[] texCoords = new float[vertexCount * 2];     // u, v per vertex
        float[] blendValues = new float[vertexCount];       // blend factor per vertex
        int[] indices = new int[(resolution - 1) * (resolution - 1) * 6];

        // Fill in vertex positions, texture coordinates and blend values.
        int vertexPointer = 0;
        for (int i = 0; i < resolution; i++) {
            for (int j = 0; j < resolution; j++) {
                // X and Z coordinates.
                float x = ((float) j / (resolution - 1)) * size;
                float z = ((float) i / (resolution - 1)) * size;
                
                // Compute height using fractal noise.
                double y = 0;
                double amp = 1.0;
                double freq = frequency;
                for (int o = 0; o < octaves; o++) {
                    y += noise.eval(x * freq, z * freq) * amp;
                    amp *= persistence;
                    freq *= 2;
                }
                y *= amplitude; // Scale the final height.
                
                vertices[vertexPointer * 3] = x;
                vertices[vertexPointer * 3 + 1] = (float) y;
                vertices[vertexPointer * 3 + 2] = z;
                
                // Texture coordinates.
                texCoords[vertexPointer * 2] = (float) j / (resolution - 1);
                texCoords[vertexPointer * 2 + 1] = (float) i / (resolution - 1);
                
                // Compute a blend factor based on height.
                // For example: below 20 = 0 (grass), above 60 = 1 (rock).
                float blend = (float)((y - 20.0) / 40.0);
                blend = Math.max(0.0f, Math.min(1.0f, blend));
                blendValues[vertexPointer] = blend;
                
                vertexPointer++;
            }
        }

        // Create indices for triangles (two triangles per grid square).
        int pointer = 0;
        for (int gz = 0; gz < resolution - 1; gz++) {
            for (int gx = 0; gx < resolution - 1; gx++) {
                int topLeft = (gz * resolution) + gx;
                int topRight = topLeft + 1;
                int bottomLeft = ((gz + 1) * resolution) + gx;
                int bottomRight = bottomLeft + 1;

                // First triangle.
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;
                // Second triangle.
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }

        // Create and bind the VAO.
        int vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // --- Vertex Positions (Attribute 0) ---
        int posVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, posVboId);
        FloatBuffer verticesBuffer = MemoryUtil.memAllocFloat(vertices.length);
        verticesBuffer.put(vertices).flip();
        glBufferData(GL_ARRAY_BUFFER, verticesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(verticesBuffer);
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // --- Texture Coordinates (Attribute 1) ---
        int texVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, texVboId);
        FloatBuffer texBuffer = MemoryUtil.memAllocFloat(texCoords.length);
        texBuffer.put(texCoords).flip();
        glBufferData(GL_ARRAY_BUFFER, texBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(texBuffer);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(1);
        
        // --- Blend Values (Attribute 2) ---
        int blendVboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, blendVboId);
        FloatBuffer blendBuffer = MemoryUtil.memAllocFloat(blendValues.length);
        blendBuffer.put(blendValues).flip();
        glBufferData(GL_ARRAY_BUFFER, blendBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(blendBuffer);
        glVertexAttribPointer(2, 1, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(2);

        // --- Indices ---
        int indexVboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, indexVboId);
        IntBuffer indicesBuffer = MemoryUtil.memAllocInt(indices.length);
        indicesBuffer.put(indices).flip();
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, indicesBuffer, GL_STATIC_DRAW);
        MemoryUtil.memFree(indicesBuffer);

        // Unbind the VAO (the EBO remains bound to it).
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        glBindVertexArray(0);

        // (Optional) Compute a bounding sphere value if needed.
        float furthest = 0;
        for (int i = 0; i < vertexCount; i++) {
            float vx = vertices[i * 3];
            float vy = vertices[i * 3 + 1];
            float vz = vertices[i * 3 + 2];
            float len = (float) Math.sqrt(vx * vx + vy * vy + vz * vz);
            if (len > furthest) {
                furthest = len;
            }
        }

        // Create the Mesh using your available constructor.
        this.terrainMesh = new Mesh(vaoId, indices.length, furthest);
    }

    /**
     * Returns the generated terrain mesh.
     *
     * @return The terrain Mesh.
     */
    public Mesh getTerrainMesh() {
        return terrainMesh;
    }
}
