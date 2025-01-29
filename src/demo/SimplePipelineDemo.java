package demo;

import org.lwjgl.*;
import org.lwjgl.glfw.*;
import org.lwjgl.opengl.*;
import org.lwjgl.system.*;

import java.nio.FloatBuffer;

import static org.lwjgl.glfw.Callbacks.glfwFreeCallbacks;
import static org.lwjgl.glfw.GLFW.*;
import static org.lwjgl.opengl.GL40.*;  // GL4 entry points
import static org.lwjgl.system.MemoryStack.stackPush;
import static org.lwjgl.system.MemoryUtil.NULL;

public class SimplePipelineDemo {

    // --------------------------------------------------------------------------------
    // 1) Vertex Shader - applies rotation + 3D projection, passes color to TCS
    // --------------------------------------------------------------------------------
    private static final String VERTEX_SHADER_SRC =
        "#version 400 core\n" +
        "layout(location = 0) in vec3 inPosition;\n" +
        "layout(location = 1) in vec3 inColor;\n" +
        "\n" +
        "// We'll pass position via gl_Position, but color needs a custom block\n" +
        "out VS_OUT {\n" +
        "    vec3 color;\n" +
        "} vs_out;\n" +
        "\n" +
        "uniform mat4 model;\n" +
        "uniform mat4 view;\n" +
        "uniform mat4 projection;\n" +
        "\n" +
        "void main() {\n" +
        "    // Pass the color along\n" +
        "    vs_out.color = inColor;\n" +
        "\n" +
        "    // Compute final position\n" +
        "    gl_Position = projection * view * model * vec4(inPosition, 1.0);\n" +
        "}\n";

    // --------------------------------------------------------------------------------
    // 2) Tessellation Control Shader - pass-through position **and** color
    // --------------------------------------------------------------------------------
    private static final String TESSELLATION_CONTROL_SHADER_SRC =
        "#version 400 core\n" +
        "layout(vertices = 3) out;\n" +
        "\n" +
        "// Receive color from Vertex Shader\n" +
        "in VS_OUT {\n" +
        "    vec3 color;\n" +
        "} tcs_in[];\n" +
        "\n" +
        "// Pass color onward to TES\n" +
        "out VS_OUT {\n" +
        "    vec3 color;\n" +
        "} tcs_out[];\n" +
        "\n" +
        "void main() {\n" +
        "    // Copy position from input to output\n" +
        "    gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;\n" +
        "\n" +
        "    // Copy color\n" +
        "    tcs_out[gl_InvocationID].color = tcs_in[gl_InvocationID].color;\n" +
        "\n" +
        "    // Tessellation levels (1 => no subdivision)\n" +
        "    gl_TessLevelInner[0] = 1.0;\n" +
        "    gl_TessLevelOuter[0] = 1.0;\n" +
        "    gl_TessLevelOuter[1] = 1.0;\n" +
        "    gl_TessLevelOuter[2] = 1.0;\n" +
        "}\n";

    // --------------------------------------------------------------------------------
    // 3) Tessellation Evaluation Shader - pass-through position, **interpolate color**
    // --------------------------------------------------------------------------------
    private static final String TESSELLATION_EVALUATION_SHADER_SRC =
        "#version 400 core\n" +
        "layout(triangles, equal_spacing, cw) in;\n" +
        "\n" +
        "// Receive color from TCS\n" +
        "in VS_OUT {\n" +
        "    vec3 color;\n" +
        "} tes_in[];\n" +
        "\n" +
        "// Pass color to the Geometry Shader\n" +
        "out VS_OUT {\n" +
        "    vec3 color;\n" +
        "} tes_out;\n" +
        "\n" +
        "void main() {\n" +
        "    // Barycentric interpolation of color\n" +
        "    tes_out.color = gl_TessCoord.x * tes_in[0].color +\n" +
        "                    gl_TessCoord.y * tes_in[1].color +\n" +
        "                    gl_TessCoord.z * tes_in[2].color;\n" +
        "\n" +
        "    // Interpolate position\n" +
        "    gl_Position = gl_TessCoord.x * gl_in[0].gl_Position +\n" +
        "                  gl_TessCoord.y * gl_in[1].gl_Position +\n" +
        "                  gl_TessCoord.z * gl_in[2].gl_Position;\n" +
        "}\n";

