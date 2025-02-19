package loaders;

import entities.Entity;
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

    /**
     * Container class to hold the asynchronous task's result.
     */
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

    /**
     * Loads a scene from an OBJ file with multiple objects.
     * Mesh data is parsed asynchronously and OpenGL resource creation is deferred to the main thread.
     *
     * @param fullObjFileName The OBJ file name (e.g., "sponza.obj") in "res/"
     * @param mtlFileName     The MTL file name (e.g., "sponza.mtl") in "res/"
     * @return A list of Entity objects for the scene.
     */
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
        }

        // 2. Extract header lines (vertex, texture coordinate, normal, etc.).
        List<String> headerLines = new ArrayList<>();
        for (String line : allLines) {
            if (line.startsWith("v ") || line.startsWith("vt ") ||
                line.startsWith("vn ") || line.startsWith("vp ")) {
                headerLines.add(line);
            }
        }

        // 3. Split the OBJ file into groups based on "o" (object) or "g" (group) tokens.
        // Each group is stored as a list of lines (excluding the header).
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
            // Exclude header lines—they will be added separately.
            if (!(line.startsWith("v ") || line.startsWith("vt ") ||
                  line.startsWith("vn ") || line.startsWith("vp "))) {
                objectLinesMap.get(currentObject).add(line);
            }
        }

        // 4. Load materials from the MTL file.
        Map<String, Material> materials = MTLLoader.loadMTL(mtlFileName);

        // 5. Create an ExecutorService for asynchronous CPU-bound work.
        int threads = Runtime.getRuntime().availableProcessors();
        System.out.println("[SceneLoader] Launching async tasks using " + threads + " threads.");
        ExecutorService executor = Executors.newFixedThreadPool(threads);
        List<Future<MeshDataResult>> futures = new ArrayList<>();

        // 6. For each object/group, submit a task to parse its mesh data.
        for (Map.Entry<String, List<String>> entry : objectLinesMap.entrySet()) {
            final String objectName = entry.getKey();
            final List<String> groupLines = entry.getValue();

            System.out.println("[SceneLoader] Submitting async job for object: " + objectName);
            Future<MeshDataResult> future = executor.submit(() -> {
                System.out.println("[Async Job] Started processing object: " + objectName);
                // Combine header lines and group-specific lines.
                List<String> combinedLines = new ArrayList<>();
                combinedLines.addAll(headerLines);
                combinedLines.addAll(groupLines);

                // Parse OBJ data into MeshData without making any OpenGL calls.
                // (You must implement parseMeshDataFromLines in your ObjLoader.)
                MeshData meshData = ObjLoader.parseMeshDataFromLines(combinedLines);

                // Determine the material name from the group lines (first "usemtl" token).
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
                System.out.println("[Async Job] Finished processing object: " + objectName);
                return new MeshDataResult(meshData, materialName, objectName);
            });
            futures.add(future);
        }

        // Shutdown the executor as we have submitted all tasks.
        executor.shutdown();
        System.out.println("[SceneLoader] All async jobs submitted. Waiting for results...");

        // 7. In the main thread (with the OpenGL context), create the Mesh and Entities.
        int processedCount = 0;
        int totalJobs = futures.size();
        for (Future<MeshDataResult> future : futures) {
            try {
                MeshDataResult result = future.get(); // Wait for the asynchronous task to complete.
                processedCount++;
                System.out.println("[SceneLoader] Processing result (" + processedCount + "/" + totalJobs + ") for object: " + result.objectName);

                // Create the Mesh on the main thread (this involves OpenGL calls).
                Mesh mesh = new Mesh(result.meshData);

                // Retrieve material properties.
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
                    System.err.println("[SceneLoader] Material not found for object '" + result.objectName + "': " + result.materialName);
                }

                // Create the Entity using an identity transform (adjust as needed).
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

                entities.add(entity);

            } catch (InterruptedException | ExecutionException e) {
                e.printStackTrace();
            }
        }
        System.out.println("[SceneLoader] All async jobs completed and processed. Total entities: " + entities.size());
        return entities;
    }
}
