package animatedModel;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AIFace;
import org.lwjgl.assimp.AIMesh;
import org.lwjgl.assimp.AINode;
import org.lwjgl.assimp.AINodeAnim;
import org.lwjgl.assimp.AIQuatKey;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIVector3D;
import org.lwjgl.assimp.AIVectorKey;

import toolbox.Maths;
import toolbox.Mesh;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.GL_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_ELEMENT_ARRAY_BUFFER;
import static org.lwjgl.opengl.GL15.GL_STATIC_DRAW;
import static org.lwjgl.opengl.GL15.glBindBuffer;
import static org.lwjgl.opengl.GL15.glBufferData;
import static org.lwjgl.opengl.GL15.glGenBuffers;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.glBindVertexArray;
import static org.lwjgl.opengl.GL30.glGenVertexArrays;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AnimatedModel
{
    private final int vaoID;
    private int count;
    
    private org.joml.Matrix4f localTransform;

    Bone[] bones;
    AIAnimation[] animations;
    AINode root;
    
    
    private String meshNodeName;
    
    private String attachedBoneName;
    
    private boolean isMultiMeshPart = false;
    private int meshIndex = -1;
    
    private boolean skinned;
    
    private Matrix4f globalInverseTransform;
    
    private Matrix4f bindPoseNodeGlobal = new Matrix4f().identity();
    private Matrix4f bindPoseNodeGlobalInverse = new Matrix4f().identity();
    
    
    private final Map<String, Matrix4f> animatedNodeTransforms = new HashMap<>();
    
    private Map<String, Matrix4f> bindPoseGlobalByNode = new HashMap<>();

    
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
    
    public boolean isMultiMeshPart() {
        return isMultiMeshPart;
    }
    
    public void setMultiMeshPart(boolean isMultiMeshPart) {
        this.isMultiMeshPart = isMultiMeshPart;
    }
    
    public int getMeshIndex() {
        return meshIndex;
    }
    
    public void setMeshIndex(int meshIndex) {
        this.meshIndex = meshIndex;
    }
    
    public Matrix4f getAnimatedNodeTransform(String nodeName) {
        return animatedNodeTransforms.get(nodeName);
    }
    
    public void setMeshNodeName(String name) {
        this.meshNodeName = name;
    }

    public String getMeshNodeName() {
        return meshNodeName;
    }
    
    public void setSkinned(boolean skinned) {
        this.skinned = skinned;
    }

    public boolean isSkinned() {
        return skinned;
    }
    
    public void setGlobalInverseTransform(Matrix4f m) {
        this.globalInverseTransform = m;
    }
    
    public Matrix4f getGlobalInverseTransform() {
        return globalInverseTransform;
    }
    
    public void setBindPoseNodeGlobal(Matrix4f m) {
        this.bindPoseNodeGlobal.set(m);
        this.bindPoseNodeGlobalInverse.set(m).invert();
    }

    public Matrix4f getBindPoseNodeGlobal() {
        return new Matrix4f(bindPoseNodeGlobal);
    }

    public Matrix4f getBindPoseNodeGlobalInverse() {
        return new Matrix4f(bindPoseNodeGlobalInverse);
    }
    
    public void setBindPoseGlobalByNode(Map<String, Matrix4f> map) {
        this.bindPoseGlobalByNode = map;
    }

    private Matrix4f getBindPoseGlobalForNode(String nodeName) {
        Matrix4f m = bindPoseGlobalByNode.get(nodeName);
        return (m != null) ? m : new Matrix4f().identity();
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
        animatedNodeTransforms.clear();
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
        animatedNodeTransforms.clear();
        
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

 // 1️⃣ Global node transform (bind + animation)
    Matrix4f nodeGlobal =
            Maths.mul(parentTransform, nodeTransform);

    // 2️⃣ Store GLOBAL NODE transform (used by rigid meshes)
    //animatedNodeTransforms.put(nodeName, new Matrix4f(nodeGlobal));
    
    Matrix4f bindGlobal = getBindPoseGlobalForNode(nodeName);
    Matrix4f bindInv = new Matrix4f(bindGlobal).invert();

    // ✅ delta = currentGlobal * inverse(bindPoseGlobalForThatSameNode)
    Matrix4f delta = new Matrix4f(nodeGlobal).mul(bindInv);

    animatedNodeTransforms.put(nodeName, delta);

    // 3️⃣ If this node is a bone, compute bone matrix
    Bone bone = findBone(nodeName);
    if (bone != null) {
        Matrix4f boneFinal =
                new Matrix4f(globalInverseTransform)
                        .mul(nodeGlobal)
                        .mul(bone.getOffsetMatrix());

        bone.setTransformation(boneFinal);
    }


    // Recursively process the child nodes
    //System.out.println("  Processing " + node.mNumChildren() + " children");
    for (int i = 0; i < node.mNumChildren(); i++)
    {
        AINode childNode = AINode.create(node.mChildren().get(i));
        processNode(target, target2, animationTime, animationTime2, childNode, nodeGlobal, blend);
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
        //System.out.println("DEBUG: Animating node: " + nodeName); // Add this
        
        Vector3f interpolatedScale = calcInterpolatedScale(animationTime, boneAnimation);
        Matrix4f scaleMatrix = new Matrix4f().scale(interpolatedScale);

        Quaternionf interpolatedRotation = calcInterpolatedRotation(animationTime, boneAnimation);
        Matrix4f rotationMatrix = new Matrix4f().rotate(interpolatedRotation);

        Vector3f interpolatedPosition = calcInterpolatedPosition(animationTime, boneAnimation);
        Matrix4f translationMatrix = new Matrix4f().translate(interpolatedPosition);

        nodeTransform = Maths.mul(translationMatrix, rotationMatrix, scaleMatrix);
    }

 // 1️⃣ Global node transform (bind + animation)
    Matrix4f nodeGlobal =
            Maths.mul(parentTransform, nodeTransform);

    // 2️⃣ Store GLOBAL NODE transform (used by rigid meshes)
    //animatedNodeTransforms.put(nodeName, new Matrix4f(nodeGlobal));
    Matrix4f bindGlobal = getBindPoseGlobalForNode(nodeName);
    Matrix4f bindInv = new Matrix4f(bindGlobal).invert();

    // ✅ delta = currentGlobal * inverse(bindPoseGlobalForThatSameNode)
    Matrix4f delta = new Matrix4f(nodeGlobal).mul(bindInv);

    animatedNodeTransforms.put(nodeName, delta);

    // 3️⃣ If this node is a bone, compute bone matrix
    Bone bone = findBone(nodeName);
    if (bone != null) {
        Matrix4f boneFinal =
                new Matrix4f(globalInverseTransform)
                        .mul(nodeGlobal)
                        .mul(bone.getOffsetMatrix());

        bone.setTransformation(boneFinal);
    }


    // Recursively process the child nodes
    //System.out.println("DEBUG: Processing " + node.mNumChildren() + " children of " + nodeName); // Add this
    for (int i = 0; i < node.mNumChildren(); i++)
    {
        AINode childNode = AINode.create(node.mChildren().get(i));
        processNode(target, animationTime, childNode, nodeGlobal);
    }
}
 
    
 // In your AnimatedModel class, add a method to handle node animations:
    public void updateNodeAnimation(int animationIndex, float time, String nodeName) {
        assert animationIndex >= 0 && animationIndex < animations.length;
        
        // Find the specific node in the hierarchy
        AINode targetNode = findNodeByName(root, nodeName);
        if (targetNode != null) {
            updateNodeTransformation(animations[animationIndex], time, targetNode);
        }
    }

    AINode findNodeByName(AINode currentNode, String name) {
        if (currentNode.mName().dataString().equals(name)) {
            return currentNode;
        }
        
        for (int i = 0; i < currentNode.mNumChildren(); i++) {
            AINode child = AINode.create(currentNode.mChildren().get(i));
            AINode result = findNodeByName(child, name);
            if (result != null) {
                return result;
            }
        }
        
        return null;
    }

    private void updateNodeTransformation(AIAnimation animation, float time, AINode node) {
        // This should compute the animated transformation for this specific node
        // Similar to your bone animation code but applied to the node's local transform
        String nodeName = node.mName().dataString();
        AINodeAnim nodeAnimation = findBoneAnimation(animation, nodeName);
        
        if (nodeAnimation != null) {
            // Compute animated transformation
            Vector3f scale = calcInterpolatedScale(time, nodeAnimation);
            Quaternionf rotation = calcInterpolatedRotation(time, nodeAnimation);
            Vector3f position = calcInterpolatedPosition(time, nodeAnimation);
            
            // Create transformation matrix
            Matrix4f translationMatrix = new Matrix4f().translate(position);
            Matrix4f rotationMatrix = new Matrix4f().rotate(rotation);
            Matrix4f scaleMatrix = new Matrix4f().scale(scale);
            
            Matrix4f animatedTransform = Maths.mul(translationMatrix, rotationMatrix, scaleMatrix);
            
            // Store this as the node's local transform
            this.localTransform = animatedTransform;
        }
    }
    
 // Add this to your AnimatedModel class:
    public void initializeAllBoneTransformations() {
        if (bones == null || root == null) return;
        
        System.out.println("Initializing transformations for all " + bones.length + " bones");
        
        // Reset all bones to identity first
        for (Bone bone : bones) {
            bone.setTransformation(new Matrix4f().identity());
        }
        
        // Then compute transformations from the root
        computeTransformationsRecursive(root, new Matrix4f().identity());
        
        
    }

    private void computeTransformationsRecursive(AINode node, Matrix4f parentTransform) {
        String nodeName = node.mName().dataString();
        Matrix4f nodeTransform = Maths.convertMatrix(node.mTransformation());
        Matrix4f toGlobalSpace = Maths.mul(parentTransform, nodeTransform);

        Bone bone = findBone(nodeName);
        if (bone != null) {
        	bone.setTransformation(
        		    new Matrix4f(globalInverseTransform)
        		        .mul(toGlobalSpace)
        		        .mul(bone.getOffsetMatrix())
        		);
        }

        for (int i = 0; i < node.mNumChildren(); i++) {
            AINode childNode = AINode.create(node.mChildren().get(i));
            computeTransformationsRecursive(childNode, toGlobalSpace);
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
    
    
    public org.joml.Matrix4f getLocalTransform() {
        return localTransform;
    }
    
    public void setLocalTransform(org.joml.Matrix4f localTransform) {
        this.localTransform = localTransform;
    }

    public String getAttachedBoneName() {
        return attachedBoneName;
    }
    
    public void setAttachedBoneName(String attachedBoneName) {
        this.attachedBoneName = attachedBoneName;
    }
    
    public String getAnimationName(int index) {
        if (animations == null || index < 0 || index >= animations.length)
            return "";

        return animations[index].mName().dataString();
    }
    
    public float getAnimationLengthSeconds(int index) {
        if (animations == null || index < 0 || index >= animations.length)
            return 0f;

        AIAnimation anim = animations[index];

        float ticksPerSecond =
                anim.mTicksPerSecond() != 0
                        ? (float) anim.mTicksPerSecond()
                        : 60.0f;

        return (float) anim.mDuration() / ticksPerSecond;
    }
    
    public Mesh createMesh() {
  	    return new Mesh(this.vaoID, this.count);
  }



    
    
}