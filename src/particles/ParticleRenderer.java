package particles;

import java.nio.FloatBuffer;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;

import entities.Camera;
import shaders.ShaderProgram;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL31.*;
import static org.lwjgl.opengl.GL33.*;

public class ParticleRenderer {

    private static final float[] QUAD_VERTICES = {
        -0.5f,  0.5f,
        -0.5f, -0.5f,
         0.5f,  0.5f,
         0.5f, -0.5f
    };

    private static final int MAX_INSTANCES = 40_000;
    private static final int INSTANCE_DATA_LENGTH = 21; // 16 + 4 + 1

    private final int vao;
    private final int quadVbo;
    private final int instanceVbo;

    private final ShaderProgram shader;

    private final FloatBuffer instanceBuffer =
            BufferUtils.createFloatBuffer(MAX_INSTANCES * INSTANCE_DATA_LENGTH);

    private final float[] instanceData =
            new float[MAX_INSTANCES * INSTANCE_DATA_LENGTH];

    private final Matrix4f modelMatrix = new Matrix4f();
    private final Matrix4f modelViewMatrix = new Matrix4f();

    public ParticleRenderer(Matrix4f projectionMatrix) {

        shader = new ShaderProgram(
                "src/particles/particle_vertex.glsl",
                null, null, null,
                "src/particles/particle_fragment.glsl"
        );

        vao = glGenVertexArrays();
        glBindVertexArray(vao);

        // quad vbo (location=0)
        quadVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, quadVbo);
        glBufferData(GL_ARRAY_BUFFER, QUAD_VERTICES, GL_STATIC_DRAW);
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 0, 0);
        glEnableVertexAttribArray(0);

        // instance vbo
        instanceVbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
        glBufferData(GL_ARRAY_BUFFER,
                (long)MAX_INSTANCES * INSTANCE_DATA_LENGTH * Float.BYTES,
                GL_STREAM_DRAW
        );

        int strideBytes = INSTANCE_DATA_LENGTH * Float.BYTES;
        int offsetBytes = 0;

        // modelView rows -> locations 1..4 (each vec4)
        for (int i = 0; i < 4; i++) {
            glVertexAttribPointer(1 + i, 4, GL_FLOAT, false, strideBytes, offsetBytes);
            glEnableVertexAttribArray(1 + i);
            glVertexAttribDivisor(1 + i, 1);
            offsetBytes += 4 * Float.BYTES;
        }

        // texOffsets -> location 5 (vec4)
        glVertexAttribPointer(5, 4, GL_FLOAT, false, strideBytes, offsetBytes);
        glEnableVertexAttribArray(5);
        glVertexAttribDivisor(5, 1);
        offsetBytes += 4 * Float.BYTES;

        // blend -> location 6 (float)
        glVertexAttribPointer(6, 1, GL_FLOAT, false, strideBytes, offsetBytes);
        glEnableVertexAttribArray(6);
        glVertexAttribDivisor(6, 1);

        glBindVertexArray(0);

        // set uniforms once
        shader.bind();
        shader.setUniformMat4("projectionMatrix", projectionMatrix);
        shader.setUniformSampler("particleTexture", 0); // IMPORTANT
        shader.unbind();
    }

    public void render(Map<ParticleTexture, List<Particle>> particles, Camera camera) {
        if (particles.isEmpty()) return;

        shader.bind();
        glBindVertexArray(vao);

        // PARTICLES: usually render with depth test OFF (or ON but depthMask false).
        // Start OFF so you can see them immediately:
        glDisable(GL_CULL_FACE);
        glEnable(GL_DEPTH_TEST);
        glDepthMask(false);
        glDepthFunc(GL_LEQUAL);
        glEnable(GL_BLEND);


        for (ParticleTexture texture : particles.keySet()) {

            // blend mode
            if (texture.isAdditive()) {
                glBlendFunc(GL_SRC_ALPHA, GL_ONE);
            } else {
                glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            }

            // bind texture unit 0
            glActiveTexture(GL_TEXTURE0);
            glBindTexture(GL_TEXTURE_2D, texture.getTextureID());
            shader.setUniform1i("particleTexture", 0);  // Add this line
            shader.setUniform1f("numberOfRows", texture.getNumberOfRows());

            List<Particle> list = particles.get(texture);
            int count = Math.min(list.size(), MAX_INSTANCES);

            int pointer = 0;

            Matrix4f view = camera.getViewMatrix();

            for (int i = 0; i < count; i++) {
                Particle p = list.get(i);

                buildModelViewMatrix(
                        // NOTE: you're still using LWJGL util vectors inside Particle.
                        // This is OK for now, but eventually switch Particle to JOML.
                        new Vector3f(p.getPosition().getX(), p.getPosition().getY(), p.getPosition().getZ()),
                        p.getRotation(),
                        p.getScale(),
                        view
                );

                modelViewMatrix.get(instanceData, pointer);
                pointer += 16;

                org.lwjgl.util.vector.Vector2f t1 = p.getTexOffset1();
                org.lwjgl.util.vector.Vector2f t2 = p.getTexOffset2();
                instanceData[pointer++] = t1.x;
                instanceData[pointer++] = t1.y;
                instanceData[pointer++] = t2.x;
                instanceData[pointer++] = t2.y;

                instanceData[pointer++] = p.getBlend();
            }

            instanceBuffer.clear();
            instanceBuffer.put(instanceData, 0, pointer).flip();

            glBindBuffer(GL_ARRAY_BUFFER, instanceVbo);
            glBufferSubData(GL_ARRAY_BUFFER, 0, instanceBuffer);

            glDrawArraysInstanced(GL_TRIANGLE_STRIP, 0, 4, count);
        }

        // restore
        glDisable(GL_BLEND);
        glDepthMask(true);
        glDepthFunc(GL_LESS);
        glEnable(GL_CULL_FACE);

        glBindVertexArray(0);
        shader.unbind();
    }

    private void buildModelViewMatrix(Vector3f position, float rotationDeg, float scale, Matrix4f view) {
        modelMatrix.identity().translate(position);

        // billboard from view (same as you had)
        modelMatrix.m00(view.m00()); modelMatrix.m01(view.m10()); modelMatrix.m02(view.m20());
        modelMatrix.m10(view.m01()); modelMatrix.m11(view.m11()); modelMatrix.m12(view.m21());
        modelMatrix.m20(view.m02()); modelMatrix.m21(view.m12()); modelMatrix.m22(view.m22());

        modelMatrix.rotateZ((float)Math.toRadians(rotationDeg));
        modelMatrix.scale(scale);

        modelViewMatrix.set(view).mul(modelMatrix);
    }

    public void cleanUp() {
        shader.destroy();
        glDeleteBuffers(quadVbo);
        glDeleteBuffers(instanceVbo);
        glDeleteVertexArrays(vao);
    }
}
