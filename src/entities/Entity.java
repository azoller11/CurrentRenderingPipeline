package entities;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;

import toolbox.Mesh;

public class Entity {

    private Mesh mesh;

    // Basic transform
    private Vector3f position;
    private Vector3f rotation; // rotation.x => pitch, rotation.y => yaw, rotation.z => roll
    private float scale;
    
    private int textureId;
    
    
  
 // Parallax
    private Vector3f parallaxScale ;
    
    private int metallicMap;
    private int roughnessMap;
    private int aoMap; //Ambient Occlusion
    private int heighMapId;
    private int normalMapId;
    
    private float shineDamper = 0;
    private float reflectivity = 0;
    
    private boolean hasTransparency;
    private boolean hasOpaque;
    
    
    
    public Entity(Mesh mesh, int textureId, Vector3f position, Vector3f rotation, float scale) {
        this.mesh = mesh;
        this.textureId = textureId;
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = scale;
    }

    public Entity(Mesh mesh, Vector3f position, Vector3f rotation, float scale) {
        this.mesh = mesh;
        this.position = new Vector3f(position);
        this.rotation = new Vector3f(rotation);
        this.scale = scale;
    }

    public Mesh getMesh() {
        return mesh;
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

	public int getTextureId() {
		return textureId;
	}

	public void setTextureId(int textureId) {
		textureId = textureId;
	}

	public int getNormalMapId() {
		return normalMapId;
	}

	public void setNormalMapId(int normalMapId) {
		this.normalMapId = normalMapId;
	}

	public float getShineDamper() {
		return shineDamper;
	}

	public void setShineDamper(float shineDamper) {
		this.shineDamper = shineDamper;
	}

	public float getReflectivity() {
		return reflectivity;
	}

	public void setReflectivity(float reflectivity) {
		this.reflectivity = reflectivity;
	}

	public int getHeighMapId() {
		return heighMapId;
	}

	public void setHeighMapId(int heighMapId) {
		this.heighMapId = heighMapId;
	}

	public int getMetallicMap() {
		return metallicMap;
	}

	public void setMetallicMap(int metallicMap) {
		this.metallicMap = metallicMap;
	}

	public int getRoughnessMap() {
		return roughnessMap;
	}

	public void setRoughnessMap(int roughnessMap) {
		this.roughnessMap = roughnessMap;
	}

	public int getAoMap() {
		return aoMap;
	}

	public void setAoMap(int aoMap) {
		this.aoMap = aoMap;
	}

	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}

	public void setRotation(Vector3f rotation) {
		this.rotation = rotation;
	}

	public void setScale(float scale) {
		this.scale = scale;
	}

	public Vector3f getParallaxScale() {
		return parallaxScale;
	}

	public void setParallaxScale(Vector3f parallaxScale) {
		this.parallaxScale = parallaxScale;
	}

	public boolean isHasTransparency() {
		return hasTransparency;
	}

	public void setHasTransparency(boolean hasTransparency) {
		this.hasTransparency = hasTransparency;
	}

	public boolean isHasOpaque() {
		return hasOpaque;
	}

	public void setHasOpaque(boolean hasOpaque) {
		this.hasOpaque = hasOpaque;
	}

	public void setPosition(float x, float y, float z) {
		this.position = new Vector3f(x,y,z);
		
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
	
	/*
	public Vector3f getTruePosition() {
		Matrix4f model = new Matrix4f()
    		    .translate(getPosition())
    		    .rotateXYZ(getRotation().x, getRotation().y, getRotation().z)
    		    .scale(getScale());
	}
*/
	
}
