package main;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import debugRenderer.DebugRenderer;
import entities.Camera;
import entities.Entity;
import entities.Light;
import gui.TextureRenderer;
import loaders.ObjLoader;
import loaders.TextureLoader;
import renderer.MasterRenderer;
import settings.EngineSettings;
import skybox.SkyboxRenderer;
import toolbox.Mesh;
import toolbox.MousePicker;

import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL40.*;
import static org.lwjgl.system.MemoryUtil.NULL;

public class Main {

    private long window;
    private final int width = 900;
    private final int height = 600;
    
    private int frames = 0;
    private double timeCounter = 0.0;
    public static int currentFPS = 0; // optional: store current FPS

    public Random random = new Random();
    
    // The central renderer
    private MasterRenderer masterRenderer;
    // The debug renderer
    private DebugRenderer debugRenderer;
    //Texture renderer
    private TextureRenderer textureRenderer;
    //Skybox renderer
    private SkyboxRenderer skyboxRenderer;
    
    // A simple camera
    private Camera camera;

    // Some example entities
    private List<Entity> entities = new ArrayList<>();
    List<Light> lights = new ArrayList<>();
    
    //Mouse Picker
    private MousePicker picker;

    // Timing
    private double lastTime;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        window = glfwCreateWindow(width, height, "Elk Engine 2", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        glfwMakeContextCurrent(window);
        glfwSwapInterval(1); // vsync
        GL.createCapabilities();

       
        
        // Create the central renderer
        masterRenderer = new MasterRenderer(width, height);

        debugRenderer = new DebugRenderer();
        
        textureRenderer = new TextureRenderer();
        
        skyboxRenderer = new SkyboxRenderer(window);
        
        gui.GuiTexture texture2 = new gui.GuiTexture("peeling-painted-metal_albedo.png");
        
        gui.GuiButton button1 = new gui.GuiButton("colorWheel.png", 100, 100, 100, 100, new Runnable() {
            @Override
            public void run() {
                System.out.println("Button clicked!");
            }
        });
        
      


        // Add Textures to Renderer
     
        
        textureRenderer.addTexture(button1);
        
        // Create the camera, starting at (0,0,5) 
        camera = new Camera(new Vector3f(0,0,0), 0f, 0f);

        // Create a single "cube" mesh
        //Mesh cubeMesh = Mesh.createCube();
        Mesh cubeMesh = ObjLoader.loadObj("crate");
        Mesh boxMesh = ObjLoader.loadObj("sphere");
        Mesh planeMesh = ObjLoader.loadObj("plane");
        
        int TextureId = TextureLoader.loadTexture("peeling-painted-metal_albedo.png");
        int normalTexture = TextureLoader.loadTexture("peeling-painted-metal_normal-ogl.png");
        int heightMapTexture = TextureLoader.loadTexture("peeling-painted-metal_height.png");
        
        int metallicMapTexture = TextureLoader.loadTexture("peeling-painted-metal_metallic.png");
        int roughnessMapTexture = TextureLoader.loadTexture("peeling-painted-metal_height.png");
        int aoMapTexture = TextureLoader.loadTexture("peeling-painted-metal_ao.png");
        
        TextureLoader.loadTexture("peeling-painted-metal_metallic.png");
        TextureLoader.loadTexture("peeling-painted-metal_metallic.png");
        TextureLoader.loadTexture("peeling-painted-metal_metallic.png");
        
        /*
   		private int metallicMap;
    	private int roughnessMap;
    	private int aoMap;
         */
        
        //uniform float parallaxScale = 0.12;
        //uniform int minLayers = 120; 
        //uniform int maxLayers = 160;
        

        // Create multiple entities with different positions
        Entity cube1 = new Entity(planeMesh, TextureLoader.loadTexture("box.png"), new Vector3f(-1, 0, 0), new Vector3f(0,0,0), 1f);
        cube1.setNormalMapId(TextureLoader.loadTexture("boxNormal.png"));
        cube1.setHeighMapId(TextureLoader.loadTexture("boxHeightMap.png"));
        cube1.setParallaxScale(new Vector3f(0.12f, 120, 160));
          //cube1.setReflectivity(0.1f);
          //cube1.setShineDamper(1);
        cube1.setMetallicMap(metallicMapTexture);
        cube1.setAoMap(aoMapTexture);
        cube1.setRoughnessMap(roughnessMapTexture);
        
        
        Entity cube2 = new Entity(cubeMesh, TextureId, new Vector3f(+10, 10, 0), new Vector3f(0,0,0), 1f);
        cube2.setNormalMapId(normalTexture);
        cube2.setHeighMapId(heightMapTexture);
        cube2.setParallaxScale(new Vector3f(0.12f, 120, 160));
        cube2.setReflectivity(10);
        cube2.setShineDamper(100);
        
        Entity cube3 = new Entity(boxMesh, TextureId, new Vector3f(30, 20, 0),  new Vector3f(0,0,0), 1f);
        cube3.setNormalMapId(normalTexture);
        cube3.setHeighMapId(heightMapTexture);
        cube3.setParallaxScale(new Vector3f(0.12f, 120, 160));
        cube3.setMetallicMap(metallicMapTexture);
        cube3.setAoMap(aoMapTexture);
        cube3.setRoughnessMap(roughnessMapTexture);
        
        
        Entity cube4 = new Entity(ObjLoader.loadObj("tallPine4"), TextureLoader.loadTexture("pineTexture3.png"), new Vector3f(5, 0, 0), new Vector3f(0,0,0), 1f);
        //cube4.setNormalMapId(TextureLoader.loadTexture("boxNormal.png"));
        //cube4.setHeighMapId(TextureLoader.loadTexture("boxHeightMap.png"));
        cube4.setReflectivity(0.1f);
        cube4.setShineDamper(1);
        cube4.setHasTransparency(true);
        //cube4.setMetallicMap(metallicMapTexture);
        //cube4.setAoMap(aoMapTexture);
        //cube4.setRoughnessMap(roughnessMapTexture);
        
        Entity cube5 = new Entity(cubeMesh, TextureLoader.loadTexture("colorWheel.png"), new Vector3f(5, 3, 5), new Vector3f(0,0,0), 1f);
        //cube4.setNormalMapId(TextureLoader.loadTexture("boxNormal.png"));
        //cube4.setHeighMapId(TextureLoader.loadTexture("boxHeightMap.png"));
        cube5.setReflectivity(0.1f);
        cube5.setShineDamper(1);
        cube5.setHasOpaque(true);
        //cube4.setMetallicMap(metallicMapTexture);
        //cube4.setAoMap(aoMapTexture);
        //cube4.setRoughnessMap(roughnessMapTexture);

        entities.add(cube1);
        entities.add(cube2);
        entities.add(cube3);
        entities.add(cube4);
        entities.add(cube5);
        
        
        for (int i = 0; i < 0; i++) {
        	int scale = 1000;
        	Entity cubec = new Entity(boxMesh, TextureId, new Vector3f(random.nextInt(1000) - 1000/2, 100,random.nextInt(1000) - 1000/2),  new Vector3f(random.nextInt(90),random.nextInt(90),random.nextInt(90)), 1f);
        	cubec.setNormalMapId(normalTexture);
        	//cubec.setHeighMapId(heightMapTexture);
        	//cubec.setParallaxScale(new Vector3f(0.12f, 120, 160));
        	cubec.setMetallicMap(metallicMapTexture);
        	cubec.setAoMap(aoMapTexture);
        	cubec.setRoughnessMap(roughnessMapTexture);
        	
        	 cube2.setReflectivity(10);
             cube2.setShineDamper(100);
        	entities.add(cubec);
        }
        
        //Sun
        lights.add(new Light(new Vector3f(10000, 20000,0), new Vector3f(2,2,2))); 
        //Moon
        lights.add(new Light(new Vector3f(-10000, -20000,0), new Vector3f(0,0,0))); 
        
	     
        
      
        
        
        // A point light at (2,1,0) with color = (1,0.8,0.7), attenuation(1,0.09,0.032)
	    
       
        lights.add(new Light(
	         new Vector3f(2,0,0),
	         new Vector3f(1.0f, 0.8f, 0.7f),
	         new Vector3f(1, 0.09f, 0.032f)
	     ));
	     
	     lights.add(new Light(
		         new Vector3f(-2,-10,0),
		         new Vector3f(0.0f, 8f, 7f),
		         new Vector3f(1, 0.09f, 0.032f)
		     ));
	     
	     
	     lights.add(new Light(
		         new Vector3f(-20,0,20),
		         new Vector3f(10.0f, 0.0f, 0.7f),
		         new Vector3f(1, 0.09f, 0.032f)
		     ));
	     
	     
	     lights.add(new Light(
		         new Vector3f(0,10,0),
		         new Vector3f(10.0f, 10.0f, 0.7f),
		         new Vector3f(1, 0.09f, 0.032f)
		     ));
	     
	     
	     gui.GuiTexture texture1 = new gui.GuiTexture(12, 0.0f,0.0f, 100.0f, 100.0f);
	        textureRenderer.addTexture(texture1);
	     
	     //Mouse picker
	     picker = new MousePicker(width, height, camera, masterRenderer.getProjectionMatrix(), entities, lights);
	        

        // Basic GL states
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.2f, 0.3f, 0.4f, 1.0f);
        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        lastTime = glfwGetTime();
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();
            double currentTime = glfwGetTime();
            float deltaTime = (float)(currentTime - lastTime);
            lastTime = currentTime;
            frames++;
            timeCounter += deltaTime;
            // Once we've accumulated 1 second or more, compute FPS
            if (timeCounter >= 1.0) {
                currentFPS = frames;
                frames = 0;
                timeCounter -= 1.0;
            }
            
            
            //Do sun movement
            
            
            
