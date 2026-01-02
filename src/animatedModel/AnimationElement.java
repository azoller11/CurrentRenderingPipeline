package animatedModel;

public class AnimationElement {

    private int animationIndex;
    private float animationTime;
    private float blendedTime;
    private float animationSpeed = 60.01f; // Add speed multiplier
    
    public AnimationElement(int animationIndex, float animationTime) {
        super();
        this.animationIndex = animationIndex;
        this.animationTime = animationTime;
    }
    
    public void incAnimationTime(float deltaTime) { // Change to accept deltaTime
        animationTime += deltaTime * animationSpeed;
    }
    
    public void incBlendedTime(float deltaTime) { // Change to accept deltaTime
        blendedTime += deltaTime;
    }
    
    public void incBlendedTime(float deltaTime, int inc) {
        blendedTime += deltaTime * inc;
    }
    
    public void resetAnimationTime() {
        animationTime = 0;
    }
    
    public int getAnimationIndex() {
        return animationIndex;
    }
    
    public void setAnimationIndex(int animationIndex) {
        this.animationIndex = animationIndex;
    }
    
    public float getAnimationTime() {
        return animationTime;
    }
    
    public void setAnimationTime(float animationTime) {
        this.animationTime = animationTime;
    }
    
    public float getBlendedTime() {
        return blendedTime;
    }
    
    public void setBlendedTime(float blendedTime) {
        this.blendedTime = blendedTime;
    }
    
    public float getAnimationSpeed() {
        return animationSpeed;
    }
    
    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
    }
}