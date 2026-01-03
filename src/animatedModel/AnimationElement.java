package animatedModel;

public class AnimationElement {

    private final int animationIndex;
    private final String name;
    private final float durationSeconds;

    private float timeSeconds = 0.0f;
    private float speed = 60.0f; // 1.0 = normal speed
    private boolean loop = true;
    private boolean finished = false;

    public AnimationElement(int animationIndex, String name, float durationSeconds) {
        this.animationIndex = animationIndex;
        this.name = name;
        this.durationSeconds = durationSeconds;
    }

    public void update(float deltaTime) {
        if (finished) return;

        timeSeconds += deltaTime * speed;

        if (timeSeconds >= durationSeconds) {
            if (loop) {
                timeSeconds %= durationSeconds;
            } else {
                timeSeconds = durationSeconds;
                finished = true;
            }
        }
    }

    public void reset() {
        timeSeconds = 0.0f;
        finished = false;
    }

    // ---------------- Getters ----------------

    public int getAnimationIndex() {
        return animationIndex;
    }

    public String getName() {
        return name;
    }

    public float getTimeSeconds() {
        return timeSeconds;
    }

    public float getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isFinished() {
        return finished;
    }

    public void setLoop(boolean loop) {
        this.loop = loop;
    }

    public void setSpeed(float speed) {
        this.speed = speed;
    }
}
