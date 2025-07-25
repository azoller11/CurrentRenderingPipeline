package shaders;

import org.joml.Matrix3f;
import org.joml.Matrix3fc;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.joml.Vector3f; // Changed to JOML's Vector3f for consistency
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL40;
import org.lwjgl.system.MemoryStack;

import entities.Light;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.nio.FloatBuffer;

import static org.lwjgl.opengl.GL40.*;

public class ShaderProgram {
    private final int programId;

    //For a straight path
    public ShaderProgram(String vertPath,
                         String tessControlPath,
                         String tessEvalPath,
                         String geomPath,
                         String fragPath) {
        // Create a program handle
        programId = glCreateProgram();
        if (programId == 0) {
            throw new IllegalStateException("Could not create ShaderProgram!");
        }

        // Compile each shader if the path is not null
        int vs = compileShader(vertPath, GL_VERTEX_SHADER);
        int tcs = compileShader(tessControlPath, GL_TESS_CONTROL_SHADER);
        int tes = compileShader(tessEvalPath, GL_TESS_EVALUATION_SHADER);
        int gs = compileShader(geomPath, GL_GEOMETRY_SHADER);
        int fs = compileShader(fragPath, GL_FRAGMENT_SHADER);

        // Attach shaders
        if (vs != 0) glAttachShader(programId, vs);
        if (tcs != 0) glAttachShader(programId, tcs);
        if (tes != 0) glAttachShader(programId, tes);
        if (gs != 0) glAttachShader(programId, gs);
        if (fs != 0) glAttachShader(programId, fs);

        // Link the program
        glLinkProgram(programId);

        // Check for linking errors
        int linked = glGetProgrami(programId, GL_LINK_STATUS);
        if (linked == 0) {
            String log = glGetProgramInfoLog(programId);
            throw new RuntimeException("Program link failed:\n" + log);
        }

        // Detach and delete shaders after successful linking
        if (vs != 0) {
            glDetachShader(programId, vs);
            glDeleteShader(vs);
        }
        if (tcs != 0) {
            glDetachShader(programId, tcs);
            glDeleteShader(tcs);
        }
        if (tes != 0) {
            glDetachShader(programId, tes);
            glDeleteShader(tes);
        }
        if (gs != 0) {
            glDetachShader(programId, gs);
            glDeleteShader(gs);
        }
        if (fs != 0) {
            glDetachShader(programId, fs);
            glDeleteShader(fs);
        }
    }

    private static int compileShader(String filePath, int type) {
        if (filePath == null) return 0; // If no shader provided, skip

        String source = null;
        try {
            source = new String(Files.readAllBytes(Paths.get(filePath)));
        } catch (IOException e) {
            e.printStackTrace();
            return 0;
        }
        int shaderId = glCreateShader(type);
        glShaderSource(shaderId, source);
        glCompileShader(shaderId);

        // Check compile status
        int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
        if (status == GL_FALSE) {
            String log = glGetShaderInfoLog(shaderId);
            throw new RuntimeException("Shader compile error (" + filePath + "):\n" + log);
        }

        return shaderId;
    }
    //
    
    
    //For a modular approach
        private static String loadShaderSource(String[] filePaths) {
        StringBuilder source = new StringBuilder();
        for (String filePath : filePaths) {
            try {
                List<String> lines = Files.readAllLines(Paths.get(filePath));
                for (String line : lines) {
                    source.append(line).append("\n");
                }
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("Failed to load shader file: " + filePath);
            }
        }
        return source.toString();
    }
    
