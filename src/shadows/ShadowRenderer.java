package shadows;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL30.*;

import java.nio.ByteBuffer;
import java.nio.FloatBuffer;
import java.util.List;

import entities.Camera;
import entities.Entity;
import entities.Light;
import entities.TexturedModel;
import renderer.MasterRenderer;
import shaders.ShaderProgram;
import toolbox.Frustum;

public class ShadowRenderer {
    private static int shadowWidth = 0;
    private static int shadowHeight;

    // OpenGL handles
    private int depthMapFBO;
    private int depthMap; // Depth texture ID

    // Simple shader to render depth only
    private ShaderProgram shadowShader;

    private Frustum frustum;

    // Reused across calls to avoid allocating a new native buffer every animated entity, every frame.
    private final FloatBuffer boneBuffer = org.lwjgl.BufferUtils.createFloatBuffer(16 * MasterRenderer.MAX_BONES);
    private final float[] boneArrayScratch = new float[16 * MasterRenderer.MAX_BONES];

    public ShadowRenderer(int shadowWidth, int shadowHeight) {
        ShadowRenderer.shadowWidth = shadowWidth;
        ShadowRenderer.shadowHeight = shadowHeight;
        this.frustum = new Frustum();
        initShadowFBO();
        initShadowShader();
    }

