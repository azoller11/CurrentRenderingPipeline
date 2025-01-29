package gui;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL40;
import org.lwjgl.stb.STBImage;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;

public class Texture {
    private int textureId;
    private int width;
    private int height;
    private String filePath;

    // Position and size for rendering
    private float posX;
    private float posY;
    private float scaleX;
    private float scaleY;

    public Texture(String filePath) {
        this.filePath = filePath;
        loadTexture(filePath);
        // Initialize position and scale
        posX = 0.0f;
        posY = 0.0f;
        scaleX = 1.0f;
        scaleY = 1.0f;
    }
    
    public Texture(String filePath, float posX, float posY, float scaleX, float scaleY) {
        this.filePath = filePath;
        this.posX = posX;
        this.posY = posY;
        this.scaleX = scaleX;
        this.scaleY = scaleY;
        loadTexture(filePath);
    }

    private void loadTexture(String filePath) {
        // Generate texture ID
        textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);	
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Load image using STB
        IntBuffer widthBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer heightBuffer = BufferUtils.createIntBuffer(1);
        IntBuffer channelsBuffer = BufferUtils.createIntBuffer(1);

        ByteBuffer image = null;
        try {
            image = STBImage.stbi_load(filePath, widthBuffer, heightBuffer, channelsBuffer, 4);
            if (image == null) {
                throw new RuntimeException("Failed to load a texture file!"
                        + System.lineSeparator() + STBImage.stbi_failure_reason());
            }

            width = widthBuffer.get(0);
            height = heightBuffer.get(0);

            // Upload texture to GPU
            glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA, width, height, 0,
                    GL_RGBA, GL_UNSIGNED_BYTE, image);
            glGenerateMipmap(GL_TEXTURE_2D);
        } finally {
            if (image != null) {
                STBImage.stbi_image_free(image);
            }
        }

        // Unbind texture
        glBindTexture(GL_TEXTURE_2D, 0);
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

    // Getters and Setters for position and scale
    public float getPosX() {
        return posX;
    }

    public void setPosX(float posX) {
        this.posX = posX;
    }

    public float getPosY() {
        return posY;
    }

    public void setPosY(float posY) {
        this.posY = posY;
    }

    public float getScaleX() {
        return scaleX;
    }

    public void setScaleX(float scaleX) {
        this.scaleX = scaleX;
    }

    public float getScaleY() {
        return scaleY;
    }

    public void setScaleY(float scaleY) {
        this.scaleY = scaleY;
    }

    public int getTextureId() {
        return textureId;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }
}