package gui;

import loaders.TextureLoader;
import settings.EngineSettings;

import org.joml.Vector4f;
import static org.lwjgl.opengl.GL40.*;

public class GuiTexture {
	private int textureId = -1; // Default no texture
	private Vector4f color; // Color if no texture
    private int width;
    private int height;
    private String filePath;

    private float posX;
    private float posY;
    private float scaleX;
    private float scaleY;
    
    private boolean isHovered;
    
    public GuiTexture(String filePath) {
        this.filePath = filePath;
        this.textureId = TextureLoader.loadExplicitTexture(filePath);
        posX = 0.0f;
        posY = 0.0f;
        scaleX = 1.0f;
        scaleY = 1.0f;
    }
    
    public GuiTexture(String filePath, float posX, float posY, float scaleX, float scaleY) {
        this.filePath = filePath;
        this.posX = posX;
        this.posY = posY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.textureId = TextureLoader.loadExplicitTexture(filePath);
    }
    
    public GuiTexture(int TextureId, float posX, float posY, float scaleX, float scaleY) {
        this.posX = posX;
        this.posY = posY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        this.textureId = TextureId;
    }
    
    public GuiTexture(Vector4f color, float posX, float posY, float scaleX, float scaleY) {
        this.color = color;
        this.posX = posX;
        this.posY = posY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
    }
    
    public void checkOver(double mouseX, double mouseY) {
        float minX = getPosX(); 
        float maxX = getPosX() + getScaleX();
        float minY = getPosY();
        float maxY = getPosY() + getScaleY();
        isHovered = mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;
        if (isHovered)
        	EngineSettings.overTexture = true;
       
       
    }

    public void bind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, textureId);
    }

    public void unbind(int textureUnit) {
        glActiveTexture(GL_TEXTURE0 + textureUnit);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    public void destroy() {
        glDeleteTextures(textureId);
    }
    
    public Vector4f getColor() {
        return color;
    }

    public boolean hasTexture() {
        return textureId != -1;
    }

    public float getPosX() { return posX; }
    public void setPosX(float posX) { this.posX = posX; }
    public float getPosY() { return posY; }
    public void setPosY(float posY) { this.posY = posY; }
    public float getScaleX() { return scaleX; }
    public void setScaleX(float scaleX) { this.scaleX = scaleX; }
    public float getScaleY() { return scaleY; }
    public void setScaleY(float scaleY) { this.scaleY = scaleY; }
    public int getTextureId() { return textureId; }
    public int getWidth() { return width; }
    public int getHeight() { return height; }
}
