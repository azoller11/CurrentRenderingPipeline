package entityManager;

import loaders.ObjLoader;
import loaders.TextureLoader;
import toolbox.Mesh;

import java.util.Map;

import org.joml.Vector3f;

import entities.TexturedModel;

public class TexturedModels {
	
	
	public TexturedModels() {
		
	}
	
	public void init(Map<String, TexturedModel> texturedModels) {
		
		//CUBE 
		TexturedModel cube = new TexturedModel();
		cube.setMesh(ObjLoader.loadObj("crate"));
		cube.setTextureId(TextureLoader.loadTexture("blue_metal_plate_diff_2k.png"));
		cube.setNormalMapId(TextureLoader.loadTexture("blue_metal_plate_nor_gl_2k.png"));
		cube.setHeighMapId(TextureLoader.loadTexture("blue_metal_plate_disp_2k.png"));
		cube.setMetallicMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
		cube.setRoughnessMap(TextureLoader.loadTexture("blue_metal_plate_rough_2k.png"));
		cube.setAoMap(TextureLoader.loadTexture("blue_metal_plate_ao_2k.png"));
        cube.setParallaxScale(new Vector3f(0.05f, 120, 160));
        cube.setCastShadows(true);
        texturedModels.put("Cube", cube);
        
        //Barrel
		TexturedModel Barrel = new TexturedModel();
		Barrel.setMesh(ObjLoader.loadObj("Barrel"));
		Barrel.setTextureId(TextureLoader.loadTexture("rust_diff_2k.png"));
		Barrel.setNormalMapId(TextureLoader.loadTexture("rust_nor_2k.png"));
		Barrel.setHeighMapId(TextureLoader.loadTexture("rust_disp_2k.png"));
		Barrel.setMetallicMap(TextureLoader.loadTexture("rust_metal_2k.png"));
		Barrel.setRoughnessMap(TextureLoader.loadTexture("rust_rough_2k.png"));
		Barrel.setAoMap(TextureLoader.loadTexture("rust_ao_2k.png"));
		Barrel.setParallaxScale(new Vector3f(0.01f, 120, 160));
        Barrel.setCastShadows(true);
        texturedModels.put("barrel", Barrel);
        
        
        //Container
		TexturedModel Container = new TexturedModel();
		Container.setMesh(ObjLoader.loadObj("container"));
		Container.setTextureId(TextureLoader.loadTexture("container_rust_diff_2k.png"));
		Container.setNormalMapId(TextureLoader.loadTexture("container_rust_nor_2k.png"));
		//Container.setHeighMapId(TextureLoader.loadTexture("container_rust_disp_2k.png"));
		Container.setMetallicMap(TextureLoader.loadTexture("container_rust_rough_2k.png"));
		Container.setRoughnessMap(TextureLoader.loadTexture("container_rust_rough_2k.png"));
		Container.setAoMap(TextureLoader.loadTexture("container_rust_ao_2k.png"));
		//Container.setParallaxScale(new Vector3f(0.03f, 120, 160));
		Container.setCastShadows(true);
        texturedModels.put("container", Container);
        
        //concrete building
		TexturedModel concreteBuilding = new TexturedModel();
		concreteBuilding.setMesh(ObjLoader.loadObj("concreteBuilding"));
		concreteBuilding.setTextureId(TextureLoader.loadTexture("concrete2_diff_2k.png"));
		concreteBuilding.setNormalMapId(TextureLoader.loadTexture("concrete2_nor_2k.png"));
		concreteBuilding.setHeighMapId(TextureLoader.loadTexture("concrete2_disp_2k.png"));
		concreteBuilding.setMetallicMap(TextureLoader.loadTexture("concrete2_rough_2k.png"));
		concreteBuilding.setRoughnessMap(TextureLoader.loadTexture("concrete2_rough_2k.png"));
		concreteBuilding.setAoMap(TextureLoader.loadTexture("concrete2_ao_2k.png"));
		concreteBuilding.setParallaxScale(new Vector3f(0.01f, 20, 60));
		concreteBuilding.setCastShadows(true);
        texturedModels.put("concreteBuilding", concreteBuilding);
        
        
        //rustedBarrier
		TexturedModel rustedBarrier = new TexturedModel();
		rustedBarrier.setMesh(ObjLoader.loadObj("rustedBarrier"));
		rustedBarrier.setTextureId(TextureLoader.loadTexture("building_diff_2k.png"));
		rustedBarrier.setNormalMapId(TextureLoader.loadTexture("building_nor_2k.png"));
		rustedBarrier.setHeighMapId(TextureLoader.loadTexture("building_disp_2k.png"));
		rustedBarrier.setMetallicMap(TextureLoader.loadTexture("building_rough_2k.png"));
		rustedBarrier.setRoughnessMap(TextureLoader.loadTexture("building_rough_2k.png"));
		rustedBarrier.setAoMap(TextureLoader.loadTexture("building_ao_2k.png"));
		rustedBarrier.setParallaxScale(new Vector3f(0.01f, 20, 60));
		rustedBarrier.setCastShadows(true);
		rustedBarrier.setHasTransparency(true);
        texturedModels.put("rusted_barrier", rustedBarrier);
        
        
        // camo
        //https://www.sharetextures.com/textures
        //tank
        //https://www.cgtrader.com/
        
        //tank
		TexturedModel tank = new TexturedModel();
		tank.setMesh(ObjLoader.loadObj("tank"));
		tank.setTextureId(TextureLoader.loadTexture("camo_2k.png"));
		tank.setNormalMapId(TextureLoader.loadTexture("rust_nor_2k.png"));
		tank.setHeighMapId(TextureLoader.loadTexture("rust_disp_2k.png"));
		tank.setMetallicMap(TextureLoader.loadTexture("rust_metal_2k.png"));
		tank.setRoughnessMap(TextureLoader.loadTexture("rust_rough_2k.png"));
		tank.setAoMap(TextureLoader.loadTexture("rust_ao_2k.png"));
		tank.setParallaxScale(new Vector3f(0.01f, 20, 60));
		tank.setCastShadows(true);
        texturedModels.put("tank", tank);
        
        //tank2
		TexturedModel tank2 = new TexturedModel();
		tank2.setMesh(ObjLoader.loadObj("tank2"));
		tank2.setTextureId(TextureLoader.loadTexture("camo_2k.png"));
		tank2.setNormalMapId(TextureLoader.loadTexture("rust_nor_2k.png"));
		tank2.setHeighMapId(TextureLoader.loadTexture("rust_disp_2k.png"));
		tank2.setMetallicMap(TextureLoader.loadTexture("rust_metal_2k.png"));
		tank2.setRoughnessMap(TextureLoader.loadTexture("rust_rough_2k.png"));
		tank2.setAoMap(TextureLoader.loadTexture("rust_ao_2k.png"));
		tank2.setParallaxScale(new Vector3f(0.01f, 20, 60));
		tank2.setCastShadows(true);
        texturedModels.put("tank2", tank2);
        
        
		
		//Brick Plane
        TexturedModel brickPlane = new TexturedModel();
        brickPlane.setMesh(ObjLoader.loadObj("plane"));
        brickPlane.setTextureId(TextureLoader.loadTexture("medieval_red_brick_diff_2k.png"));
		brickPlane.setNormalMapId(TextureLoader.loadTexture("medieval_red_brick_nor_gl_2k.png"));
		brickPlane.setHeighMapId(TextureLoader.loadTexture("medieval_red_brick_disp_2k.png"));
		brickPlane.setMetallicMap(TextureLoader.loadTexture("medieval_red_brick_rough_2k.png"));
		brickPlane.setRoughnessMap(TextureLoader.loadTexture("medieval_red_brick_rough_2k.png"));
		brickPlane.setAoMap(TextureLoader.loadTexture("medieval_red_brick_ao_2k.png"));
		brickPlane.setParallaxScale(new Vector3f(0.05f, 120, 160));
        brickPlane.setCastShadows(true);
        texturedModels.put("brick_plane", brickPlane);
        
        
        //rock Plane
        TexturedModel rockPlane = new TexturedModel();
        rockPlane.setMesh(ObjLoader.loadObj("plane"));
        rockPlane.setTextureId(TextureLoader.loadTexture("ganges_river_pebbles_diff_2k.png"));
        rockPlane.setNormalMapId(TextureLoader.loadTexture("ganges_river_pebbles_nor_gl_2k.png"));
        rockPlane.setHeighMapId(TextureLoader.loadTexture("ganges_river_pebbles_disp_2k.png"));
        rockPlane.setMetallicMap(TextureLoader.loadTexture("ganges_river_pebbles_rough_2k.png"));
        rockPlane.setRoughnessMap(TextureLoader.loadTexture("ganges_river_pebbles_rough_2k.png"));
        rockPlane.setAoMap(TextureLoader.loadTexture("ganges_river_pebbles_ao_2k.png"));
		rockPlane.setParallaxScale(new Vector3f(0.05f, 120, 160));
        rockPlane.setCastShadows(true);
        texturedModels.put("rock_plane", rockPlane);
        
       //Roots Plane
        TexturedModel rootsPlane = new TexturedModel();
        rootsPlane.setMesh(ObjLoader.loadObj("plane"));
        rootsPlane.setTextureId(TextureLoader.loadTexture("roots_diff_2k.png"));
        rootsPlane.setNormalMapId(TextureLoader.loadTexture("roots_nor_gl_2k.png"));
        rootsPlane.setHeighMapId(TextureLoader.loadTexture("roots_disp_2k.png"));
		rootsPlane.setMetallicMap(TextureLoader.loadTexture("roots_rough_2k.png"));
		rootsPlane.setRoughnessMap(TextureLoader.loadTexture("roots_ao_2k.png"));
		rootsPlane.setAoMap(TextureLoader.loadTexture("roots_ao_2k.png"));
		rootsPlane.setParallaxScale(new Vector3f(0.09f, 120, 160));
        rootsPlane.setCastShadows(true);
        texturedModels.put("roots_plane", rootsPlane);
        
        
        //Bush
        TexturedModel bush1 = new TexturedModel();
        bush1.setMesh(ObjLoader.loadObj("bush1"));
        bush1.setTextureId(TextureLoader.loadTexture("searsia_lucida_diff_2k.png"));
        bush1.setNormalMapId(TextureLoader.loadTexture("searsia_lucida_nor_gl_2k.png"));
		bush1.setRoughnessMap(TextureLoader.loadTexture("searsia_lucida_rough_2k.png"));
		bush1.setAoMap(TextureLoader.loadTexture("searsia_lucida_ao_2k.png"));
		bush1.setHasTransparency(true);
		bush1.setHasOpaque(false);
        bush1.setCastShadows(true);
        bush1.setUseFakeLighting(true);
        texturedModels.put("bush_1", bush1);
        
        
        //Tree 9
        TexturedModel Tree9 = new TexturedModel();
        Tree9.setMesh(ObjLoader.loadObj("tree9"));
        Tree9.setTextureId(TextureLoader.loadTexture("treeTextures5.png"));
        //Tree9.setNormalMapId(TextureLoader.loadTexture("up_normal_2k.png"));
        Tree9.setHasTransparency(true);
        Tree9.setHasOpaque(false);
        Tree9.setCastShadows(true);
        Tree9.setUseFakeLighting(true);
        texturedModels.put("tree_9", Tree9);
        
        
        //Tree 8
        TexturedModel Tree8 = new TexturedModel();
        Tree8.setMesh(ObjLoader.loadObj("tree8"));
        Tree8.setTextureId(TextureLoader.loadTexture("treeTextures5.png"));
        //Tree8.setNormalMapId(TextureLoader.loadTexture("up_normal_2k.png"));
        Tree8.setHasTransparency(true);
        Tree8.setHasOpaque(false);
        Tree8.setCastShadows(true);
        Tree8.setUseFakeLighting(true);
        texturedModels.put("tree_8", Tree8);
      
        
        //Tree 3
        TexturedModel Tree3 = new TexturedModel();
        Tree3.setMesh(ObjLoader.loadObj("tree3"));
        Tree3.setTextureId(TextureLoader.loadTexture("treeTextures4.png"));
        Tree3.setHasTransparency(true);
        Tree3.setHasOpaque(false);
        Tree3.setCastShadows(true);
        Tree3.setUseFakeLighting(true);
        texturedModels.put("tree_3", Tree3);
        
        //spruce tree
        TexturedModel SpruceTree = new TexturedModel();
        SpruceTree.setMesh(ObjLoader.loadObj("spruceTree"));
        SpruceTree.setTextureId(TextureLoader.loadTexture("treeTextures2.png"));
        SpruceTree.setHasTransparency(true);
        SpruceTree.setHasOpaque(false);
        SpruceTree.setUseFakeLighting(true);
        SpruceTree.setCastShadows(true);
        SpruceTree.setUseFakeLighting(true);
        texturedModels.put("spruce_tree", SpruceTree);
        
        //cedarTree2
        TexturedModel cedarTree2 = new TexturedModel();
        cedarTree2.setMesh(ObjLoader.loadObj("cedarTree2"));
        cedarTree2.setTextureId(TextureLoader.loadTexture("pineTexture2.png"));
        cedarTree2.setHasTransparency(true);
        cedarTree2.setHasOpaque(false);
        cedarTree2.setCastShadows(true);
        cedarTree2.setUseFakeLighting(true);
        texturedModels.put("cedar_tree_2", cedarTree2);
        
        //pineTree4
        TexturedModel pineTree4 = new TexturedModel();
        pineTree4.setMesh(ObjLoader.loadObj("pineTree4"));
        pineTree4.setTextureId(TextureLoader.loadTexture("pineTexture3.png"));
        pineTree4.setHasTransparency(true);
        pineTree4.setHasOpaque(false);
        pineTree4.setCastShadows(true);
        pineTree4.setUseFakeLighting(true);
        texturedModels.put("pine_tree_4", pineTree4);
        
        //pineTree2
        TexturedModel pineTree2 = new TexturedModel();
        pineTree2.setMesh(ObjLoader.loadObj("tree2"));
        pineTree2.setTextureId(TextureLoader.loadTexture("pineTexture2.png"));
        pineTree2.setHasTransparency(true);
        pineTree2.setHasOpaque(false);
        pineTree2.setCastShadows(true);
        pineTree2.setUseFakeLighting(true);
        texturedModels.put("pine_tree_2", pineTree2);
        
        
        //Player Body
        TexturedModel playerBody = new TexturedModel();
        playerBody.setMesh(ObjLoader.loadObj("playerBody"));
        playerBody.setTextureId(TextureLoader.loadTexture("blue_metal_plate_diff_2k.png"));
        playerBody.setCastShadows(true);
        texturedModels.put("player_body", playerBody);
        
        //Bush 2
        TexturedModel Bush2 = new TexturedModel();
        Bush2.setMesh(ObjLoader.loadObj("Bush2"));
        Bush2.setTextureId(TextureLoader.loadTexture("leaveTexture.png"));
        Bush2.setHasTransparency(true);
        Bush2.setHasOpaque(false);
        Bush2.setCastShadows(true);
        Bush2.setUseFakeLighting(true);
        texturedModels.put("bush_2", Bush2);
        
        //Bush 3
        TexturedModel Bush3 = new TexturedModel();
        Bush3.setMesh(ObjLoader.loadObj("Bush3"));
        Bush3.setTextureId(TextureLoader.loadTexture("leaveTexture.png"));
        Bush3.setHasTransparency(true);
        Bush3.setHasOpaque(false);
        Bush3.setCastShadows(true);
        Bush3.setUseFakeLighting(true);
        texturedModels.put("bush_3", Bush3);
        
        //Bush 4
        TexturedModel Bush4 = new TexturedModel();
        Bush4.setMesh(ObjLoader.loadObj("Bush4"));
        Bush4.setTextureId(TextureLoader.loadTexture("pineTexture3.png"));
        Bush4.setHasTransparency(true);
        Bush4.setHasOpaque(false);
        Bush4.setCastShadows(true);
        texturedModels.put("bush_4", Bush4);
        
        //Bush 5
        TexturedModel Bush5 = new TexturedModel();
        Bush5.setMesh(ObjLoader.loadObj("Bush5"));
        Bush5.setTextureId(TextureLoader.loadTexture("pineTexture3.png"));
        Bush5.setHasTransparency(true);
        Bush5.setHasOpaque(false);
        Bush5.setCastShadows(true);
        Bush5.setUseFakeLighting(true);
        texturedModels.put("bush_5", Bush5);
        
        //Boulder 1
        TexturedModel Boulder1 = new TexturedModel();
        Boulder1.setMesh(ObjLoader.loadObj("boulder1"));
        Boulder1.setTextureId(TextureLoader.loadTexture("rock3_diff_2k.png"));
        Boulder1.setNormalMapId(TextureLoader.loadTexture("rock3_nor_2k.png"));
        Boulder1.setHeighMapId(TextureLoader.loadTexture("rock3_disp_2k.png"));
        Boulder1.setMetallicMap(TextureLoader.loadTexture("rock3_rough_2k.png"));
        Boulder1.setRoughnessMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder1.setAoMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder1.setParallaxScale(new Vector3f(0.01f, 20, 60));
        
        Boulder1.setCastShadows(true);
        texturedModels.put("boulder_1", Boulder1);
        
        
        
        //Boulder 7
        TexturedModel Boulder3 = new TexturedModel();
        Boulder3.setMesh(ObjLoader.loadObj("boulder7"));
        Boulder3.setTextureId(TextureLoader.loadTexture("rock3_diff_2k.png"));
        Boulder3.setNormalMapId(TextureLoader.loadTexture("rock3_nor_2k.png"));
        
        Boulder3.setHeighMapId(TextureLoader.loadTexture("rock3_disp_2k.png"));
        Boulder3.setMetallicMap(TextureLoader.loadTexture("rock3_rough_2k.png"));
        Boulder3.setRoughnessMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder3.setAoMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder3.setParallaxScale(new Vector3f(0.01f, 20, 60));
        Boulder3.setCastShadows(true);
        texturedModels.put("boulder_3", Boulder3);
        
        //Boulder 6
        TexturedModel Boulder7 = new TexturedModel();
        Boulder7.setMesh(ObjLoader.loadObj("boulder7"));
        Boulder7.setTextureId(TextureLoader.loadTexture("rock3_diff_2k.png"));
        Boulder7.setNormalMapId(TextureLoader.loadTexture("rock3_nor_2k.png"));
        
        Boulder7.setHeighMapId(TextureLoader.loadTexture("rock3_disp_2k.png"));
        Boulder7.setMetallicMap(TextureLoader.loadTexture("rock3_rough_2k.png"));
        Boulder7.setRoughnessMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder7.setAoMap(TextureLoader.loadTexture("rock3_ao_2k.png"));
        Boulder7.setParallaxScale(new Vector3f(0.01f, 20, 60));
        Boulder7.setCastShadows(true);
        texturedModels.put("boulder_7", Boulder7);
  
        
        //tall grass
        TexturedModel tallGrass = new TexturedModel();
        tallGrass.setMesh(ObjLoader.loadObj("tallGrass3"));
        tallGrass.setTextureId(TextureLoader.loadVegetationTexture("grassy3.png"));
        tallGrass.setVegitation(true);
        tallGrass.setHasTransparency(true);
        tallGrass.setHasOpaque(false);
        tallGrass.setCastShadows(false);
        //tallGrass.setShineDamper(0.5f);
        texturedModels.put("tall_grass", tallGrass);
        
        //blades3
        TexturedModel blades3 = new TexturedModel();
        blades3.setMesh(ObjLoader.loadObj("blades3"));
        blades3.setTextureId(TextureLoader.loadExplicitTexture("blades7.png"));
        blades3.setHasTransparency(true);
        blades3.setHasOpaque(false);
        blades3.setCastShadows(false);
        texturedModels.put("blades_3", blades3);
		
	}
	
	

}
