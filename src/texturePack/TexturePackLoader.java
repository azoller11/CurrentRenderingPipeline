package texturePack;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import loaders.TextureLoader;
import materialLoader.Material;

/**
 * Discovers and loads texture packs from res/texturePacks/<packName>/.
 * Each pack folder is expected to contain PBR maps (albedo, normal,
 * metallic, roughness, ao, height) named with any of the common suffix
 * conventions used across the packs already in res/ (e.g. "diff"/"albedo",
 * "nor_gl"/"normal", "rough", "ao", "disp"/"height").
 */
public class TexturePackLoader {

	private static final String PACKS_DIR = "texturePacks/"; // relative to res/, TextureLoader prepends "res/"
	private static final File PACKS_ROOT = new File("res/" + PACKS_DIR);

	private static final Map<String, TexturePack> cache = new HashMap<>();

	private static final String[] ALBEDO_TOKENS = { "diff", "albedo", "diffuse", "basecolor", "base_color" };
	private static final String[] NORMAL_GL_TOKENS = { "nor_gl", "normal-ogl", "normal_ogl", "normalgl", "normal", "nrm", "nor", "ddn" };
	private static final String[] NORMAL_DX_TOKENS = { "nor_dx", "normal-dx", "normaldx" };
	private static final String[] METALLIC_TOKENS = { "metallic", "metalness", "mtl" };
	private static final String[] ROUGHNESS_TOKENS = { "roughness", "rough" };
	private static final String[] AO_TOKENS = { "ambientocclusion", "occlusion", "_ao_", "_ao.", "-ao-", "-ao.", "ao" };
	private static final String[] HEIGHT_TOKENS = { "displacement", "disp", "height", "dip", "bump" };

	/**
	 * Lists the names of the texture packs found under res/texturePacks/.
	 */
	public static List<String> listAvailablePacks() {
		List<String> packs = new ArrayList<>();
		File[] dirs = PACKS_ROOT.listFiles(File::isDirectory);
		if (dirs != null) {
			for (File dir : dirs) {
				packs.add(dir.getName());
			}
		}
		return packs;
	}

	/**
	 * Returns a cached TexturePack, loading it from disk the first time it's requested.
	 */
	public static TexturePack get(String packName) {
		return cache.computeIfAbsent(packName, TexturePackLoader::load);
	}

	/**
	 * Scans res/texturePacks/<packName>/ for PBR maps and uploads them via
	 * TextureLoader.loadTexture, returning a TexturePack wrapping the result.
	 */
	public static TexturePack load(String packName) {
		File packDir = new File(PACKS_ROOT, packName);
		String[] filenames = packDir.list();
		if (filenames == null) {
			throw new RuntimeException("Texture pack not found: " + packDir.getPath());
		}

		Material material = new Material();

		String albedo = findMap(filenames, ALBEDO_TOKENS);
		if (albedo != null) {
			material.setTextureId(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + albedo));
		}

		String normal = findMap(filenames, NORMAL_GL_TOKENS);
		if (normal == null) {
			normal = findMap(filenames, NORMAL_DX_TOKENS);
		}
		if (normal != null) {
			material.setNormalMapId(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + normal));
		}

		String metallic = findMap(filenames, METALLIC_TOKENS);
		if (metallic != null) {
			material.setMetallicMap(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + metallic));
		}

		String roughness = findMap(filenames, ROUGHNESS_TOKENS);
		if (roughness != null) {
			material.setRoughnessMap(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + roughness));
		}

		String ao = findMap(filenames, AO_TOKENS);
		if (ao != null) {
			material.setAoMap(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + ao));
		}

		String height = findMap(filenames, HEIGHT_TOKENS);
		if (height != null) {
			material.setHeighMapId(TextureLoader.loadTexture(PACKS_DIR + packName + "/" + height));
		}

		return new TexturePack(packName, material);
	}

	/**
	 * Returns the first filename matching any token, checking tokens in
	 * priority order (most specific first) across all filenames before
	 * moving on to the next token.
	 */
	private static String findMap(String[] filenames, String[] tokensInPriorityOrder) {
		for (String token : tokensInPriorityOrder) {
			for (String filename : filenames) {
				if (isImage(filename) && filename.toLowerCase(Locale.ROOT).contains(token)) {
					return filename;
				}
			}
		}
		return null;
	}

	private static boolean isImage(String filename) {
		String lower = filename.toLowerCase(Locale.ROOT);
		return lower.endsWith(".png") || lower.endsWith(".jpg") || lower.endsWith(".jpeg")
				|| lower.endsWith(".tga") || lower.endsWith(".bmp");
	}

}
