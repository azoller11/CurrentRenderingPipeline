package debugRenderer;


import org.joml.Vector3f;
import java.util.List;

public class DebugCircle implements DebugObject {

    private final Vector3f center;
    private final float radius;
    private final Vector3f color;
    private final int segments;

    public DebugCircle(Vector3f center, float radius, Vector3f color) {
        this(center, radius, color, 48); // default ~smooth circle
    }

    public DebugCircle(Vector3f center, float radius, Vector3f color, int segments) {
        this.center = new Vector3f(center);
        this.radius = radius;
        this.color = new Vector3f(color);
        this.segments = segments;
    }

    @Override
    public void appendVertexData(List<Float> data) {
        float fixedY = center.y; // circle lies flat on XZ plane

        float step = (float)(Math.PI * 2.0 / segments);

        for (int i = 0; i < segments; i++) {
            float a0 = i * step;
            float a1 = (i + 1) * step;

            float x0 = center.x + (float)Math.cos(a0) * radius;
            float z0 = center.z + (float)Math.sin(a0) * radius;

            float x1 = center.x + (float)Math.cos(a1) * radius;
            float z1 = center.z + (float)Math.sin(a1) * radius;

            // Line segment start
            data.add(x0); data.add(fixedY); data.add(z0);
            data.add(color.x); data.add(color.y); data.add(color.z);

            // Line segment end
            data.add(x1); data.add(fixedY); data.add(z1);
            data.add(color.x); data.add(color.y); data.add(color.z);
        }
    }
}
