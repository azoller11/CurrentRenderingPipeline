package entities;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import com.bulletphysics.dynamics.RigidBody;

import toolbox.Mesh;

public class Entity {

	private int Id;
	
	private TexturedModel texturedModel;
	

    // Basic transform
    private Vector3f position;
    private Vector3f rotation; // rotation.x => pitch, rotation.y => yaw, rotation.z => roll
    private float scale;
    
    
    
    
    
    private RigidBody collisionBody;
    private String collisionType = "STATIC";
    private float mass = 0f;
    
    
    public Entity(TexturedModel texturedModel, Vector3f position, Vector3f rotation, float scale) {
        this.texturedModel = texturedModel;
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = scale;
        this.Id = generateNewId();
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
		this.rotation = rotation;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}



	public void setPosition(float x, float y, float z) {
		this.position = new Vector3f(x,y,z);
		
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


	
	/*
	public Vector3f getTruePosition() {
		Matrix4f model = new Matrix4f()
    		    .translate(getPosition())
    		    .rotateXYZ(getRotation().x, getRotation().y, getRotation().z)
    		    .scale(getScale());
	}
*/
	
}
