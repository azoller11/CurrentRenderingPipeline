package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import static org.lwjgl.opengl.GL40.*;


public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; // (constant, linear, quadratic)




    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
    }

    public Light(Vector3f direction, Vector3f color) {
        this.position = new Vector3f(direction);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(1, 0, 0);
    }

  

    /**
     * Determines if the light is directional based on its attenuation.
     *
     * @return True if directional, false otherwise.
     */
    public boolean isDirectional() {
        return attenuation.x == 1.0f && attenuation.y == 0 && attenuation.z == 0;
    }


    // Existing getters
    public Vector3f getPosition() { return position; }
    public Vector3f getColor() { return color; }
    public Vector3f getAttenuation() { return attenuation; }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition); // Instead of replacing the object, modify it in place
    }

	public void setColor(Vector3f lerp) {
		 this.color.set(lerp);
		// TODO Auto-generated method stub
		
	}
}
