# Graphical Assets Needed

Tracking doc for externally-sourced art. See `CONCEPT.md` -> "Visualization
/ UI" for the design context behind each item. Nothing here is currently
blocking: `TerrainRenderer` (`ui/.../map/TerrainRenderer.kt`) continuously
blends real per-element textures where they exist and falls back to a flat
placeholder color (same values the map used before texture blending
existed - see `TerrainTextures.kt`) where they don't, so the sim runs and
looks reasonable with zero art supplied. Each item below is a drop-in
replacement, one element at a time — no code changes needed, just add the
file at the exact path/name listed.

**Format notes (applies to every tile/overlay below):** the map is a
**top-down 2D square grid** (not isometric) — see CONCEPT.md's "Square grid
cells for v1." Supply transparent PNGs, square, filename **exactly the
element's enum name, lowercased** (e.g. `TerrainElement.FOREST_TREES` ->
`forest_trees.png`), dropped into `ui/src/main/resources/textures/`.
Any resolution works — `TerrainTextures` scales every tile to a canonical
256x256 internally — but supply at least 256x256 source art to avoid
upscaling blur; the textures already in the repo (`sand.png`,
`forest_trees.png`, `rock.png`) are 256x256 and are a good reference point.
Overlay tiles should tile cleanly at the edges against their neighbors
where the element is meant to form contiguous regions (forests, mountain
ranges, etc.).

## Terrain tile art

### Ground composition (base tile)

- [x] Sand — `sand.png`
- [ ] Clay — `clay.png`
- [ ] Loam — `loam.png`
- [x] Rock — `rock.png`
- [ ] Permafrost — `permafrost.png`
- [ ] Peat/Bog — `peat_bog.png`

### Vegetation cover (transparent overlay on the ground tile)

- [ ] Grass/Scrubland — `grass_scrubland.png`
- [x] Forest/Trees — `forest_trees.png`
- [ ] Moss/Tundra vegetation — `moss_tundra.png`
- [ ] Marsh/Mangrove — `marsh_mangrove.png`

*(Barren/None needs no art — it renders fully transparent, showing bare
ground through with no overlay.)*

### Topography (overlay - optional)

- [ ] Hills
- [ ] Mountains
- [ ] Valley/Canyon
- [ ] Plateau

*(CONCEPT.md suggests hillshading here. This is rendered procedurally today
as a brightness multiplier in `TerrainTextures.TOPOGRAPHY_BRIGHTNESS` and
`TerrainRenderer`, with no texture support at all yet — this art is only
needed if/when real relief art replaces that procedural shading, which
would require renderer changes too, not just a dropped-in file like every
other item on this list.)*

### Water features (transparent overlay)

- [ ] Coastline — `coastline.png`
- [ ] River/Lake-adjacent — `river_lake_adjacent.png`
- [ ] Wetland-saturated — `wetland_saturated.png`

*(Landlocked needs no overlay — it renders fully transparent.)*

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
