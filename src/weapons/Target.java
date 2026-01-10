package weapons;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_T;
import static org.lwjgl.glfw.GLFW.GLFW_KEY_U;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import org.joml.Vector3f;

import animatedModel.AnimationScript;
import entities.Entity;

public class Target {
	
	
	private Entity entity;
	private Vector3f position;
	
	private int health = 1;
	private float resetTime = 100;
	private boolean putDown = false;
	private boolean putUp = true;
	
	private AnimationScript hitScript;
	private AnimationScript reloadScript;
	
	
	public Target(Entity entity) {
		this.entity = entity;
		this.health = 1;
		this.putUp = true;
		
		loadScripts();
	}
	
	public void loadScripts() {
		hitScript = new AnimationScript(entity.getTexturedModel())
	        	    .then(0, false); 
		
		reloadScript = new AnimationScript(entity.getTexturedModel())
        	    .then(1, false); 
		 this.putUp();
		
	}
	
	private boolean tPressedLastFrame = false;
	private boolean uPressedLastFrame = false;
	
	public void update(float deltaTime, long window) {

	    boolean tPressedNow = glfwGetKey(window, GLFW_KEY_T) == GLFW_PRESS;
	    boolean uPressedNow = glfwGetKey(window, GLFW_KEY_U) == GLFW_PRESS;

	    // ---- EDGE DETECTION ----
	    boolean tJustPressed = tPressedNow && !tPressedLastFrame;
	    boolean uJustPressed = uPressedNow && !uPressedLastFrame;

	    if (this.isPutUp() && tJustPressed && !hitScript.isRunning() && !reloadScript.isRunning()) {
	        this.putDown();
	    }

	    if (this.isPutDown() && uJustPressed && !hitScript.isRunning() && !reloadScript.isRunning()) {
	        this.putUp();
	    }

	    // ---- UPDATE ANIMATIONS ----
	    if (hitScript.isRunning()) {
	        hitScript.update(deltaTime);
	    } else if (reloadScript.isRunning()) {
	        reloadScript.update(deltaTime);
	    }

	    // ---- STORE LAST FRAME STATE ----
	    tPressedLastFrame = tPressedNow;
	    uPressedLastFrame = uPressedNow;
	}


	

	public Entity getEntity() {
		return entity;
	}

	public void setEntity(Entity entity) {
		this.entity = entity;
	}

	public Vector3f getPosition() {
		return position;
	}


	public void setPosition(Vector3f position) {
		this.position = position;
	}


	public int getHealth() {
		return health;
	}


	public void setHealth(int health) {
		this.health = health;
	}


	public float getResetTime() {
		return resetTime;
	}


	public void setResetTime(float resetTime) {
		this.resetTime = resetTime;
	}


	public boolean isPutDown() {
		return putDown;
	}


	public void setPutDown(boolean putDown) {
		this.putDown = putDown;
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


	public boolean isPutUp() {
		return putUp;
	}


	public void setPutUp(boolean putUp) {
		this.putUp = putUp;
	}
	
	
	public void putUp() {
		if (this.reloadScript.isRunning())
			return;
		this.setPutUp(true);
		this.setPutDown(false);
		this.reloadScript.start();
	}
	
	public void putDown() {
		if (this.hitScript.isRunning())
			return;
		this.setPutDown(true);
		this.setPutUp(false);
		
		this.hitScript.start();
	}
	
	

}
