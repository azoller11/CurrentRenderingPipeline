package entities;

import org.joml.Matrix4f;
import org.joml.Vector3f;

import toolbox.Equations;

import static org.lwjgl.opengl.GL40.*;



public class Light {
    private Vector3f position;
    private Vector3f color;
    private Vector3f attenuation; // (constant, linear, quadratic)
    private boolean castShadow;
  

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

  
    public float getEffectiveDistance() {
        // Fixed threshold for minimum significant light intensity.
        final float threshold = 0.01f;
        
        // Attenuation components: a = constant, b = linear, c = quadratic.
        float a = attenuation.x;
        float b = attenuation.y;
        float c = attenuation.z;
        
        // We set up the equation where the intensity equals the threshold:
        //    1 / (a + b*d + c*d^2) = threshold
        // Rearranged, this becomes:
        //    c*d^2 + b*d + (a - 1/threshold) = 0
        // Solve this quadratic equation for d.
        
        // When c is non-zero, use the quadratic formula.
        if (c != 0) {
            float discriminant = b * b - 4 * c * (a - 1 / threshold);
            if (discriminant < 0) {
                // No real solution; return 0 as a safe default.
                return 0;
            }
            // Only the positive root is meaningful for distance.
            float distance = (float)((-b + Math.sqrt(discriminant)) / (2 * c));
            return distance;
        }
        // If c is zero but b is non-zero, then the equation is linear.
        else if (b != 0) {
            float distance = (1 / threshold - a) / b;
            return distance;
        }
        // If both c and b are zero, the light does not attenuate with distance.
        else {
            return Float.POSITIVE_INFINITY;
        }
    }

    public static float calculateLightContribution(Light light, Entity entity) {
        // Get the effective distance (range) from the light
        float effectiveDistance = light.getEffectiveDistance();
        
        // Calculate the actual distance between the light and the entity.
        // Here we use the Euclidean distance between the two Vector3f positions.
        float distance = Equations.calculateDistance(light.getPosition(), entity.getPosition());
        
        // If the entity is farther than the effective distance, the light has negligible impact.
        if (distance > effectiveDistance) {
            return 0.0f;
        }
        
        // Otherwise, calculate the light intensity using the attenuation formula:
        // intensity = 1 / (a + b*d + c*d^2)
        Vector3f att = light.getAttenuation();
        float a = att.x;   // constant term
        float b = att.y;   // linear term
        float c = att.z;   // quadratic term
        
        float intensity = 1.0f / (a + b * distance + c * distance * distance);
        return intensity;
    }

    public Vector3f getPosition() { return position; }
    public Vector3f getColor() { return color; }
    public Vector3f getAttenuation() { return attenuation; }
    public void setPosition(Vector3f newPosition) { this.position.set(newPosition); }
	public void setColor(Vector3f lerp) {this.color.set(lerp);}
	public boolean isCastShadow() {return castShadow;}
	public void setCastShadow(boolean castShadow) {this.castShadow = castShadow;}
}
