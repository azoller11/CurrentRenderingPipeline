package gui;

import org.lwjgl.glfw.GLFW;
import static org.lwjgl.glfw.GLFW.*;

import org.joml.Vector4f;

public class GuiButton extends GuiTexture {
    private Runnable onClick;
    private boolean isHovered;
    private float brightness = 1.0f; // Default full brightness
    // New field to track previous mouse button state
    private boolean wasPressed = false; 

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
        brightness = isHovered ? 0.35f : 1.0f;

        // Get the current mouse button state
        boolean currentPressed = glfwGetMouseButton(glfwGetCurrentContext(), GLFW_MOUSE_BUTTON_LEFT) == GLFW_PRESS;
        
        // Run the click action only when the button is hovered,
        // currently pressed, but was not pressed in the previous check.
        if (isHovered && currentPressed && !wasPressed) {
            onClick.run();
        }

        // Update the state for the next check
        wasPressed = currentPressed;
    }

    public float getBrightness() {
        return brightness;
    }
}
