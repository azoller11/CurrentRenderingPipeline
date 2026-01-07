package terrain;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.vector.Vector2f;

import loaders.TextureLoader;

import java.nio.ByteBuffer;
import java.util.Random;

import settings.EngineSettings;
import settings.EngineSettings.TerrainBrushColor;
import settings.EngineSettings.TerrainBrushTool;

public class TerrainEditor {
	
	private Random random = new Random();
	
	private long lastBlendUploadTime = 0;
	private static final long BLEND_UPLOAD_INTERVAL_NS = 33_000_000; // ~30 fp
	private boolean paintedBefore = false;
	
	
	public void update(Terrain terrain, float mousePointX, float mousePointZ, long window) {
		if (EngineSettings.SelectedTerrain != terrain) {EngineSettings.SelectedTerrain = terrain;}
		
		float terrainY = terrain.getHeightOfTerrain(mousePointX, mousePointZ);
		float brushSize = EngineSettings.TerrainBrushSize;
		float paintStrength = 0.5f;
		
		
		//Edit the shape of the terrain
		String heightMapPath = terrain.getHeightmapPath();
	    boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;

	
		if (EngineSettings.TerrainEditor) {
			if (mouseDown && !EngineSettings.overTexture) {
				if (EngineSettings.TerrainTool == TerrainBrushTool.SMOOTH) {
					System.out.println("Applying change!");
				}
				
				if (EngineSettings.TerrainTool == TerrainBrushTool.FLATTEN) {
					System.out.println("Applying change!");

				}
				
				if (EngineSettings.TerrainTool == TerrainBrushTool.RAISE) {
					System.out.println("Applying change!");

				}
				
				if (EngineSettings.TerrainTool == TerrainBrushTool.LOWER) {
					System.out.println("Applying change!");

				}
				
				if (EngineSettings.TerrainTool == TerrainBrushTool.RANDOM) {
					System.out.println("Applying change!");

				}
			}
			
			
			
			
			
			
			
		}
		
		
		//Edit the textures of the terrain
		String blendMapPath = terrain.getBlendMapPath();
		// In your update method, try this simpler approach first:
		if (EngineSettings.TerrainPainter) {
		    if (mouseDown && !EngineSettings.overTexture) {
		        // DEBUG: Check if we're getting here
		        System.out.println("DEBUG: Painting with color: " + EngineSettings.TerrainColor);
		        
		        boolean changed = paintBlendMap(
		            terrain,
		            mousePointX,
		            mousePointZ,
		            brushSize,
		            paintStrength,
		            EngineSettings.TerrainColor
		        );

		        if (changed) {
		            long now = System.nanoTime();
		            if (now - lastBlendUploadTime > BLEND_UPLOAD_INTERVAL_NS) {
		                
		                // DEBUG: Check buffer before upload
		                ByteBuffer buffer = terrain.getBlendMapImage();
		                System.out.println("DEBUG: Uploading buffer. Position: " + buffer.position() + 
		                                   ", Limit: " + buffer.limit() + ", Capacity: " + buffer.capacity());
		                
		                //buffer.rewind(); // 🔥 Make sure to rewind!
		                
		                TextureLoader.updateTextureData(
		                    terrain.getBlendMapTexture(),
		                    terrain.getBlendMapWidth(),
		                    terrain.getBlendMapHeight(),
		                    buffer
		                );

		                lastBlendUploadTime = now;
		            }
		            
		            terrain.saveBlendMapImage();
		        }
		    }
		}
		
	}
	
