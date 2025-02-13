package toolbox;

import org.joml.*;
import org.joml.Math;
import org.lwjgl.glfw.GLFW;

import debugRenderer.DebugRenderer;

import java.util.List;
import java.util.Stack;

import entities.Camera;
import entities.Entity;
import entities.Light;
import settings.EngineSettings;

/**
 * A more advanced MousePicker for dragging and rotating Entities/Lights in 3D.
 *
 * Features:
 * - Dynamic dragging plane (camera-facing or axis-aligned) with axis-locking.
 * - Rotation with optional axis-locking.
 * - Snapping to a grid for precise positioning.
 * - Debug drawing of the active drag constraint (axis/plane) using lines,
 *   and rotation constraints using debug spheres.
 * - Undo/Redo functionality (Ctrl+Z/Ctrl+Y) for reverting object movements.
 */
public class MousePicker {

    private int width, height;          // Window dimensions.
    private Camera camera;
    private Matrix4f projectionMatrix;  // For unprojecting the mouse.

    // Scene references.
    private List<Entity> entities;
    private List<Light> lights;

    // Undo/Redo stacks.
    private Stack<TransformState> undoStack = new Stack<>();
    private Stack<TransformState> redoStack = new Stack<>();

    // For key debouncing.
    private boolean undoKeyWasDown = false;
    private boolean redoKeyWasDown = false;

    // Dragging state.
    private boolean isDragging = false;
    private MouseButton draggingButton = MouseButton.NONE;

    // Previous mouse positions (for calculating deltas).
    private double previousMouseX = 0.0;
    private double previousMouseY = 0.0;
    
    // Record the object's position at the start of the drag (for translation locking).
    private Vector3f dragStartPos = null;
    
    // Drag constraint enum (for translation).
    public enum DragConstraint {
        FREE,
        LOCK_X,
        LOCK_Y,
        LOCK_Z,
        LOCK_XY, // free axis: Z (movement on XY plane)
        LOCK_XZ, // free axis: Y (movement on XZ plane)
        LOCK_YZ  // free axis: X (movement on YZ plane)
    }
    private DragConstraint currentDragConstraint = DragConstraint.FREE;
    
    // Rotation constraint enum.
    public enum RotationConstraint {
        FREE,
        LOCK_X,
        LOCK_Y,
        LOCK_Z
    }
    private RotationConstraint currentRotationConstraint = RotationConstraint.FREE;

    // Settings for interaction.
    private float pickRadius = 5.0f;         // How close the ray must be to pick a light.
    private boolean snappingEnabled = false; // Enable/disable snapping.
    private float snapGridSize = 1.0f;         // Grid size for snapping.
    private boolean useCameraPlane = true;     // Dragging on a camera-facing plane.

    // Rotation settings.
    private float rotationSpeed = 0.2f;      // Degrees per pixel movement.

    // Enum to represent mouse buttons.
    private enum MouseButton {
        LEFT,
        RIGHT,
        NONE
    }
    
    /**
     * Inner class to store an object's transform state (position and rotation)
     * for undo/redo operations.
     * Note: The target will be either an Entity or a Light.
     */
    private class TransformState {
        public Object target;
        public Vector3f position;
        public Vector3f rotation;
        
        public TransformState(Object target, Vector3f pos, Vector3f rot) {
            this.target = target;
            // Create copies so further changes don’t affect our stored state.
            this.position = new Vector3f(pos);
            this.rotation = new Vector3f(rot);
        }
    }

    public MousePicker(int width, int height,
                       Camera camera, Matrix4f projectionMatrix,
                       List<Entity> entities, List<Light> lights) {
        this.width = width;
        this.height = height;
        this.camera = camera;
        this.projectionMatrix = new Matrix4f(projectionMatrix); // Make a copy.
        this.entities = entities;
        this.lights = lights;
    }
    
    /**
     * Public getter for the current translation drag constraint.
     */
    public DragConstraint getCurrentDragConstraint() {
        return currentDragConstraint;
    }
    
