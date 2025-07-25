package settings;

import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.glfw.GLFW.glfwGetKey;

import java.util.HashMap;
import java.util.Map;

import org.joml.Vector2f;

import org.lwjgl.opengl.GL11;

import entities.Entity;
import entities.Light;
import main.Main;
import toolbox.Mesh;

public class EngineSettings {
	
	//General 
	public static boolean pause = false;
	public static boolean grabMouse = true;
	public static boolean overTexture = false;
	
	//OpenGL
	public static boolean FillMode = true;
	public static boolean WireFrameMode = false;
	public static boolean PointMode = false; 
	public static boolean VisualiseObjects = false;
	public static boolean VisualiseLights = false;
	
	//Graphics
	public static DebugMode ShaderDebug = DebugMode.STANDARD_RENDERING;
	
	//Computation
	public static boolean MemoryUsage = false;
	
	//Editor
	public static boolean MouseItemPicker = true;
	public static boolean ObjectPicker = false;
	public static boolean LightPicker = false;
	public static Entity SelectedEntity;
	public static Light SelectedLight;
	
	public static Entity OpenEntity;
	public static Light OpenLight;
	
	
	
	 // Cache to store loaded meshes
    public static final Map<String, Mesh> meshCache = new HashMap<>();
    // Cache to store loaded textures
    public static final Map<String, Integer> textureCache = new HashMap<>();
	
	
	public static boolean keyPressing = false;
	
	public static void updateSettings(long window) {
		
		
		//General
		if (glfwGetKey(window, GLFW_KEY_TAB) == GLFW_PRESS) {
		    if (!keyPressing) { // Only trigger when the key was not previously pressed
		        pause = !pause;
		        grabMouse = !grabMouse;
		       // System.out.println("Pause: " + pause);
		        keyPressing = true; // Mark the key as pressed
		    }
		} else if (glfwGetKey(window, GLFW_KEY_TAB) == GLFW_RELEASE) {
			keyPressing = false; // Reset the state when the key is released
		}
		
		
		
		
		
		//OpenGL
		if (PointMode) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_FILL);
		}
		
		if (WireFrameMode) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
		}
		
		if (PointMode) {
			GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_POINT);
		}
		
		
		
		//Graphics
		
		
		//Computation
		if (MemoryUsage) {
			 long totalMem = Runtime.getRuntime().totalMemory();
		        long freeMem  = Runtime.getRuntime().freeMemory();
		        long usedMem  = totalMem - freeMem;
		        // Convert to MB
		        long usedMB = usedMem / (1024 * 1024);
		        long totalMB= totalMem / (1024 * 1024);

		        // 2) Print
		        System.out.println("FPS: " + Main.currentFPS + " | Used Memory: " + usedMB + " MB / " + totalMB + " MB");
		}
		
		//Selected Entity and Light
		if (SelectedEntity != null)
			OpenEntity = SelectedEntity;
		
		if (SelectedLight != null)
			OpenLight = SelectedLight; 
		
		if (SelectedEntity != null)
			OpenLight = null;
		
		if (SelectedLight != null)
			OpenEntity = null;
		
		
		
	}
	
	
	
	public enum DebugMode {
		STANDARD_RENDERING(0),
	    VISUALIZE_NORMALS(1),
	    VISUALIZE_TEXCOORDS(2),
	    VISUALIZE_DEPTH(3),
	    VISUALIZE_DIFFUSE_LIGHT(4),
	    VISUALIZE_MATERIAL_PROPERTIES(5),
	    VISUALIZE_SURFACE_CURVATURE(6),
	    VISUALIZE_VIEW_VECTOR(7),
	    VISUALIZE_PERFORMANCE_HEAT_MAP(8),
	    VISUALIZE_WORLDPOS(9),
	    VISUALIZE_LIGHTDIR(10);

	    private final int value;

	    private DebugMode(int value) {
	        this.value = value;
	    }

	    public int getValue() {
	        return this.value;
	    }
	}

}

