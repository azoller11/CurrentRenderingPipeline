package text;

import java.util.List;
import java.util.ArrayList;

import text.TextRenderer.TextAlignment;

public class TextRendererMaster {
	
	
	
	public static List<RenderText> toRenderText = new ArrayList<>();
	
	
	public static void renderAllText(TextRenderer textRenderer) {
		for (RenderText t : toRenderText) {
			textRenderer.renderText(t.getText(), t.getxNorm(), t.getyNorm(), t.getScale(), t.getMaxWidthNorm(), t.getAlignment());
		}
		toRenderText.clear();
	}
	
	public static void renderText(String text, float xNorm, float yNorm, float scale, float maxWidthNorm,
			TextAlignment alignment) {
		RenderText rt = new RenderText(text,  xNorm,  yNorm,  scale,  maxWidthNorm, alignment);
		
		if (toRenderText == null) {
			toRenderText = new ArrayList<>();
			toRenderText.add(rt);
		} else {
			toRenderText.add(rt);
		}
			
	}
	
	
	
	
	 public static class RenderText{
	            String text;
	            float xNorm; 
	            float yNorm;
	            float scale;
	            float maxWidthNorm;
	            TextAlignment alignment;
				public RenderText(String text, float xNorm, float yNorm, float scale, float maxWidthNorm,
						TextAlignment alignment) {
					super();
					this.text = text;
					this.xNorm = xNorm;
					this.yNorm = yNorm;
					this.scale = scale;
					this.maxWidthNorm = maxWidthNorm;
					this.alignment = alignment;
				}
				public String getText() {
					return text;
				}
				public void setText(String text) {
					this.text = text;
				}
				public float getxNorm() {
					return xNorm;
				}
				public void setxNorm(float xNorm) {
					this.xNorm = xNorm;
				}
				public float getyNorm() {
					return yNorm;
				}
				public void setyNorm(float yNorm) {
					this.yNorm = yNorm;
				}
				public float getScale() {
					return scale;
				}
				public void setScale(float scale) {
					this.scale = scale;
				}
				public float getMaxWidthNorm() {
					return maxWidthNorm;
				}
				public void setMaxWidthNorm(float maxWidthNorm) {
					this.maxWidthNorm = maxWidthNorm;
				}
				public TextAlignment getAlignment() {
					return alignment;
				}
				public void setAlignment(TextAlignment alignment) {
					this.alignment = alignment;
				}
				
				
	            
	            
	 }
	

}
