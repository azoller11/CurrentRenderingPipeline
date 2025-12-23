package animatedModel;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL13;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.util.vector.Matrix4f;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import entities.Entity;
import models.RawModel;
import models.TexturedModel;
import renderEngine.MasterRenderer;
import textures.ModelTexture;
import toolbox.Maths;

public class AnimatedRenderer {

	private AnimatedShader shader;

	
	public AnimatedRenderer(AnimatedShader shader, Matrix4f projectionMatrix) {
		this.shader = shader;
		shader.start();
		shader.loadProjectionMatrix(projectionMatrix);
		shader.stop();
	}
	
	public void render(Entity model) {
		prepareTexturedModel(model);
		prepareInstance(model);
		shader.loadBoneTransforms(model.getAnimatedModel().getBones());
		GL11.glDrawElements(GL11.GL_TRIANGLES, model.getRawModel().getVertexCount(), GL11.GL_UNSIGNED_INT, 0);
		unbindTexturedModel();
	}
	
	private void prepareTexturedModel (Entity model) {
		
		RawModel rawModel = model.getRawModel();
		GL30.glBindVertexArray(rawModel.getVaoID());
		GL20.glEnableVertexAttribArray(0);
		GL20.glEnableVertexAttribArray(1);
		GL20.glEnableVertexAttribArray(2);
		GL20.glEnableVertexAttribArray(3);
		GL20.glEnableVertexAttribArray(4);
		ModelTexture texture = model.getTexture();
		shader.loadNumberOfRows(texture.getNumberOfRows());
		if(texture.isHasTransparency()) {
			MasterRenderer.disableCulling();
		}
		shader.loadFakeLightingVariable(texture.isUseFakeLighting());
		shader.loadShineVariables(texture.getShineDamper(), texture.getReflectivity());
		GL13.glActiveTexture(GL13.GL_TEXTURE0);
		GL11.glBindTexture(GL11.GL_TEXTURE_2D, model.getTexture().getID());
		
	}
	
	private void prepareInstance(Entity model) {
		
		Matrix4f transfomationMatrix = Maths.createTransformationMatrix(model.getPosition(), model.getRotX(), model.getRotY(), model.getRotZ(), model.getScale());
		shader.loadTransformationMatrix(transfomationMatrix);
		shader.loadOffset(0, 0);
		
	}
	
	 public void setTransformationMatrix(Entity animatedEntity) {
	        Matrix4f transformationMatrix = Maths.createTransformationMatrix(animatedEntity.getPosition(), 
	                animatedEntity.getRotX(), animatedEntity.getRotY(), animatedEntity.getRotZ(), animatedEntity.getScale());
	        shader.loadTransformationMatrix(transformationMatrix);
	    }
	
	
	private void unbindTexturedModel() {
		
		MasterRenderer.enableCulling();
		GL20.glDisableVertexAttribArray(0);
		GL20.glDisableVertexAttribArray(1);
		GL20.glDisableVertexAttribArray(2);
		GL20.glDisableVertexAttribArray(3);
		GL20.glDisableVertexAttribArray(4);
		GL30.glBindVertexArray(0);
		
	}
	
}