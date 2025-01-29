package debugRenderer;

import org.joml.Vector3f;

import java.util.List;

public class DebugLine implements DebugObject {
    private final Vector3f start;
    private final Vector3f end;
    private final Vector3f color;

    public DebugLine(Vector3f start, Vector3f end, Vector3f color) {
        this.start = new Vector3f(start);
        this.end = new Vector3f(end);
        this.color = new Vector3f(color);
    }

    public void setStart(Vector3f newStart) {
        this.start.set(newStart);
    }

    public void setEnd(Vector3f newEnd) {
        this.end.set(newEnd);
    }

    @Override
    public void appendVertexData(List<Float> vertexData) {
        for (Vector3f point : new Vector3f[]{start, end}) {
            vertexData.add(point.x);
            vertexData.add(point.y);
            vertexData.add(point.z);
            vertexData.add(color.x);
            vertexData.add(color.y);
            vertexData.add(color.z);
        }
    }
}
