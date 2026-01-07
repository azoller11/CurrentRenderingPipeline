package physics;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionWorld;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CapsuleShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.collision.shapes.SphereShape;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import com.bulletphysics.collision.shapes.ConvexHullShape;

import entities.Camera;
import entities.Entity;
import settings.EngineSettings;
import terrain.Terrain;
import toolbox.Mesh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Random;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.vecmath.Vector3f;

import org.joml.Vector3fc;

import javax.vecmath.Matrix3f;
import javax.vecmath.Quat4f;
import org.joml.Quaternionf;

public class PhysicsManager {

    public DiscreteDynamicsWorld dynamicsWorld;
    
    //private Map<Entity, RigidBody> entityRigidBodyMap = new HashMap<>();
    
    private static final ScalarType PHY_FLOAT = ScalarType.FLOAT;
    
    public static final int CF_STATIC_OBJECT = 1;
    public static final int CF_KINEMATIC_OBJECT = 2;
    public static final int CF_NO_CONTACT_RESPONSE = 4;
    
    public static Random random = new Random();


    public PhysicsManager() {
        // Set up the physics world with basic configuration
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        BroadphaseInterface broadphase = new DbvtBroadphase();
        ConstraintSolver solver = new SequentialImpulseConstraintSolver();
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0f, -150, 0f));
        
        dynamicsWorld.getSolverInfo().numIterations = 6; // More iterations for better stability
        dynamicsWorld.getDispatchInfo().allowedCcdPenetration = 0.02f;
    }
    
    /**
     * Adds a dynamic player RigidBody with capsule shape and CCD enabled.
     * This is designed to prevent "walking through walls" at high speeds.
     *
     * @param entity The player entity
     * @param mass   The mass of the player (e.g., 70 kg)
     * @param radius The radius of the capsule (typically ~0.5 units)
     * @param height The height of the capsule (typically player height minus radius*2)
     * @return The created RigidBody
     */
    public RigidBody addPlayer(Entity entity, float mass) {

        // 10× larger WORLD UNITS
        float playerHeight = 60.0f;     // total height
        float playerRadius = 7.5f;      // half width
        float cylinderHeight = playerHeight - (2.0f * playerRadius);

        CollisionShape shape = new CapsuleShape(playerRadius, cylinderHeight);

        // Margin should scale WITH size
        shape.setMargin(0.2f); // bigger object → bigger margin

        Vector3f inertia = new Vector3f();
        shape.calculateLocalInertia(mass, inertia);

        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(
            new Vector3f(
                entity.getPosition().x,
                entity.getPosition().y,
                entity.getPosition().z
            )
        );

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo info =
            new RigidBodyConstructionInfo(mass, motionState, shape, inertia);

        RigidBody body = new RigidBody(info);

        // Character stability
        body.setFriction(0.0f);
        body.setRestitution(0.0f);
        body.setDamping(0.00f, 0.0f);
        body.setAngularFactor(0f);
        body.setActivationState(RigidBody.DISABLE_DEACTIVATION);

        // CCD must scale too
        body.setCcdMotionThreshold(5.0f);
        body.setCcdSweptSphereRadius(playerRadius * 0.9f);

        dynamicsWorld.addRigidBody(body);
        entity.setCollisionBody(body);
        return body;
    }



    /**
     * Calculate approximate bounding sphere radius for CCD
     */
    private float calculateBoundingSphereRadius(Entity entity) {
        Mesh mesh = entity.getTexturedModel().getMesh();
        float[] vertices = mesh.getVertices();
        
        // Simple bounding sphere calculation
        float maxDistance = 0;
        for (int i = 0; i < vertices.length; i += 3) {
            float distance = (float)Math.sqrt(
                vertices[i] * vertices[i] + 
                vertices[i+1] * vertices[i+1] + 
                vertices[i+2] * vertices[i+2]
            );
            maxDistance = Math.max(maxDistance, distance);
        }
        
        return maxDistance * entity.getScale();
    }

    /**
     * Creates an accurate collision shape from an entity's mesh.
     * This method uses a triangle mesh shape (BvhTriangleMeshShape) for high-precision collision detection.
     *
     * @param entity the game entity whose mesh is used to build the collision shape
     * @return a CollisionShape for the given entity
     */
    public CollisionShape createAccurateCollisionEntity(Entity entity) {
        Mesh mesh = entity.getTexturedModel().getMesh();
        return createAccurateCollisionMesh(mesh, entity.getScale());
    }
    
    public CollisionShape createAccurateCollisionMesh(Mesh mesh, float scale) {
        // Get the vertex and index ByteBuffers from the Mesh.
        ByteBuffer vertexBuffer = vertexBuffer = ByteBuffer.allocateDirect(mesh.getVertices().length * 4).order(ByteOrder.nativeOrder());
        for (float v : mesh.getVertices()) {
            vertexBuffer.putFloat(v);
        }
        vertexBuffer.flip(); // vertices stored as 3 floats per vertex
        ByteBuffer indexBuffer = indexBuffer = ByteBuffer.allocateDirect(mesh.getIndices().length * 4).order(ByteOrder.nativeOrder());
        for (int index : mesh.getIndices()) {
            indexBuffer.putInt(index);
        }
        indexBuffer.flip();  // indices stored as ints (3 per triangle)

        IndexedMesh indexedMesh = new IndexedMesh();
        // Calculate the number of triangles: each triangle uses 3 indices, 4 bytes per int.
        indexedMesh.numTriangles = indexBuffer.capacity() / (3 * 4);
        indexedMesh.triangleIndexBase = indexBuffer;
        indexedMesh.triangleIndexStride = 3 * 4;  // 3 ints per triangle, 4 bytes each

        // Calculate the number of vertices: each vertex is 3 floats (4 bytes each).
        indexedMesh.numVertices = vertexBuffer.capacity() / (3 * 4);
        indexedMesh.vertexBase = vertexBuffer;
        indexedMesh.vertexStride = 3 * 4;         // 3 floats per vertex, 4 bytes each

        // Create the TriangleIndexVertexArray and add the IndexedMesh.
        TriangleIndexVertexArray indexVertexArray = new TriangleIndexVertexArray();
        indexVertexArray.addIndexedMesh(indexedMesh, ScalarType.INTEGER);

        // Create a high-precision collision shape from the triangle mesh.
        CollisionShape shape = new BvhTriangleMeshShape(indexVertexArray, true);
    	shape.setLocalScaling(new Vector3f(scale,scale,scale));

        return shape;
    }


    /**
     * Creates a simplified collision shape using a bounding box approximation.
     * This “not so accurate” method is faster and good for less critical collisions.
     *
     * @param entity the game entity used to compute the bounding box
     * @return a simple box CollisionShape
     */
    public CollisionShape createNotSoAccurateCollisionMesh(Entity entity) {
        // In a real implementation, compute the bounding box from the mesh.
        // Here we use a fixed size as a placeholder.
        Vector3f halfExtents = new Vector3f(1f, 1f, 1f);
        CollisionShape shape = new BoxShape(halfExtents);
    	shape.setLocalScaling(new Vector3f(entity.getScale(),entity.getScale(),entity.getScale()));

        return shape;
    }

    /**
     * Adds a movable collision mesh (i.e. a dynamic rigid body) to the physics world.
     *
     * @param entity the game entity for which the collision mesh is created
     * @param shape  the collision shape to use (could be accurate or simplified)
     * @param mass   the mass of the object (non-zero for dynamic bodies)
     * @return the created RigidBody
     */
    public RigidBody addMovableCollisionMesh(Entity entity, CollisionShape shape, float mass) {
        Vector3f inertia = new Vector3f(0, 0, 0);
        shape.calculateLocalInertia(mass, inertia);

        Transform transform = new Transform();
        transform.setIdentity();
        // Set the initial position from the entity.
        Vector3f pos = new Vector3f(entity.getPosition().x, entity.getPosition().y, entity.getPosition().z);
        transform.origin.set(pos);
        
 
        // Rotation
        float rotX = entity.getRotation().x();
        float rotY = entity.getRotation().y();
        float rotZ = entity.getRotation().z();

        // Convert to QUATERNION for Bullet
        javax.vecmath.Quat4f quat = new javax.vecmath.Quat4f();
        Matrix3f rotationMatrix = new Matrix3f();

        // Build JOML matrix
        org.joml.Matrix3f jomlMat = new org.joml.Matrix3f()
                .identity()
                .rotateXYZ((float) Math.toRadians(rotX),
                           (float) Math.toRadians(rotY),
                           (float) Math.toRadians(rotZ));

        // Convert JOML → javax.vecmath
        rotationMatrix.m00 = jomlMat.m00(); rotationMatrix.m01 = jomlMat.m01(); rotationMatrix.m02 = jomlMat.m02();
        rotationMatrix.m10 = jomlMat.m10(); rotationMatrix.m11 = jomlMat.m11(); rotationMatrix.m12 = jomlMat.m12();
        rotationMatrix.m20 = jomlMat.m20(); rotationMatrix.m21 = jomlMat.m21(); rotationMatrix.m22 = jomlMat.m22();

        quat.set(rotationMatrix);
        transform.setRotation(quat);

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
        RigidBody body = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(body);
        
        //entityRigidBodyMap.put(entity, body);
        entity.setCollisionBody(body);
        return body;
    }

    /**
     * Adds a static collision mesh (i.e. a non-moving rigid body) to the physics world.
     *
     * @param entity the game entity for which the collision mesh is created
     * @param shape  the collision shape to use
     * @return the created RigidBody with mass set to 0
     */
    public RigidBody addStaticCollisionMesh(Entity entity, CollisionShape shape) {
        float mass = 0f;  // Static objects have zero mass.
        Vector3f inertia = new Vector3f(0, 0, 0);

        Transform transform = new Transform();
        transform.setIdentity();
        Vector3f pos = new Vector3f(entity.getPosition().x, entity.getPosition().y, entity.getPosition().z);
        transform.origin.set(pos);
        

        // Rotation
        float rotX = entity.getRotation().x();
        float rotY = entity.getRotation().y();
        float rotZ = entity.getRotation().z();

        // Convert to QUATERNION for Bullet
        javax.vecmath.Quat4f quat = new javax.vecmath.Quat4f();
        Matrix3f rotationMatrix = new Matrix3f();

        // Build JOML matrix
        org.joml.Matrix3f jomlMat = new org.joml.Matrix3f()
                .identity()
                .rotateXYZ((float) Math.toRadians(rotX),
                           (float) Math.toRadians(rotY),
                           (float) Math.toRadians(rotZ));

        // Convert JOML → javax.vecmath
        rotationMatrix.m00 = jomlMat.m00(); rotationMatrix.m01 = jomlMat.m01(); rotationMatrix.m02 = jomlMat.m02();
        rotationMatrix.m10 = jomlMat.m10(); rotationMatrix.m11 = jomlMat.m11(); rotationMatrix.m12 = jomlMat.m12();
        rotationMatrix.m20 = jomlMat.m20(); rotationMatrix.m21 = jomlMat.m21(); rotationMatrix.m22 = jomlMat.m22();

        quat.set(rotationMatrix);
        transform.setRotation(quat);

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
        RigidBody body = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(body);
        //entityRigidBodyMap.put(entity, body);
        entity.setCollisionBody(body);
        return body;
    }
    
    public RigidBody addRigidBody(Entity entity) {
    	dynamicsWorld.addRigidBody(entity.getCollisionBody());
    	entity.setCollisionBody(entity.getCollisionBody());
    	return  entity.getCollisionBody();
    }
    
    public RigidBody addStaticCollisionTerrainMesh(Terrain terrain, CollisionShape shape) {
        float mass = 0f;  // Static objects have zero mass.
        Vector3f inertia = new Vector3f(0, 0, 0);

        Transform transform = new Transform();
        transform.setIdentity();
        Vector3f pos = new Vector3f(terrain.getX(), 0, terrain.getZ());
        transform.origin.set(pos);

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
        RigidBody body = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(body);
        //entityRigidBodyMap.put(entity, body);
        return body;
    }
    
    public CollisionShape createDynamicConvexCollisionMesh(Entity entity) {
        Mesh mesh = entity.getTexturedModel().getMesh();
        float[] vertices = mesh.getVertices();
        
        // Create an ObjectArrayList and add each vertex point.
        ObjectArrayList<Vector3f> bulletPoints = new ObjectArrayList<>();
        for (int i = 0; i < vertices.length; i += 3) {
            bulletPoints.add(new Vector3f(vertices[i], vertices[i+1], vertices[i+2]));
        }
        
        // Create the ConvexHullShape using the ObjectArrayList.
        ConvexHullShape convexShape = new ConvexHullShape(bulletPoints);
        return convexShape;
    }
    
    public CollisionShape createSimpleAABBCollisionShape(Entity entity) {
        Mesh mesh = entity.getTexturedModel().getMesh();
        float[] vertices = mesh.getVertices();

        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;

        for (int i = 0; i < vertices.length; i += 3) {
            float x = vertices[i];
            float y = vertices[i + 1];
            float z = vertices[i + 2];

            if (x < minX) minX = x;
            if (y < minY) minY = y;
            if (z < minZ) minZ = z;

            if (x > maxX) maxX = x;
            if (y > maxY) maxY = y;
            if (z > maxZ) maxZ = z;
        }

        // half-extents
        float hx = (maxX - minX) / 2f;
        float hy = (maxY - minY) / 2f;
        float hz = (maxZ - minZ) / 2f;

        return new BoxShape(new Vector3f(hx, hy, hz));
    }
    
    public CollisionShape createSimpleSphereCollisionShape(Entity entity) {
       
        return new SphereShape(entity.getTexturedModel().getMesh().getFurthestPoint() / 10.0f);
    }

    
    /**
     * Adds a movable (dynamic) entity with an accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @param mass   the mass of the object (non-zero for dynamic bodies)
     * @return the created RigidBody
     */
    public RigidBody addMovableAccurateCollision(Entity entity, float mass) {
    	CollisionShape shape = createDynamicConvexCollisionMesh(entity);
    	shape.setLocalScaling(new Vector3f(entity.getScale(),entity.getScale(),entity.getScale()));
        return addMovableCollisionMesh(entity, shape, mass);
    }

    /**
     * Adds a movable (dynamic) entity with a not-so-accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @param mass   the mass of the object (non-zero for dynamic bodies)
     * @return the created RigidBody
     */
    public RigidBody addMovableNotSoAccurateCollision(Entity entity, float mass) {
        CollisionShape shape = createNotSoAccurateCollisionMesh(entity);
    	shape.setLocalScaling(new Vector3f(entity.getScale(),entity.getScale(),entity.getScale()));
        return addMovableCollisionMesh(entity, shape, mass);
    }

    /**
     * Adds a static entity with an accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @return the created RigidBody (with zero mass)
     */
    public RigidBody addStaticAccurateCollision(Entity entity) {
        CollisionShape shape = createAccurateCollisionEntity(entity);
    	shape.setLocalScaling(new Vector3f(entity.getScale(),entity.getScale(),entity.getScale()));
        return addStaticCollisionMesh(entity, shape);
    }
    
    public RigidBody addTerrainAccurateCollision(Terrain terrain) {
        CollisionShape shape = createAccurateCollisionMesh(terrain.getMesh(), 1);
    	shape.setLocalScaling(new Vector3f(1,1,1));
        return addStaticCollisionTerrainMesh(terrain, shape);
    }

    /**
     * Adds a static entity with a not-so-accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @return the created RigidBody (with zero mass)
     */
    public RigidBody addStaticNotSoAccurateCollision(Entity entity) {
        CollisionShape shape = createDynamicConvexCollisionMesh(entity);
    	shape.setLocalScaling(new Vector3f(entity.getScale(),entity.getScale(),entity.getScale()));
        return addStaticCollisionMesh(entity, shape);
    }

    private float physicsAccumulator = 2000000f;
    private static final float FIXED_TIME_STEP = 1f / 60;
    private static final int MAX_SUB_STEPS = 1;

    public void updateEntitiesFromCollisionShapes(float deltaTime, List<Entity> entities) {
    	
    	  deltaTime = Math.min(deltaTime, 0.05f);

    	    physicsAccumulator += deltaTime;

    	    int steps = 0;
    	    while (physicsAccumulator >= FIXED_TIME_STEP && steps < MAX_SUB_STEPS) {
    	        dynamicsWorld.stepSimulation(FIXED_TIME_STEP, 0);
    	        physicsAccumulator -= FIXED_TIME_STEP;
    	        steps++;
    	    }
           
           
        for (Entity entity : entities) {
            //RigidBody body = entityRigidBodyMap.get(entity);
        	RigidBody body = entity.getCollisionBody();
            if (body == null) {
                continue; // No physics body associated with this entity.
            }
            
            Transform transform = new Transform();
            // Retrieve the world transform from the body's motion state.
            if (body.getMotionState() != null) {
                body.getMotionState().getWorldTransform(transform);

                // Update the entity's position.
                Vector3f pos = new Vector3f(transform.origin);
                entity.setPosition(pos.x, pos.y, pos.z);
                //System.out.println("Updating: " + pos);

                // Update the entity's rotation if applicable.
                
                Quat4f rotationQuat = new Quat4f();
                transform.getRotation(rotationQuat);
                Vector3f eulerRotation = quaternionToEuler(rotationQuat);
                
                Quat4f rq = new Quat4f();
                transform.getRotation(rq);

                // Bullet → JOML quaternion
                Quaternionf q = new Quaternionf(rq.x, rq.y, rq.z, rq.w).normalize();

                // Extract Euler XYZ (radians)
                org.joml.Vector3f eulerXYZ = new org.joml.Vector3f();
                q.getEulerAnglesXYZ(eulerXYZ);

                // Apply directly (radians)
                entity.setRotation(eulerXYZ);
                
                
                //entity.setRotation(new org.joml.Vector3f(eulerRotation.x, eulerRotation.y, eulerRotation.z));
                
            }
        }
     
        
    }

    //For the mouse picker.
    public void setBodyKinematic(RigidBody body) {
        body.setCollisionFlags(body.getCollisionFlags() | CF_KINEMATIC_OBJECT);
        body.setActivationState(RigidBody.ACTIVE_TAG);
        body.setLinearVelocity(new Vector3f(0, 0, 0));
        body.setAngularVelocity(new Vector3f(0, 0, 0));
    }

    public void setBodyDynamic(Entity entity, RigidBody body) {
        // If the body is static (invMass == 0), don't change it
        if (body.getInvMass() == 0f) {
            return;
        }

        body.setCollisionFlags(body.getCollisionFlags() & ~CF_KINEMATIC_OBJECT);

        Vector3f inertia = new Vector3f();
        float mass = 1.0f; // You can hardcode or store your dynamic object's default
        body.getCollisionShape().calculateLocalInertia(mass, inertia);
        body.setMassProps(mass, inertia);
        body.setSleepingThresholds(0.8f, 1.0f); //
        body.activate(true);
        //body.setActivationState(RigidBody.ACTIVE_TAG);
    }



    
    public RigidBody getBodyForEntity(Entity entity) {
        return entity.getCollisionBody();
    }
    
    public String getCollisionType(Entity entity) {
        RigidBody body = getBodyForEntity(entity);
        if (body == null) {
            return "UNKNOWN";
        }

        int flags = body.getCollisionFlags();
        float invMass = body.getInvMass();

        if ((flags & CF_KINEMATIC_OBJECT) != 0) {
            return "KINEMATIC";
        } else if (invMass == 0f) {
            return "STATIC";
        } else {
            return "DYNAMIC";
        }
    }
    
    public RigidBody cloneRigidBody(Entity entity, float newMass) {
        // 1) Copy transform (position + rotation)
    	RigidBody original = entity.getCollisionBody();
        
        Transform transform = new Transform();
        transform.setIdentity();
        transform.origin.set(new Vector3f(entity.getPosition().x, entity.getPosition().y, entity.getPosition().z));

        // 2) Copy shape (safe to reuse shared shape)
        CollisionShape shape = original.getCollisionShape();

        // 3) Calculate inertia
        Vector3f inertia = new Vector3f(0, 0, 0);
        if (newMass > 0f) {
            shape.calculateLocalInertia(newMass, inertia);
        }

        // 4) Motion state
        DefaultMotionState motionState = new DefaultMotionState(transform);

        // 5) Build construction info
        RigidBodyConstructionInfo info =
                new RigidBodyConstructionInfo(newMass, motionState, shape, inertia);

        // Copy basic physics properties
        info.friction = original.getFriction();
        info.restitution = original.getRestitution();
        info.linearDamping = original.getLinearDamping();
        info.angularDamping = original.getAngularDamping();
        //info.additionalDamping = original.getAdditionalDamping();
        info.additionalAngularDampingFactor = original.getAngularDamping();
        info.additionalLinearDampingThresholdSqr = original.getLinearDamping();
        
        // 6) Create the new body
        RigidBody clone = new RigidBody(info);

        // 7) Copy velocities
        Vector3f linearVel = new Vector3f();
        Vector3f angularVel = new Vector3f();
        original.getLinearVelocity(linearVel);
        original.getAngularVelocity(angularVel);
        clone.setLinearVelocity(linearVel);
        clone.setAngularVelocity(angularVel);

        // 8) Copy activation / flags
        clone.setCollisionFlags(original.getCollisionFlags());
        clone.setActivationState(original.getActivationState());

       

        return clone;
    }

    
    public RayCastResult performRayCast(org.joml.Vector3f start, org.joml.Vector3f end) {
        // Convert JOML vectors to Bullet vectors
        Vector3f from = new Vector3f(start.x, start.y, start.z);
        Vector3f to = new Vector3f(end.x, end.y, end.z);
        
        // Create callback for closest hit
        CollisionWorld.ClosestRayResultCallback callback = 
            new CollisionWorld.ClosestRayResultCallback(from, to);
        
        // Perform the ray test
        dynamicsWorld.rayTest(from, to, callback);
        
        RayCastResult result = new RayCastResult();
        
        if (callback.hasHit()) {
            result.hit = true;
            result.hitPoint = new org.joml.Vector3f(
                callback.hitPointWorld.x,
                callback.hitPointWorld.y,
                callback.hitPointWorld.z
            );
            
            // IMPORTANT: Bullet doesn't guarantee normalized normals
            // Normalize the hit normal
            Vector3f bulletNormal = callback.hitNormalWorld;
            float length = bulletNormal.length();
            
            if (length > 0) {
                // Normalize the normal
                bulletNormal.normalize();
                result.hitNormal = new org.joml.Vector3f(
                    bulletNormal.x,
                    bulletNormal.y,
                    bulletNormal.z
                );
            } else {
                // Fallback in case length is zero (shouldn't happen, but just in case)
                result.hitNormal = new org.joml.Vector3f(0, 1, 0);
                System.err.println("WARNING: Zero-length normal from Bullet physics!");
            }
            
            result.hitBody = (RigidBody) callback.collisionObject;
            result.fraction = callback.closestHitFraction;
        }
        
        return result;
    }
    
    

    public RayCastResult rayCast(org.joml.Vector3f start, org.joml.Vector3f direction, float maxDistance) {
        org.joml.Vector3f end = new org.joml.Vector3f(direction).mul(maxDistance).add(start);
        return performRayCast(start, end); // Call the actual implementation
    }
    
    public RayCastResult rayCast(org.joml.Vector3f from, org.joml.Vector3f to) {
        return performRayCast(from, to);
    }

	    public static RayCastResult rayCast(PhysicsManager physics, org.joml.Vector3f start, 
	            org.joml.Vector3f direction, float maxDistance) {
	return physics.rayCast(start, direction, maxDistance);
	}
	    
	    

	    public static HitResult shoot(Camera camera, PhysicsManager physics, float maxDistance) {
	        if (physics == null) {
	            return new HitResult();
	        }
	        
	        org.joml.Vector3f cameraForward = camera.getForward();
	        org.joml.Vector3f cameraPos = camera.getPosition();
	        
	        // Start bullet slightly in front of camera to avoid self-collision
	        org.joml.Vector3f bulletStart = new org.joml.Vector3f(cameraForward).mul(15.0f).add(cameraPos);
	        bulletStart.y += 2.0f; // Slightly above camera
	        
	        float error = 20.0f;
	        
	        // Randomness for bullet spread
	        bulletStart.x += random.nextFloat(error) - (error / 2.0);
	        bulletStart.y += random.nextFloat(error) - (error / 2.0);
	        bulletStart.z += random.nextFloat(error) - (error / 2.0);
	        
	        org.joml.Vector3f bulletEnd = new org.joml.Vector3f(cameraForward).mul(maxDistance).add(bulletStart);
	        
	        // Get the ray cast result
	        RayCastResult rayResult = physics.performRayCast(bulletStart, bulletEnd);
	        
	        if (!rayResult.hit) {
	            return new HitResult();
	        }
	        
	        // Apply reaction force to dynamic object if hit
	        if (rayResult.hitBody != null ) {
	            // Calculate ray direction
	            org.joml.Vector3f rayDirection = new org.joml.Vector3f(
	                bulletEnd.x - bulletStart.x,
	                bulletEnd.y - bulletStart.y,
	                bulletEnd.z - bulletStart.z
	            ).normalize();
	            
	            // Calculate hit point (if not already provided)
	            org.joml.Vector3f hitPoint = new org.joml.Vector3f(rayResult.hitPoint);
	            
	            // Apply impulse at the hit point
	           
	            float bulletForce = 1000.5f;
	            org.joml.Vector3f impulse = new org.joml.Vector3f(rayDirection).mul(bulletForce);
	            
	            // Apply the impulse to the body at the hit point
	         // World-space center of mass
	            Transform center = new Transform();
	            rayResult.hitBody.getCenterOfMassTransform(center);

	            javax.vecmath.Vector3f relPos = new javax.vecmath.Vector3f(
	                hitPoint.x - center.origin.x,
	                hitPoint.y - center.origin.y,
	                hitPoint.z - center.origin.z
	            );

	            rayResult.hitBody.activate(true);
	            rayResult.hitBody.applyImpulse(
	                new javax.vecmath.Vector3f(impulse.x, impulse.y, impulse.z),
	                relPos
	            );

	            
	    
	        }
	        
	        // Calculate distance from start to hit point
	        float distance = ((Vector3fc) rayResult.hitPoint).distance(bulletStart);
	        
	        // Calculate impact angle
	        org.joml.Vector3f rayDirection = new org.joml.Vector3f(
	            bulletEnd.x - bulletStart.x,
	            bulletEnd.y - bulletStart.y,
	            bulletEnd.z - bulletStart.z
	        ).normalize();
	        
	        // Make sure hit normal is normalized (defensive programming)
	        org.joml.Vector3f normalizedHitNormal = new org.joml.Vector3f(rayResult.hitNormal);
	        if (Math.abs(normalizedHitNormal.lengthSquared() - 1.0f) > 0.001f) {
	            System.err.println("Hit normal not normalized, length: " + normalizedHitNormal.length());
	            normalizedHitNormal.normalize();
	        }
	        
	        // Clamp dot product to avoid NaN from floating-point errors
	        float dot = rayDirection.dot(normalizedHitNormal);
	        dot = Math.abs(dot);
	        
	        if (dot > 1.0f) {
	            dot = 1.0f;
	        } else if (dot < 0) {
	            dot = 0;
	        }
	        
	        if (Float.isNaN(dot)) {
	            dot = 0.0f;
	        }
	        
	        float angle = (float) Math.toDegrees(Math.acos(dot));
	        
	        if (Float.isNaN(angle)) {
	            angle = 0.0f;
	        }
	        
	        return new HitResult(
	            true,
	            rayResult.hitPoint,
	            normalizedHitNormal,  // Use the normalized version
	            distance,
	            angle,
	            rayResult.hitBody
	        );
	    }
    public HitResult rayCastWithDetails(org.joml.Vector3f from, org.joml.Vector3f to, float maxDistance) {
        RayCastResult rayResult = rayCast(from, to, maxDistance);
        
        if (!rayResult.hit) {
            return new HitResult();
        }
        
        // Calculate hit point (already in rayResult.hitPoint)
        org.joml.Vector3f hitPoint = rayResult.getHitPointJOML();
        
        // Calculate distance from start to hit point
        org.joml.Vector3f fromJOML = new org.joml.Vector3f(from.x, from.y, from.z);
        float distance = hitPoint.distance(fromJOML);
        
        // Calculate impact angle (angle between ray direction and surface normal)
        org.joml.Vector3f rayDirection = new org.joml.Vector3f(
            to.x - from.x,
            to.y - from.y,
            to.z - from.z
        ).normalize();
        
        org.joml.Vector3f hitNormal = rayResult.getHitNormalJOML();
        
        // Calculate angle between ray direction and surface normal (0° = perpendicular, 90° = grazing)
        float dot = Math.abs(rayDirection.dot(hitNormal));
        float angle = (float) Math.toDegrees(Math.acos(dot));
        
        return new HitResult(
            true,
            hitPoint,
            hitNormal,
            distance,
            angle,
            rayResult.hitBody
        );
    }
    
    
    public static Vector3f quaternionToEuler(Quat4f q) {
        // Roll (x-axis rotation)
        float sinr_cosp = 2.0f * (q.w * q.x + q.y * q.z);
        float cosr_cosp = 1.0f - 2.0f * (q.x * q.x + q.y * q.y);
        float roll = (float) Math.atan2(sinr_cosp, cosr_cosp);

        // Pitch (y-axis rotation)
        float sinp = 2.0f * (q.w * q.y - q.z * q.x);
        float pitch;
        if (Math.abs(sinp) >= 1)
            pitch = (float) Math.copySign(Math.PI / 2, sinp); // use 90 degrees if out of range
        else
            pitch = (float) Math.asin(sinp);

        // Yaw (z-axis rotation)
        float siny_cosp = 2.0f * (q.w * q.z + q.x * q.y);
        float cosy_cosp = 1.0f - 2.0f * (q.y * q.y + q.z * q.z);
        float yaw = (float) Math.atan2(siny_cosp, cosy_cosp);

        return new Vector3f(roll, pitch, yaw);
    }
    
    public static Quat4f eulerToQuaternion(double roll, double pitch, double yaw) {
        // Convert degrees to radians
        roll = Math.toRadians(roll);
        pitch = Math.toRadians(pitch);
        yaw = Math.toRadians(yaw);

        double cy = Math.cos(yaw * 0.5);
        double sy = Math.sin(yaw * 0.5);
        double cp = Math.cos(pitch * 0.5);
        double sp = Math.sin(pitch * 0.5);
        double cr = Math.cos(roll * 0.5);
        double sr = Math.sin(roll * 0.5);

        double w = cr * cp * cy + sr * sp * sy;
        double x = sr * cp * cy - cr * sp * sy;
        double y = cr * sp * cy + sr * cp * sy;
        double z = cr * cp * sy - sr * sp * cy;

        return new Quat4f((float)x, (float)y, (float)z, (float)w);
    }

    
    
}