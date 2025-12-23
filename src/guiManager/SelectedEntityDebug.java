package guiManager;

import gui.TextureRenderer;
import gui.GuiTexture;
import gui.GuiButton;
import loaders.TextureLoader;
import renderer.MasterRenderer;
import settings.EngineSettings;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;
import java.util.ArrayList;
import java.util.List;

public class SelectedEntityDebug {
	
	private boolean loadedTextures;
	
	private List<GuiTexture> textures;
	private List<GuiButton> buttons;
	
	private GuiTexture entityTexture;
	
	private GuiTexture metallicMap;
	private GuiTexture roughnessMap;
	private GuiTexture aoMap;
	private GuiTexture heighMapId;
	private GuiTexture normalMapId;
	
	
	
	public SelectedEntityDebug(TextureRenderer textureRender) {
		this.loadedTextures = false;
		this.textures = new ArrayList<>();
		this.buttons = new ArrayList<>();
		
		
		
	}
	
	
	public void loadTexturesButtons(TextureRenderer textureRender, MasterRenderer masterRenderer) {
			entityTexture = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getTextureId(), 0, masterRenderer.getScreenHeight() - 150, 50, 50);
			entityTexture.setVisible(false);
			textures.add(entityTexture);
			textureRender.addTexture(entityTexture);
			
			metallicMap = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getMetallicMap(), 50, masterRenderer.getScreenHeight() - 150, 50, 50);
			metallicMap.setVisible(false);
			textures.add(metallicMap);
			textureRender.addTexture(metallicMap);
			
			
			roughnessMap = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getRoughnessMap(), 100, masterRenderer.getScreenHeight() - 150, 50, 50);
			roughnessMap.setVisible(false);
			textures.add(roughnessMap);
			textureRender.addTexture(roughnessMap);
			
			
			aoMap = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getAoMap(), 150, masterRenderer.getScreenHeight() - 150, 50, 50);
			aoMap.setVisible(false);
			textures.add(aoMap);
			textureRender.addTexture(aoMap);
			
			heighMapId = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getHeighMapId(), 200, masterRenderer.getScreenHeight() - 150, 50, 50);
			heighMapId.setVisible(false);
			textures.add(heighMapId);
			textureRender.addTexture(heighMapId);
			
			
			normalMapId = new GuiTexture(EngineSettings.OpenEntity.getTexturedModel().getNormalMapId(), 250, masterRenderer.getScreenHeight() - 150, 50, 50);
			normalMapId.setVisible(false);
			textures.add(normalMapId);
			textureRender.addTexture(normalMapId);
			
			
			
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
	public void update(TextureRenderer textureRender, TextRenderer textRenderer, MasterRenderer masterRenderer) {
	
			if (EngineSettings.pause && EngineSettings.SelectedEntity != null && EngineSettings.OpenEntity != null && !EngineSettings.grabMouse) {
			
				 if (!loadedTextures)
				     loadTexturesButtons(textureRender, masterRenderer);
				
				 // Update UI text
				 int x = masterRenderer.getScreenHeight();
				 
				 textRenderer.renderText(""+EngineSettings.OpenEntity.getId(), 
				         0, x - 20, 0.25f, masterRenderer.getFlatProjection(),
				         masterRenderer.getScreenWidth(), TextAlignment.LEFT);
				
				 textRenderer.renderText(
				         "Pos: [" +
				         EngineSettings.OpenEntity.getPosition().x() + ", " +
				         EngineSettings.OpenEntity.getPosition().y() + ", " +
				         EngineSettings.OpenEntity.getPosition().z() + "]",
					         0, x - 40, 0.25f, masterRenderer.getFlatProjection(),
					         masterRenderer.getScreenWidth(), TextAlignment.LEFT);
				 
				 textRenderer.renderText("Size: "+EngineSettings.OpenEntity.getScale() + " RotY: " + EngineSettings.OpenEntity.getRotation().y() + " Collision: " + EngineSettings.OpenEntity.getCollisionType(), 
				         0, x - 60, 0.25f, masterRenderer.getFlatProjection(),
				         masterRenderer.getScreenWidth(), TextAlignment.LEFT);
					
					 entityTexture.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getTextureId());
					 
					 
					 if (EngineSettings.OpenEntity.getTexturedModel().getMetallicMap() > 0) {
						 metallicMap.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getMetallicMap());
					 } else {
						 metallicMap.setTextureId(0);
					 }
					 
					 if (EngineSettings.OpenEntity.getTexturedModel().getRoughnessMap() > 0) {
						 roughnessMap.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getRoughnessMap());
					 } else {
						 roughnessMap.setTextureId(0);
					 }
					 
					 if (EngineSettings.OpenEntity.getTexturedModel().getAoMap() > 0) {
						 aoMap.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getAoMap());
					 } else {
						 aoMap.setTextureId(0);
					 }
					 
					 if (EngineSettings.OpenEntity.getTexturedModel().getHeighMapId() > 0) {
						 heighMapId.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getHeighMapId());
					 } else {
						 heighMapId.setTextureId(0);
					 }
					 
					 if (EngineSettings.OpenEntity.getTexturedModel().getNormalMapId() > 0) {
						 normalMapId.setTextureId(EngineSettings.OpenEntity.getTexturedModel().getNormalMapId());
					 } else {
						 normalMapId.setTextureId(0);
					 }
					 
					 
					 
					 
					 
					 
					
					 showGui();
					}
				else {
			 hideGui();
			}
	}

	

}
