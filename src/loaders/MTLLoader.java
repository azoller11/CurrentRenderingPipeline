package loaders;

import java.io.*;
import java.util.HashMap;
import java.util.Map;
import toolbox.Material;
import loaders.TextureLoader;

public class MTLLoader {

    private static final String RES_LOC = "res/";

    /**
     * Normalizes a texture file path:
     * - Replaces backslashes with forward slashes.
     * - If the path starts with "textures/", that part is removed.
     * 
     * @param path The original texture path from the MTL file.
     * @return The normalized texture file name.
     */
    private static String normalizeTexturePath(String path) {
        String normalized = path.replace("\\", "/");
        if (normalized.startsWith("textures/")) {
            normalized = normalized.substring("textures/".length());
        }
        return normalized;
    }

    /**
     * Loads a MTL file and returns a mapping from material names to Material objects.
     * It looks for diffuse texture definitions under both "map_Ka" and "map_Kd",
     * and for normal maps under "map_bump" or "bump".
     *
     * @param mtlFileName The filename of the MTL file (e.g., "sponza.mtl")
     * @return a Map of material names to Material objects.
     */
    public static Map<String, Material> loadMTL(String mtlFileName) {
        Map<String, Material> materials = new HashMap<>();
        File file = new File(RES_LOC + mtlFileName);
        Material currentMaterial = null;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            while((line = reader.readLine()) != null) {
                line = line.trim();
                // Skip empty lines or comments.
                if (line.isEmpty() || line.startsWith("#")) {
                    continue;
                }
                if (line.startsWith("newmtl")) {
                    String[] tokens = line.split("\\s+");
                    if (tokens.length >= 2) {
                        currentMaterial = new Material();
                        currentMaterial.name = tokens[1];
                        materials.put(currentMaterial.name, currentMaterial);
                    }
                } else if (currentMaterial != null) {
                    // Look for diffuse texture using either map_Kd or map_Ka.
                    if (line.startsWith("map_Kd") || line.startsWith("map_Ka")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            // Only load once if not already set.
                            if (currentMaterial.diffuseTextureId == 0) {
                                currentMaterial.diffuseTextureId = TextureLoader.loadTexture(texFile);
                                try {
                                	if (texFile.replace("diff", "mask") != texFile)
                                		currentMaterial.roughnessMapId = TextureLoader.loadTexture(texFile.replace("diff", "mask"));
                                } catch(Exception e) {}
                                try {
                                	if (texFile.replace("diff", "spec") != texFile)
                                		currentMaterial.metallicMapId = TextureLoader.loadTexture(texFile.replace("diff", "spec"));
                                	    currentMaterial.roughnessMapId = TextureLoader.loadTexture(texFile.replace("diff", "spec"));
                                } catch(Exception e) {}
                                try {
                                	if (texFile.replace("diff", "bump") != texFile)
                                		currentMaterial.heightMapId = TextureLoader.loadTexture(texFile.replace("diff", "bump"));
                                } catch(Exception e) {}
                               
                                try {
                                	if (texFile.replace("diff", "ddn") != texFile)
                                		currentMaterial.normalTextureId = TextureLoader.loadTexture(texFile.replace("diff", "ddn"));
                                } catch(Exception e) {}
                                
                                try {
                                	if (texFile.replace("diff", "NRM") != texFile)
                                		currentMaterial.normalTextureId = TextureLoader.loadTexture(texFile.replace("diff", "NRM"));
                                } catch(Exception e) {}
                                
                            }
                        }
                    }
                    /*
                    // Optionally, you can also check for an alpha mask with "map_d"
                    // if desired.
                    // Look for normal map using "map_bump" or "bump".
                    else if (line.startsWith("map_bump") || line.startsWith("bump")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            if (currentMaterial.normalTextureId == 0) {
                                currentMaterial.normalTextureId = TextureLoader.loadTexture(texFile);
                            }
                        }
                    }
                    // Parse shininess value (Ns).
                    else if (line.startsWith("Ns")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            currentMaterial.shineDamper = Float.parseFloat(tokens[1]);
                        }
                    }
                    // Optionally parse reflectivity (using "Ni" in this example).
                    else if (line.startsWith("Ni")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            currentMaterial.reflectivity = Float.parseFloat(tokens[1]);
                        }
                    }
                    // Optionally, parse additional maps if your MTLs include them.
                    else if (line.startsWith("map_metallic")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            currentMaterial.metallicMapId = TextureLoader.loadTexture(texFile);
                        }
                    }
                    else if (line.startsWith("map_roughness")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            currentMaterial.roughnessMapId = TextureLoader.loadTexture(texFile);
                        }
                    }
                    else if (line.startsWith("map_AO")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            currentMaterial.aoMapId = TextureLoader.loadTexture(texFile);
                        }
                    }
                    else if (line.startsWith("map_height") || line.startsWith("map_disp")) {
                        String[] tokens = line.split("\\s+");
                        if (tokens.length >= 2) {
                            String texFile = normalizeTexturePath(tokens[1]);
                            currentMaterial.heightMapId = TextureLoader.loadTexture(texFile);
                        }
                    }
                    */
                }
            }
        } catch (IOException e) {
            System.err.println("Error reading MTL file: " + file.getAbsolutePath());
            e.printStackTrace();
        }
        return materials;
    }
}
