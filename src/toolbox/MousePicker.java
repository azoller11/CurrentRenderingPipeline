package toolbox;

import org.joml.*;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;

import java.util.List;

import entities.Camera;
import entities.Entity;
import entities.Light;

/**
 * A more advanced MousePicker for dragging Entities/Lights in 3D.
 * 
 * Features:
 * - Dynamic dragging plane (camera-facing or axis-aligned).
 * - Snapping to a grid for precise positioning.
 * - Optional axis locking for constrained movement.
 * - Rotation of objects via right-click dragging using Euler angles.
 */
public class MousePicker {

    private int width, height;          // Window dimensions
    private Camera camera;
    private Matrix4f projectionMatrix; // For unprojecting the mouse

    // Scene references
    private List<Entity> entities;
    private List<Light> lights;

    // Currently picked object
    private Entity pickedEntity = null;
    private Light pickedLight = null;

    // Dragging state
    private boolean isDragging = false;
    private MouseButton draggingButton = MouseButton.NONE;

    // Previous mouse positions for calculating deltas
    private double previousMouseX = 0.0;
    private double previousMouseY = 0.0;

    // Settings for interaction
    private float pickRadius = 5.0f;         // How close the ray must be to pick a light
    private boolean snappingEnabled = false; // Enable/disable snapping
    private float snapGridSize = 1.0f;       // Grid size for snapping
    private boolean useCameraPlane = true;    // Dragging on a camera-facing plane

    // Rotation settings
    private float rotationSpeed = 0.2f;      // Degrees per pixel movement

    // Enum to represent mouse buttons
    private enum MouseButton {
        LEFT,
        RIGHT,
        NONE
    }

    public MousePicker(int width, int height,
                       Camera camera, Matrix4f projectionMatrix,
                       List<Entity> entities, List<Light> lights) {
        this.width = width;
        this.height = height;
        this.camera = camera;
        this.projectionMatrix = new Matrix4f(projectionMatrix); // Make a copy
        this.entities = entities;
        this.lights = lights;
    }

    /**
     * Call this each frame.
     * Handles picking and dragging logic based on mouse input.
     */
    public void update(long window) {
        boolean leftPressed = (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
                == GLFW.GLFW_PRESS);
        boolean rightPressed = (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT)
                == GLFW.GLFW_PRESS);

        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        GLFW.glfwGetCursorPos(window, mouseX, mouseY);

        // Determine which mouse button is pressed
        MouseButton currentButton = MouseButton.NONE;
        if (leftPressed && !rightPressed) {
            currentButton = MouseButton.LEFT;
        } else if (rightPressed && !leftPressed) {
            currentButton = MouseButton.RIGHT;
        }

