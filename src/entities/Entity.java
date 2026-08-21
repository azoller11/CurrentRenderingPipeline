package entities;

import java.util.List;
import java.util.ArrayList;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bulletphysics.dynamics.RigidBody;

import animatedModel.AnimatedModel;
import animatedModel.AnimationElement;
import animation.Animator;
import toolbox.Mesh;

public class Entity {

	private int Id;
	
	private TexturedModel texturedModel;
	
	private AnimatedModel animatedModel;
	
	private List<AnimatedModel> animatedModels;
	

    // Basic transform
    private Vector3f position;
    private Vector3f rotation; // rotation.x => pitch, rotation.y => yaw, rotation.z => roll
    private float scale;
    
    private int activeAnimation = -1;
    private List<AnimationElement> animationState;
    private Animator animator;
    
    
    
    private RigidBody collisionBody;
    private String collisionType = "STATIC";
    private float mass = 0f;

    private boolean saveToScene = true;

    
    public Entity(TexturedModel texturedModel, Vector3f position, Vector3f rotation, float scale) {
        this.texturedModel = texturedModel;
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = scale;
        this.Id = generateNewId();
        
        //Generate animations
        animationState = new ArrayList<>();
        if (texturedModel.getAnimationElements() != null) {
        	this.activeAnimation = 0;
        	 for (AnimationElement def : texturedModel.getAnimationElements()) {
                 animationState.add(new AnimationElement(def)); // COPY
             }
        	 this.animator = new Animator(this);
        	 this.activeAnimation = -1;
        }
        
        // ---- Single animated model (legacy support) ----
        if (texturedModel.getAnimatedModel() != null) {
        	this.setAnimatedModel(texturedModel.getAnimatedModel().clone());
        }

        // ---- Multiple animated models ----
        if (texturedModel.getAnimatedModels() != null) {
        	animatedModels = new ArrayList<>();
            for (AnimatedModel  model : texturedModel.getAnimatedModels()) {
              this.animatedModels.add(model.clone());
            }
        }
       
    }

    public Entity() {
		// TODO Auto-generated constructor stub
	}

    public Vector3f getPosition() {
        return position;
    }

    public Vector3f getRotation() {
        return rotation;
    }

    public float getScale() {
        return scale;
    }

    public void setPosition(Vector3f newPos) {
        this.position.set(newPos);
    }
    // Similarly, setRotation, setScale, etc.

	
	public void setRotation(Vector3f rotation) {
		this.rotation.set(rotation);
	}

	public void setScale(float scale) {
		this.scale = scale;
	}



	public void setPosition(float x, float y, float z) {
		this.position.set(x, y, z);
	}
	
	
	
	public RigidBody getCollisionBody() {
		return collisionBody;
	}

	public void setCollisionBody(RigidBody collisionBody) {
		this.collisionBody = collisionBody;
	}

	public org.lwjgl.util.vector.Matrix4f createTransformationMatrix() {
		org.lwjgl.util.vector.Matrix4f matrix = new org.lwjgl.util.vector.Matrix4f();
		matrix.setIdentity();
		org.lwjgl.util.vector.Matrix4f.translate(new org.lwjgl.util.vector.Vector3f(this.getPosition().x,this.getPosition().y,this.getPosition().z), matrix, matrix);
		org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(this.getRotation().x), new org.lwjgl.util.vector.Vector3f(1,0,0), matrix, matrix);
		org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(this.getRotation().y), new org.lwjgl.util.vector.Vector3f(0,1,0), matrix, matrix);
		org.lwjgl.util.vector.Matrix4f.rotate((float) Math.toRadians(this.getRotation().z), new org.lwjgl.util.vector.Vector3f(0,0,1), matrix, matrix);
		org.lwjgl.util.vector.Matrix4f.scale(new org.lwjgl.util.vector.Vector3f(this.scale,this.scale,this.scale), matrix, matrix);
		return matrix;
	}

	public Matrix4f getModelMatrix() {
		Matrix4f model = new Matrix4f()
    		    .identity()
    		    //.scale(this.getScale(),this.getScale(),this.getScale())            // Scale first
    		    .rotateXYZ(this.getRotation().x, this.getRotation().y, this.getRotation().z)         // Rotate next
    		    .translate(this.getPosition());    // Finally translate

    		model.setTranslation(this.getPosition());
    		model.scale(this.getScale(),this.getScale(),this.getScale());
    		return model;
	}

	public int getId() {
		return Id;
	}

	public void setId(int id) {
		Id = id;
	}
	
	public String getCollisionType() {
	    return collisionType;
	}

	public void setCollisionType(String collisionType) {
	    this.collisionType = collisionType;
	}

	public float getMass() {
	    return mass;
	}

	public void setMass(float mass) {
	    this.mass = mass;
	}

	public boolean isSaveToScene() {
		return saveToScene;
	}

	public void setSaveToScene(boolean saveToScene) {
		this.saveToScene = saveToScene;
	}

	private static int generateNewId() {
	    return (int)(System.nanoTime() ^ System.currentTimeMillis());
	}
	
	
	
	
	public TexturedModel getTexturedModel() {
		return texturedModel;
	}

	public void setTexturedModel(TexturedModel texturedModel) {
		this.texturedModel = texturedModel;
	}

	public Entity cloneEntity() {
	    Entity e = new Entity();

	    // ---- Clone basic properties ----
	    e.texturedModel = this.getTexturedModel();

	    e.position = new Vector3f(this.position);
	    e.rotation = new Vector3f(this.rotation);
	    e.scale = this.scale;


	    


	    // ---- Clone physics properties ----
	    e.collisionType = this.collisionType;
	    e.mass = this.mass;

	    // IMPORTANT:
	    // Do NOT clone collisionBody, Bullet rigid bodies cannot be duplicated safely.
	    e.collisionBody = null;

	    // ---- Generate NEW ID ----
	    e.Id = generateNewId();

	    return e;
	}
	

    public void playAnimation(int index, boolean loop) {
        if (index < 0) {
            activeAnimation = -1;
            return;
        }

        if (activeAnimation == index) return;

        activeAnimation = index;
        AnimationElement anim = animationState.get(index);
        anim.reset();
        anim.setLoop(loop);
    }

    public AnimationElement getActiveAnimation() {
        if (activeAnimation < 0 || activeAnimation >= animationState.size())
            return null;
        return animationState.get(activeAnimation);
    }
    
    public void setActiveAnimation(int activeAnimation) {
		this.activeAnimation = activeAnimation;
	}
    
	public int getActiveAnimationIndex() {
		return activeAnimation;
	}

	public Animator getAnimator() {
	    return animator;
	}

	public AnimatedModel getAnimatedModel() {
		return animatedModel;
	}

	public void setAnimatedModel(AnimatedModel animatedModel) {
		this.animatedModel = animatedModel;
	}

	public List<AnimatedModel> getAnimatedModels() {
		return animatedModels;
	}

	public void setAnimatedModels(List<AnimatedModel> animatedModels) {
		this.animatedModels = animatedModels;
	}

	

	
	/*
	public Vector3f getTruePosition() {
		Matrix4f model = new Matrix4f()
    		    .translate(getPosition())
    		    .rotateXYZ(getRotation().x, getRotation().y, getRotation().z)
    		    .scale(getScale());
	}
*/
	
}
