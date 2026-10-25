package de.farm;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Particle;
import org.bukkit.Sound;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.Chest;
import org.bukkit.block.structure.Mirror;
import org.bukkit.block.structure.StructureRotation;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Entity;
import org.bukkit.entity.LivingEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.entity.CreatureSpawnEvent;
import org.bukkit.event.entity.EntityDeathEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.InventoryHolder;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.scheduler.BukkitTask;
import org.bukkit.structure.Structure;
import org.bukkit.util.Vector;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Random;
import java.util.stream.Stream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public final class FarmBuilder extends JavaPlugin implements Listener {

    private static final int[] SIZES = {15, 31, 51, 75};
    private static final String[] SIZE_NAMES = {"Klein", "Mittel", "Mega", "Riesig"};
    private static final int HEIGHT = 6;
    private static final Material[] SIZE_ICONS = {
        Material.LIME_CONCRETE, Material.YELLOW_CONCRETE, Material.ORANGE_CONCRETE, Material.RED_CONCRETE
    };
    private static final String PERM = "farmbuilder.use";

    private final Map<UUID, Preview> previews = new HashMap<>();
    private final List<Region> regions = new ArrayList<>();

    @Override
    public void onEnable() {
        getConfig().addDefault("max-villagers", 100);
        getConfig().options().copyDefaults(true);
        saveConfig();
        new File(getDataFolder(), "farms").mkdirs();
        loadRegions();
        getServer().getPluginManager().registerEvents(this, this);
        getLogger().info("FarmBuilder geladen: " + Farms.implemented() + " von " + Farms.totalFarms()
            + " Katalog-Einträgen implementiert (ungetestet), " + regions.size() + " Spawn-Hallen aktiv.");
    }

    private void saveRegions() {
        List<String> rl = new ArrayList<>();
        for (Region r : regions) rl.add(r.serialize());
        getConfig().set("regions", rl);
        saveConfig();
    }

    private void loadRegions() {
        regions.clear();
        for (String s : getConfig().getStringList("regions")) {
            try {
                regions.add(Region.parse(s));
            } catch (RuntimeException ex) {
                getLogger().warning("Ungültiger Bereich übersprungen: " + s);
            }
        }
    }

    private Region regionAt(Location l) {
        for (Region r : regions) if (r.contains(l)) return r;
        return null;
    }

    /** Natürliche Spawn-Plattformen: nur die Ziel-Mob-Art darf spawnen; sie wird nach kurzer Zeit getötet. */
    @EventHandler
    public void onNaturalSpawn(CreatureSpawnEvent e) {
        if (e.getSpawnReason() != CreatureSpawnEvent.SpawnReason.NATURAL) return;
        Region r = regionAt(e.getLocation());
        if (r == null) return;
        if (e.getEntityType() != r.type()) {
            e.setCancelled(true);
            return;
        }
        Entity ent = e.getEntity();
        Bukkit.getScheduler().runTaskLater(this, () -> {
            if (ent.isValid() && ent instanceof LivingEntity le) le.damage(100000.0);
        }, 20L);
    }

    /** Drops aus einer Spawn-Plattform wandern in die Kiste. */
    @EventHandler
    public void onMobDeath(EntityDeathEvent e) {
        Region r = regionAt(e.getEntity().getLocation());
        if (r == null || e.getEntityType() != r.type()) return;
        World w = e.getEntity().getWorld();
        Block main = w.getBlockAt(r.cx(), r.cy(), r.cz());
        Block other = r.hasOther() ? w.getBlockAt(r.ox(), r.oy(), r.oz()) : null;
        List<ItemStack> drops = new ArrayList<>(e.getDrops());
        e.getDrops().clear();
        e.setDroppedExp(0);
        for (ItemStack it : drops) {
            boolean toOther = other != null && it.getType() != Material.BONE;
            Block target = toOther ? other : main;
            if (target.getState() instanceof Chest c) {
                for (ItemStack left : c.getBlockInventory().addItem(it).values()) {
                    w.dropItemNaturally(e.getEntity().getLocation(), left);
                }
            } else {
                w.dropItemNaturally(e.getEntity().getLocation(), it);
            }
        }
    }

    @Override
    public void onDisable() {
        for (Preview p : previews.values()) p.task.cancel();
        previews.clear();
    }

    // ------------------------------------------------------------------ Befehl

    @Override
    public boolean onCommand(CommandSender sender, Command cmd, String label, String[] args) {
        if (!(sender instanceof Player p)) {
            sender.sendMessage("Nur im Spiel nutzbar.");
            return true;
        }
        if (!p.isOp() && !p.hasPermission(PERM)) {
            p.sendMessage(Component.text("Nur Admins dürfen /farm benutzen.", NamedTextColor.RED));
            return true;
        }
        if (args.length > 0 && args[0].equalsIgnoreCase("cancel")) {
            cancelPreview(p, true);
            return true;
        }
        openCategories(p);
        return true;
    }

    // ------------------------------------------------------------------ Menüs

    private static final class Menu implements InventoryHolder {
        final int level; // 0 = Kategorien, 1 = Farmen, 2 = Größen
        final Category cat;
        final Farm farm;
        List<File> files = new ArrayList<>();
        Inventory inv;

        Menu(int level, Category cat, Farm farm) {
            this.level = level;
            this.cat = cat;
            this.farm = farm;
        }

        @Override
        public Inventory getInventory() {
            return inv;
        }
    }

    private static ItemStack item(Material m, String name, String... lore) {
        ItemStack it = new ItemStack(m);
        ItemMeta meta = it.getItemMeta();
        meta.displayName(Component.text(name, NamedTextColor.GOLD).decoration(TextDecoration.ITALIC, false));
        if (lore.length > 0) {
            meta.lore(java.util.Arrays.stream(lore)
                .map(s -> Component.text(s, NamedTextColor.GRAY).decoration(TextDecoration.ITALIC, false))
                .toList());
        }
        it.setItemMeta(meta);
        return it;
    }

    private static Inventory create(Menu m, int size, String title) {
        Inventory inv = Bukkit.createInventory(m, size, Component.text(title));
        m.inv = inv;
        return inv;
    }

    private void openCategories(Player p) {
        Menu m = new Menu(0, null, null);
        Inventory inv = create(m, 36, "Farm-Menü: Kategorie");
        List<Category> cats = Farms.CATEGORIES;
        for (int i = 0; i < cats.size() && i < 10; i++) {
            int slot = i < 5 ? 11 + i : 20 + (i - 5);
            Category c = cats.get(i);
            inv.setItem(slot, item(c.icon(), c.name(), c.farms().size() + " Farm(en)"));
        }
        inv.setItem(31, item(Material.STRUCTURE_BLOCK, "Eigene Farmen (.nbt)",
            "Getestete Farmen aus Dateien",
            "Ordner: plugins/FarmBuilder/farms/"));
        p.openInventory(inv);
    }

    private List<File> structureFiles() {
        File dir = new File(getDataFolder(), "farms");
        List<File> out = new ArrayList<>();
        try (Stream<java.nio.file.Path> s = Files.walk(dir.toPath())) {
            s.filter(x -> x.toString().toLowerCase().endsWith(".nbt")).sorted()
                .forEach(x -> out.add(x.toFile()));
        } catch (IOException | RuntimeException ex) {
            getLogger().warning("Ordner farms/ konnte nicht gelesen werden: " + ex.getMessage());
        }
        return out;
    }

    private void openCustom(Player p) {
        Menu m = new Menu(3, null, null);
        Inventory inv = create(m, 45, "Eigene Farmen (.nbt)");
        File base = new File(getDataFolder(), "farms");
        m.files = structureFiles();
        for (int i = 0; i < m.files.size() && i < 44; i++) {
            String name = base.toPath().relativize(m.files.get(i).toPath()).toString().replace('\\', '/');
            name = name.substring(0, name.length() - 4);
            inv.setItem(i, item(Material.STRUCTURE_BLOCK, name, "Klick: Vorschau"));
        }
        if (m.files.isEmpty()) {
            inv.setItem(22, item(Material.PAPER, "Noch keine Dateien",
                ".nbt-Dateien in plugins/FarmBuilder/farms/ legen", "Unterordner sind erlaubt"));
        }
        inv.setItem(44, item(Material.BARRIER, "Zurück"));
        p.openInventory(inv);
    }

    private void startStructurePreview(Player p, File file) {
        Structure st;
        try {
            st = Bukkit.getStructureManager().loadStructure(file);
        } catch (IOException | RuntimeException ex) {
            p.sendMessage(Component.text("Datei konnte nicht geladen werden: " + ex.getMessage(), NamedTextColor.RED));
            return;
        }
        cancelPreview(p, false);
        p.closeInventory();
        String name = file.getName().substring(0, file.getName().length() - 4);
        Preview pv = new Preview(null, null, Math.max(st.getSize().getBlockX(), st.getSize().getBlockZ()), "Datei");
        pv.title = name;
        pv.structure = st;
        pv.sx = st.getSize().getBlockX();
        pv.sy = st.getSize().getBlockY();
        pv.sz = st.getSize().getBlockZ();
        pv.task = Bukkit.getScheduler().runTaskTimer(this, () -> tickPreview(p, pv), 0L, 5L);
        previews.put(p.getUniqueId(), pv);
        p.sendMessage(Component.text("Vorschau: " + name + " (" + pv.sx + "x" + pv.sy + "x" + pv.sz
            + "). Rechtsklick = platzieren, Linksklick oder /farm cancel = abbrechen.", NamedTextColor.GREEN));
    }

    private void openFarms(Player p, Category c) {
        Menu m = new Menu(1, c, null);
        Inventory inv = create(m, 36, "Farm-Menü: " + c.name());
        for (int i = 0; i < c.farms().size() && i < 27; i++) {
            Farm f = c.farms().get(i);
            List<String> lore = new ArrayList<>();
            lore.add("Status: " + f.status());
            for (String part : f.desc().split("\\|")) lore.add(part);
            lore.add(f.kind() == Farm.PLANNED ? "Noch nicht verfügbar" : "Klick: Größe wählen");
            inv.setItem(i, item(f.icon(), f.name(), lore.toArray(new String[0])));
        }
        inv.setItem(35, item(Material.BARRIER, "Zurück"));
        p.openInventory(inv);
    }

    private void openSizes(Player p, Category c, Farm f) {
        Menu m = new Menu(2, c, f);
        Inventory inv = create(m, 27, f.name() + ": Größe");
        int[] slots = {10, 12, 14, 16};
        for (int i = 0; i < 4; i++) {
            inv.setItem(slots[i], item(SIZE_ICONS[i], SIZE_NAMES[i], SIZES[i] + " x " + SIZES[i] + " Blöcke"));
        }
        inv.setItem(26, item(Material.BARRIER, "Zurück"));
        p.openInventory(inv);
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getInventory().getHolder() instanceof Menu m)) return;
        e.setCancelled(true);
        if (!(e.getWhoClicked() instanceof Player p)) return;
        if (!p.isOp() && !p.hasPermission(PERM)) {
            p.closeInventory();
            return;
        }
        if (e.getClickedInventory() != e.getInventory()) return;
        int slot = e.getRawSlot();
        ItemStack clicked = e.getCurrentItem();
        if (clicked == null || clicked.getType() == Material.AIR) return;

        switch (m.level) {
            case 3 -> {
                if (slot == 44) {
                    openCategories(p);
                } else if (slot >= 0 && slot < m.files.size()) {
                    startStructurePreview(p, m.files.get(slot));
                }
            }
            case 0 -> {
                if (slot == 31) {
                    openCustom(p);
                    return;
                }
                int idx = slot >= 20 ? 5 + (slot - 20) : slot - 11;
                if (idx >= 0 && idx < Farms.CATEGORIES.size()) openFarms(p, Farms.CATEGORIES.get(idx));
            }
            case 1 -> {
                if (slot == 35) {
                    openCategories(p);
                } else if (slot < m.cat.farms().size()) {
                    Farm sel = m.cat.farms().get(slot);
                    if (sel.kind() == Farm.PLANNED) {
                        p.sendMessage(Component.text(sel.name() + " ist noch nicht gebaut (Status: geplant).", NamedTextColor.YELLOW));
                    } else {
                        openSizes(p, m.cat, sel);
                    }
                }
            }
            case 2 -> {
                if (slot == 26) {
                    openFarms(p, m.cat);
                    return;
                }
                int idx = switch (slot) {
                    case 10 -> 0;
                    case 12 -> 1;
                    case 14 -> 2;
                    case 16 -> 3;
                    default -> -1;
                };
                if (idx >= 0) startPreview(p, m.farm, m.cat.theme(), SIZES[idx], SIZE_NAMES[idx]);
            }
            default -> { }
        }
    }

    // ------------------------------------------------------------------ Vorschau

    private static final class Preview {
        final Farm farm;
        final Theme theme;
        final int n;
        final String sizeName;
        BukkitTask task;
        Location origin;
        String title;
        Structure structure;
        int sx, sy, sz;

        Preview(Farm farm, Theme theme, int n, String sizeName) {
            this.title = farm != null ? farm.name() : "";
            this.farm = farm;
            this.theme = theme;
            this.n = n;
            this.sizeName = sizeName;
        }
    }

    private void startPreview(Player p, Farm f, Theme theme, int n, String sizeName) {
        cancelPreview(p, false);
        p.closeInventory();
        Preview pv = new Preview(f, theme, n, sizeName);
        pv.task = Bukkit.getScheduler().runTaskTimer(this, () -> tickPreview(p, pv), 0L, 5L);
        previews.put(p.getUniqueId(), pv);
        p.sendMessage(Component.text("Vorschau: " + f.name() + " (" + sizeName + ", " + n + "x" + n + "). "
            + "Bewege dich und schau auf die Stelle. Rechtsklick = platzieren, Linksklick oder /farm cancel = abbrechen.",
            NamedTextColor.GREEN));
    }

    private void cancelPreview(Player p, boolean message) {
        Preview pv = previews.remove(p.getUniqueId());
        if (pv != null) {
            pv.task.cancel();
            if (message) p.sendMessage(Component.text("Vorschau abgebrochen.", NamedTextColor.YELLOW));
        } else if (message) {
            p.sendMessage(Component.text("Keine aktive Vorschau.", NamedTextColor.YELLOW));
        }
    }

    private Location computeOrigin(Player p, Preview pv) {
        Block t = p.getTargetBlockExact(50);
        if (t != null) return t.getLocation();
        Location l = p.getLocation();
        Vector d = l.getDirection().setY(0);
        if (d.lengthSquared() < 1.0E-6) d = new Vector(0, 0, 1);
        d.normalize().multiply(pv.n / 2.0 + 3);
        return l.clone().add(d).add(0, -1, 0).getBlock().getLocation();
    }

    private void tickPreview(Player p, Preview pv) {
        if (!p.isOnline()) {
            cancelPreview(p, false);
            return;
        }
        Location o = computeOrigin(p, pv);
        pv.origin = o;
        int w = pv.structure != null ? pv.sx : pv.n;
        int d = pv.structure != null ? pv.sz : pv.n;
        int ph = pv.structure != null ? pv.sy : HEIGHT;
        double x0 = o.getBlockX() - w / 2;
        double x1 = x0 + w;
        double z0 = o.getBlockZ() - d / 2;
        double z1 = z0 + d;
        double y = o.getBlockY() + 1.05;
        for (int i = 0; i <= w; i++) {
            spawn(p, x0 + i, y, z0);
            spawn(p, x0 + i, y, z1);
        }
        for (int i = 0; i <= d; i++) {
            spawn(p, x0, y, z0 + i);
            spawn(p, x1, y, z0 + i);
        }
        for (int dy = 0; dy <= ph; dy++) {
            spawn(p, x0, y + dy, z0);
            spawn(p, x1, y + dy, z0);
            spawn(p, x0, y + dy, z1);
            spawn(p, x1, y + dy, z1);
        }
        p.sendActionBar(Component.text(pv.title + " – " + pv.sizeName + " (" + w + "x" + d
            + ") | Rechtsklick: platzieren | Linksklick: abbrechen", NamedTextColor.AQUA));
    }

    private static void spawn(Player p, double x, double y, double z) {
        p.spawnParticle(Particle.END_ROD, x, y, z, 1, 0, 0, 0, 0);
    }

    @EventHandler
    public void onInteract(PlayerInteractEvent e) {
        Player p = e.getPlayer();
        Preview pv = previews.get(p.getUniqueId());
        if (pv == null || e.getHand() != EquipmentSlot.HAND) return;
        if (e.getAction() == Action.PHYSICAL) return;
        e.setCancelled(true);
        if (e.getAction() == Action.RIGHT_CLICK_AIR || e.getAction() == Action.RIGHT_CLICK_BLOCK) {
            place(p, pv);
        } else {
            cancelPreview(p, true);
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        cancelPreview(e.getPlayer(), false);
    }

    // ------------------------------------------------------------------ Platzieren

    private void place(Player p, Preview pv) {
        if (!p.isOp() && !p.hasPermission(PERM)) {
            cancelPreview(p, false);
            return;
        }
        Location o = pv.origin != null ? pv.origin : computeOrigin(p, pv);
        Farm f = pv.farm;
        cancelPreview(p, false);

        if (pv.structure != null) {
            Location corner = new Location(o.getWorld(), o.getBlockX() - pv.sx / 2, o.getBlockY() + 1, o.getBlockZ() - pv.sz / 2);
            pv.structure.place(corner, true, StructureRotation.NONE, Mirror.NONE, -1, 1.0f, new Random());
            p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
            p.sendMessage(Component.text(pv.title + " wurde platziert.", NamedTextColor.GREEN));
            return;
        }

        Builder b = new Builder(o, pv.n);
        int maxV = getConfig().getInt("max-villagers", 100);
        switch (f.kind()) {
            case Farm.DARK -> {
                regions.add(Constructions.darkFarm(b, f, pv.theme));
                saveRegions();
            }
            case Farm.HALL -> Constructions.hall(b, f, maxV);
            case Farm.COMPOST -> Constructions.compost(b);
            case Farm.ROCKET -> Constructions.rocket(b);
            default -> {
                p.sendMessage(Component.text("Diese Farm ist noch nicht gebaut.", NamedTextColor.YELLOW));
                return;
            }
        }

        p.playSound(p.getLocation(), Sound.ENTITY_PLAYER_LEVELUP, 1f, 1f);
        p.sendMessage(Component.text(f.name() + " (" + pv.n + "x" + pv.n + ") wurde platziert. Status: "
            + f.status(), NamedTextColor.GREEN));
        switch (f.kind()) {
            case Farm.DARK -> p.sendMessage(Component.text("Die Drops landen in der Kiste vor der Halle. Sie läuft, wenn du etwa 30 bis 100 Blöcke entfernt bist (nicht direkt daneben).", NamedTextColor.GRAY));
            case Farm.COMPOST -> p.sendMessage(Component.text("Pflanzenreste in die obere Kiste legen, Knochenmehl aus der unteren Kiste holen. Nur Kompostierbares einlegen.", NamedTextColor.GRAY));
            default -> { }
        }
    }
}
