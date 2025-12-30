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
        // Bind shadow FBO + viewport
        glBindFramebuffer(GL_FRAMEBUFFER, depthMapFBO);
        glViewport(0, 0, shadowWidth, shadowHeight);
        glClear(GL_DEPTH_BUFFER_BIT);

        // --- IMPORTANT STATE FOR CLEAN SHADOWS ---
        // 1) Render backfaces into shadow map to reduce acne at the source
        glEnable(GL_CULL_FACE);
        glCullFace(GL_FRONT);

        // 2) Apply slope-scaled rasterization bias in the SHADOW PASS (stable, no swimming)
        glEnable(GL_POLYGON_OFFSET_FILL);
        glPolygonOffset(0f, 0f);

        shadowShader.bind();
        shadowShader.setUniformMat4("lightSpaceMatrix", lightSpaceMatrix);

        // Alpha test threshold for masked textures (leaves your behavior intact)
        shadowShader.setUniform1f("alphaThreshold", 0.1f);

        // Frustum cull (your existing logic)
        frustum.calculateFrustum(projectionMatrix, viewMatrix);

        for (Entity entity : entities) {
        	
        	float scale = 100;
        	if (entity.getTexturedModel().getMesh() == null)
        		scale = 100;
        	else 
        		scale = entity.getTexturedModel().getMesh().getFurthestPoint() * entity.getScale();
        	
            if ((Boolean.valueOf(entity.getTexturedModel().isCastShadows())) &&
                frustum.contains(
                    entity.getPosition(),
                    scale
                )) {

                Matrix4f modelMatrix = entity.getModelMatrix();
                shadowShader.setUniformMat4("model", modelMatrix);
                
                
                // Set bone matrices if the model is animated
                if (entity.getTexturedModel().getAnimatedModel() != null) {
                    shadowShader.setUniform1i("useBones", 1);
                    
                    var bones = entity.getTexturedModel().getAnimatedModel().getBones();
                    int boneCount = Math.min(bones.length, MasterRenderer.MAX_BONES);
                    
                    // Create bone matrix array
                    float[] boneArray = new float[16 * MasterRenderer.MAX_BONES];
                    int arrayIndex = 0;
                    
                    // Fill with bone matrices
                    for (int i = 0; i < boneCount; i++) {
                        Matrix4f m = bones[i].getTransformation();
                        
                        boneArray[arrayIndex++] = m.m00();
                        boneArray[arrayIndex++] = m.m01();
                        boneArray[arrayIndex++] = m.m02();
                        boneArray[arrayIndex++] = m.m03();
                        
                        boneArray[arrayIndex++] = m.m10();
                        boneArray[arrayIndex++] = m.m11();
                        boneArray[arrayIndex++] = m.m12();
                        boneArray[arrayIndex++] = m.m13();
                        
                        boneArray[arrayIndex++] = m.m20();
                        boneArray[arrayIndex++] = m.m21();
                        boneArray[arrayIndex++] = m.m22();
                        boneArray[arrayIndex++] = m.m23();
                        
                        boneArray[arrayIndex++] = m.m30();
                        boneArray[arrayIndex++] = m.m31();
                        boneArray[arrayIndex++] = m.m32();
                        boneArray[arrayIndex++] = m.m33();
                    }
                    
                    // Fill remaining with identity matrices
                    for (int i = boneCount; i < MasterRenderer.MAX_BONES; i++) {
                        // Identity matrix
                        boneArray[arrayIndex++] = 1.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 1.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 1.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 0.0f;
                        boneArray[arrayIndex++] = 1.0f;
                    }
                    
                    // Create FloatBuffer and send to shader
                    FloatBuffer fb = org.lwjgl.BufferUtils.createFloatBuffer(16 * MasterRenderer.MAX_BONES);
                    fb.put(boneArray);
                    fb.flip();
                    shadowShader.setUniformMat4ArrayBones("bones", fb, MasterRenderer.MAX_BONES);
                } else {
                    shadowShader.setUniform1i("useBones", 0);
                }

                // Bind diffuse for alpha-tested shadow caster
                shadowShader.setUniform1i("diffuseMap", 0);
                glActiveTexture(GL_TEXTURE0);
                glBindTexture(GL_TEXTURE_2D, entity.getTexturedModel().getTextureId());

                if (entity.getTexturedModel().isHasOpaque() || entity.getTexturedModel().isHasTransparency()) {
                    shadowShader.setUniform1i("useTexture", 1);
                } else {
                    shadowShader.setUniform1i("useTexture", 0);
                }

                int vaoID = 0;
                int vertexCount = 0;
                if (entity.getTexturedModel().getMesh() != null) {
                	vaoID = entity.getTexturedModel().getMesh().getVaoId();
                	vertexCount = entity.getTexturedModel().getMesh().getVertexCount();
                }
                if (entity.getTexturedModel().getAnimatedModel() != null) {
                	vaoID = entity.getTexturedModel().getAnimatedModel().getVaoID();
                	vertexCount = entity.getTexturedModel().getAnimatedModel().getCount();
                }
                
                glBindVertexArray(vaoID);
                
                glDrawArrays(GL_TRIANGLES, 0, vertexCount);
                glBindVertexArray(0);
            }
        }

        shadowShader.unbind();

        // Restore state
        glDisable(GL_POLYGON_OFFSET_FILL);
        glCullFace(GL_BACK);         // restore default
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
