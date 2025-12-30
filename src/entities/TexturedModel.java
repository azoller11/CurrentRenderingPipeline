package entities;

import java.util.ArrayList;
import java.util.List;

import org.joml.Vector3f;
import org.lwjgl.assimp.AIAnimation;

import animatedModel.AnimatedModel;
import animatedModel.AnimationElement;
import toolbox.Mesh;

public class TexturedModel {
	
	private Mesh mesh;
	private AnimatedModel animatedModel;
	 
	private int textureId;
	 
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
    
    private boolean castShadows;
    
    
    
    private boolean isVegitation;
    private boolean useFakeLighting;
 
    private List<AnimationElement> animationElements;


	public TexturedModel(Mesh mesh, int textureId) {
		super();
		this.mesh = mesh;
		this.textureId = textureId;
	}
	
	public TexturedModel(AnimatedModel animatedModel, int textureId) {
		super();
		this.animatedModel = animatedModel;
		this.textureId = textureId;
		//Load the animation elements
		generateAnimations();
	}


	public TexturedModel() {
		// TODO Auto-generated constructor stub
	}

	

	public AnimatedModel getAnimatedModel() {
		return animatedModel;
	}

	public void setAnimatedModel(AnimatedModel animatedModel) {
		this.animatedModel = animatedModel;
		generateAnimations();
	}

	public Mesh getMesh() {
		return mesh;
	}


	public void setMesh(Mesh mesh) {
		this.mesh = mesh;
	}


	public int getTextureId() {
		return textureId;
	}


	public void setTextureId(int textureId) {
		this.textureId = textureId;
	}


	public Vector3f getParallaxScale() {
		return parallaxScale;
	}


	public void setParallaxScale(Vector3f parallaxScale) {
		this.parallaxScale = parallaxScale;
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


	public boolean isCastShadows() {
		return castShadows;
	}


	public void setCastShadows(boolean castShadows) {
		this.castShadows = castShadows;
	}


	public void generateAnimations() {
		animationElements = new ArrayList<>();
		 int index = 0;
		 for (AIAnimation a : animatedModel.getAnimations()) {
	        	animationElements.add(new AnimationElement(index, 0));
	        	System.out.println("New ANIMATION!");
	        	System.out.println("boneCount: " + animatedModel.getBones().length);
	        	index++;
	        }
	}
    
	   
		public List<AnimationElement> getAnimationElements() {
			return animationElements;
		}

	public void setAnimationElements(List<AnimationElement> animationElements) {
		this.animationElements = animationElements;
	}

		public boolean isVegitation() {
			return isVegitation;
		}


		public void setVegitation(boolean isVegitation) {
			this.isVegitation = isVegitation;
		}


		public boolean isUseFakeLighting() {
			return useFakeLighting;
		}


		public void setUseFakeLighting(boolean useFakeLighting) {
			this.useFakeLighting = useFakeLighting;
		}
    

}
