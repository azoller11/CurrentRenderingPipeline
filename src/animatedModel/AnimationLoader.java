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

        player_constant = loadConstant(player_scene);
    }
    
    public AnimatedModel loadPlayer() {
        int player_vao;
        int player_indices_length;
        Bone[] player_bones;
        AIAnimation[] player_animations;
        AINode player_root;
        
        AnimatedModel player_model;
        
        player_bones = load(player_scene).getBones();
        player_vao = player_constant.getVao();
        player_indices_length = player_constant.getIndicesLength();
        player_animations = player_constant.getAnimations();
        player_root = player_constant.getRootNode();
        
        player_model = new AnimatedModel(player_vao, player_indices_length);
        player_model.setBones(player_bones);
        player_model.setAnimations(player_animations);
        player_model.setRoot(player_root);
        return player_model;
    }
    
    public AnimatedModel loadObject(AIScene obj_scene) {
        int player_vao;
        int player_indices_length;
        Bone[] player_bones;
        AIAnimation[] player_animations;
        AINode player_root;
        
        AnimatedModel player_model;
        AnimatedModelData  obj_constant = loadConstant(obj_scene);
        
        player_bones = load(obj_scene).getBones();
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

    final int vertexSize = 19; // 19 floats per vertex (3+2+3+3+4+4)
    final int floatSize = 4;    // Size of a float in bytes

    int numVertices = mesh.mNumVertices();
    float[] vertices = new float[numVertices * vertexSize];
    System.out.println("Number of vertices: " + numVertices);
    System.out.println("Vertices array length: " + vertices.length);

    int i = 0;
    for (int v = 0; v < numVertices; v++) {
        if (i + 19 > vertices.length) {
            throw new RuntimeException("Attempting to write beyond the vertices array at vertex index: " + v);
        }

        AIVector3D position = mesh.mVertices().get(v);
        AIVector3D tex = mesh.mTextureCoords(0).get(v);
        AIVector3D normal = mesh.mNormals().get(v);
        AIVector3D tangent = mesh.mTangents().get(v); // Fetch tangent

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

        // Initialize weights to 0
        for (int j = 0; j < 4; j++) { // Weights
            vertices[i++] = 0.0f;
        }

        // Initialize bone IDs to 0
        for (int j = 0; j < 4; j++) { // Bone IDs
            vertices[i++] = 0.0f;
        }
    }

    // Ensure 'i' does not exceed 'vertices.length'
    if (i != vertices.length) {
        throw new RuntimeException("Mismatch in vertices array population. Expected " + vertices.length + " but got " + i);
    }

    int[] indices = new int[mesh.mNumFaces() * 3];
    i = 0;
    for (int f = 0; f < mesh.mNumFaces(); f++) {
        AIFace face = mesh.mFaces().get(f);
        indices[i++] = face.mIndices().get(0);
        indices[i++] = face.mIndices().get(1);
        indices[i++] = face.mIndices().get(2);
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
            for (int j = 0; j < 4; j++) { // Support up to 4 bones per vertex
                int weightIndex = vertexId * vertexSize + 11 + j;  // Weights start at float 11
                int boneIndex = vertexId * vertexSize + 15 + j;    // Bone IDs start at float 15

                if (weightIndex >= vertices.length || boneIndex >= vertices.length) {
                    throw new RuntimeException("Weight or Bone Index out of bounds for vertex ID: " + vertexId);
                }

                if (vertices[weightIndex] == 0.0f) {
                    vertices[weightIndex] = weightValue;
                    vertices[boneIndex] = (float) b;
                    break;
                }
            }
        }
    }

    AIAnimation[] animations = new AIAnimation[scene.mNumAnimations()];
    for (int a = 0; a < animations.length; a++) {
        animations[a] = AIAnimation.create(scene.mAnimations().get(a));
    }

    int vao = glGenVertexArrays();
    vaos.add(vao);
    glBindVertexArray(vao);

    int vbo = glGenBuffers();
    vbos.add(vbo);

    glBindBuffer(GL_ARRAY_BUFFER, vbo);
    glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);

    // Enable vertex attributes
    glEnableVertexAttribArray(0); // position
    glEnableVertexAttribArray(1); // textureCoordinates
    glEnableVertexAttribArray(2); // normal
    glEnableVertexAttribArray(3); // tangent
    glEnableVertexAttribArray(4); // weight
    glEnableVertexAttribArray(5); // bone_id

    int stride = vertexSize * floatSize; // 19 * 4 = 76 bytes

    // Define vertex attribute pointers
    glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);                     // position
    glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * floatSize);        // textureCoordinates
    glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * floatSize);        // normal
    glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * floatSize);        // tangent
    glVertexAttribPointer(4, 4, GL_FLOAT, false, stride, 11 * floatSize);       // weight
    glVertexAttribPointer(5, 4, GL_FLOAT, false, stride, 15 * floatSize);       // bone_id

    // Element Buffer Object
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
