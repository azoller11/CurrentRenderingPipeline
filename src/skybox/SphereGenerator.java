package skybox;

import java.util.ArrayList;
import java.util.List;

public class SphereGenerator {
    private final float[] vertices;
    private final int[] indices;

    public SphereGenerator(int resolution) {
        List<Float> vertexList = new ArrayList<>();
        List<Integer> indexList = new ArrayList<>();

        for (int i = 0; i <= resolution; i++) {
            double lat = Math.PI * (-0.5 + (double) i / resolution);
            float y = (float) Math.sin(lat);
            float radius = (float) Math.cos(lat);

            for (int j = 0; j <= resolution; j++) {
                double lon = 2 * Math.PI * (double) j / resolution;
                float x = radius * (float) Math.cos(lon);
                float z = radius * (float) Math.sin(lon);

                vertexList.add(x);
                vertexList.add(y);
                vertexList.add(z);

                if (i < resolution && j < resolution) {
                    int a = i * (resolution + 1) + j;
                    int b = a + resolution + 1;

                    indexList.add(a);
                    indexList.add(b);
                    indexList.add(a + 1);

                    indexList.add(b);
                    indexList.add(b + 1);
                    indexList.add(a + 1);
                }
            }
        }

        vertices = new float[vertexList.size()];
        for (int i = 0; i < vertexList.size(); i++) {
            vertices[i] = vertexList.get(i);
        }

        indices = new int[indexList.size()];
        for (int i = 0; i < indexList.size(); i++) {
            indices[i] = indexList.get(i);
        }
    }

    public float[] getVertices() {
        return vertices;
    }

    public int[] getIndices() {
        return indices;
    }
}