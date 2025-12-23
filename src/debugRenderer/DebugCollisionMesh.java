package debugRenderer;

import java.util.List;
import javax.vecmath.Vector3f;
import com.bulletphysics.collision.shapes.*;
import com.bulletphysics.dynamics.RigidBody;
import com.bulletphysics.linearmath.Transform;

public class DebugCollisionMesh implements DebugObject {
    private final RigidBody rigidBody;
    private final CollisionShape shape;
    private final Transform transform;
    private final Vector3f color;
    
    /**
     * Constructor for debugging a RigidBody
     */
    public DebugCollisionMesh(RigidBody rigidBody, Vector3f color) {
        this.rigidBody = rigidBody;
        this.shape = null;
        this.transform = null;
        this.color = color;
    }
    
    /**
     * Constructor for debugging a specific CollisionShape with transform
     */
    public DebugCollisionMesh(CollisionShape shape, Transform transform, Vector3f color) {
        this.rigidBody = null;
        this.shape = shape;
        this.transform = transform;
        this.color = color;
    }
    
    @Override
    public void appendVertexData(List<Float> vertexData) {
        if (rigidBody != null) {
            appendRigidBodyVertexData(vertexData);
        } else if (shape != null && transform != null) {
            appendShapeVertexData(vertexData);
        }
    }
    
    private void appendRigidBodyVertexData(List<Float> vertexData) {
        Transform bodyTransform = new Transform();
        rigidBody.getMotionState().getWorldTransform(bodyTransform);
        
        CollisionShape bodyShape = rigidBody.getCollisionShape();
        appendShapeWithTransform(bodyShape, bodyTransform, vertexData);
    }
    
    private void appendShapeVertexData(List<Float> vertexData) {
        appendShapeWithTransform(shape, transform, vertexData);
    }
    
