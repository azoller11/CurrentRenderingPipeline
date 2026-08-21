package guiManager;

import java.util.List;

import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import entities.Entity;
import entities.TexturedModel;
import entityManager.EntityManager;
import entityManager.EntityManager.CollisionType;
import physics.HitResult;
import settings.EngineSettings;
import toolbox.MousePicker;

/**
 * Handles the "Add Entity" placement tool: while EngineSettings.EntityPlacementMode is
 * on, a preview entity follows wherever the mouse ray hits in the world (terrain, other
 * entities - whatever's actually there), the scroll wheel cycles through the registered
 * TexturedModel types, and a left click (not over a GUI element) finalizes placement -
 * giving the entity real collision and selecting it in the MousePicker.
 */
public class EntityGuiManager {

	private static final float MAX_PLACEMENT_DISTANCE = 5000f;

	private List<String> modelNames;
	private int selectedIndex = 0;

	private Entity previewEntity;

	private boolean placeKeyWasDown = false;

	public void update(EntityManager entityManager, MousePicker picker, long window) {
		if (!EngineSettings.EntityPlacementMode) {
			cancelPreview(entityManager);
			return;
		}

		if (modelNames == null) {
			modelNames = entityManager.getTexturedModelNames();
		}
		if (modelNames.isEmpty()) return;

		if (previewEntity == null) {
			spawnPreview(entityManager);
			// Ignore the click that was still held down from pressing the "Add Entity"
			// button itself - only place on the next fresh press.
			placeKeyWasDown = true;
		}

		HitResult hit = picker.raycastMouseAgainstWorld(window, entityManager.getPhysicsManager(), MAX_PLACEMENT_DISTANCE);
		Vector3f pos = hit.hit
				? hit.hitPoint
				: new Vector3f(picker.getWorldPosX(), 0, picker.getWorldPosZ());
		previewEntity.setPosition(pos);

		boolean placePressed = GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS;
		if (placePressed && !placeKeyWasDown && !EngineSettings.overTexture) {
			finalizePlacement(entityManager, picker);
		}
		placeKeyWasDown = placePressed;
	}

	public void handleScroll(double yOffset, EntityManager entityManager) {
		if (!EngineSettings.EntityPlacementMode || modelNames == null || modelNames.isEmpty()) return;

		if (yOffset > 0) {
			selectedIndex = (selectedIndex + 1) % modelNames.size();
		} else if (yOffset < 0) {
			selectedIndex = (selectedIndex - 1 + modelNames.size()) % modelNames.size();
		} else {
			return;
		}

		if (previewEntity != null) {
			previewEntity.setTexturedModel(entityManager.getTexturedModel(modelNames.get(selectedIndex)));
		}
	}

	private void spawnPreview(EntityManager entityManager) {
		TexturedModel model = entityManager.getTexturedModel(modelNames.get(selectedIndex));
		previewEntity = new Entity(model, new Vector3f(), new Vector3f(), 1f);
		entityManager.addEntity(previewEntity, CollisionType.NONE, 0);
	}

	private void finalizePlacement(EntityManager entityManager, MousePicker picker) {
		entityManager.getPhysicsManager().addStaticAccurateCollision(previewEntity);
		previewEntity.setCollisionType(CollisionType.STATIC_ACCURATE.name());
		previewEntity.setMass(0);

		EngineSettings.SelectedEntity = previewEntity;
		EngineSettings.SelectedLight = null;

		// MousePicker's own entity list is a snapshot taken once at construction (only
		// refreshed elsewhere after a scene load), so without this the newly placed
		// entity would be selected in name only - unclickable/undraggable via the
		// picker until something else happened to refresh that list.
		picker.setEntities(entityManager.getEntitiesList());

		previewEntity = null;
		EngineSettings.EntityPlacementMode = false;
	}

	private void cancelPreview(EntityManager entityManager) {
		if (previewEntity != null) {
			entityManager.removeEntityByID(previewEntity.getId());
			previewEntity = null;
		}
	}

}
