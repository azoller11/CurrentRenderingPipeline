package grass;

import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.nio.FloatBuffer;
import java.util.List;

public class GrassGenerator {
    private int vaoId;
    private int vboId;
    private int bladeCount;

    /**
     * Creates a grass generator using a list of grass instances.
     *
     * @param grassList List of grass blades with their base positions.
     */
    public GrassGenerator(List<Grass> grassList) {
        this.bladeCount = grassList.size();
        float[] positions = new float[bladeCount * 3];
        for (int i = 0; i < bladeCount; i++) {
            Vector3f pos = grassList.get(i).getPosition();
            positions[i * 3]     = pos.x;
            positions[i * 3 + 1] = pos.y;
            positions[i * 3 + 2] = pos.z;
        }
        
        // Generate and bind VAO
        vaoId = GL30.glGenVertexArrays();
        GL30.glBindVertexArray(vaoId);

        // Generate and bind VBO
        vboId = GL30.glGenBuffers();
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, vboId);

        FloatBuffer buffer = BufferUtils.createFloatBuffer(positions.length);
        buffer.put(positions).flip();
        GL30.glBufferData(GL30.GL_ARRAY_BUFFER, buffer, GL30.GL_STATIC_DRAW);

        // Define layout: location 0 = vec3 (position)
        GL30.glVertexAttribPointer(0, 3, GL30.GL_FLOAT, false, 0, 0);
        GL30.glEnableVertexAttribArray(0);

        // Unbind VAO and VBO
        GL30.glBindBuffer(GL30.GL_ARRAY_BUFFER, 0);
        GL30.glBindVertexArray(0);
    }

    /**
     * Renders all the grass blades as points.
     * Each point will be expanded to a blade in the geometry shader.
     */
    public void render() {
    	GL11.glDisable(GL11.GL_CULL_FACE);
        GL30.glBindVertexArray(vaoId);
        GL30.glDrawArrays(GL30.GL_POINTS, 0, bladeCount);
        GL30.glBindVertexArray(0);
        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glCullFace(GL11.GL_BACK);
    }
    
    /**
     * Cleans up the allocated OpenGL resources.
     */
    public void cleanup() {
        GL30.glDeleteBuffers(vboId);
        GL30.glDeleteVertexArrays(vaoId);
    }
}