	private boolean paintBlendMap(
        Terrain terrain,
        float worldX,
        float worldZ,
        float brushSize,
        float strength,
        TerrainBrushColor color
) {
    
    ByteBuffer image = terrain.getBlendMapImage();
    int width = terrain.getBlendMapWidth();
    int height = terrain.getBlendMapHeight();
    
    if (image == null || width <= 0 || height <= 0) {
        System.out.println("ERROR: Invalid blend map data!");
        return false;
    }
    
    // Save position
    int originalPos = image.position();
    image.rewind();
    
    // DEBUG: Sample some pixels before painting
    if (!paintedBefore) { // Add a class variable: private boolean paintedBefore = false;
        System.out.println("DEBUG: First paint operation - checking initial state");
        System.out.println("DEBUG: Checking pixel at position 0:");
        int r = image.get(0) & 0xFF;
        int g = image.get(1) & 0xFF;
        int b = image.get(2) & 0xFF;
        System.out.println("  R:" + r + " G:" + g + " B:" + b);
        
        // Check middle pixel
        int midIndex = (width/2 + (height/2) * width) * 4;
        if (midIndex < image.capacity() - 4) {
            r = image.get(midIndex) & 0xFF;
            g = image.get(midIndex + 1) & 0xFF;
            b = image.get(midIndex + 2) & 0xFF;
            System.out.println("DEBUG: Middle pixel: R:" + r + " G:" + g + " B:" + b);
        }
        
        paintedBefore = true;
        image.rewind(); // Reset again
    }
    
    Vector2f uv = worldToBlendUV(terrain, worldX, worldZ);
    System.out.println("DEBUG: UV coordinates: " + uv.x + ", " + uv.y);
    
    int centerX = (int)(uv.x * (width - 1));
    int centerY = (int)(uv.y * (height - 1));
    
    System.out.println("DEBUG: Center pixel coordinates: " + centerX + ", " + centerY);
    
    int radius = (int)((brushSize / terrain.getSize()) * width);
    radius = Math.max(1, radius);
    
    System.out.println("DEBUG: Brush radius in pixels: " + radius);
    
    boolean changed = false;
    int pixelsPainted = 0;
    
    for (int dy = -radius; dy <= radius; dy++) {
        for (int dx = -radius; dx <= radius; dx++) {
            int px = centerX + dx;
            int py = centerY + dy;
            
            if (px < 0 || px >= width || py < 0 || py >= height) continue;
            
            float distance = (float)Math.sqrt(dx*dx + dy*dy);
            if (distance > radius) continue;
            
            float falloff = 1.0f - (distance / radius);
            float paintAmount = falloff * strength;
            
            int index = (px + py * width) * 4;
            
            // Get current values
            int r = image.get(index) & 0xFF;
            int g = image.get(index + 1) & 0xFF;
            int b = image.get(index + 2) & 0xFF;
            
            // Debug only the very center pixel
            if (dx == 0 && dy == 0) {
                System.out.println("DEBUG: Painting center pixel at " + px + "," + py);
                System.out.println("DEBUG: Before - R:" + r + " G:" + g + " B:" + b);
                System.out.println("DEBUG: Painting color: " + color + " amount: " + paintAmount);
            }
            
            // Apply paint based on color
            switch (color) {
                case RED:
                    r = Math.min(255, r + (int)(paintAmount * 255));
                    break;
                case GREEN:
                    g = Math.min(255, g + (int)(paintAmount * 255));
                    break;
                case BLUE:
                    b = Math.min(255, b + (int)(paintAmount * 255));
                    break;
                case BLACK:
                    // Don't reduce below 0
                    int reduce = (int)(paintAmount * 255);
                    r = Math.max(0, r - reduce);
                    g = Math.max(0, g - reduce);
                    b = Math.max(0, b - reduce);
                    break;
            }
            
            // Debug only the very center pixel
            if (dx == 0 && dy == 0) {
                System.out.println("DEBUG: After - R:" + r + " G:" + g + " B:" + b);
            }
            
            // Write back
            image.put(index, (byte)r);
            image.put(index + 1, (byte)g);
            image.put(index + 2, (byte)b);
            image.put(index + 3, (byte)255);
            
            changed = true;
            pixelsPainted++;
        }
    }
    
    // Restore original position
    image.position(originalPos);
    
    System.out.println("DEBUG: Pixels painted: " + pixelsPainted);
    return changed;
}
	
	
	
	
	private Vector2f worldToBlendUV(Terrain terrain, float worldX, float worldZ) {
	    float terrainX = worldX - terrain.getX();
	    float terrainZ = worldZ - terrain.getZ();

	    float u = terrainX / terrain.getSize();
	    float v = 1.0f - (terrainZ / terrain.getSize());

	    return new Vector2f(
	        Math.max(0, Math.min(1, u)),
	        Math.max(0, Math.min(1, v))
	    );
	}


}
