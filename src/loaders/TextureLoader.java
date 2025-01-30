package loaders;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import gui.Texture;

import java.io.File;
import java.nio.ByteBuffer;
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

    // Cache to store loaded textures
    private static final Map<String, Integer> textureCache = new HashMap<>();

    /**
     * Loads a texture from e.g. "res/textures/diffuse.png"
     * and returns the OpenGL texture ID.
     *
     * @param filename  "diffuse.png" (omit path if you like)
     * @return          The GL texture ID.
     */
    public static int loadTexture(String filename) {
        
        if (textureCache.containsKey(filename)) {
            //System.out.println("Texture \"" + filename + "\" retrieved from cache.");
            return textureCache.get(filename);
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
        
        textureCache.put(filename, textureId);

        return textureId;
    }
}
