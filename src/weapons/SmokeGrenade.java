package weapons;

import org.lwjgl.util.vector.Vector3f;
import loaders.TextureLoader;
import particles.Particle;
import particles.ParticleMaster;
import particles.ParticleSystem;
import particles.ParticleTexture;

import java.util.Random;

public class SmokeGrenade {

    private Vector3f position;
    private ParticleTexture smokeTexture;
    private Random random = new Random();

    public SmokeGrenade(Vector3f position) {
        this.position = position;

        smokeTexture = new ParticleTexture(
            TextureLoader.loadExplicitTexture("smoke.png"),
            8,          // atlas rows (good for variation)
            false       // NOT additive (important for smoke)
        );
    }

    public void update() {

        int particlesPerFrame = 3;   // density of smoke

        float gravity = -0.005f + -(random.nextFloat(0.1f));     // almost floating
        float life = 80f + random.nextFloat() * 6f;
        float baseScale = 0.1f;

        for (int i = 0; i < particlesPerFrame; i++) {

            // Random spawn jitter so it doesn't look like a point source
            Vector3f spawnPos = new Vector3f(
                position.x + randomRange(-0.5f, 0.5f),
                position.y + randomRange(0f, 0.3f),
                position.z + randomRange(-0.5f, 0.5f)
            );

            // Smoke rises slowly and spreads outward
            Vector3f velocity = new Vector3f(
                randomRange(-0.6f + -(random.nextFloat()), 0.6f+ (random.nextFloat())),
                randomRange(0.8f+ (random.nextFloat()), 1.5f+ (random.nextFloat())),
                randomRange(-0.6f+ -(random.nextFloat()), 0.6f+ (random.nextFloat()))
            );

            float rotation = random.nextFloat() * 360f;
            float scale = baseScale + random.nextFloat() * 25f;

            Particle p = ParticleMaster.createParticle(
            	    smokeTexture,
            	    spawnPos,
            	    velocity,
            	    gravity,
            	    life,
            	    rotation,
            	    scale
            	);
            p.setScaleGrowth(24.0f + random.nextFloat(20f)); 
        }
    }

    private float randomRange(float min, float max) {
        return min + random.nextFloat() * (max - min);
    }
}
