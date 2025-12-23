package guiManager;

import java.util.ArrayList;
import java.util.List;

import org.lwjgl.glfw.GLFW;

import gui.GuiButton;
import gui.GuiTexture;
import gui.TextureRenderer;
import renderer.MasterRenderer;
import settings.EngineSettings;
import settings.EngineSettings.TerrainBrushColor;
import terrain.Terrain;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;

public class TerrainPainterGuiManager {

private boolean loadedTextures;
	
	private List<GuiTexture> textures;
	private List<GuiButton> buttons;
	

	
	private GuiButton terrainBlendMap;
	
	private GuiButton terrainTextureA;
	private GuiButton terrainTextureB;
	private GuiButton terrainTextureC;
	private GuiButton terrainTextureD;
	
	public TerrainPainterGuiManager() {
		this.textures = new ArrayList<>();
		this.buttons = new ArrayList<>();
	}
	
	
	
	public void loadTexturesButtons(TextureRenderer textureRender, MasterRenderer masterRenderer, long window) {
		Terrain terrain = EngineSettings.SelectedTerrain;
		 GLFW.glfwSetScrollCallback(window, (win, xOffset, yOffset) -> {
	            handleScroll(yOffset);
	        });
		
		terrainBlendMap = new GuiButton(terrain.getBlendMapTexture(), masterRenderer.getScreenWidth() - 50, 100, 50, 50, new Runnable() {
            @Override
            public void run() {
            	
            }
        });
		buttons.add(terrainBlendMap);
		textureRender.addTexture(terrainBlendMap);
		
		terrainTextureA = new GuiButton(terrain.getLayers()[3].getAlbedoMap(), masterRenderer.getScreenWidth() - 50, 150, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainColor = TerrainBrushColor.BLACK;
            }
        });
		buttons.add(terrainTextureA);
		textureRender.addTexture(terrainTextureA);

		terrainTextureB = new GuiButton(terrain.getLayers()[0].getAlbedoMap(), masterRenderer.getScreenWidth() - 50, 200, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainColor = TerrainBrushColor.RED;
            }
        });
		buttons.add(terrainTextureB);
		textureRender.addTexture(terrainTextureB);

		terrainTextureC = new GuiButton(terrain.getLayers()[1].getAlbedoMap(), masterRenderer.getScreenWidth() - 50, 250, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainColor = TerrainBrushColor.GREEN;
            }
        });
		buttons.add(terrainTextureC);
		textureRender.addTexture(terrainTextureC);

		terrainTextureD = new GuiButton(terrain.getLayers()[2].getAlbedoMap(), masterRenderer.getScreenWidth() - 50, 300, 50, 50, new Runnable() {
            @Override
            public void run() {
            	EngineSettings.TerrainColor = TerrainBrushColor.BLUE;
            }
        });
		buttons.add(terrainTextureD);
		textureRender.addTexture(terrainTextureD);

		
		
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
		
		if (!loadedTextures && EngineSettings.TerrainPainter)
		     loadTexturesButtons(textureRender, masterRenderer, window);
		
	
		if (EngineSettings.TerrainPainter) {
			showGui();
			textRenderer.renderText("Brush: "+EngineSettings.TerrainBrushSize,  masterRenderer.getScreenWidth() - 115, 370, 0.25f, masterRenderer.getFlatProjection(), masterRenderer.getScreenWidth(), TextAlignment.LEFT);

			
		} else {
			hideGui();
		}
		
		
	}
	
	public void handleScroll(double yOffset) {
	    if (!EngineSettings.TerrainPainter) return;

	    // Adjust brush size
	    if (yOffset > 0) {
	        EngineSettings.TerrainBrushSize += 5.0f;   // scroll up → bigger brush
	    } else if (yOffset < 0) {
	        EngineSettings.TerrainBrushSize -= 5.0f;   // scroll down → smaller brush
	    }

	}


}
