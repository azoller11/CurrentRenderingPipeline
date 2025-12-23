package decals;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class DecalManager {

    private static final List<Decal> decals = new ArrayList<>();

    public static final int MAX_BULLETS = 1000;

    public static void add(Decal decal) {
        if (decal.getDecalType() == Decal.DecalType.BULLET) {
            enforceBulletLimit();
        }

        decals.add(decal);
    }

    private static void enforceBulletLimit() {
        int bulletCount = 0;

        // Count existing bullets
        for (Decal d : decals) {
            if (d.getDecalType() == Decal.DecalType.BULLET) {
                bulletCount++;
            }
        }

        // Remove oldest bullets until under limit
        if (bulletCount >= MAX_BULLETS) {
            Iterator<Decal> it = decals.iterator();
            while (it.hasNext()) {
                Decal d = it.next();
                if (d.getDecalType() == Decal.DecalType.BULLET) {
                    it.remove(); // removes oldest BULLET
                    break;
                }
            }
        }
    }

    public static List<Decal> getDecals() {
        return decals;
    }
}
