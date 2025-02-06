package toolbox;

import org.joml.Vector3f;

public class Equations {
	
	public static float calculateDistance(Vector3f pointA, Vector3f pointB) {
        float dx = pointB.x - pointA.x;
        float dy = pointB.y - pointA.y;
        float dz = pointB.z - pointA.z;
        
        return (float) Math.sqrt(dx * dx + dy * dy + dz * dz);
    }
  

}
