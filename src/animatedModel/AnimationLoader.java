package animatedModel;

import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.lwjgl.assimp.*;

import toolbox.Maths;
import toolbox.Mesh;
import toolbox.MeshData;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.HashMap;

import static org.lwjgl.opengl.GL11.GL_FLOAT;
import static org.lwjgl.opengl.GL15.*;
import static org.lwjgl.opengl.GL20.glEnableVertexAttribArray;
import static org.lwjgl.opengl.GL20.glVertexAttribPointer;
import static org.lwjgl.opengl.GL30.*;

public class AnimationLoader
{
    private static List<Integer> vaos = new ArrayList<>();
    private static List<Integer> vbos = new ArrayList<>();

    public AnimationLoader() {}
    
    
    public List<AnimatedModel> loadMultiMeshObject(AIScene obj_scene) {
        List<AnimatedModel> models = new ArrayList<>();
        
        // IMPORTANT: Create ONE shared bone array for ALL meshes
        Bone[] sharedBones = loadSharedBones(obj_scene);
        
        System.out.println("Assimp scene animations: " + obj_scene.mNumAnimations());

        
        // Get animations and root (shared across all meshes)
        AIAnimation[] animations = new AIAnimation[obj_scene.mNumAnimations()];
        for (int a = 0; a < animations.length; a++) {
            animations[a] = AIAnimation.create(obj_scene.mAnimations().get(a));
            System.out.println("Animation: " + a + " "  +  animations[a].mName().dataString());
        }
        AINode root = obj_scene.mRootNode();
        
        Matrix4f globalInverse =
        	    new Matrix4f(Maths.convertMatrix(root.mTransformation()))
        	        .invert();
        
        //System.out.println("DEBUG: Creating multi-mesh model with " + sharedBones.length + " shared bones");
        
        // First, collect all mesh transforms from the scene hierarchy
        List<MeshTransformInfo> meshTransforms = new ArrayList<>();
        collectMeshTransforms(obj_scene.mRootNode(), new Matrix4f().identity(), meshTransforms);
        
        Map<String, Matrix4f> bindPoseGlobalByNode = new HashMap<>();
        collectBindPoseGlobals(obj_scene.mRootNode(), new Matrix4f().identity(), bindPoseGlobalByNode);

        
        // Create a separate AnimatedModel for each mesh, but share the SAME bones
        for (int meshIndex = 0; meshIndex < obj_scene.mNumMeshes(); meshIndex++) {
            AIMesh mesh = AIMesh.create(obj_scene.mMeshes().get(meshIndex));
            
            // Find the transform for this mesh
            Matrix4f meshTransform = new Matrix4f().identity();
            String meshName = mesh.mName().dataString();
            
            MeshTransformInfo foundInfo = null;
            
            for (MeshTransformInfo info : meshTransforms) {
                if (info.meshIndex == meshIndex) {
                    meshTransform = info.transform;
                    foundInfo = info;
                    break;
                }
            }
            
            // Load this mesh into VAO with the shared bones AND mesh transform
            MeshData meshData = loadMeshToVAO(mesh, sharedBones, meshTransform);
            
            // Create AnimatedModel for this mesh with SHARED bones
            AnimatedModel model = new AnimatedModel(meshData.vaoID, meshData.vertexCount);
            
          
            
            // CRITICAL: All models share the EXACT SAME bone array
            model.setBones(sharedBones);
            model.setAnimations(animations);
            model.setRoot(root);
            model.setMeshIndex(meshIndex);
            model.setLocalTransform(meshTransform);
            model.setMultiMeshPart(true);
            model.setGlobalInverseTransform(globalInverse);
            model.setBindPoseGlobalByNode(bindPoseGlobalByNode);
            if (foundInfo != null) {
                model.setMeshNodeName(foundInfo.nodeName);
            }
            
            //System.out.println("\n[BONE ↔ NODE MATCH CHECK]");
            for (Bone b : sharedBones) {
                boolean nodeExists = model.findNodeByName(obj_scene.mRootNode(), b.getName()) != null;
               // System.out.println(
               //     "Bone '" + b.getName() + "' → node exists: " + nodeExists
               // );
            }

            
            String attachedBoneName = null;
            for (MeshTransformInfo info : meshTransforms) {
                if (info.meshIndex == meshIndex) {
                    // The node containing this mesh is likely the bone
                    attachedBoneName = info.nodeName;
                    break;
                }
            }
            // Set this in the model
            model.setAttachedBoneName(attachedBoneName);

            // Whether this mesh is skinned or rigid
            model.setSkinned(mesh.mNumBones() > 0);
            models.add(model);
        }
        
        // Force verification that all models share the same bone array
        if (!models.isEmpty() && models.size() > 1) {
            Bone[] firstBones = models.get(0).getBones();
            for (int i = 1; i < models.size(); i++) {
                models.get(i).setBones(firstBones);
            }
            //System.out.println("DEBUG: All " + models.size() + " meshes now share the same bone array");
        }
        
        for (AnimatedModel model : models) {
            model.initializeAllBoneTransformations();
        }
        
        //System.out.println("\n=== NODE HIERARCHY ===");
        //dumpHierarchy(obj_scene.mRootNode(), 0);
        
        return models;
    }
     
  

    
private void collectMeshTransforms(AINode node, Matrix4f parentTransform, List<MeshTransformInfo> meshTransforms) {
         // Get this node's transform
         Matrix4f nodeTransform = Maths.convertMatrix(node.mTransformation());
         Matrix4f globalTransform = new Matrix4f(parentTransform).mul(nodeTransform);
         
         String nodeName = node.mName().dataString();
         
         // Check if this node has meshes attached to it
         int meshCount = node.mNumMeshes();
         for (int i = 0; i < meshCount; i++) {
             int meshIndex = node.mMeshes().get(i);
             meshTransforms.add(
            		    new MeshTransformInfo(meshIndex, nodeName, new Matrix4f(globalTransform))
            		);
             //System.out.println("Node '" + nodeName + "' contains mesh index: " + meshIndex);
         }
         
         // Process children
         for (int i = 0; i < node.mNumChildren(); i++) {
             AINode child = AINode.create(node.mChildren().get(i));
             collectMeshTransforms(child, globalTransform, meshTransforms);
         }
     }
     
