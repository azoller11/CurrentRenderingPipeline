package entities;

import static org.lwjgl.glfw.GLFW.*;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Vector3f;

import physics.PhysicsManager;
import settings.EngineSettings;
import terrain.Terrain;

public class Player {

    public float x, y, z;
    public Camera camera;
    public Entity playerModel;

    private RigidBody physicsBody;
    private PhysicsManager physicsManager;

    // ----------------------
    // Movement tuning
    // ----------------------
    private float moveSpeed = 1000 * 4;
    private float airControlMultiplier = 0.6f;
    private float mass = 17f;

    // ----------------------
    // Terrain grounding
    // ----------------------
    private static final float TERRAIN_CLEARANCE = 31.0f;
    private boolean terrainGrounded = false;
    private boolean physicsGrounded = false;

    // ----------------------
    // Camera
    // ----------------------
    private float eyeHeight = 25f;

    private float standingEyeHeight = 25f;
    private float crouchEyeHeight   = 8.0f;

    // ----------------------
    // Camera bobbing
    // ----------------------
    private float bobTimer = 0f;
    private float bobOffset = 0f;

    private float walkBobAmount = 0.6f;
    private float runBobAmount  = 1.4f;
    private float crouchBobAmount = 0.25f;

    private float walkBobSpeed = 8f;
    private float runBobSpeed  = 14f;

    // Jump / land impulses
    private float verticalImpulse = 0f;
    private float impulseVelocity = 0f;

    // ----------------------
    // Crouch
    // ----------------------
    private boolean crouching = false;
    private float crouchSpeedMultiplier = 0.45f;

    // ----------------------
    // Constructor
    // ----------------------
    public Player(float x, float y, float z, Camera camera, Entity playerModel, PhysicsManager physicsManager) {
        this.x = x;
        this.y = y;
        this.z = z;
        this.camera = camera;
        this.playerModel = playerModel;
        this.physicsManager = physicsManager;

        this.physicsBody = physicsManager.addPlayer(playerModel, mass);

        if (this.physicsBody != null) {
            playerModel.setCollisionType("DYNAMIC");
            playerModel.setMass(mass);
        }
    }

    public Player(Entity playerModel, PhysicsManager physicsManager, Camera camera) {
        this(
            playerModel.getPosition().x,
            playerModel.getPosition().y,
            playerModel.getPosition().z,
            camera,
            playerModel,
            physicsManager
        );
    }

