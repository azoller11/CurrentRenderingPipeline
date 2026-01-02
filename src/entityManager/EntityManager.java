package entityManager;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.joml.Matrix4f;

import java.util.HashMap;

import entities.Camera;
import entities.Entity;
import entities.TexturedModel;
import entities.Light;
import entities.Player;
import physics.PhysicsManager;
import settings.EngineSettings;
import terrain.Terrain;
import com.bulletphysics.dynamics.RigidBody;

import animatedModel.AnimatedModel;
import animatedModel.AnimationElement;
import animatedModel.Bone;


public class EntityManager {
	
	
	public Camera camera;
	public Player player;
	
	public Terrain terrain;
	
	public Map<String, TexturedModel> texturedModels;
	
	public Map<Entity, RigidBody> entityRigidBodyMap = new HashMap<>();
	
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
		if (collisionType.equals(CollisionType.STATIC_NOT_ACCURATE)) {
			entityRigidBodyMap.put(newEntity, this.physicsManager.addStaticNotSoAccurateCollision(newEntity));
		}
			
		if (collisionType.equals(CollisionType.STATIC_ACCURATE)) {
			entityRigidBodyMap.put(newEntity, this.physicsManager.addStaticAccurateCollision(newEntity));
		}
			
		if (collisionType.equals(CollisionType.DYNAMIC_NOT_ACCURATE)) {
			entityRigidBodyMap.put(newEntity, this.physicsManager.addMovableNotSoAccurateCollision(newEntity, mass));
		}
			
		if (collisionType.equals(CollisionType.DYNAMIC_ACCURATE)) {
			entityRigidBodyMap.put(newEntity, this.physicsManager.addMovableAccurateCollision(newEntity, mass));
		}
			
		if (collisionType.equals(CollisionType.ADD_BODY)) {
			entityRigidBodyMap.put(newEntity, this.physicsManager.addRigidBody(newEntity));
		}
			
		entities.put(newEntity.getId(), newEntity);
		if (entities.containsKey(newEntity.getId())) {
			//System.out.println("Successfully Created Entity: " + newEntity.getId());
		} else {
			//System.out.println("Unable to add Entity: " + newEntity.getId());
		}
		return newEntity;
	}
	
	public void updateAnimations(float deltaTime) {
    for (int a : entities.keySet()) {
        Entity e = entities.get(a);
        TexturedModel texturedModel = e.getTexturedModel();
        
        if (texturedModel == null) continue;
        
        // Get animation elements for this entity
        List<AnimationElement> animationElements = texturedModel.getAnimationElements();
        if (animationElements == null || animationElements.isEmpty()) {
            // No animation elements? Add a default one
            if (texturedModel.getAnimatedModel() != null || 
                texturedModel.getAnimatedModels() != null) {
                texturedModel.addAnimationElement(new AnimationElement(0, 0));
                animationElements = texturedModel.getAnimationElements();
            }
        }
        
        if (animationElements == null || animationElements.isEmpty()) continue;
        
        // Update each animation element
        for (AnimationElement element : animationElements) {
            element.incAnimationTime(deltaTime);
            float animationTime = element.getAnimationTime();
            int animIndex = 0;//element.getAnimationIndex();
            
            // Handle single model
            if (texturedModel.getAnimatedModel() != null) {
                AnimatedModel animatedModel = texturedModel.getAnimatedModel();
                if (animatedModel.getAnimations() != null && 
                    animIndex < animatedModel.getAnimations().length) {
                    animatedModel.updateAnimation(animIndex, animationTime);
                }
            }
            // Handle multiple models - ONLY UPDATE THE FIRST MODEL!
            else // In your updateAnimations method, add debug for multiple bones:
            	if (texturedModel.getAnimatedModels() != null) {
            	    List<AnimatedModel> animatedModels = texturedModel.getAnimatedModels();
            	    
            	    if (!animatedModels.isEmpty()) {
            	    	for (AnimatedModel firstModel : animatedModels) {
            	    		//AnimatedModel firstModel = animatedModels.get(0);
                	        if (firstModel.getAnimations() != null && 
                	            animIndex < firstModel.getAnimations().length) {
                	            firstModel.updateAnimation(animIndex, animationTime);
                	        }
            	    	}
            	        
            	     
            	    }
            	}
        }
    }
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



	public Map<Entity, RigidBody> getEntityRigidBodyMap() {
		return entityRigidBodyMap;
	}



	public void setEntityRigidBodyMap(Map<Entity, RigidBody> entityRigidBodyMap) {
		this.entityRigidBodyMap = entityRigidBodyMap;
	}
	
	
	public RigidBody getEntityRigidBody(Entity e) {
		return (this.entityRigidBodyMap.containsKey(e) ? this.entityRigidBodyMap.get(e) : null);
	}
	

	
	
	
	
}
