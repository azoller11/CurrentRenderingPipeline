package toolbox;

public class Material {
    public String name;
    
    // Texture IDs corresponding to the textures loaded via your TextureLoader
    public int diffuseTextureId; // corresponds to Entity.textureId
    public int normalTextureId;  // corresponds to Entity.normalMapId
    public int metallicMapId;    // corresponds to Entity.metallicMap
    public int roughnessMapId;   // corresponds to Entity.roughnessMap
    public int aoMapId;          // corresponds to Entity.aoMap
    public int heightMapId;      // corresponds to Entity.heighMapId (or heightMapId)
    
    // Material properties
    public float shineDamper;    // e.g., from "Ns"
    public float reflectivity;   // additional property

    @Override
    public String toString() {
        return "Material{" +
                "name='" + name + '\'' +
                ", diffuseTextureId=" + diffuseTextureId +
                ", normalTextureId=" + normalTextureId +
                ", metallicMapId=" + metallicMapId +
                ", roughnessMapId=" + roughnessMapId +
                ", aoMapId=" + aoMapId +
                ", heightMapId=" + heightMapId +
                ", shineDamper=" + shineDamper +
                ", reflectivity=" + reflectivity +
                '}';
    }
}
