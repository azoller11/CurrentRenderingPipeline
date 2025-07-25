package gui;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.BufferUtils;
import org.lwjgl.opengl.GL40;
import shaders.ShaderProgram;
import gui.GuiTexture;
import settings.EngineSettings;

import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL40.*;

public class TextureRenderer {
    private ShaderProgram shaderProgram;
    private int vaoId;
    private int vboId;
    private List<GuiTexture> textures;

    // Quad vertices (positions and texture coordinates)
    private static final float[] VERTICES = {
            // Positions    // Texture Coords
            0.0f, 1.0f, 0.0f, 0.0f, 1.0f, // Top-left
            1.0f, 1.0f, 0.0f, 1.0f, 1.0f, // Top-right
            1.0f, 0.0f, 0.0f, 1.0f, 0.0f, // Bottom-right
            0.0f, 0.0f, 0.0f, 0.0f, 0.0f  // Bottom-left
    };

    // Indices for two triangles
    private static final int[] INDICES = {
            0, 1, 2,
            2, 3, 0
    };

    public TextureRenderer() {
        this.shaderProgram = new ShaderProgram(
                "src/gui/texture_vertex.glsl",
                null, 
                null, 
                null,
                "src/gui/texture_fragment.glsl"
        );
        this.textures = new ArrayList<>();
        setupMesh();
    }

    private void setupMesh() {
        vaoId = glGenVertexArrays();
        glBindVertexArray(vaoId);

        // Vertex Buffer Object
        vboId = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vboId);
        FloatBuffer vertexBuffer = BufferUtils.createFloatBuffer(VERTICES.length);
        vertexBuffer.put(VERTICES).flip();
        glBufferData(GL_ARRAY_BUFFER, vertexBuffer, GL_STATIC_DRAW);

        // Position attribute
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 5 * Float.BYTES, 0);
        glEnableVertexAttribArray(0);

        // Texture coordinate attribute
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 5 * Float.BYTES, 3 * Float.BYTES);
        glEnableVertexAttribArray(1);

        // Element Buffer Object
        int eboId = glGenBuffers();
        glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, eboId);
        glBufferData(GL_ELEMENT_ARRAY_BUFFER, INDICES, GL_STATIC_DRAW);

        // Unbind VAO
        glBindVertexArray(0);
    }

    public void addTexture(GuiTexture texture) {
        textures.add(texture);
    }
    
    public void addTexture(String filePath, float posX, float posY, float scaleX, float scaleY) {
        GuiTexture newTexture = new GuiTexture(filePath, posX, posY, scaleX, scaleY);
        textures.add(newTexture);
    }

    public void removeTexture(GuiTexture texture) {
        textures.remove(texture);
    }

    FloatBuffer projBuffer;
    FloatBuffer viewBuffer;
    GuiTexture texture;
    Matrix4f model;
    FloatBuffer modelBuffer;
    public void render(Matrix4f projection, Matrix4f view, double mouseX, double mouseY) {
        shaderProgram.bind();

        // Set projection and view matrices
        try (org.lwjgl.system.MemoryStack stack = org.lwjgl.system.MemoryStack.stackPush()) {
            projBuffer = stack.mallocFloat(16);
            projection.get(projBuffer);
            shaderProgram.setUniformMat4("projection", false, projBuffer);

            viewBuffer = stack.mallocFloat(16);
            view.get(viewBuffer);
            shaderProgram.setUniformMat4("view", false, viewBuffer);
        }

        glBindVertexArray(vaoId);
        EngineSettings.overTexture = false;
        for (int i = 0; i < textures.size(); i++) {
            texture = textures.get(i);
            
            
            shaderProgram.setUniform1f("brightness", 1.0f);
            if (!EngineSettings.grabMouse) {
            	texture.checkOver(mouseX, mouseY);
                
                if (texture instanceof GuiButton) {
                    GuiButton button = (GuiButton) texture;
                    button.checkClick(mouseX, mouseY);
                    shaderProgram.setUniform1f("brightness", button.getBrightness()); // Pass brightness to shader
                } else {
                    
                }
            }
            
            
            // Bind texture to texture unit 0
            if (texture.hasTexture()) {
                texture.bind(0);
                shaderProgram.setUniformSampler("textureSampler", 0);
                shaderProgram.setUniform1i("useTexture", 1);
            } else {
                shaderProgram.setUniform4f("solidColor", texture.getColor().x,texture.getColor().y,texture.getColor().z,texture.getColor().w);
                shaderProgram.setUniform1i("useTexture", 0);
            }

            // Calculate model matrix for position and scale
            model = new Matrix4f()
            	    .translate(new Vector3f(texture.getPosX(), texture.getPosY(), 0.0f)) // Move to position
            	    .rotateZ((float) Math.toRadians(texture.getRotation())) // Rotate around Z-axis
            	    .scale(new Vector3f(texture.getScaleX(), texture.getScaleY(), 1.0f)); // Scale

            modelBuffer = BufferUtils.createFloatBuffer(16);
            model.get(modelBuffer);
            shaderProgram.setUniformMat4("model", false, modelBuffer);

            // Draw the quad
            glDrawElements(GL_TRIANGLES, INDICES.length, GL_UNSIGNED_INT, 0);
            // Unbind texture
            texture.unbind(0);
            
            
        }

        glBindVertexArray(0);
        shaderProgram.unbind();
    }

    public void cleanUp() {
        glDeleteBuffers(vboId);
        glDeleteVertexArrays(vaoId);
        for (GuiTexture texture : textures) {
            texture.destroy();
        }
    }
}