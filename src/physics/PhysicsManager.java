package physics;

import com.bulletphysics.BulletGlobals;
import com.bulletphysics.collision.broadphase.BroadphaseInterface;
import com.bulletphysics.collision.broadphase.DbvtBroadphase;
import com.bulletphysics.collision.dispatch.CollisionDispatcher;
import com.bulletphysics.collision.dispatch.CollisionConfiguration;
import com.bulletphysics.collision.dispatch.DefaultCollisionConfiguration;
import com.bulletphysics.collision.shapes.BoxShape;
import com.bulletphysics.collision.shapes.BvhTriangleMeshShape;
import com.bulletphysics.collision.shapes.CollisionShape;
import com.bulletphysics.collision.shapes.TriangleIndexVertexArray;
import com.bulletphysics.collision.shapes.IndexedMesh;
import com.bulletphysics.collision.shapes.ScalarType;
import com.bulletphysics.dynamics.DiscreteDynamicsWorld;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.dynamics.RigidBodyConstructionInfo;
import com.bulletphysics.dynamics.constraintsolver.ConstraintSolver;
import com.bulletphysics.dynamics.constraintsolver.SequentialImpulseConstraintSolver;
import com.bulletphysics.linearmath.DefaultMotionState;
import com.bulletphysics.linearmath.Transform;
import com.bulletphysics.util.ObjectArrayList;
import com.bulletphysics.collision.shapes.ConvexHullShape;
import entities.Entity;
import toolbox.Mesh;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;

import javax.vecmath.Vector3f;
import javax.vecmath.Quat4f;

public class PhysicsManager {

    private DiscreteDynamicsWorld dynamicsWorld;
    
    private Map<Entity, RigidBody> entityRigidBodyMap = new HashMap<>();
    
    private static final ScalarType PHY_FLOAT = ScalarType.FLOAT;

    public PhysicsManager() {
        // Set up the physics world with basic configuration
        CollisionConfiguration collisionConfiguration = new DefaultCollisionConfiguration();
        CollisionDispatcher dispatcher = new CollisionDispatcher(collisionConfiguration);
        BroadphaseInterface broadphase = new DbvtBroadphase();
        ConstraintSolver solver = new SequentialImpulseConstraintSolver();
        dynamicsWorld = new DiscreteDynamicsWorld(dispatcher, broadphase, solver, collisionConfiguration);
        dynamicsWorld.setGravity(new Vector3f(0f, -9.81f, 0f));
    }

    /**
     * Creates an accurate collision shape from an entity's mesh.
     * This method uses a triangle mesh shape (BvhTriangleMeshShape) for high-precision collision detection.
     *
     * @param entity the game entity whose mesh is used to build the collision shape
     * @return a CollisionShape for the given entity
     */
    public CollisionShape createAccurateCollisionMesh(Entity entity) {
        Mesh mesh = entity.getMesh();
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

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
        RigidBody body = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(body);
        
        entityRigidBodyMap.put(entity, body);
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

        DefaultMotionState motionState = new DefaultMotionState(transform);
        RigidBodyConstructionInfo rbInfo = new RigidBodyConstructionInfo(mass, motionState, shape, inertia);
        RigidBody body = new RigidBody(rbInfo);
        dynamicsWorld.addRigidBody(body);
        entityRigidBodyMap.put(entity, body);
        return body;
    }
    
    public CollisionShape createDynamicConvexCollisionMesh(Entity entity) {
        Mesh mesh = entity.getMesh();
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
    
    /**
     * Adds a movable (dynamic) entity with an accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @param mass   the mass of the object (non-zero for dynamic bodies)
     * @return the created RigidBody
     */
    public RigidBody addMovableAccurateCollision(Entity entity, float mass) {
    	CollisionShape shape = createDynamicConvexCollisionMesh(entity);
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
        return addMovableCollisionMesh(entity, shape, mass);
    }

    /**
     * Adds a static entity with an accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @return the created RigidBody (with zero mass)
     */
    public RigidBody addStaticAccurateCollision(Entity entity) {
        CollisionShape shape = createAccurateCollisionMesh(entity);
        return addStaticCollisionMesh(entity, shape);
    }

    /**
     * Adds a static entity with a not-so-accurate collision mesh.
     *
     * @param entity the game entity for which the collision mesh is created
     * @return the created RigidBody (with zero mass)
     */
    public RigidBody addStaticNotSoAccurateCollision(Entity entity) {
        CollisionShape shape = createDynamicConvexCollisionMesh(entity);
        return addStaticCollisionMesh(entity, shape);
    }


    /**
     * Updates the physics simulation.
     *
     * @param deltaTime the time elapsed since the last update (in seconds)
     */
    public void update(float deltaTime) {
        dynamicsWorld.stepSimulation(deltaTime);
    }
    
    public void updateEntitiesFromCollisionShapes(float deltaTime, List<Entity> entities) {
    	
        for (Entity entity : entities) {
            RigidBody body = entityRigidBodyMap.get(entity);
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
                entity.setRotation(new org.joml.Vector3f(eulerRotation.x, eulerRotation.y, eulerRotation.z));
            }
        }
        
        dynamicsWorld.stepSimulation(deltaTime);
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
