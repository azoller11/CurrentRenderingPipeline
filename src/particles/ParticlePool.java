package particles;

import java.util.List;
import java.util.ArrayList;

public class ParticlePool {
    private List<Particle> inactiveParticles = new ArrayList<>();

    public Particle getParticle() {
        if (!inactiveParticles.isEmpty()) {
            return inactiveParticles.remove(inactiveParticles.size() - 1);
        }
        return null; // Create a new Particle if needed.
    }

    public void releaseParticle(Particle particle) {
        inactiveParticles.add(particle);
    }
}