    // --------------------------------------------------------------------------------
    // 4) Geometry Shader - pass-through position and color
    // --------------------------------------------------------------------------------
    private static final String GEOMETRY_SHADER_SRC =
        "#version 400 core\n" +
        "layout(triangles) in;\n" +
        "layout(triangle_strip, max_vertices=3) out;\n" +
        "\n" +
        "// Receive color from TES\n" +
        "in VS_OUT {\n" +
        "    vec3 color;\n" +
        "} gs_in[];\n" +
        "\n" +
        "// Pass color to the Fragment Shader\n" +
        "out vec3 fragColor;\n" +
        "\n" +
        "void main() {\n" +
        "    // Emit each vertex, copying position and color\n" +
        "    for(int i = 0; i < 3; i++) {\n" +
        "        fragColor = gs_in[i].color;\n" +
        "        gl_Position = gl_in[i].gl_Position;\n" +
        "        EmitVertex();\n" +
        "    }\n" +
        "    EndPrimitive();\n" +
        "}\n";

    // --------------------------------------------------------------------------------
    // 5) Fragment Shader - receives `fragColor` and outputs final color
    // --------------------------------------------------------------------------------
    private static final String FRAGMENT_SHADER_SRC =
        "#version 400 core\n" +
        "in vec3 fragColor;\n" +
        "out vec4 outColor;\n" +
        "void main() {\n" +
        "    outColor = vec4(fragColor, 1.0);\n" +
        "}\n";

    private long window;
    private int width = 800;
    private int height = 600;

    private int shaderProgram;
    private int vaoId;
    private int modelLoc, viewLoc, projLoc;

    // Rotation angles
    private float angleX = 0.0f;
    private float angleY = 0.0f;

    public static void main(String[] args) {
        new SimplePipelineDemo().run();
    }

    public void run() {
        init();
        loop();
        cleanup();
    }

    private void init() {
        // Initialize GLFW
        if (!glfwInit()) {
            throw new IllegalStateException("Unable to initialize GLFW");
        }

        // Configure GLFW for OpenGL4 core profile
        glfwWindowHint(GLFW_CONTEXT_VERSION_MAJOR, 4);
        glfwWindowHint(GLFW_CONTEXT_VERSION_MINOR, 0);
        glfwWindowHint(GLFW_OPENGL_PROFILE, GLFW_OPENGL_CORE_PROFILE);
        glfwWindowHint(GLFW_OPENGL_FORWARD_COMPAT, GLFW_TRUE);

        // Create the window
        window = glfwCreateWindow(width, height, "OpenGL4 Rotating Cube", NULL, NULL);
        if (window == NULL) {
            throw new RuntimeException("Failed to create the GLFW window");
        }
        glfwMakeContextCurrent(window);

        // Enable v-sync
        glfwSwapInterval(1);

        // Create the OpenGL capabilities
        GL.createCapabilities();

        // Build and link our shader program
        shaderProgram = createShaderProgram();

        // Create VAO, VBO, and upload geometry data
        vaoId = createVAO();

        // Retrieve uniform locations for model/view/projection
        modelLoc = glGetUniformLocation(shaderProgram, "model");
        viewLoc = glGetUniformLocation(shaderProgram, "view");
        projLoc = glGetUniformLocation(shaderProgram, "projection");

        // Configure the default OpenGL state
        glEnable(GL_DEPTH_TEST);
        glClearColor(0.2f, 0.3f, 0.4f, 1.0f);
    }