    private void appendShapeWithTransform(CollisionShape shape, Transform worldTransform, List<Float> vertexData) {
        // Handle different shape types
        if (shape instanceof BvhTriangleMeshShape) {
            appendTriangleMeshVertexData((BvhTriangleMeshShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof BoxShape) {
            appendBoxVertexData((BoxShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof SphereShape) {
            appendSphereVertexData((SphereShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof CapsuleShape) {
            appendCapsuleVertexData((CapsuleShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof ConvexHullShape) {
            appendConvexHullVertexData((ConvexHullShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof ConeShape) {
            appendConeVertexData((ConeShape) shape, worldTransform, vertexData);
        }
        else if (shape instanceof CylinderShape) {
            appendCylinderVertexData((CylinderShape) shape, worldTransform, vertexData);
        }
        else {
            System.err.println("DebugCollisionMesh: Unsupported shape type: " + 
                shape.getClass().getSimpleName());
        }
    }
    
    private void appendTriangleMeshVertexData(BvhTriangleMeshShape meshShape, 
                                             Transform worldTransform, 
                                             List<Float> vertexData) {
        TriangleCollector collector = new TriangleCollector(worldTransform, color, vertexData);
        Vector3f aabbMin = new Vector3f(-10000, -10000, -10000);
        Vector3f aabbMax = new Vector3f(10000, 10000, 10000);
        meshShape.processAllTriangles(collector, aabbMin, aabbMax);
    }
    
    private void appendBoxVertexData(BoxShape boxShape, Transform worldTransform, List<Float> vertexData) {
        Vector3f halfExtents = new Vector3f();
        boxShape.getHalfExtentsWithoutMargin(halfExtents);
        
        // Apply local scaling
        Vector3f scaling = new Vector3f();
        boxShape.getLocalScaling(scaling);
        halfExtents.x *= scaling.x;
        halfExtents.y *= scaling.y;
        halfExtents.z *= scaling.z;
        
        // Calculate the 8 corners of the box in local space
        Vector3f[] corners = new Vector3f[8];
        for (int i = 0; i < 8; i++) {
            corners[i] = new Vector3f(
                (i & 1) == 0 ? -halfExtents.x : halfExtents.x,
                (i & 2) == 0 ? -halfExtents.y : halfExtents.y,
                (i & 4) == 0 ? -halfExtents.z : halfExtents.z
            );
        }
        
        // Transform corners to world space
        for (Vector3f corner : corners) {
            worldTransform.transform(corner);
        }
        
        // Draw the 12 edges of the box
        int[][] edges = {
            {0,1}, {0,2}, {0,4}, {1,3}, {1,5}, {2,3},
            {2,6}, {3,7}, {4,5}, {4,6}, {5,7}, {6,7}
        };
        
        for (int[] edge : edges) {
            addLineAsVertices(corners[edge[0]], corners[edge[1]], vertexData);
        }
    }
    
    private void appendSphereVertexData(SphereShape sphereShape, Transform worldTransform, List<Float> vertexData) {
        float radius = sphereShape.getRadius();
        
        // Apply local scaling (take max component)
        Vector3f scaling = new Vector3f();
        sphereShape.getLocalScaling(scaling);
        radius *= Math.max(scaling.x, Math.max(scaling.y, scaling.z));
        
        Vector3f center = new Vector3f(worldTransform.origin);
        
        // Draw 3 orthogonal circles (approximation of sphere)
        int segments = 16;
        float angleStep = (float) (2 * Math.PI / segments);
        
        // XY circle
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            Vector3f p1 = new Vector3f(
                center.x + radius * (float)Math.cos(angle1),
                center.y + radius * (float)Math.sin(angle1),
                center.z
            );
            Vector3f p2 = new Vector3f(
                center.x + radius * (float)Math.cos(angle2),
                center.y + radius * (float)Math.sin(angle2),
                center.z
            );
            
            addLineAsVertices(p1, p2, vertexData);
        }
        
        // XZ circle
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            Vector3f p1 = new Vector3f(
                center.x + radius * (float)Math.cos(angle1),
                center.y,
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f p2 = new Vector3f(
                center.x + radius * (float)Math.cos(angle2),
                center.y,
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p1, p2, vertexData);
        }
        
        // YZ circle
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            Vector3f p1 = new Vector3f(
                center.x,
                center.y + radius * (float)Math.cos(angle1),
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f p2 = new Vector3f(
                center.x,
                center.y + radius * (float)Math.cos(angle2),
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p1, p2, vertexData);
        }
    }
    
    private void appendCapsuleVertexData(CapsuleShape capsuleShape, Transform worldTransform, List<Float> vertexData) {
        float radius = capsuleShape.getRadius();
        float halfHeight = capsuleShape.getHalfHeight();
        
        // Apply local scaling
        Vector3f scaling = new Vector3f();
        capsuleShape.getLocalScaling(scaling);
        radius *= Math.max(scaling.x, Math.max(scaling.y, scaling.z));
        halfHeight *= scaling.y;
        
        Vector3f center = new Vector3f(worldTransform.origin);
        
        // Draw cylinder part
        int segments = 16;
        float angleStep = (float) (2 * Math.PI / segments);
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            // Top circle
            Vector3f p1 = new Vector3f(
                center.x + radius * (float)Math.cos(angle1),
                center.y + halfHeight,
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f p2 = new Vector3f(
                center.x + radius * (float)Math.cos(angle2),
                center.y + halfHeight,
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p1, p2, vertexData);
            
            // Bottom circle
            Vector3f p3 = new Vector3f(
                center.x + radius * (float)Math.cos(angle1),
                center.y - halfHeight,
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f p4 = new Vector3f(
                center.x + radius * (float)Math.cos(angle2),
                center.y - halfHeight,
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p3, p4, vertexData);
            
            // Vertical lines
            addLineAsVertices(p1, p3, vertexData);
        }
        
        // Draw hemispherical caps (simplified as circles)
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            // Top hemisphere circle (XY plane at top)
            Vector3f t1 = new Vector3f(
                center.x,
                center.y + halfHeight + radius * (float)Math.cos(angle1),
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f t2 = new Vector3f(
                center.x,
                center.y + halfHeight + radius * (float)Math.cos(angle2),
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(t1, t2, vertexData);
            
            // Bottom hemisphere circle (XY plane at bottom)
            Vector3f b1 = new Vector3f(
                center.x,
                center.y - halfHeight - radius * (float)Math.cos(angle1),
                center.z + radius * (float)Math.sin(angle1)
            );
            Vector3f b2 = new Vector3f(
                center.x,
                center.y - halfHeight - radius * (float)Math.cos(angle2),
                center.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(b1, b2, vertexData);
        }
    }
    
    private void appendConvexHullVertexData(ConvexHullShape hullShape, Transform worldTransform, List<Float> vertexData) {
        int numVertices = hullShape.getNumVertices();
        
        // Apply local scaling
        Vector3f scaling = new Vector3f();
        hullShape.getLocalScaling(scaling);
        
        // Get and transform all vertices
        Vector3f[] vertices = new Vector3f[numVertices];
        for (int i = 0; i < numVertices; i++) {
            Vector3f vertex = new Vector3f();
            hullShape.getVertex(i, vertex);
            
            // Apply scaling
            vertex.x *= scaling.x;
            vertex.y *= scaling.y;
            vertex.z *= scaling.z;
            
            // Transform to world space
            worldTransform.transform(vertex);
            vertices[i] = vertex;
        }
        
        // Simple wireframe: draw lines between vertices (this may create many lines)
        // For better visualization, you might want to get the actual edges
        for (int i = 0; i < numVertices; i++) {
            for (int j = i + 1; j < Math.min(i + 5, numVertices); j++) { // Limit connections
                addLineAsVertices(vertices[i], vertices[j], 
                    new Vector3f(color.x * 0.7f, color.y * 0.7f, color.z * 0.7f), vertexData);
            }
        }
    }
    
    private void appendConeVertexData(ConeShape coneShape, Transform worldTransform, List<Float> vertexData) {
        float radius = coneShape.getRadius();
        float height = coneShape.getHeight();
        
        Vector3f scaling = new Vector3f();
        coneShape.getLocalScaling(scaling);
        radius *= Math.max(scaling.x, scaling.z);
        height *= scaling.y;
        
        Vector3f baseCenter = new Vector3f(worldTransform.origin);
        baseCenter.y -= height / 2;
        
        Vector3f tip = new Vector3f(worldTransform.origin);
        tip.y += height / 2;
        
        int segments = 16;
        float angleStep = (float) (2 * Math.PI / segments);
        
        // Draw base circle
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            Vector3f p1 = new Vector3f(
                baseCenter.x + radius * (float)Math.cos(angle1),
                baseCenter.y,
                baseCenter.z + radius * (float)Math.sin(angle1)
            );
            Vector3f p2 = new Vector3f(
                baseCenter.x + radius * (float)Math.cos(angle2),
                baseCenter.y,
                baseCenter.z + radius * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p1, p2, vertexData);
            
            // Draw lines from base to tip
            addLineAsVertices(p1, tip, vertexData);
        }
    }
    
    private void appendCylinderVertexData(CylinderShape cylinderShape, Transform worldTransform, List<Float> vertexData) {
        Vector3f halfExtents = new Vector3f();
        cylinderShape.getHalfExtentsWithoutMargin(halfExtents);
        
        Vector3f scaling = new Vector3f();
        cylinderShape.getLocalScaling(scaling);
        halfExtents.x *= scaling.x;
        halfExtents.y *= scaling.y;
        halfExtents.z *= scaling.z;
        
        Vector3f center = new Vector3f(worldTransform.origin);
        
        // Draw similar to capsule but without hemispherical caps
        int segments = 16;
        float angleStep = (float) (2 * Math.PI / segments);
        
        for (int i = 0; i < segments; i++) {
            float angle1 = i * angleStep;
            float angle2 = ((i + 1) % segments) * angleStep;
            
            // Top circle
            Vector3f p1 = new Vector3f(
                center.x + halfExtents.x * (float)Math.cos(angle1),
                center.y + halfExtents.y,
                center.z + halfExtents.z * (float)Math.sin(angle1)
            );
            Vector3f p2 = new Vector3f(
                center.x + halfExtents.x * (float)Math.cos(angle2),
                center.y + halfExtents.y,
                center.z + halfExtents.z * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p1, p2, vertexData);
            
            // Bottom circle
            Vector3f p3 = new Vector3f(
                center.x + halfExtents.x * (float)Math.cos(angle1),
                center.y - halfExtents.y,
                center.z + halfExtents.z * (float)Math.sin(angle1)
            );
            Vector3f p4 = new Vector3f(
                center.x + halfExtents.x * (float)Math.cos(angle2),
                center.y - halfExtents.y,
                center.z + halfExtents.z * (float)Math.sin(angle2)
            );
            
            addLineAsVertices(p3, p4, vertexData);
            
            // Vertical lines
            addLineAsVertices(p1, p3, vertexData);
        }
    }
    
    /**
     * Helper class to collect triangle vertices for rendering
     */
    private static class TriangleCollector extends TriangleCallback {
        private final Transform transform;
        private final Vector3f color;
        private final List<Float> vertexData;
        
        public TriangleCollector(Transform transform, Vector3f color, List<Float> vertexData) {
            this.transform = transform;
            this.color = color;
            this.vertexData = vertexData;
        }
        
        @Override
        public void processTriangle(Vector3f[] triangle, int partId, int triangleIndex) {
            // Transform triangle vertices to world space
            Vector3f v1 = new Vector3f(triangle[0]);
            Vector3f v2 = new Vector3f(triangle[1]);
            Vector3f v3 = new Vector3f(triangle[2]);
            
            transform.transform(v1);
            transform.transform(v2);
            transform.transform(v3);
            
            // Add triangle as 3 lines (wireframe)
            addLineAsVertices(v1, v2, color, vertexData);
            addLineAsVertices(v2, v3, color, vertexData);
            addLineAsVertices(v3, v1, color, vertexData);
        }
        
        public void internalProcessTriangleIndex(Vector3f[] triangle, int partId, int triangleIndex) {
            processTriangle(triangle, partId, triangleIndex);
        }
    }
    
    private void addLineAsVertices(Vector3f start, Vector3f end, List<Float> vertexData) {
        addLineAsVertices(start, end, color, vertexData);
    }
    
    private static void addLineAsVertices(Vector3f start, Vector3f end, Vector3f lineColor, List<Float> vertexData) {
        // Add start vertex
        vertexData.add(start.x);
        vertexData.add(start.y);
        vertexData.add(start.z);
        vertexData.add(lineColor.x);
        vertexData.add(lineColor.y);
        vertexData.add(lineColor.z);
        
        // Add end vertex
        vertexData.add(end.x);
        vertexData.add(end.y);
        vertexData.add(end.z);
        vertexData.add(lineColor.x);
        vertexData.add(lineColor.y);
        vertexData.add(lineColor.z);
    }
    
    // Getters for external use if needed
    public RigidBody getRigidBody() {
        return rigidBody;
    }
    
    public CollisionShape getShape() {
        return shape;
    }
    
    public Vector3f getColor() {
        return color;
    }
}