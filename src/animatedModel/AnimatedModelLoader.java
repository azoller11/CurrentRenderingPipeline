package animatedModel;

import java.util.HashMap;
import java.util.Map;

import org.lwjgl.assimp.AIAnimation;
import org.lwjgl.assimp.AINode;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import animation.Animation;
import animation.JointTransform;
import animation.KeyFrame;
import animation.Quaternion;
import colladaLoader.ColladaLoader;
import dataStructures.AnimatedModelData;
import dataStructures.AnimationData;
import dataStructures.JointData;
import dataStructures.JointTransformData;
import dataStructures.KeyFrameData;
import dataStructures.MeshData;
import dataStructures.SkeletonData;
import dataStructures.TangentCalculator;
import entities.Entity;



public class AnimatedModelLoader {

	//positions
	//textureCoords
	//normals
	//indices
	//jointIndices
	//weights
	
	
	private int jointCount = 0;
	
	private Joint rootJoint = new Joint(jointCount, null, null);;
	
	private static Joint createJoints(JointData data) {
		Joint joint = new Joint(data.index, data.nameId, data.bindLocalTransform);
		for (JointData child : data.children) {
			joint.addChild(createJoints(child));
		}
		return joint;
	}
	
	/*
	public Entity loadAnimatedModel(String modelfile, String texturefile, Vector3f position, float rotX, float rotY, float rotZ, float scale, Loader loader) {
		
		int player_vao;
        int player_indices_length;
        Bone[] player_bones;
        AIAnimation[] player_animations;
        AINode player_root;
        
        AnimatedModel player_model;
		
		
		
        player_bones = load(loader, player_scene).getBones();
		
		
		
		
		AnimatedModelData entityData = ColladaLoader.loadColladaModel(modelfile, 3);
		MeshData mesh = entityData.getMeshData();
		mesh.setTangents(TangentCalculator.calculateTangents(mesh.getVertices(), mesh.getTextureCoords(), mesh.getNormals(), mesh.getIndices()));
		
		
		RawModel model = loader.loadToVAO(mesh.getVertices(), mesh.getTextureCoords(), mesh.getNormals(), mesh.getTangents(), mesh.getIndices(), 
				mesh.getJointIds(), mesh.getVertexWeights());
		ModelTexture texture = new ModelTexture(loader.loadTexture("diffuse"));
		SkeletonData skeletonData = entityData.getJointsData();
		Joint headJoint = createJoints(skeletonData.headJoint);
		return new Entity(model, texture, entityData, position, rotX, rotY, rotZ, scale);
	}
	*/
	public Animation loadAnimation(String file) {
		
		
		AnimationData animationData = ColladaLoader.loadColladaAnimation(file);
		KeyFrame[] frames = new KeyFrame[animationData.keyFrames.length];
		for (int i = 0; i < frames.length; i++) {
			frames[i] = createKeyFrame(animationData.keyFrames[i]);
		}
		return new Animation(animationData.lengthSeconds, frames);
	}
	
	private static KeyFrame createKeyFrame(KeyFrameData data) {
		Map<String, JointTransform> map = new HashMap<String, JointTransform>();
		for (JointTransformData jointData : data.jointTransforms) {
			JointTransform jointTransform = createTransform(jointData);
			map.put(jointData.jointNameId, jointTransform);
		}
		return new KeyFrame(data.time, map);
	}
	
	private static JointTransform createTransform(JointTransformData data) {
		Matrix4f mat = data.jointLocalTransform;
		Vector3f translation = new Vector3f(mat.m30, mat.m31, mat.m32);
		Quaternion rotation = Quaternion.fromMatrix(mat);
		return new JointTransform(translation, rotation);
	}
	
}