    private void loop() {
        while (!glfwWindowShouldClose(window)) {
            glfwPollEvents();

            glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);

            // Update rotation angles
            angleX += 0.01f;
            angleY += 0.015f;

            // Model matrix: rotate around X and Y
            float[] modelMatrix = multiply(
                    makeRotateX(angleX),
                    makeRotateY(angleY));

            // View matrix: translate camera back a bit (z = -2.5)
            float[] viewMatrix = makeTranslation(0.0f, 0.0f, -2.5f);

            // Perspective projection
            float[] projMatrix = makePerspective(
                    (float) Math.toRadians(45.0),
                    (float) width / height,
                    0.1f,
                    100.0f);

            // Use our shader
            glUseProgram(shaderProgram);

            // Upload matrices
            try (var stack = stackPush()) {
                FloatBuffer fb = stack.mallocFloat(16);

                glUniformMatrix4fv(modelLoc, false, fb.put(modelMatrix).flip());
                fb.clear();
                glUniformMatrix4fv(viewLoc, false, fb.put(viewMatrix).flip());
                fb.clear();
                glUniformMatrix4fv(projLoc, false, fb.put(projMatrix).flip());
            }

            // Bind VAO and draw
            glBindVertexArray(vaoId);

            // Use GL_PATCHES because we have tessellation
            glPatchParameteri(GL_PATCH_VERTICES, 3);
            // We have 36 vertices for the cube (6 faces × 2 triangles × 3 vertices)
            glDrawArrays(GL_PATCHES, 0, 36);

            glBindVertexArray(0);

            glfwSwapBuffers(window);
        }
    }

    private void cleanup() {
        glDeleteProgram(shaderProgram);
        glDeleteVertexArrays(vaoId);

        glfwFreeCallbacks(window);
        glfwDestroyWindow(window);
        glfwTerminate();
    }

    private int createVAO() {
        // A cube with 6 faces, each face has 2 triangles = 12 triangles
        // => 12 × 3 vertices = 36 total. Each vertex has Position (x,y,z) + Color (r,g,b)
        // Cube center at (0,0,0) with side length = 1.0 => corners at ±0.5

        float[] vertices = {
            // Front face (Z = +0.5), color = Red
            -0.5f, -0.5f, +0.5f,   1.0f, 0.0f, 0.0f,
             0.5f, -0.5f, +0.5f,   1.0f, 0.0f, 0.0f,
             0.5f,  0.5f, +0.5f,   1.0f, 0.0f, 0.0f,

            -0.5f, -0.5f, +0.5f,   1.0f, 0.0f, 0.0f,
             0.5f,  0.5f, +0.5f,   1.0f, 0.0f, 0.0f,
            -0.5f,  0.5f, +0.5f,   1.0f, 0.0f, 0.0f,

            // Back face (Z = -0.5), color = Green
             0.5f, -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
            -0.5f, -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 0.0f,

             0.5f, -0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
            -0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 0.0f,

            // Left face (X = -0.5), color = Blue
            -0.5f, -0.5f, -0.5f,   0.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,   0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,   0.0f, 0.0f, 1.0f,

            -0.5f, -0.5f, -0.5f,   0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f,  0.5f,   0.0f, 0.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,   0.0f, 0.0f, 1.0f,

            // Right face (X = +0.5), color = Yellow
             0.5f, -0.5f,  0.5f,   1.0f, 1.0f, 0.0f,
             0.5f, -0.5f, -0.5f,   1.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,   1.0f, 1.0f, 0.0f,

             0.5f, -0.5f,  0.5f,   1.0f, 1.0f, 0.0f,
             0.5f,  0.5f, -0.5f,   1.0f, 1.0f, 0.0f,
             0.5f,  0.5f,  0.5f,   1.0f, 1.0f, 0.0f,

            // Top face (Y = +0.5), color = Cyan
            -0.5f,  0.5f,  0.5f,   0.0f, 1.0f, 1.0f,
             0.5f,  0.5f,  0.5f,   0.0f, 1.0f, 1.0f,
             0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 1.0f,

            -0.5f,  0.5f,  0.5f,   0.0f, 1.0f, 1.0f,
             0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 1.0f,
            -0.5f,  0.5f, -0.5f,   0.0f, 1.0f, 1.0f,

            // Bottom face (Y = -0.5), color = Magenta
            -0.5f, -0.5f, -0.5f,   1.0f, 0.0f, 1.0f,
             0.5f, -0.5f, -0.5f,   1.0f, 0.0f, 1.0f,
             0.5f, -0.5f,  0.5f,   1.0f, 0.0f, 1.0f,

            -0.5f, -0.5f, -0.5f,   1.0f, 0.0f, 1.0f,
             0.5f, -0.5f,  0.5f,   1.0f, 0.0f, 1.0f,
            -0.5f, -0.5f,  0.5f,   1.0f, 0.0f, 1.0f
        };

        int vao = glGenVertexArrays();
        glBindVertexArray(vao);

        int vbo = glGenBuffers();
        glBindBuffer(GL_ARRAY_BUFFER, vbo);

        // Upload data
        try (var stack = stackPush()) {
            FloatBuffer buffer = stack.mallocFloat(vertices.length);
            buffer.put(vertices).flip();
            glBufferData(GL_ARRAY_BUFFER, buffer, GL_STATIC_DRAW);
        }

        // Position attribute layout(location = 0)
        glVertexAttribPointer(0, 3, GL_FLOAT, false, 6 * Float.BYTES, 0L);
        glEnableVertexAttribArray(0);

        // Color attribute layout(location = 1)
        glVertexAttribPointer(1, 3, GL_FLOAT, false, 6 * Float.BYTES, (3L * Float.BYTES));
        glEnableVertexAttribArray(1);

        glBindVertexArray(0);
        return vao;
    }

    private int createShaderProgram() {
        int vertexShader = compileShader(VERTEX_SHADER_SRC, GL_VERTEX_SHADER);
        int tessControlShader = compileShader(TESSELLATION_CONTROL_SHADER_SRC, GL_TESS_CONTROL_SHADER);
        int tessEvalShader = compileShader(TESSELLATION_EVALUATION_SHADER_SRC, GL_TESS_EVALUATION_SHADER);
        int geometryShader = compileShader(GEOMETRY_SHADER_SRC, GL_GEOMETRY_SHADER);
        int fragmentShader = compileShader(FRAGMENT_SHADER_SRC, GL_FRAGMENT_SHADER);

        int program = glCreateProgram();
        glAttachShader(program, vertexShader);
        glAttachShader(program, tessControlShader);
        glAttachShader(program, tessEvalShader);
        glAttachShader(program, geometryShader);
        glAttachShader(program, fragmentShader);

        glLinkProgram(program);

        // Check for linking errors
        int linked = glGetProgrami(program, GL_LINK_STATUS);
        if (linked == 0) {
            String log = glGetProgramInfoLog(program);
            throw new RuntimeException("Program link failed:\n" + log);
        }

        // Once linked, we can delete the individual shaders
        glDeleteShader(vertexShader);
        glDeleteShader(tessControlShader);
        glDeleteShader(tessEvalShader);
        glDeleteShader(geometryShader);
        glDeleteShader(fragmentShader);

        return program;
    }

    private int compileShader(String source, int type) {
        int shader = glCreateShader(type);
        glShaderSource(shader, source);
        glCompileShader(shader);

        // Check compile status
        int compiled = glGetShaderi(shader, GL_COMPILE_STATUS);
        if (compiled == 0) {
            String log = glGetShaderInfoLog(shader);
            throw new RuntimeException("Shader compile failed:\n" + log);
        }
        return shader;
    }

    // -- Simple 4x4 Matrix Helpers --
    private float[] makeTranslation(float x, float y, float z) {
        return new float[] {
            1, 0, 0, 0,
            0, 1, 0, 0,
            0, 0, 1, 0,
            x, y, z, 1
        };
    }

    // Rotation around the X axis
    private float[] makeRotateX(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new float[] {
            1,  0,  0, 0,
            0,  c, -s, 0,
            0,  s,  c, 0,
            0,  0,  0, 1
        };
    }

    // Rotation around the Y axis
    private float[] makeRotateY(float angle) {
        float c = (float) Math.cos(angle);
        float s = (float) Math.sin(angle);
        return new float[] {
            c,  0,  s, 0,
            0,  1,  0, 0,
           -s,  0,  c, 0,
            0,  0,  0, 1
        };
    }

    // Simple perspective projection matrix
    private float[] makePerspective(float fov, float aspect, float near, float far) {
        float tanHalfFovy = (float) Math.tan(fov / 2.0);
        float[] mat = new float[16];

        mat[0] = 1.0f / (aspect * tanHalfFovy);
        mat[5] = 1.0f / tanHalfFovy;
        mat[10] = -(far + near) / (far - near);
        mat[11] = -1.0f;
        mat[14] = -(2.0f * far * near) / (far - near);
        return mat;
    }

    // Multiply two 4x4 matrices (column-major)
    private float[] multiply(float[] A, float[] B) {
        float[] M = new float[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                float sum = 0;
                for (int i = 0; i < 4; i++) {
                    sum += A[i*4 + col] * B[row*4 + i];
                }
                M[row*4 + col] = sum;
            }
        }
        return transpose(M);  // or adjust indexing if you prefer row-major
    }

    // Transpose 4x4
    private float[] transpose(float[] m) {
        return new float[] {
            m[0], m[4], m[8],  m[12],
            m[1], m[5], m[9],  m[13],
            m[2], m[6], m[10], m[14],
            m[3], m[7], m[11], m[15]
        };
    }
}