    /**
     * Public getter for the current rotation constraint.
     */
    public RotationConstraint getCurrentRotationConstraint() {
        return currentRotationConstraint;
    }
    
    /**
     * (Optional) Public getter for the drag start position.
     * Useful for external debug drawing.
     */
    public Vector3f getDragStartPos() {
        return dragStartPos;
    }
    
    /**
     * Draws debug helper lines for translation based on the current drag constraint.
     */
    public void drawDebug(DebugRenderer debugRenderer) {
        Vector3f pos = null;
        if (EngineSettings.SelectedEntity != null) {
            pos = EngineSettings.SelectedEntity.getPosition();
        } else if (EngineSettings.SelectedLight != null) {
            pos = EngineSettings.SelectedLight.getPosition();
        } else {
            return;
        }
        
        float debugLength = 10.0f;
        if (EngineSettings.SelectedEntity != null) {
            float objectRadius = EngineSettings.SelectedEntity.getMesh().getFurthestPoint() * EngineSettings.SelectedEntity.getScale();
            debugLength = 200.0f * objectRadius;
        }
        
        switch(currentDragConstraint) {
            case LOCK_X:
                drawAxis(debugRenderer, pos, new Vector3f(1, 0, 0), debugLength, new Vector3f(1, 0, 0));
                break;
            case LOCK_Y:
                drawAxis(debugRenderer, pos, new Vector3f(0, 1, 0), debugLength, new Vector3f(0, 1, 0));
                break;
            case LOCK_Z:
                drawAxis(debugRenderer, pos, new Vector3f(0, 0, 1), debugLength, new Vector3f(0, 0, 1));
                break;
            case LOCK_XY:
                drawAxis(debugRenderer, pos, new Vector3f(1, 0, 0), debugLength, new Vector3f(1, 0, 0));
                drawAxis(debugRenderer, pos, new Vector3f(0, 1, 0), debugLength, new Vector3f(0, 1, 0));
                break;
            case LOCK_XZ:
                drawAxis(debugRenderer, pos, new Vector3f(1, 0, 0), debugLength, new Vector3f(1, 0, 0));
                drawAxis(debugRenderer, pos, new Vector3f(0, 0, 1), debugLength, new Vector3f(0, 0, 1));
                break;
            case LOCK_YZ:
                drawAxis(debugRenderer, pos, new Vector3f(0, 1, 0), debugLength, new Vector3f(0, 1, 0));
                drawAxis(debugRenderer, pos, new Vector3f(0, 0, 1), debugLength, new Vector3f(0, 0, 1));
                break;
            case FREE:
            default:
                drawAxis(debugRenderer, pos, new Vector3f(1, 0, 0), debugLength / 2, new Vector3f(1, 1, 1));
                drawAxis(debugRenderer, pos, new Vector3f(0, 1, 0), debugLength / 2, new Vector3f(1, 1, 1));
                drawAxis(debugRenderer, pos, new Vector3f(0, 0, 1), debugLength / 2, new Vector3f(1, 1, 1));
                break;
        }
    }
    
    /**
     * Helper method that draws a line along the given axis.
     */
    private void drawAxis(DebugRenderer renderer, Vector3f pos, Vector3f axis, float length, Vector3f color) {
        Vector3f start = new Vector3f(pos).fma(-length, axis);
        Vector3f end   = new Vector3f(pos).fma(length, axis);
        renderer.addLine(start, end, color);
    }
    
