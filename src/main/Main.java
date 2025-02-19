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
import loaders.SceneLoader;
import loaders.TextureLoader;
import postProcessing.BloomRenderer;
import postProcessing.PostProcessingRenderer;
import renderer.MasterRenderer;
import settings.EngineSettings;
import shadows.ShadowRenderer;
import skybox.SkyboxRenderer;
import terrain.TerrainRenderer;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;
import text.Font;
import terrain.TerrainGenerator;
import terrain.AdaptiveTerrainGenerator;
import toolbox.Equations;
import toolbox.Mesh;
import toolbox.MousePicker;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.HashMap;
import java.util.Map;


import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL11.GL_COLOR_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.GL_DEPTH_BUFFER_BIT;
import static org.lwjgl.opengl.GL11.glClear;
import static org.lwjgl.opengl.GL11.glViewport;
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
    
    private TextRenderer textRenderer;
    //Skybox renderer
    private SkyboxRenderer skyboxRenderer;
    //Main post processing renderer
    private PostProcessingRenderer postRenderer;
    //Bloom
    private BloomRenderer bloomRenderer;
    //Terrain
    private TerrainRenderer terrainRenderer;
    private Mesh terrainMesh;
    private Matrix4f terrainModelMatrix;
    private AdaptiveTerrainGenerator adaptiveGen;
    //
    private ShadowRenderer shadowRenderer;
    //
    //private ShadowMapRenderer shadowMapRenderer;
    
    
    // A simple camera
    private Camera camera;

    // Some example entities
    private List<Entity> entities = new ArrayList<>();
    List<Light> lights = new ArrayList<>();
    
    //Mouse Picker
    private MousePicker picker;

    // Timing
    private double lastTime;
    
    private gui.GuiTexture loadingScreen;

    public static void main(String[] args) {
        new Main().run();
    }

    private void run() {
    	firstLoop();
        init();
        loop();
        cleanup();
    }

    private void firstLoop() {
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
       
       textureRenderer = new TextureRenderer();
       loadingScreen = new gui.GuiTexture(TextureLoader.loadExplicitTexture("ElkEngine.png"), 0, 0, width,height);
       
       textureRenderer.addTexture(loadingScreen);

       // Clear both color and depth buffers
       glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
       glViewport(0, 0, width, height); // Ensure viewport is correct
       glEnable(GL_BLEND);
       glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
       
       // Render loading screen
       textureRenderer.render(new Matrix4f().ortho2D(0.0f, width, 0.0f, height), new Matrix4f().identity(), 0, 0);
       
       // Swap buffers to display the loading screen immediately
       glfwSwapBuffers(window);
       
       
   }
    
    
    private void init() {
       

        camera = new Camera(new Vector3f(0,0,0), 0f, 0f);
        
        // Create the central renderer
        masterRenderer = new MasterRenderer(width, height);

        debugRenderer = new DebugRenderer();
        
        
        
        Font font = new Font("res/verdana.fnt", "verdana.png");
  
        textRenderer = new TextRenderer(font, 100);
        
        textRenderer.setTextColor(1.9f,1.9f, 1.9f, 1.0f);
        textRenderer.setOutlineColor(0.0f, 0.0f, 0.0f, 0.0f);
        textRenderer.setEdgeSmoothness(0.5f);
        textRenderer.setOutlineWidth(0.0f);
        
        
        skyboxRenderer = new SkyboxRenderer(window);
        
        postRenderer = new PostProcessingRenderer(width, height, 1);
        
        bloomRenderer = new BloomRenderer(width, height);
        
        
     // Initialize the ShadowMapRenderer
        shadowRenderer = new ShadowRenderer(2048 * 16,2048 * 16);
       
        //shadowMapRenderer = new ShadowMapRenderer(camera);
        
        gui.GuiTexture texture2 = new gui.GuiTexture(8, 0, 100, 50,50);
        textureRenderer.addTexture(texture2);
        
        gui.GuiTexture texture12 = new gui.GuiTexture(9, 60, 100, 50,50);
        textureRenderer.addTexture(texture12);
        
       
        
        gui.GuiButton button1 = new gui.GuiButton("cube.png", 0, 0, 50, 50, new Runnable() {
            @Override
            public void run() {
                //EngineSettings.VisualiseObjects = !EngineSettings.VisualiseObjects;
                EngineSettings.ObjectPicker = !EngineSettings.ObjectPicker;
            }
        });
        textureRenderer.addTexture(button1);
        
        gui.GuiButton button2 = new gui.GuiButton("idea.png", 50, 0, 50, 50, new Runnable() {
            @Override
            public void run() {
                EngineSettings.VisualiseLights = !EngineSettings.VisualiseLights;
                EngineSettings.LightPicker = !EngineSettings.LightPicker;
            }
        });
        
        textureRenderer.addTexture(button2);
        
        gui.GuiButton button3 = new gui.GuiButton("colorWheel.png", 100, 0, 50, 50, new Runnable() {
            @Override
            public void run() {
                EngineSettings.MemoryUsage = !EngineSettings.MemoryUsage;
            }
        });
        
        textureRenderer.addTexture(button3);
        
        
        
        
      //Mesh cubeMesh = Mesh.createCube();
        Mesh cubeMesh = ObjLoader.loadObj("crate");
        Mesh sphereMesh = ObjLoader.loadObj("sphere");
        Mesh planeMesh = ObjLoader.loadObj("plane");
        
        int TextureId = TextureLoader.loadTexture("peeling-painted-metal_albedo.png");
        int normalTexture = TextureLoader.loadTexture("peeling-painted-metal_normal-ogl.png");
        int heightMapTexture = TextureLoader.loadTexture("peeling-painted-metal_height.png");
        
        int metallicMapTexture = TextureLoader.loadTexture("peeling-painted-metal_metallic.png");
        int roughnessMapTexture = TextureLoader.loadTexture("peeling-painted-metal_height.png");
        int aoMapTexture = TextureLoader.loadTexture("peeling-painted-metal_ao.png");
        
        
        /*
   		private int metallicMap;
    	private int roughnessMap;
    	private int aoMap;
         */
        
        //uniform float parallaxScale = 0.12;
        //uniform int minLayers = 120; 
        //uniform int maxLayers = 160;
        


        
        
        Entity cube2 = new Entity(cubeMesh, TextureId, new Vector3f(+10, 10, 0), new Vector3f(0,0,0), 1f);
        cube2.setNormalMapId(normalTexture);
        cube2.setHeighMapId(heightMapTexture);
        cube2.setParallaxScale(new Vector3f(0.12f, 120, 160));
        cube2.setMetallicMap(metallicMapTexture);
        cube2.setAoMap(aoMapTexture);
        cube2.setRoughnessMap(roughnessMapTexture);
        
        Entity cube3 = new Entity(sphereMesh, TextureId, new Vector3f(30, 20, 0),  new Vector3f(0,0,0), 1f);
        cube3.setNormalMapId(normalTexture);
        cube3.setHeighMapId(heightMapTexture);
        cube3.setParallaxScale(new Vector3f(0.12f, 120, 160));
        cube3.setMetallicMap(metallicMapTexture);
        //cube3.setAoMap(aoMapTexture);
        //cube3.setRoughnessMap(roughnessMapTexture);
        
        
        Entity cube4 = new Entity(ObjLoader.loadObj("tallPine4"), TextureLoader.loadTexture("pineTexture3.png"), new Vector3f(5, 0, 0), new Vector3f(0,0,0), 1f);
        //cube4.setNormalMapId(TextureLoader.loadTexture("boxNormal.png"));
        //cube4.setHeighMapId(TextureLoader.loadTexture("boxHeightMap.png"));
        //cube4.setReflectivity(0.1f);
        //cube4.setShineDamper(1);
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

        
        
        
        Entity cube6 = new Entity(planeMesh, TextureLoader.loadTexture("medieval_red_brick_diff_2k.png"), new Vector3f(15, 5, 15), new Vector3f(0,0,0), 1f);
        cube6.setNormalMapId(TextureLoader.loadTexture("medieval_red_brick_nor_gl_2k.png"));
        cube6.setHeighMapId(TextureLoader.loadTexture("medieval_red_brick_disp_2k.png"));
        cube6.setParallaxScale(new Vector3f(0.05f, 120, 160));
        cube6.setMetallicMap(TextureLoader.loadTexture("medieval_red_brick_rough_2k.png"));
        cube6.setAoMap(TextureLoader.loadTexture("medieval_red_brick_ao_2k.png"));
        cube6.setRoughnessMap(TextureLoader.loadTexture("medieval_red_brick_rough_2k.png"));
        
        
        
        
        Entity cube7 = new Entity(planeMesh, TextureLoader.loadTexture("ganges_river_pebbles_diff_2k.png"), new Vector3f(15, 25, 15), new Vector3f(0,0,0), 1f);
        cube7.setNormalMapId(TextureLoader.loadTexture("ganges_river_pebbles_nor_gl_2k.png"));
        cube7.setHeighMapId(TextureLoader.loadTexture("ganges_river_pebbles_disp_2k.png"));
        cube7.setParallaxScale(new Vector3f(0.05f, 120, 160));
        cube7.setMetallicMap(TextureLoader.loadTexture("ganges_river_pebbles_rough_2k.png"));
        cube7.setAoMap(TextureLoader.loadTexture("ganges_river_pebbles_ao_2k.png"));
        cube7.setRoughnessMap(TextureLoader.loadTexture("ganges_river_pebbles_rough_2k.png"));
        entities.add(cube7);
        
        
        Entity cube8 = new Entity(planeMesh, TextureLoader.loadTexture("blue_metal_plate_diff_2k.png"), new Vector3f(15, 45, 15), new Vector3f(0,0,0), 1f);
        cube8.setNormalMapId(TextureLoader.loadTexture("blue_metal_plate_nor_gl_2k.png"));
        cube8.setHeighMapId(TextureLoader.loadTexture("blue_metal_plate_disp_2k.png"));
        cube8.setParallaxScale(new Vector3f(0.05f, 120, 160));
        cube8.setMetallicMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
        cube8.setAoMap(TextureLoader.loadTexture("blue_metal_plate_ao_2k.png"));
        cube8.setRoughnessMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
        entities.add(cube8);
        
        Entity cube9 = new Entity(planeMesh, TextureLoader.loadTexture("roots_diff_2k.png"), new Vector3f(15, 15, 85), new Vector3f(0,0,0), 1f);
        cube9.setNormalMapId(TextureLoader.loadTexture("roots_nor_gl_2k.png"));
        cube9.setHeighMapId(TextureLoader.loadTexture("roots_disp_2k.png"));
        cube9.setParallaxScale(new Vector3f(0.09f, 120, 160));
        cube9.setMetallicMap(TextureLoader.loadTexture("roots_rough_2k.png"));
        cube9.setAoMap(TextureLoader.loadTexture("roots_ao_2k.png"));
        cube9.setRoughnessMap(TextureLoader.loadTexture("roots_rough_2k.png"));
        entities.add(cube9);
        
        Entity cube10 = new Entity(cubeMesh, TextureLoader.loadTexture("blue_metal_plate_diff_2k.png"), new Vector3f(0, 45, 0), new Vector3f(0,0,0), 1f);
        cube10.setNormalMapId(TextureLoader.loadTexture("blue_metal_plate_nor_gl_2k.png"));
        cube10.setHeighMapId(TextureLoader.loadTexture("blue_metal_plate_disp_2k.png"));
        cube10.setParallaxScale(new Vector3f(0.15f, 120, 160));
        cube10.setMetallicMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
        cube10.setAoMap(TextureLoader.loadTexture("blue_metal_plate_ao_2k.png"));
        cube10.setRoughnessMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
        entities.add(cube10);
        
        
        for (int i = 0; i < 20; i++) {
        	Entity bush = new Entity(ObjLoader.loadObj("bush1"), TextureLoader.loadTexture("searsia_lucida_diff_2k.png"),
        			new Vector3f(-30 + random.nextInt(60), 15, 60 + random.nextInt(60)), 
        			new Vector3f(0,0,0), random.nextFloat(5) + 10);
            bush.setNormalMapId(TextureLoader.loadTexture("searsia_lucida_nor_gl_2k.png"));
            //bush.setMetallicMap(TextureLoader.loadTexture("searsia_lucida_rough_2k.png"));
            bush.setAoMap(TextureLoader.loadTexture("searsia_lucida_ao_2k.png"));
            bush.setRoughnessMap(TextureLoader.loadTexture("searsia_lucida_rough_2k.png"));
            bush.setHasTransparency(true);
            bush.setHasOpaque(false);
            entities.add(bush);
            
        }
        
        
        
        
        
        
        //cube6.setReflectivity(0.1f);
        //cube6.setShineDamper(1);
        entities.add(cube6);

        entities.add(cube2);
        entities.add(cube3);
        entities.add(cube4);
        entities.add(cube5);
        
        
        for (int i = 0; i < 300; i++) {
        	int scale = 6000;
        	Entity cubec = new Entity(sphereMesh, TextureId, new Vector3f(random.nextInt(scale) - scale/2, 300,random.nextInt(scale) - scale/2),  new Vector3f(random.nextInt(90),random.nextInt(90),random.nextInt(90)), random.nextFloat(10));
        	cubec.setNormalMapId(normalTexture);
        	//cubec.setHeighMapId(heightMapTexture);
        	//cubec.setParallaxScale(new Vector3f(0.12f, 120, 160));
        	cubec.setMetallicMap(metallicMapTexture);
        	cubec.setAoMap(aoMapTexture);
        	cubec.setRoughnessMap(roughnessMapTexture);
        	
        	 cube2.setReflectivity(10);
             cube2.setShineDamper(100);
        	entities.add(cubec);
        	
        	cubec.setPosition(cubec.getPosition().x, cubec.getPosition().y+100, cubec.getPosition().z);
        	entities.add(cubec);
        	
        }

      
        
        entities.addAll(SceneLoader.loadScene("sponza.obj", "sponza.mtl"));
        
    
        
        
        
        //Sun
        Light sun = new Light(new Vector3f(10000, 20000,0), new Vector3f(12,12,12));
        lights.add(sun); 
        //Moon
        Light moon = new Light(new Vector3f(-10000, -20000,0), new Vector3f(0,0,0)); 
        lights.add(moon); 
        
        lights.add(new Light(
   	         new Vector3f(2,0,0),
   	         new Vector3f(1.0f, 0.8f, 0.7f),
   	         new Vector3f(1, 0.62f, 0.0032f)
   	     ));
   	     
   	     lights.add(new Light(
   		         new Vector3f(-2,-10,0),
   		         new Vector3f(0.0f, 8f, 7f),
   		         new Vector3f(1, 0.62f, 0.0032f)
   		     ));
   	     
   	     
   	     lights.add(new Light(
   		         new Vector3f(-20,0,20),
   		         new Vector3f(10.0f, 0.0f, 0.7f),
   		         new Vector3f(1, 0.62f, 0.232f)
   		     ));
   	     
   	     
   	     lights.add(new Light(
   		         new Vector3f(0,10,0),
   		         new Vector3f(10.0f, 10.0f, 0.7f),
   		         new Vector3f(1, 0.62f, 0.232f)
   		     ));
      
   	     
   	  Light fakesun = new Light(new Vector3f(0, 20,0), new Vector3f(12,12,12),new Vector3f(1, 0.62f, 0.232f));
      lights.add(fakesun); 
    
	     picker = new MousePicker(width, height, camera, masterRenderer.getProjectionMatrix(), entities, lights);
	        

        // Basic GL states
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.2f, 0.3f, 0.4f, 1.0f);
        //GL11.glPolygonMode(GL11.GL_FRONT_AND_BACK, GL11.GL_LINE);
        textureRenderer.removeTexture(loadingScreen);
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
                	debugRenderer.addSphere(e.getPosition(), e.getMesh().getFurthestPoint() + e.getScale(), new Vector3f(0,1,0));
                }
            }
            
            
            if (EngineSettings.VisualiseLights) {
            	for (Light e : lights) {
                	debugRenderer.addSphere(e.getPosition(), 1 ,e.getColor());
                }
            }
            
            if (!EngineSettings.grabMouse && EngineSettings.MouseItemPicker && !EngineSettings.overTexture) {
            	picker.update(window);
            	picker.drawDebug(debugRenderer);
            	picker.drawRotationDebug(debugRenderer);
            	
         
            	
            }
            	
            
            
            
            
            
            
        	//System.out.println(shadowTextureID);
            
            if (skyboxRenderer.isSunOut()) {
            	 shadowRenderer.renderShadowMap(entities,
                 		shadowRenderer.createLightSpaceMatrix(lights.get(0), camera), 
                 		camera.getViewMatrix(),  masterRenderer.getProjectionMatrix());
            } else {
            	 shadowRenderer.renderShadowMap(entities,
                 		shadowRenderer.createLightSpaceMatrix(lights.get(1), camera), 
                 		camera.getViewMatrix(),  masterRenderer.getProjectionMatrix());
            }
            
           
            int shadowTextureID = shadowRenderer.getDepthMapTexture();
            //System.out.println(shadowTextureID);
            int err = glGetError();
            
            //shadowMapRenderer.render(entities, lights.get(lights.size() - 1), camera);
            //System.out.println(shadowMapRenderer.getShadowMap());
            
            postRenderer.bindFBO();
            bloomRenderer.bindSceneFBO();
            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            
          

         // (Optional) Output the texture ID to the console.
         //System.out.println("Shadow Texture ID: " + shadowTextureID);
       
            
        
            // Render everything
            masterRenderer.render(entities, lights, camera, shadowTextureID);
         // Assuming you have projection, view, and model matrices available.
          
            //terrainRenderer.renderAdaptiveTerrain(adaptiveGen, masterRenderer.getProjectionMatrix(), camera.getViewMatrix(), terrainModelMatrix, camera.getPosition(), lights);

            
            
            // Could add more interesting transforms as well
            debugRenderer.render(camera, masterRenderer.getProjectionMatrix(), camera.getViewMatrix());
            
            skyboxRenderer.render(camera, camera.getViewMatrix(), masterRenderer.getProjectionMatrix(), lights.get(0),lights.get(1), 1000000);            
           

            
            postRenderer.unbindFBO(width, height);
            bloomRenderer.unbindSceneFBO(width, height);
            
            postRenderer.renderPostProcess();
            bloomRenderer.renderBloom(width, height, 1.8f, 1.8f);
            
            //int c = Equations.combineTexturesFixed(3, 4, width, height);
            //System.out.print(c);
            
            //Render Texture
            glClear(GL_DEPTH_BUFFER_BIT);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            textureRenderer.render(masterRenderer.getFlatProjection(), camera.getFlatViewMatrix(), mouseX[0], adjustedMouseY);
          
            textRenderer.renderText(""+Main.currentFPS, 0, height - 20, 0.25f, masterRenderer.getFlatProjection(), width, TextAlignment.LEFT);
            
            
            if (!EngineSettings.grabMouse) {
            	
            	
            	if (EngineSettings.OpenEntity != null) {
                    textRenderer.renderText(""+EngineSettings.OpenEntity.getId(), 0, height - 120, 0.25f, masterRenderer.getFlatProjection(), width, TextAlignment.LEFT);
                    textRenderer.renderText("Pos: "+EngineSettings.OpenEntity.getPosition(), 0, height - 140, 0.25f, masterRenderer.getFlatProjection(), width, TextAlignment.LEFT);

            	}
            	
            	if (EngineSettings.OpenLight != null) {
                    textRenderer.renderText(""+EngineSettings.OpenLight.getId(), 0, height - 120, 0.25f, masterRenderer.getFlatProjection(), width, TextAlignment.LEFT);

                    textRenderer.renderText(""+EngineSettings.OpenLight.getPosition(), 0, height - 140, 0.25f, masterRenderer.getFlatProjection(), width, TextAlignment.LEFT);

            	}
            }
            
           	
            
            
            EngineSettings.updateSettings(window);
            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        // Cleanup
    	//terrainRenderer.cleanup();
    	textRenderer.cleanUp();
    	//postRenderer.cleanup();
        masterRenderer.cleanup();
        skyboxRenderer.cleanUp();
        debugRenderer.cleanup();
        textureRenderer.cleanUp();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
