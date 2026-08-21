package guiManager;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector4f;

import entities.Entity;
import entityManager.EntityManager;
import gui.GuiButton;
import gui.GuiTexture;
import gui.TextureRenderer;
import main.Main;
import renderer.MasterRenderer;
import settings.EngineSettings;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;

public class GuiManager {
	
	
	public List<GuiButton> buttons;
	public List<GuiTexture> textures;
	
	public MasterRenderer masterRenderer;
	
	public SelectedEntityDebug selectedEntityDebug;
	public SelectedLightDebug selectedLightDebug;
	
	public TerrainEditorGuiManager terrainEditorGuiManager;
	public TerrainPainterGuiManager terrainPainterGuiManager;
	public EntityGuiManager entityGuiManager;

	private EntityManager entityManager;

	public void init(TextureRenderer textureRenderer, MasterRenderer masterRenderer) {
		loadDebugButtons(textureRenderer);
		loadTextures(textureRenderer);
		this.masterRenderer = masterRenderer;

		selectedEntityDebug = new SelectedEntityDebug(textureRenderer);
		selectedLightDebug = new SelectedLightDebug();
		terrainEditorGuiManager = new TerrainEditorGuiManager();
		terrainPainterGuiManager = new TerrainPainterGuiManager();
		entityGuiManager = new EntityGuiManager();

	}

	/**
	 * Dispatches a mouse-scroll event to whichever tool cares about it right now.
	 * Called from the single consolidated GLFW scroll callback registered in Main.java
	 * (GLFW only supports one scroll callback per window, so every scroll-driven tool
	 * routes through here instead of each registering its own).
	 */
	public void handleScroll(double yOffset) {
		terrainEditorGuiManager.handleScroll(yOffset);
		terrainPainterGuiManager.handleScroll(yOffset);
		if (entityManager != null) {
			entityGuiManager.handleScroll(yOffset, entityManager);
		}
	}


