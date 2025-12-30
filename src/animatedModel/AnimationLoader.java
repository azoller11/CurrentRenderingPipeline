package animatedModel;

import org.lwjgl.assimp.*;

import toolbox.Maths;

import java.util.ArrayList;
import java.util.List;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class AnimationLoader
{
    private static List<Integer> vaos = new ArrayList<>();
    private static List<Integer> vbos = new ArrayList<>();
    
    private AnimatedModelData player_constant;
    
    private AIScene player_scene = loadScene("lpm11102024.fbx");
    
    public AnimationLoader() {

    }
    
  
    public AnimatedModel loadObject(AIScene obj_scene) {
        int player_vao;
        int player_indices_length;
        Bone[] player_bones;
        AIAnimation[] player_animations;
        AINode player_root;
        
        AnimatedModel player_model;
        AnimatedModelData  obj_constant = loadConstant( obj_scene);
        
        player_bones = load( obj_scene).getBones();
        player_vao = obj_constant.getVao();
        player_indices_length = obj_constant.getIndicesLength();
        player_animations = obj_constant.getAnimations();
        player_root = obj_constant.getRootNode();
        
        player_model = new AnimatedModel(player_vao, player_indices_length);
        player_model.setBones(player_bones);
        player_model.setAnimations(player_animations);
        player_model.setRoot(player_root);
        return player_model;
    }
    
    public AIScene loadScene(String fileName) {
        AIScene scene = Assimp.aiImportFile("./res/" + fileName,
                Assimp.aiProcess_Triangulate |
                        Assimp.aiProcess_GenSmoothNormals |
                        Assimp.aiProcess_FlipUVs |
                        Assimp.aiProcess_CalcTangentSpace |
                        Assimp.aiProcess_JoinIdenticalVertices
        );
        return scene;   
    }
    
  public AnimatedModelData loadConstant(AIScene scene) {
    assert scene != null;
    assert scene.mNumMeshes() == 1;
    assert scene.mNumAnimations() > 0;
    AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));

    // NEW: Use separate arrays for bone IDs (ints) and weights (floats)
    final int vertexSizeFloats = 14; // 3+2+3+3+3 (position+tex+normal+tangent+bitangent)
    final int floatSize = 4;    // Size of a float in bytes
    final int intSize = 4;      // Size of an int in bytes

    int numVertices = mesh.mNumVertices();
    float[] vertices = new float[numVertices * vertexSizeFloats];
    
    // NEW: Separate arrays for bone data
    int[] boneIdsArray = new int[numVertices * 4]; // 4 bone IDs per vertex
    float[] boneWeightsArray = new float[numVertices * 4]; // 4 weights per vertex
    
    //System.out.println("Number of vertices: " + numVertices);
    //System.out.println("Vertices array length: " + vertices.length);

    int i = 0;
    for (int v = 0; v < numVertices; v++) {
        if (i + 14 > vertices.length) {
            throw new RuntimeException("Attempting to write beyond the vertices array at vertex index: " + v);
        }

        AIVector3D position = mesh.mVertices().get(v);
        AIVector3D tex = mesh.mTextureCoords(0).get(v);
        AIVector3D normal = mesh.mNormals().get(v);
        AIVector3D tangent = mesh.mTangents().get(v);
        
        // Calculate bitangent (normal × tangent)
        org.joml.Vector3f normalVec = new org.joml.Vector3f(normal.x(), normal.y(), normal.z());
        org.joml.Vector3f tangentVec = new org.joml.Vector3f(tangent.x(), tangent.y(), tangent.z());
        org.joml.Vector3f bitangent = normalVec.cross(tangentVec);

        // Populate position
        vertices[i++] = position.x();
        vertices[i++] = position.y();
        vertices[i++] = position.z();

        // Populate texture coordinates
        vertices[i++] = tex.x();
        vertices[i++] = tex.y();

        // Populate normal
        vertices[i++] = normal.x();
        vertices[i++] = normal.y();
        vertices[i++] = normal.z();

        // Populate tangent
        vertices[i++] = tangent.x();
        vertices[i++] = tangent.y();
        vertices[i++] = tangent.z();

        // Populate bitangent
        vertices[i++] = bitangent.x();
        vertices[i++] = bitangent.y();
        vertices[i++] = bitangent.z();
    }

    // Initialize bone weights and IDs to defaults
    for (int v = 0; v < numVertices; v++) {
        int baseIdx = v * 4;
        for (int j = 0; j < 4; j++) {
            boneIdsArray[baseIdx + j] = 0;
            boneWeightsArray[baseIdx + j] = 0.0f;
        }
    }

    // Bone assignments
    for (int b = 0; b < mesh.mNumBones(); b++) {
        AIBone bone = AIBone.create(mesh.mBones().get(b));
        for (int w = 0; w < bone.mNumWeights(); w++) {
            AIVertexWeight vw = bone.mWeights().get(w);
            int vertexId = vw.mVertexId();
            float weightValue = vw.mWeight();

            if (vertexId < 0 || vertexId >= numVertices) {
                throw new RuntimeException("Invalid vertex ID: " + vertexId);
            }

            // Find the first empty weight slot
            int baseIdx = vertexId * 4;
            for (int j = 0; j < 4; j++) {
                if (boneWeightsArray[baseIdx + j] == 0.0f) {
                    boneIdsArray[baseIdx + j] = b;
                    boneWeightsArray[baseIdx + j] = weightValue;
                    break;
                }
            }
        }
    }
    
 // Bone assignments
    for (int b = 0; b < mesh.mNumBones(); b++) {
        AIBone bone = AIBone.create(mesh.mBones().get(b));
        //System.out.println("Bone " + b + ": " + bone.mName().dataString() + " - " + bone.mNumWeights() + " weights");
        
        // Debug: Print weight distribution
        int weightCounts = 0;
        for (int w = 0; w < bone.mNumWeights(); w++) {
            AIVertexWeight vw = bone.mWeights().get(w);
            if (vw.mWeight() > 0.1f) { // Only count significant weights
                weightCounts++;
            }
        }
       // System.out.println("  Significant weights (>0.1): " + weightCounts);
        
        for (int w = 0; w < bone.mNumWeights(); w++) {
            AIVertexWeight vw = bone.mWeights().get(w);
            int vertexId = vw.mVertexId();
            float weightValue = vw.mWeight();

            if (vertexId < 0 || vertexId >= numVertices) {
                throw new RuntimeException("Invalid vertex ID: " + vertexId);
            }

            // Find the first empty weight slot
            int baseIdx = vertexId * 4;
            for (int j = 0; j < 4; j++) {
                if (boneWeightsArray[baseIdx + j] == 0.0f) {
                    boneIdsArray[baseIdx + j] = b;
                    boneWeightsArray[baseIdx + j] = weightValue;
                    break;
                }
            }
        }
    }

    // Debug: Check vertex weight distribution
    int verticesWithWeights = 0;
    int maxWeightsPerVertex = 0;
    for (int v = 0; v < numVertices; v++) {
        int baseIdx = v * 4;
        int weightCount = 0;
        float totalWeight = 0.0f;
        for (int j = 0; j < 4; j++) {
            if (boneWeightsArray[baseIdx + j] > 0.0f) {
                weightCount++;
                totalWeight += boneWeightsArray[baseIdx + j];
            }
        }
        if (weightCount > 0) {
            verticesWithWeights++;
            if (weightCount > maxWeightsPerVertex) maxWeightsPerVertex = weightCount;
            
            // Debug first few vertices with weights
            if (v < 10 && weightCount > 0) {
               /*
            	System.out.println("Vertex " + v + " - Bones: [" + 
                    boneIdsArray[baseIdx] + "," + boneIdsArray[baseIdx+1] + "," + 
                    boneIdsArray[baseIdx+2] + "," + boneIdsArray[baseIdx+3] + 
                    "], Weights: [" + 
                    boneWeightsArray[baseIdx] + "," + boneWeightsArray[baseIdx+1] + "," + 
                    boneWeightsArray[baseIdx+2] + "," + boneWeightsArray[baseIdx+3] + 
                    "], Total: " + totalWeight);
                    */
            }
        }
    }
    //System.out.println("Vertices with weights: " + verticesWithWeights + "/" + numVertices);
    //System.out.println("Max weights per vertex: " + maxWeightsPerVertex);

    AIAnimation[] animations = new AIAnimation[scene.mNumAnimations()];
    for (int a = 0; a < animations.length; a++) {
        animations[a] = AIAnimation.create(scene.mAnimations().get(a));
    }

    int vao = glGenVertexArrays();
    vaos.add(vao);
    glBindVertexArray(vao);

    // VBO for float vertex data (positions, normals, etc.)
    int vbo = glGenBuffers();
    vbos.add(vbo);
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

    // NEW: VBO for bone IDs (integers)
    int boneIdVBO = glGenBuffers();
    vbos.add(boneIdVBO);
    glBindBuffer(GL_ARRAY_BUFFER, boneIdVBO);
    glBufferData(GL_ARRAY_BUFFER, boneIdsArray, GL_STATIC_DRAW);

    // NEW: VBO for bone weights (floats)
    int boneWeightVBO = glGenBuffers();
    vbos.add(boneWeightVBO);
    glBindBuffer(GL_ARRAY_BUFFER, boneWeightVBO);
    glBufferData(GL_ARRAY_BUFFER, boneWeightsArray, GL_STATIC_DRAW);

    // Set up vertex attributes for the first VBO
    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    
    int stride = vertexSizeFloats * floatSize; // 14 * 4 = 56 bytes

    // Define vertex attribute pointers for float data
    glEnableVertexAttribArray(0); // position
    glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
    
    glEnableVertexAttribArray(1); // textureCoordinates
    glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * floatSize);
    
    glEnableVertexAttribArray(2); // normal
    glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * floatSize);
    
    glEnableVertexAttribArray(3); // tangent
    glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * floatSize);
    
    glEnableVertexAttribArray(4); // bitangent
    glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * floatSize);

    // Set up vertex attributes for bone IDs (integers)
    glBindBuffer(GL_ARRAY_BUFFER, boneIdVBO);
    glEnableVertexAttribArray(5); // bone IDs
    glVertexAttribIPointer(5, 4, GL_INT, 0, 0); // Note: glVertexAttribIPointer for integers

    // Set up vertex attributes for bone weights
    glBindBuffer(GL_ARRAY_BUFFER, boneWeightVBO);
    glEnableVertexAttribArray(6); // bone weights
    glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);

    // Element Buffer Object for indices
    int[] indices = new int[mesh.mNumFaces() * 3];
    i = 0;
    for (int f = 0; f < mesh.mNumFaces(); f++) {
        AIFace face = mesh.mFaces().get(f);
        indices[i++] = face.mIndices().get(0);
        indices[i++] = face.mIndices().get(1);
        indices[i++] = face.mIndices().get(2);
    }

    int ibo = glGenBuffers();
    glBindBuffer(GL_ELEMENT_ARRAY_BUFFER, ibo);
    glBufferData(GL_ELEMENT_ARRAY_BUFFER, indices, GL_STATIC_DRAW);

    glBindVertexArray(0);

    return new AnimatedModelData(vao, indices.length, animations, scene.mRootNode());
}
    public Bones load(AIScene scene)
    {

        assert scene != null;
        assert scene.mNumMeshes() == 1;
        assert scene.mNumAnimations() > 0;
        AIMesh mesh = AIMesh.create(scene.mMeshes().get(0));

//        for (int j = 1100 * vertexSize; j < 1101 * vertexSize; j++)
//        {
//            System.out.println(vertices[j]);
//            if ((j + 1) % vertexSize == 0)
//                System.out.println();
//        }

        Bone[] bones = new Bone[mesh.mNumBones()];

        for (int b = 0; b < mesh.mNumBones(); b++)
        {
            AIBone bone = AIBone.create(mesh.mBones().get(b));
            bones[b] = new Bone(bone.mName().dataString(), Maths.convertMatrix(bone.mOffsetMatrix()));
        }


        //AnimatedModel model = new AnimatedModel(vao, indices.length);
        //model_vao = vao;
        //model_indices_length = indices.length;
        
        //model_bones = bones;
        //model_animations = animations;
        //model_root = scene.mRootNode();
        
        //model.setBones(bones);
        //model.setAnimations(animations);
        //model.setRoot(scene.mRootNode());

        //return model;
        
        return new Bones(bones);
    }

    public void terminate()
    {
        for (int vao : vaos)
            glDeleteVertexArrays(vao);
        for (int vbo : vbos)
            glDeleteBuffers(vbo);
    }
    
    
}
