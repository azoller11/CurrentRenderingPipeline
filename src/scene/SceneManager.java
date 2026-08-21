package scene;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.joml.Vector3f;
import org.json.JSONArray;
import org.json.JSONObject;

import entities.Entity;
import entities.Light;
import entityManager.EntityManager;

/**
 * Saves the live world state (entities + lights) out to a scene file under
 * res/scenes/<name>.json.
 */
public class SceneManager {

	private static final String SCENE_DIR = "res/scenes/";

	public static void saveScene(String sceneName, EntityManager entityManager, List<Light> lights) {
		JSONObject root = new JSONObject();

		JSONArray entityArray = new JSONArray();
		for (Entity entity : entityManager.getEntitiesList()) {
			if (!entity.isSaveToScene()) {
				continue;
			}

			String modelName = entityManager.getTexturedModelName(entity.getTexturedModel());
			if (modelName == null) {
				System.err.println("SceneManager: skipping entity " + entity.getId()
						+ " - its TexturedModel isn't in the registry, so it can't be re-loaded by name.");
				continue;
			}

			JSONObject entityJson = new JSONObject();
			entityJson.put("modelName", modelName);
			entityJson.put("position", vectorToJSON(entity.getPosition()));
			entityJson.put("rotation", rotationToJSON(entity.getRotation()));
			entityJson.put("scale", entity.getScale());
			entityJson.put("collisionType", entity.getCollisionType());
			entityJson.put("mass", entity.getMass());
			entityArray.put(entityJson);
		}
		root.put("entities", entityArray);

		JSONArray lightArray = new JSONArray();
		for (Light light : lights) {
			JSONObject lightJson = new JSONObject();
			lightJson.put("position", vectorToJSON(light.getPosition()));
			lightJson.put("color", vectorToJSON(light.getColor()));
			lightJson.put("attenuation", vectorToJSON(light.getAttenuation()));
			lightJson.put("castShadow", light.isCastShadow());
			lightArray.put(lightJson);
		}
		root.put("lights", lightArray);

		try {
			Path path = Path.of(SCENE_DIR + sceneName + ".json");
			Files.createDirectories(path.getParent());
			Files.writeString(path, root.toString(4));
			System.out.println("SceneManager: saved scene to " + path);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private static JSONObject vectorToJSON(Vector3f v) {
		JSONObject json = new JSONObject();
		json.put("x", v.x);
		json.put("y", v.y);
		json.put("z", v.z);
		return json;
	}

	/**
	 * Entity.rotation is stored in radians (that's what Entity.getModelMatrix()
	 * feeds straight into JOML's rotateXYZ), but a radians value in a hand-edited
	 * scene file is unreadable, so this writes degrees instead.
	 */
	private static JSONObject rotationToJSON(Vector3f radians) {
		JSONObject json = new JSONObject();
		json.put("x", Math.toDegrees(radians.x));
		json.put("y", Math.toDegrees(radians.y));
		json.put("z", Math.toDegrees(radians.z));
		return json;
	}

}
