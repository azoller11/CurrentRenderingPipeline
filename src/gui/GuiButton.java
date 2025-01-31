package gui;

import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector4f;

public class GuiButton extends GuiTexture {
    private Runnable onClick;
    private boolean isHovered;
    private float brightness = 1.0f; // Default full brightness

    public GuiButton(String filePath, float posX, float posY, float scaleX, float scaleY, Runnable onClick) {
        super(filePath, posX, posY, scaleX, scaleY);
        this.onClick = onClick;
        this.isHovered = false;
    }
    public GuiButton(Vector4f color, float posX, float posY, float scaleX, float scaleY, Runnable onClick) {
        super(color, posX, posY, scaleX, scaleY);
        this.onClick = onClick;
        this.isHovered = false;
    }

    public void checkClick(double mouseX, double mouseY) {
        float minX = getPosX();
        float maxX = getPosX() + getScaleX();
        float minY = getPosY();
        float maxY = getPosY() + getScaleY();

        isHovered = mouseX >= minX && mouseX <= maxX && mouseY >= minY && mouseY <= maxY;

        // Darken when hovered
        brightness = isHovered ? 0.7f : 1.0f;

        if (isHovered && glfwGetMouseButton(GLFW.glfwGetCurrentContext(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS) {
            onClick.run();
        }
    }

    public float getBrightness() {
        return brightness;
    }
}
