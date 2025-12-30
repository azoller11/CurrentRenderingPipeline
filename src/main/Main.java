package main;

import org.lwjgl.*;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import com.bulletphysics.dynamics.RigidBody;

import animatedModel.AnimatedModel;
import animatedModel.AnimationLoader;
import debugRenderer.DebugRenderer;
import decals.Decal;
import decals.DecalManager;
import decals.DecalRenderer;
import entities.Camera;
import entities.Entity;
import entities.Player;
import entityManager.EntityManager;
import entities.Light;
import gui.GuiTexture;
import gui.TextureRenderer;
import guiManager.GuiManager;
import grass.GrassRenderer;
import grass.Grass;
import loaders.ObjLoader;
import loaders.SceneLoader;
import loaders.SceneLoader2;
import loaders.TextureLoader;
import physics.HitResult;
import physics.PhysicsManager;
import postProcessing.BloomRenderer;
import postProcessing.PostProcessingRenderer;
import particles.ParticleMaster;
import particles.ParticleSystem;
import particles.ParticleTexture;
import renderer.MasterRenderer;
import settings.EngineSettings;
import settings.EngineSettings.TerrainBrushColor;
import shadows.ShadowRenderer;
import skybox.SkyboxRenderer;
import terrain.TerrainRenderer;
import terrain.TerrainTextureLayer;
import terrain.Terrain;
import terrain.TerrainEditor;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;
import text.Font;
import toolbox.Equations;
import toolbox.Mesh;
import toolbox.MousePicker;

