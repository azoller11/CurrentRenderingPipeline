package weapons;


import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import java.util.List;

import org.joml.Vector3f;

import animatedModel.AnimationScript;
import entities.Entity;

public class Target {

    private Entity entity;
    private boolean up = true;

    private AnimationScript hitScript;
    private AnimationScript reloadScript;

    public Target(Entity entity, boolean up) {
        this.entity = entity;
        this.up = up;
        loadScripts();
    }

    private void loadScripts() {
        hitScript = new AnimationScript(entity)
                .then(1, false);

        reloadScript = new AnimationScript(entity)
                .then(0, false);

        if (up) {
        	putUp(); // start upright
        }
    }

    public void update(float deltaTime) {
        if (hitScript.isRunning()) {
            hitScript.update(deltaTime);
        } else if (reloadScript.isRunning()) {
            reloadScript.update(deltaTime);
        }
    }

    public void putDown() {
        if (!up) return;

        up = false;
        hitScript.start();
    }

    public void putUp() {
        if (this.isUp())
            return;

        this.setUp(true);
        this.reloadScript.start();
    }

    private void setUp(boolean b) {
        this.up = b;
    }


	public boolean isUp() {
        return up;
    }

    public Entity getEntity() {
        return entity;
    }
    
    
    public AnimationScript getHitScript() {
		return hitScript;
	}

	public void setHitScript(AnimationScript hitScript) {
		this.hitScript = hitScript;
	}

	public AnimationScript getReloadScript() {
		return reloadScript;
	}

	public void setReloadScript(AnimationScript reloadScript) {
		this.reloadScript = reloadScript;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public static void updateAllTargets(List<Target> targets, float deltaTime) {
        boolean resetTargets = true;
        for (Target target : targets) {
        	target.update(deltaTime);
        	if (target.isUp() || target.getHitScript().isRunning()) {
        		resetTargets = false; 
        	}
        		
        }
        if (resetTargets) {
        	 for (Target target : targets) {
        		 target.putUp();
             }
        }
    }
}