    private static String[] concatenatePaths(String mainPath, String[] additionalPaths) {
        String[] allPaths = new String[additionalPaths.length + 1];
        allPaths[0] = mainPath;
        System.arraycopy(additionalPaths, 0, allPaths, 1, additionalPaths.length);
        return allPaths;
    }

    
    private static int compileShader(String type, String[] filePaths) {
    // Load and concatenate the shader files
    String source = loadShaderSource(filePaths);

    // Select the correct shader type based on the argument
    int shaderType;
    switch (type) {
        case "vertex":
            shaderType = GL_VERTEX_SHADER;
            break;
        case "fragment":
            shaderType = GL_FRAGMENT_SHADER;
            break;
        case "geometry":
            shaderType = GL_GEOMETRY_SHADER;
            break;
        case "tessellation_control":
            shaderType = GL_TESS_CONTROL_SHADER;
            break;
        case "tessellation_eval":
            shaderType = GL_TESS_EVALUATION_SHADER;
            break;
        default:
            throw new IllegalArgumentException("Unknown shader type: " + type);
    }

    int shaderId = glCreateShader(shaderType);
    glShaderSource(shaderId, source);
    glCompileShader(shaderId);

    // Check for compile status
    int status = glGetShaderi(shaderId, GL_COMPILE_STATUS);
    if (status == GL_FALSE) {
        String log = glGetShaderInfoLog(shaderId);
        throw new RuntimeException("Shader compile error:\n" + log);
    }

    return shaderId;
}

    
    public ShaderProgram(String vertPath,
		            String tessControlPath,
		            String tessEvalPath,
		            String geomPath,
		            String fragPath,
		            String[] additionalVertexShaders,
		            String[] additionalFragmentShaders,
		            String[] additionalGeometryShaders) {
		// Create a program handle
		programId = glCreateProgram();
		if (programId == 0) {
		throw new IllegalStateException("Could not create ShaderProgram!");
		}
		
		// Compile the main shaders if the path is not null
		int vs = compileShader("vertex", concatenatePaths(vertPath, additionalVertexShaders));
		int tcs = compileShader("tessellation_control", new String[]{tessControlPath});
		int tes = compileShader("tessellation_eval", new String[]{tessEvalPath});
		int gs = compileShader("geometry", concatenatePaths(geomPath, additionalGeometryShaders));
		int fs = compileShader("fragment", concatenatePaths(fragPath, additionalFragmentShaders));
		
		// Attach the shaders
		glAttachShader(programId, vs);
		glAttachShader(programId, tcs);
		glAttachShader(programId, tes);
		glAttachShader(programId, gs);
		glAttachShader(programId, fs);
		
		// Link the program
		glLinkProgram(programId);
		
		// Check for linking errors
		int linked = glGetProgrami(programId, GL_LINK_STATUS);
		if (linked == 0) {
		String log = glGetProgramInfoLog(programId);
		throw new RuntimeException("Program link failed:\n" + log);
		}
		
		// Detach and delete shaders after successful linking
		glDetachShader(programId, vs);
		glDetachShader(programId, tcs);
		glDetachShader(programId, tes);
		glDetachShader(programId, gs);
		glDetachShader(programId, fs);
		
		glDeleteShader(vs);
		glDeleteShader(tcs);
		glDeleteShader(tes);
		glDeleteShader(gs);
		glDeleteShader(fs);
		}


    
    //
    
    
    
    
    // Activate (use) this shader program
    public void bind() {
        glUseProgram(programId);
    }

    // Unbind (use no shader program)
    public void unbind() {
        glUseProgram(0);
    }

    // Delete the shader program
    public void destroy() {
        unbind();
        if (programId != 0) {
            glDeleteProgram(programId);
        }
    }

    // Uniform utility methods
    public int getUniformLocation(String name) {
        int loc = glGetUniformLocation(programId, name);
        if (loc < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found!");
        }
        return loc;
    }

