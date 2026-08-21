package terrain;

import org.lwjgl.glfw.GLFW;
import org.lwjgl.util.vector.Vector2f;

import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.dynamics.RigidBody;

import loaders.TextureLoader;
import physics.PhysicsManager;
import toolbox.Mesh;

import java.nio.ByteBuffer;
import java.util.Random;

import settings.EngineSettings;
import settings.EngineSettings.TerrainBrushColor;
import settings.EngineSettings.TerrainBrushTool;

public class TerrainEditor {

	private Random random = new Random();

	// The terrain's current collision body, so it can be swapped out for a fresh one
	// after sculpting. Set once via setTerrainCollisionBody() right after the initial
	// PhysicsManager.addTerrainAccurateCollision(terrain) call in Main's init.
	private RigidBody terrainCollisionBody;

	public void setTerrainCollisionBody(RigidBody terrainCollisionBody) {
		this.terrainCollisionBody = terrainCollisionBody;
	}

	private long lastBlendUploadTime = 0;
	private static final long BLEND_UPLOAD_INTERVAL_NS = 33_000_000; // ~30 fp
	private boolean paintedBefore = false;

	// Height-sculpting state
	private static final long HEIGHT_REBUILD_INTERVAL_NS = 33_000_000; // ~30 fps GPU mesh rebuild
	private static final float HEIGHT_CHANGE_SPEED = 60.0f; // world units/sec at full brush strength (RAISE/LOWER/RANDOM)
	private static final float FLATTEN_SPEED = 4.0f;        // convergence rate/sec toward the captured target height
	private static final float SMOOTH_SPEED = 4.0f;         // convergence rate/sec toward the local neighborhood average
	private boolean sculptStrokeActive = false;
	private boolean heightDataDirty = false;
	private boolean strokeHasUnsavedChanges = false;
	private float flattenTargetHeight = 0f;
	private long lastHeightRebuildTime = 0;

	// Collision is only rebuilt once, when the editor is exited (not per stroke) -
	// rebuilding the terrain's BvhTriangleMeshShape is expensive, and a long editing
	// session can involve many strokes. The expensive part (building the shape from
	// the mesh) runs on a background thread since it's pure CPU work with no Bullet
	// world/GL state involved; only the cheap swap into the dynamicsWorld happens on
	// the main thread, once the background build finishes.
	private boolean wasTerrainEditorActive = false;
	private boolean collisionDirty = false;
	private volatile CollisionShape pendingCollisionShape;
	private boolean collisionRebuildInProgress = false;


