package terrain;

public class TerrainTextureLayer {
    public int albedoMap;
    public int normalMap;
    public int metallicMap;
    public int roughnessMap;
    public int aoMap;
    public int heightMap;

    public boolean hasNormal;
    public boolean hasHeight;
    public boolean hasMetallic;
    public boolean hasRoughness;
    public boolean hasAo;

    public float tileScale;
    public float r, g, b; // mask color

    public TerrainTextureLayer(int albedoMap, float r, float g, float b) {
        this.albedoMap = albedoMap;
        this.r = r;
        this.g = g;
        this.b = b;
        this.tileScale = 1f;
    }

    public TerrainTextureLayer(int albedoMap, float tileScale, float r, float g, float b) {
        this.albedoMap = albedoMap;
        this.tileScale = tileScale;
        this.r = r;
        this.g = g;
        this.b = b;
    }

    // ---------------------------------------------------
    // AUTO-FLAGGING SETTERS
    // ---------------------------------------------------

    public void setAlbedoMap(int albedoMap) {
        this.albedoMap = albedoMap;
    }

    public void setNormalMap(int normalMap) {
        this.normalMap = normalMap;
        this.hasNormal = (normalMap != 0);
    }

    public void setMetallicMap(int metallicMap) {
        this.metallicMap = metallicMap;
        this.hasMetallic = (metallicMap != 0);
    }

    public void setRoughnessMap(int roughnessMap) {
        this.roughnessMap = roughnessMap;
        this.hasRoughness = (roughnessMap != 0);
    }

    public void setAoMap(int aoMap) {
        this.aoMap = aoMap;
        this.hasAo = (aoMap != 0);
    }

    public void setHeightMap(int heightMap) {
        this.heightMap = heightMap;
        this.hasHeight = (heightMap != 0);
    }

    // ---------------------------------------------------
    // Getters (unchanged)
    // ---------------------------------------------------

    public int getAlbedoMap() { return albedoMap; }
    public int getNormalMap() { return normalMap; }
    public int getMetallicMap() { return metallicMap; }
    public int getRoughnessMap() { return roughnessMap; }
    public int getAoMap() { return aoMap; }
    public int getHeightMap() { return heightMap; }

    public boolean isHasNormal() { return hasNormal; }
    public boolean isHasHeight() { return hasHeight; }
    public boolean isHasMetallic() { return hasMetallic; }
    public boolean isHasRoughness() { return hasRoughness; }
    public boolean isHasAo() { return hasAo; }

    public float getTileScale() { return tileScale; }
    public void setTileScale(float tileScale) { this.tileScale = tileScale; }

    public float getR() { return r; }
    public float getG() { return g; }
    public float getB() { return b; }

    public void setR(float r) { this.r = r; }
    public void setG(float g) { this.g = g; }
    public void setB(float b) { this.b = b; }
}
