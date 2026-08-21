package scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

import javax.vecmath.Quat4f;

import entities.Entity;
import entities.Light;
import entities.TexturedModel;
import entityManager.EntityManager;
import entityManager.EntityManager.CollisionType;

/**
 * Loads a scene file saved by SceneManager, replacing the current world's
 * entities and lights with what's in the file.
 */
public class SceneLoader {

	private static final String SCENE_DIR = "res/scenes/";

	public static void loadScene(String sceneName, EntityManager entityManager, List<Light> lights) {
		Path path = Path.of(SCENE_DIR + sceneName + ".json");
		String content;
		try {
			content = Files.readString(path);
		} catch (IOException e) {
			System.err.println("SceneLoader: could not read scene file " + path);
			e.printStackTrace();
			return;
		}

		JSONObject root = new JSONObject(content);

		// Clear the current world before loading.
		for (Entity entity : entityManager.getEntitiesList()) {
			if (entity.isSaveToScene()) {
				entityManager.removeEntityByID(entity.getId());
			}
			
		}
		lights.clear();

		JSONArray entityArray = root.optJSONArray("entities");
		if (entityArray != null) {
			for (int i = 0; i < entityArray.length(); i++) {
				JSONObject entityJson = entityArray.getJSONObject(i);
				String modelName = entityJson.getString("modelName");

				if (!entityManager.hasTexturedModel(modelName)) {
					System.err.println("SceneLoader: skipping entity - unknown modelName \"" + modelName + "\"");
					continue;
				}
				TexturedModel model = entityManager.getTexturedModel(modelName);

				Entity entity = new Entity(
						model,
						vectorFromJSON(entityJson.getJSONObject("position")),
						rotationFromJSON(entityJson.getJSONObject("rotation")),
						(float) entityJson.getDouble("scale"));

				CollisionType collisionType = CollisionType.valueOf(
						entityJson.optString("collisionType", "STATIC_ACCURATE"));
				float mass = (float) entityJson.optDouble("mass", 0);

				entityManager.addEntity(entity, collisionType, mass);
				fixInitialBodyRotation(entity);
			}
		}

		JSONArray lightArray = root.optJSONArray("lights");
		if (lightArray != null) {
			for (int i = 0; i < lightArray.length(); i++) {
				JSONObject lightJson = lightArray.getJSONObject(i);
				Light light = new Light(
						vectorFromJSON(lightJson.getJSONObject("position")),
						vectorFromJSON(lightJson.getJSONObject("color")),
						vectorFromJSON(lightJson.getJSONObject("attenuation")));
				light.setCastShadow(lightJson.optBoolean("castShadow", false));
				lights.add(light);
			}
		}

		System.out.println("SceneLoader: loaded scene from " + path);
	}

	/**
	 * EntityManager.addEntity() builds the physics body's initial rotation
	 * (PhysicsManager.addStaticCollisionMesh/addMovableCollisionMesh) assuming
	 * Entity.rotation is degrees and converts it - but it's actually radians, so an
	 * already-small radian value gets shrunk further, leaving the body's collision
	 * shape at close to zero rotation. That mismatched body rotation then gets read
	 * back into the entity every frame by PhysicsManager.updateEntitiesFromCollisionShapes(),
	 * which is what made loaded entities' rotation appear to reset. Push the correct
	 * (radians, unconverted) rotation onto the body right after creation to fix it.
	 */
	private static void fixInitialBodyRotation(Entity entity) {
		RigidBody body = entity.getCollisionBody();
		if (body == null) return;

		Vector3f r = entity.getRotation();
		Quaternionf jomlQuat = new Quaternionf().rotateXYZ(r.x, r.y, r.z);
		Quat4f quat = new Quat4f(jomlQuat.x, jomlQuat.y, jomlQuat.z, jomlQuat.w);

		Transform transform = new Transform();
		body.getWorldTransform(transform);
		transform.setRotation(quat);

		body.setWorldTransform(transform);
		if (body.getMotionState() != null) {
			body.getMotionState().setWorldTransform(transform);
		}
	}

	private static Vector3f vectorFromJSON(JSONObject json) {
		return new Vector3f(
				(float) json.getDouble("x"),
				(float) json.getDouble("y"),
				(float) json.getDouble("z"));
	}

	/**
	 * Scene files store rotation in degrees (see SceneManager.rotationToJSON),
	 * but Entity.rotation is expected to be radians, so convert back on load.
	 */
	private static Vector3f rotationFromJSON(JSONObject json) {
		return new Vector3f(
				(float) Math.toRadians(json.getDouble("x")),
				(float) Math.toRadians(json.getDouble("y")),
				(float) Math.toRadians(json.getDouble("z")));
	}

}
