package toolbox;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import static org.lwjgl.opengl.GL46.*;

public class Equations {
	
	public static float calculateDistance(Vector3f pointA, Vector3f pointB) {
        float dx = pointB.x - pointA.x;
        float dy = pointB.y - pointA.y;
        float dz = pointB.z - pointA.z;
        
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
	
	public static org.joml.Matrix4f toJomlViaBuffer(org.lwjgl.util.vector.Matrix4f oldMat) {
	    // 1. Store LWJGL matrix into a FloatBuffer
	    FloatBuffer buf = BufferUtils.createFloatBuffer(16);
	    oldMat.store(buf); // LWJGL stores in row-major order
	    buf.flip();

	    // 2. Create JOML matrix and tell it how to interpret the buffer
	    org.joml.Matrix4f newMat = new org.joml.Matrix4f();
	    // By default, JOML’s set() expects column-major data,
	    // so we might need to transpose or read row-major explicitly:
	    newMat.set(buf).transpose(); // if you want an exact match of the original transform

	    return newMat;
	}
	
	public static org.joml.Matrix4f toJoml(org.lwjgl.util.vector.Matrix4f oldMat) {
	    // 1. Create a FloatBuffer to hold the 16 floats
	    FloatBuffer fb = BufferUtils.createFloatBuffer(16);
	    
	    // 2. LWJGL’s oldMat.store() writes it in row-major order
	    oldMat.store(fb);
	    fb.flip();
	    
	    // 3. Create a new JOML matrix
	    org.joml.Matrix4f newMat = new org.joml.Matrix4f();
	    
	    // 4. By default, newMat.set(FloatBuffer) expects column-major data,
	    //    which is the OpenGL standard. oldMat.store() gave us row-major,
	    //    so we set then transpose:
	    newMat.set(fb).transpose();
	    
	    return newMat;
	}
	
	
	public static int combineTexturesFixed(int textureId1, int textureId2, int width, int height) {
	    // 1. Create and bind an FBO.
	    int fbo = glGenFramebuffers();
	    glBindFramebuffer(GL_FRAMEBUFFER, fbo);

	    // 2. Create the texture that will hold the combined result.
	    int combinedTextureId = glGenTextures();
	    glBindTexture(GL_TEXTURE_2D, combinedTextureId);
	    glTexImage2D(GL_TEXTURE_2D, 0, GL_RGBA8, width, height, 0, GL_RGBA, GL_UNSIGNED_BYTE, (ByteBuffer) null);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_LINEAR);
	    glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_LINEAR);

	    // Attach the texture to the FBO.
	    glFramebufferTexture2D(GL_FRAMEBUFFER, GL_COLOR_ATTACHMENT0, GL_TEXTURE_2D, combinedTextureId, 0);

	    // Check if the framebuffer is complete.
	    if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
	        throw new RuntimeException("Framebuffer is not complete!");
	    }

	    // 3. Set the viewport to match the texture size and clear the FBO.
	    glViewport(0, 0, width, height);
	    glClear(GL_COLOR_BUFFER_BIT);

	    // 4. Set up multi-texturing using the fixed-function pipeline.

	    // --- Texture Unit 0: Base texture (use REPLACE mode) ---
	    glActiveTexture(GL_TEXTURE0);
	    glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, textureId1);
	    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_REPLACE);

	    // --- Texture Unit 1: Second texture blended with the previous result ---
	    glActiveTexture(GL_TEXTURE1);
	    glEnable(GL_TEXTURE_2D);
	    glBindTexture(GL_TEXTURE_2D, textureId2);
	    glTexEnvi(GL_TEXTURE_ENV, GL_TEXTURE_ENV_MODE, GL_COMBINE);
	    // Use INTERPOLATE mode to blend the previous texture and this texture.
	    glTexEnvi(GL_TEXTURE_ENV, GL_COMBINE_RGB, GL_INTERPOLATE);
	    // First operand comes from the previous stage (texture unit 0)
	    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE0_RGB, GL_PREVIOUS);
	    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND0_RGB, GL_SRC_COLOR);
	    // Second operand is the color from this texture.
	    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE1_RGB, GL_TEXTURE);
	    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND1_RGB, GL_SRC_COLOR);
	    // Third operand is a constant that sets the interpolation factor.
	    glTexEnvi(GL_TEXTURE_ENV, GL_SOURCE2_RGB, GL_CONSTANT);
	    glTexEnvi(GL_TEXTURE_ENV, GL_OPERAND2_RGB, GL_SRC_ALPHA);

	    // Set the constant color to (0.5, 0.5, 0.5, 0.5) which means 50% blend.
	    float[] constantColor = {0.5f, 0.5f, 0.5f, 0.5f};
	    glTexEnvfv(GL_TEXTURE_ENV, GL_TEXTURE_ENV_COLOR, constantColor);

	    // 5. Render a full-screen quad with multi-texture coordinates.
	    glBegin(GL_QUADS);
	        // Bottom-left corner
	        glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 0.0f);
	        glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 0.0f);
	        glVertex2f(-1.0f, -1.0f);
	        
	        // Bottom-right corner
	        glMultiTexCoord2f(GL_TEXTURE0, 1.0f, 0.0f);
	        glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 0.0f);
	        glVertex2f(1.0f, -1.0f);
	        
	        // Top-right corner
	        glMultiTexCoord2f(GL_TEXTURE0, 1.0f, 1.0f);
	        glMultiTexCoord2f(GL_TEXTURE1, 1.0f, 1.0f);
	        glVertex2f(1.0f, 1.0f);
	        
	        // Top-left corner
	        glMultiTexCoord2f(GL_TEXTURE0, 0.0f, 1.0f);
	        glMultiTexCoord2f(GL_TEXTURE1, 0.0f, 1.0f);
	        glVertex2f(-1.0f, 1.0f);
	    glEnd();

	    // 6. Reset state.
	    glActiveTexture(GL_TEXTURE1);
	    glDisable(GL_TEXTURE_2D);
	    glActiveTexture(GL_TEXTURE0);
	    glDisable(GL_TEXTURE_2D);

	    // Unbind the framebuffer (back to the default framebuffer).
	    glBindFramebuffer(GL_FRAMEBUFFER, 0);

	    // Optionally, delete the FBO if it’s no longer needed.
	    glDeleteFramebuffers(fbo);

	    // The combined texture now contains the blended result.
	    return combinedTextureId;
	}



	
	
	public static void checkGLError(String location) {
        int error;
        while ((error = glGetError()) != GL_NO_ERROR) {
            String errorStr;
            switch (error) {
                case GL_INVALID_ENUM: errorStr = "INVALID_ENUM"; break;
                case GL_INVALID_VALUE: errorStr = "INVALID_VALUE"; break;
                case GL_INVALID_OPERATION: errorStr = "INVALID_OPERATION"; break;
                case GL_OUT_OF_MEMORY: errorStr = "OUT_OF_MEMORY"; break;
                case GL_INVALID_FRAMEBUFFER_OPERATION: errorStr = "INVALID_FRAMEBUFFER_OPERATION"; break;
                default: errorStr = "UNKNOWN_ERROR"; break;
            }
            System.err.println("[OpenGL Error] " + errorStr + " at: " + location);
        }
    }

  

}