	public void update(TextureRenderer textureRender, TextRenderer textRenderer, EntityManager entityManager,
			toolbox.MousePicker picker, long window, float deltaTime) {
		// Clear all effects for simple white text
		textRenderer.setTextColor(0.5f, 1.0f, 0.5f, 1.0f);      // Pure white
		textRenderer.setGlow(false, 0.0f, 0.0f, 0, 0, 0, 0);  // No glow
		textRenderer.setShadow(false, 0, 0, 0, 0, 0, 0, 0);   // No shadow
		textRenderer.setOutlineColor(0.40f,1, 0.4f, 1.0f);
		textRenderer.setOutlineWidth(0.00f);
		textRenderer.setShadow(false, 2, -2, 0,0,0,0.75f, 1.7f);
		textRenderer.setGlow(true,0.4f, 0.20f,
				0.5f, 1.0f, 0.5f, 
				0.8f);
		textRenderer.setTintGradient(1.35f,
			    new Vector4f(1,1,1,1),
			    new Vector4f(0.6f,0.8f,1,1)
			);
		
		textRenderer.setWave(0.0f, 0.0f);
		textRenderer.setTimeSeconds(deltaTime);
		textRenderer.setJitter(0.0f);
		
		textRenderer.renderText(""+Main.currentFPS,  0.95f, 0.95f, 0.5f,  masterRenderer.getScreenWidth(), TextAlignment.LEFT);

		selectedEntityDebug.update(textureRender, textRenderer, masterRenderer);
		selectedLightDebug.update(textureRender, textRenderer, masterRenderer);
		
		terrainEditorGuiManager.update(textureRender, textRenderer, masterRenderer, window);
		terrainPainterGuiManager.update(textureRender, textRenderer, masterRenderer, window);
		entityGuiManager.update(entityManager, picker, window);

		this.entityManager = entityManager;

		
	}
	
	
	
	
	
	
	public void loadDebugButtons(TextureRenderer textureRenderer) {
		   gui.GuiTexture texture2 = new gui.GuiTexture(1, 0, 100, 50,50);
		   texture2.setVisible(true);
	        textureRenderer.addTexture(texture2);
	        
	        gui.GuiTexture texture12 = new gui.GuiTexture(86, 60, 100, 50,50);
	        texture12.setVisible(true);
	        textureRenderer.addTexture(texture12);
	        
	       
	        
	        gui.GuiButton button1 = new gui.GuiButton("cube.png", 0, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                //EngineSettings.VisualiseObjects = !EngineSettings.VisualiseObjects;
	                EngineSettings.ObjectPicker = !EngineSettings.ObjectPicker;
	            }
	        });
	        button1.setVisible(true);
	        textureRenderer.addTexture(button1);
	        
	        gui.GuiButton button2 = new gui.GuiButton("idea.png", 50, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                EngineSettings.VisualiseLights = !EngineSettings.VisualiseLights;
	                EngineSettings.LightPicker = !EngineSettings.LightPicker;
	            }
	        });
	        button2.setVisible(true);
	        textureRenderer.addTexture(button2);
	        
	        gui.GuiButton button3 = new gui.GuiButton("colorWheel.png", 100, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                EngineSettings.MemoryUsage = !EngineSettings.MemoryUsage;
	            }
	        });
	        button3.setVisible(true);
	        textureRenderer.addTexture(button3);
	        
	        
	        
	        gui.GuiButton button4 = new gui.GuiButton("plus.png", 150, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                EngineSettings.EntityPlacementMode = !EngineSettings.EntityPlacementMode;
	                if (EngineSettings.EntityPlacementMode) {
	                    EngineSettings.TerrainEditor = false;
	                    EngineSettings.TerrainPainter = false;
	                    EngineSettings.ObjectPicker = true; // so the placed entity is immediately selectable/draggable
	                    EngineSettings.LightPicker = false; // don't let clicks pick a light instead while placing
	                }
	                System.out.println("Entity Placement Mode: " + EngineSettings.EntityPlacementMode);
	            }
	        });
	        button4.setVisible(true);
	        textureRenderer.addTexture(button4);
	        
	        gui.GuiButton button5 = new gui.GuiButton("copy.png", 200, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                
	                if (EngineSettings.SelectedEntity != null) {
	                	System.out.println("Copy Entity: " + EngineSettings.SelectedEntity.getId());
	                	Entity copiedEntity = EngineSettings.SelectedEntity.cloneEntity();
	                	
	                	if (EngineSettings.SelectedEntity.getCollisionBody() != null) {
		                	copiedEntity.setCollisionBody(Main.entityManager.physicsManager.cloneRigidBody(EngineSettings.SelectedEntity, EngineSettings.SelectedEntity.getMass()));
		                	EngineSettings.SelectedEntity = Main.entityManager.addEntity(copiedEntity, EntityManager.CollisionType.ADD_BODY, 0);

	                	} else {
		                	EngineSettings.SelectedEntity = Main.entityManager.addEntity(copiedEntity, EntityManager.CollisionType.NONE, 0);

	                	}
	                	
	                	 
	                	
	                	
	                }
	                
	                
	            }
	        });
	        button5.setVisible(true);
	        textureRenderer.addTexture(button5);
	        
	        
	        gui.GuiButton button6 = new gui.GuiButton("minus.png", 250, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
	                
	                if (EngineSettings.SelectedEntity != null) {
	                	System.out.println("Remove Entity: " + EngineSettings.SelectedEntity.getId());
	                	Main.entityManager.removeEntityByID(EngineSettings.SelectedEntity.getId());
	                }
	                
	                
	            }
	        });
	        button6.setVisible(true);
	        textureRenderer.addTexture(button6);
	        
	        
	        
	        gui.GuiButton button7 = new gui.GuiButton("terrain_outline.png", 300, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
                	EngineSettings.TerrainEditor = !EngineSettings.TerrainEditor;
                	if (EngineSettings.TerrainPainter)  EngineSettings.TerrainPainter = !EngineSettings.TerrainPainter;
                	if (EngineSettings.TerrainEditor) EngineSettings.EntityPlacementMode = false;
                	System.out.println("Terrain Height Editor: " + EngineSettings.TerrainEditor);
	            }
	        });
	        button7.setVisible(true);
	        textureRenderer.addTexture(button7);
	        
	        gui.GuiButton button8 = new gui.GuiButton("paint_outline.png", 350, 0, 50, 50, new Runnable() {
	            @Override
	            public void run() {
                	EngineSettings.TerrainPainter = !EngineSettings.TerrainPainter;
                	if (EngineSettings.TerrainEditor)  EngineSettings.TerrainEditor = !EngineSettings.TerrainEditor;
                	if (EngineSettings.TerrainPainter) EngineSettings.EntityPlacementMode = false;
                	System.out.println("Terrain Paint Editor: " + EngineSettings.TerrainPainter);
	            }
	        });
	        button8.setVisible(true);
	        textureRenderer.addTexture(button8);
	        
	        
	        
	        
	        
	}
	
	public void loadTextures(TextureRenderer textureRenderer) {
		
	}
	
	

}
