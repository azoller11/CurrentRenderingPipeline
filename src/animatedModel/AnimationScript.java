package animatedModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import entities.TexturedModel;

public class AnimationScript {

    private static class Step {
        int animationIndex;
        boolean loop;
        Runnable callback;

        Step(int index, boolean loop, Runnable callback) {
            this.animationIndex = index;
            this.loop = loop;
            this.callback = callback;
        }
    }

    private final TexturedModel model;

    // 🔒 Immutable script definition
    private final List<Step> steps = new ArrayList<>();

    // ▶ Runtime execution state
    private Queue<Step> runQueue;
    private Step currentStep;
    private boolean running = false;

    private Runnable onComplete;

    public AnimationScript(TexturedModel model) {
        this.model = model;
    }

    // ---------- Builder API ----------

    public AnimationScript play(int animationIndex, boolean loop) {
        steps.add(new Step(animationIndex, loop, null));
        return this;
    }

    public AnimationScript then(int animationIndex, boolean loop) {
        return play(animationIndex, loop);
    }

    public AnimationScript then(int animationIndex, boolean loop, Runnable callback) {
        steps.add(new Step(animationIndex, loop, callback));
        return this;
    }

    public AnimationScript onComplete(Runnable callback) {
        this.onComplete = callback;
        return this;
    }

    // ---------- Control ----------

    public void start() {
        if (steps.isEmpty()) return;

        // 🔁 Reset execution state
        runQueue = new ArrayDeque<>(steps);
        currentStep = null;
        running = true;

        advance();
    }

    public void stop() {
        running = false;
        currentStep = null;
        runQueue = null;
        model.playAnimation(-1, false);
    }

    public boolean isRunning() {
        return running;
    }

    // ---------- Update ----------

    public void update(float deltaTime) {
        if (!running || currentStep == null) return;

        var anim = model.getActiveAnimation();

        // Animation stopped externally
        if (anim == null) {
            finishSequence();
            return;
        }

        // Finished animation
        if (anim.isFinished()) {

            if (currentStep.callback != null) {
                currentStep.callback.run();
            }

            // Looping step intentionally never advances
            if (currentStep.loop) return;

            advance();
        }
    }

    // ---------- Internal ----------

    private void advance() {
        currentStep = runQueue.poll();

        if (currentStep == null) {
            finishSequence();
            return;
        }

        model.playAnimation(currentStep.animationIndex, currentStep.loop);
    }

    private void finishSequence() {
        running = false;
        currentStep = null;
        runQueue = null;

        model.playAnimation(-1, false);

        if (onComplete != null) {
            onComplete.run();
        }
    }
}
