package loaders;


import entities.Entity;
import entities.Light;
import physics.PhysicsManager;
import settings.EngineSettings;

import org.joml.Vector3f;

import com.bulletphysics.dynamics.RigidBody;

import toolbox.Mesh;

import java.io.*;
import java.util.ArrayList;
import java.util.List;

public class SceneLoader2 {
	
	public static String sceneDirectory = "res/scenes/";
	public static String sceneName = "testScene.txt";
	
	public static List<Entity> entities = new ArrayList<>();
	public static List<Light> lights = new ArrayList<>();


	public static void CreateScene(String fileName, List<Entity> entities, List<Light> lights, PhysicsManager physicsManager) {
	    String filePath = sceneDirectory + fileName;

	    try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
	        for (Entity entity : entities) {
	            writer.write("ENTITY\n");
	            writer.write("mesh=" + MeshManager.getMeshNameById(entity.getTexturedModel().getMesh()) + "\n");
	            writer.write("texture=" + TextureManager.getTextureNameById(entity.getTexturedModel().getTextureId()) + "\n");
	            writer.write("position=" + formatVec3(entity.getPosition()) + "\n");
	            writer.write("rotation=" + formatVec3(entity.getRotation()) + "\n");
	            writer.write("scale=" + entity.getScale() + "\n");
	            writer.write("normalMap=" + TextureManager.getTextureNameById(entity.getTexturedModel().getNormalMapId()) + "\n");
	            writer.write("metallicMap=" + TextureManager.getTextureNameById(entity.getTexturedModel().getMetallicMap()) + "\n");
	            writer.write("roughnessMap=" + TextureManager.getTextureNameById(entity.getTexturedModel().getRoughnessMap()) + "\n");
	            writer.write("aoMap=" + TextureManager.getTextureNameById(entity.getTexturedModel().getAoMap()) + "\n");
	            writer.write("heightMap=" + TextureManager.getTextureNameById(entity.getTexturedModel().getHeighMapId()) + "\n");
	            writer.write("shineDamper=" + entity.getTexturedModel().getShineDamper() + "\n");
	            writer.write("reflectivity=" + entity.getTexturedModel().getReflectivity() + "\n");
	            writer.write("transparency=" + entity.getTexturedModel().isHasTransparency() + "\n");
	            writer.write("opaque=" + entity.getTexturedModel().isHasOpaque() + "\n");

	            Vector3f parallax = entity.getTexturedModel().getParallaxScale();
	            if (parallax != null) {
	                writer.write("parallaxScale=" + formatVec3(parallax) + "\n");
	            }
	            
	            String collisionType = physicsManager.getCollisionType(entity);
	            writer.write("collisionType=" + collisionType + "\n");

	           
	            
	            RigidBody body = physicsManager.getBodyForEntity(entity);
                float invMass = body != null ? 1f / body.getInvMass() : 1f;
                writer.write("mass=" + invMass + "\n");

	            writer.write("\n");
	        }

	        for (Light light : lights) {
	            writer.write("LIGHT\n");
	            writer.write("position=" + formatVec3(light.getPosition()) + "\n");
	            writer.write("color=" + formatVec3(light.getColor()) + "\n");
	            writer.write("attenuation=" + formatVec3(light.getAttenuation()) + "\n");
	            //writer.write("direction=" + formatVec3(light.getDirection()) + "\n");
	            writer.write("castShadow=" + light.isCastShadow() + "\n");
	            writer.write("\n");
	        }

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}


