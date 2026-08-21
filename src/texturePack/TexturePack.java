package texturePack;

import entities.TexturedModel;
import materialLoader.Material;

public class TexturePack {

	private final String name;
	private final Material material;

	public TexturePack(String name, Material material) {
		this.name = name;
		this.material = material;
	}

	public String getName() {
		return name;
	}

	public Material getMaterial() {
		return material;
	}

	/**
	 * Assigns this pack's PBR maps onto a TexturedModel, both on the new
	 * Material field and on TexturedModel's legacy int fields (which is what
	 * MasterRenderer currently reads when binding textures for rendering).
	 */
	public void applyTo(TexturedModel model) {
		model.setMaterial(material);
		model.setTextureId(material.getTextureId());
		model.setNormalMapId(material.getNormalMapId());
		model.setHeighMapId(material.getHeighMapId());
		model.setMetallicMap(material.getMetallicMap());
		model.setRoughnessMap(material.getRoughnessMap());
		model.setAoMap(material.getAoMap());
	}

}