        if (currentButton != MouseButton.NONE && !isDragging) {
            // Start dragging
            pickObject(mouseX[0], mouseY[0], currentButton);
            if (isDragging) {
                draggingButton = currentButton;
                previousMouseX = mouseX[0];
                previousMouseY = mouseY[0];
            }
        } else if (!leftPressed && !rightPressed && isDragging) {
            // Stop dragging when both buttons are released
            isDragging = false;
            draggingButton = MouseButton.NONE;
            pickedEntity = null;
            pickedLight = null;
        } else if (isDragging && draggingButton != MouseButton.NONE) {
            // Continue dragging based on the button type
            if (draggingButton == MouseButton.LEFT) {
                dragObject(mouseX[0], mouseY[0]);
            } else if (draggingButton == MouseButton.RIGHT) {
                rotateObject(mouseX[0], mouseY[0]);
            }
            previousMouseX = mouseX[0];
            previousMouseY = mouseY[0];
        }
    }

    /**
     * Casts a ray into the scene to pick the closest Entity or Light based on the mouse button.
     * @param mouseX X position of the mouse
     * @param mouseY Y position of the mouse
     * @param button  Which mouse button initiated the pick
     */
    private void pickObject(double mouseX, double mouseY, MouseButton button) {
        Ray ray = calculateMouseRay(mouseX, mouseY);

        float closestDist = Float.MAX_VALUE;
        Entity bestEntity = null;

        // Check Entities
        for (Entity e : entities) {
            Vector3f center = e.getPosition(); // Assume bounding sphere
            float radius = e.getMesh().getFurthestPoint();
            float dist = distanceRayToPoint(ray, center);
            if (dist < radius && dist < closestDist) {
                closestDist = dist;
                bestEntity = e;
            }
        }

        // Check Lights
        Light bestLight = null;
        for (Light L : lights) {
            Vector3f pos = L.getPosition();
            float dist = distanceRayToPoint(ray, pos);
            if (dist < pickRadius && dist < closestDist) {
                closestDist = dist;
                bestLight = L;
                bestEntity = null; // Lights take priority
            }
        }

        if (bestEntity != null) {
            pickedEntity = bestEntity;
            pickedLight = null;
            isDragging = true;
            System.out.println("Picked ENTITY at " + bestEntity.getPosition());
        } else if (bestLight != null) {
            pickedLight = bestLight;
            pickedEntity = null;
            isDragging = true;
            System.out.println("Picked LIGHT at " + bestLight.getPosition());
        }
    }

    /**
     * Moves the picked object based on mouse position and plane intersection.
     */
    private void dragObject(double mouseX, double mouseY) {
        if (pickedEntity == null && pickedLight == null) return;

        Ray ray = calculateMouseRay(mouseX, mouseY);

        // Define the dragging plane
        Vector3f planeNormal;
        float planeD;

        if (useCameraPlane) {
            // Camera-facing plane
            Matrix4f viewMatrix = camera.getViewMatrix();
            // Extract the forward vector from the view matrix
            planeNormal = new Vector3f(-viewMatrix.m20(), -viewMatrix.m21(), -viewMatrix.m22()).normalize();
            Vector3f position = pickedEntity != null ? pickedEntity.getPosition() : pickedLight.getPosition();
            planeD = planeNormal.dot(position);
        } else {
            // Default to Y=0 plane
            planeNormal = new Vector3f(0, 1, 0);
            planeD = 0.0f;
        }

        // Intersect ray with plane
        Vector3f intersection = intersectRayPlane(ray, planeNormal, planeD);
        if (intersection != null) {
            // Apply snapping if enabled
            if (snappingEnabled) {
                intersection.x = Math.round(intersection.x / snapGridSize) * snapGridSize;
                intersection.y = Math.round(intersection.y / snapGridSize) * snapGridSize;
                intersection.z = Math.round(intersection.z / snapGridSize) * snapGridSize;
            }

            // Move the picked object
            if (pickedEntity != null) {
                pickedEntity.getPosition().set(intersection);
            }
            if (pickedLight != null) {
                pickedLight.getPosition().set(intersection);
            }
        }
    }

    /**
     * Rotates the picked object based on mouse movement deltas using Euler angles.
     * @param mouseX Current X position of the mouse
     * @param mouseY Current Y position of the mouse
     */
    private void rotateObject(double mouseX, double mouseY) {
        if (pickedEntity == null && pickedLight == null) return;

        // Calculate mouse movement deltas
        float deltaX = (float) (mouseX - previousMouseX);
        float deltaY = (float) (mouseY - previousMouseY);

        // Calculate rotation angles based on mouse movement
        float angleY = deltaX * rotationSpeed; // Yaw (around Y-axis)
        float angleX = deltaY * rotationSpeed; // Pitch (around X-axis)

        // Retrieve current rotation
        Vector3f currentRotation;
        if (pickedEntity != null) {
            currentRotation = new Vector3f(pickedEntity.getRotation());
            // Update rotation angles
            currentRotation.x += angleX;
            currentRotation.y += angleY;
            // Optionally, limit the pitch to avoid flipping
            currentRotation.x = Math.clamp(currentRotation.x, -89.9f, 89.9f);

            // Apply the updated rotation
            if (pickedEntity != null) {
                pickedEntity.setRotation(currentRotation);
            }
        } else { // pickedLight != null
            //currentRotation = new Vector3f(pickedLight.getRotation());
        }

       
        if (pickedLight != null) {
            //pickedLight.setRotation(currentRotation);
        }

        // Optional: Update the dragging plane based on new rotation if necessary
    }

    // -----------------------------------------------------------------------
    // Ray + Intersection Helpers
    // -----------------------------------------------------------------------

    private Ray calculateMouseRay(double mouseX, double mouseY) {
        // Convert to Normalized Device Coordinates (NDC)
        float ndcX = (float) (2.0 * mouseX / width - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / height);

        // Clip space
        Vector4f clipCoords = new Vector4f(ndcX, ndcY, -1.0f, 1.0f);

        // Eye space
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Vector4f eyeCoords = invProj.transform(clipCoords);
        eyeCoords.z = -1.0f;
        eyeCoords.w = 0.0f;

        // World space
        Matrix4f invView = new Matrix4f(camera.getViewMatrix()).invert();
        Vector4f rayWorld4 = invView.transform(eyeCoords);
        Vector3f rayDir = new Vector3f(rayWorld4.x, rayWorld4.y, rayWorld4.z).normalize();

        return new Ray(camera.getPosition(), rayDir);
    }

    private float distanceRayToPoint(Ray ray, Vector3f point) {
        Vector3f originToPoint = new Vector3f(point).sub(ray.origin);
        Vector3f cross = new Vector3f();
        originToPoint.cross(ray.direction, cross);
        return cross.length() / ray.direction.length();
    }

    private Vector3f intersectRayPlane(Ray ray, Vector3f planeNormal, float planeD) {
        float denom = planeNormal.dot(ray.direction);
        if (Math.abs(denom) < 1e-6) return null; // Parallel
        float t = (planeD - planeNormal.dot(ray.origin)) / denom;
        if (t < 0) return null; // Behind camera
        return new Vector3f(ray.origin).fma(t, ray.direction);
    }

    private static class Ray {
        Vector3f origin;
        Vector3f direction;

        Ray(Vector3f o, Vector3f d) {
            origin = new Vector3f(o);
            direction = new Vector3f(d);
        }
    }
}
