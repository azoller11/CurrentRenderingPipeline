package terrain;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;


import org.joml.Vector3f;
import org.lwjgl.stb.STBImage;
import org.lwjgl.stb.STBImageWrite;
import org.lwjgl.system.MemoryStack;

import loaders.TextureLoader;
import toolbox.Mesh;
import toolbox.MeshData;

public class Terrain {

    private final float x, z;
    private final float size;
    private final float maxHeight;

    private final String heightmapPath;
    private int heightMapTexture;
    
    private TerrainTextureLayer[] layers;
    private int blendMapTexture;
    private String blendMapPath;

    private Mesh mesh;
    private float[][] heightData;
    
    private ByteBuffer blendMapImage;
    private int blendMapWidth;
    private int blendMapHeight;

    public Terrain(float worldX, float worldZ, float size,
                   float maxHeight, String heightmapFile) {

        this.x = worldX;
        this.z = worldZ;
        this.size = size;
        this.maxHeight = maxHeight;
        this.heightmapPath = "res/" + heightmapFile;
        this.heightMapTexture = TextureLoader.loadExplicitTexture(heightmapFile);
        

        generateTerrainMesh();
    }

    public Mesh getMesh() { return mesh; }
    public float getX() { return x; }
    public float getZ() { return z; }
    public float[][] getHeightData() { return heightData; }

    // -------------------------------------------------------------------------------
    // Get terrain height at a given WORLD x,z
    // -------------------------------------------------------------------------------
    public float getHeightOfTerrain(float worldX, float worldZ) {
        if (heightData == null || heightData.length == 0) {
            return 0.0f;
        }

        // Convert world position to local (0..size) space
        float localX = worldX - this.x;
        float localZ = worldZ - this.z;

        // Outside this terrain bounds
        if (localX < 0 || localZ < 0 || localX > size || localZ > size) {
            return 0.0f;
        }

        int height = heightData.length;        // rows
        int width  = heightData[0].length;     // cols

        // Map localX in [0,size] -> xIndex in [0,width-1]
        float gridX = (localX / size) * (width - 1);

        // Z was flipped in HeightmapLoader:
        // worldZ = ((height - 1 - zIndex)/(height - 1)) * size
        // => zIndex = (height - 1) * (1 - localZ/size)
        float gridZ = (1.0f - (localZ / size)) * (height - 1);

        int x0 = (int) Math.floor(gridX);
        int z0 = (int) Math.floor(gridZ);
        int x1 = x0 + 1;
        int z1 = z0 + 1;

        // Clamp to valid indices (or early-out if you prefer)
        if (x0 < 0 || x1 >= width || z0 < 0 || z1 >= height) {
            return 0.0f;
        }

        float sx = gridX - x0; // [0,1] within cell in X
        float sz = gridZ - z0; // [0,1] within cell in Z

        float h00 = heightData[z0][x0];
        float h10 = heightData[z0][x1];
        float h01 = heightData[z1][x0];
        float h11 = heightData[z1][x1];

        // Bilinear interpolation
        float h0 = h00 + (h10 - h00) * sx;
        float h1 = h01 + (h11 - h01) * sx;
        float h  = h0 + (h1 - h0) * sz;

        return h;
    }

    // -------------------------------------------------------------------------------

    private void generateTerrainMesh() {
        MeshData data = HeightmapLoader.load(heightmapPath, size, maxHeight);
        this.mesh = new Mesh(data);  // uses your Mesh class
        this.heightData = HeightmapLoader.getLoadedHeights();
    }

    // Regenerates the GPU mesh from the current (mutated) heightData array and swaps
    // it in, disposing the old VAO/VBO/EBO. Called by the terrain editor after
    // sculpting so height changes are visible immediately.
    public void rebuildMeshFromHeightData() {
        if (heightData == null || heightData.length == 0) {
            return;
        }

        MeshData data = HeightmapLoader.createMeshFromHeightData(heightData, size, maxHeight);
        Mesh newMesh = new Mesh(data);
        Mesh oldMesh = this.mesh;
        this.mesh = newMesh;
        if (oldMesh != null) {
            oldMesh.dispose();
        }
    }

