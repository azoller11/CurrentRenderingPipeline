package animatedModel;

import org.joml.Quaternionf;
import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class to store animation keys for a single bone.
 */
public class BoneAnimation {
    private final String boneName;
    private final List<Float> scalingTimes;
    private final List<Vector3f> scalingValues;

    private final List<Float> rotationTimes;
    private final List<Quaternionf> rotationValues;

    private final List<Float> positionTimes;
    private final List<Vector3f> positionValues;

    /**
     * Constructs a BoneAnimation for the specified bone.
     *
     * @param boneName The name of the bone.
     */
    public BoneAnimation(String boneName) {
        this.boneName = boneName;
        this.scalingTimes = new ArrayList<>();
        this.scalingValues = new ArrayList<>();
        this.rotationTimes = new ArrayList<>();
        this.rotationValues = new ArrayList<>();
        this.positionTimes = new ArrayList<>();
        this.positionValues = new ArrayList<>();
    }

    // ------------------ Scaling Methods ------------------

    /**
     * Adds a scaling key to the bone animation.
     *
     * @param time  The timestamp of the scaling key.
     * @param scale The scaling vector.
     */
    public void addScalingKey(float time, Vector3f scale) {
        scalingTimes.add(time);
        scalingValues.add(scale);
    }

    /**
     * Retrieves the number of scaling keys.
     *
     * @return The number of scaling keys.
     */
    public int getNumScalingKeys() {
        return scalingTimes.size();
    }

    /**
     * Retrieves the timestamp of a specific scaling key.
     *
     * @param index The index of the scaling key.
     * @return The timestamp of the scaling key.
     */
    public float getScalingTime(int index) {
        return scalingTimes.get(index);
    }

    /**
     * Retrieves the scaling vector of a specific scaling key.
     *
     * @param index The index of the scaling key.
     * @return The scaling vector.
     */
    public Vector3f getScalingValue(int index) {
        return scalingValues.get(index);
    }

    // ------------------ Rotation Methods ------------------

    /**
     * Adds a rotation key to the bone animation.
     *
     * @param time     The timestamp of the rotation key.
     * @param rotation The rotation quaternion.
     */
    public void addRotationKey(float time, Quaternionf rotation) {
        rotationTimes.add(time);
        rotationValues.add(rotation);
    }

    /**
     * Retrieves the number of rotation keys.
     *
     * @return The number of rotation keys.
     */
    public int getNumRotationKeys() {
        return rotationTimes.size();
    }

    /**
     * Retrieves the timestamp of a specific rotation key.
     *
     * @param index The index of the rotation key.
     * @return The timestamp of the rotation key.
     */
    public float getRotationTime(int index) {
        return rotationTimes.get(index);
    }

    /**
     * Retrieves the rotation quaternion of a specific rotation key.
     *
     * @param index The index of the rotation key.
     * @return The rotation quaternion.
     */
    public Quaternionf getRotationValue(int index) {
        return rotationValues.get(index);
    }

    // ------------------ Position Methods ------------------

    /**
     * Adds a position key to the bone animation.
     *
     * @param time     The timestamp of the position key.
     * @param position The position vector.
     */
    public void addPositionKey(float time, Vector3f position) {
        positionTimes.add(time);
        positionValues.add(position);
    }

    /**
     * Retrieves the number of position keys.
     *
     * @return The number of position keys.
     */
    public int getNumPositionKeys() {
        return positionTimes.size();
    }

    /**
     * Retrieves the timestamp of a specific position key.
     *
     * @param index The index of the position key.
     * @return The timestamp of the position key.
     */
    public float getPositionTime(int index) {
        return positionTimes.get(index);
    }

    /**
     * Retrieves the position vector of a specific position key.
     *
     * @param index The index of the position key.
     * @return The position vector.
     */
    public Vector3f getPositionValue(int index) {
        return positionValues.get(index);
    }

    /**
     * Retrieves the name of the bone.
     *
     * @return The bone name.
     */
    public String getBoneName() {
        return boneName;
    }
}
