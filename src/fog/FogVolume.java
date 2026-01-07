package fog;

import org.joml.Matrix4f;
import org.joml.Vector4f;

public class FogVolume {

    private final Matrix4f modelMatrix;
    private float uniformScale;
    
    private int noiseTexture;

    private float density = 0.003f;
    private float noiseScale = 40.0f;
    private float noiseStrength = 0.6f;
    private float baseOpacity = 1.0f;

    private final Vector4f fogColor = new Vector4f(0.9f, 0.95f, 1.0f, 1.0f);

    public FogVolume(Matrix4f modelMatrix, float uniformScale, int noiseTexture) {
        this.modelMatrix = new Matrix4f(modelMatrix);
        this.uniformScale = uniformScale;
        this.noiseTexture = noiseTexture;
    }
    
    

    public int getNoiseTexture() {
		return noiseTexture;
	}



	public void setNoiseTexture(int noiseTexture) {
		this.noiseTexture = noiseTexture;
	}



	public Matrix4f getModelMatrix() {
        return modelMatrix;
    }

    public float getUniformScale() {
        return uniformScale;
    }

    public void setUniformScale(float s) {
        this.uniformScale = s;
    }

    public float getDensity() {
        return density;
    }

    public void setDensity(float density) {
        this.density = density;
    }

    public float getNoiseScale() {
        return noiseScale;
    }

    public void setNoiseScale(float noiseScale) {
        this.noiseScale = noiseScale;
    }

    public float getNoiseStrength() {
        return noiseStrength;
    }

    public void setNoiseStrength(float noiseStrength) {
        this.noiseStrength = noiseStrength;
    }

    public float getBaseOpacity() {
        return baseOpacity;
    }

    public void setBaseOpacity(float baseOpacity) {
        this.baseOpacity = baseOpacity;
    }

    public Vector4f getFogColor() {
        return fogColor;
    }
}
