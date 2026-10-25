FarmBuilder 5.0 - ehrlicher Katalog (ohne Spawner, ohne erzeugte Items)
======================================================================
/farm          -> Menue: Kategorien -> Farm -> Groesse (Klein 15, Mittel 31, Mega 51, Riesig 75)
/farm cancel   -> Vorschau abbrechen
Vorschau mit Partikel-Rahmen; Rechtsklick = platzieren (ohne Materialien), Linksklick = abbrechen.
Nur OPs bzw. Permission farmbuilder.use.

Status steht in jeder Menue-Beschreibung und in FARMEN.md.
Implementiert (alles NICHT kompiliert und NICHT im Spiel getestet):
* Raketen-Herstellungsstation (Werkbank, Kisten, Schilder - Handarbeit)
* Komposter-Station fuer Knochenmehl (Kiste -> Trichter -> Komposter -> Trichter -> Kiste)
* 6 dunkle Spawn-Hallen (Creeper, Skelett, Zombie, Spinne, Enderman, Hexe): natuerliches Spawnen, kein Spawner.
  Das Plugin laesst nur die Ziel-Mob-Art spawnen, toetet sie und legt die Drops in die Kiste vor der Halle.
* 10 Villager-Handelshallen
Geplant (nicht gebaut): Zuckerrohr, Kuh, Eisen, Gold, Nahrung, Holz, Bambus, Kaktus u. a.

Eigene getestete Farmen: .nbt-Dateien in plugins/FarmBuilder/farms/ legen (Menue "Eigene Farmen (.nbt)").
config.yml: max-villagers (Lag-Schutz), regions (gespeicherte Spawn-Hallen).
