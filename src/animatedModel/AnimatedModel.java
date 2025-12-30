package animatedModel;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;

import toolbox.Maths;

public class AnimatedModel
{
    private final int vaoID;
    private int count;

    Bone[] bones;
    AIAnimation[] animations;
    AINode root;
    
    public AnimatedModel(int vaoID, int count)
    {
        this.vaoID = vaoID;
        this.count = count;
    }

    public int getVaoID()
    {
        return vaoID;
    }

    public int getCount()
    {
        return count;
    }
    
    public void updateAnimationBlended(int animationIndex, int animationIndex2, float time, float time2, float blend)
    {
        assert animationIndex >= 0 && animationIndex < animations.length;
        updateBoneTransformationBlended(time/60f, time2/60f, animationIndex, animationIndex2, blend);
    }

    public void updateAnimation(int animationIndex, float time)
    {
        assert animationIndex >= 0 && animationIndex < animations.length;
        updateBoneTransformation(time/60f, animationIndex);
    }
    
    private void updateBoneTransformationBlended(float timeInSeconds, float timeInSeconds2, int animationIndex, int animationIndex2, float blend) {
        Matrix4f identity = new Matrix4f();

        AIAnimation target = animations[animationIndex];
        AIAnimation target2 = animations[animationIndex2];

        float ticksPerSecond = target.mTicksPerSecond() != 0 ? (float) target.mTicksPerSecond() : 60.0f;
        float ticks = timeInSeconds * ticksPerSecond;
        float animationTime = (ticks % (float) target.mDuration());
        
        ticksPerSecond = target2.mTicksPerSecond() != 0 ? (float) target2.mTicksPerSecond() : 60.0f;
        ticks = timeInSeconds2 * ticksPerSecond;
        float animationTime2 = (ticks % (float) target2.mDuration());

        processNode(target, target2, animationTime, animationTime2, root, identity, blend);
    }

    private void updateBoneTransformation(float timeInSeconds, int animationIndex)
    {
        Matrix4f identity = new Matrix4f();

        AIAnimation target = animations[animationIndex];

        float ticksPerSecond = target.mTicksPerSecond() != 0 ? (float) target.mTicksPerSecond() : 60.0f;
        float ticks = timeInSeconds * ticksPerSecond;
        float animationTime = (ticks % (float) target.mDuration());

        processNode(target, animationTime, root, identity);
    }
    