	public void update(Terrain terrain, PhysicsManager physics, float mousePointX, float mousePointZ, long window, float deltaTime) {
		if (EngineSettings.SelectedTerrain != terrain) {EngineSettings.SelectedTerrain = terrain;}

		float terrainY = terrain.getHeightOfTerrain(mousePointX, mousePointZ);
		float brushSize = EngineSettings.TerrainBrushSize;
		float paintStrength = 0.5f;


		//Edit the shape of the terrain
	    boolean mouseDown = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;


		if (EngineSettings.TerrainEditor) {
			if (mouseDown && !EngineSettings.overTexture) {
				if (!sculptStrokeActive) {
					// Start of a new stroke: capture the height under the cursor so FLATTEN
					// has a stable target for the whole drag instead of chasing the cursor.
					sculptStrokeActive = true;
					flattenTargetHeight = terrainY;
				}

				applyHeightBrush(terrain, mousePointX, mousePointZ, brushSize, EngineSettings.TerrainTool, deltaTime);
				heightDataDirty = true;
				strokeHasUnsavedChanges = true;

				long now = System.nanoTime();
				if (now - lastHeightRebuildTime > HEIGHT_REBUILD_INTERVAL_NS) {
					terrain.rebuildMeshFromHeightData();
					lastHeightRebuildTime = now;
					heightDataDirty = false;
				}
			} else if (sculptStrokeActive) {
				// Stroke just ended: flush any change that missed the last throttled rebuild
				// so the mesh never lags behind the final brush position, then persist the
				// edited heights to the heightmap file so they survive a reload.
				if (heightDataDirty) {
					terrain.rebuildMeshFromHeightData();
					heightDataDirty = false;
				}
				if (strokeHasUnsavedChanges) {
					terrain.saveHeightMapImage();
					strokeHasUnsavedChanges = false;
					// Collision is now stale, but rebuilding it (an expensive
					// BvhTriangleMeshShape rebuild) is deferred until the editor is
					// exited rather than done per stroke - see below.
					collisionDirty = true;
				}
				sculptStrokeActive = false;
			}
		} else if (wasTerrainEditorActive) {
			// Terrain editor was just exited: kick off the (expensive) collision shape
			// rebuild on a background thread so it doesn't stall the frame, from
			// whatever the final sculpted mesh ended up being.
			if (collisionDirty && physics != null && terrainCollisionBody != null && !collisionRebuildInProgress) {
				collisionDirty = false;
				collisionRebuildInProgress = true;
				Mesh meshSnapshot = terrain.getMesh();
				Thread rebuildThread = new Thread(() -> {
					pendingCollisionShape = physics.createAccurateCollisionMesh(meshSnapshot, 1);
				}, "terrain-collision-rebuild");
				rebuildThread.setDaemon(true);
				rebuildThread.start();
			}
		}
		wasTerrainEditorActive = EngineSettings.TerrainEditor;

		// If a background collision rebuild finished, do the cheap part (swapping it
		// into the physics world) here on the main thread.
		if (pendingCollisionShape != null) {
			CollisionShape shape = pendingCollisionShape;
			pendingCollisionShape = null;
			physics.dynamicsWorld.removeRigidBody(terrainCollisionBody);
			terrainCollisionBody = physics.addStaticCollisionTerrainMesh(terrain, shape);
			collisionRebuildInProgress = false;
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
	
	
	
	
	// Mutates terrain.getHeightData() in place under a circular brush centered on the
	// given world-space point. Does not touch the GPU mesh - the caller is responsible
	// for calling terrain.rebuildMeshFromHeightData() to push the change.
	private void applyHeightBrush(Terrain terrain, float worldX, float worldZ, float brushSize, TerrainBrushTool tool, float deltaTime) {
		float[][] heightData = terrain.getHeightData();
		if (heightData == null || heightData.length == 0) {
			return;
		}

		int gridHeight = heightData.length;
		int gridWidth = heightData[0].length;
		float size = terrain.getSize();
		float maxHeight = terrain.getMaxHeight();

		float localX = worldX - terrain.getX();
		float localZ = worldZ - terrain.getZ();

		// Same world->grid mapping used by Terrain.getHeightOfTerrain / HeightmapLoader.
		float gridXf = (localX / size) * (gridWidth - 1);
		float gridZf = (1.0f - (localZ / size)) * (gridHeight - 1);

		int centerX = Math.round(gridXf);
		int centerZ = Math.round(gridZf);

		int radius = Math.max(1, (int) ((brushSize / size) * (gridWidth - 1)));

		for (int dz = -radius; dz <= radius; dz++) {
			int z = centerZ + dz;
			if (z < 0 || z >= gridHeight) continue;

			for (int dx = -radius; dx <= radius; dx++) {
				int x = centerX + dx;
				if (x < 0 || x >= gridWidth) continue;

				float distance = (float) Math.sqrt(dx * dx + dz * dz);
				if (distance > radius) continue;

				float falloff = 1.0f - (distance / radius);
				falloff *= falloff; // softer brush edge

				switch (tool) {
					case RAISE:
						heightData[z][x] = clamp(heightData[z][x] + falloff * HEIGHT_CHANGE_SPEED * deltaTime, 0f, maxHeight);
						break;

					case LOWER:
						heightData[z][x] = clamp(heightData[z][x] - falloff * HEIGHT_CHANGE_SPEED * deltaTime, 0f, maxHeight);
						break;

					case FLATTEN: {
						float blend = Math.min(1f, FLATTEN_SPEED * deltaTime) * falloff;
						heightData[z][x] += (flattenTargetHeight - heightData[z][x]) * blend;
						break;
					}

					case SMOOTH: {
						float avg = averageNeighborHeight(heightData, x, z, gridWidth, gridHeight);
						float blend = Math.min(1f, SMOOTH_SPEED * deltaTime) * falloff;
						heightData[z][x] += (avg - heightData[z][x]) * blend;
						break;
					}

					case RANDOM: {
						float noise = (random.nextFloat() * 2f - 1f) * falloff * HEIGHT_CHANGE_SPEED * deltaTime;
						heightData[z][x] = clamp(heightData[z][x] + noise, 0f, maxHeight);
						break;
					}
				}
			}
		}
	}

	private float averageNeighborHeight(float[][] heightData, int x, int z, int width, int height) {
		float sum = 0f;
		int count = 0;
		for (int nz = z - 1; nz <= z + 1; nz++) {
			if (nz < 0 || nz >= height) continue;
			for (int nx = x - 1; nx <= x + 1; nx++) {
				if (nx < 0 || nx >= width) continue;
				sum += heightData[nz][nx];
				count++;
			}
		}
		return count > 0 ? sum / count : heightData[z][x];
	}

	private static float clamp(float value, float min, float max) {
		return Math.max(min, Math.min(max, value));
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
