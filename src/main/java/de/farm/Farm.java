package de.farm;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

/**
 * kind: DARK = dunkle Spawn-Halle (natürliches Spawnen, kein Spawner), HALL = Villager-Handelshalle,
 * COMPOST = Komposter-Station (Knochenmehl), ROCKET = Raketen-Herstellungsstation, PLANNED = noch nicht gebaut.
 * status: ehrlicher Entwicklungsstand. desc: Kurzbeschreibung, Teile mit "|" getrennt.
 */
record Farm(String name, Material icon, int kind, EntityType mob, Villager.Profession job, Material station,
            String status, String desc) {
    static final int DARK = 0, HALL = 1, COMPOST = 2, ROCKET = 3, PLANNED = 4;
}
