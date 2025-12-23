package guiManager;

import gui.TextureRenderer;
import renderer.MasterRenderer;
import settings.EngineSettings;
import text.TextRenderer;
import text.TextRenderer.TextAlignment;

public class SelectedLightDebug {

	public void update(TextureRenderer textureRender, TextRenderer textRenderer, MasterRenderer masterRenderer) {
		if (EngineSettings.SelectedLight != null && EngineSettings.pause) {
			

        	if (EngineSettings.OpenLight != null) {
                textRenderer.renderText(""+EngineSettings.OpenLight.getId(), 0, 120, 0.25f, masterRenderer.getFlatProjection(), masterRenderer.getScreenWidth(), TextAlignment.LEFT);

                textRenderer.renderText(""+EngineSettings.OpenLight.getPosition(), 0, 140, 0.25f, masterRenderer.getFlatProjection(), masterRenderer.getScreenWidth(), TextAlignment.LEFT);

        	}
			
		}
	}
	
	
}
