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
	
	private EntityManager entityManager;
	
	public void init(TextureRenderer textureRenderer, MasterRenderer masterRenderer) {
		loadDebugButtons(textureRenderer);
		loadTextures(textureRenderer);
		this.masterRenderer = masterRenderer;
		
		selectedEntityDebug = new SelectedEntityDebug(textureRenderer);
		selectedLightDebug = new SelectedLightDebug();
		terrainEditorGuiManager = new TerrainEditorGuiManager();
		terrainPainterGuiManager = new TerrainPainterGuiManager();
		
	}
	
	
	public void update(TextureRenderer textureRender, TextRenderer textRenderer, EntityManager entityManager, long window, float deltaTime) {
		// Clear all effects for simple white text
		textRenderer.setTextColor(1.0f, 0.5f, 0.5f, 1.0f);      // Pure white
		//textRenderer.setOutlineWidth(0.0f);                    // Disable outline
		//textRenderer.setOutlineColor(0, 0, 0, 0);              // Transparent outline
		//textRenderer.setBorderWidth(0.0f);                     // Disable border
		//textRenderer.setBorderColor(0, 0, 0, 0);               // Transparent border
		textRenderer.setGlow(false, 0.0f, 0.0f, 0, 0, 0, 0);  // No glow
		textRenderer.setShadow(false, 0, 0, 0, 0, 0, 0, 0);   // No shadow
		//textRenderer.setTintGradient(0.0f, new Vector4f(1,1,1,0), new Vector4f(1,1,1,0)); // No tint
		//textRenderer.setWave(0.0f, 0.0f);                     // No wave
		//textRenderer.setJitter(0.0f);                         // No jitter
		
		//textRenderer.setGlow(true, 0.28f, 2.2f, 1f, 1f, 1f, 1f);
		//textRenderer.setShadow(false, 2f,5f, 0f, 0f, 0f, 0.7f, 0.8f);
		//textRenderer.setTimeSeconds(deltaTime);
		//textRenderer.setJitter(0);
		//textRenderer.setWave(0, 0);
		textRenderer.setOutlineColor(0.40f,1, 0.4f, 1.0f);
		textRenderer.setOutlineWidth(0.00f);
		textRenderer.setShadow(false, 2, -2, 0,0,0,0.75f, 1.7f);
		textRenderer.setGlow(true,0.6f, 0.40f,1, 0.4f, 0, 0.8f);
		textRenderer.setTintGradient(1.35f,
			    new Vector4f(1,1,1,1),
			    new Vector4f(0.6f,0.8f,1,1)
			);
		
		textRenderer.setWave(0.0f, 0.0f);
		textRenderer.setTimeSeconds(deltaTime);
		textRenderer.setJitter(0.0f);
		
		textRenderer.renderText(""+Main.currentFPS,  0.75f, 0.75f, 1.025f,  masterRenderer.getScreenWidth(), TextAlignment.LEFT);
		
		selectedEntityDebug.update(textureRender, textRenderer, masterRenderer);
		selectedLightDebug.update(textureRender, textRenderer, masterRenderer);
		
		terrainEditorGuiManager.update(textureRender, textRenderer, masterRenderer, window);
		terrainPainterGuiManager.update(textureRender, textRenderer, masterRenderer, window);
		
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
	                System.out.println("Add new Entity");
	                Entity ne = new Entity();
	                
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
                	System.out.println("Terrain Paint Editor: " + EngineSettings.TerrainPainter);
	            }
	        });
	        button8.setVisible(true);
	        textureRenderer.addTexture(button8);
	        
	        
	        
	        
	        
	}
	
	public void loadTextures(TextureRenderer textureRenderer) {
		
	}
	
	

}