     private MeshData loadMeshToVAO(AIMesh mesh, Bone[] sharedBones, Matrix4f meshTransform) {
         final int vertexSizeFloats = 14;
         final int floatSize = 4;
         
         int numVertices = mesh.mNumVertices();
         float[] vertices = new float[numVertices * vertexSizeFloats];
         int[] boneIdsArray = new int[numVertices * 4];
         float[] boneWeightsArray = new float[numVertices * 4];
         
         // Initialize bone data
         for (int v = 0; v < numVertices; v++) {
             int baseIdx = v * 4;
             for (int j = 0; j < 4; j++) {
                 boneIdsArray[baseIdx + j] = 0;
                 boneWeightsArray[baseIdx + j] = 0.0f;
             }
         }
         
         // Process vertices WITH MESH TRANSFORM APPLIED
         int i = 0;
         for (int v = 0; v < numVertices; v++) {
        	    AIVector3D position = mesh.mVertices().get(v);
        	    AIVector3D tex = mesh.mTextureCoords(0).get(v);
        	    AIVector3D normal = mesh.mNormals().get(v);
        	    AIVector3D tangent = mesh.mTangents().get(v);

        	    Vector3f normalVec  = new Vector3f(normal.x(), normal.y(), normal.z());
        	    Vector3f tangentVec = new Vector3f(tangent.x(), tangent.y(), tangent.z());
        	    Vector3f bitangent  = new Vector3f();
        	    normalVec.cross(tangentVec, bitangent);
        	    
        	      // Transform position by mesh's local transform
                Vector3f transformedPos = new Vector3f(position.x(), position.y(), position.z());
                meshTransform.transformPosition(transformedPos);
               
                // Transform normals and tangents (without translation)
                Vector3f transformedNormal =  new Vector3f(normalVec);
                Matrix4f normalMatrix = new Matrix4f(meshTransform);
                normalMatrix.setTranslation(0, 0, 0); // Remove translation for normals
                normalMatrix.transformDirection(transformedNormal);
               
                Vector3f transformedTangent = new Vector3f(tangentVec);
                normalMatrix.transformDirection(transformedTangent);
               
                Vector3f transformedBitangent = new Vector3f(bitangent);
                normalMatrix.transformDirection(transformedBitangent);

        	    // POSITION (RAW, UNTRANSFORMED)
        	   // vertices[i++] = position.x();
        	   // vertices[i++] = position.y();
        	   // vertices[i++] = position.z();
        	    
        	    
        	    vertices[i++] = transformedPos.x();
        	    vertices[i++] = transformedPos.y();
        	    vertices[i++] = transformedPos.z();
				
				
				
        	    // UV
        	    vertices[i++] = tex.x();
        	    vertices[i++] = tex.y();

      
        	    
        	       // Transformed normal
                vertices[i++] = transformedNormal.x();
                vertices[i++] = transformedNormal.y();
                vertices[i++] = transformedNormal.z();
               
                // Transformed tangent
                vertices[i++] = transformedTangent.x();
                vertices[i++] = transformedTangent.y();
                vertices[i++] = transformedTangent.z();
               
                // Transformed bitangent
                vertices[i++] = transformedBitangent.x();
                vertices[i++] = transformedBitangent.y();
                vertices[i++] = transformedBitangent.z();
        	}

         
         // Create bone name to index map for shared bones
         Map<String, Integer> boneNameToIndex = new HashMap<>();
         for (int b = 0; b < sharedBones.length; b++) {
             boneNameToIndex.put(sharedBones[b].getName(), b);
         }
      // Insert bone weights (top 4 only)
         for (int b = 0; b < mesh.mNumBones(); b++) {
             AIBone aiBone = AIBone.create(mesh.mBones().get(b));
             String boneName = aiBone.mName().dataString();

             Integer boneIndex = boneNameToIndex.get(boneName);
             if (boneIndex == null) continue;

             for (int w = 0; w < aiBone.mNumWeights(); w++) {
                 AIVertexWeight vw = aiBone.mWeights().get(w);
                 insertWeight(
                     vw.mVertexId(),
                     boneIndex,
                     vw.mWeight(),
                     boneIdsArray,
                     boneWeightsArray
                 );
             }
         }
      // Normalize bone weights per vertex (REQUIRED)
         for (int v = 0; v < numVertices; v++) {
             int base = v * 4;

             float sum =
                 boneWeightsArray[base] +
                 boneWeightsArray[base + 1] +
                 boneWeightsArray[base + 2] +
                 boneWeightsArray[base + 3];

             if (sum > 0f) {
                 boneWeightsArray[base]     /= sum;
                 boneWeightsArray[base + 1] /= sum;
                 boneWeightsArray[base + 2] /= sum;
                 boneWeightsArray[base + 3] /= sum;
             } else {
                 boneIdsArray[base] = 0;
                 boneWeightsArray[base] = 1.0f;
             }

             for (int r = 0; r < 4; r++) {
                 if (boneIdsArray[base + r] < 0) {
                     boneIdsArray[base + r] = 0;
                 }
             }
         }

        
         
         
      // Process bone weights for this specific mesh (TOP 4, sorted)
         for (int b = 0; b < mesh.mNumBones(); b++) {
             AIBone aiBone = AIBone.create(mesh.mBones().get(b));
             String boneName = aiBone.mName().dataString();

             Integer boneIndex = boneNameToIndex.get(boneName);
             if (boneIndex == null) continue;

             for (int w = 0; w < aiBone.mNumWeights(); w++) {
                 AIVertexWeight vw = aiBone.mWeights().get(w);
                 insertWeight(
                     vw.mVertexId(),
                     boneIndex,
                     vw.mWeight(),
                     boneIdsArray,
                     boneWeightsArray
                 );
             }
         }

         
      // DEBUG: per-vertex weight sanity
         for (int v = 0; v < numVertices; v++) {
             int base = v * 4;
             float total = 0;
             int nonZero = 0;

             for (int j = 0; j < 4; j++) {
                 if (boneWeightsArray[base + j] > 0.0001f) {
                     total += boneWeightsArray[base + j];
                     nonZero++;
                 }
             }

          
         }

         
         // Create VAO
         int vao = glGenVertexArrays();
         vaos.add(vao);
         glBindVertexArray(vao);
         
         int vbo = glGenBuffers();
         vbos.add(vbo);
         glBindBuffer(GL_ARRAY_BUFFER, vbo);
         glBufferData(GL_ARRAY_BUFFER, vertices, GL_STATIC_DRAW);
         
         int stride = vertexSizeFloats * floatSize;
         
         glEnableVertexAttribArray(0);
         glVertexAttribPointer(0, 3, GL_FLOAT, false, stride, 0);
         
         glEnableVertexAttribArray(1);
         glVertexAttribPointer(1, 2, GL_FLOAT, false, stride, 3 * floatSize);
         
         glEnableVertexAttribArray(2);
         glVertexAttribPointer(2, 3, GL_FLOAT, false, stride, 5 * floatSize);
         
         glEnableVertexAttribArray(3);
         glVertexAttribPointer(3, 3, GL_FLOAT, false, stride, 8 * floatSize);
         
         glEnableVertexAttribArray(4);
         glVertexAttribPointer(4, 3, GL_FLOAT, false, stride, 11 * floatSize);
         
         // Bone IDs
         int boneIdVBO = glGenBuffers();
         vbos.add(boneIdVBO);
         glBindBuffer(GL_ARRAY_BUFFER, boneIdVBO);
         glBufferData(GL_ARRAY_BUFFER, boneIdsArray, GL_STATIC_DRAW);
         glEnableVertexAttribArray(5);
         glVertexAttribIPointer(5, 4, GL_INT, 0, 0);
         
         // Bone weights
         int boneWeightVBO = glGenBuffers();
         vbos.add(boneWeightVBO);
         glBindBuffer(GL_ARRAY_BUFFER, boneWeightVBO);
         glBufferData(GL_ARRAY_BUFFER, boneWeightsArray, GL_STATIC_DRAW);
         glEnableVertexAttribArray(6);
         glVertexAttribPointer(6, 4, GL_FLOAT, false, 0, 0);
         
         // Indices
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
         
         // After processing bone weights, add debug output:
         //System.out.println("DEBUG: Mesh '" + mesh.mName().dataString() + "' bone usage:");
         
         // Count which bones are used by this mesh
         Map<Integer, Integer> boneUsage = new HashMap<>();
         for (int v = 0; v < numVertices; v++) {
             int baseIdx = v * 4;
             for (int j = 0; j < 4; j++) {
                 int boneId = boneIdsArray[baseIdx + j];
                 float weight = boneWeightsArray[baseIdx + j];
                 if (weight > 0.001f) {
                     boneUsage.put(boneId, boneUsage.getOrDefault(boneId, 0) + 1);
                 }
             }
         }
    
         
         return new MeshData(vao, indices.length);
     }
     
    
     private Bone[] loadSharedBones(AIScene scene) {
         // First pass: collect ALL unique bones from ALL meshes
         Map<String, Integer> boneNameToIndex = new HashMap<>();
         List<Bone> boneList = new ArrayList<>();
         
         for (int meshIndex = 0; meshIndex < scene.mNumMeshes(); meshIndex++) {
             AIMesh mesh = AIMesh.create(scene.mMeshes().get(meshIndex));
             
             for (int b = 0; b < mesh.mNumBones(); b++) {
                 AIBone bone = AIBone.create(mesh.mBones().get(b));
                 String boneName = bone.mName().dataString();
                 
                 if (!boneNameToIndex.containsKey(boneName)) {
                     // Create new bone
                     Bone newBone = new Bone(boneName, Maths.convertMatrix(bone.mOffsetMatrix()));
                     boneList.add(newBone);
                     boneNameToIndex.put(boneName, boneList.size() - 1);
                 }
               
             }
         }
         
         // Convert to array
         Bone[] bones = new Bone[boneList.size()];
         bones = boneList.toArray(bones);
       
         
         return bones;
     }
    

