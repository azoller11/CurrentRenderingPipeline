package toolbox;


import java.io.*;
import java.util.*;

public class ObjSceneSplitter {

    /**
     * Splits a single OBJ file (which represents a full scene) into separate temporary OBJ files—one per object/group.
     * The method returns a mapping from object names to the temporary file names (without extension)
     * that can be passed to your original ObjLoader.
     *
     * @param fullObjFileName The original OBJ file name (e.g., "sponza.obj") located in "res/"
     * @return Map from object name to temporary file base name (e.g., "temp_objName")
     */
    public static Map<String, String> splitObj(String fullObjFileName) {
        final String RES_LOC = "res/";
        List<String> headerLines = new ArrayList<>();
        // Map from object name to list of lines (its face data, plus any usemtl etc.)
        Map<String, List<String>> objectData = new LinkedHashMap<>();
        // Also, track material per object (we’ll re-read it later from the temporary file if needed)
        // For now, we only need the object name to later load the mesh.
        
        String currentObject = "default"; // default group if none defined
        // Make sure there is an entry for the default object.
        objectData.put(currentObject, new ArrayList<>());
        
        File objFile = new File(RES_LOC + fullObjFileName);
        try (BufferedReader reader = new BufferedReader(new FileReader(objFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if(line.isEmpty()) continue;
                // Collect header lines (vertex, uv, normal, etc.)
                if(line.startsWith("v ") || line.startsWith("vt ") || line.startsWith("vn ") || line.startsWith("vp ")) {
                    headerLines.add(line);
                    continue;
                }
                // When a new object or group is defined:
                if(line.startsWith("o ") || line.startsWith("g ")) {
                    // Get the object name (the token after "o" or "g")
                    String[] tokens = line.split("\\s+");
                    if(tokens.length >= 2) {
                        currentObject = tokens[1];
                    } else {
                        currentObject = "default";
                    }
                    // Create a new list for this object if not already present.
                    if(!objectData.containsKey(currentObject)) {
                        objectData.put(currentObject, new ArrayList<>());
                    }
                    // Optionally, add the group header to the object's data.
                    objectData.get(currentObject).add(line);
                    continue;
                }
                // Otherwise, assign the line (e.g., usemtl, f, etc.) to the current object.
                objectData.get(currentObject).add(line);
            }
        } catch(IOException e) {
            e.printStackTrace();
        }
        
        // Now, for each object, write a temporary OBJ file that includes the header lines and that object's data.
        Map<String, String> objectToTempFile = new LinkedHashMap<>();
        for (Map.Entry<String, List<String>> entry : objectData.entrySet()) {
            String objName = entry.getKey();
            List<String> lines = new ArrayList<>();
            // Include header lines first.
            lines.addAll(headerLines);
            // Then the object-specific lines.
            lines.addAll(entry.getValue());
            
            // Define a temporary file name.
            // (We remove any spaces or invalid characters from the object name as needed.)
            String tempBaseName = "temp_" + objName.replaceAll("\\s+", "_");
            String tempFileName = RES_LOC + tempBaseName + ".obj";
            File tempFile = new File(tempFileName);
            try (PrintWriter writer = new PrintWriter(new FileWriter(tempFile))) {
                for(String l : lines) {
                    writer.println(l);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            // Map the object name to the base file name (without ".obj")
            objectToTempFile.put(objName, tempBaseName);
        }
        return objectToTempFile;
    }
}
