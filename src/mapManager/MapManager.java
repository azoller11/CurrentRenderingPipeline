package mapManager;

import java.util.List;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;

import org.json.JSONArray;
import org.json.JSONObject;

public class MapManager {
	
	
	private final String mapFolder = "/res/maps/";
	
	
	public Map createMap(String mapName) {
		return null;
	}
	
	
	public List<String> availibleMaps() {
		return new ArrayList<String>();
	}
	
	
	public Map loadMap(String mapName) {
	    try {
	        Path path = Path.of(mapFolder + mapName + ".json");
	        String content = Files.readString(path);

	        JSONObject json = new JSONObject(content);
	        return Map.fromJSON(json);

	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	    return null;
	}

	
	
	public Map offLoadMap(String mapName) {
		return null;
	}
	
	
	public void saveMap(Map map) {
	        try {
	            JSONObject json = map.toJSON();
	            Path path = Path.of(mapFolder + map.getName() + ".json");
	
	        Files.createDirectories(path.getParent());
	        Files.writeString(path, json.toString(4)); // pretty print
	
	    } catch (IOException e) {
	        e.printStackTrace();
	    }
	}
	  
	

}
