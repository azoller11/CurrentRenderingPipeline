package grass;

import org.joml.Vector3f;

public class Grass {
    private Vector3f position;
    
    public Grass(Vector3f position) {
        this.position = position;
    }
    
    public Vector3f getPosition() {
        return position;
    }
}
