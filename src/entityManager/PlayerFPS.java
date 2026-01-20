package entityManager;

import static org.lwjgl.glfw.GLFW.GLFW_KEY_R;
import static org.lwjgl.glfw.GLFW.GLFW_PRESS;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import java.util.List;
import java.util.ArrayList;

import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.joml.Vector4f;
import org.lwjgl.glfw.GLFW;

import animatedModel.AnimationScript;
import entities.Camera;
import entities.Entity;
import entities.Player;
import main.Main;
import physics.HitResult;
import physics.PhysicsManager;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;
import text.TextRendererMaster;

public class PlayerFPS {
	
	private Entity FPSAnimation;
	
	private AnimationScript shootScript;
	private AnimationScript weildScript;
	
	private AnimationScript reloadStartScript;
	private AnimationScript reloadOneScript;
	private AnimationScript reloadFinishScript;
	
	
	private float currentAimHeight = -10f;
	private float targetAimHeight  = -10f;
	
	private int ammo = 999;
	private int loadedAmmo = 4;
	private int maxLoaded = 4;
	
	private int bulletsPerFire = 10; // Shotgun, 1 for a rifle for example
	private float bulleDistance = 10000f;
	private float bulletWideRange = 10;
	
	// Reload input state
	private boolean rWasDown = false;
	private boolean reloadHeld = false;
	
	private enum ReloadState {
	    NONE,
	    START,
	    LOADING,
	    FINISH
	}

	private ReloadState reloadState = ReloadState.NONE;
	
	
	public PlayerFPS(EntityManager entityManager) {
		FPSAnimation = new Entity(entityManager.getTexturedModel("FPSAnimation"), new Vector3f(500, 0, 0), new Vector3f(0,90,0), 0.25f);
        FPSAnimation = entityManager.addEntity(FPSAnimation, EntityManager.CollisionType.NONE, 3);
        
        shootScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(0, false); // Fire
        	        //.then(2, false); //PostFire
        
        reloadStartScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(3, false); // PriorToReload
        
        reloadOneScript = 
          	    new AnimationScript(FPSAnimation)
    	        .play(5, false); // ReloadOne
        
        
        reloadFinishScript = 
          	    new AnimationScript(FPSAnimation)
    	       .play(4, false) //Reload Last
    	       .then(2, false); // Finish
        		
        
        weildScript =
        	    new AnimationScript(FPSAnimation)
        	        .play(7, false); // Fire
        
        weildScript.start();
        
	}
	
	
	public List<HitResult>  shootGun(Camera camera, PhysicsManager physicsManager) {
		List<HitResult> hits = new ArrayList<HitResult>();
		loadedAmmo--;
		for (int i = 0; i < bulletsPerFire; i++ ) {
            HitResult hit = PhysicsManager.shoot(camera, physicsManager, bulleDistance);
            hits.add(hit);
		}
		 return hits;
	}
	
	public List<HitResult> updatePlayerFPS(Player player, Camera camera, PhysicsManager physicsManager, TextRenderer textRenderer, Long window, float deltaTime) {
		List<HitResult> hits = new ArrayList<HitResult>();
		if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS && reloadState == ReloadState.NONE && camera.getPitch() < 37 && camera.getPitch() > -42) {
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
          
          hits.addAll(updatePlayerInput(window, deltaTime, camera, physicsManager));
          
          textRenderer.setTextColor(1.15f, 1.15f, 2.65f, 1.0f);
          textRenderer.setGlow(true,0.4f, 0.20f,
  				1.5f, 0.5f, 0.5f, 
  				0.8f);
  		
  			TextRendererMaster.renderText(loadedAmmo + "/" + ammo,  0.85f, 0.05f, 0.5f,  1000, TextAlignment.LEFT);
  			
  			if (loadedAmmo == 0) {
  		      textRenderer.setTextColor(2.5f, 0.5f, 0.5f, 1.0f);
  	          textRenderer.setGlow(true,0.4f, 0.20f,
  	  				1.5f, 0.5f, 0.5f, 
  	  				0.8f);
  	  			TextRendererMaster.renderText("RELOAD",  0.45f, 0.55f, 0.25f,  1000, TextAlignment.LEFT);
  			}
          
          return hits;
	}
	
	public List<HitResult> updatePlayerInput(Long window, float deltaTime, Camera camera, PhysicsManager physicsManager) {

    List<HitResult> hits = new ArrayList<>();

    boolean rDown = glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS;

    /* ===============================
       INPUT EDGE DETECTION
       =============================== */

    if (rDown && !rWasDown) {
        reloadHeld = true;

        if (reloadState == ReloadState.NONE
                && loadedAmmo < maxLoaded
                && ammo > 0
                && !shootScript.isRunning()) {

            reloadStartScript.start();
            reloadState = ReloadState.START;
        }
    }

    if (!rDown) {
        reloadHeld = false;
    }

    rWasDown = rDown;

    /* ===============================
       RELOAD STATE MACHINE
       =============================== */

    switch (reloadState) {

        case START -> {
            reloadStartScript.update(deltaTime);

            if (!reloadStartScript.isRunning()) {
                reloadOneScript.start();
                reloadState = ReloadState.LOADING;
            }
        }

        case LOADING -> {
            reloadOneScript.update(deltaTime);

            if (!reloadOneScript.isRunning()) {

                // Apply ammo AFTER animation
                if (ammo > 0 && loadedAmmo < maxLoaded) {
                    loadedAmmo++;
                    ammo--;
                }

                // Continue reloading?
                if (reloadHeld && ammo > 0 && loadedAmmo < maxLoaded) {
                    reloadOneScript.start();
                } else {
                    reloadFinishScript.start();
                    reloadState = ReloadState.FINISH;
                }
            }
        }

        case FINISH -> {
            reloadFinishScript.update(deltaTime);

            if (!reloadFinishScript.isRunning()) {
                reloadState = ReloadState.NONE;
            }
        }

        default -> {}
    }

    /* ===============================
       SHOOTING (blocked while reloading)
       =============================== */

    boolean mouseDown =
            GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT)
                    == GLFW.GLFW_PRESS;

    if (reloadState == ReloadState.NONE
            && !shootScript.isRunning()
            && mouseDown
            && loadedAmmo > 0) {

        shootScript.start();
        hits.addAll(shootGun(camera, physicsManager));
    }

    if (shootScript.isRunning()) {
        shootScript.update(deltaTime);
    }

    return hits;
}


	public int getMaxLoaded() {
		return maxLoaded;
	}


	public void setMaxLoaded(int maxLoaded) {
		this.maxLoaded = maxLoaded;
	}


	public int getAmmo() {
		return ammo;
	}


	public void setAmmo(int ammo) {
		this.ammo = ammo;
	}


	public int getLoadedAmmo() {
		return loadedAmmo;
	}


	public void setLoadedAmmo(int loadedAmmo) {
		this.loadedAmmo = loadedAmmo;
	}



}