   private void processNode(AIAnimation target, AIAnimation target2, float animationTime, float animationTime2, AINode node, 
    Matrix4f parentTransform, float blend)
{
    String nodeName = node.mName().dataString();
    System.out.println("Processing node: " + nodeName);

    Matrix4f nodeTransform = Maths.convertMatrix(node.mTransformation());

    AINodeAnim boneAnimation = findBoneAnimation(target, nodeName);
    AINodeAnim boneAnimation2 = findBoneAnimation(target2, nodeName);
    
    System.out.println("  Found boneAnimation: " + (boneAnimation != null) + 
                      ", boneAnimation2: " + (boneAnimation2 != null));

    // If this node refers bone (contains animation), Do interpolate transforms.
    if (boneAnimation != null || boneAnimation2 != null)
    {
        System.out.println("  Animating node: " + nodeName);
        
        // Handle scaling
        Vector3f blendedScale;
        if (boneAnimation != null && boneAnimation2 != null)
        {
            System.out.println("    Both animations have this bone, blending");
            Vector3f interpolatedScale = calcInterpolatedScale(animationTime, boneAnimation);
            Vector3f interpolatedScale2 = calcInterpolatedScale(animationTime2, boneAnimation2);
            blendedScale = new Vector3f();
            interpolatedScale.lerp(interpolatedScale2, blend, blendedScale);
        }
        else if (boneAnimation != null)
        {
            System.out.println("    Only first animation has this bone");
            blendedScale = calcInterpolatedScale(animationTime, boneAnimation);
        }
        else
        {
            System.out.println("    Only second animation has this bone");
            blendedScale = calcInterpolatedScale(animationTime2, boneAnimation2);
        }
        System.out.println("    Final scale: " + blendedScale);
        Matrix4f scaleMatrix = new Matrix4f().scale(blendedScale);

        // Handle rotation
        Quaternionf blendedRotation;
        if (boneAnimation != null && boneAnimation2 != null)
        {
            Quaternionf interpolatedRotation = calcInterpolatedRotation(animationTime, boneAnimation);
            Quaternionf interpolatedRotation2 = calcInterpolatedRotation(animationTime2, boneAnimation2);
            blendedRotation = new Quaternionf();
            interpolatedRotation.slerp(interpolatedRotation2, blend, blendedRotation);
        }
        else if (boneAnimation != null)
        {
            blendedRotation = calcInterpolatedRotation(animationTime, boneAnimation);
        }
        else
        {
            blendedRotation = calcInterpolatedRotation(animationTime2, boneAnimation2);
        }
        Matrix4f rotationMatrix = new Matrix4f().rotate(blendedRotation);

        // Handle position
        Vector3f blendedPosition;
        if (boneAnimation != null && boneAnimation2 != null)
        {
            Vector3f interpolatedPosition = calcInterpolatedPosition(animationTime, boneAnimation);
            Vector3f interpolatedPosition2 = calcInterpolatedPosition(animationTime2, boneAnimation2);
            blendedPosition = new Vector3f();
            interpolatedPosition.lerp(interpolatedPosition2, blend, blendedPosition);
        }
        else if (boneAnimation != null)
        {
            blendedPosition = calcInterpolatedPosition(animationTime, boneAnimation);
        }
        else
        {
            blendedPosition = calcInterpolatedPosition(animationTime2, boneAnimation2);
        }
        Matrix4f translationMatrix = new Matrix4f().translate(blendedPosition);

        nodeTransform = Maths.mul(translationMatrix, rotationMatrix, scaleMatrix);
    }

    Matrix4f toGlobalSpace = Maths.mul(parentTransform, nodeTransform);

    Bone bone = findBone(nodeName);

    if (bone != null) {
        //System.out.println("  Found bone, setting transformation");
        bone.setTransformation(Maths.mul(toGlobalSpace, bone.getOffsetMatrix()));
    } else {
        //System.out.println("  No bone found for node: " + nodeName);
    }

    // Recursively process the child nodes
    //System.out.println("  Processing " + node.mNumChildren() + " children");
    for (int i = 0; i < node.mNumChildren(); i++)
    {
        AINode childNode = AINode.create(node.mChildren().get(i));
        processNode(target, target2, animationTime, animationTime2, childNode, toGlobalSpace, blend);
    }
}
    
    
    private void processNode(AIAnimation target, float animationTime, AINode node, Matrix4f parentTransform)
    {
        String nodeName = node.mName().dataString();

        Matrix4f nodeTransform = Maths.convertMatrix(node.mTransformation());

        AINodeAnim boneAnimation = findBoneAnimation(target, nodeName);

        // If this node refers bone (contains animation), Do interpolate transforms.
        if (boneAnimation != null)
        {
            Vector3f interpolatedScale = calcInterpolatedScale(animationTime, boneAnimation);
            Matrix4f scaleMatrix = new Matrix4f().scale(interpolatedScale);

            Quaternionf interpolatedRotation = calcInterpolatedRotation(animationTime, boneAnimation);
            Matrix4f rotationMatrix = new Matrix4f().rotate(interpolatedRotation);

            Vector3f interpolatedPosition = calcInterpolatedPosition(animationTime, boneAnimation);
            Matrix4f translationMatrix = new Matrix4f().translate(interpolatedPosition);

            nodeTransform = Maths.mul(translationMatrix, rotationMatrix, scaleMatrix);
        }

        Matrix4f toGlobalSpace = Maths.mul(parentTransform, nodeTransform);

        Bone bone = findBone(nodeName);

        if (bone != null)
            bone.setTransformation(Maths.mul(toGlobalSpace, bone.getOffsetMatrix()));
        // Recursively process the child nodes
        for (int i = 0; i < node.mNumChildren(); i++)
        {
            AINode childNode = AINode.create(node.mChildren().get(i));
            processNode(target, animationTime, childNode, toGlobalSpace);
        }
    }

