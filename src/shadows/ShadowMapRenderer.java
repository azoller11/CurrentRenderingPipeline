package shadows;

import java.nio.FloatBuffer;
import java.util.List;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL30;
import org.lwjgl.system.MemoryStack;

import entities.Light;
import entities.Entity;
import shaders.ShaderProgram;
import toolbox.Mesh;
import toolbox.Frustum;

public class ShadowMapRenderer {

    private ShaderProgram shadowShader;
    private int shadowWidth;
    private int shadowHeight;
    private float nearPlane;
    private float farPlane;

    /**
     * Constructs the shadow map renderer.
     *
     * @param shadowShader The shader program used to render the depth (shadow) pass.
     *                     This shader should output depth only.
     * @param shadowWidth  The width (in pixels) of each face of the shadow map cube.
     * @param shadowHeight The height (in pixels) of each face of the shadow map cube.
     * @param nearPlane    The near clipping distance for the shadow projection.
     * @param farPlane     The far clipping distance for the shadow projection.
     */
    public ShadowMapRenderer(int shadowWidth, int shadowHeight, float nearPlane, float farPlane) {
        this.shadowWidth = shadowWidth;
        this.shadowHeight = shadowHeight;
        this.nearPlane = nearPlane;
        this.farPlane = farPlane;
        this.shadowShader =  new ShaderProgram("src/shadows/vertex.glsl",
                null, // no tessellation control shader
                null, // no tessellation eval shader
                null, // no geometry shader
                "src/shadows/fragment.glsl");
    }

    /**
     * Renders the shadow maps for all provided point lights, using frustum culling
     * for each cube map face so that only visible entities are rendered.
     *
     * For each light, this method iterates over the 6 cube map faces:
     * - It computes a view matrix for the face.
     * - It creates a frustum using a common projection (90° FOV, aspect 1) and the view matrix.
     * - It renders only those entities whose bounding sphere (here assumed radius 1.0f)
     *   is inside the frustum.
     *
     * The shadow shader is expected to have the following uniforms:
     * - mat4 shadowMatrix
     * - mat4 model
     *
     * @param entities The list of entities to render into the shadow map.
     * @param lights   The list of point lights that cast shadows.
     */
    public void render(List<Entity> entities, List<Light> lights) {
        // For each point light with a depth cube map:
        for (Light light : lights) {
            // Debug: Print the depth cube map texture ID and framebuffer ID
            //System.out.println("Debug: Depth Cube Map Texture ID: " + light.getDepthCubeMap());
            //System.out.println("Debug: Depth Map FBO: " + light.getDepthMapFBO());

            // Bind the light's framebuffer for rendering to its cube map.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, light.getDepthMapFBO());
           

            // Retrieve the 6 combined view-projection matrices for the cube map faces.
            Matrix4f[] shadowMatrices = light.getShadowMatrices(nearPlane, farPlane);

            // Create a common projection matrix for a 90° FOV (cube face) with aspect ratio 1.
            Matrix4f projection = new Matrix4f().perspective((float) Math.toRadians(90.0f), 1.0f, nearPlane, farPlane);

            // Render one pass per cube map face.
            for (int face = 0; face < 6; face++) {
                // Compute the view matrix for the current face.
                Matrix4f view = getViewMatrixForFace(light.getPosition(), face);

                // Create and calculate the frustum using the projection and view matrices.
                Frustum frustum = new Frustum();
                frustum.calculateFrustum(projection, view);

                // Attach the current face of the cube map as the depth attachment.
                int faceTarget = GL13.GL_TEXTURE_CUBE_MAP_POSITIVE_X + face;
                GL30.glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, faceTarget, light.getDepthCubeMap(), 0);
                // Debug: Print the cube map face target (in hex) for this face.
                //System.out.println("Debug: Rendering cube map face " + face + " (target: 0x" + Integer.toHexString(faceTarget) + ")");

                // Set the viewport to the size of the shadow map.
                GL11.glViewport(0, 0, shadowWidth, shadowHeight);
                // Clear the depth buffer for this face.
                GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);

                // Bind the shadow mapping shader.
                shadowShader.bind();
                // Upload the combined view-projection ("shadow") matrix for this face.
                shadowShader.setUniformMat4("shadowMatrix", shadowMatrices[face]);

                // Render each entity that is inside the frustum.
                for (Entity entity : entities) {
                    // Assume a default bounding sphere radius of 1.0f (adjust as needed)
                    float boundingRadius = 1.0f;
                    if (frustum.contains(entity.getPosition(), boundingRadius)) {
                        drawEntity(entity);
                    }
                }

                shadowShader.unbind();
            }

            // Unbind the framebuffer once done with this light.
            GL30.glBindFramebuffer(GL30.GL_FRAMEBUFFER, 0);
        }
    }


    /**
     * Helper method to compute the view matrix for a given cube face.
     *
     * The face indices map as follows:
     * 0: +X, 1: -X, 2: +Y, 3: -Y, 4: +Z, 5: -Z
     *
     * @param position The position of the light.
     * @param face     The cube face index (0 through 5).
     * @return The view matrix for the given face.
     */
    private Matrix4f getViewMatrixForFace(Vector3f position, int face) {
        Matrix4f view;
        switch (face) {
            case 0:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(1, 0, 0), new Vector3f(0, -1, 0));
                break;
            case 1:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(-1, 0, 0), new Vector3f(0, -1, 0));
                break;
            case 2:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(0, 1, 0), new Vector3f(0, 0, 1));
                break;
            case 3:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(0, -1, 0), new Vector3f(0, 0, -1));
                break;
            case 4:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(0, 0, 1), new Vector3f(0, -1, 0));
                break;
            case 5:
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(0, 0, -1), new Vector3f(0, -1, 0));
                break;
            default:
                // Default to face 0 if an invalid face is provided.
                view = new Matrix4f().lookAt(position, new Vector3f(position).add(1, 0, 0), new Vector3f(0, -1, 0));
                break;
        }
        return view;
    }

    /**
     * Renders a single entity for the shadow pass.
     *
     * This method builds the entity's model matrix from its transform,
     * uploads the "model" uniform, binds the entity's mesh, and issues
     * the draw call.
     *
     * Unnecessary texture bindings and material settings are removed
     * for the depth-only shadow pass.
     *
     * @param entity The entity to be rendered into the shadow map.
     */
    private void drawEntity(Entity entity) {
        // 1) Build the model matrix from the entity's transform.
        Matrix4f model = new Matrix4f()
                .translate(entity.getPosition())
                .rotateXYZ(entity.getRotation().x, entity.getRotation().y, entity.getRotation().z)
                .scale(entity.getScale());

        // 2) Upload the "model" uniform.
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer fb = stack.mallocFloat(16);
            model.get(fb);
            shadowShader.setUniformMat4("model", false, fb);
        }

        // 3) Bind the entity's mesh and issue the draw call.
        Mesh mesh = entity.getMesh();
        GL30.glBindVertexArray(mesh.getVaoId());
        // Draw the mesh using triangles.
        GL11.glDrawArrays(GL11.GL_TRIANGLES, 0, mesh.getVertexCount());
        GL30.glBindVertexArray(0);
    }
}
