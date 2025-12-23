package fbxLoader;


import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Stack;

public class FBXParser {

	  /**
     * Loads an FBX file and returns the root FBXNode.
     * 
     * @param filePath The path to the FBX file.
     * @return The root FBXNode representing the parsed FBX structure.
     */
    public static FBXNode loadFBXFile(String filePath) {
        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(filePath));
            String line;
            Stack<FBXNode> nodeStack = new Stack<>();
            FBXNode root = null;

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue; // Skip empty lines
                }

                // Check for end of a node
                if (line.equals("}")) {
                    if (!nodeStack.isEmpty()) {
                        nodeStack.pop();
                    }
                    continue;
                }

                // Check for start of a node
                if (line.endsWith("{")) {
                    // Remove the trailing '{' and trim
                    String nodeName = line.substring(0, line.length() - 1).trim();
                    // Remove any trailing ':' if present
                    if (nodeName.endsWith(":")) {
                        nodeName = nodeName.substring(0, nodeName.length() - 1).trim();
                    }
                    FBXNode newNode = new FBXNode(nodeName);

                    if (nodeStack.isEmpty()) {
                        root = newNode; // This is the root node
                    } else {
                        nodeStack.peek().addChild(newNode);
                    }
                    nodeStack.push(newNode);
                } else {
                    // This line should be a property or data
                    if (!nodeStack.isEmpty()) {
                        parsePropertyLine(line, nodeStack.peek());
                    }
                }
            }

            reader.close();
            return root;
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("Failed to read the FBX file: " + filePath);
            return null;
        }
    }

    /**
     * Parses a property line and adds it to the given FBXNode.
     * 
     * @param line The line containing the property.
     * @param currentNode The current FBXNode to add the property to.
     */
    private static void parsePropertyLine(String line, FBXNode currentNode) {
        // Example property lines:
        // P: "Name", "KString", "", "", "Model::Cube"
        // P: "Vertices", "a", "Vector3", "Array", "0,0,0,1,0,0,1,1,0,0,1,0"
        // K: "Version",1

        if (line.startsWith("P:")) {
            // Property with multiple values
            // Extract the property name and its values
            String content = line.substring(2).trim(); // Remove "P:"
            // Split by commas not within quotes
            String[] parts = splitIgnoringQuotes(content, ',');
            if (parts.length >= 5) {
                String key = stripQuotes(parts[0].trim());
                String value = stripQuotes(parts[4].trim());
                currentNode.addAttribute(key, value);
            }
        } else if (line.startsWith("K:")) {
            // Single key-value property
            String content = line.substring(2).trim(); // Remove "K:"
            String[] parts = splitIgnoringQuotes(content, ',');
            if (parts.length == 2) {
                String key = stripQuotes(parts[0].trim());
                String value = parts[1].trim();
                currentNode.addAttribute(key, value);
            }
        } else if (line.startsWith("C:")) {
            // Canvas property or other types, can be handled similarly
            // For simplicity, treat as a single data string
            String content = line.substring(2).trim(); // Remove "C:"
            currentNode.setData(content);
        } else {
            // Other types of properties can be handled here
            // For now, ignore or implement as needed
        }
    }

    /**
     * Splits a string by a delimiter, ignoring delimiters within quotes.
     * 
     * @param input The input string to split.
     * @param delimiter The delimiter character.
     * @return An array of split strings.
     */
    private static String[] splitIgnoringQuotes(String input, char delimiter) {
        boolean inQuotes = false;
        StringBuilder sb = new StringBuilder();
        java.util.List<String> parts = new java.util.ArrayList<>();

        for (char c : input.toCharArray()) {
            if (c == '\"') {
                inQuotes = !inQuotes;
            } else if (c == delimiter && !inQuotes) {
                parts.add(sb.toString());
                sb.setLength(0);
                continue;
            }
            sb.append(c);
        }
        parts.add(sb.toString());
        return parts.toArray(new String[0]);
    }

    /**
     * Strips leading and trailing quotes from a string, if present.
     * 
     * @param input The input string.
     * @return The string without leading and trailing quotes.
     */
    private static String stripQuotes(String input) {
        if (input.startsWith("\"") && input.endsWith("\"") && input.length() >= 2) {
            return input.substring(1, input.length() - 1);
        }
        return input;
    }
}
