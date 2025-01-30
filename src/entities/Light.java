package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL40.*;


public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; // (constant, linear, quadratic)

    // Shadow mapping properties
    private int shadowMapFBO;
    private int shadowMapTexture;
    private Matrix4f lightSpaceMatrix;

    // Shadow map dimensions
    public static final int SHADOW_WIDTH = 2048;
    public static final int SHADOW_HEIGHT = 2048;

    //point lights
    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
    }

    //directional lights
    public Light(Vector3f direction, Vector3f color) {
        this.position = new Vector3f(direction);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(1, 0, 0); // indicates directional
    }

  

    /**
     * Determines if the light is directional based on its attenuation.
     *
     * @return True if directional, false otherwise.
     */
    public boolean isDirectional() {
        return attenuation.x == 1.0f && attenuation.y == 0 && attenuation.z == 0;
    }

    // Getters for shadow mapping
    public int getShadowMapFBO() {
        return shadowMapFBO;
    }

    public int getShadowMapTexture() {
        return shadowMapTexture;
    }

    public Matrix4f getLightSpaceMatrix() {
        return lightSpaceMatrix;
    }

    // Existing getters
    public Vector3f getPosition() { return position; }
    public Vector3f getColor() { return color; }
    public Vector3f getAttenuation() { return attenuation; }
}