            // Get Mouse Position
            double[] mouseX = new double[1];
            double[] mouseY = new double[1];
            glfwGetCursorPos(window, mouseX, mouseY);

            // Adjust Y-coordinate since OpenGL uses bottom-left as (0,0)
            double adjustedMouseY = height - mouseY[0];

            // Update camera (WASD + mouse)
            camera.handleInput(window, deltaTime);

            // Example: rotate the second cube around Y
            if (EngineSettings.VisualiseObjects) {
            	for (Entity e : entities) {
                	debugRenderer.addSphere(e.getPosition(), e.getMesh().getFurthestPoint(), new Vector3f(0,0,1));
                }
            }
            
            
            if (EngineSettings.VisualiseLights) {
            	for (Light e : lights) {
                	debugRenderer.addSphere(e.getPosition(), 1,e.getColor());
                }
            }
            
            if (!EngineSettings.grabMouse && EngineSettings.MouseItemPicker && !EngineSettings.overTexture)
            	picker.update(window);
            
            
            
            
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Render everything
            masterRenderer.render(entities, lights, camera);

            // Could add more interesting transforms as well
            debugRenderer.render(camera, masterRenderer.getProjectionMatrix(), camera.getViewMatrix());
            
            skyboxRenderer.render(camera.getViewMatrix(), masterRenderer.getProjectionMatrix(), lights.get(0),lights.get(1), 1000);            
            
            
            //Render Texture
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            textureRenderer.render(masterRenderer.getFlatProjection(), camera.getFlatViewMatrix(), mouseX[0], adjustedMouseY);
          
            EngineSettings.updateSettings(window);
            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        // Cleanup
        masterRenderer.cleanup();
        skyboxRenderer.cleanUp();
        debugRenderer.cleanup();
        textureRenderer.cleanUp();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
