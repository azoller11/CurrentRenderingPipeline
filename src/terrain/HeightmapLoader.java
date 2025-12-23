package terrain;

import org.joml.Vector3f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import toolbox.MeshData;

import java.nio.ByteBuffer;
import java.nio.IntBuffer;

public class HeightmapLoader {

    private static float[][] heightData;

    public static float[][] getLoadedHeights() {
        return heightData;
    }

    public static MeshData load(String file, float terrainSize, float maxHeight) {

        ByteBuffer image;
        int width, height;

        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer comp = stack.mallocInt(1);

            image = STBImage.stbi_load(file, w, h, comp, 1);
            if (image == null) {
                throw new RuntimeException("Failed to load heightmap: " + file);
            }

            width = w.get(0);
            height = h.get(0);
        }

        int count = width * height;
        float[] vertices = new float[count * 3];
        float[] normals  = new float[count * 3];
        float[] texCoords = new float[count * 2];
        int[] indices    = new int[(width - 1) * (height - 1) * 6];

        heightData = new float[height][width];

        // ----------------------------------------------------------
        // Fill positions, heights, UVs
        // ----------------------------------------------------------
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {

                int i = z * width + x;

                int pixel = image.get(i) & 0xFF;
                float h = (pixel / 255.0f) * maxHeight;
                heightData[z][x] = h;

                // X in [0, terrainSize]
                vertices[i * 3 + 0] = (float) x / (width - 1) * terrainSize;

                // Y = height
                vertices[i * 3 + 1] = h;

                // Z is flipped so the image row 0 ends up at max Z
                vertices[i * 3 + 2] =
                        (float) (height - 1 - z) / (height - 1) * terrainSize;

                // UV (0..1)
                texCoords[i * 2 + 0] = (float) x / (width - 1);
                texCoords[i * 2 + 1] = (float) z / (height - 1);
            }
        }

        // ----------------------------------------------------------
        // Generate indices (triangle list)
        // ----------------------------------------------------------
        int pointer = 0;
        for (int gz = 0; gz < height - 1; gz++) {
            for (int gx = 0; gx < width - 1; gx++) {
                int topLeft     = gz * width + gx;
                int topRight    = topLeft + 1;
                int bottomLeft  = (gz + 1) * width + gx;
                int bottomRight = bottomLeft + 1;

                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;

                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }

        // ----------------------------------------------------------
        // Generate smooth normals (NO zf, same space as vertices)
        // ----------------------------------------------------------
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {

                int xLeft  = Math.max(0, x - 1);
                int xRight = Math.min(width - 1, x + 1);
                int zUp    = Math.max(0, z - 1);
                int zDown  = Math.min(height - 1, z + 1);

                float hL = heightData[z][xLeft];
                float hR = heightData[z][xRight];
                float hU = heightData[zUp][x];
                float hD = heightData[zDown][x];

                // Gradient in "image index" space
                float dx = hL - hR;
                float dz = hD - hU;  // if this looks flipped, try (hU - hD)

                org.joml.Vector3f normal = new org.joml.Vector3f(dx, 2.0f, dz).normalize();

                int i = z * width + x;
                normals[i * 3 + 0] = normal.x;
                normals[i * 3 + 1] = normal.y;
                normals[i * 3 + 2] = normal.z;
            }
        }

        STBImage.stbi_image_free(image);

        return new MeshData(vertices, texCoords, normals, indices, 1000);
    }
    
    public static MeshData createMeshFromHeightData(float[][] heightData, float terrainSize, float maxHeight) {
        int height = heightData.length;
        int width = heightData[0].length;
        
        int count = width * height;
        float[] vertices = new float[count * 3];
        float[] normals  = new float[count * 3];
        float[] texCoords = new float[count * 2];
        int[] indices    = new int[(width - 1) * (height - 1) * 6];
        
        // ----------------------------------------------------------
        // Fill positions, heights, UVs
        // ----------------------------------------------------------
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                int i = z * width + x;
                
                float h = heightData[z][x];
                
                // X in [0, terrainSize]
                vertices[i * 3 + 0] = (float) x / (width - 1) * terrainSize;
                
                // Y = height
                vertices[i * 3 + 1] = h;
                
                // Z is flipped so the image row 0 ends up at max Z
                vertices[i * 3 + 2] =
                        (float) (height - 1 - z) / (height - 1) * terrainSize;
                
                // UV (0..1)
                texCoords[i * 2 + 0] = (float) x / (width - 1);
                texCoords[i * 2 + 1] = (float) z / (height - 1);
            }
        }
        
        // ----------------------------------------------------------
        // Generate indices (triangle list)
        // ----------------------------------------------------------
        int pointer = 0;
        for (int gz = 0; gz < height - 1; gz++) {
            for (int gx = 0; gx < width - 1; gx++) {
                int topLeft     = gz * width + gx;
                int topRight    = topLeft + 1;
                int bottomLeft  = (gz + 1) * width + gx;
                int bottomRight = bottomLeft + 1;
                
                indices[pointer++] = topLeft;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = topRight;
                
                indices[pointer++] = topRight;
                indices[pointer++] = bottomLeft;
                indices[pointer++] = bottomRight;
            }
        }
        
        // ----------------------------------------------------------
        // Generate smooth normals
        // ----------------------------------------------------------
        for (int z = 0; z < height; z++) {
            for (int x = 0; x < width; x++) {
                
                int xLeft  = Math.max(0, x - 1);
                int xRight = Math.min(width - 1, x + 1);
                int zUp    = Math.max(0, z - 1);
                int zDown  = Math.min(height - 1, z + 1);
                
                float hL = heightData[z][xLeft];
                float hR = heightData[z][xRight];
                float hU = heightData[zUp][x];
                float hD = heightData[zDown][x];
                
                // Gradient in "image index" space
                float dx = hL - hR;
                float dz = hD - hU;  // if this looks flipped, try (hU - hD)
                
                Vector3f normal = new Vector3f(dx, 2.0f, dz).normalize();
                
                int i = z * width + x;
                normals[i * 3 + 0] = normal.x;
                normals[i * 3 + 1] = normal.y;
                normals[i * 3 + 2] = normal.z;
            }
        }
        
        return new MeshData(vertices, texCoords, normals, indices, 1000);
    }
}
