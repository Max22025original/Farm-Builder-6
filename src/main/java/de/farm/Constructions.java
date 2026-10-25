package de.farm;

import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.block.data.Directional;
import org.bukkit.block.sign.Side;
import org.bukkit.entity.Villager;

/** Baut die Farmen. Es werden nur echte Blöcke gesetzt; keine Items werden per Plugin erzeugt. */
final class Constructions {
    private Constructions() {}

    private static void data(Builder b, int x, int y, int z, String blockData, boolean physics) {
        b.at(x, y, z).setBlockData(Bukkit.createBlockData(blockData), physics);
    }

    private static void chest(Builder b, int x, int y, int z) {
        b.set(x, y, z, Material.CHEST);
        Block c = b.at(x, y, z);
        if (c.getBlockData() instanceof Directional d) {
            d.setFacing(BlockFace.SOUTH);
            c.setBlockData(d, false);
        }
    }

    private static void sign(Builder b, int x, int y, int z, String... lines) {
        data(b, x, y, z, "minecraft:oak_wall_sign[facing=north]", false);
        BlockState st = b.at(x, y, z).getState();
        if (st instanceof Sign s) {
            for (int i = 0; i < lines.length && i < 4; i++) s.getSide(Side.FRONT).line(i, Component.text(lines[i]));
            s.update(true, false);
        }
    }

    private static void villager(Builder b, int x, int z, Villager.Profession job) {
        Location loc = new Location(b.w, b.ox + x + 0.5, b.oy + 1, b.oz + z + 0.5);
        b.w.spawn(loc, Villager.class, (Villager v) -> {
            v.setProfession(job);
            v.setPersistent(true);
        });
    }

    /** Anzahl Module pro Achse je nach Größe (Klein/Mittel/Mega/Riesig). */
    private static int axis(int n, int klein, int mittel, int mega, int riesig) {
        return n >= 75 ? riesig : n >= 51 ? mega : n >= 31 ? mittel : klein;
    }

    // ------------------------------------------------------------ Raketen-Herstellungsstation

