package debugRenderer;

import org.joml.Vector3f;

import java.util.List;

public class DebugTriangle implements DebugObject {
    private final Vector3f v1, v2, v3;
    private final Vector3f color;

    public DebugTriangle(Vector3f v1, Vector3f v2, Vector3f v3, Vector3f color) {
        this.v1 = new Vector3f(v1);
        this.v2 = new Vector3f(v2);
        this.v3 = new Vector3f(v3);
        this.color = new Vector3f(color);
    }

    @Override
    public void appendVertexData(List<Float> vertexData) {
        for (Vector3f point : new Vector3f[]{v1, v2, v3}) {
            vertexData.add(point.x);
            vertexData.add(point.y);
            vertexData.add(point.z);
            vertexData.add(color.x);
            vertexData.add(color.y);
            vertexData.add(color.z);
        }
    }
}
