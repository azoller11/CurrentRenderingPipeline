package fbxLoader;

import dataStructures.AnimatedModelData;
import dataStructures.AnimationData;
import dataStructures.MeshData;
import dataStructures.SkeletonData;
import dataStructures.SkinningData;
import colladaLoader.SkinLoader;
import colladaLoader.SkeletonLoader;
import colladaLoader.GeometryLoader;

// Hypothetical FBXParser package


public class FBXLoader {

    /**
     * Loads an FBX model and extracts animated model data.
     * 
     * @param fbxFile The path to the FBX file.
     * @param maxWeights The maximum number of weights per vertex.
     * @return An instance of AnimatedModelData containing mesh and skeleton information.
     */
    public static AnimatedModelData loadFBXModel(String fbxFile, int maxWeights) {
        FBXNode root = FBXParser.loadFBXFile(fbxFile);

        /*
        SkinLoader skinLoader = new SkinLoader(root.getChild("Controllers"), maxWeights);
        SkinningData skinningData = skinLoader.extractSkinData();

        SkeletonLoader jointsLoader = new SkeletonLoader(root.getChild("Skeleton"), skinningData.jointOrder);
        SkeletonData jointsData = jointsLoader.extractBoneData();

        GeometryLoader geometryLoader = new GeometryLoader(root.getChild("Geometry"), skinningData.verticesSkinData);
        MeshData meshData = geometryLoader.extractModelData();

         	*/
        //return new AnimatedModelData(meshData, jointsData);
        return null;
    }

    /**
     * Loads FBX animation data.
     * 
     * @param fbxFile The path to the FBX file.
     * @return An instance of AnimationData containing animation keyframes and related information.
     */
    public static AnimationData loadFBXAnimation(String fbxFile) {
        FBXNode root = FBXParser.loadFBXFile(fbxFile);
        FBXNode animNode = root.getChild("Animations");
        FBXNode skeletonNode = root.getChild("Skeleton");

       // AnimationLoader animationLoader = new AnimationLoader(animNode, skeletonNode);
        //AnimationData animationData = animationLoader.extractAnimation();

       // return animationData;
        return null;
    }
}
