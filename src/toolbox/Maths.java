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
	 
	 public static Matrix4f sub(Matrix4f... sequence) {
		    assert sequence.length > 0;

		    Matrix4f res = new Matrix4f(sequence[0]);

		    for (int i = 1; i < sequence.length; i++) {
		        Matrix4f m = sequence[i];

		        res.m00(res.m00() - m.m00());
		        res.m01(res.m01() - m.m01());
		        res.m02(res.m02() - m.m02());
		        res.m03(res.m03() - m.m03());

		        res.m10(res.m10() - m.m10());
		        res.m11(res.m11() - m.m11());
		        res.m12(res.m12() - m.m12());
		        res.m13(res.m13() - m.m13());

		        res.m20(res.m20() - m.m20());
		        res.m21(res.m21() - m.m21());
		        res.m22(res.m22() - m.m22());
		        res.m23(res.m23() - m.m23());

		        res.m30(res.m30() - m.m30());
		        res.m31(res.m31() - m.m31());
		        res.m32(res.m32() - m.m32());
		        res.m33(res.m33() - m.m33());
		    }

		    return res;
		}

	 
	    public static org.joml.Matrix4f invert(org.joml.Matrix4f original)
	    {
	        return new org.joml.Matrix4f(original).invert();
	    }


	   public static org.joml.Matrix4f mul(org.joml.Matrix4f... sequence)
	    {
	    	org.joml.Matrix4f res = new org.joml.Matrix4f();

	        for (org.joml.Matrix4f m : sequence)
	            res.mul(m);

	        return res;
	    }

	  public static org.joml.Vector3f convertVector(AIVector3D assimp)
	    {
	        return new org.joml.Vector3f(assimp.x(), assimp.y(), assimp.z());
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

	    public static void convertMatrix(AIMatrix4x4 assimp, Matrix4f dest)
	    {
	        dest.set(
	            assimp.a1(), assimp.b1(), assimp.c1(), assimp.d1(),
	            assimp.a2(), assimp.b2(), assimp.c2(), assimp.d2(),
	            assimp.a3(), assimp.b3(), assimp.c3(), assimp.d3(),
	            assimp.a4(), assimp.b4(), assimp.c4(), assimp.d4()
	        );
	    }

	    public static float lerp(float a, float b, float t) {
	        return a + (b - a) * t;
	    }

	    public static float clamp(float value, float min, float max) {
	        return Math.max(min, Math.min(max, value));
	    }


	

}
