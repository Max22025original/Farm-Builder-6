package de.farm;

import org.bukkit.Material;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Villager;

import java.util.List;

/** Katalog. Nur Farmen mit echtem Aufbau sind "implementiert"; alles andere ist offen als "geplant" markiert. */
final class Farms {
    private Farms() {}

    static final String IMPL = "implementiert (nicht kompiliert, nicht im Spiel getestet)";
    static final String PLAN = "geplant - noch nicht gebaut";

    static final Theme DARKHALL = new Theme(Material.DEEPSLATE_BRICKS, Material.DEEPSLATE_BRICKS, 3, Material.BLACKSTONE, Material.POLISHED_DEEPSLATE, false);
    static final Theme NONE = new Theme(Material.STONE_BRICKS, Material.STONE_BRICKS, 3, Material.STONE_BRICKS, Material.STONE_BRICKS, false);

    private static Farm dark(String n, Material icon, EntityType mob, String drop) {
        return new Farm(n, icon, Farm.DARK, mob, null, null, IMPL,
            "Dunkle, geschlossene Halle mit mehreren Ebenen. Ziel-Drops: " + drop
                + "|Natürliches Spawnen, kein Spawner. Spieler muss ca. 30-100 Blöcke entfernt sein, Chunks geladen."
                + "|Das Plugin lässt nur diese Mob-Art spawnen, tötet sie und legt die Drops in die Kiste vor der Halle (Abweichung von reinem Vanilla)."
                + "|Mobcap und Spawnrate nicht geprüft.");
    }

    private static Farm hall(String n, Material station, Villager.Profession job) {
        return new Farm(n, station, Farm.HALL, null, job, station, IMPL,
            "Zellen auf beiden Seiten eines Gangs. Villager mit Beruf, Arbeitsblock direkt dahinter."
                + "|Handel durch ein Eisengitter. Eingang an der Westseite."
                + "|Villager-Zahl ist per config (max-villagers) begrenzt; übrige Zellen bleiben leer.");
    }

    private static Farm plan(String n, Material icon, String hint) {
        return new Farm(n, icon, Farm.PLANNED, null, null, null, PLAN, hint);
    }