    // Initialize the framebuffer and attach a depth texture
    private void initShadowFBO() {
        // Generate framebuffer
        depthMapFBO = glGenFramebuffers();

        // Create depth texture
        depthMap = glGenTextures();
        glBindTexture(GL_TEXTURE_2D, depthMap);

        // Use 32-bit float depth for better precision (important with large ortho volumes)
        glTexImage2D(
            GL_TEXTURE_2D,
            0,
            GL_DEPTH_COMPONENT32F,
            shadowWidth,
            shadowHeight,
            0,
            GL_DEPTH_COMPONENT,
            GL_FLOAT,
            (ByteBuffer) null
        );

        // Set texture parameters
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MIN_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_MAG_FILTER, GL_NEAREST);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_S, GL_CLAMP_TO_BORDER);
        glTexParameteri(GL_TEXTURE_2D, GL_TEXTURE_WRAP_T, GL_CLAMP_TO_BORDER);

        // Border color (white – meaning “not in shadow”)
        float[] borderColor = new float[] { 1.0f, 1.0f, 1.0f, 1.0f };
        glTexParameterfv(GL_TEXTURE_2D, GL_TEXTURE_BORDER_COLOR, borderColor);

        // Attach depth texture as FBO's depth buffer
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glFramebufferTexture2D(GL_FRAMEBUFFER, GL_DEPTH_ATTACHMENT, GL_TEXTURE_2D, depthMap, 0);

        // We are not going to render any color data
        glDrawBuffer(GL_NONE);
        glReadBuffer(GL_NONE);

        if (glCheckFramebufferStatus(GL_FRAMEBUFFER) != GL_FRAMEBUFFER_COMPLETE) {
            throw new RuntimeException("Error: Shadow framebuffer is not complete!");
        }

        glBindFramebuffer(GL_FRAMEBUFFER, 0);
        glBindTexture(GL_TEXTURE_2D, 0);
    }

    // Initialize the shadow shader that renders the scene from the light’s perspective.
    private void initShadowShader() {
        shadowShader = new ShaderProgram("src/shadows/vertex.glsl", null, null, null, "src/shadows/fragment.glsl");
    }

    /**
     * Renders the scene’s depth (shadow map) from the light’s point of view.
     *
     * @param entities         List of entities to render
     * @param lightSpaceMatrix The transformation matrix (from world to light space)
     */
  public void renderShadowMap(
        List<Entity> entities,
        Matrix4f lightSpaceMatrix,
        Matrix4f viewMatrix,
        Matrix4f projectionMatrix
) {
    glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
    glViewport(0, 0, shadowWidth, shadowHeight);
    glClear(GL_DEPTH_BUFFER_BIT);

    // Shadow pass state
    glEnable(GL_CULL_FACE);
    glCullFace(GL_FRONT); // reduce acne
    glEnable(GL_POLYGON_OFFSET_FILL);
    glPolygonOffset(2.0f, 4.0f);

    shadowShader.bind();
    shadowShader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);
    shadowShader.setUniform1f("alphaThreshold", 0.1f);

    frustum.calculateFrustum(projectionMatrix, viewMatrix);

    for (Entity entity : entities) {
        TexturedModel tm = entity.getTexturedModel();
        if (tm == null || !tm.isCastShadows()) continue;

        float radius = 100f;
        if (tm.getMesh() != null) {
            radius = tm.getMesh().getFurthestPoint() * entity.getScale();
        }

        if (!frustum.contains(entity.getPosition(), radius)) continue;

        // Alpha-tested shadow casters
        shadowShader.setUniform1i("diffuseMap", 0);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, tm.getTextureId());

        boolean useTexture = tm.isHasOpaque() || tm.isHasTransparency();
        shadowShader.setUniform1i("useTexture", useTexture ? 1 : 0);

        Matrix4f entityMatrix = entity.getModelMatrix();

        // ============================================================
        // MULTI-MESH ANIMATED MODEL
        // ============================================================
        if (entity.getAnimatedModels() != null && !entity.getAnimatedModels().isEmpty()) {

            for (animatedModel.AnimatedModel part : entity.getAnimatedModels()) {

                // MUST match main renderer logic
                Matrix4f finalModel = new Matrix4f(entityMatrix);

                boolean verticesWereBakedToNodeSpace = true; // ← MUST match loader
                if (!verticesWereBakedToNodeSpace) {
                    finalModel.mul(part.getBindPoseNodeGlobal());

                    if (!part.isSkinned()) {
                        Matrix4f nodeGlobal = part.getAnimatedNodeTransform(part.getMeshNodeName());
                        if (nodeGlobal != null) finalModel.mul(nodeGlobal);
                    }
                }

                shadowShader.setUniformMat4("model", finalModel);

                // Bones are per-part
                setBonesForShadow(part);

                glBindVertexArray(part.getVaoID());
                glDrawElements(GL_TRIANGLES, part.getCount(), GL_UNSIGNED_INT, 0);
                glBindVertexArray(0);
            }

        }
        // ============================================================
        // SINGLE-MESH PATH
        // ============================================================
        else {
            shadowShader.setUniformMat4("model", entityMatrix);

            if (tm.getAnimatedModel() != null) {
                setBonesForShadow(tm.getAnimatedModel());
            } else {
                shadowShader.setUniform1i("useBones", 0);
            }

            if (tm.getMesh() != null) {
                glBindVertexArray(tm.getMesh().getVaoId());
                glDrawArrays(GL_TRIANGLES, 0, tm.getMesh().getVertexCount());
                glBindVertexArray(0);
            }
            else if (tm.getAnimatedModel() != null) {
                glBindVertexArray(tm.getAnimatedModel().getVaoID());
                glDrawElements(
                        GL_TRIANGLES,
                        tm.getAnimatedModel().getCount(),
                        GL_UNSIGNED_INT,
                        0
                );
                glBindVertexArray(0);
            }
        }
    }

    shadowShader.unbind();

    // Restore state
    glDisable(GL_POLYGON_OFFSET_FILL);
    glCullFace(GL_BACK);
    glBindFramebuffer(GL_FRAMEBUFFER, 0);
}

    public static Matrix4f createLightSpaceMatrix(Light light, Camera camera) {
        float orthoSize = 400.0f * 15;
        float near = 1.0f;
        float far = 1800.0f * 15;

        // Light direction (keep your existing inversion logic)
        Vector3f lightDir = new Vector3f(light.getPosition()).normalize();
        lightDir.z = -lightDir.z;
        lightDir.x = -lightDir.x;

        // Use camera position as the shadow map center
        Vector3f target = new Vector3f(camera.getPosition());

        // Light position relative to camera
        Vector3f lightPos = new Vector3f(target).sub(new Vector3f(lightDir).mul(orthoSize * 2));

        Matrix4f lightView = new Matrix4f().lookAt(
            lightPos,
            target,
            new Vector3f(0, 1, 0)
        );

        Matrix4f lightProjection = new Matrix4f().ortho(
            -orthoSize, orthoSize,
            -orthoSize, orthoSize,
            near, far
        );

        Matrix4f lightSpaceMatrix = new Matrix4f();
        lightProjection.mul(lightView, lightSpaceMatrix);

        // Keep your existing Y-flip
        lightSpaceMatrix.scale(1, -1, 1);

        // Texel snapping for stable shadows
        float texelSize = (2.0f * orthoSize) / shadowWidth;
        Vector4f origin = new Vector4f(target, 1.0f);
        lightSpaceMatrix.transform(origin);

        origin.x = Math.round(origin.x / texelSize) * texelSize;
        origin.y = Math.round(origin.y / texelSize) * texelSize;

        Matrix4f adjust = new Matrix4f().translate(-origin.x, -origin.y, 0);
        lightSpaceMatrix.mul(adjust);

        return lightSpaceMatrix;
    }
    
    private void setBonesForShadow(animatedModel.AnimatedModel animatedModel) {
        if (animatedModel != null && animatedModel.isSkinned()) {
            shadowShader.setUniform1i("useBones", 1);

            var bones = animatedModel.getBones();
            int boneCount = (bones == null) ? 0 : Math.min(bones.length, MasterRenderer.MAX_BONES);

            float[] boneArray = boneArrayScratch;
            int idx = 0;

            for (int i = 0; i < boneCount; i++) {
                Matrix4f m = bones[i].getTransformation();

                boneArray[idx++] = m.m00(); boneArray[idx++] = m.m01(); boneArray[idx++] = m.m02(); boneArray[idx++] = m.m03();
                boneArray[idx++] = m.m10(); boneArray[idx++] = m.m11(); boneArray[idx++] = m.m12(); boneArray[idx++] = m.m13();
                boneArray[idx++] = m.m20(); boneArray[idx++] = m.m21(); boneArray[idx++] = m.m22(); boneArray[idx++] = m.m23();
                boneArray[idx++] = m.m30(); boneArray[idx++] = m.m31(); boneArray[idx++] = m.m32(); boneArray[idx++] = m.m33();
            }

            // Fill remaining with identity
            for (int i = boneCount; i < MasterRenderer.MAX_BONES; i++) {
                boneArray[idx++] = 1; boneArray[idx++] = 0; boneArray[idx++] = 0; boneArray[idx++] = 0;
                boneArray[idx++] = 0; boneArray[idx++] = 1; boneArray[idx++] = 0; boneArray[idx++] = 0;
                boneArray[idx++] = 0; boneArray[idx++] = 0; boneArray[idx++] = 1; boneArray[idx++] = 0;
                boneArray[idx++] = 0; boneArray[idx++] = 0; boneArray[idx++] = 0; boneArray[idx++] = 1;
            }

            boneBuffer.clear();
            boneBuffer.put(boneArray).flip();
            shadowShader.setUniformMat4ArrayBones("bones", boneBuffer, MasterRenderer.MAX_BONES);
        } else {
            shadowShader.setUniform1i("useBones", 0);
        }
    }


    /**
     * Returns the OpenGL texture ID for the depth map.
     */
    public int getDepthMapTexture() {
        return depthMap;
    }

    // Call this method during cleanup
    public void cleanUp() {
        glDeleteFramebuffers(depthMapFBO);
        glDeleteTextures(depthMap);
        shadowShader.destroy();
    }
}
