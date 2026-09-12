# Future / V2 Ideas

A backlog of deferred, "not for v1" ideas — collected in one place instead
of scattered as inline asides across `CONCEPT.md`, so they don't get lost
or duplicated as the design/codebase evolve. Nothing here is scheduled;
this is where an idea goes when it's worth remembering but wrong to build
now (adds complexity/scope v1 doesn't need yet, or depends on v1 groundwork
that doesn't exist yet).

## Simulation

- **Continuous unit-variance formula.** v1 uses discrete bands
  (person/family/community-style tiers) for how much a unit's stat rolls
  vary from its base value. A continuous formula (variance scaling smoothly
  with unit size rather than jumping between three fixed tiers) would be
  more exacting, but isn't needed for v1 to be internally consistent. See
  `CONCEPT.md` -> "Population Model: Unique Units" -> "Variance bands".

## Map / Rendering

- **Hex grid.** v1 uses a square grid (pairs naturally with the
  elevation/gradient-driven disaster spread model already designed).
  Hex would give uniform adjacency distance in every direction (no
  diagonal-distance ambiguity), but is a larger migration than v1 needs to
  ship with. See `CONCEPT.md` -> "Main map view" and "Open Questions" ->
  "Square-to-hex migration path".
- **Multiple texture variants per element, blended for variety.** Today
  (`TerrainTextures.kt`) each `TerrainElement` maps to exactly one texture
  tile, so a large contiguous zone of the same element (a big Sand region,
  say) visibly repeats that one tile edge-to-edge. v2 would let an element
  have several art variants and blend/vary between them across a zone
  (e.g. a secondary noise field or per-tile hash choosing/blending which
  variant shows where) so large same-element areas read as natural
  variation instead of an obvious repeating pattern. Needs: multiple source
  textures per element (art cost), a variant-selection mechanism in
  `TerrainTextures`, and a blending approach in `TerrainRenderer` that
  doesn't reintroduce hard seams at variant boundaries.