    static void rocket(Builder b) {
        int k = axis(b.n, 1, 2, 3, 4);
        int startX = -((k - 1) * 10) / 2;
        int startZ = -((k - 1) * 6) / 2;
        b.fill(-b.h, 1, -b.h, b.h, 6, b.h, Material.AIR);
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                station(b, startX + i * 10, startZ + j * 6);
            }
        }
    }

    private static void station(Builder b, int sx, int sz) {
        b.fill(sx - 4, 0, sz - 1, sx + 4, 0, sz + 1, Material.SMOOTH_STONE);
        b.fill(sx - 4, 1, sz + 1, sx + 4, 3, sz + 1, Material.STONE_BRICKS);
        chest(b, sx - 3, 1, sz);   // Papier
        chest(b, sx - 1, 1, sz);   // Schießpulver
        b.set(sx + 1, 1, sz, Material.CRAFTING_TABLE);
        chest(b, sx + 3, 1, sz);   // Raketen
        sign(b, sx - 3, 2, sz, "Papier", "(Eingabe)");
        sign(b, sx - 1, 2, sz, "Schiesspulver", "(Eingabe)");
        sign(b, sx + 1, 2, sz, "Rakete:", "1 Papier +", "1-3 Pulver");
        sign(b, sx + 3, 2, sz, "Raketen", "(Ausgabe)");
        b.set(sx - 4, 1, sz - 1, Material.LANTERN);
        b.set(sx + 4, 1, sz - 1, Material.LANTERN);
    }

    // ------------------------------------------------------------ Komposter-Station (Knochenmehl)

    /**
     * Pro Modul von oben nach unten: Eingangskiste, Trichter (nach unten), Komposter, Trichter (nach unten),
     * Ausgabekiste. Trichter füllen kompostierbare Items in den Komposter und holen das Knochenmehl unten wieder heraus.
     */
    static void compost(Builder b) {
        int k = axis(b.n, 2, 4, 6, 8);
        int start = -((k - 1) * 4) / 2;
        b.fill(-b.h, 1, -b.h, b.h, 7, b.h, Material.AIR);
        for (int i = 0; i < k; i++) {
            for (int j = 0; j < k; j++) {
                int cx = start + i * 4;
                int cz = start + j * 4;
                b.fill(cx - 1, 0, cz - 1, cx + 1, 0, cz + 1, Material.STONE_BRICKS);
                chest(b, cx, 1, cz);                                  // Ausgabe: Knochenmehl
                data(b, cx, 2, cz, "minecraft:hopper[facing=down]", false);
                b.set(cx, 3, cz, Material.COMPOSTER);
                data(b, cx, 4, cz, "minecraft:hopper[facing=down]", false);
                chest(b, cx, 5, cz);                                  // Eingabe: Pflanzenreste
            }
        }
    }

    // ------------------------------------------------------------ Dunkle Spawn-Hallen (ohne Spawner)

    /**
     * Dunkle, geschlossene Hallen mit mehreren Ebenen. Monster spawnen dort ganz normal (natürliches Spawnen).
     * Das Plugin lässt nur die Ziel-Mob-Art spawnen, tötet sie kurz nach dem Spawn und legt die Drops in die Kiste
     * vor der Halle. Gibt den Bereich zurück.
     */
    static Region darkFarm(Builder b, Farm f, Theme t) {
        int h = b.h;
        int layers = b.n >= 75 ? 4 : b.n >= 51 ? 3 : b.n >= 31 ? 2 : 1;
        int roofY = 4 * layers;

        b.fill(-h, 1, -h, h, roofY + 2, h, Material.AIR);

        for (int i = 0; i < layers; i++) {
            int y = 4 * i;
            b.fill(-h, y, -h, h, y, h, t.floor());
            for (int x = -h; x <= h; x++) {
                for (int z = -h; z <= h; z++) {
                    if (Math.abs(x) == h || Math.abs(z) == h) {
                        for (int yy = y; yy <= y + 3; yy++) b.set(x, yy, z, t.wall());
                    }
                }
            }
        }
        b.fill(-h, roofY, -h, h, roofY, h, t.wall());

        int sz = -h - 3;
        b.fill(-1, 0, sz - 1, 1, 0, sz + 1, t.base());
        chest(b, 0, 1, sz);

        return new Region(b.w.getName(),
            b.ox - h, b.oy, b.oz - h, b.ox + h, b.oy + roofY + 3, b.oz + h, f.mob(),
            b.ox, b.oy + 1, b.oz + sz,
            false, 0, 0, 0);
    }

    // ------------------------------------------------------------ Villager-Handelshalle

    /**
     * Gänge mit Zellen auf beiden Seiten. Pro Zelle: ein Villager mit Beruf, direkt dahinter sein Arbeitsblock.
     * Der Spieler handelt vom Gang aus durch ein Eisengitter. Einlass an der Westseite (x = -h).
     */
    static void hall(Builder b, Farm f, int maxVillagers) {
        int h = b.h, n = b.n;
        b.fill(-h, 1, -h, h, 6, h, Material.AIR);
        b.floor(Material.OAK_PLANKS);
        b.fill(-h, 3, -h, h, 3, h, Material.OAK_PLANKS);
        int spawned = 0;

        for (int z = -h; z <= h; z++) {
            for (int x = -h; x <= h; x++) {
                int zz = z + h, xx = x + h;
                boolean edgeZ = zz == 0 || zz == n - 1;
                int q = zz - 1;
                int unit = q / 8;
                int r = q % 8;
                boolean complete = !edgeZ && (unit * 8 + 6 <= n - 3);
                boolean divider = xx % 2 == 0;

                if (edgeZ || !complete || r == 7) {
                    b.fill(x, 1, z, x, 2, z, Material.STONE_BRICKS);
                    continue;
                }
                switch (r) {
                    case 3 -> { // Gang
                        if (x == h) b.fill(x, 1, z, x, 2, z, Material.STONE_BRICKS);
                        if (xx % 4 == 1) b.set(x, 3, z, Material.SEA_LANTERN);
                    }
                    case 0, 6 -> { // Arbeitsblock-Reihe
                        if (divider) b.fill(x, 1, z, x, 2, z, Material.STONE_BRICKS);
                        else b.set(x, 1, z, f.station());
                    }
                    case 1, 5 -> { // Villager-Reihe
                        if (divider) {
                            b.fill(x, 1, z, x, 2, z, Material.STONE_BRICKS);
                        } else {
                            b.set(x, 3, z, Material.SEA_LANTERN);
                            if (spawned < maxVillagers) {
                                villager(b, x, z, f.job());
                                spawned++;
                            }
                        }
                    }
                    case 2, 4 -> { // Trennwand zum Gang mit Eisengitter
                        if (divider) b.fill(x, 1, z, x, 2, z, Material.STONE_BRICKS);
                        else b.set(x, 1, z, Material.IRON_BARS);
                    }
                    default -> { }
                }
            }
        }
    }
}
