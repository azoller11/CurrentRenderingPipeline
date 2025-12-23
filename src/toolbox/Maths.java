package toolbox;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.lwjgl.assimp.AIMatrix4x4;
import org.lwjgl.assimp.AIQuaternion;
import org.lwjgl.assimp.AIVector3D;

public class Maths {
	
	 public static org.joml.Matrix4f convertMatrix(AIMatrix4x4 assimp)
	    {
//	        return new Matrix4f(
//	                assimp.a1(), assimp.a2(), assimp.a3(), assimp.a4(),
//	                assimp.b1(), assimp.b2(), assimp.b3(), assimp.b4(),
//	                assimp.c1(), assimp.c2(), assimp.c3(), assimp.c4(),
//	                assimp.d1(), assimp.d2(), assimp.d3(), assimp.d4()
//	        );
	        return new org.joml.Matrix4f(
	                assimp.a1(), assimp.b1(), assimp.c1(), assimp.d1(),
	                assimp.a2(), assimp.b2(), assimp.c2(), assimp.d2(),
	                assimp.a3(), assimp.b3(), assimp.c3(), assimp.d3(),
	                assimp.a4(), assimp.b4(), assimp.c4(), assimp.d4()
	        );
	    }

	 public static org.joml.Matrix4f mul(org.joml.Matrix4f... sequence)
	    {
	    	org.joml.Matrix4f res = new org.joml.Matrix4f();

	        for (org.joml.Matrix4f m : sequence)
	            res.mul(m);

	        return res;
	    }

	public static Vector3f convertVector(AIVector3D mValue) {
		// TODO Auto-generated method stub
		return null;
	}

	 public static org.joml.Vector3f sum(org.joml.Vector3f... sequence)
	    {
	    	org.joml.Vector3f res = new org.joml.Vector3f();

	        for (org.joml.Vector3f v : sequence)
	            res.add(v);

	        return res;
	    }

	    public static org.joml.Vector2f sum(org.joml.Vector2f... sequence)
	    {
	    	org.joml.Vector2f res = new org.joml.Vector2f();

	        for (org.joml.Vector2f v : sequence)
	            res.add(v);

	        return res;
	    }

	    public static org.joml.Vector3f sub(org.joml.Vector3f... sequence)
	    {
	        assert sequence.length > 0;

	        org.joml.Vector3f res = new org.joml.Vector3f(sequence[0]);

	        for (int i = 1; i < sequence.length; i++)
	            res.sub(sequence[i]);

	        return res;
	    }

	    public static org.joml.Vector2f sub(org.joml.Vector2f... sequence)
	    {
	        assert sequence.length > 0;

	        org.joml.Vector2f res = new org.joml.Vector2f(sequence[0]);

	        for (int i = 1; i < sequence.length; i++)
	            res.sub(sequence[i]);

	        return res;
	    }

		public static Quaternionf convertQuaternion(AIQuaternion mValue) {
			// TODO Auto-generated method stub
			return null;
		}


	    public static Quaternionf slerp(Quaternionf start, Quaternionf end, float alpha)
	    {
	        Quaternionf a = new Quaternionf(start);
	        Quaternionf b = new Quaternionf(end);

	        a.slerp(b, alpha);

	        return a;
	    }
	

}
