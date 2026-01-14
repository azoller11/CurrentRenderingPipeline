package animatedModel;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import entities.Entity;
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

    //private final TexturedModel model;
    private Entity entity;

    // Immutable script definition
    private final List<Step> steps = new ArrayList<>();

    // Runtime execution state
    private Queue<Step> runQueue;
    private Step currentStep;
    private boolean running = false;
    
    private boolean stepBegan = false;

    public AnimationScript(Entity entity) {
        this.entity = entity;
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

    // ---------- Control ----------

    public void start() {
        if (steps.isEmpty()) return;

        runQueue = new ArrayDeque<>(steps);
        currentStep = null;
        running = true;

        advance();
    }

    public void stop() {
        running = false;
        currentStep = null;
        runQueue = null;

        entity.playAnimation(-1, false);
    }

    public boolean isRunning() {
        return running;
    }

    // ---------- Update ----------

    public void update(float deltaTime) {
        if (!running || currentStep == null) return;

        AnimationElement anim =
        	    entity.getActiveAnimation();

        // ✅ If animation exists, run it normally
        if (anim != null) {
            stepBegan = true;                 // we have observed it playing
            anim.incAnimationTime(deltaTime);

            if (!anim.isFinished()) return;

            // step finished
            if (currentStep.callback != null) currentStep.callback.run();
            if (currentStep.loop) return;     // looping steps intentionally never advance

            // done?
            if (runQueue == null || runQueue.isEmpty()) {
                finishSequence();
            } else {
                advance();
            }
            return;
        }

        // ✅ anim == null
        // If we already saw this step playing, and now it's gone, treat as finished.
        if (stepBegan) {
            if (currentStep.callback != null) currentStep.callback.run();
            if (currentStep.loop) return;

            if (runQueue == null || runQueue.isEmpty()) {
                finishSequence();
            } else {
                advance();
            }
        }

        // else: anim is null but step hasn't started yet → just wait
    }



    // ---------- Internal ----------

    private void advance() {
        currentStep = runQueue.poll();

        if (currentStep == null) {
            finishSequence();
            return;
        }

        stepBegan = false; // ✅ new step, haven't observed it playing yet
        entity.playAnimation(currentStep.animationIndex, currentStep.loop);
    }

    private void finishSequence() {
        running = false;
        currentStep = null;
        runQueue = null;
        stepBegan = false;

        entity.playAnimation(-1, false);
    }

    private void finish() {
        running = false;
        currentStep = null;
        runQueue = null;

        entity.playAnimation(-1, false);
    }
}
