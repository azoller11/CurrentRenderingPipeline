package debugRenderer;

import org.joml.Vector3f;

import java.util.List;

public class DebugPoint implements DebugObject {
    private final Vector3f position;
    private final Vector3f color;

    public DebugPoint(Vector3f position, Vector3f color) {
        this.position = new Vector3f(position);
        this.color = new Vector3f(color);
    }

    public void setPosition(Vector3f newPosition) {
        this.position.set(newPosition);
    }

    @Override
    public void appendVertexData(List<Float> vertexData) {
        vertexData.add(position.x);
        vertexData.add(position.y);
        vertexData.add(position.z);
        vertexData.add(color.x);
        vertexData.add(color.y);
        vertexData.add(color.z);
    }
}
