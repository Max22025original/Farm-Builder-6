package de.farm;

import org.bukkit.Material;
import java.util.List;

record Category(String name, Material icon, Theme theme, List<Farm> farms) {
}
