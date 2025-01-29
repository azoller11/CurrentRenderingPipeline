package debugRenderer;

import org.joml.Vector3f;

import java.util.List;

public class DebugSphere implements DebugObject {
    private final Vector3f center;
    private final float radius;
    private final Vector3f color;

    public DebugSphere(Vector3f center, float radius, Vector3f color) {
        this.center = new Vector3f(center);
        this.radius = radius;
        this.color = new Vector3f(color);
    }

    @Override
    public void appendVertexData(List<Float> vertexData) {
        int stacks = 16; // Number of horizontal slices
        int slices = 16; // Number of vertical slices

        for (int i = 0; i < stacks; i++) {
            float theta1 = (float) Math.PI * i / stacks;
            float theta2 = (float) Math.PI * (i + 1) / stacks;

            for (int j = 0; j < slices; j++) {
                float phi1 = (float) (2 * Math.PI * j / slices);
                float phi2 = (float) (2 * Math.PI * (j + 1) / slices);

                Vector3f v1 = computeSphereVertex(theta1, phi1);
                Vector3f v2 = computeSphereVertex(theta2, phi1);
                Vector3f v3 = computeSphereVertex(theta2, phi2);
                Vector3f v4 = computeSphereVertex(theta1, phi2);

                // Add lines for the edges of the quad
                addLineData(vertexData, v1, v2);
                addLineData(vertexData, v2, v3);
                addLineData(vertexData, v3, v4);
                addLineData(vertexData, v4, v1);
            }
        }
    }

    private Vector3f computeSphereVertex(float theta, float phi) {
        return new Vector3f(
                center.x + radius * (float) (Math.sin(theta) * Math.cos(phi)),
                center.y + radius * (float) Math.cos(theta),
                center.z + radius * (float) (Math.sin(theta) * Math.sin(phi))
        );
    }

    private void addLineData(List<Float> vertexData, Vector3f v1, Vector3f v2) {
        for (Vector3f point : new Vector3f[]{v1, v2}) {
            vertexData.add(point.x);
            vertexData.add(point.y);
            vertexData.add(point.z);
            vertexData.add(color.x);
            vertexData.add(color.y);
            vertexData.add(color.z);
        }
    }
}
