package fbxLoader;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class FBXNode {
    
    private String name;
    private Map<String, String> attributes;
    private String data;
    private List<FBXNode> children;

    /**
     * Constructs an FBXNode with the specified name.
     * 
     * @param name The name of the node.
     */
    public FBXNode(String name) {
        this.name = name;
        this.attributes = new HashMap<>();
        this.children = new ArrayList<>();
        this.data = null;
    }

    /**
     * Retrieves the name of the node.
     * 
     * @return The node's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets the name of the node.
     * 
     * @param name The new name for the node.
     */
    public void setName(String name) {
        this.name = name;
    }

    /**
     * Adds an attribute to the node.
     * 
     * @param key   The attribute's key.
     * @param value The attribute's value.
     */
    public void addAttribute(String key, String value) {
        attributes.put(key, value);
    }

    /**
     * Retrieves the value of a specific attribute.
     * 
     * @param key The attribute's key.
     * @return The attribute's value, or null if not found.
     */
    public String getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Retrieves all attributes of the node.
     * 
     * @return A map of attribute keys and their corresponding values.
     */
    public Map<String, String> getAttributes() {
        return attributes;
    }

    /**
     * Sets the data associated with the node.
     * 
     * @param data The data to set.
     */
    public void setData(String data) {
        this.data = data;
    }

    /**
     * Retrieves the data associated with the node.
     * 
     * @return The node's data, or null if none is set.
     */
    public String getData() {
        return data;
    }

    /**
     * Adds a child node to this node.
     * 
     * @param child The child FBXNode to add.
     */
    public void addChild(FBXNode child) {
        children.add(child);
    }

    /**
     * Retrieves all child nodes of this node.
     * 
     * @return A list of child FBXNodes.
     */
    public List<FBXNode> getChildren() {
        return children;
    }

    /**
     * Retrieves the first child node with the specified name.
     * 
     * @param name The name of the child node to retrieve.
     * @return The first matching child FBXNode, or null if not found.
     */
    public FBXNode getChild(String name) {
        for (FBXNode child : children) {
            if (child.getName().equalsIgnoreCase(name)) {
                return child;
            }
        }
        return null;
    }

    /**
     * Retrieves all child nodes with the specified name.
     * 
     * @param name The name of the child nodes to retrieve.
     * @return A list of matching child FBXNodes.
     */
    public List<FBXNode> getChildrenByName(String name) {
        List<FBXNode> matchingChildren = new ArrayList<>();
        for (FBXNode child : children) {
            if (child.getName().equalsIgnoreCase(name)) {
                matchingChildren.add(child);
            }
        }
        return matchingChildren;
    }

    /**
     * Checks if the node has any children.
     * 
     * @return True if the node has one or more children; false otherwise.
     */
    public boolean hasChildren() {
        return !children.isEmpty();
    }

    /**
     * Removes a child node from this node.
     * 
     * @param child The child FBXNode to remove.
     * @return True if the child was successfully removed; false otherwise.
     */
    public boolean removeChild(FBXNode child) {
        return children.remove(child);
    }

    /**
     * Removes all child nodes from this node.
     */
    public void removeAllChildren() {
        children.clear();
    }

    /**
     * Provides a string representation of the FBXNode for debugging purposes.
     * 
     * @return A string detailing the node's name, attributes, data, and child count.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("FBXNode{name='").append(name).append('\'');
        if (!attributes.isEmpty()) {
            sb.append(", attributes=").append(attributes);
        }
        if (data != null) {
            sb.append(", data='").append(data).append('\'');
        }
        if (!children.isEmpty()) {
            sb.append(", children=").append(children.size());
        }
        sb.append('}');
        return sb.toString();
    }
}