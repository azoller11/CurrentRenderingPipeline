package particles;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;

public class ParticleMaster {

	private static Map<ParticleTexture, List<Particle>> particles = new HashMap<ParticleTexture, List<Particle>>();
	private static ParticleRenderer renderer;
	
	private static ParticlePool particlePool = new ParticlePool();


	public static void init(org.joml.Matrix4f projectionMatrix) {
		renderer = new ParticleRenderer(projectionMatrix);
	}
	
	public static Particle createParticle(ParticleTexture texture, Vector3f position, Vector3f velocity,
        float gravityEffect, float lifeLength, float rotation, float scale) {
		Particle particle = new Particle(); // particlePool.getParticle();
		if (particle == null) {
			particle = new Particle();
		}
		particle.setActive(texture, position, velocity, gravityEffect, lifeLength, rotation, scale);
		addParticle(particle);
		return particle;
		}
	
	public static void releaseParticle(Particle particle) {
	  
	    particlePool.releaseParticle(particle);
	    //particle.setInactive();
	}
	
	public static void update(Camera camera, float delta) {
	    Iterator<Entry<ParticleTexture, List<Particle>>> mapIterator = particles.entrySet().iterator();
	    while (mapIterator.hasNext()) {
	        Entry<ParticleTexture, List<Particle>> entry = mapIterator.next();
	        List<Particle> list = entry.getValue();
	        Iterator<Particle> iterator = list.iterator();
	        while (iterator.hasNext()) {
	            Particle p = iterator.next();
	            if (!p.update(camera, delta)) {
	                iterator.remove();
	                ParticleMaster.releaseParticle(p);
	                if (list.isEmpty()) {
	                    mapIterator.remove();
	                }
	            }
	        }
	        if (!entry.getKey().isAdditive()) {
	            InsertionSort.sortHighToLow(list);
	        }
	    }
	}



	public static void renderParticles(Camera camera) {
	    //System.out.println("Render called. Total texture groups: " + particles.size());
	    int totalParticles = 0;
	    for (List<Particle> list : particles.values()) {
	        totalParticles += list.size();
	    }
	   // System.out.println("Total particles to render: " + totalParticles);
	    renderer.render(particles, camera);
	}

	public static void cleanUp() {
		renderer.cleanUp();
	}

	public static void addParticle(Particle particle) {
	   // System.out.println("Adding particle to system. Texture: " + 
	      //  particle.getTexture().getTextureID());
	    List<Particle> list = particles.get(particle.getTexture());
	    if (list == null) {
	        list = new ArrayList<Particle>();
	        particles.put(particle.getTexture(), list);
	   //     System.out.println("Created new particle list for texture");
	    }
	    list.add(particle);
	    //System.out.println("Total particles in list: " + list.size());
	}
}