    static final List<Category> CATEGORIES = List.of(
        new Category("Raketen-Kette", Material.FIREWORK_ROCKET, NONE, List.of(
            new Farm("Raketen-Herstellungsstation", Material.CRAFTING_TABLE, Farm.ROCKET, null, null, null, IMPL,
                "Werkbank, getrennte Kisten für Papier und Schießpulver, Ausgabekiste, Hinweisschilder."
                    + "|Handarbeit: Crafting von Raketen ist in Vanilla nicht einfach vollautomatisch."
                    + "|Mega/Riesig bauen mehrere Stationen."),
            plan("Zuckerrohrfarm", Material.SUGAR_CANE, "Beobachter + Kolben + Sammelsystem (Design offen)")
        )),
        new Category("Knochenmehl", Material.BONE_MEAL, NONE, List.of(
            new Farm("Komposter-Station", Material.COMPOSTER, Farm.COMPOST, null, null, null, IMPL,
                "Eingangskiste -> Trichter -> Komposter -> Trichter -> Ausgabekiste (Knochenmehl)."
                    + "|Braucht Pflanzenreste als Eingabe (z. B. Zuckerrohr, Kürbis, Weizen). Nur Kompostierbares einlegen."
                    + "|Mega/Riesig bauen viele Module parallel.")
        )),
        new Category("Monster", Material.CREEPER_HEAD, DARKHALL, List.of(
            dark("Creeper-Farm (Schießpulver)", Material.GUNPOWDER, EntityType.CREEPER, "Schießpulver"),
            dark("Skelett-Farm (Knochen)", Material.BONE, EntityType.SKELETON, "Knochen, Pfeile"),
            dark("Zombie-Farm", Material.ROTTEN_FLESH, EntityType.ZOMBIE, "Faulfleisch"),
            dark("Spinnen-Farm", Material.STRING, EntityType.SPIDER, "Fäden, Spinnenaugen"),
            dark("Enderman-Farm", Material.ENDER_PEARL, EntityType.ENDERMAN, "Enderperlen"),
            dark("Hexen-Farm", Material.REDSTONE, EntityType.WITCH, "Redstone u. a.")
        )),
        new Category("Tiere", Material.LEAD, NONE, List.of(
            plan("Kuhfarm", Material.BEEF, "Zucht mit Weizen (manuell), Ernte/Sammlung - Design offen"),
            plan("Wollfarm", Material.WHITE_WOOL, "Schafe, Spender mit Scheren - Design offen"),
            plan("Eierfarm", Material.EGG, "Hühner, Sammeltrichter - Design offen"),
            plan("Honigfarm", Material.HONEYCOMB, "Bienen, Beuten, Sammlung - Design offen")
        )),
        new Category("Erze & Eisen", Material.IRON_INGOT, NONE, List.of(
            plan("Eisenfarm", Material.IRON_INGOT, "Villager-Golem-Farm - Design offen"),
            plan("Goldfarm", Material.GOLD_INGOT, "Natürliche Spawns - Design offen"),
            plan("Lavafarm", Material.LAVA_BUCKET, "Tropfstein + Kessel - Design offen")
        )),
        new Category("Nahrung", Material.WHEAT, NONE, List.of(
            plan("Weizenfarm", Material.WHEAT, "Design offen"),
            plan("Karotten-/Kartoffelfarm (Villager)", Material.CARROT, "Design offen"),
            plan("Melonen-/Kürbisfarm", Material.MELON_SLICE, "Beobachter/Kolben - Design offen"),
            plan("Gegrilltes Fleisch", Material.COOKED_BEEF, "Tierzucht mit Lava/Ofen - Design offen")
        )),
        new Category("Holz & Pflanzen", Material.OAK_SAPLING, NONE, List.of(
            plan("Holzfarm", Material.OAK_LOG, "Design offen"),
            plan("Bambusfarm", Material.BAMBOO, "Design offen"),
            plan("Kaktusfarm", Material.CACTUS, "Design offen"),
            plan("Pilzfarm", Material.RED_MUSHROOM, "Design offen"),
            plan("Netherwarzenfarm", Material.NETHER_WART, "Design offen"),
            plan("Seetangfarm", Material.KELP, "Design offen")
        )),
        new Category("Steine", Material.COBBLESTONE, NONE, List.of(
            plan("Bruchsteinfarm", Material.COBBLESTONE, "Generator + Kolben - Design offen")
        )),
        new Category("Villager-Hallen", Material.EMERALD, NONE, List.of(
            hall("Trading Hall: Bibliothekar", Material.LECTERN, Villager.Profession.LIBRARIAN),
            hall("Trading Hall: Waffenschmied", Material.GRINDSTONE, Villager.Profession.WEAPONSMITH),
            hall("Trading Hall: Rüstungsschmied", Material.BLAST_FURNACE, Villager.Profession.ARMORER),
            hall("Trading Hall: Werkzeugschmied", Material.SMITHING_TABLE, Villager.Profession.TOOLSMITH),
            hall("Trading Hall: Geistlicher", Material.BREWING_STAND, Villager.Profession.CLERIC),
            hall("Trading Hall: Bauer", Material.COMPOSTER, Villager.Profession.FARMER),
            hall("Trading Hall: Metzger", Material.SMOKER, Villager.Profession.BUTCHER),
            hall("Trading Hall: Gerber", Material.CAULDRON, Villager.Profession.LEATHERWORKER),
            hall("Trading Hall: Steinmetz", Material.STONECUTTER, Villager.Profession.MASON),
            hall("Trading Hall: Fischer", Material.BARREL, Villager.Profession.FISHERMAN)
        ))
    );

    static int totalFarms() {
        return CATEGORIES.stream().mapToInt(c -> c.farms().size()).sum();
    }

    static int implemented() {
        return (int) CATEGORIES.stream().flatMap(c -> c.farms().stream()).filter(f -> f.kind() != Farm.PLANNED).count();
    }
}
