package toolbox;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.joml.Vector4f;

public class Frustum {
    private static final Vector4f[] coefficients = new Vector4f[6];
    private float bufferDistance = 1.0f; // Adjust this value as needed

    /**
     * Sets the buffer distance for the frustum.
     * 
     * @param bufferDistance The distance to buffer behind the viewpoint.
     */
    public void setBufferDistance(float bufferDistance) {
        this.bufferDistance = bufferDistance;
    }

    /**
     * Calculates the frustum planes based on the projection and view matrices,
     * then offsets them by the buffer distance.
     *
     * @param projectionMatrix The projection matrix.
     * @param view             The view matrix.
     */
    public void calculateFrustum(Matrix4f projectionMatrix, Matrix4f view) {
        // Combine the projection and view matrices
        Matrix4f combined = new Matrix4f();
        projectionMatrix.mul(view, combined);

        // Extract planes from the combined matrix
        coefficients[0] = new Vector4f(
                combined.m00() + combined.m03(), // Left
                combined.m10() + combined.m13(),
                combined.m20() + combined.m23(),
                combined.m30() + combined.m33()
        );
        coefficients[1] = new Vector4f(
                combined.m03() - combined.m00(), // Right
                combined.m13() - combined.m10(),
                combined.m23() - combined.m20(),
                combined.m33() - combined.m30()
        );
        coefficients[2] = new Vector4f(
                combined.m01() + combined.m03(), // Bottom
                combined.m11() + combined.m13(),
                combined.m21() + combined.m23(),
                combined.m31() + combined.m33()
        );
        coefficients[3] = new Vector4f(
                combined.m03() - combined.m01(), // Top
                combined.m13() - combined.m11(),
                combined.m23() - combined.m21(),
                combined.m33() - combined.m31()
        );
        coefficients[4] = new Vector4f(
                combined.m02() + combined.m03(), // Near
                combined.m12() + combined.m13(),
                combined.m22() + combined.m23(),
                combined.m32() + combined.m33()
        );
        coefficients[5] = new Vector4f(
                combined.m03() - combined.m02(), // Far
                combined.m13() - combined.m12(),
                combined.m23() - combined.m22(),
                combined.m33() - combined.m32()
        );

        // Normalize and offset each plane
        for (int i = 0; i < 6; i++) {
            normalizeAndOffsetPlane(coefficients[i], bufferDistance);
        }
    }

    /**
     * Normalizes a plane and offsets it by the buffer distance.
     *
     * @param plane  The plane to normalize and offset.
     * @param buffer The buffer distance.
     */
    private void normalizeAndOffsetPlane(Vector4f plane, float buffer) {
        float length = plane.x * plane.x + plane.y * plane.y + plane.z * plane.z;
        length = (float) Math.sqrt(length);
        plane.x /= length;
        plane.y /= length;
        plane.z /= length;
        plane.w /= length;

        // Offset the plane outward by the buffer distance
        // Positive buffer moves the plane outward along its normal
        plane.w += buffer;
    }

    /**
     * Checks if a sphere is inside the frustum.
     *
     * @param position The center of the sphere.
     * @param radius   The radius of the sphere.
     * @return True if the sphere is inside the frustum, false otherwise.
     */
    public boolean contains(Vector3f position, float radius) {
        Vector4f point = new Vector4f(position, 1.0f);
        for (int i = 0; i < 6; i++) {
            float distance = coefficients[i].dot(point);
            if (distance < -radius) {
                return false; // Sphere is completely outside this plane
            }
        }
        return true; // Sphere is inside or intersects all planes
    }
}