	public static SceneData ReadScene(String fileName, PhysicsManager physicsManager) {
		 String filePath = sceneDirectory + fileName;
		
		
		try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
		    String line;
		    Entity currentEntity = null;
		    Light currentLight = null;
		
		    while ((line = reader.readLine()) != null) {
		        if (line.trim().isEmpty()) {
		            // Scene block complete
		          
		            continue;
		        }
		
		        if (line.equals("ENTITY")) {
		            currentEntity = new Entity(null, new Vector3f(), new Vector3f(), 1);
		            entities.add(currentEntity);
		        } else if (line.equals("LIGHT")) {
		            currentLight = new Light(new Vector3f(), new Vector3f(), new Vector3f(1, 0, 0));
		            lights.add(currentLight);
		        } else {
		            String[] parts = line.split("=");
		            if (parts.length != 2) continue;
		            String key = parts[0].trim();
		            String value = parts[1].trim();
		
		            if (currentEntity != null && !value.contains("NOT_FOUND")) {
		                switch (key) {
		                    case "mesh" -> currentEntity.getTexturedModel().setMesh(MeshManager.getMeshByName(value));
		                    case "texture" -> currentEntity.getTexturedModel().setTextureId(TextureLoader.loadTexture(value));
		                    case "position" -> currentEntity.setPosition(parseVec3(value));
		                    case "rotation" -> currentEntity.setRotation(parseVec3(value));
		                    case "scale" -> currentEntity.setScale(Float.parseFloat(value));
		                    case "normalMap" -> currentEntity.getTexturedModel().setNormalMapId(TextureLoader.loadTexture(value));
		                    case "metallicMap" -> currentEntity.getTexturedModel().setMetallicMap(TextureLoader.loadTexture(value));
		                    case "roughnessMap" -> currentEntity.getTexturedModel().setRoughnessMap(TextureLoader.loadTexture(value));
		                    case "aoMap" -> currentEntity.getTexturedModel().setAoMap(TextureLoader.loadTexture(value));
		                    case "heightMap" -> currentEntity.getTexturedModel().setHeighMapId(TextureLoader.loadTexture(value));
		                    case "shineDamper" -> currentEntity.getTexturedModel().setShineDamper(Float.parseFloat(value));
		                    case "reflectivity" -> currentEntity.getTexturedModel().setReflectivity(Float.parseFloat(value));
		                    case "transparency" -> currentEntity.getTexturedModel().setHasTransparency(Boolean.parseBoolean(value));
		                    case "opaque" -> currentEntity.getTexturedModel().setHasOpaque(Boolean.parseBoolean(value));
		                    case "parallaxScale" -> currentEntity.getTexturedModel().setParallaxScale(parseVec3(value));
		                    case "collisionType" -> currentEntity.setCollisionType(value);
		                    case "mass" -> currentEntity.setMass(Float.parseFloat(value));
		                }
		                
		                
		            }
		
		            if (currentLight != null) {
		                switch (key) {
		                    case "position" -> currentLight.setPosition(parseVec3(value));
		                    case "color" -> currentLight.setColor(parseVec3(value));
		                    case "attenuation" -> currentLight.setAttenuation(parseVec3(value));
		                   // case "direction" -> currentLight.setDirection(parseVec3(value));
		                    case "castShadow" -> currentLight.setCastShadow(Boolean.parseBoolean(value));
		                    }
		                }
		            }
		
		        }
		
		    } catch (IOException e) {
		        e.printStackTrace();
		    }
		
		
		for (Entity entity : entities) {
		    switch (entity.getCollisionType()) {
		        case "DYNAMIC" -> physicsManager.addMovableNotSoAccurateCollision(entity, entity.getMass());
		        case "STATIC" -> physicsManager.addStaticAccurateCollision(entity);
		        case "KINEMATIC" -> {
		            RigidBody body = physicsManager.addMovableNotSoAccurateCollision(entity, entity.getMass());
		            physicsManager.setBodyKinematic(body);
		        }
		    }
		}
		
		    return new SceneData(entities, lights);
		}
   
	
	private static String formatVec3(Vector3f vec) {
        return vec.x + "," + vec.y + "," + vec.z;
    }

    private static Vector3f parseVec3(String s) {
        String[] tokens = s.split(",");
        return new Vector3f(
            Float.parseFloat(tokens[0]),
            Float.parseFloat(tokens[1]),
            Float.parseFloat(tokens[2])
        );
    }

    public static class SceneData {
        public final List<Entity> entities;
        public final List<Light> lights;

        public SceneData(List<Entity> entities, List<Light> lights) {
            this.entities = entities;
            this.lights = lights;
        }
    }

    public static class MeshManager {
        public static String getMeshNameById(Mesh mesh) {
            //return mesh != null ? mesh.toString() : "null";
        	for (String meshName : EngineSettings.meshCache.keySet()) {
        		if (EngineSettings.meshCache.get(meshName) == mesh)
        			return meshName + ".obj";
        	}
        	return "NOT_FOUND";
        }

        public static Mesh getMeshByName(String name) {
        	if (EngineSettings.meshCache.containsKey(name))
        		return EngineSettings.meshCache.get(name); // Replace with actual logic
        	else 
        		return ObjLoader.loadObj(name);
        }
    }

    public static class TextureManager {
        public static String getTextureNameById(int id) {
        	//textureCache
        	for (String textureName : EngineSettings.textureCache.keySet()) {
        		if (EngineSettings.textureCache.get(textureName) == id)
        			return textureName;
        	}
        	return "NOT_FOUND";
        	
        }

        public static int getTextureIdByName(String name) {
        	//if (EngineSettings.textureCache.containsKey(name))
        //		return EngineSettings.textureCache.get(name);
        //	else 
        		return TextureLoader.loadTexture(name);
            //
        }
    }
}
