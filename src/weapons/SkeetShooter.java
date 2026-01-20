package weapons;

import org.joml.Vector3f;

import com.bulletphysics.dynamics.RigidBody;

import entities.Entity;
import entities.Player;
import entityManager.EntityManager;
import particles.ParticleMaster;
import text.TextRendererMaster;
import text.TextRenderer.TextAlignment;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_C;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import java.util.List;
import java.util.ArrayList;
import java.util.Random;

public class SkeetShooter {

	private static final float LAUNCH_COOLDOWN = 4.0f; // seconds
	private float launchTimer = 0f;
	
	private int numberHit = 0;
	
	private Vector3f position;
	private Vector3f direction;
	private float randomError;
	
	private Random random = new Random();
	
	private List<Entity> firedPigeons;
	
	
	
	public SkeetShooter(Vector3f position, Vector3f direction, float randomError) {
		super();
		this.position = position;
		this.direction = direction;
		this.randomError = randomError;
		this.firedPigeons = new ArrayList<>();
	}
	
	
	public void update(EntityManager entityManager, Player player, float deltaTime, long window) {
		float playerDistance = new Vector3f(player.x, player.y, player.z).distance(this.position);
		//System.out.println("playerDistance: " + playerDistance + " launchTimer:" + launchTimer);
		launchTimer -= deltaTime;
		if (launchTimer < 0f) {
		    launchTimer = 0f;
		}
		

		if (playerDistance < 10000) {
			if (launchTimer > 0f) {
				TextRendererMaster.renderText("Next Launch: " + (int)launchTimer,  0.0f, 0.95f, 0.5f,  1000, TextAlignment.LEFT);

				return;
			}
			TextRendererMaster.renderText("Press C to launch",  0.0f, 0.95f, 0.5f,  1000, TextAlignment.LEFT);

			if (glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS) {
				Entity p = spawnClayPigeon(entityManager);
				launch(p);
				firedPigeons.add(p);
				launchTimer = LAUNCH_COOLDOWN;
			}
		}
		
	}
	
	
	
	public Entity spawnClayPigeon(EntityManager entityManager) {
		Entity cube14 = new Entity(entityManager.getTexturedModel("clay_pigeon"), this.getPosition(), new Vector3f(0,0,0), 1.25f);
        return entityManager.addEntity(cube14, EntityManager.CollisionType.DYNAMIC_ACCURATE, 1f);
	}
	
	private void launch(Entity entity) {
	    RigidBody body = entity.getCollisionBody();

	    float launchSpeed = 0500 / 1f; // tune this

	    Vector3f launchDir = getLaunchDirection().mul(launchSpeed);

	    javax.vecmath.Vector3f impulse = new javax.vecmath.Vector3f(
	        launchDir.x,
	        launchDir.y,
	        launchDir.z
	    );

	    body.activate();                
	    body.applyCentralImpulse(impulse);
	}

	
	private Vector3f getLaunchDirection() {
	    Vector3f dir = new Vector3f(direction).normalize();
	    randomError = 0.5f;
	    if (randomError > 0f) {
	        dir.x += (random.nextFloat() - 0.5f) * randomError;
	        dir.y += (random.nextFloat() - 0.5f) * randomError;
	        dir.z += (random.nextFloat() - 0.5f) * randomError;
	        dir.normalize();
	    }

	    return dir;
	}
	
	public boolean checkHit(RigidBody body, EntityManager entityManager) {
		for (Entity e : firedPigeons) {
			if (e.getCollisionBody() == body) {
				this.numberHit++;
				createParticles(e);
				entityManager.removeEntityByID(e.getId());
				System.out.println("HIT!!");
				return true;
			}
		}
		return false;
	}
	
	
	public void createParticles(Entity e) {

	    RigidBody body = e.getCollisionBody();

	    javax.vecmath.Vector3f linVel = body.getLinearVelocity(new javax.vecmath.Vector3f());

	    Vector3f baseDir = new Vector3f(linVel.x, linVel.y, linVel.z);

	    if (baseDir.lengthSquared() < 0.0001f) {
	        baseDir.set(0, 1, 0); // fallback
	    } else {
	        baseDir.normalize();
	    }

	    int count = 40;

	    for (int i = 0; i < count; i++) {

	        org.lwjgl.util.vector.Vector3f vel =
	                new org.lwjgl.util.vector.Vector3f(
	                        baseDir.x,
	                        baseDir.y,
	                        baseDir.z
	                );

	        float spread = 10.6f;
	        vel.x += (random.nextFloat() - 0.5f) * spread;
	        vel.y += (random.nextFloat() - 0.5f) * spread * 0.5f;
	        vel.z += (random.nextFloat() - 0.5f) * spread;

	        vel.normalise();

	        float speed = 8f + random.nextFloat() * 6f;
	        vel.scale(speed);

	        float life = 1.2f + random.nextFloat() * 1.5f;
	        float scale = 1.4f + random.nextFloat() * 0.8f;

	        ParticleMaster.createParticle(
	                e.getTexturedModel().getTextureId(),
	                new org.lwjgl.util.vector.Vector3f(
	                        e.getPosition().x,
	                        e.getPosition().y,
	                        e.getPosition().z
	                ),
	                vel,
	                0.3f,
	                life,
	                random.nextFloat() * 360f,
	                scale
	        );
	    }
	}


	
	public Vector3f getPosition() {
		return position;
	}
	public void setPosition(Vector3f position) {
		this.position = position;
	}
	public Vector3f getDirection() {
		return direction;
	}
	public void setDirection(Vector3f direction) {
		this.direction = direction;
	}
	public float getRandomError() {
		return randomError;
	}
	public void setRandomError(float randomError) {
		this.randomError = randomError;
	}
	
	
	
	
	
	
}
