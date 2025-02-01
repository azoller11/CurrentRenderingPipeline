package shadows;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.system.MemoryStack;
import entities.Entity;
import entities.Light;
import shaders.ShaderProgram;
import toolbox.Mesh;

import java.nio.FloatBuffer;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.opengl.GL11.*;

public class ShadowMapRenderer {

    // Shader for cube map shadows
    private final ShaderProgram cubeShadowShader;

    // Shadow map dimensions (square size for each cube face)
    public static final int SHADOW_SIZE = 2048;

    /**
     * Initializes the ShadowMapRenderer by loading the cube shadow shaders.
     */
    public ShadowMapRenderer() {
        // Create the shader program for point lights using cube mapping:
        cubeShadowShader = new ShaderProgram(
            "src/shadows/cube_shadow_vertex.glsl", null,null,
            "src/shadows/cube_shadow_geometry.glsl",
            "src/shadows/cube_shadow_fragment.glsl"
        );
    }

    /**
     * Renders cube map shadow maps for all point lights.
     *
     * @param entities The list of entities to render into the shadow maps.
     * @param lights   The list of point lights that cast shadows.
     */
    public void renderCubeShadowMaps(List<Entity> entities, List<Light> lights) {
        for (int i = 0; i < lights.size(); i++) {
            Light light = lights.get(i);
            // Assume that this light is a point light that uses cube map shadows.
            renderCubeShadowMap(entities, light);
        }
    }

    /**
     * Renders a cube map shadow map for a single point light.
     *
     * @param entities The list of entities to render.
     * @param light    The point light.
     */
    private void renderCubeShadowMap(List<Entity> entities, Light light) {
        // Bind the light’s framebuffer (which should have a cube map depth attachment)
        glBindFramebuffer(GL_FRAMEBUFFER, light.getShadowMapFBO());
        glViewport(0, 0, SHADOW_SIZE, SHADOW_SIZE);
        glClear(GL_DEPTH_BUFFER_BIT);

        // Use the cube shadow shader.
        cubeShadowShader.bind();

        // Compute the 6 view-projection matrices.
        Matrix4f[] shadowMatrices = computeCubeShadowMatrices(light.getPosition(), 0.001f, 1000f);
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16 * 6);
            for (int i = 0; i < 6; i++) {
                shadowMatrices[i].get(16 * i, fb); // offset by face index
            }
            // Upload the array to the shader.
            cubeShadowShader.setUniformMat4Array("shadowMatrices", fb, 6);
        }

        // For each entity, set up its model matrix and render.
        for (Entity entity : entities) {
            renderEntityForCubeShadow(entity);
        }

        cubeShadowShader.unbind();
        glBindFramebuffer(GL_FRAMEBUFFER, 0);
    }

    /**
     * Renders a single entity using the cube shadow shader.
     *
     * @param entity The entity to render.
     */
    private void renderEntityForCubeShadow(Entity entity) {
        // Build the model matrix.
        Matrix4f model = new Matrix4f()
                .translate(entity.getPosition())
                .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)
                .scale(entity.getScale());

        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            cubeShadowShader.setUniformMat4("model", false, fb);
        }

        // Bind the entity’s VAO and draw.
        Mesh mesh = entity.getMesh();
        glBindVertexArray(mesh.getVaoId());
        glDrawArrays(GL_TRIANGLES, 0, mesh.getVertexCount());
        glBindVertexArray(0);
    }

    /**
     * Computes the 6 view-projection matrices for cube mapping from the light’s point of view.
     *
     * @param lightPos The position of the point light.
     * @param near     Near plane distance.
     * @param far      Far plane distance.
     * @return An array of 6 matrices.
     */
    private Matrix4f[] computeCubeShadowMatrices(Vector3f lightPos, float near, float far) {
        Matrix4f[] shadowMatrices = new Matrix4f[6];
        // Create a perspective projection matrix with 90° FOV, aspect ratio 1, and given near/far.
        Matrix4f proj = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, near, far);

        // Define the look directions and corresponding up vectors for each cube face.
        Vector3f[] lookDirs = new Vector3f[] {
            new Vector3f( 1.0f,  0.0f,  0.0f),  // +X
            new Vector3f(-1.0f,  0.0f,  0.0f),  // -X
            new Vector3f( 0.0f,  1.0f,  0.0f),  // +Y
            new Vector3f( 0.0f, -1.0f,  0.0f),  // -Y
            new Vector3f( 0.0f,  0.0f,  1.0f),  // +Z
            new Vector3f( 0.0f,  0.0f, -1.0f)   // -Z
        };

        Vector3f[] upVectors = new Vector3f[] {
            new Vector3f(0.0f, -1.0f,  0.0f),   // +X
            new Vector3f(0.0f, -1.0f,  0.0f),   // -X
            new Vector3f(0.0f,  0.0f,  1.0f),   // +Y
            new Vector3f(0.0f,  0.0f, -1.0f),   // -Y
            new Vector3f(0.0f, -1.0f,  0.0f),   // +Z
            new Vector3f(0.0f, -1.0f,  0.0f)    // -Z
        };

        for (int i = 0; i < 6; i++) {
            // Create the view matrix for this face.
            Matrix4f view = new Matrix4f().lookAt(
                    lightPos,
                    new Vector3f(lightPos).add(lookDirs[i]),
                    upVectors[i]
            );
            shadowMatrices[i] = new Matrix4f(proj).mul(view);
        }
        return shadowMatrices;
    }

    /**
     * Cleans up the cube shadow shader program.
     */
    public void cleanup() {
        cubeShadowShader.destroy();
    }
}
