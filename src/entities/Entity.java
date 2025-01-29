package entities;

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
    private int normalMapId;
    
    private int heighMapId;
 // Parallax
    private Vector3f parallaxScale ;
    
    private int albedoMap;
    private int metallicMap;
    private int roughnessMap;
    private int aoMap;
    
    private float shineDamper = 0;
    private float reflectivity = 0;
    
    private boolean hasTransparency;
    
    
    
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

	public int getAlbedoMap() {
		return albedoMap;
	}

	public void setAlbedoMap(int albedoMap) {
		this.albedoMap = albedoMap;
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

	
}