    // Each node has a name. If that node is bone, the node name equals to bone name.
    private AINodeAnim findBoneAnimation(AIAnimation target, String nodeName)
    {
        for (int i = 0; i < target.mNumChannels(); i++)
        {
            AINodeAnim nodeAnim = AINodeAnim.create(target.mChannels().get(i));

            if (nodeAnim.mNodeName().dataString().equals(nodeName))
                return nodeAnim;
        }

        return null;
    }

    private org.joml.Vector3f calcInterpolatedScale(float timeAt, AINodeAnim boneAnimation)
{
    // Debug: Print bone animation info
   // System.out.println("calcInterpolatedScale called with boneAnimation: " + 
        //(boneAnimation == null ? "null" : boneAnimation.mNodeName().dataString()));
    
    if (boneAnimation == null) {
        //System.out.println("  boneAnimation is null, returning default scale");
        return new org.joml.Vector3f(1, 1, 1); // Default scale
    }
    
    //System.out.println("  Number of scaling keys: " + boneAnimation.mNumScalingKeys());
    
    // Check if there are any scaling keys at all
    if (boneAnimation.mNumScalingKeys() == 0) {
        //System.out.println("  No scaling keys, returning default scale");
        return new org.joml.Vector3f(1, 1, 1); // Default scale
    }
    
    if (boneAnimation.mNumScalingKeys() == 1) {
        AIVectorKey key = boneAnimation.mScalingKeys().get(0);
        //System.out.println("  Single key, value: " + 
          //  (key == null ? "null key" : 
          //   (key.mValue() == null ? "null value" : "has value")));
        
        if (key == null || key.mValue() == null) {
           // System.out.println("  Key or value is null, returning default scale");
            return new org.joml.Vector3f(1, 1, 1); // Default scale
        }
        
        try {
            org.joml.Vector3f result = Maths.convertVector(key.mValue());
           // System.out.println("  Returning single key value: " + result);
            return result;
        } catch (Exception e) {
           // System.out.println("  Error converting vector: " + e.getMessage());
            return new org.joml.Vector3f(1, 1, 1); // Default scale
        }
    }

    int index0 = findScaleIndex(timeAt, boneAnimation);
    int index1 = index0 + 1;
    
   // System.out.println("  Found indices: " + index0 + ", " + index1);
    
    // Check if indices are valid
    if (index0 < 0 || index0 >= boneAnimation.mNumScalingKeys() || 
        index1 < 0 || index1 >= boneAnimation.mNumScalingKeys()) {
      //  System.out.println("  Invalid indices, returning default scale");
        return new org.joml.Vector3f(1, 1, 1); // Default scale
    }
    
    AIVectorKey key0 = boneAnimation.mScalingKeys().get(index0);
    AIVectorKey key1 = boneAnimation.mScalingKeys().get(index1);
    
   // System.out.println("  Key0: " + (key0 == null ? "null" : "not null") + 
     //                  ", Key1: " + (key1 == null ? "null" : "not null"));
    //
    // Check if keys or their values are null
    if (key0 == null || key0.mValue() == null || key1 == null || key1.mValue() == null) {
      //  System.out.println("  Keys or values are null, returning default scale");
        return new org.joml.Vector3f(1, 1, 1); // Default scale
    }
    
    try {
        float time0 = (float) key0.mTime();
        float time1 = (float) key1.mTime();
        float deltaTime = time1 - time0;
        float percentage = (timeAt - time0) / deltaTime;
        
       // System.out.println("  Times: " + time0 + " -> " + time1 + ", percentage: " + percentage);

        org.joml.Vector3f start = Maths.convertVector(key0.mValue());
        org.joml.Vector3f end = Maths.convertVector(key1.mValue());
        
       // System.out.println("  Start: " + start + ", End: " + end);
        
        // Check if conversion returned null
        if (start == null || end == null) {
           // System.out.println("  Start or end is null after conversion");
            return new org.joml.Vector3f(1, 1, 1); // Default scale
        }
        
        org.joml.Vector3f delta = Maths.sub(end, start);
        
        //System.out.println("  Delta: " + delta);
        
        // Check if subtraction returned null
        if (delta == null) {
            //System.out.println("  Delta is null");
            return new org.joml.Vector3f(1, 1, 1); // Default scale
        }

        org.joml.Vector3f result = Maths.sum(start, delta.mul(percentage));
        //System.out.println("  Result: " + result);
        return result != null ? result : new org.joml.Vector3f(1, 1, 1);
    } catch (Exception e) {
        System.out.println("  Exception during interpolation: " + e.getMessage());
        e.printStackTrace();
        return new org.joml.Vector3f(1, 1, 1); // Default scale
    }
}
    private int findScaleIndex(float timeAt, AINodeAnim boneAnimation)
    {
        assert boneAnimation.mNumScalingKeys() > 0;

        for (int i = 0; i < boneAnimation.mNumScalingKeys() - 1; i++)
        {
            if (timeAt < boneAnimation.mScalingKeys().get(i + 1).mTime())
                return i;
        }

        return 0;
    }

