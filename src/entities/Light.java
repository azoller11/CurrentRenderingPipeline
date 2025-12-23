package entities;

import org.joml.Vector3f;

public class Light {
	
	private int Id;
	
    private Vector3f position;     
    private Vector3f color;
    private Vector3f attenuation;  
    private boolean castShadow;

    /**
     * Constructor for a point light.
     */
    public Light(Vector3f position, Vector3f color, Vector3f attenuation) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
        this.attenuation = new Vector3f(attenuation);
        
        this.Id = position.hashCode();
    }

    /**
     * Constructor for a directional light (e.g., the sun).
     * For a directional light, the position is less meaningful because its rays are parallel.
     * Instead, the direction indicates where the light comes from.
     */
    public Light(Vector3f position, Vector3f color) {
        this.color = new Vector3f(color);
        // For directional lights, you might set a default position (or leave it at zero).
        this.position = position;
        // Attenuation is not used for directional lights.
        this.attenuation = new Vector3f(1, 0, 0);
        
        this.Id = position.hashCode();
    }

    // Getters and setters

    public Vector3f getPosition() {
        return position;
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
    }

    public Vector3f getColor() {
        return color;
    }

    public void setColor(Vector3f newColor) {
        this.color.set(newColor);
    }

    public Vector3f getAttenuation() {
        return attenuation;
    }
    
    

    public void setAttenuation(Vector3f attenuation) {
		this.attenuation = attenuation;
	}

	public boolean isCastShadow() {
        return castShadow;
    }

    public void setCastShadow(boolean castShadow) {
        this.castShadow = castShadow;
    }

    

    // Existing methods for attenuation calculations remain unchanged.

    public float getEffectiveDistance() {
        final float threshold = 0.01f;
        float a = attenuation.x;
        float b = attenuation.y;
        float c = attenuation.z;

        if (c != 0) {
            float discriminant = b * b - 4 * c * (a - 1 / threshold);
            if (discriminant < 0) {
                return 0;
            }
            return (float)((-b + Math.sqrt(discriminant)) / (2 * c));
        } else if (b != 0) {
            return (1 / threshold - a) / b;
        } else {
            return Float.POSITIVE_INFINITY;
        }
    }

    public static float calculateLightContribution(Light light, Entity entity) {
        float effectiveDistance = light.getEffectiveDistance();
        float distance = entity.getPosition().distance(light.getPosition());

        if (distance > effectiveDistance) {
            return 0.0f;
        }

        Vector3f att = light.getAttenuation();
        float a = att.x;
        float b = att.y;
        float c = att.z;

        return 1.0f / (a + b * distance + c * distance * distance);
    }

	public int getId() {
		return Id;
	}

	public void setId(int id) {
		Id = id;
	}
}
