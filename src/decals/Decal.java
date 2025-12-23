package decals;

import org.joml.Matrix4f;
import org.joml.Vector3f;

public class Decal {
    public Vector3f position;
    public Vector3f rotation;
    public Vector3f size;
    public int textureId;
    public int normalTextureId;  // New: normal map texture ID
    public DecalType decalType;
  

	public static enum DecalType {
		BULLET
	}
    
  
    
    public Decal(Vector3f position, Vector3f rotation, Vector3f size, 
                 int textureId, int normalTextureId, DecalType decalType) {
        this.position = position;
        this.rotation = rotation;
        this.size = size;
        this.textureId = textureId;
        this.normalTextureId = normalTextureId;
        this.decalType = decalType;
    }
    
    // Getters and setters...
    public int getNormalTextureId() {
        return normalTextureId;
    }
    
    public void setNormalTextureId(int normalTextureId) {
        this.normalTextureId = normalTextureId;
    }
    
   
    
    public Vector3f getPosition() {
		return position;
	}

	public void setPosition(Vector3f position) {
		this.position = position;
	}

	public Vector3f getRotation() {
		return rotation;
	}

	public void setRotation(Vector3f rotation) {
		this.rotation = rotation;
	}

	public Vector3f getSize() {
		return size;
	}

	public void setSize(Vector3f size) {
		this.size = size;
	}

	public int getTextureId() {
		return textureId;
	}

	public void setTextureId(int textureId) {
		this.textureId = textureId;
	}
	
	  
    public DecalType getDecalType() {
		return decalType;
	}

	public void setDecalType(DecalType decalType) {
		this.decalType = decalType;
	}

	public Matrix4f getModelMatrix() {
        return new Matrix4f()
            .identity()
            .translate(position.x, position.y, position.z)
            .rotateXYZ(rotation.x, rotation.y, rotation.z)
            .scale(size.x, size.y, size.z);
    }
    
    public Matrix4f getProjectionMatrix() {
        return getModelMatrix().invert(new Matrix4f());
    }
}