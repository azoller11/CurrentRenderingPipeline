package text;

import static org.lwjgl.opengl.GL11.*;
import static org.lwjgl.opengl.GL13.*;
import static org.lwjgl.opengl.GL30.*;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.*;
import static org.lwjgl.opengl.GL12.*;
import static org.lwjgl.opengl.EXTTextureFilterAnisotropic.*;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector4f;
import java.nio.FloatBuffer;
import java.util.ArrayList;
import java.util.List;

import org.lwjgl.opengl.GL;
import org.lwjgl.opengl.GL11;
import org.lwjgl.system.MemoryStack;

import shaders.ShaderProgram;

public class TextRenderer {

    public enum TextAlignment { LEFT, CENTER, RIGHT }

    private final Font font;
    private final ShaderProgram shader;
    private final int vao;
    private final int vbo;
    private final int maxChars;

    private float screenWidth;
    private float screenHeight;

    // Core colors
    private final Vector4f textColor = new Vector4f(1, 1, 1, 1);
    private final Vector4f outlineColor = new Vector4f(0, 0, 0, 0);
    private float outlineWidth = 0.0f;

    // Border properties
    private final Vector4f borderColor = new Vector4f(0, 0, 0, 0);
    private float borderWidth = 0.0f;
    private float borderSoftness = 0.0f;

    // Effects
    private final Vector4f shadowColor = new Vector4f(0, 0, 0, 0f);
    private final Vector2f shadowOffsetPx = new Vector2f(0f, 0f);
    private float shadowSoftness = 0.0f;

    private final Vector4f glowColor = new Vector4f(0, 0, 0, 0);
    private float glowStrength = 0.0f;
    private float glowRadius = 0.0f;

    // Optional tint gradient
    private final Vector4f tintTop = new Vector4f(1,1,1,0);
    private final Vector4f tintBottom = new Vector4f(1,1,1,0);
    private float tintMix = 0.0f;

    // Animation
    private float timeSeconds = 0f;
    private float waveAmplitudePx = 0.0f;
    private float waveFrequency = 0.0f;
    private float jitterPx = 0.0f;

    public TextRenderer(Font font, int maxChars, float screenWidth, float screenHeight) {
        this.font = font;
        this.maxChars = maxChars;
        this.shader = createShader();

        this.vao = createVAO();
        this.vbo = createVBO();

        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
    }

    public void resize(float w, float h) {
        this.screenWidth = w;
        this.screenHeight = h;
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
        int id = glGenVertexArrays();
        glBindVertexArray(id);
        return id;
    }

    private int createVBO() {
        int id = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, id);

        glBufferData(GL_ARRAY_BUFFER, (long) maxChars * 6L * 4L * Float.BYTES, GL_DYNAMIC_DRAW);