    /**
     * Draws a debug sphere to indicate the rotation constraint.
     */
    public void drawRotationDebug(DebugRenderer debugRenderer) {
        Vector3f pos = null;
        if (EngineSettings.SelectedEntity != null) {
            pos = EngineSettings.SelectedEntity.getPosition();
        } else if (EngineSettings.SelectedLight  != null) {
            pos = EngineSettings.SelectedLight.getPosition();
        } else {
            return;
        }
        
        float debugRadius = 10.0f;
        if (EngineSettings.SelectedEntity != null) {
            float objectRadius = EngineSettings.SelectedEntity.getMesh().getFurthestPoint() * EngineSettings.SelectedEntity.getScale();
            debugRadius = 1.0f * objectRadius;
        }
        
        Vector3f sphereColor;
        switch(currentRotationConstraint) {
            case LOCK_X: sphereColor = new Vector3f(1, 0, 0); break;
            case LOCK_Y: sphereColor = new Vector3f(0, 1, 0); break;
            case LOCK_Z: sphereColor = new Vector3f(0, 0, 1); break;
            default:     sphereColor = new Vector3f(1, 1, 1); break;
        }
        if (draggingButton == MouseButton.RIGHT) {
            debugRenderer.addSphere(new Vector3f(pos), debugRadius, sphereColor);
        }
    }
    
    /**
     * Call this each frame.
     * Handles picking, dragging (translation), rotation, and undo/redo based on mouse and keyboard input.
     */
    public void update(long window) {
        // First, check for undo/redo key commands.
        handleUndoRedo(window);
        
        boolean leftPressed = (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS);
        boolean rightPressed = (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS);

        double[] mouseX = new double[1];
        double[] mouseY = new double[1];
        GLFW.glfwGetCursorPos(window, mouseX, mouseY);
        
        // Determine which mouse button is pressed.
        MouseButton currentButton = MouseButton.NONE;
        if (leftPressed && !rightPressed) {
            currentButton = MouseButton.LEFT;
        } else if (rightPressed && !leftPressed) {
            currentButton = MouseButton.RIGHT;
        }

        if (currentButton != MouseButton.NONE && !isDragging) {
            // Start dragging/rotating.
            pickObject(mouseX[0], mouseY[0], currentButton);
            if (isDragging) {
                // Capture initial state for undo using the target reference.
                if (EngineSettings.SelectedEntity != null) {
                    undoStack.push(new TransformState(EngineSettings.SelectedEntity,
                        EngineSettings.SelectedEntity.getPosition(),
                        EngineSettings.SelectedEntity.getRotation()));
                } else if (EngineSettings.SelectedLight != null) {
                    // For lights, we only use position (rotation is not used).
                    undoStack.push(new TransformState(EngineSettings.SelectedLight,
                        EngineSettings.SelectedLight.getPosition(),
                        new Vector3f(0, 0, 0)));
                }
                // Clear the redo stack because a new action has begun.
                redoStack.clear();
                
                draggingButton = currentButton;
                previousMouseX = mouseX[0];
                previousMouseY = mouseY[0];
                dragStartPos = new Vector3f(
                    (EngineSettings.SelectedEntity != null) ? EngineSettings.SelectedEntity.getPosition() : EngineSettings.SelectedLight.getPosition()
                );
            }
        } else if (!leftPressed && !rightPressed && isDragging) {
            // Stop dragging/rotating.
            isDragging = false;
            draggingButton = MouseButton.NONE;
            // Optionally, you can keep the object selected for undo/redo.
            // For this example, we re-select the object in handleUndoRedo if needed.
            dragStartPos = null;
            currentDragConstraint = DragConstraint.FREE;
            currentRotationConstraint = RotationConstraint.FREE;
        } else if (isDragging && draggingButton != MouseButton.NONE) {
            // Continue dragging or rotating based on the button type.
            if (draggingButton == MouseButton.LEFT) {
                dragObject(window, mouseX[0], mouseY[0]);
            } else if (draggingButton == MouseButton.RIGHT) {
                rotateObject(window, mouseX[0], mouseY[0]);
            }
            previousMouseX = mouseX[0];
            previousMouseY = mouseY[0];
        }
    }
    
