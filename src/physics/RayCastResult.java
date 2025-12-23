package physics;

import javax.vecmath.Vector3f;
import com.bulletphysics.dynamics.RigidBody;

public class RayCastResult {
    public boolean hit = false;
    public org.joml.Vector3f hitPoint = new org.joml.Vector3f();
    public org.joml.Vector3f hitNormal = new org.joml.Vector3f();
    public RigidBody hitBody = null;
    public float fraction = 1.0f; // How far along the ray (0-1)
    
    // Helper method to convert to JOML Vector3f
    public org.joml.Vector3f getHitPointJOML() {
        return new org.joml.Vector3f(hitPoint.x, hitPoint.y, hitPoint.z);
    }
    
    // Helper method to convert normal to JOML Vector3f
    public org.joml.Vector3f getHitNormalJOML() {
        return new org.joml.Vector3f(hitNormal.x, hitNormal.y, hitNormal.z);
    }
    
    // Check if the hit body is static (mass = 0)
    public boolean isHitStatic() {
        return hitBody != null && hitBody.getInvMass() == 0.0f;
    }
    
    // Check if the hit body is dynamic (mass > 0)
    public boolean isHitDynamic() {
        return hitBody != null && hitBody.getInvMass() > 0.0f;
    }
    
    // Get the collision group/mask info if available
    public int getHitCollisionFilter() {
        if (hitBody != null) {
            return hitBody.getCollisionFlags();
        }
        return 0;
    }
}