   private Quaternionf calcInterpolatedRotation(float timeAt, AINodeAnim boneAnimation)
{
   // System.out.println("  calcInterpolatedRotation called for: " + boneAnimation.mNodeName().dataString());
   // System.out.println("    Number of rotation keys: " + boneAnimation.mNumRotationKeys());
    
    if (boneAnimation == null) {
        return new Quaternionf();
    }
    
    if (boneAnimation.mNumRotationKeys() == 0) {
      //  System.out.println("    No rotation keys");
        return new Quaternionf();
    }
    
    if (boneAnimation.mNumRotationKeys() == 1) {
        AIQuatKey key = boneAnimation.mRotationKeys().get(0);
        if (key == null || key.mValue() == null) {
            return new Quaternionf();
        }
        AIQuaternion value = key.mValue();
       // System.out.println("    Single rotation key: (" + value.x() + ", " + value.y() + ", " + value.z() + ", " + value.w() + ")");
        return new Quaternionf(value.x(), value.y(), value.z(), value.w());
    }

    int index0 = findRotationIndex(timeAt, boneAnimation);
    int index1 = index0 + 1;
    
    //System.out.println("    Found rotation indices: " + index0 + ", " + index1);
    
    if (index0 < 0 || index0 >= boneAnimation.mNumRotationKeys() || 
        index1 < 0 || index1 >= boneAnimation.mNumRotationKeys()) {
       // System.out.println("    Invalid rotation indices");
        return new Quaternionf();
    }
    
    AIQuatKey key0 = boneAnimation.mRotationKeys().get(index0);
    AIQuatKey key1 = boneAnimation.mRotationKeys().get(index1);
    
    if (key0 == null || key0.mValue() == null || key1 == null || key1.mValue() == null) {
        //System.out.println("    Rotation keys or values are null");
        return new Quaternionf();
    }
    
    AIQuaternion value0 = key0.mValue();
    AIQuaternion value1 = key1.mValue();
    
    float time0 = (float) key0.mTime();
    float time1 = (float) key1.mTime();
    float deltaTime = time1 - time0;
    float percentage = (timeAt - time0) / deltaTime;
    
   // System.out.println("    Rotation times: " + time0 + " -> " + time1 + ", percentage: " + percentage);
   // System.out.println("    Start rotation: (" + value0.x() + ", " + value0.y() + ", " + value0.z() + ", " + value0.w() + ")");
    //System.out.println("    End rotation: (" + value1.x() + ", " + value1.y() + ", " + value1.z() + ", " + value1.w() + ")");

    Quaternionf start = new Quaternionf(value0.x(), value0.y(), value0.z(), value0.w());
    Quaternionf end = new Quaternionf(value1.x(), value1.y(), value1.z(), value1.w());
    
    Quaternionf result = new Quaternionf();
    start.slerp(end, percentage, result);
    
    //System.out.println("    Result rotation: " + result);
    
    return result;
}
    private int findRotationIndex(float timeAt, AINodeAnim boneAnimation)
    {
        assert boneAnimation.mNumRotationKeys() > 0;

        for (int i = 0; i < boneAnimation.mNumRotationKeys() - 1; i++)
        {
            if (timeAt < boneAnimation.mRotationKeys().get(i + 1).mTime())
                return i;
        }

        return 0;
    }

