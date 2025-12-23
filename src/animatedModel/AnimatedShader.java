package animatedModel;

import java.util.List;

import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;
import org.lwjgl.util.vector.Vector4f;

import animatedModel.Bone;
import entities.Camera;
import entities.Light;
import shaders.ShaderProgram;
import toolbox.Maths;

public class AnimatedShader extends ShaderProgram{

private static final int MAX_LIGHTS = 4;
private static final int MAX_JOINTS = 50;
	
	private static final String VERTEX_FILE = "src/animatedModel/vertexShader.txt";
	private static final String FRAGMENT_FILE = "src/animatedModel/fragmentShader.txt";
	
	private int location_transformationMatrix;
	private int location_projectionMatrix;
	private int location_viewMatrix;
	private int location_lightPosition[];
	private int location_lightColor[];
	private int location_attenuation[];
	private int location_shineDamper;
	private int location_reflectivity;
	private int location_useFakeLighting;
	private int location_skyColor; 
	private int location_numberOfRows;
	private int location_offset;
	private int location_jointTransforms[]; 
	private int location_clipPlane;
	private int[] boneTransforms;
	private int location_useBones;
	
	public AnimatedShader() {
		super(VERTEX_FILE, FRAGMENT_FILE);
	}

	@Override
	protected void bindAttributes() {
		super.bindAttribute(0, "position");
		super.bindAttribute(1, "textureCoords");
		super.bindAttribute(2, "normal");
	}

	@Override
	protected void getAllUniformLocations() {
		location_transformationMatrix = super.getUniformLocation("transformationMatrix");
		location_projectionMatrix = super.getUniformLocation("projectionMatrix");
		location_viewMatrix = super.getUniformLocation("viewMatrix");
		location_shineDamper = super.getUniformLocation("shineDamper");
		location_reflectivity = super.getUniformLocation("reflectivity");
		location_useFakeLighting = super.getUniformLocation("useFakeLighting");
		location_skyColor = super.getUniformLocation("skyColor");
		location_numberOfRows = super.getUniformLocation("numberOfRows");
		location_offset = super.getUniformLocation("offset");
		location_clipPlane = super.getUniformLocation("clipPlane");
		
		location_lightPosition = new int[MAX_LIGHTS];
		location_lightColor = new int[MAX_LIGHTS];
		location_attenuation = new int[MAX_LIGHTS];
		for(int i=0; i < MAX_LIGHTS; i++) {
			location_lightPosition[i] = super.getUniformLocation("lightPosition["+i+"]");
			location_lightColor[i] = super.getUniformLocation("lightColor["+i+"]");
			location_attenuation[i] = super.getUniformLocation("attenuation[" + i + "]");
		}
		
		location_jointTransforms = new int[MAX_JOINTS];
		for(int i=0; i<MAX_JOINTS; i++) {
			location_jointTransforms[i] = super.getUniformLocation("jointTransforms[" + i + "]");
		}
		
	    location_useBones = super.getUniformLocation("useBones"); // Retrieve useBones location

		
		
	}
	
	public void loadCLipPlane(Vector4f plane) {
		super.load4DVector(location_clipPlane, plane);
	}
	
	public void loadShineVariables(float damper, float refectivity) {
		super.loadFloat(location_shineDamper, damper);
		super.loadFloat(location_reflectivity, refectivity);
	}
	
	public void loadNumberOfRows(int numberOfRows) {
		super.loadFloat(location_numberOfRows, numberOfRows);
	}
	
	public void loadOffset(float x, float y) {
		super.load2DVector(location_offset, new Vector2f(x, y));
	}
	
	public void loadSkyColor(float red, float green, float blue) {
		super.loadVector(location_skyColor, new Vector3f(red, green, blue));
	}
	
	public void loadFakeLightingVariable(boolean useFakeLighting) {
		super.loadBoolean(location_useFakeLighting, useFakeLighting);
	}
	
	public void loadTransformationMatrix(Matrix4f matrix) {
		super.loadMatrix(location_transformationMatrix, matrix);
	}
	
	public void loadLights(List<Light> lights) {
		for(int i = 0; i < MAX_LIGHTS; i++) {
			if(i < lights.size()) {
				super.loadVector(location_lightPosition[i], lights.get(i).getPosition());
				super.loadVector(location_lightColor[i], lights.get(i).getColour());
				super.loadVector(location_attenuation[i], lights.get(i).getAttenuation());
			}else {
				super.loadVector(location_lightPosition[i], new Vector3f(0, 0, 0));
				super.loadVector(location_lightColor[i], new Vector3f(0, 0, 0));
				super.loadVector(location_attenuation[i], new Vector3f(1, 0, 0));
			}
		}
	}
	
  public void loadBoneTransforms(Bone[] bones)
    {
        for (int i = 0; i < bones.length; i++)
            super.loadMatrix(this.boneTransforms[i], bones[i].getTransformation());
    }

  public void loadUseBones(boolean useBones) {
	    super.loadBoolean(location_useBones, useBones);
	} 
	  
	public void loadViewMatrix(Camera camera) {
		Matrix4f viewMatrix = Maths.createViewMatrix(camera);
		super.loadMatrix(location_viewMatrix, viewMatrix);
	}
	
	public void loadProjectionMatrix(Matrix4f projection) {
		super.loadMatrix(location_projectionMatrix, projection);
	}
}