    public void setUniformMat4(String name, boolean transpose, FloatBuffer matrixBuffer) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniformMatrix4fv(loc, transpose, matrixBuffer);
        }
    }
    
    public void setUniformMat4(String name, boolean transpose, float[] matrix) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniformMatrix4fv(loc, transpose, matrix);
        }
    }

    public void setUniformMat4(String name, boolean transpose, Matrix4f matrix) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            matrix.get(fb);
            setUniformMat4(name, transpose, fb);
        }
    }


    public void setUniform1i(String name, int value) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniform1i(loc, value);
        }
    }

    public void setUniform3f(String name, float x, float y, float z) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniform3f(loc, x, y, z);
        }
    }
    
    public void setUniform4f(String name, float x, float y, float z, float a) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniform4f(loc, x, y, z, a);
        }
    }
    
    public void setUniform4f(String name, Vector4f vec) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniform4f(loc, vec.x, vec.y, vec.z, vec.w);
        }
    }

    public void setUniform3f(String name, Vector3f vec) {
        setUniform3f(name, vec.x, vec.y, vec.z);
    }

    /**
     * Sets a sampler2D uniform to the specified texture unit.
     *
     * @param name        The name of the sampler2D uniform in the shader.
     * @param textureUnit The texture unit to bind to (e.g., 0 for GL_TEXTURE0).
     */
    public void setUniformSampler(String name, int textureUnit) {
        int loc = getUniformLocation(name);
        if (loc >= 0) {
            glUniform1i(loc, textureUnit);
        }
    }

    public void setUniform1f(String name, float value) {
        int location = glGetUniformLocation(programId, name); // Get the uniform location
        if (location < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found in shader program!");
            return;
        }
        glUniform1f(location, value); // Set the float uniform
    }
    
    public void setUniformLights(String arrayName, List<Light> lights) {
        // Suppose arrayName = "lights"
        // We'll also set uniform "numLights"
        setUniform1i("numLights", lights.size());

        for (int i = 0; i < lights.size(); i++) {
            Light l = lights.get(i);
            String prefix = arrayName + "[" + i + "]";
            // position
            setUniform3f(prefix + ".position", l.getPosition());
            // color
            setUniform3f(prefix + ".color", l.getColor());
            // attenuation
            setUniform3f(prefix + ".attenuation", l.getAttenuation());
            // distance
            setUniform1f(prefix + ".distance", l.getEffectiveDistance());
            //System.out.println("Light[" + i + "] Distance: " + l.getEffectiveDistance());
        }
    }


    public int getProgramId() {
        return programId;
    }

    public void setUniformMat4(String name, Matrix4f matrix) {
        int location = getUniformLocation(name); // Get the uniform location
        if (location < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found in shader program!");
            return;
        }

        // Convert Matrix4f to a FloatBuffer and upload it to the shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            matrix.get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }
    
    public void setUniformMat4(String name, org.lwjgl.util.vector.Matrix4f matrix) {
        int location = getUniformLocation(name); // Get the uniform location
        if (location < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found in shader program!");
            return;
        }

        // Convert Matrix4f to a FloatBuffer and upload it to the shader
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(16);
            ((Matrix3fc) matrix).get(buffer);
            glUniformMatrix4fv(location, false, buffer);
        }
    }

    public void setUniformMat3(String name, Matrix3f matrix) {
        int location = getUniformLocation(name);
        if (location < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found!");
            return;
        }

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(9);
            matrix.get(buffer);
            // Use glUniformMatrix3fv instead of glUniformMatrix4fv for a 3x3 matrix.
            glUniformMatrix3fv(location, false, buffer);
        }
    }

    public void setUniformMat4(String name, FloatBuffer fb) {
        // Get the location of the uniform variable from the shader program.
        int location = glGetUniformLocation(programId, name);
        if (location < 0) {
            System.err.println("Warning: Uniform '" + name + "' not found or is not used in shader.");
            return;
        }
        // Upload the 4x4 matrix. The 'false' flag indicates that the matrix is not transposed.
        glUniformMatrix4fv(location, false, fb);
    }

    public void setUniformMat4Array(String uniformName, FloatBuffer fb, int count) {
        // Get the location of the uniform array in the shader
        int location = glGetUniformLocation(programId, uniformName);
        if (location < 0) {
            System.err.println("Warning: Uniform '" + uniformName + "' not found or inactive in shader.");
            return;
        }
        
        // Upload the array of matrices.
        // count is the number of mat4's, fb should contain count * 16 floats.
        glUniformMatrix4fv2(location, count, false, fb);
    }

    private void glUniformMatrix4fv2(int location, int count, boolean transpose, FloatBuffer fb) {
        // Ensure the buffer has enough data.
        if (fb.remaining() < count * 16) {
            throw new IllegalArgumentException("The FloatBuffer does not contain enough data. " +
                "Expected " + (count * 16) + " floats, but got " + fb.remaining());
        }
        
        // Save the original limit of the buffer.
        int originalLimit = fb.limit();
        
        // Set the limit so that only count * 16 floats will be used.
        fb.limit(fb.position() + count * 16);
        
        // Upload the matrix array.
        GL20.glUniformMatrix4fv(location, transpose, fb);
        
        // Restore the original limit.
        fb.limit(originalLimit);
    }

	public void setUniform2f(String name, float f, float g) {
		 int loc = getUniformLocation(name);
	        if (loc >= 0) {
	            glUniform2f(loc, f, g);
	        }
		
	}

	public void cleanUp() {
		// TODO Auto-generated method stub
		
	}
	
	


}
