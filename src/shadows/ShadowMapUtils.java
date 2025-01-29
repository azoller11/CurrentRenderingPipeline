package shadows;

import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL14.*;

public class ShadowMapUtils {
    /**
     * Creates a framebuffer for shadow mapping.
     */
	public static int createDepthFramebuffer(int width, int height) {
	    int fbo = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, fbo);

	    // No attachments are made here; attachments are handled externally in the Light class

	    glBindFramebuffer(GL_FRAMEBUFFER, 0); // Unbind the framebuffer
	    return fbo;
	}

	public static int createDepthTexture(int width, int height) {
	    int texture = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, texture);
	    glTexImage2D(GL_TEXTURE_2D, 0, GL_DEPTH_COMPONENT24, width, height, 0,
	             GL_DEPTH_COMPONENT, GL_UNSIGNED_INT, (java.nio.ByteBuffer)null);


	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
	    // Prevent shadow map bleeding
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);
	    float[] borderColor = {1.0f, 1.0f, 1.0f, 1.0f};
	    glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

	    glBindTexture(GL_TEXTURE_2D, 0); // Unbind the texture
	    return texture;
	}

}
