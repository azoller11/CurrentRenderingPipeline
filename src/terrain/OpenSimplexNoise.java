package terrain;

/*
 * OpenSimplex Noise in Java.
 *
 * Originally created by Kurt Spencer
 * (public domain).
 *
 * This version is a simplified port to be used for terrain generation.
 */
public class OpenSimplexNoise {
    
    private static final double STRETCH_CONSTANT_2D = -0.211324865405187; //(1/Math.sqrt(2+1)-1)/2;
    private static final double SQUISH_CONSTANT_2D = 0.366025403784439; //(Math.sqrt(2+1)-1)/2;
    
    private static final double NORM_CONSTANT_2D = 47.0;
    
    private short[] perm;
    private short[] permGradIndex2D;
    
    public OpenSimplexNoise(long seed) {
        perm = new short[256];
        permGradIndex2D = new short[256];
        short[] source = new short[256];
        for (short i = 0; i < 256; i++)
            source[i] = i;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        seed = seed * 6364136223846793005L + 1442695040888963407L;
        for (int i = 255; i >= 0; i--) {
            seed = seed * 6364136223846793005L + 1442695040888963407L;
            int r = (int)((seed + 31) % (i + 1));
            if (r < 0)
                r += (i + 1);
            perm[i] = source[r];
            permGradIndex2D[i] = (short)((perm[i] % (gradients2D.length / 2)) * 2);
            source[r] = source[i];
        }
    }
    
    // 2D OpenSimplex Noise.
    public double eval(double x, double y) {

        // Place input coordinates onto grid.
        double stretchOffset = (x + y) * STRETCH_CONSTANT_2D;
        double xs = x + stretchOffset;
        double ys = y + stretchOffset;
        
        // Floor to get grid coordinates of rhombus (stretched square) cells.
        int xsb = fastFloor(xs);
        int ysb = fastFloor(ys);
        
        // Skew out to get actual coordinates of rhombus origin. We'll need these later.
        double squishOffset = (xsb + ysb) * SQUISH_CONSTANT_2D;
        double xb = xsb + squishOffset;
        double yb = ysb + squishOffset;
        
        // Compute grid coordinates relative to rhombus origin.
        double xins = xs - xsb;
        double yins = ys - ysb;
        
        // Sum those together to get a value that determines which region we're in.
        double inSum = xins + yins;
        
        // Positions relative to origin point.
        double dx0 = x - xb;
        double dy0 = y - yb;
        
        // We'll be defining these inside the next block and using them afterwards.
        double dx_ext, dy_ext;
        int xsv_ext, ysv_ext;
        
        double value = 0;
        
        // Contribution (1,0)
        double dx1 = dx0 - 1 - SQUISH_CONSTANT_2D;
        double dy1 = dy0 - 0 - SQUISH_CONSTANT_2D;
        double attn1 = 2 - dx1 * dx1 - dy1 * dy1;
        if (attn1 > 0) {
            attn1 *= attn1;
            int ii = xsb + 1;
            int jj = ysb;
            int gi = perm[(ii + perm[jj & 0xFF]) & 0xFF] % (gradients2D.length / 2) * 2;
            double grad = gradients2D[gi] * dx1 + gradients2D[gi+1] * dy1;
            value += attn1 * attn1 * grad;
        }
        
        // Contribution (0,1)
        double dx2 = dx0 - 0 - SQUISH_CONSTANT_2D;
        double dy2 = dy0 - 1 - SQUISH_CONSTANT_2D;
        double attn2 = 2 - dx2 * dx2 - dy2 * dy2;
        if (attn2 > 0) {
            attn2 *= attn2;
            int ii = xsb;
            int jj = ysb + 1;
            int gi = perm[(ii + perm[jj & 0xFF]) & 0xFF] % (gradients2D.length / 2) * 2;
            double grad = gradients2D[gi] * dx2 + gradients2D[gi+1] * dy2;
            value += attn2 * attn2 * grad;
        }
        
        // Contribution (0,0)
        double attn0 = 2 - dx0 * dx0 - dy0 * dy0;
        if (attn0 > 0) {
            attn0 *= attn0;
            int gi = perm[(xsb + perm[ysb & 0xFF]) & 0xFF] % (gradients2D.length / 2) * 2;
            double grad = gradients2D[gi] * dx0 + gradients2D[gi+1] * dy0;
            value += attn0 * attn0 * grad;
        }
        
        return value / NORM_CONSTANT_2D;
    }
    
    private static int fastFloor(double x) {
        int xi = (int)x;
        return x < xi ? xi - 1 : xi;
    }
    
    private static final double[] gradients2D = {
        5,  2,    2,  5,
       -5,  2,   -2,  5,
        5, -2,    2, -5,
       -5, -2,   -2, -5,
    };
}
