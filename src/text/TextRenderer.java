package text;

import static org.lwjgl.opengl.GL30.*;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;
import org.lwjgl.system.MemoryStack;
import shaders.ShaderProgram;

public class TextRenderer {
    public enum TextAlignment { LEFT, CENTER, RIGHT }
    
    private final Font font;
    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int maxChars;
    
    // Rendering parameters
    private final Vector4f textColor = new Vector4f(1, 1, 1, 1);
    private final Vector4f outlineColor = new Vector4f(0, 0, 0, 1);
    private float edgeSmoothness = 0.1f;
    private float outlineWidth = 0.3f;

    public TextRenderer(Font font, int maxChars) {
        this.font = font;
        this.maxChars = maxChars;
        this.shader = createShader();
        this.vao = createVAO();
        this.vbo = createVBO();
    }

    private ShaderProgram createShader() {
        return new ShaderProgram(
            "src/text/vertex.glsl", 
            null, 
            null, 
            null, 
            "src/text/fragment.glsl"
        );
    }

    private int createVAO() {
        int vao = glGenVertexArrays();
        glBindVertexArray(vao);
        return vao;
    }

    private int createVBO() {
        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferData(GL_ARRAY_BUFFER, maxChars * 6 * 4 * 4, GL_DYNAMIC_DRAW);
        
        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8);
        glEnableVertexAttribArray(1);
        
        glBindVertexArray(0);
        return vbo;
    }

    public void renderText(String text, float x, float y, float scale, 
                          Matrix4f projection, float maxWidth, TextAlignment alignment) {
        List<String> lines = wrapText(text, scale, maxWidth);
        float lineHeight = font.getLineHeight() * scale;
        
        shader.bind();
        setupShaderUniforms(projection);
        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTextureID());

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineWidth = calculateStringWidth(line, scale);
            float xOffset = calculateXOffset(alignment, lineWidth, maxWidth);
            renderLine(line, x + xOffset, y + (i * lineHeight), scale);
        }
        
        shader.unbind();
    }

    private List<String> wrapText(String text, float scale, float maxWidth) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float currentWidth = 0;
        
        for (String word : words) {
            float wordWidth = calculateStringWidth(word, scale);
            
            if (currentLine.isEmpty()) {
                currentLine.append(word);
                currentWidth = wordWidth;
            } else {
                float spaceWidth = calculateStringWidth(" ", scale);
                float potentialWidth = currentWidth + spaceWidth + wordWidth;
                
                if (potentialWidth <= maxWidth) {
                    currentLine.append(" ").append(word);
                    currentWidth = potentialWidth;
                } else {
                    lines.add(currentLine.toString());
                    currentLine = new StringBuilder(word);
                    currentWidth = wordWidth;
                }
            }
        }
        
        if (!currentLine.isEmpty()) lines.add(currentLine.toString());
        return lines;
    }

    private float calculateXOffset(TextAlignment alignment, float lineWidth, float maxWidth) {
        return switch (alignment) {
            case CENTER -> (maxWidth - lineWidth) / 2;
            case RIGHT -> maxWidth - lineWidth;
            default -> 0;
        };
    }

    private void setupShaderUniforms(Matrix4f projection) {
        shader.setUniformMat4("projectionMatrix", projection);
        shader.setUniform4f("textColor", textColor);
        shader.setUniform4f("outlineColor", outlineColor);
        shader.setUniform1f("edgeSmoothness", edgeSmoothness);
        shader.setUniform1f("outlineWidth", outlineWidth);
    }

    private void renderLine(String text, float x, float y, float scale) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(text.length() * 6 * 4);
            float cursorX = x;
            float cursorY = y + font.getBase() * scale;

            for (char c : text.toCharArray()) {
                Font.Character ch = font.getCharacter(c);
                if (ch == null) continue;

                addCharacterVertices(buffer, cursorX, cursorY, ch, scale);
                cursorX += ch.xadvance * scale;
            }

            updateVBO(buffer);
            drawText(text.length());
        }
    }

    private void addCharacterVertices(FloatBuffer buffer, float cursorX, float cursorY, 
                                      Font.Character ch, float scale) {
        float xpos = cursorX + ch.xoffset * scale;
        float ypos = cursorY - (ch.yoffset + ch.height) * scale;
        float w = ch.width * scale;
        float h = ch.height * scale;

        float u1 = (float) ch.x / font.getAtlasWidth();
        float v1 = (float) (ch.y + ch.height) / font.getAtlasHeight();
        float u2 = u1 + (float) ch.width / font.getAtlasWidth();
        float v2 = (float) ch.y / font.getAtlasHeight();

        // Triangle 1
        addVertex(buffer, xpos,     ypos + h, u1, v2);
        addVertex(buffer, xpos + w, ypos,     u2, v1);
        addVertex(buffer, xpos,     ypos,     u1, v1);

        // Triangle 2
        addVertex(buffer, xpos,     ypos + h, u1, v2);
        addVertex(buffer, xpos + w, ypos + h, u2, v2);
        addVertex(buffer, xpos + w, ypos,     u2, v1);
    }

    private void updateVBO(FloatBuffer buffer) {
        buffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
    }

    private void drawText(int charCount) {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, charCount * 6);
        glBindVertexArray(0);
    }

    // Configuration methods
    public void setTextColor(float r, float g, float b, float a) {
        textColor.set(r, g, b, a);
    }

    public void setOutlineColor(float r, float g, float b, float a) {
        outlineColor.set(r, g, b, a);
    }

    public void setEdgeSmoothness(float smoothness) {
        this.edgeSmoothness = smoothness;
    }

    public void setOutlineWidth(float width) {
        this.outlineWidth = width;
    }

    public float calculateStringWidth(String text, float scale) {
        float width = 0;
        for (char c : text.toCharArray()) {
            Font.Character ch = font.getCharacter(c);
            if (ch != null) width += ch.xadvance * scale;
        }
        return width;
    }

    public void cleanUp() {
        glDeleteVertexArrays(vao);
        glDeleteBuffers(vbo);
        shader.destroy();
    }

    private void addVertex(FloatBuffer buffer, float x, float y, float u, float v) {
        buffer.put(x).put(y).put(u).put(v);
    }
}