    // =====================================================
    // UPDATE
    // =====================================================
    public void updatePlayer(long window, float dt, Terrain terrain) {
        if (EngineSettings.pause || physicsBody == null || camera == null || playerModel == null) return;

        // -------------------------------------------------
        // INPUT
        // -------------------------------------------------
        boolean forward = glfwGetKey(window, GLFW_KEY_W) == GLFW_PRESS;
        boolean back    = glfwGetKey(window, GLFW_KEY_S) == GLFW_PRESS;
        boolean left    = glfwGetKey(window, GLFW_KEY_A) == GLFW_PRESS;
        boolean right   = glfwGetKey(window, GLFW_KEY_D) == GLFW_PRESS;
        boolean jumpKey = glfwGetKey(window, GLFW_KEY_SPACE) == GLFW_PRESS;
        boolean sprint  = glfwGetKey(window, GLFW_KEY_LEFT_SHIFT) == GLFW_PRESS;
        boolean crouchKey = glfwGetKey(window, GLFW_KEY_LEFT_CONTROL) == GLFW_PRESS;

        // -------------------------------------------------
        // SPEED
        // -------------------------------------------------
        float speedMultiplier = crouching ? crouchSpeedMultiplier : (sprint ? 1.8f : 1f);
        float speed = moveSpeed * speedMultiplier * dt;

        // -------------------------------------------------
        // MOVE DIRECTION (camera-relative)
        // -------------------------------------------------
        org.joml.Vector3f camForward = new org.joml.Vector3f(camera.getDirection()).setComponent(1, 0).normalize();
        org.joml.Vector3f camRight   = new org.joml.Vector3f(camera.getRight()).setComponent(1, 0).normalize();

        org.joml.Vector3f desiredDir = new org.joml.Vector3f();
        if (forward) desiredDir.add(camForward);
        if (back)    desiredDir.sub(camForward);
        if (right)   desiredDir.add(camRight);
        if (left)    desiredDir.sub(camRight);

        boolean hasInput = desiredDir.lengthSquared() > 0.0001f;
        if (hasInput) desiredDir.normalize();

        Vector3f vel = physicsBody.getLinearVelocity(new Vector3f());

        // -------------------------------------------------
        // APPLY MOVEMENT
        // -------------------------------------------------
        boolean grounded = terrainGrounded || physicsGrounded;
        float control = grounded ? 1.0f : airControlMultiplier;

        if (hasInput) {
            Vector3f desiredVel = new Vector3f(desiredDir.x * speed, 0, desiredDir.z * speed);
            Vector3f currentVel = new Vector3f(vel.x, 0, vel.z);

            Vector3f accel = new Vector3f();
            accel.sub(desiredVel, currentVel);
            accel.scale(control * 10f);

            physicsBody.applyCentralForce(new Vector3f(
                accel.x * mass,
                0,
                accel.z * mass
            ));
        } else if (grounded) {
            physicsBody.setLinearVelocity(new Vector3f(0, vel.y, 0));
        }

        // -------------------------------------------------
        // JUMP
        // -------------------------------------------------
        if (jumpKey && grounded && !crouching) {
            Vector3f v = physicsBody.getLinearVelocity(new Vector3f());
            v.y = 120f;
            physicsBody.setLinearVelocity(v);

            terrainGrounded = false;
            physicsGrounded = false;
            impulseVelocity = 3.5f;
        }

        // -------------------------------------------------
        // CROUCH
        // -------------------------------------------------
        if (sprint && grounded) crouching = false;
        else if (crouchKey && grounded) crouching = true;
        else if (!crouchKey) crouching = false;

        // -------------------------------------------------
        // LOCK ROTATION
        // -------------------------------------------------
        physicsBody.setAngularVelocity(new Vector3f(0, 0, 0));
        physicsBody.setAngularFactor(0f);

        // -------------------------------------------------
        // READ TRANSFORM
        // -------------------------------------------------
        Transform t = new Transform();
        physicsBody.getMotionState().getWorldTransform(t);
        Vector3f origin = t.origin;

        // -------------------------------------------------
        // TERRAIN CLAMP (AUTHORITATIVE)
        // -------------------------------------------------
        float terrainY = terrain.getHeightOfTerrain(origin.x, origin.z);
        float minY = terrainY + TERRAIN_CLEARANCE;
        float separation = origin.y - terrainY;
        
        //System.out.println("separation: " + separation + " TERRAIN_CLEARANCE: " + TERRAIN_CLEARANCE);

        terrainGrounded = separation <= TERRAIN_CLEARANCE + 1.0f;
        

        if (terrainGrounded) {
            origin.y = minY;

            Vector3f v = physicsBody.getLinearVelocity(new Vector3f());
            if (v.y < 0f) v.y = 0f;
            physicsBody.setLinearVelocity(v);

            t.origin.set(origin);
            physicsBody.setWorldTransform(t);
            physicsBody.getMotionState().setWorldTransform(t);
        }

        // -------------------------------------------------
        // PHYSICS GROUND (NON-TERRAIN)
        // -------------------------------------------------
        physicsGrounded = !terrainGrounded && Math.abs(vel.y) < 0.4f;

        grounded = terrainGrounded || physicsGrounded;

        // -------------------------------------------------
        // CAMERA BOB
        // -------------------------------------------------
        float horizontalSpeed = (float)Math.sqrt(vel.x * vel.x + vel.z * vel.z);
        boolean moving = horizontalSpeed > 0.1f && grounded;

        float targetBobAmount = moving
                ? (crouching ? crouchBobAmount : (sprint ? runBobAmount : walkBobAmount))
                : 0f;

        float targetBobSpeed = sprint ? runBobSpeed : walkBobSpeed;

        if (moving) bobTimer += dt * targetBobSpeed;
        else bobTimer = 0f;

        bobOffset += ((float)Math.sin(bobTimer) * targetBobAmount - bobOffset) * Math.min(1f, dt * 10f);

        // -------------------------------------------------
        // CAMERA HEIGHT
        // -------------------------------------------------
        float targetEyeHeight = crouching ? crouchEyeHeight : standingEyeHeight;
        eyeHeight += (targetEyeHeight - eyeHeight) * Math.min(1f, dt * 12f);

        float finalCameraY = origin.y + eyeHeight + bobOffset + verticalImpulse;
        camera.getPosition().set(origin.x, finalCameraY, origin.z);

        // -------------------------------------------------
        // WRITE BACK
        // -------------------------------------------------
        //this.x = origin.x;
        //this.y = origin.y;
        //this.z = origin.z;
        //playerModel.setPosition(x, y, z);
    }

    public RigidBody getPhysicsBody() {
        return physicsBody;
    }

    public float getPlayerHeight() {
        return eyeHeight;
    }
}
