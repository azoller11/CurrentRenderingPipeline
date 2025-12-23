package loaders;

import entities.Entity;
import entities.TexturedModel;
import org.joml.Vector3f;
import toolbox.Material;
import toolbox.Mesh;
import toolbox.MeshData;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.concurrent.*;

public class SceneLoader { 

    private static final int MAX_CONCURRENT_JOBS = 4; // Limit concurrent jobs
    private static final int MAX_TOTAL_JOBS = 500;     // Limit total jobs to prevent memory issues

    private static class MeshDataResult {
        MeshData meshData;
        String materialName;
        String objectName;

        public MeshDataResult(MeshData meshData, String materialName, String objectName) {
            this.meshData = meshData;
            this.materialName = materialName;
            this.objectName = objectName;
        }
    }

    public static List<Entity> loadScene(String fullObjFileName, String mtlFileName) {
        final String RES_LOC = "res/";
        List<Entity> entities = new ArrayList<>();

        // 1. Read the entire OBJ file into a list of lines.
        List<String> allLines = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new FileReader(RES_LOC + fullObjFileName))) {
            String line;
            while ((line = reader.readLine()) != null) {
                allLines.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
            return entities;
        }

        // 2. Extract header lines
        List<String> headerLines = new ArrayList<>();
        for (String line : allLines) {
            if (line.startsWith("v ") || line.startsWith("vt ") ||
                line.startsWith("vn ") || line.startsWith("vp ")) {
                headerLines.add(line);
            }
        }

        // 3. Split the OBJ file into groups
        Map<String, List<String>> objectLinesMap = new LinkedHashMap<>();
        String currentObject = "default";
        objectLinesMap.put(currentObject, new ArrayList<>());
        for (String line : allLines) {
            line = line.trim();
            if (line.startsWith("o ") || line.startsWith("g ")) {
                String[] tokens = line.split("\\s+");
                currentObject = tokens.length >= 2 ? tokens[1] : "default";
                objectLinesMap.putIfAbsent(currentObject, new ArrayList<>());
            }
            if (!(line.startsWith("v ") || line.startsWith("vt ") ||
                  line.startsWith("vn ") || line.startsWith("vp "))) {
                objectLinesMap.get(currentObject).add(line);
            }
        }

        // 4. Load materials
        Map<String, Material> materials = MTLLoader.loadMTL(mtlFileName);

        // 5. Limit the number of objects to process
        if (objectLinesMap.size() > MAX_TOTAL_JOBS) {
            System.err.println("[SceneLoader] Warning: Too many objects (" + objectLinesMap.size() + 
                             "). Limiting to " + MAX_TOTAL_JOBS);
            // Keep only the first MAX_TOTAL_JOBS objects
            objectLinesMap = objectLinesMap.entrySet().stream()
                .limit(MAX_TOTAL_JOBS)
                .collect(LinkedHashMap::new, (m, e) -> m.put(e.getKey(), e.getValue()), Map::putAll);
        }

        // 6. Create limited thread pool
        System.out.println("[SceneLoader] Processing " + objectLinesMap.size() + " objects with " + 
                         MAX_CONCURRENT_JOBS + " concurrent jobs");
        
        ExecutorService executor = Executors.newFixedThreadPool(MAX_CONCURRENT_JOBS);
        List<Future<MeshDataResult>> futures = new ArrayList<>();

        // 7. Submit tasks with progress tracking
        int submittedCount = 0;
        for (Map.Entry<String, List<String>> entry : objectLinesMap.entrySet()) {
            final String objectName = entry.getKey();
            final List<String> groupLines = entry.getValue();

            submittedCount++;
            System.out.println("[SceneLoader] Submitting job " + submittedCount + "/" + 
                             objectLinesMap.size() + ": " + objectName);
            
            Future<MeshDataResult> future = executor.submit(() -> {
                System.out.println("[Async Job] Started: " + objectName);
                List<String> combinedLines = new ArrayList<>();
                combinedLines.addAll(headerLines);
                combinedLines.addAll(groupLines);

                MeshData meshData = ObjLoader.parseMeshDataFromLines(combinedLines);

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
                System.out.println("[Async Job] Finished: " + objectName);
                return new MeshDataResult(meshData, materialName, objectName);
            });
            futures.add(future);
        }

        executor.shutdown();
        
        // 8. Process results with timeout protection
        int processedCount = 0;
        int totalJobs = futures.size();
        
        for (Future<MeshDataResult> future : futures) {
            try {
                // Add timeout to prevent hanging
                MeshDataResult result = future.get(30, TimeUnit.SECONDS);
                processedCount++;
                
                System.out.println("[SceneLoader] Processing result (" + processedCount + "/" + 
                                 totalJobs + "): " + result.objectName);

                // Create mesh and entity on main thread
                processMeshResult(result, materials, entities);

            } catch (TimeoutException e) {
                System.err.println("[SceneLoader] Timeout processing object, skipping...");
                future.cancel(true);
            } catch (InterruptedException | ExecutionException e) {
                System.err.println("[SceneLoader] Error processing object: " + e.getMessage());
                e.printStackTrace();
            }
        }
        
        System.out.println("[SceneLoader] Completed: " + entities.size() + " entities loaded");
        return entities;
    }

    private static void processMeshResult(MeshDataResult result, Map<String, Material> materials, List<Entity> entities) {
        try {
            Mesh mesh = new Mesh(result.meshData);

            int diffuseTextureId = 0;
            int normalTextureId = 0;
            int metallicMapId = 0;
            int roughnessMapId = 0;
            int aoMapId = 0;
            int heightMapId = 0;
            float shineDamper = 0;
            float reflectivity = 0;
            
            if (materials.containsKey(result.materialName)) {
                Material mat = materials.get(result.materialName);
                diffuseTextureId = mat.diffuseTextureId;
                normalTextureId = mat.normalTextureId;
                metallicMapId = mat.metallicMapId;
                roughnessMapId = mat.roughnessMapId;
                aoMapId = mat.aoMapId;
                heightMapId = mat.heightMapId;
                shineDamper = mat.shineDamper;
                reflectivity = mat.reflectivity;
            } else {
                System.err.println("[SceneLoader] Material not found: " + result.materialName);
            }

          
            
            TexturedModel tm = new TexturedModel();
            
            
            if (normalTextureId != 0) tm.setNormalMapId(normalTextureId);
            if (shineDamper != 0) tm.setShineDamper(shineDamper);
            if (reflectivity != 0) tm.setReflectivity(reflectivity);
            if (metallicMapId != 0) tm.setMetallicMap(metallicMapId);
            if (roughnessMapId != 0) tm.setRoughnessMap(roughnessMapId);
            if (aoMapId != 0) tm.setAoMap(aoMapId);
            if (heightMapId != 0) {
            	tm.setHeighMapId(heightMapId);
            	tm.setParallaxScale(new Vector3f(0.01f, 120, 160));
            }
            
            Entity entity = new Entity(tm, new Vector3f(0, 0, 0),
                    new Vector3f(0, 0, 0), 1.0f);

            entities.add(entity);
            
        } catch (Exception e) {
            System.err.println("[SceneLoader] Error creating entity for: " + result.objectName);
            e.printStackTrace();
        }
    }
}