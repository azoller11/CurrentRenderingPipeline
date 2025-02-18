package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;
import settings.EngineSettings;

import static org.lwjgl.glfw.GLFW.*;

public class Camera {

    private Vector3f position;
    private float yaw;   // around Y
    private float pitch; // around X

    private Vector3f front = new Vector3f();
    private Vector3f right = new Vector3f();
    private Vector3f up = new Vector3f();
    private final Vector3f worldUp = new Vector3f(0, 1, 0);

    // Movement & mouse settings
    private float moveSpeed = 50.5f;
    private float mouseSensitivity = 0.2f;

    // Track last mouse pos
    private double lastMouseX;
    private double lastMouseY;
    private boolean firstMouse = true;

    // To detect changes in grabMouse state
    private boolean previousGrabMouse = false;
    
    // Near and far plane values used for frustum calculations.
    // These can be adjusted to match your projection settings.
    private float nearPlane = 1.0f;
    private float farPlane = 10000.0f;

    public Camera(Vector3f startPos, float startYaw, float startPitch) {
        this.position = new Vector3f(startPos);
        this.yaw = startYaw;
        this.pitch = startPitch;
        updateVectors();
    }

    // Call every frame to handle WASD + mouse input
    public void handleInput(long window, float dt) {
        float velocity = moveSpeed * dt;

        // WASD movement
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
        // Optional vertical movement
        if (glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS) {
            position.y += velocity;
        }
        if (glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS) {
            position.y -= velocity;
        }

        // Handle mouse grabbing
        if (EngineSettings.grabMouse) {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_DISABLED);
        } else {
            glfwSetInputMode(window, GLFW_CURSOR, GLFW_CURSOR_NORMAL);
        }

        // Detect if grabMouse state has changed
        if (EngineSettings.grabMouse != previousGrabMouse) {
            firstMouse = true; // Reset mouse handling
            previousGrabMouse = EngineSettings.grabMouse;
        }

        double[] mx = new double[1];
        double[] my = new double[1];
        glfwGetCursorPos(window, mx, my);

        if (firstMouse) {
            lastMouseX = mx[0];
            lastMouseY = my[0];
            firstMouse = false;
        }

        // Only handle mouse movement if grabMouse is enabled
        if (EngineSettings.grabMouse) {
            float offsetX = (float) (mx[0] - lastMouseX) * mouseSensitivity;
            float offsetY = (float) (lastMouseY - my[0]) * mouseSensitivity;
            lastMouseX = mx[0];
            lastMouseY = my[0];

            yaw += offsetX;
            pitch += offsetY;

            // Constrain the pitch
            if (pitch > 89f)
                pitch = 89f;
            if (pitch < -89f)
                pitch = -89f;

            updateVectors();
        }
    }

    // Recompute front/right/up vectors from yaw & pitch
    private void updateVectors() {
        float radYaw = (float) Math.toRadians(yaw);
        float radPitch = (float) Math.toRadians(pitch);

        front.x = (float) (Math.cos(radPitch) * Math.cos(radYaw));
        front.y = (float) (Math.sin(radPitch));
        front.z = (float) (Math.cos(radPitch) * Math.sin(radYaw));
        front.normalize();

        // Recompute right and up vectors
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
    
    // You can use this if you need a "flat" view matrix (for example, in UI rendering)
    public Matrix4f getFlatViewMatrix() {
        return new Matrix4f().identity();
    }

    public Vector3f getPosition() {
        return position;
    }

    public float getYaw() {
        return yaw;
    }

    public void setYaw(float yaw) {
        this.yaw = yaw;
    }

    public float getPitch() {
        return pitch;
    }

    public void setPitch(float pitch) {
        this.pitch = pitch;
    }

    // --- Added Getter Methods ---

    /**
     * Returns a new vector representing the camera's forward direction.
     */
    public Vector3f getDirection() {
        return new Vector3f(front);
    }

    /**
     * Returns the near plane distance.
     */
    public float getNearPlane() {
        return nearPlane;
    }

    /**
     * Returns the far plane distance.
     */
    public float getFarPlane() {
        return farPlane;
    }
    
    /**
     * Optionally, setters for near and far planes in case you want to adjust them:
     */
    public void setNearPlane(float nearPlane) {
        this.nearPlane = nearPlane;
    }

    public void setFarPlane(float farPlane) {
        this.farPlane = farPlane;
    }
    
    public Vector3f getUp() {
        return new Vector3f(up);
    }

    public Vector3f getRight() {
        // If you haven't added one, you can compute it as:
        Vector3f right = new Vector3f();
        getDirection().cross(getUp(), right).normalize();
        return right;
    }
   
    public Vector3f[] getFrustumCorners(Matrix4f projectionMatrix) {
        Vector3f[] corners = new Vector3f[8];
        
        // Compute the combined projection-view matrix
        Matrix4f viewMatrix = getViewMatrix();
        Matrix4f viewProj = new Matrix4f();
        projectionMatrix.mul(viewMatrix, viewProj);
        
        // Invert the matrix so we can transform from clip space to world space.
        Matrix4f invViewProj = new Matrix4f(viewProj).invert();

        // Loop over all combinations of x,y,z in clip space (-1 or 1)
        int i = 0;
        for (int x = -1; x <= 1; x += 2) {
            for (int y = -1; y <= 1; y += 2) {
                for (int z = -1; z <= 1; z += 2) {
                    Vector4f clipSpaceCorner = new Vector4f(x, y, z, 1.0f);
                    Vector4f worldSpaceCorner = new Vector4f();
                    invViewProj.transform(clipSpaceCorner, worldSpaceCorner);
                    // Divide by w to perform perspective divide.
                    worldSpaceCorner.div(worldSpaceCorner.w);
                    corners[i++] = new Vector3f(worldSpaceCorner.x, worldSpaceCorner.y, worldSpaceCorner.z);
                }
            }
        }
        
        return corners;
    }

    /**
     * Computes and returns the center of the camera's view frustum.
     * This is useful for generating a stable reference point when computing
     * the light-space matrix for shadow mapping.
     */
    public Vector3f getFrustumCenter() {
        // Compute the center of the near and far planes along the view direction.
        Vector3f nearCenter = new Vector3f(position).add(new Vector3f(front).mul(nearPlane));
        Vector3f farCenter = new Vector3f(position).add(new Vector3f(front).mul(farPlane));
        // Return the midpoint between nearCenter and farCenter.
        return new Vector3f(nearCenter).lerp(farCenter, 0.5f);
    }
}