        glVertexAttribPointer(0, 2, GL_FLOAT, false, 16, 0);
        glEnableVertexAttribArray(0);
        glVertexAttribPointer(1, 2, GL_FLOAT, false, 16, 8);
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
        return id;
    }

    public void renderText(
            String text,
            float xNorm, float yNorm,
            float scale,
            float maxWidthNorm,
            TextAlignment alignment
    ) {
        if (text == null || text.isEmpty()) return;
        
        

        float xPx = xNorm * screenWidth;
        float yPx = yNorm * screenHeight;
        float maxWidthPx = maxWidthNorm * screenWidth;

        List<String> lines = wrapText(text, scale, maxWidthPx);
        float lineHeightPx = font.getLineHeight() * scale;

        glDisable(GL_DEPTH_TEST);
        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        shader.bind();
        setupShaderUniforms(getFlatProjectionPixels());

        glActiveTexture(GL_TEXTURE0);
        glBindTexture(GL_TEXTURE_2D, font.getTextureID());
        shader.setUniform1i("textAtlas", 0);

        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            float lineWidthPx = calculateStringWidth(line, scale);
            float xOffsetPx = calculateXOffset(alignment, lineWidthPx, maxWidthPx);
            renderLine(line, xPx + xOffsetPx, yPx + (i * lineHeightPx), scale);
        }

        shader.unbind();
        glBindTexture(GL_TEXTURE_2D, 0);
        GL11.glDisable(GL11.GL_BLEND);
        GL11.glEnable(GL11.GL_DEPTH_TEST);
    }

    private List<String> wrapText(String text, float scale, float maxWidthPx) {
        List<String> lines = new ArrayList<>();
        String[] words = text.split(" ");
        StringBuilder currentLine = new StringBuilder();
        float currentWidth = 0;

        float spaceWidth = calculateStringWidth(" ", scale);

        for (String word : words) {
            float wordWidth = calculateStringWidth(word, scale);

            if (currentLine.isEmpty()) {
                currentLine.append(word);
                currentWidth = wordWidth;
            } else {
                float potentialWidth = currentWidth + spaceWidth + wordWidth;
                if (potentialWidth <= maxWidthPx) {
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

    private float calculateXOffset(TextAlignment alignment, float lineWidthPx, float maxWidthPx) {
        return switch (alignment) {
            case CENTER -> (maxWidthPx - lineWidthPx) * 0.5f;
            case RIGHT -> (maxWidthPx - lineWidthPx);
            default -> 0f;
        };
    }

    private void setupShaderUniforms(Matrix4f projectionPixels) {
        shader.setUniformMat4("projectionMatrix", projectionPixels);
        
        shader.setUniform1i("sdfChannel", 3);   // alpha
        shader.setUniform1i("invertSdf", 0);    // based on your atlas (likely 1)
        
        // Core colors
        shader.setUniform4f("textColor", textColor);
        shader.setUniform4f("outlineColor", outlineColor);
        shader.setUniform1f("outlineWidth", outlineWidth);

 

        // Shadow
        shader.setUniform4f("shadowColor", shadowColor);
        shader.setUniform2f("shadowOffsetPx", shadowOffsetPx);
        shader.setUniform1f("shadowSoftness", shadowSoftness);

        // Glow
        shader.setUniform4f("glowColor", glowColor);
        shader.setUniform1f("glowStrength", glowStrength);
        shader.setUniform1f("glowRadius", glowRadius);

        // Tint gradient
        shader.setUniform4f("tintTop", tintTop);
        shader.setUniform4f("tintBottom", tintBottom);
        shader.setUniform1f("tintMix", tintMix);

        // Animation/effects
        shader.setUniform1f("time", timeSeconds);
        shader.setUniform2f("screenSize", new Vector2f(screenWidth, screenHeight));
        shader.setUniform1f("waveAmplitudePx", waveAmplitudePx);
        shader.setUniform1f("waveFrequency", waveFrequency);
        shader.setUniform1f("jitterPx", jitterPx);
    }

    private void renderLine(String text, float xPx, float yPx, float scale) {
        try (MemoryStack stack = MemoryStack.stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(text.length() * 6 * 4);

            float cursorX = xPx;
            float cursorY = yPx + font.getBase() * scale;

            int quads = 0;

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                Font.Character ch = font.getCharacter(c);
                if (ch == null) continue;

                addCharacterVertices(buffer, cursorX, cursorY, ch, scale, quads);
                cursorX += ch.xadvance * scale;
                quads++;
            }

            if (quads == 0) return;

            updateVBO(buffer);
            drawText(quads);
        }
    }

    private void addCharacterVertices(
            FloatBuffer buffer,
            float cursorX, float cursorY,
            Font.Character ch,
            float scale,
            int quadIndex
    ) {
        float xpos = cursorX + ch.xoffset * scale;
        float ypos = cursorY - (ch.yoffset + ch.height) * scale;
        float w = ch.width * scale;
        float h = ch.height * scale;

        float u1 = (float) ch.x / font.getAtlasWidth();
        float v1 = (float) (ch.y + ch.height) / font.getAtlasHeight();
        float u2 = u1 + (float) ch.width / font.getAtlasWidth();
        float v2 = (float) ch.y / font.getAtlasHeight();

        // Tri 1
        addVertex(buffer, xpos,     ypos + h, u1, v2);
        addVertex(buffer, xpos + w, ypos,     u2, v1);
        addVertex(buffer, xpos,     ypos,     u1, v1);

        // Tri 2
        addVertex(buffer, xpos,     ypos + h, u1, v2);
        addVertex(buffer, xpos + w, ypos + h, u2, v2);
        addVertex(buffer, xpos + w, ypos,     u2, v1);
    }

    private void updateVBO(FloatBuffer buffer) {
        buffer.flip();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);
        glBufferSubData(GL_ARRAY_BUFFER, 0, buffer);
        glBindBuffer(GL_ARRAY_BUFFER, 0);
    }

    private void drawText(int quadCount) {
        glBindVertexArray(vao);
        glDrawArrays(GL_TRIANGLES, 0, quadCount * 6);
        glBindVertexArray(0);
    }

    // --- Public config methods ---

    // Core colors
    public void setTextColor(float r, float g, float b, float a) { 
        textColor.set(r, g, b, a); 
    }
    
    public void setOutlineColor(float r, float g, float b, float a) { 
        outlineColor.set(r, g, b, a); 
    }
    
    public void setOutlineWidth(float width) { 
        this.outlineWidth = Math.max(0.0f, width); 
    }

    // Border
    public void setBorderColor(float r, float g, float b, float a) { 
        borderColor.set(r, g, b, a); 
    }
    
    public void setBorderWidth(float width) { 
        this.borderWidth = Math.max(0.0f, Math.min(1.0f, width)); 
    }
    
    public void setBorderSoftness(float softness) {
        this.borderSoftness = Math.max(0.0f, Math.min(2.0f, softness));
    }
    
    public void setBorder(boolean enabled, float width, float softness, float r, float g, float b, float a) {
        borderWidth = enabled ? width : 0.0f;
        borderSoftness = softness;
        borderColor.set(r, g, b, a);
    }

    // Shadow
    public void setShadow(boolean enabled, float offsetXPx, float offsetYPx, float r, float g, float b, float a, float softness) {
        shadowOffsetPx.set(offsetXPx, offsetYPx);
        shadowColor.set(r, g, b, a);
        shadowSoftness = softness;
        if (!enabled) shadowColor.w = 0f;
    }

    // Glow
    public void setGlow(boolean enabled, float strength, float radius, float r, float g, float b, float a) {
        glowStrength = enabled ? strength : 0f;
        glowRadius = radius;
        glowColor.set(r, g, b, a);
    }

    // Tint gradient
    public void setTintGradient(float mix, Vector4f top, Vector4f bottom) {
        tintMix = mix;
        tintTop.set(top);
        tintBottom.set(bottom);
    }

    // Animation
    public void setTimeSeconds(float t) { 
        this.timeSeconds = t; 
    }
    
    public void setWave(float amplitudePx, float frequency) { 
        waveAmplitudePx = amplitudePx; 
        waveFrequency = frequency; 
    }
    
    public void setJitter(float jitterPx) { 
        this.jitterPx = jitterPx; 
    }

    public float calculateStringWidth(String text, float scale) {
        float width = 0;
        for (int i = 0; i < text.length(); i++) {
            Font.Character ch = font.getCharacter(text.charAt(i));
            if (ch != null) width += ch.xadvance * scale;
        }
        return width;
    }

    private Matrix4f getFlatProjectionPixels() {
        return new Matrix4f().ortho2D(0.0f, screenWidth, 0.0f, screenHeight);
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