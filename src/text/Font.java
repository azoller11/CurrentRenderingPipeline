package text;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashMap;
import java.util.Map;

import loaders.TextureLoader;

public class Font {
    private int textureID;
    private Map<Integer, Character> characters = new HashMap<>();
    private int lineHeight;
    private int base;
    private int atlasWidth;
    private int atlasHeight;

    public Font(String fntFile, String pngFile) {
        this.textureID = TextureLoader.loadExplicitTexture(pngFile);
        loadFntFile(fntFile);
    }

    private void loadFntFile(String fntFile) {
        try (BufferedReader reader = new BufferedReader(new FileReader(fntFile))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("char ")) {
                    parseCharLine(line);
                } else if (line.startsWith("common ")) {
                    parseCommonLine(line);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void parseCommonLine(String line) {
        String[] parts = line.split(" +");
        for (String part : parts) {
            if (part.startsWith("lineHeight=")) {
                lineHeight = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("base=")) {
                base = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("scaleW=")) {
                atlasWidth = Integer.parseInt(part.split("=")[1]);
            } else if (part.startsWith("scaleH=")) {
                atlasHeight = Integer.parseInt(part.split("=")[1]);
            }
        }
    }

    private void parseCharLine(String line) {
        String[] parts = line.split(" +");
        Character ch = new Character();
        for (String part : parts) {
            String[] keyValue = part.split("=");
            if (keyValue.length == 2) {
                switch (keyValue[0]) {
                    case "id": ch.id = Integer.parseInt(keyValue[1]); break;
                    case "x": ch.x = Integer.parseInt(keyValue[1]); break;
                    case "y": ch.y = Integer.parseInt(keyValue[1]); break;
                    case "width": ch.width = Integer.parseInt(keyValue[1]); break;
                    case "height": ch.height = Integer.parseInt(keyValue[1]); break;
                    case "xoffset": ch.xoffset = Integer.parseInt(keyValue[1]); break;
                    case "yoffset": ch.yoffset = Integer.parseInt(keyValue[1]); break;
                    case "xadvance": ch.xadvance = Integer.parseInt(keyValue[1]); break;
                }
            }
        }
        characters.put(ch.id, ch);
    }

    public static class Character {
        public int id, x, y, width, height, xoffset, yoffset, xadvance;
    }

    public int getTextureID() { return textureID; }
    public Character getCharacter(int id) { return characters.get(id); }
    public int getLineHeight() { return lineHeight; }
    public int getBase() { return base; }
    public int getAtlasWidth() { return atlasWidth; }
    public int getAtlasHeight() { return atlasHeight; }
}