package loaders;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;
import settings.EngineSettings;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import org.lwjgl.opengl.EXTTextureFilterAnisotropic;
import org.lwjgl.opengl.GL;

public class TextureLoader {

    private static final String TEXTURE_DIR = "res/";
    private static final boolean DEBUG = true;

    // Set vertical flip once for all texture loads.
    static {
        STBImage.stbi_set_flip_vertically_on_load(false);
    }

    /**
     * Loads a standard texture with mipmaps and anisotropic filtering.
     */
    public static int loadTexture(String filename) {
        long totalStartTime = System.nanoTime();

        if (EngineSettings.textureCache.containsKey(filename)) {
            return EngineSettings.textureCache.get(filename);
        }

        String filePath = TEXTURE_DIR + filename;
        int width, height;
        ByteBuffer imageData;

        // 1) Load image data using STB
        long imageLoadStart = System.nanoTime();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // Force 4 components (RGBA)
            imageData = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load texture file: " + filePath
                        + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get(0);
            height = h.get(0);
        }
        long imageLoadEnd = System.nanoTime();

        // 2) Create an OpenGL texture and upload the image data
        long gpuUploadStart = System.nanoTime();
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, imageData);
        glGenerateMipmap(GL_TEXTURE_2D);

        // Set filtering and wrapping
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // Anisotropic filtering (if supported)
        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            float desiredAnisotropy = Math.min(16.0f, maxAnisotropy);
            glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, desiredAnisotropy);
        }
        long gpuUploadEnd = System.nanoTime();

        // 3) Free the loaded image data and cache the texture
        STBImage.stbi_image_free(imageData);
        EngineSettings.textureCache.put(filename, textureId);

        long totalEndTime = System.nanoTime();
        if (DEBUG) {
            String debugInfo = String.format("Texture Load [%s]: imageLoad=%.2f ms, gpuUpload=%.2f ms, total=%.2f ms",
                    filename,
                    (imageLoadEnd - imageLoadStart) / 1_000_000.0,
                    (gpuUploadEnd - gpuUploadStart) / 1_000_000.0,
                    (totalEndTime - totalStartTime) / 1_000_000.0);
            System.out.println(debugInfo);
        }

        return textureId;
    }

    /**
     * Loads a texture with explicit parameters (using linear filtering and clamp-to-edge wrapping).
     */
    public static int loadExplicitTexture(String filename) {
        long totalStartTime = System.nanoTime();

        if (EngineSettings.textureCache.containsKey(filename)) {
            return EngineSettings.textureCache.get(filename);
        }

        String filePath = TEXTURE_DIR + filename;
        int width, height;
        ByteBuffer imageData;

        // 1) Load image data using STB
        long imageLoadStart = System.nanoTime();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            imageData = STBImage.stbi_load(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load texture file: " + filePath
                        + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get(0);
            height = h.get(0);
        }
        long imageLoadEnd = System.nanoTime();

        // 2) Create texture and upload image data with explicit parameters
        long gpuUploadStart = System.nanoTime();
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0,
                     GL_RGBA, GL_UNSIGNED_BYTE, imageData);

        // Set explicit filtering and wrapping options
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_EDGE);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_EDGE);
        long gpuUploadEnd = System.nanoTime();

        STBImage.stbi_image_free(imageData);
        EngineSettings.textureCache.put(filename, textureId);

        long totalEndTime = System.nanoTime();
        if (DEBUG) {
            String debugInfo = String.format("Explicit Texture Load [%s]: imageLoad=%.2f ms, gpuUpload=%.2f ms, total=%.2f ms",
                    filename,
                    (imageLoadEnd - imageLoadStart) / 1_000_000.0,
                    (gpuUploadEnd - gpuUploadStart) / 1_000_000.0,
                    (totalEndTime - totalStartTime) / 1_000_000.0);
            System.out.println(debugInfo);
        }

        return textureId;
    }

    /**
     * Loads an HDR texture using floating-point data.
     */
    public static int loadHDRTexture(String filename) {
        long totalStartTime = System.nanoTime();

        if (EngineSettings.textureCache.containsKey(filename)) {
            return EngineSettings.textureCache.get(filename);
        }

        String filePath = TEXTURE_DIR + filename;
        int width, height;
        FloatBuffer imageData;

        // 1) Load HDR image data
        long imageLoadStart = System.nanoTime();
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            imageData = STBImage.stbi_loadf(filePath, w, h, channels, 4);
            if (imageData == null) {
                throw new RuntimeException("Failed to load HDR texture file: " + filePath
                        + "\n" + STBImage.stbi_failure_reason());
            }
            width = w.get(0);
            height = h.get(0);
        }
        long imageLoadEnd = System.nanoTime();

        // 2) Create texture and upload HDR image data
        long gpuUploadStart = System.nanoTime();
        int textureId = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, textureId);

        // Use a floating-point internal format (GL_RGBA16F is common for HDR)
        glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA16F, width, height, 0,
                     GL_RGBA, GL_FLOAT, imageData);
        glGenerateMipmap(GL_TEXTURE_2D);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        if (GL.getCapabilities().GL_EXT_texture_filter_anisotropic) {
            float maxAnisotropy = glGetFloat(EXTTextureFilterAnisotropic.GL_MAX_TEXTURE_MAX_ANISOTROPY_EXT);
            float desiredAnisotropy = Math.min(16.0f, maxAnisotropy);
            glTexParameterf(GL_TEXTURE_2D, EXTTextureFilterAnisotropic.GL_TEXTURE_MAX_ANISOTROPY_EXT, desiredAnisotropy);
        }
        long gpuUploadEnd = System.nanoTime();

        STBImage.stbi_image_free(imageData);
        EngineSettings.textureCache.put(filename, textureId);

        long totalEndTime = System.nanoTime();
        if (DEBUG) {
            String debugInfo = String.format("HDR Texture Load [%s]: imageLoad=%.2f ms, gpuUpload=%.2f ms, total=%.2f ms",
                    filename,
                    (imageLoadEnd - imageLoadStart) / 1_000_000.0,
                    (gpuUploadEnd - gpuUploadStart) / 1_000_000.0,
                    (totalEndTime - totalStartTime) / 1_000_000.0);
            System.out.println(debugInfo);
        }

        return textureId;
    }
}
