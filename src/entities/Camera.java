package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import settings.EngineSettings;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Vector3f position;
    private float yaw;   // around Y
    private float pitch; // around X

    private Vector3f front   = new Vector3f();
    private Vector3f right   = new Vector3f();
    private Vector3f up      = new Vector3f();
    private final Vector3f worldUp = new Vector3f(0,1,0);

    // movement & mouse settings
    private float moveSpeed = 16.5f;
    private float mouseSensitivity = 0.1f;

    // track last mouse pos
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;

    public Camera(Vector3f startPos, float startYaw, float startPitch) {
        this.position = new Vector3f(startPos);
        this.yaw = startYaw;
        this.pitch = startPitch;
        updateVectors();
    }

    // Call every frame to handle WASD + mouse input
    public void handleInput(long window, float dt) {
        float velocity = moveSpeed * dt;

        // WASD
        if (glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS) {
            position.fma(velocity, front);
        }
        if (glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS) {
            position.fma(-velocity, front);
        }
        if (glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS) {
            position.fma(-velocity, right);
        }
        if (glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS) {
            position.fma(velocity, right);
        }
        // optional vertical
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            position.y += velocity;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            position.y -= velocity;
        }

        // Mouse movement => yaw/pitch
        // Hide/lock mouse
        if (EngineSettings.grabMouse)
        	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        else
        	glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        
        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);
        if (firstMouse) {
            lastMouseX = mx[0];
            lastMouseY = my[0];
            firstMouse = false;
        }
        float offsetX = (float)(mx[0] - lastMouseX) * mouseSensitivity;
        float offsetY = (float)(lastMouseY - my[0]) * mouseSensitivity; 
        lastMouseX = mx[0];
        lastMouseY = my[0];

        yaw   += offsetX;
        pitch += offsetY;

        if (pitch >  89f) pitch =  89f;
        if (pitch < -89f) pitch = -89f;

        if (EngineSettings.grabMouse)
        	updateVectors();
        
        //System.out.println(position);
    }

    // Recompute front/right/up from yaw & pitch
    private void updateVectors() {
        float radYaw   = (float)Math.toRadians(yaw);
        float radPitch = (float)Math.toRadians(pitch);

        front.x = (float)(Math.cos(radPitch)*Math.cos(radYaw));
        front.y = (float)(Math.sin(radPitch));
        front.z = (float)(Math.cos(radPitch)*Math.sin(radYaw));
        front.normalize();

        front.cross(worldUp, right).normalize();
        right.cross(front, up).normalize();
    }

    public Matrix4f getViewMatrix() {
        return new Matrix4f().lookAt(
            position,
            new Vector3f(position).add(front),
            up
        );
    }

    public Vector3f getPosition() { return position; }
}
