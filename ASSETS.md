# Graphical Assets Needed

Tracking doc for externally-sourced art. See `CONCEPT.md` -> "Visualization
/ UI" for the design context behind each item. Nothing here is currently
blocking: the map renders today with flat, code-generated placeholder
colors (`ui/.../map/TerrainColors.kt`), so the sim runs and looks reasonable
without any of this. This list is for replacing that placeholder look, and
for the disaster/marker/app-identity art that doesn't exist yet at all.

**Format notes (applies to every tile/overlay below):** the map is a
**top-down 2D square grid** (not isometric) — see CONCEPT.md's "Square grid
cells for v1." Supply transparent PNGs, square, at a decent base resolution
(**128x128 suggested**) — exact on-screen cell size isn't locked yet
(CONCEPT.md flags grid resolution as "a fixed engine constant" still TBD),
so oversized source art scales down cleanly later. Overlay tiles should tile
cleanly at the edges against their neighbors where the element is meant to
form contiguous regions (forests, mountain ranges, etc.).

## Terrain tile art

### Ground composition (base tile)

- [ ] Sand
- [ ] Clay
- [ ] Loam
- [ ] Rock
- [ ] Permafrost
- [ ] Peat/Bog

### Vegetation cover (transparent overlay on the ground tile)

- [ ] Grass/Scrubland
- [ ] Forest/Trees
- [ ] Moss/Tundra vegetation
- [ ] Marsh/Mangrove

*(Barren/None needs no art — it shows bare ground with no overlay.)*

### Topography (overlay - optional)

- [ ] Hills
- [ ] Mountains
- [ ] Valley/Canyon
- [ ] Plateau

*(CONCEPT.md suggests hillshading here. Currently done procedurally in code
as a brightness shift — this art is only needed if you'd rather have real
relief art instead of the procedural shading.)*

### Water features (transparent overlay)

- [ ] Coastline
- [ ] River/Lake-adjacent
- [ ] Wetland-saturated

*(Landlocked needs no overlay.)*

## Disaster overlay icons

Shown as a highlight/icon on cells struck that cycle. One icon per type is
enough; Epidemic can share a single icon across its Crop/Livestock/Human
variants unless you want them visually distinct.

- [ ] Drought
- [ ] Sandstorm
- [ ] Hurricane/Cyclone
- [ ] Flood
- [ ] Earthquake
- [ ] Wildfire
- [ ] Blizzard
- [ ] Tornado
- [ ] Volcanic eruption
- [ ] Landslide
- [ ] Tsunami
- [ ] Avalanche
- [ ] Sinkhole
- [ ] Extreme heat
- [ ] Meteor
- [ ] Epidemic (Crop / Livestock / Human - one icon, or three if distinct)

Suggested format: transparent PNG, ~64x64, simple/high-contrast so it reads
clearly at small size over busy terrain.

## Occupancy markers

Small marker/density indicator for cells with population units.

- [ ] Base occupancy marker (v1: one icon is enough)
- [ ] Optional: Person / Family / Community variants, if scaling the
      marker visually by unit tier is wanted

## App identity

For the window/taskbar icon, and eventually jpackage installers (deferred
in `ui/build.gradle.kts` until there's more UI to package).

- [ ] App icon, source at 256x256, exported as `.ico` (Windows), `.icns`
      (Mac), and `.png` (Linux)

## Deliberately not on this list (handled procedurally, no art needed)

- **Faction territory tint/border** — generated per-faction from a color.
- **Playback controls** (play/pause/step/speed) — plain JavaFX
  buttons/glyphs unless custom-styled icons are specifically wanted.
- **Faction legend swatches** — same per-faction color, no art needed.