     public AnimatedModel loadObject(AIScene obj_scene) {
    	    int player_vao;
    	    int player_indices_length;
    	    Bone[] player_bones;
    	    AIAnimation[] player_animations;
    	    AINode player_root;

    	    AnimatedModelData obj_constant = loadConstant(obj_scene);

    	    player_bones = load(obj_scene).getBones();
    	    player_vao = obj_constant.getVao();
    	    player_indices_length = obj_constant.getIndicesLength();
    	    player_animations = obj_constant.getAnimations();
    	    player_root = obj_constant.getRootNode();

    	    // ✅ COMPUTE GLOBAL INVERSE
    	    Matrix4f globalInverse =
    	        new Matrix4f(Maths.convertMatrix(player_root.mTransformation()))
    	            .invert();

    	    AnimatedModel player_model =
    	        new AnimatedModel(player_vao, player_indices_length);

    	    player_model.setBones(player_bones);
    	    player_model.setAnimations(player_animations);
    	    player_model.setRoot(player_root);

    	    // 🔥 THIS WAS MISSING
    	    player_model.setGlobalInverseTransform(globalInverse);

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
    
    private void collectBindPoseGlobals(AINode node,
            Matrix4f parentGlobal,
            Map<String, Matrix4f> outBindPoseGlobalByNode)
				{
				String nodeName = node.mName().dataString();
				Matrix4f local = Maths.convertMatrix(node.mTransformation());
				
				Matrix4f global = new Matrix4f(parentGlobal).mul(local);
				outBindPoseGlobalByNode.put(nodeName, new Matrix4f(global));
				
				for (int i = 0; i < node.mNumChildren(); i++) {
				AINode child = AINode.create(node.mChildren().get(i));
				collectBindPoseGlobals(child, global, outBindPoseGlobalByNode);
				}
				//System.out.println(
			//		    "[BIND POSE] node='" + nodeName + "'\n" + global
			//		);
	}

    private void dumpHierarchy(AINode node, int depth) {
        System.out.println("  ".repeat(depth) + node.mName().dataString());
        for (int i = 0; i < node.mNumChildren(); i++) {
            dumpHierarchy(AINode.create(node.mChildren().get(i)), depth + 1);
        }
    }
    
    private static void insertWeight(int vertexId, int boneId, float weight,
            int[] boneIds, float[] boneWeights) {
		// Arrays are 4-per-vertex packed: idx = vertexId*4 + slot
		int base = vertexId * 4;
		
		// Ignore zero weights
		if (weight <= 0f) return;
		
		// Find slot to insert if it's heavier than an existing one
		for (int i = 0; i < 4; i++) {
		int idx = base + i;
		
		if (weight > boneWeights[idx]) {
		// shift down
		for (int j = 3; j > i; j--) {
		boneWeights[base + j] = boneWeights[base + j - 1];
		boneIds[base + j]     = boneIds[base + j - 1];
		}
		boneWeights[idx] = weight;
		boneIds[idx]     = boneId;
		return;
		}
		}
		// If we get here, it's not in the top 4 → discard
	}

    
    private static class MeshData {
        int vaoID;
        int vertexCount;
        
        MeshData(int vaoID, int vertexCount) {
            this.vaoID = vaoID;
            this.vertexCount = vertexCount;
        }
    }
    private static class MeshTransformInfo {
        int meshIndex;
        String nodeName;
        Matrix4f transform;

        MeshTransformInfo(int meshIndex, String nodeName, Matrix4f transform) {
            this.meshIndex = meshIndex;
            this.nodeName = nodeName;
            this.transform = transform;
        }
    }
    
    public void terminate()
    {
        for (int vao : vaos)
            glDeleteVertexArrays(vao);
        for (int vbo : vbos)
            glDeleteBuffers(vbo);
    }

}