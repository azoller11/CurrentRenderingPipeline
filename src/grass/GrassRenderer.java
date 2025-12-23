package grass;

import org.joml.Matrix4f;
import org.joml.Vector2f;
import org.joml.Vector3f;

import entities.Camera;
import entities.Light;
import shaders.ShaderProgram;
import shadows.ShadowRenderer;

import java.util.List;

public class GrassRenderer {
    private ShaderProgram shaderProgram;
    private GrassGenerator grassGenerator;
    
    /**
     * Constructs the grass renderer using a shader program and a list of Grass objects.
     *
     * @param shaderProgram The shader program with grass vertex, geometry, and fragment shaders.
     * @param grassList     The list of grass instances to render.
     */
    public GrassRenderer(List<Grass> grassList) {
        this.shaderProgram = new ShaderProgram("src/grass/vertex.glsl",null,null,"src/grass/geometry.glsl","src/grass/fragment.glsl");
        this.grassGenerator = new GrassGenerator(grassList);
    }
    
    /**
     * Renders the grass blades.
     *
     * @param bladeLength  The height of each grass blade.
     * @param bladeWidth   The base width of each grass blade.
     * @param windStrength The amplitude of the wind effect.
     * @param windDirection The normalized wind direction as a Vector2f.
     * @param time         The current time for animating wind.
     */
    public void render(float bladeLength, float bladeWidth, float windStrength, Vector2f windDirection, float time,Matrix4f projection,  Matrix4f view, List<Light> lights, int shadowMap, Camera camera) {
    	Matrix4f model = new Matrix4f().identity(); // Or a custom model transform if needed
    	shaderProgram.bind();
    	//shaderProgram.setUniformMat4("model", model);
    	shaderProgram.setUniformMat4("view", view);
    	shaderProgram.setUniformMat4("projection", projection);

    	// Set the grass-specific uniforms.
    	shaderProgram.setUniform1f("bladeLength", bladeLength);
    	shaderProgram.setUniform1f("bladeWidth", bladeWidth);
    	shaderProgram.setUniform1f("windStrength", windStrength);
    	shaderProgram.setUniform2f("windDirection", windDirection.x, windDirection.y);
    	shaderProgram.setUniform1f("time", time);
    	
    	shaderProgram.setUniformLights("lights", lights); 
    	shaderProgram.setUniform1i("shadowMap", shadowMap); // texture unit 0, for example
    	shaderProgram.setUniformMat4("lightSpaceMatrix", ShadowRenderer.createLightSpaceMatrix(lights.get(0), camera));
         Vector3f lightDir = new Vector3f(lights.get(0).getPosition()).normalize();
         lightDir.z = -lightDir.z;
         lightDir.x = -lightDir.x;
         shaderProgram.setUniform3f("directionalLightDir", lightDir);
         
         
         shaderProgram.setUniform1f("roughness", 0.5f); // Adjust as desired.
         shaderProgram.setUniform1f("metallic", 0.0f);  // Typically grass is non-metallic.
         shaderProgram.setUniform1f("ao", 1.0f);
    	

    	grassGenerator.render();
    	shaderProgram.unbind();


    }
    
    /**
     * Releases OpenGL resources.
     */
    public void cleanup() {
        grassGenerator.cleanup();
        shaderProgram.destroy();
    }
}
