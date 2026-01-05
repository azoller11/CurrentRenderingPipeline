package physics;

import org.joml.Vector3f;
import java.util.Random;

import com.bulletphysics.dynamics.RigidBody;

import particles.ParticleMaster;
import particles.ParticleTexture;

public class HitResult {
    public boolean hit = false;
    public org.joml.Vector3f hitPoint = new Vector3f();
    public org.joml.Vector3f hitNormal = new Vector3f();
    public float distance = 0.0f;
    public float angle = 0.0f; // Angle of impact (in degrees)
    public RigidBody hitBody = null;
    
    // Constructor for no hit
    public HitResult() {}
    
    public Random random = new Random();
    
    // Constructor for hit
    public HitResult(boolean hit, org.joml.Vector3f hitPoint, org.joml.Vector3f hitNormal, float distance, 
                    float angle, RigidBody hitBody) {
        this.hit = hit;
        this.hitPoint = hitPoint;
        this.hitNormal = hitNormal;
        this.distance = distance;
        this.angle = angle;
        this.hitBody = hitBody;
    }
    
    
    
    public void createSmokePuff(
            Vector3f position,
            Vector3f normal,
            Vector3f hitDirection,
            ParticleTexture smokeTexture
    ) {
        // Slight offset from surface to avoid z-fighting
        Vector3f offset = new Vector3f(normal).mul(0.08f);

        org.lwjgl.util.vector.Vector3f spawnPos =
            new org.lwjgl.util.vector.Vector3f(
                position.x + offset.x,
                position.y + offset.y,
                position.z + offset.z
            );

        // -------------------------------
        // Gunshot smoke configuration
        // -------------------------------
        int smokeCount = 5;                 // tight puff
        float baseSpeed = 5.5f;
        float maxSpeed  = 1.0f;              // later particles go faster
        float coneSpread = 0.6f;             // cone angle
        float gravity = 0.015f;

        // Normalize directions
        Vector3f n = new Vector3f(normal).normalize();
        Vector3f bulletDir = hitDirection != null
                ? new Vector3f(hitDirection).normalize()
                : new Vector3f(n);

        for (int i = 0; i < smokeCount; i++) {

            // -----------------------------------------
            // Speed ramps UP as index increases
            // -----------------------------------------
            float t = i / (float)(smokeCount - 1); // 0 → 1
            float speed = baseSpeed + t * (maxSpeed - baseSpeed);

            // -----------------------------------------
            // Direction: mostly away from surface
            // -----------------------------------------
            Vector3f dir = new Vector3f(n)
                    .mul(0.7f)
                    .add(new Vector3f(bulletDir).mul(0.3f));

            // Add cone spread
            dir.add(
                (random.nextFloat() - 0.5f) * coneSpread,
                (random.nextFloat() - 0.5f) * coneSpread,
                (random.nextFloat() - 0.5f) * coneSpread
            ).normalize();

            // Final velocity
            Vector3f velocity = dir.mul(speed);

            // -----------------------------------------
            // Visual shaping
            // -----------------------------------------
            float life = 2.25f + random.nextFloat() * 1.6f;   // quick dissipate
            float scale = 13 + t * 12f + random.nextFloat() * 2f;

            ParticleMaster.createParticle(
                smokeTexture,
                spawnPos,
                new org.lwjgl.util.vector.Vector3f(
                    velocity.x, velocity.y, velocity.z
                ),
                gravity,
                life,
                random.nextFloat() * 360f,
                scale
            );
        }
    }

    
    
    
    
}