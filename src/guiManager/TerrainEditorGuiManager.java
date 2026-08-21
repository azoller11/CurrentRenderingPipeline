package guiManager;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import gui.GuiButton;
import gui.GuiTexture;
import gui.TextureRenderer;
import main.Main;
import renderer.MasterRenderer;
import settings.EngineSettings;
import settings.EngineSettings.TerrainBrushTool;
import terrain.Terrain;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;

public class TerrainEditorGuiManager {
	
private boolean loadedTextures;
	
	private List<GuiTexture> textures;
	private List<GuiButton> buttons;
	
	private GuiButton terrainHeightMap;
	
	private GuiButton flatTool;
	private GuiButton randomTool;
	private GuiButton smoothTool;
	private GuiButton raiseTool;
	private GuiButton lowerTool;
	
	
	public TerrainEditorGuiManager() {
		this.textures = new ArrayList<>();
		this.buttons = new ArrayList<>();
	}
	
	
	
	public void loadTexturesButtons(TextureRenderer textureRender, MasterRenderer masterRenderer, long window) {
		Terrain terrain = EngineSettings.SelectedTerrain;
		// Scroll handling is dispatched from the single consolidated GLFW scroll
		// callback registered in Main.java - see handleScroll(double) below.
		terrainHeightMap = new GuiButton(terrain.getHeightMapTexture(), masterRenderer.getScreenWidth() - 50, 100, 50, 50, new Runnable() {
            @Override
            public void run() {
            	
            }
        });
		buttons.add(terrainHeightMap);
		textureRender.addTexture(terrainHeightMap);
		
		
		randomTool = new GuiButton("terrainRandom.png", masterRenderer.getScreenWidth() - 50, 150, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainTool = TerrainBrushTool.RANDOM;
            }
        });
		buttons.add(randomTool);
		textureRender.addTexture(randomTool);
		
		smoothTool = new GuiButton("terrainSmooth.png", masterRenderer.getScreenWidth() - 50, 200, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainTool = TerrainBrushTool.SMOOTH;
            }
        });
		buttons.add(smoothTool);
		textureRender.addTexture(smoothTool);
		
		raiseTool = new GuiButton("terrainRaise.png", masterRenderer.getScreenWidth() - 50, 250, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainTool = TerrainBrushTool.RAISE;
            }
        });
		buttons.add(raiseTool);
		textureRender.addTexture(raiseTool);
		
		flatTool = new GuiButton("terrainFlatten.png", masterRenderer.getScreenWidth() - 50, 300, 50, 50, new Runnable() {
            @Override
            public void run() {
            	 EngineSettings.TerrainTool = TerrainBrushTool.FLATTEN;;
            }
        });
		buttons.add(flatTool);
		textureRender.addTexture(flatTool);
		
		lowerTool = new GuiButton("terrainLower.png", masterRenderer.getScreenWidth() - 50, 350, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainTool = TerrainBrushTool.LOWER;
            }
        });
		buttons.add(lowerTool);
		textureRender.addTexture(lowerTool);
		
		
		System.out.println("loadTexturesButtons: " + true);
		loadedTextures = true;
	}
	
	
	public void showGui() {
	    for (GuiTexture t : textures) t.setVisible(true);
	    for (GuiButton b : buttons) b.setVisible(true);
	}
	
	public void hideGui() {
	    for (GuiTexture t : textures) t.setVisible(false);
	    for (GuiButton b : buttons) b.setVisible(false);
	}
	
	public void update(TextureRenderer textureRender, TextRenderer textRenderer, MasterRenderer masterRenderer, long window) {
		
		if (!loadedTextures && EngineSettings.TerrainEditor)
		     loadTexturesButtons(textureRender, masterRenderer, window);
		
		if (EngineSettings.TerrainEditor) {
			showGui();
			textRenderer.renderText("Brush: "+EngineSettings.TerrainBrushSize,  masterRenderer.getScreenWidth() - 115, 420, 0.25f,  masterRenderer.getScreenWidth(), TextAlignment.LEFT);
			textRenderer.renderText("Tool: "+EngineSettings.TerrainTool,  masterRenderer.getScreenWidth() - 155, 460, 0.25f,  masterRenderer.getScreenWidth(), TextAlignment.LEFT);

			
		} else {
			hideGui();
		}
		
		
		
	}
	
	public void handleScroll(double yOffset) {
	    if (!EngineSettings.TerrainEditor) return;

	    // Adjust brush size
	    if (yOffset > 0) {
	        EngineSettings.TerrainBrushSize += 5.0f;   // scroll up → bigger brush
	    } else if (yOffset < 0) {
	        EngineSettings.TerrainBrushSize -= 5.0f;   // scroll down → smaller brush
	    }

	}


}