    private Vector3f calcInterpolatedPosition(float timeAt, AINodeAnim boneAnimation)
    {
        //System.out.println("  calcInterpolatedPosition called for: " + boneAnimation.mNodeName().dataString());
        //System.out.println("    Number of position keys: " + boneAnimation.mNumPositionKeys());
        
        if (boneAnimation == null) {
           // System.out.println("    boneAnimation is null, returning zero");
            return new Vector3f(0, 0, 0);
        }
        
        if (boneAnimation.mNumPositionKeys() == 0) {
            //System.out.println("    No position keys, returning zero");
            return new Vector3f(0, 0, 0);
        }
        
        if (boneAnimation.mNumPositionKeys() == 1) {
            AIVectorKey key = boneAnimation.mPositionKeys().get(0);
            if (key == null || key.mValue() == null) {
                return new Vector3f(0, 0, 0);
            }
            AIVector3D value = key.mValue();
            //System.out.println("    Single position key: (" + value.x() + ", " + value.y() + ", " + value.z() + ")");
            return new Vector3f(value.x(), value.y(), value.z());
        }

        int index0 = findPositionIndex(timeAt, boneAnimation);
        int index1 = index0 + 1;
        
        //System.out.println("    Found position indices: " + index0 + ", " + index1);
        
        if (index0 < 0 || index0 >= boneAnimation.mNumPositionKeys() || 
            index1 < 0 || index1 >= boneAnimation.mNumPositionKeys()) {
            //System.out.println("    Invalid position indices");
            return new Vector3f(0, 0, 0);
        }
        
        AIVectorKey key0 = boneAnimation.mPositionKeys().get(index0);
        AIVectorKey key1 = boneAnimation.mPositionKeys().get(index1);
        
        if (key0 == null || key0.mValue() == null || key1 == null || key1.mValue() == null) {
           // System.out.println("    Position keys or values are null");
            return new Vector3f(0, 0, 0);
        }
        
        AIVector3D value0 = key0.mValue();
        AIVector3D value1 = key1.mValue();
        
        float time0 = (float) key0.mTime();
        float time1 = (float) key1.mTime();
        float deltaTime = time1 - time0;
        float percentage = (timeAt - time0) / deltaTime;
        
       // System.out.println("    Position times: " + time0 + " -> " + time1 + ", percentage: " + percentage);
       /// System.out.println("    Start position: (" + value0.x() + ", " + value0.y() + ", " + value0.z() + ")");
       // System.out.println("    End position: (" + value1.x() + ", " + value1.y() + ", " + value1.z() + ")");

        Vector3f start = new Vector3f(value0.x(), value0.y(), value0.z());
        Vector3f end = new Vector3f(value1.x(), value1.y(), value1.z());
        Vector3f delta = end.sub(start);
        
        Vector3f result = start.add(delta.mul(percentage));
        //System.out.println("    Result position: " + result);
        
        return result;
    }

    private int findPositionIndex(float timeAt, AINodeAnim boneAnimation)
    {
        assert boneAnimation.mNumPositionKeys() > 0;

        for (int i = 0; i < boneAnimation.mNumPositionKeys() - 1; i++)
        {
            if (timeAt < boneAnimation.mPositionKeys().get(i + 1).mTime())
                return i;
        }

        return 0;
    }

    private Bone findBone(String nodeName)
    {
        for (Bone b : bones)
            if (b.getName().equals(nodeName))
                return b;

        return null;
    }

    public Bone[] getBones()
    {
        return bones;
    }

    public void setBones(Bone[] bones) {
        this.bones = bones;
    }

    public void setAnimations(AIAnimation[] animations) {
        this.animations = animations;
    }

    public void setRoot(AINode root) {
        this.root = root;
    }

	public AIAnimation[] getAnimations() {
		return animations;
	}
    
    
    
    
    
    
}