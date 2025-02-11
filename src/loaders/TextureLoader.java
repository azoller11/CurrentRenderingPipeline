
package loaders;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import gui.GuiTexture;
import settings.EngineSettings;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;
import java.util.HashMap;
import java.util.Map;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryUtil.NULL;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;

public class TextureLoader {

    private static final String TEXTURE_DIR = "res/";

    

   
    public static int loadTexture(String filename) {
        
        if (EngineSettings.textureCache.containsKey(filename)) {
            //System.out.println("Texture \"" + filename + "\" retrieved from cache.");
            return EngineSettings.textureCache.get(filename);
        }
        
        // 1) Load image file into a ByteBuffer with STB
        String filePath = TEXTURE_DIR + filename;
        int width, height;
        ByteBuffer imageData;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // STBI_rgb_alpha forces 4 components (RGBA)
            STBImage.stbi_set_flip_vertically_on_load(false);
            imageData = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load texture file: " + filePath
                    + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();
        }

        // 2) Create an OpenGL texture object
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // 3) Upload the image to the GPU
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, imageData);

        // Generate mipmaps
        glGenerateMipmap(GL_TEXTURE_2D);

        // Set default filtering parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        // Set wrapping parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // **Anisotropic Filtering Implementation**
        // Initialize LWJGL's OpenGL capabilities
        GL.createCapabilities();

        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            float desiredAnisotropy = Math.min(16.0f, maxAnisotropy); // Commonly 16
            glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, desiredAnisotropy);
            //System.out.println("Anisotropic filtering set to " + desiredAnisotropy);
        } else {
            //System.out.println("Anisotropic filtering not supported.");
        }

        // 4) Free the raw image data from CPU
        STBImage.stbi_image_free(imageData);
        
        EngineSettings.textureCache.put(filename, textureId);

        return textureId;
    }

    public static int loadExplicitTexture(String filename) {
        if (EngineSettings.textureCache.containsKey(filename)) {
            return EngineSettings.textureCache.get(filename);
        }

        String filePath = TEXTURE_DIR + filename;
        int width, height;
        ByteBuffer imageData;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            STBImage.stbi_set_flip_vertically_on_load(false);
            imageData = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load texture file: " + filePath
                    + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();
        }

        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
            GL_RGBA, GL_UNSIGNED_BYTE, imageData);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        STBImage.stbi_image_free(imageData);
        EngineSettings.textureCache.put(filename, textureId);

        return textureId;
    }

    public static int loadHDRTexture(String filename) {
        if (EngineSettings.textureCache.containsKey(filename)) {
            return EngineSettings.textureCache.get(filename);
        }
        
        String filePath = TEXTURE_DIR + filename;
        int width, height;
        FloatBuffer imageData;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);
            
            // Set vertical flip if needed (depending on your coordinate system)
            STBImage.stbi_set_flip_vertically_on_load(false);
            
            // Load HDR image as floating-point data
            imageData = STBImage.stbi_loadf(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load HDR texture file: " + filePath
                    + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get();
            height = h.get();
        }
        
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        
        // Use a floating-point internal format; GL_RGBA16F is common for HDR images.
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0,
            GL_RGBA, GL_FLOAT, imageData);
        
        // Generate mipmaps if desired (note: mipmapping HDR textures might require extra care)
        glGenerateMipmap(GL_TEXTURE_2D);
        
        // Set texture filtering and wrapping options as needed.
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);
        
        // Anisotropic filtering remains the same.
        GL.createCapabilities();
        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            float desiredAnisotropy = Math.min(16.0f, maxAnisotropy);
            glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, desiredAnisotropy);
        }
        
        // Free the HDR image data
        STBImage.stbi_image_free(imageData);
        
        EngineSettings.textureCache.put(filename, textureId);
        return textureId;
    }
}
