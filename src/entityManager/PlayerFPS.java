package entityManager;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.glfw.GLFW;

import animatedModel.AnimationScript;
import entities.Camera;
import entities.Entity;
import entities.Player;

public class PlayerFPS {
	
	private Entity FPSAnimation;
	
	private AnimationScript shootScript;
	private AnimationScript reloadScript;
	private AnimationScript weildScript;
	
	private float currentAimHeight = -10f;
	private float targetAimHeight  = -10f;
	
	
	
	
	public PlayerFPS(EntityManager entityManager) {
		FPSAnimation = new Entity(entityManager.getTexturedModel("FPSAnimation"), new Vector3f(500, 0, 0), new Vector3f(0,90,0), 0.25f);
        FPSAnimation = entityManager.addEntity(FPSAnimation, EntityManager.CollisionType.NONE, 3);
        
        shootScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(0, false); // Fire
        	        //.then(2, false); //PostFire
        
        reloadScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(3, false) // PriorToReload
        	        .then(5, false) // ReloadOne
        	       .then(4, false) //PostFire
        	       .then(2, false);
        
        weildScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(7, false); // Fire
        
        weildScript.start();
        
	}
	
	
	public void updatePlayerFPS(Player player, Camera camera, Long window, float deltaTime) {
		
		if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS && !reloadScript.isRunning() && camera.getPitch() < 37 && camera.getPitch() > -42) {
		    targetAimHeight = -2.5f;   // aimed
		} else {
		    targetAimHeight = -5.5f;  // hip fire
		}
		float aimSpeed = 12.0f; // higher = snappier, lower = slower

		currentAimHeight += (targetAimHeight - currentAimHeight) * aimSpeed * deltaTime;
		
		FPSAnimation.setPosition(player.x, (player.y) + player.getPlayerHeight() + currentAimHeight, player.z);
		
	      float yawRad   = -org.joml.Math.toRadians(camera.getYaw() - 90f);
          float pitchRad =  -org.joml.Math.toRadians(camera.getPitch());

          Quaternionf q = new Quaternionf()
                  .rotateY(yawRad)
                  .rotateX(pitchRad);

          // Extract Euler radians (XYZ order)
          Vector3f euler = new Vector3f();
          q.getEulerAnglesXYZ(euler);
          FPSAnimation.setRotation(euler);
          updatePlayerInput(window, deltaTime);
	}
	
	public void updatePlayerInput(Long window, float deltaTime) {
		boolean reload = glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS;
		if (!reloadScript.isRunning() && reload && !shootScript.isRunning()) {
			reloadScript.start();
		}
		
		
		
		if (reloadScript.isRunning()) {
			reloadScript.update(deltaTime);
		}
		
		
		boolean mouseDown =
		        GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
		        == GLFW.GLFW_PRESS;
		
		if (!shootScript.isRunning() && mouseDown && !reloadScript.isRunning()) {
			shootScript.start();
		}
		
		if (shootScript.isRunning()) {
			shootScript.update(deltaTime);
		}
		
		
		
		/*

	    boolean reload = glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS;

	    boolean mouseDown =
	        GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
	        == GLFW.GLFW_PRESS;

	    // ---------------- FIRE ----------------

	    if (mouseDown && !fireConsumed) {
	        if (!shootScript.isRunning() && !reloadScript.isRunning()) {
	            shootScript.start();
	            fireConsumed = true; 
	        }
	    }

	    // Release unlock
	    if (!mouseDown) {
	        fireConsumed = false;
	    }

	    // ---------------- RELOAD ----------------

	    if (reload && !reloadConsumed) {
	        if (!reloadScript.isRunning() && !shootScript.isRunning()) {
	            reloadScript.start();
	            reloadConsumed = true; 
	        }
	    }

	    // Release unlock
	    if (!reload) {
	        reloadConsumed = false;
	    }

	    // Always update scripts
	    shootScript.update(deltaTime);
	    reloadScript.update(deltaTime);
	    
	   // System.out.println(shootScript.isRunning() + " ");
	    
	    */
	}



}