    // Writes the current heightData back out to the heightmap PNG on disk (as a grayscale
    // image, inverse of the mapping HeightmapLoader.load uses to read it in) so sculpted
    // changes survive a reload. Called by the terrain editor once a sculpting stroke ends.
    public void saveHeightMapImage() {
        if (heightData == null || heightData.length == 0 || heightmapPath == null) {
            return;
        }

        final int height = heightData.length;
        final int width = heightData[0].length;

        ByteBuffer buffer = ByteBuffer.allocateDirect(width * height);
        for (int zz = 0; zz < height; zz++) {
            for (int xx = 0; xx < width; xx++) {
                int pixel = Math.round((heightData[zz][xx] / maxHeight) * 255f);
                pixel = Math.max(0, Math.min(255, pixel));
                buffer.put((byte) pixel);
            }
        }
        buffer.flip();

        final String path = heightmapPath;

        Thread saveThread = new Thread(() -> {
            boolean result = STBImageWrite.stbi_write_png(path, width, height, 1, buffer, width);
            if (!result) {
                System.err.println("Failed to save heightmap image: " + path);
            }
        });
        saveThread.setName("HeightMap-Save-Thread");
        saveThread.setDaemon(true);
        saveThread.start();
    }
    
    
    public void loadBlendMapImage() {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer c = stack.mallocInt(1);
            
            // ADD THE TEXTURE_DIR prefix!
            String fullPath = "res/" + blendMapPath;  // <-- FIX HERE
            
            STBImage.stbi_set_flip_vertically_on_load(false);

            blendMapImage = STBImage.stbi_load(fullPath, w, h, c, 4);
            if (blendMapImage == null) {
                throw new RuntimeException("Failed to load blendmap image: " + fullPath +
                        "\n" + STBImage.stbi_failure_reason());
            }

            blendMapWidth = w.get(0);
            blendMapHeight = h.get(0);
            blendMapImage.rewind();
            
           
            blendMapImage.rewind();
        }
    }
    
   public void saveBlendMapImage() {
    if (blendMapImage == null || blendMapPath == null) {
        System.out.println("ERROR: No blend map image to save!");
        return;
    }
    
    // Use the same full path as loadBlendMapImage()
    String fullPath = "res/" + blendMapPath;
    
    // DEBUG: Check what path we're using
    File file = new File(fullPath);
    
    // Create parent directories if they don't exist
    File parentDir = file.getParentFile();
    if (parentDir != null && !parentDir.exists()) {
        if (parentDir.mkdirs()) {
        }
    }
    
    // Check if we can write (skip parent check if parent is null)
    if (parentDir != null) {
    }
    
    
    // Create a duplicate buffer for thread safety
    ByteBuffer bufferCopy = ByteBuffer.allocateDirect(blendMapImage.capacity());
    blendMapImage.rewind();
    bufferCopy.put(blendMapImage);
    blendMapImage.rewind();
    bufferCopy.rewind();
    
    // DEBUG: Check copied buffer
    
    final int width = blendMapWidth;
    final int height = blendMapHeight;
    final String path = fullPath; // Use the full path!
    
    // DEBUG: Check dimensions
    Thread saveThread = new Thread(() -> {
        try {
            System.out.println("DEBUG: Thread started, saving to: " + path);
            
            // Try to save with STB
            boolean result = STBImageWrite.stbi_write_png(
                path, 
                width, 
                height, 
                4, // RGBA
                bufferCopy, 
                width * 4
            );
            
            if (!result) {
                System.err.println("✗ STBImageWrite failed - returned false");
            } else {
                
                // Verify file was written
                File savedFile = new File(path);
                if (savedFile.exists()) {
                } else {
                    System.err.println("✗ File was not created: " + path);
                }
            }
            
        } catch (Exception e) {
            System.err.println("✗ Async save failed: " + e.getMessage());
            e.printStackTrace();
        }
    });
    
    saveThread.setName("BlendMap-Save-Thread");
    saveThread.setDaemon(true);
    saveThread.start();
    
    // Wait a bit to see if thread starts
    try {
        Thread.sleep(100);
    } catch (InterruptedException e) {
        e.printStackTrace();
    }
}
    public ByteBuffer getBlendMapImage() { return blendMapImage; }
    public int getBlendMapWidth() { return blendMapWidth; }
    public int getBlendMapHeight() { return blendMapHeight; }
    
    

    public Vector3f getPosition() {
        return new Vector3f(x, 0, z);
    }
    
    public void setTextureLayers(TerrainTextureLayer[] layers) {
        this.layers = layers;
    }

    public TerrainTextureLayer[] getTextureLayers() {
        return layers;
    }

    public void setBlendMap(String path) {
        this.blendMapPath = path;
        this.blendMapTexture = TextureLoader.loadBlendTexture(blendMapPath);
        loadBlendMapImage();
    }
    
    

    public String getBlendMapPath() {
		return blendMapPath;
	}

	public void setBlendMapPath(String blendMapPath) {
		this.blendMapPath = blendMapPath;
	}

	public int getBlendMapTexture() {
        return blendMapTexture;
    }

	public TerrainTextureLayer[] getLayers() {
		return layers;
	}

	public void setLayers(TerrainTextureLayer[] layers) {
		this.layers = layers;
	}

	public float getSize() {
		return size;
	}

	public float getMaxHeight() {
		return maxHeight;
	}

	public String getHeightmapPath() {
		return heightmapPath;
	}
	

	public int getHeightMapTexture() {
		return heightMapTexture;
	}

	public void setHeightMapTexture(int heightMapTexture) {
		this.heightMapTexture = heightMapTexture;
	}

	public void setBlendMapTexture(int blendMapTexture) {
		this.blendMapTexture = blendMapTexture;
	}

	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}

	public void setHeightData(float[][] heightData) {
		this.heightData = heightData;
	}
    
    
}