import org.joml.Matrix4f;
import org.joml.Vector2f;
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
    public static int MAX_FPS = 250;   // set to -1 for uncapped
    
    float outsideAmbience = 0;

    public Random random = new Random(2342342);
    
    // The central renderer
    private MasterRenderer masterRenderer;
    // The debug renderer
    private DebugRenderer debugRenderer;
    //Texture renderer
    private TextureRenderer textureRenderer;
    
    private TextRenderer textRenderer;
    
    private GuiManager guiManager;
    //Skybox renderer
    private SkyboxRenderer skyboxRenderer;
    //Main post processing renderer
    private PostProcessingRenderer postRenderer;
    //Bloom
    private BloomRenderer bloomRenderer;
    
    private TerrainEditor terrainEditor;
    
    
    //ENTITIES
    public static EntityManager entityManager;
    
    
    private DecalManager decalManager;
    private DecalRenderer decalRenderer;
    
    //
    //private PhysicsManager physicsManager;
    //
    GrassRenderer grassRenderer;
     
    
    TerrainRenderer terrainRenderer;
    
    Terrain terrain;
    
    ParticleTexture fireTexture;
    private ParticleTexture smokeTexture;
    private ParticleSystem smokeSystem;


    //
    private ShadowRenderer shadowRenderer;
    //
    //private ShadowMapRenderer shadowMapRenderer;
    
    
    // A simple camera
    private Camera camera;
    
    private Player player;

    // Some example entities
    //private List<Entity> entities = new ArrayList<>();
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
       glfwWindowHint(GLFW_SAMPLES, 4); // Request 4x MSAA

       window = glfwCreateWindow(width, height, "Elk Engine 2", NULL, NULL);
       if (window == NULL) {
           throw new RuntimeException("Failed to create the GLFW window");
       }
       glfwMakeContextCurrent(window);
       glfwSwapInterval(0); // vsync
       GL.createCapabilities();
       
    // Enable multisampling
       glEnable(GL_MULTISAMPLE);
       
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
    	
    	entityManager = new EntityManager();
    	
    	
    	

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
        
        guiManager = new GuiManager();
        
        
        guiManager.init(textureRenderer, masterRenderer);
        
        
        skyboxRenderer = new SkyboxRenderer(window);
        
        skyboxRenderer.setSkyboxTextures(new int[] {
        	    TextureLoader.loadExplicitTexture("sFront.png"),  // +X
        	    TextureLoader.loadExplicitTexture("sBack.png"),   // -X
        	    TextureLoader.loadExplicitTexture("sDown.png"),    // +Y
        	    TextureLoader.loadExplicitTexture("sUp.png"), // -Y
        	    TextureLoader.loadExplicitTexture("sRight.png"),  // +Z
        	    TextureLoader.loadExplicitTexture("sLeft.png")    // -Z
        	});
        skyboxRenderer.setSkyboxBlend(1.0f);
        
        
        postRenderer = new PostProcessingRenderer(width, height, 1);
        
        bloomRenderer = new BloomRenderer(width, height);
        
        terrainEditor = new TerrainEditor();
        
        terrainRenderer = new TerrainRenderer();
        
        float terrainSize = 3000;
        float terrainTileScale = 35;
        float terrainHeight = 180;
        
        
        terrain = new Terrain(
                50, 250,
                terrainSize,           // terrain size
                terrainHeight,            // max height
                "heightmap perlin.png"
        );
        terrain.setBlendMap("blend_map.png");
        
        
        
        terrain.setTextureLayers(new TerrainTextureLayer[3]);
        
        TerrainTextureLayer a = new TerrainTextureLayer(TextureLoader.loadTexture("roots_diff_2k.png"), terrainTileScale, 0,0,0);
        a.setNormalMap(TextureLoader.loadTexture("roots_nor_gl_2k.png"));
        a.setHeightMap(TextureLoader.loadTexture("roots_disp_2k.png"));
        a.setMetallicMap(TextureLoader.loadTexture("roots_rough_2k.png"));
        a.setRoughnessMap(TextureLoader.loadTexture("roots_rough_2k.png"));
        a.setAoMap(TextureLoader.loadTexture("roots_ao_2k.png"));
        
        TerrainTextureLayer b = new TerrainTextureLayer(TextureLoader.loadTexture("debris_diff_2k.png"), terrainTileScale, 1,0,0);
        b.setNormalMap(TextureLoader.loadTexture("debris_nor_2k.png"));
        b.setHeightMap(TextureLoader.loadTexture("debris_disp_2k.png"));
        b.setMetallicMap(TextureLoader.loadTexture("debris_rough_2k.png"));
        b.setRoughnessMap(TextureLoader.loadTexture("debris_rough_2k.png"));
        b.setAoMap(TextureLoader.loadTexture("debris_ao_2k.png"));
        
        TerrainTextureLayer c = new TerrainTextureLayer(TextureLoader.loadTexture("forest_ground_04_diff_2k.png"), terrainTileScale, 0,1,0);
        c.setNormalMap(TextureLoader.loadTexture("forest_ground_04_nor_2k.png"));
        c.setHeightMap(TextureLoader.loadTexture("forest_ground_04_disp_2k.png"));
        c.setMetallicMap(TextureLoader.loadTexture("forest_ground_04_rough_2k.png"));
        c.setRoughnessMap(TextureLoader.loadTexture("forest_ground_04_rough_2k.png"));
        c.setAoMap(TextureLoader.loadTexture("forest_ground_04_ao_2k.png"));
        
        
        TerrainTextureLayer d = new TerrainTextureLayer(TextureLoader.loadTexture("fall_leaves_2k.png"), terrainTileScale, 0,0,1);
        d.setNormalMap(TextureLoader.loadTexture("fall_leaves_normal_2k.png"));
        d.setHeightMap(TextureLoader.loadTexture("fall_leaves_disp_2k.png"));
        d.setMetallicMap(TextureLoader.loadTexture("fall_leaves_rough_2k.png"));
        d.setRoughnessMap(TextureLoader.loadTexture("fall_leaves_rough_2k.png"));
        d.setAoMap(TextureLoader.loadTexture("fall_leaves_ao_2k.png"));
    
        
        
        terrain.setTextureLayers(new TerrainTextureLayer[] {
        	    a, b, c, d
        	});
        
        
     // Initialize the ShadowMapRenderer
        int shadowMapSize = 2048 * 4 * 4;
        shadowRenderer = new ShadowRenderer(shadowMapSize, shadowMapSize);
       
        
        //Grass
        List<Grass> grassList = new ArrayList<>();
        float areaSize = 50.0f;
        int bladeCount = 10000;
        for (int i = 0; i < bladeCount; i++) {
            float x = (random.nextFloat() - 0.5f) * areaSize;
            float z = (random.nextFloat() - 0.5f) * areaSize;
            grassList.add(new Grass(new Vector3f(x, 0.0f, z)));
        }
     // Create the GrassRenderer
        grassRenderer = new GrassRenderer(grassList);
        
        
     
        
        entityManager.getPhysicsManager().addTerrainAccurateCollision(terrain);
        

        decalManager = new DecalManager();
        decalRenderer = new DecalRenderer();

     
  
   
        //Particles

         fireTexture = new  ParticleTexture(
                TextureLoader.loadExplicitTexture("fire.png"),
                8,
                true// numberOfRows
        );
         
         smokeTexture = new ParticleTexture(
        		    TextureLoader.loadExplicitTexture("smoke.png"), // Make sure you have a smoke.png texture
        		    8,  // Usually smoke doesn't need texture atlases
        		    false // Not additive - smoke should be normal alpha blending
        		);

        		// Create smoke particle system
        		smokeSystem = new ParticleSystem(smokeTexture, 50, 2.0f, 0.0f, 2.0f, 0.5f);

     
        ParticleMaster.init(masterRenderer.getProjectionMatrix());
        
      

        //Animations??
        AnimationLoader animationLoader = new AnimationLoader();
        
        AnimatedModel tubeDude = animationLoader.loadObject(animationLoader.loadScene("tubeDude.fbx"));
        for (AIAnimation aa : tubeDude.getAnimations()) {
        	//System.out.println("animation: " + aa.toString());
        }
     
        
        Entity cube7 = new Entity(entityManager.texturedModels.get("rock_plane"), new Vector3f(15, 25, 15), new Vector3f(0,0,0), 10f);

        entityManager.addEntity(cube7, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
        Entity cube8 = new Entity(entityManager.texturedModels.get("container"), new Vector3f(1291, 103, 775), new Vector3f(0,0,0), 1f);

        entityManager.addEntity(cube8, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
  
        Entity cube9 = new Entity(entityManager.texturedModels.get("concreteBuilding"), new Vector3f(1197, 74, 1305), new Vector3f(0,0,0), 1f);

        entityManager.addEntity(cube9, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
  
      
        Entity cube10 = new Entity(entityManager.texturedModels.get("rusted_barrier"), new Vector3f(1414, 98, 1400), new Vector3f(0,0,0), 1f);

        entityManager.addEntity(cube10, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
        
        Entity cube11 = new Entity(entityManager.texturedModels.get("tank"), new Vector3f(1637, 106, 1326), new Vector3f(0,0,0), 1f);

        entityManager.addEntity(cube11, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
        Entity cube12 = new Entity(entityManager.texturedModels.get("tank2"), new Vector3f(-205, 25, -105), new Vector3f(0,0,0), 1f);

        entityManager.addEntity(cube12, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        
        
        Entity cube13 = new Entity(entityManager.texturedModels.get("MK2"), new Vector3f(0, 45, 0), new Vector3f(0,0,0), 1f);

        //entityManager.addEntity(cube13, EntityManager.CollisionType.DYNAMIC_ACCURATE, 3);
        
        
        Entity tubeMan = new Entity(entityManager.texturedModels.get("tubeDude"), new Vector3f(0, 45, 0), new Vector3f(0,0,0), 20f);
        entityManager.addEntity(tubeMan, EntityManager.CollisionType.NONE, 3);
        
        
        Entity tubeMan2 = new Entity(entityManager.texturedModels.get("tubeDude"), new Vector3f(50, 45, 0), new Vector3f(0,0,0), 20f);
        entityManager.addEntity(tubeMan2, EntityManager.CollisionType.NONE, 3);
        
        
        for (int i = 0; i < 30; i++) {
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);
        	
        	
        	
        	Entity bush = new Entity(entityManager.texturedModels.get("bush_1"),
        			new Vector3f(x,y,z), 
        			new Vector3f(0,0,0), random.nextFloat(65) + 100);
            //bush.setMetallicMap(TextureLoader.loadTexture("searsia_lucida_rough_2k.png"));
            
            //entityManager.addEntity(bush, EntityManager.CollisionType.NONE, 0);
            
            
        }
        
        //Grass
        /*
        for (int i = 0; i < 20; i++) {
        	
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);
        	
        	
        	
        	Entity grass = new Entity(ObjLoader.loadObj("billboard3"), TextureLoader.loadTexture("tallGrass2.png"),
        			new Vector3f(x,y,z), 
        			new Vector3f(0,0,0), random.nextFloat(2) + 10);
        	grass.setHasTransparency(true);
        	grass.setHasOpaque(false);
        	grass.setNormalMapId(TextureLoader.loadTexture("up_normal_2k.png"));

            entityManager.addEntity(grass, EntityManager.CollisionType.NONE, 0);
            
        }
        */
        
        for (int i = 0; i < 10; i++) {
        	
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);
        	/*
        	 lights.add(new Light(
                     new Vector3f(x,y,x),
                     new Vector3f(random.nextFloat(15f), random.nextFloat(15f), random.nextFloat(15f)),
                     new Vector3f(1, 0.62f, 0.232f)
                 ));
            
            */
        }
        
        
       
        
        
        //Tree
        for (int i = 0; i < 100; i++) {
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);
        	float size = random.nextFloat(6) + 1;
        	float rotY = random.nextInt(360);
        	
        	int treeNum = random.nextInt(15);
        	Entity tree = new Entity();
        	if (treeNum == 0) {
        		tree = new Entity(entityManager.texturedModels.get("tree_9"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        	}
        	
        	if (treeNum == 1) {
        		tree = new Entity(entityManager.texturedModels.get("tree_8"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 2) {
        		tree = new Entity(entityManager.texturedModels.get("tree_3"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		
        	}
        	if (treeNum == 3) {
        		tree = new Entity(entityManager.texturedModels.get("tree_9"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 4) {
        		tree = new Entity(entityManager.texturedModels.get("bush_4"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);

        	}
        	
        	if (treeNum == 5) {
        		tree = new Entity(entityManager.texturedModels.get("bush_5"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);
        		
        	}
        	
        	if (treeNum == 6) {
        		tree = new Entity(entityManager.texturedModels.get("boulder_1"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 7) {
        		tree = new Entity(entityManager.texturedModels.get("boulder_3"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 8) {
        		tree = new Entity(entityManager.texturedModels.get("bush_3"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);
        		
        	}
        	
        	if (treeNum == 9) {
        		tree = new Entity(entityManager.texturedModels.get("bush_2"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);
        		
        	}
        	
        	if (treeNum == 10) {
        		tree = new Entity(entityManager.texturedModels.get("pine_tree_4"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		// entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		//pine_tree_2 CAUSES LAGGGG
        		
        	}
        	
        	if (treeNum == 11) {
        		tree = new Entity(entityManager.texturedModels.get("pine_tree_4"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 12) {
        		tree = new Entity(entityManager.texturedModels.get("cedar_tree_2"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_NOT_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 13) {
        		tree = new Entity(entityManager.texturedModels.get("boulder_7"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
        		 entityManager.addEntity(tree, EntityManager.CollisionType.STATIC_ACCURATE, 0);
        		
        	}
        	
        	if (treeNum == 14) {
        		tree = new Entity(entityManager.texturedModels.get("blades_3"),
            			new Vector3f(x,y,z), 
            			new Vector3f(0,rotY,0), size);
       		 entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);
        		
        	}
        	
        	
            
           

            
        }
        
        //Grass
        for (int i = 0; i < 1400; i++) {
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);
        	float size = random.nextFloat(2) + 2;
        	float rotY = random.nextInt(360);
        	
        	Entity tree = new Entity();
        	tree = new Entity(entityManager.texturedModels.get("tall_grass"),
        			new Vector3f(x,y,z), 
        			new Vector3f(0,rotY,0), size);
        	
        	
            
            entityManager.addEntity(tree, EntityManager.CollisionType.NONE, 0);

            
        }
     
        //Boxes
        for (int i = 0; i < 3; i++) {
        	float x = random.nextFloat(terrain.getSize()) + terrain.getX();
        	float z = random.nextFloat(terrain.getSize()) + terrain.getZ();
        	float y = terrain.getHeightOfTerrain(x, z);;
        	float size = 1;//random.nextFloat(3) + 1;
        	float rotY = random.nextInt(360);
        	
        	Entity tree = new Entity();
        	tree = new Entity(entityManager.texturedModels.get("barrel"),
        			new Vector3f(x,y,z), 
        			new Vector3f(0,rotY,0), size);
        	
        	
            
            entityManager.addEntity(tree, EntityManager.CollisionType.DYNAMIC_ACCURATE, 250);

            
        }
        
        
        

        
        
        Entity playerModel = new Entity(entityManager.texturedModels.get("player_body"), new Vector3f(0, 30, 0), new Vector3f(0,0,0), 3f);
        playerModel.setPosition(600,  terrain.getHeightOfTerrain(600, 600) + 30, 600);
        
        //entityManager.addEntity(playerModel, EntityManager.CollisionType.NONE, 1);

        
        player = new Player(playerModel, entityManager.getPhysicsManager(), camera);
        
        
        
 
        /*
        
        for (Entity e : SceneLoader.loadScene("sponza.obj", "sponza.mtl")) {
        	try {
        		physicsManager.addStaticAccurateCollision(e);
        	} catch (Exception a) {
        		System.out.println("ERROR ADDING On to Physics. " + a.getMessage());
        	}
        	
        	entities.add(e);
        }
        */
        
        
        
        
    
        
        
        
        //Sun
        Light sun = new Light(new Vector3f(10000, 20000,0), new Vector3f(12,12,12));
        lights.add(sun); 
        //Moon
        Light moon = new Light(new Vector3f(-10000, -20000,0), new Vector3f(0,0,0)); 
        lights.add(moon); 
         
      
        
    lights.add(new Light(
         new Vector3f(2,0,0),
         new Vector3f(1.0f, 0.8f, 0.7f),
         new Vector3f(1, 0.62f, 0.000032f)
     ));
     
     lights.add(new Light(
	         new Vector3f(-2,-10,0),
	         new Vector3f(0.0f, 8f, 7f),
	         new Vector3f(1, 0.0062f,  0.000232f)
	     ));
  
     lights.add(new Light(
	         new Vector3f(-20,0,20),
	         new Vector3f(10.0f, 0.0f, 0.7f),
	         new Vector3f(1, 0.0062f, 0.000232f)
	     ));
     
     
     lights.add(new Light(
	         new Vector3f(0,10,0),
	         new Vector3f(10.0f, 10.0f, 0.7f),
	         new Vector3f(1, 0.0062f, 0.000232f)
	     ));
      
      
      
   	    float crossHairSize = 25f;
   		textureRenderer.addTexture(new GuiTexture(TextureLoader.loadExplicitTexture("crossHair.png"),
   				(width/2.0f) - crossHairSize/2.0f, (height/2.0f) - crossHairSize/2.0f, crossHairSize,crossHairSize));

	     picker = new MousePicker(width, height, camera, masterRenderer.getProjectionMatrix(), entityManager.getEntitiesList(), lights);
	        

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
            
            
            
            //Scene stuff
            //READ
            if (glfwGetKey(window, GLFW_KEY_R) == GLFW_PRESS) {
            	//entities.clear();
            	//lights.clear();
            	//physicsManager = new PhysicsManager();
            	//SceneLoader2.ReadScene("testScene.txt", physicsManager);
            	//entities.addAll(SceneLoader2.entities);
            	//lights.addAll(lights)
            	
            	
    		}
            
            //Create
            if (glfwGetKey(window, GLFW_KEY_C) == GLFW_PRESS) {
            	//SceneLoader2.CreateScene("testScene.txt", entities, lights, entityManager.getPhysicsManager());
            }
            
            
            

            // Example: rotate the second cube around Y
            if (EngineSettings.VisualiseObjects) {
            	for (Entity e : entityManager.getEntitiesList()) {
                	debugRenderer.addSphere(e.getPosition(), e.getTexturedModel().getMesh().getFurthestPoint() + e.getScale(), new Vector3f(0,1,0));
                }
            }
            
            
            if (EngineSettings.VisualiseLights) {
            	for (Light e : lights) {
                	debugRenderer.addSphere(e.getPosition(), 1 ,e.getColor());
                }
            }
            
            if (!EngineSettings.grabMouse && EngineSettings.MouseItemPicker && !EngineSettings.overTexture) {
            	picker.update(window, entityManager.getPhysicsManager(), terrain);
            	picker.drawDebug(debugRenderer);
            	picker.drawRotationDebug(debugRenderer);
            	
         
            	
            }
            	
            
            
            player.updatePlayer(window, deltaTime, terrain);
            
            entityManager.getPhysicsManager().updateEntitiesFromCollisionShapes(deltaTime, entityManager.getEntitiesList());
            entityManager.updateAnimations(deltaTime);
            
            
            
            ParticleMaster.createParticle(
                    fireTexture,
                    new org.lwjgl.util.vector.Vector3f(600, 80, 600),
                    new org.lwjgl.util.vector.Vector3f(0, 12f, 0),
                    0f,     // gravityEffect
                    2.5f,     // lifeLength (seconds)
                    random.nextFloat() * 360f,
                    60.0f      // scale
            );
            
            ParticleMaster.update(camera, deltaTime);
            
            
            
        	//System.out.println(shadowTextureID);
            Matrix4f lightSpaceMatrix;
            if (skyboxRenderer.isSunOut()) {
            	 shadowRenderer.renderShadowMap(entityManager.getEntitiesList(),
            			 lightSpaceMatrix = shadowRenderer.createLightSpaceMatrix(lights.get(0), camera), 
                 		camera.getViewMatrix(),  masterRenderer.getProjectionMatrix());
            } else {
            	 shadowRenderer.renderShadowMap(entityManager.getEntitiesList(),
            			 lightSpaceMatrix = shadowRenderer.createLightSpaceMatrix(lights.get(1), camera), 
                 		camera.getViewMatrix(),  masterRenderer.getProjectionMatrix());
            }
            
           
            int shadowTextureID = shadowRenderer.getDepthMapTexture();
            //System.out.println(shadowTextureID);
            int err = glGetError();
            
            //shadowMapRenderer.render(entities, lights.get(lights.size() - 1), camera);
            //System.out.println(shadowMapRenderer.getShadowMap());
            
            //postRenderer.bindFBO();
            bloomRenderer.bindSceneFBO();
            //glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
            glViewport(0, 0, width, height);
            //System.out.println("outsideAmbience: " + outsideAmbience);
            outsideAmbience = skyboxRenderer.ambience;
            
            
         // (Optional) Output the texture ID to the console.
         //System.out.println("Shadow Texture ID: " + shadowTextureID);
          
        
            // Render everything
            masterRenderer.render(entityManager.getEntitiesList(), lights, camera, shadowTextureID, outsideAmbience);


         // Assuming you have projection, view, and model matrices available.
          
            //terrainRenderer.renderAdaptiveTerrain(adaptiveGen, masterRenderer.getProjectionMatrix(), camera.getViewMatrix(), terrainModelMatrix, camera.getPosition(), lights);
           
         
            skyboxRenderer.render(camera, camera.getViewMatrix(), masterRenderer.getProjectionMatrix(), lights.get(0),lights.get(1), 1000000);            
           

            // Could add more interesting transforms as well
            debugRenderer.render(camera, masterRenderer.getProjectionMatrix(), camera.getViewMatrix());
            
           
            float time = (System.currentTimeMillis() % 10000L) / 1000.0f;

            /*
            grassRenderer.render(
                    10.0f,               // bladeLength
                    0.51f,               // bladeWidth
                    0.15f,               // windStrength
                    new Vector2f(1.0f, 0.0f), // windDirection
                    time,                // time for wind animation
                    masterRenderer.getProjectionMatrix(), camera.getViewMatrix(), lights, shadowTextureID, camera
                );
            */
            terrainEditor.update(terrain, picker.getWorldPosX(), picker.getWorldPosZ(), window);
            
            
            

            
            if (EngineSettings.TerrainEditor || EngineSettings.TerrainPainter) {
            	float ty = terrain.getHeightOfTerrain(picker.getWorldPosX(), picker.getWorldPosZ()) + 5;
            	Vector3f terrainColor =  new Vector3f(1, 1, 1);
            	if (EngineSettings.TerrainPainter) {
            		if (EngineSettings.TerrainColor == TerrainBrushColor.BLACK) {
    					terrainColor =  new Vector3f(0, 0, 0);		
    				}
    				if (EngineSettings.TerrainColor == TerrainBrushColor.RED) {
    					terrainColor =  new Vector3f(1, 0, 0);
    				}
    								
    				if (EngineSettings.TerrainColor == TerrainBrushColor.GREEN) {
    					terrainColor =  new Vector3f(0, 1, 0);
    				}
    								
    				if (EngineSettings.TerrainColor == TerrainBrushColor.BLUE) {
    					terrainColor =  new Vector3f(0, 0, 1);
    				}
            	}
			
            	
            	debugRenderer.addCircle(new Vector3f(picker.getWorldPosX(), ty, picker.getWorldPosZ()), EngineSettings.TerrainBrushSize, terrainColor);
            }
            
            
            
            terrainRenderer.render(
                    terrain,
                    masterRenderer.getProjectionMatrix(),
                    camera.getViewMatrix(),
                    lights,
                    shadowTextureID,
                    lightSpaceMatrix,
                    camera.getPosition(),
                    outsideAmbience
            );
            

     

            decalRenderer.render(decalManager.getDecals(), camera, masterRenderer.getProjectionMatrix(),
            		shadowTextureID, lights, lightSpaceMatrix);
            
            
            ParticleMaster.renderParticles(camera);
            
           // debugRenderer.addCollisionMesh(player.getPhysicsBody(), new Vector3f(1,1,1));
            
            //postRenderer.unbindFBO(width, height);
            bloomRenderer.unbindSceneFBO(width, height);
            
            //postRenderer.renderPostProcess();
            bloomRenderer.renderBloom(width, height,1.0f, 0.2f);
            
            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_RIGHT) == GLFW.GLFW_PRESS) {

                Entity cube13 = new Entity(
                    entityManager.texturedModels.get("MK2"),
                    new Vector3f(
                        camera.getPosition().x(),
                        camera.getPosition().y() + 5f,
                        camera.getPosition().z()
                    ),
                    new Vector3f(0, 0, 0),
                    1f
                );

                float throwStrength = 350f;

                // ---- PITCH-BASED ARC ----
                float pitch = camera.getPitch();
                float clampedPitch = Math.max(-60f, Math.min(60f, pitch));
                float pitchFactor = clampedPitch / 60f;

                float baseArc = 0.15f;
                float arcScale = 0.85f;
                float arc = baseArc + (pitchFactor * arcScale);

                Vector3f velocity = new Vector3f(camera.getForward());

                velocity.y += arc;        // pitch-driven arc
                velocity.normalize();
                velocity.mul(throwStrength);

                Entity g = entityManager.addEntity(
                    cube13,
                    EntityManager.CollisionType.DYNAMIC_NOT_ACCURATE,
                    1
                );

                RigidBody body = entityManager.getEntityRigidBody(g);

                body.setActivationState(RigidBody.ACTIVE_TAG);
                body.setDamping(0.05f, 0.85f);
                body.setRestitution(0.4f);
                body.setFriction(0.8f);

                body.setAngularVelocity(new javax.vecmath.Vector3f(0, 0, 0));
                body.setLinearVelocity(new javax.vecmath.Vector3f(
                    velocity.x,
                    velocity.y,
                    velocity.z
                ));

                // Optional realism spin
             
            }

            
            
            

            if (GLFW.glfwGetMouseButton(window, GLFW.GLFW_MOUSE_BUTTON_LEFT) == GLFW.GLFW_PRESS) {
                //System.out.println("Shoot!");
                HitResult hit = PhysicsManager.shoot(camera, entityManager.getPhysicsManager(), 10000);

                if (hit.hit) {

                    Vector3f n = new Vector3f(hit.hitNormal).normalize();

                    Vector3f forward = new Vector3f(0, 0, 1);

               
                    if (Math.abs(n.y) > 0.999f) {
                        forward.set(0, 0, n.y > 0 ? -1 : 1);
                    }

                
                    Vector3f axis = forward.cross(n, new Vector3f());
                    float axisLen = axis.length();

                    float angle;
                    if (axisLen < 1e-6f) {
                        // Already aligned
                        axis.set(0, 1, 0);
                        angle = 0f;
                    } else {
                        axis.normalize();
                        angle = (float) Math.acos(forward.dot(n));
                    }

                    // ---------------------------------
                    // Convert axis-angle → Euler XYZ
                    // ---------------------------------
                    float s = (float) Math.sin(angle);
                    float c = (float) Math.cos(angle);
                    float t = 1f - c;

                    float x = axis.x;
                    float y = axis.y;
                    float z = axis.z;

                    // Rotation matrix from axis-angle
                    float m00 = t*x*x + c;
                    float m01 = t*x*y - s*z;
                    float m02 = t*x*z + s*y;

                    float m10 = t*x*y + s*z;
                    float m11 = t*y*y + c;
                    float m12 = t*y*z - s*x;

                    float m20 = t*x*z - s*y;
                    float m21 = t*y*z + s*x;
                    float m22 = t*z*z + c;

                    // Extract Euler XYZ (matches rotateXYZ)
                    float rotX = (float) Math.atan2(m21, m22);
                    float rotY = (float) Math.atan2(-m20, Math.sqrt(m21*m21 + m22*m22));
                    float rotZ = (float) Math.atan2(m10, m00);

                    Vector3f rotation = new Vector3f(rotX, rotY, rotZ);

              
                    Vector3f size = new Vector3f(
                        1.15f,   // width
                        1.15f,   // height
                        0.025f  // thickness
                    );


                    Vector3f position = new Vector3f(hit.hitPoint)
                            .fma(0.01f, n);

                    
                    Vector3f cameraForward = new Vector3f(0, 0, -1);
                    cameraForward.rotateY((float)Math.toRadians(camera.getYaw()));
                    cameraForward.rotateX((float)Math.toRadians(camera.getPitch()));
                    cameraForward.normalize();
                    hit.createSmokePuff(hit.hitPoint, hit.hitNormal, cameraForward, smokeTexture);
              
                    decalManager.add(
                        new Decal(
                            position,
                            rotation,
                            size,
                            TextureLoader.loadTexture("bullet_hole.png"),
                            TextureLoader.loadTexture("container_rust_nor_2k.png"),
                            Decal.DecalType.BULLET
                        )
                    );
                }
            }

            
           
            
            //int c = Equations.combineTexturesFixed(3, 4, width, height);
            //System.out.print(c);
            
            //Render Texture
            glClear(GL_DEPTH_BUFFER_BIT);
            glEnable(GL_BLEND);
            glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);
            textureRenderer.render(masterRenderer.getFlatProjection(), camera.getFlatViewMatrix(), mouseX[0], adjustedMouseY);
          
            
            
            
            if (!EngineSettings.grabMouse) {
            	
            	
            	
            }
            
           	
            guiManager.update(textureRenderer, textRenderer, entityManager, window);
            
            EngineSettings.updateSettings(window);
            if (MAX_FPS > 0) {

                double desiredFrameTime = 1.0 / MAX_FPS;
                double actualFrameTime = glfwGetTime() - currentTime;

                if (actualFrameTime < desiredFrameTime) {
                    double sleepTime = (desiredFrameTime - actualFrameTime) * 1000.0;

                    if (sleepTime > 0) {
                        try {
                            Thread.sleep((long) sleepTime);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
            }

            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        // Cleanup
    	grassRenderer.cleanup();
    	textRenderer.cleanUp();
    	terrainRenderer.cleanup();
    	//postRenderer.cleanup();
        masterRenderer.cleanup();
        skyboxRenderer.cleanUp();
        debugRenderer.cleanup();
        textureRenderer.cleanUp();
        ParticleMaster.cleanUp();
        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }
}
