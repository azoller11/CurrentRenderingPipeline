package loaders;

import org.lwjgl.stb.STBImage;
import org.lwjgl.system.MemoryStack;

import java.io.File;
import java.nio.ByteBuffer;
import java.nio.IntBuffer;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryUtil.NULL;

/**
 * Simple utility to load a 2D texture from an image file using STB.
 */
public class TextureLoader {

    private static final String TEXTURE_DIR = "res/";

    /**
     * Loads a texture from e.g. "res/textures/diffuse.png"
     * and returns the OpenGL texture ID.
     *
     * @param filename  "diffuse.png" (omit path if you like)
     * @return          The GL texture ID.
     */
    public static int loadTexture(String filename) {
        // 1) Load image file into a ByteBuffer with STB
        String filePath = TEXTURE_DIR + filename;
        int width, height;
        ByteBuffer imageData;
        try (MemoryStack stack = MemoryStack.stackPush()) {
            IntBuffer w = stack.mallocInt(1);
            IntBuffer h = stack.mallocInt(1);
            IntBuffer channels = stack.mallocInt(1);

            // STBI_rgb_alpha forces 4 components (RGBA)
            STBImage.stbi_set_flip_vertically_on_load(true);
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

        // Generate mipmaps if you want
        glGenerateMipmap(GL_TEXTURE_2D);

        // Set some default filtering/wrapping
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR_MIPMAP_LINEAR);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_REPEAT);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_REPEAT);

        // 4) Free the raw image data from CPU
        STBImage.stbi_image_free(imageData);

        return textureId;
    }
}
