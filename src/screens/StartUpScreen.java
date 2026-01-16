package screens;

import gui.GuiTexture;
import gui.TextureRenderer;
import loaders.TextureLoader;
import org.joml.Matrix4f;

import static org.lwjgl.opengl.GL11.*;

public class StartUpScreen {

    private GuiTexture loadingScreen;

    public void render(
            TextureRenderer textureRenderer,
            int width,
            int height
    ) {
        if (loadingScreen == null) {
            loadingScreen = new GuiTexture(
                    TextureLoader.loadExplicitTexture("ElkEngine.png"),
                    0, 0, width, height
            );
            textureRenderer.addTexture(loadingScreen);
        }

        glClear(GL_COLOR_BUFFER_BIT | GL_DEPTH_BUFFER_BIT);
        glViewport(0, 0, width, height);

        glEnable(GL_BLEND);
        glBlendFunc(GL_SRC_ALPHA, GL_ONE_MINUS_SRC_ALPHA);

        textureRenderer.render(
                new Matrix4f().ortho2D(0, width, 0, height),
                new Matrix4f().identity(),
                0,
                0
        );
    }

    public GuiTexture getLoadingScreen() {
        return loadingScreen;
    }
}