    /**
     * Checks for Ctrl+Z (undo) and Ctrl+Y (redo) key combinations.
     * Uses a simple debounce mechanism so that a single press triggers one action.
     */
    private void handleUndoRedo(long window) {
        boolean ctrlPressed = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_LEFT_CONTROL) == GLFW.GLFW_PRESS ||
                               GLFW.glfwGetKey(window, GLFW.GLFW_KEY_RIGHT_CONTROL) == GLFW.GLFW_PRESS);
        boolean zPressed = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS);
        boolean yPressed = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Y) == GLFW.GLFW_PRESS);

        // Handle Undo (Ctrl+Z)
        if (ctrlPressed && zPressed && !undoKeyWasDown) {
            if (!undoStack.isEmpty()) {
                TransformState prevState = undoStack.pop();
                // Before reverting, capture current state for redo.
                if (prevState.target instanceof Entity) {
                    Entity targetEntity = (Entity) prevState.target;
                    redoStack.push(new TransformState(targetEntity,
                        targetEntity.getPosition(),
                        targetEntity.getRotation()));
                    // Apply the previous state.
                    targetEntity.getPosition().set(prevState.position);
                    targetEntity.setRotation(prevState.rotation);
                    // Optionally, re-select the object.
                    EngineSettings.SelectedEntity = targetEntity;
                    EngineSettings.SelectedLight = null;
                } else if (prevState.target instanceof Light) {
                    Light targetLight = (Light) prevState.target;
                    redoStack.push(new TransformState(targetLight,
                        targetLight.getPosition(),
                        new Vector3f(0, 0, 0)));
                    targetLight.getPosition().set(prevState.position);
                    EngineSettings.SelectedLight = targetLight;
                    EngineSettings.SelectedEntity = null;
                }
            }
            undoKeyWasDown = true;
        } else if (!(ctrlPressed && zPressed)) {
            undoKeyWasDown = false;
        }

        // Handle Redo (Ctrl+Y)
        if (ctrlPressed && yPressed && !redoKeyWasDown) {
            if (!redoStack.isEmpty()) {
                TransformState redoState = redoStack.pop();
                if (redoState.target instanceof Entity) {
                    Entity targetEntity = (Entity) redoState.target;
                    undoStack.push(new TransformState(targetEntity,
                        targetEntity.getPosition(),
                        targetEntity.getRotation()));
                    targetEntity.getPosition().set(redoState.position);
                    targetEntity.setRotation(redoState.rotation);
                    EngineSettings.SelectedEntity = targetEntity;
                    EngineSettings.SelectedLight = null;
                } else if (redoState.target instanceof Light) {
                    Light targetLight = (Light) redoState.target;
                    undoStack.push(new TransformState(targetLight,
                        targetLight.getPosition(),
                        new Vector3f(0, 0, 0)));
                    targetLight.getPosition().set(redoState.position);
                    EngineSettings.SelectedLight = targetLight;
                    EngineSettings.SelectedEntity = null;
                }
            }
            redoKeyWasDown = true;
        } else if (!(ctrlPressed && yPressed)) {
            redoKeyWasDown = false;
        }
    }
    
    /**
     * Casts a ray into the scene to pick the closest Entity or Light based on the mouse position.
     */
    private void pickObject(double mouseX, double mouseY, MouseButton button) {
        Ray ray = calculateMouseRay(mouseX, mouseY);
        float closestDist = Float.MAX_VALUE;
        Entity bestEntity = null;
        
        if (EngineSettings.ObjectPicker) {
            for (Entity e : entities) {
                Vector3f center = e.getPosition();
                float effectiveScale = e.getScale();
                float radius = e.getMesh().getFurthestPoint() * effectiveScale;
                float dist = distanceRayToPoint(ray, center);
                if (dist < radius && dist < closestDist) {
                    closestDist = dist;
                    bestEntity = e;
                }
            }
        }
        
        Light bestLight = null;
        if (EngineSettings.LightPicker) {
            for (Light L : lights) {
                Vector3f pos = L.getPosition();
                float dist = distanceRayToPoint(ray, pos);
                if (dist < pickRadius && dist < closestDist) {
                    closestDist = dist;
                    bestLight = L;
                    bestEntity = null;
                }
            }
        }
        
        if (bestEntity != null) {
            EngineSettings.SelectedEntity  = bestEntity;
            EngineSettings.SelectedLight  = null;
            isDragging = true;
            System.out.println("Picked ENTITY at " + bestEntity.getPosition());
        } else if (bestLight != null) {
            EngineSettings.SelectedLight  = bestLight;
            EngineSettings.SelectedEntity = null;
            isDragging = true;
            System.out.println("Picked LIGHT at " + bestLight.getPosition());
        }
    }
    
    /**
     * Moves the picked object (translation) based on mouse position.
     */
    private void dragObject(long window, double mouseX, double mouseY) {
        if (EngineSettings.SelectedEntity == null && EngineSettings.SelectedLight == null) return;
        
        Vector3f currentPos = new Vector3f(
            (EngineSettings.SelectedEntity != null) ? 
                EngineSettings.SelectedEntity.getPosition() : 
                EngineSettings.SelectedLight.getPosition()
        );
    
        boolean lockX = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_X) == GLFW.GLFW_PRESS);
        boolean lockY = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Y) == GLFW.GLFW_PRESS);
        boolean lockZ = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS);
        int lockedCount = (lockX ? 1 : 0) + (lockY ? 1 : 0) + (lockZ ? 1 : 0);
    
        if (lockedCount == 1) {
            if (lockX) currentDragConstraint = DragConstraint.LOCK_X;
            else if (lockY) currentDragConstraint = DragConstraint.LOCK_Y;
            else if (lockZ) currentDragConstraint = DragConstraint.LOCK_Z;
        } else if (lockedCount == 2) {
            if (!lockX) currentDragConstraint = DragConstraint.LOCK_YZ;
            else if (!lockY) currentDragConstraint = DragConstraint.LOCK_XZ;
            else if (!lockZ) currentDragConstraint = DragConstraint.LOCK_XY;
        } else {
            currentDragConstraint = DragConstraint.FREE;
        }
    
        Vector3f newPos = new Vector3f();
        Ray ray = calculateMouseRay(mouseX, mouseY);
    
        if (lockedCount == 1) {
            Vector3f axis = new Vector3f();
            if (lockX) axis.set(1, 0, 0);
            else if (lockY) axis.set(0, 1, 0);
            else if (lockZ) axis.set(0, 0, 1);
            axis.normalize();
            newPos = computeClosestPointOnAxis(currentPos, axis, ray);
        } else if (lockedCount == 2) {
            Vector3f planeNormal = new Vector3f();
            if (!lockX) planeNormal.set(1, 0, 0);
            else if (!lockY) planeNormal.set(0, 1, 0);
            else if (!lockZ) planeNormal.set(0, 0, 1);
            float planeD = planeNormal.dot(currentPos);
            newPos = intersectRayPlane(ray, planeNormal, planeD);
            if (newPos == null) {
                newPos = new Vector3f(currentPos);
            }
        } else {
            newPos = getFreeIntersection(mouseX, mouseY);
            if (newPos == null) return;
        }
    
        if (snappingEnabled) {
            newPos.x = Math.round(newPos.x / snapGridSize) * snapGridSize;
            newPos.y = Math.round(newPos.y / snapGridSize) * snapGridSize;
            newPos.z = Math.round(newPos.z / snapGridSize) * snapGridSize;
        }
    
        if (lockedCount >= 1) {
            Vector3f finalPos = new Vector3f(currentPos);
            if (lockX) finalPos.x = newPos.x;
            if (lockY) finalPos.y = newPos.y;
            if (lockZ) finalPos.z = newPos.z;
            newPos.set(finalPos);
        }
    
        if (EngineSettings.SelectedEntity != null) {
            EngineSettings.SelectedEntity.getPosition().set(newPos);
        }
        if (EngineSettings.SelectedLight != null) {
            EngineSettings.SelectedLight.getPosition().set(newPos);
        }
    }
    
    /**
     * Rotates the picked object based on mouse movement.
     */
    private void rotateObject(long window, double mouseX, double mouseY) {
        if (EngineSettings.SelectedEntity == null && EngineSettings.SelectedLight  == null) return;
        float deltaX = (float) (mouseX - previousMouseX);
        float deltaY = (float) (mouseY - previousMouseY);
        
        boolean lockRotX = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_X) == GLFW.GLFW_PRESS);
        boolean lockRotY = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Y) == GLFW.GLFW_PRESS);
        boolean lockRotZ = (GLFW.glfwGetKey(window, GLFW.GLFW_KEY_Z) == GLFW.GLFW_PRESS);
        int rotLockCount = (lockRotX ? 1 : 0) + (lockRotY ? 1 : 0) + (lockRotZ ? 1 : 0);
        
        if (EngineSettings.SelectedEntity != null) {
            Vector3f currentRotation = new Vector3f(EngineSettings.SelectedEntity.getRotation());
            if (rotLockCount == 1) {
                if (lockRotX) {
                    currentRotationConstraint = RotationConstraint.LOCK_X;
                    currentRotation.x += deltaY * rotationSpeed;
                } else if (lockRotY) {
                    currentRotationConstraint = RotationConstraint.LOCK_Y;
                    currentRotation.y += deltaX * rotationSpeed;
                } else if (lockRotZ) {
                    currentRotationConstraint = RotationConstraint.LOCK_Z;
                    currentRotation.z += deltaX * rotationSpeed;
                }
            } else {
                currentRotationConstraint = RotationConstraint.FREE;
                currentRotation.x += deltaY * rotationSpeed;
                currentRotation.y += deltaX * rotationSpeed;
            }
            currentRotation.x = Math.clamp(currentRotation.x, -89.9f, 89.9f);
            EngineSettings.SelectedEntity.setRotation(currentRotation);
        }
    }
    
    // -----------------------------------------------------------------------
    // Ray and Intersection Helpers
    // -----------------------------------------------------------------------
    
    private Ray calculateMouseRay(double mouseX, double mouseY) {
        float ndcX = (float) (2.0 * mouseX / width - 1.0);
        float ndcY = (float) (1.0 - 2.0 * mouseY / height);
        Vector4f clipCoords = new Vector4f(ndcX, ndcY, -1.0f, 1.0f);
        Matrix4f invProj = new Matrix4f(projectionMatrix).invert();
        Vector4f eyeCoords = invProj.transform(clipCoords);
        eyeCoords.z = -1.0f;
        eyeCoords.w = 0.0f;
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
        if (Math.abs(denom) < 1e-6) return null;
        float t = (planeD - planeNormal.dot(ray.origin)) / denom;
        if (t < 0) return null;
        return new Vector3f(ray.origin).fma(t, ray.direction);
    }
    
    private Vector3f getFreeIntersection(double mouseX, double mouseY) {
        Ray ray = calculateMouseRay(mouseX, mouseY);
        Vector3f planeNormal;
        float planeD;
        if (useCameraPlane) {
            Matrix4f viewMatrix = camera.getViewMatrix();
            planeNormal = new Vector3f(-viewMatrix.m20(), -viewMatrix.m21(), -viewMatrix.m22()).normalize();
            Vector3f position = (EngineSettings.SelectedEntity != null) ? EngineSettings.SelectedEntity.getPosition() : EngineSettings.SelectedLight.getPosition();
            planeD = planeNormal.dot(position);
        } else {
            planeNormal = new Vector3f(0, 1, 0);
            planeD = 0.0f;
        }
        return intersectRayPlane(ray, planeNormal, planeD);
    }
    
    private Vector3f computeClosestPointOnAxis(Vector3f linePoint, Vector3f axis, Ray ray) {
        Vector3f r0 = ray.origin;
        Vector3f r = ray.direction;
        float aDotR = axis.dot(r);
        float denom = 1 - aDotR * aDotR;
        if (Math.abs(denom) < 1e-6f) return new Vector3f(linePoint);
        Vector3f diff = new Vector3f(r0).sub(linePoint);
        Vector3f temp = new Vector3f(axis).sub(new Vector3f(r).mul(aDotR));
        float numerator = diff.dot(temp);
        float t = numerator / denom;
        return new Vector3f(linePoint).fma(t, axis);
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
