package materialLoader;

import org.joml.Vector3f;

public class Material {
	
	private int textureId;
	private int metallicMap;
	private int roughnessMap;
	private int aoMap;
	private int heighMapId;
	private int normalMapId;
	
	private float shineDamper = 0;
	private float reflectivity = 0;

	
	private boolean hasTransparency;
    private boolean hasOpaque;
    
	private Vector3f parallaxScale ;
	
	/*FUTURE ADD IN SHADERS*/
	
	public Material() {}
	

	public Material(Material material) {
		// TODO Auto-generated constructor stub
	}


	public int getTextureId() {
		return textureId;
	}

	public void setTextureId(int textureId) {
		this.textureId = textureId;
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

	public int getHeighMapId() {
		return heighMapId;
	}

	public void setHeighMapId(int heighMapId) {
		this.heighMapId = heighMapId;
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

	public Vector3f getParallaxScale() {
		return parallaxScale;
	}

	public void setParallaxScale(Vector3f parallaxScale) {
		this.parallaxScale = parallaxScale;
	}
	
	
	
    
}
