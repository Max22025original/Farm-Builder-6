package de.farm;

import org.bukkit.Material;

/** Optik einer Kategorie. */
record Theme(Material floor, Material wall, int wallH, Material deco, Material base, boolean fields) {
}
