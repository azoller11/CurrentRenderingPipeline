package weapons;

import org.lwjgl.util.vector.Vector3f;
import loaders.TextureLoader;
import particles.Particle;
import particles.ParticleMaster;
import particles.ParticleTexture;
import toolbox.Maths;

import java.util.Random;

public class SmokeGrenade {

    private Vector3f position;
    private Vector3f initialDirection;
    private ParticleTexture smokeTexture;
    private Random random = new Random();

    private float age = 0f;
    private float emitAccumulator = 0f;
    private boolean dead = false;

    /* ===============================
       OVERALL LIFE MULTIPLIER
       =============================== */
    private static final float LIFE_SCALE = 2.0f;

    /* ===============================
       TIMING (scaled)
       =============================== */
    private static final float PRIMER_END     = 2f  * LIFE_SCALE;
    private static final float EMISSION_END   = 10f * LIFE_SCALE;
    private static final float FADE_OUT_TIME  = 15f * LIFE_SCALE;
    private static final float MAX_LIFE       = 2000f * LIFE_SCALE;

    public SmokeGrenade(Vector3f position, Vector3f initialDirection) {
        this.position = position;
        this.initialDirection = new Vector3f(initialDirection);
        this.initialDirection.normalise();

        smokeTexture = new ParticleTexture(
            TextureLoader.loadTexture("smoke.png"),
            8,
            false
        );
    }

    public void update(float deltaTime) {

        if (dead) return;

        age += deltaTime;

        if (age > EMISSION_END + FADE_OUT_TIME) {
            dead = true;
            return;
        }

        if (age > EMISSION_END) {
            emitAccumulator = 0f;
            return;
        }

        float emitRate;
        if (age < PRIMER_END)             emitRate = 220f;
        else if (age < 7.5f * LIFE_SCALE) emitRate = 60f;
        else                              emitRate = 18f;

        emitAccumulator += emitRate * deltaTime;
        int spawnCount = (int) emitAccumulator;
        emitAccumulator -= spawnCount;

        for (int i = 0; i < spawnCount; i++) {

            Vector3f spawnPos = new Vector3f(
                position.x + randomRange(-1.0f, 1.0f),
                position.y + randomRange(0.2f, 0.6f),
                position.z + randomRange(-1.0f, 1.0f)
            );

            /* ===============================
               DIRECTION BLENDING
               =============================== */

            // Directional cone
            Vector3f coneDir = randomConeDirection(initialDirection, 18f);

            // Radial expansion
            Vector3f radialDir = randomUnitVectorXZ();
            radialDir.y = randomRange(0.2f, 0.5f);
            radialDir.normalise();

            // Bias grows from 0 → 1
            float bias = Maths.clamp(
                (age - PRIMER_END) / (4f * LIFE_SCALE),
                0f,
                1f
            );

            // Blend directions
            Vector3f dir = lerpDirection(coneDir, radialDir, bias);

            /* ===============================
               STAGE PARAMETERS
               =============================== */
            float expansion;
            float scale;
            float life;

            if (age < PRIMER_END) {
                expansion = randomRange(20f, 28f);
                scale = randomRange(1.5f, 3.0f);
                life = randomRange(4f, 8f) * LIFE_SCALE;
            } else {
                expansion = Maths.lerp(14f, 3f, age / (6f * LIFE_SCALE));
                scale = Maths.lerp(6f, 22f, age / (6f * LIFE_SCALE)) + random.nextFloat() * 4f;
                life = randomRange(30f, 55f) * LIFE_SCALE;
            }

            Vector3f velocity = new Vector3f(
                dir.x * expansion * 0.6f,
                dir.y * expansion * 0.4f,
                dir.z * expansion * 0.6f
            );

            Particle p = ParticleMaster.createParticle(
                smokeTexture,
                spawnPos,
                velocity,
                -0.002f,
                life,
                random.nextFloat() * 360f,
                scale
            );

            p.setScaleGrowth(age < PRIMER_END ? 6f : (10f + random.nextFloat() * 6f));
        }
    }

    public boolean isDead() {
        return dead;
    }

    /* ===============================
       HELPERS
       =============================== */

    private Vector3f randomUnitVectorXZ() {
        float angle = random.nextFloat() * (float) Math.PI * 2f;
        return new Vector3f((float) Math.cos(angle), 0, (float) Math.sin(angle));
    }

    private Vector3f lerpDirection(Vector3f a, Vector3f b, float t) {
        Vector3f out = new Vector3f(
            a.x + (b.x - a.x) * t,
            a.y + (b.y - a.y) * t,
            a.z + (b.z - a.z) * t
        );
        out.normalise();
        return out;
    }

    private Vector3f randomConeDirection(Vector3f forward, float angleDeg) {
        float angleRad = (float) Math.toRadians(angleDeg);
        float u = random.nextFloat();
        float v = random.nextFloat();

        float theta = 2f * (float) Math.PI * u;
        float phi = (float) Math.acos(1f - v * (1f - Math.cos(angleRad)));

        float x = (float) (Math.sin(phi) * Math.cos(theta));
        float y = (float) (Math.sin(phi) * Math.sin(theta));
        float z = (float) Math.cos(phi);

        Vector3f up = Math.abs(forward.y) < 0.99f ? new Vector3f(0,1,0) : new Vector3f(1,0,0);
        Vector3f right = Vector3f.cross(up, forward, null);
        right.normalise();
        up = Vector3f.cross(forward, right, null);

        Vector3f dir = new Vector3f(
            right.x * x + up.x * y + forward.x * z,
            right.y * x + up.y * y + forward.y * z,
            right.z * x + up.z * y + forward.z * z
        );
        dir.normalise();
        return dir;
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
