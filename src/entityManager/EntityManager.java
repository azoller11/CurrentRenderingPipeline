package entityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import entities.Camera;
import entities.Entity;
import entities.TexturedModel;
import entities.Light;
import entities.Player;
import physics.PhysicsManager;
import settings.EngineSettings;
import terrain.Terrain;

public class EntityManager {
	
	
	public Camera camera;
	public Player player;
	
	public Terrain terrain;
	
	public Map<String, TexturedModel> texturedModels;
	
	public Map<Integer, Entity> entities;
	public Map<Integer, Light> lights;
	
	public PhysicsManager physicsManager;
	
	public TexturedModels texturedModelsClass;
	
	public static enum CollisionType {
		NONE,
		ADD_BODY,
	    STATIC_NOT_ACCURATE,
	    STATIC_ACCURATE,
	    DYNAMIC_NOT_ACCURATE,
	    DYNAMIC_ACCURATE,
	    STATIC_TERRAIN_ACCURATE
	}
	
	
	public EntityManager() {
		this.entities = new HashMap<>();
		this.lights = new HashMap<>();
		this.physicsManager = new PhysicsManager();
		this.texturedModels  = new HashMap<>();
		this.texturedModelsClass = new TexturedModels();
		this.texturedModelsClass.init(texturedModels);
	}
	

	
	public Entity addEntity(Entity newEntity, CollisionType collisionType, float mass) {
		if (collisionType.equals(CollisionType.STATIC_NOT_ACCURATE))
			this.physicsManager.addStaticNotSoAccurateCollision(newEntity);
		if (collisionType.equals(CollisionType.STATIC_ACCURATE))
			this.physicsManager.addStaticAccurateCollision(newEntity);
		if (collisionType.equals(CollisionType.DYNAMIC_NOT_ACCURATE))
			this.physicsManager.addMovableNotSoAccurateCollision(newEntity, mass);
		if (collisionType.equals(CollisionType.DYNAMIC_ACCURATE))
			this.physicsManager.addMovableAccurateCollision(newEntity, mass);
		if (collisionType.equals(CollisionType.ADD_BODY))
			this.physicsManager.addRigidBody(newEntity);
		entities.put(newEntity.getId(), newEntity);
		if (entities.containsKey(newEntity.getId())) {
			//System.out.println("Successfully Created Entity: " + newEntity.getId());
		} else {
			//System.out.println("Unable to add Entity: " + newEntity.getId());
		}
		return newEntity;
	}
	
	
	
	
	
	public void addLight(Light newLight) {
		lights.put(newLight.getId(), newLight);
	}
	
	
	public void removeEntityByID(int Id) {
		if (entities.containsKey(Id)) {
			Entity e = entities.get(Id);
			if (EngineSettings.SelectedEntity == e) {
				EngineSettings.SelectedEntity = null;
				EngineSettings.OpenEntity = null;
			}
			//Remove collision detection first
			this.physicsManager.dynamicsWorld.removeRigidBody(e.getCollisionBody());
			entities.remove(Id);
			
			if (!entities.containsKey(Id)) {
				System.out.println("Successfully removed Entity: " + Id);
			} else {
				System.out.println("Unable to remove Entity: " + Id);
			}
		}
	}
	
	
	
	public Light getLightById(Integer Id) {
		return lights.get(Id);
	}
	
	public Entity getEntityById(Integer Id) {
		return entities.get(Id);
	}
	
	public List<Entity> getEntitiesList() {
		return new ArrayList<>(this.getEntities().values());
	}
	
	public List<Light> getLightsList() {
		return new ArrayList<>(this.getLights().values());
	}



	public Camera getCamera() {
		return camera;
	}



	public void setCamera(Camera camera) {
		this.camera = camera;
	}



	public Player getPlayer() {
		return player;
	}



	public void setPlayer(Player player) {
		this.player = player;
	}



	public Terrain getTerrain() {
		return terrain;
	}



	public void setTerrain(Terrain terrain) {
		this.terrain = terrain;
	}



	public Map<Integer, Entity> getEntities() {
		return entities;
	}



	public void setEntities(Map<Integer, Entity> entities) {
		this.entities = entities;
	}



	public Map<Integer, Light> getLights() {
		return lights;
	}



	public void setLights(Map<Integer, Light> lights) {
		this.lights = lights;
	}



	public PhysicsManager getPhysicsManager() {
		return physicsManager;
	}



	public void setPhysicsManager(PhysicsManager physicsManager) {
		this.physicsManager = physicsManager;
	}
	
	

	
	
	
	
}
