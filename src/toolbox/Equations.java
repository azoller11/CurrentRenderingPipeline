package toolbox;

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
