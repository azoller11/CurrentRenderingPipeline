package particles;

import org.lwjgl.util.vector.Vector2f;
import org.lwjgl.util.vector.Vector3f;

import entities.Camera;
import entities.Player;

//single particle
public class Particle {

	private Vector3f position;
	private Vector3f velocity;
	private float gravityEffect;
	private float lifeLength;
	private float rotation;
	private float scale;
	
	private float initialScale;
	private float scaleGrowth = 0f; // units per second

	private ParticleTexture texture;

	private Vector2f texOffset1 = new Vector2f();
	private Vector2f texOffset2 = new Vector2f();
	private float blend;

	private float elapsedTime = 0;
	private float distance;

	private Vector3f reusableChange = new Vector3f();

	private boolean alive = false;
	
	public Particle() {}

	public Particle(ParticleTexture texture, Vector3f position, Vector3f velocity, float gravityEffect,
			float lifeLength, float rotation, float scale) {
			alive = true;
			this.position = position;
			this.velocity = velocity;
			this.gravityEffect = gravityEffect;
			this.lifeLength = lifeLength;
			this.rotation = rotation;
			this.scale = scale;
			this.texture = texture;
			ParticleMaster.addParticle(this);
	}

	public void setActive(ParticleTexture texture, Vector3f position, Vector3f velocity,
	        float gravityEffect, float lifeLength, float rotation, float scale) {

	    alive = true;
	    this.position = position;
	    this.velocity = velocity;
	    this.gravityEffect = gravityEffect;
	    this.lifeLength = lifeLength;
	    this.rotation = rotation;

	    this.initialScale = scale;
	    this.scale = scale;
	    this.scaleGrowth = 0f;

	    this.texture = texture;
	    this.elapsedTime = 0f;

	    ParticleMaster.addParticle(this);
	}
	
	public void setInactive() {
	    alive = false;
	    position = null;
	    velocity = null;
	    texture = null;
	    //ParticleMaster.releaseParticle(this);
	}

	public float getDistance() {
		return distance;
	}

	public Vector2f getTexOffset1() {
		return texOffset1;
	}

	public Vector2f getTexOffset2() {
		return texOffset2;
	}

	public float getBlend() {
		return blend;
	}

	public ParticleTexture getTexture() {
		return texture;
	}

	public Vector3f getPosition() {
		return position;
	}

	public float getRotation() {
		return rotation;
	}

	public float getScale() {
		return scale;
	}
	
	public void setScaleGrowth(float scaleGrowth) {
	    this.scaleGrowth = scaleGrowth;
	}
	

	protected boolean update(Camera camera, float delta) {
	    velocity.y += -50 * gravityEffect * delta;

	    reusableChange.set(velocity);
	    reusableChange.scale(delta);
	    Vector3f.add(reusableChange, position, position);

	    // 🔥 SCALE GROWTH
	    scale = initialScale + (elapsedTime * scaleGrowth);

	    distance = Vector3f.sub(
	        new Vector3f(camera.getPosition().x(), camera.getPosition().y(), camera.getPosition().z()),
	        position,
	        null
	    ).lengthSquared();

	    if (distance > 500 * 500) {
	        alive = false;
	        return false;
	    }

	    updateTextureCoordInfo();
	    elapsedTime += delta;
	    return elapsedTime < lifeLength;
	}


	private void updateTextureCoordInfo() {
		float lifeFactor = elapsedTime / lifeLength;
		int stageCount = texture.getNumberOfRows() * texture.getNumberOfRows();
		float atlasProgression = lifeFactor * stageCount;
		int index1 = (int) Math.floor(atlasProgression);
		int index2 = index1 < stageCount - 1 ? index1 + 1 : index1;
		this.blend = atlasProgression % 1;
		setTextureOffset(texOffset1, index1);
		setTextureOffset(texOffset2, index2);
	}

	private void setTextureOffset(Vector2f offset, int index) {
		int column = index % texture.getNumberOfRows();
		int row = index / texture.getNumberOfRows();
		offset.x = (float) column / texture.getNumberOfRows();
		offset.y = (float) row / texture.getNumberOfRows();
	}
}
