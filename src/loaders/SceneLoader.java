package loaders;

import entities.Entity;
import org.joml.Vector3f;
import toolbox.Material;
import toolbox.Mesh;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class SceneLoader {

    /**
     * Loads a scene from a single OBJ file (located in "res/") that contains multiple objects.
     * The file is parsed in memory (no temporary files are created) to split it into groups.
     * Material assignments are determined by the first "usemtl" token in each group.
     *
     * @param fullObjFileName The scene OBJ file name (e.g., "sponza.obj") in "res/"
     * @param mtlFileName     The MTL file name (e.g., "sponza.mtl") in "res/"
     * @return A list of Entity objects corresponding to the objects in the scene.
     */
    public static List<Entity> loadScene(String fullObjFileName, String mtlFileName) {
        final String RES_LOC = "res/";
        List<Entity> entities = new ArrayList<>();

        // Read the entire OBJ file into a list of lines.
        List<String> allLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(RES_LOC + fullObjFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        // Extract header lines (vertex, texture coordinate, normal, etc.).
        List<String> headerLines = new ArrayList<>();
        for (String line : allLines) {
            if (line.startsWith("v ") || line.startsWith("vt ") || 
                line.startsWith("vn ") || line.startsWith("vp ")) {
                headerLines.add(line);
            }
        }

        // Split the OBJ file into groups based on "o" (object) or "g" (group) tokens.
        // Each group will be stored as a list of lines (excluding the header).
        Map<String, List<String>> objectLinesMap = new LinkedHashMap<>();
        String currentObject = "default";
        objectLinesMap.put(currentObject, new ArrayList<>());
        for (String line : allLines) {
            line = line.trim();
            if (line.startsWith("o ") || line.startsWith("g ")) {
                String[] tokens = line.split("\\s+");
                currentObject = tokens.length >= 2 ? tokens[1] : "default";
                if (!objectLinesMap.containsKey(currentObject)) {
                    objectLinesMap.put(currentObject, new ArrayList<>());
                }
            }
            // Exclude header lines—they will be added separately.
            if (!(line.startsWith("v ") || line.startsWith("vt ") || 
                  line.startsWith("vn ") || line.startsWith("vp "))) {
                objectLinesMap.get(currentObject).add(line);
            }
        }

        // Load materials from the MTL file.
        Map<String, Material> materials = MTLLoader.loadMTL(mtlFileName);

        // For each object/group, combine header and group lines, extract material, and load the mesh.
        for (Map.Entry<String, List<String>> entry : objectLinesMap.entrySet()) {
            String objectName = entry.getKey();
            List<String> groupLines = entry.getValue();

            // Combine header lines and group-specific lines.
            List<String> combinedLines = new ArrayList<>();
            combinedLines.addAll(headerLines);
            combinedLines.addAll(groupLines);

            // Determine the material name by scanning the group's lines for the first "usemtl" token.
            String materialName = "";
            for (String l : groupLines) {
                if (l.startsWith("usemtl")) {
                    String[] tokens = l.split("\\s+");
                    if (tokens.length > 1) {
                        materialName = tokens[1];
                    }
                    break;
                }
            }

            // Load the mesh from the combined lines.
            Mesh mesh = ObjLoader.loadMeshFromLines(combinedLines);

            // Retrieve texture IDs and material properties from the Material.
            int diffuseTextureId = 0;
            int normalTextureId = 0;
            int metallicMapId = 0;
            int roughnessMapId = 0;
            int aoMapId = 0;
            int heightMapId = 0;
            float shineDamper = 0;
            float reflectivity = 0;
            if (materials.containsKey(materialName)) {
                Material mat = materials.get(materialName);
                diffuseTextureId = mat.diffuseTextureId;
                normalTextureId = mat.normalTextureId;
                metallicMapId = mat.metallicMapId;
                roughnessMapId = mat.roughnessMapId;
                aoMapId = mat.aoMapId;
                heightMapId = mat.heightMapId;
                shineDamper = mat.shineDamper;
                reflectivity = mat.reflectivity;
            } else {
                System.err.println("Material not found for object '" + objectName + "': " + materialName);
            }

            // Create an Entity using identity transform (world-space data is assumed to be pre-transformed).
            Entity entity = new Entity(mesh, diffuseTextureId, new Vector3f(0, 0, 0),
                                       new Vector3f(0, 0, 0), 1.0f);
            if (normalTextureId != 0)
                entity.setNormalMapId(normalTextureId);
            if (shineDamper != 0)
                entity.setShineDamper(shineDamper);
            if (reflectivity != 0)
                entity.setReflectivity(reflectivity);
            if (metallicMapId != 0)
                entity.setMetallicMap(metallicMapId);
            if (roughnessMapId != 0)
                entity.setRoughnessMap(roughnessMapId);
            if (aoMapId != 0)
                entity.setAoMap(aoMapId);
            if (heightMapId != 0) {
                entity.setHeighMapId(heightMapId);
                entity.setParallaxScale(new Vector3f(0.01f, 120, 160));
            }
            
            System.out.println(diffuseTextureId + ", " + normalTextureId + ", " + 
            		metallicMapId + ", " + 
            		roughnessMapId + ", " + 
            		aoMapId + ", " + 
            		heightMapId + ", " +
            		shineDamper + ", "        
            		+ reflectivity);

            entities.add(entity);
        }
        return entities;
    }

}
