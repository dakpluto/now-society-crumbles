# Now Society Crumbles — Concept Document

> Living design document. Updated continuously during concept development,
> before any scaffolding/coding begins. This file will likely be removed
> from the public repo once implementation starts, since it's a design
> artifact rather than part of the open-source project itself.

## Overview

**Now Society Crumbles** is a civilization simulator — not really a "game"
in the traditional sense, though it borrows game concepts, visual style,
and interaction patterns. The user configures a society's starting
conditions (population, environment, culture, technology era, etc.) and
then runs a simulation forward a set number of years to see whether the
society survives, and — just as importantly — *how* it changes along the
way (growth, fragmentation, technological/cultural evolution, happiness).

Core question the sim answers: **did the society survive, and what kind
of story did it tell getting there?**

## Technical Approach

Decided ahead of any scaffolding, so later design decisions can assume
this shape. (Originally scoped around C#/.NET/Avalonia; revised after
weighing options against the user's existing Java familiarity and
interest in Kotlin — see the JVM stack below.)

- **Native desktop app**, not a web app or general game engine. The
  simulation itself is cycle/turn-based (advancing by years, not
  real-time physics/frame updates), so a full game engine (Unity,
  Godot, Unreal, libGDX) would be more overhead than the visual ambition
  needs.
- **Kotlin on the JVM** — chosen over C#/.NET given the user's stronger
  existing familiarity with Java and interest in Kotlin specifically;
  Kotlin is fully interoperable with the wider Java ecosystem while
  offering more modern language ergonomics than plain Java.
- **JavaFX** as the UI framework — chosen over Kotlin + Compose
  Multiplatform for Desktop specifically because JavaFX's `TableView`
  and built-in `Chart` classes give the Tab-key spreadsheet/graph detail
  view essentially for free, versus needing to hand-build or interop for
  that view under Compose. Traded off: JavaFX's look/momentum is more
  dated than Compose's, and it's a separate runtime dependency (OpenJFX)
  that needs to be bundled for distribution.
- **Map view**: a custom-drawn 2D scene using JavaFX's `Canvas` node,
  redrawn per simulated cycle rather than continuously animated — a
  data-driven, procedurally generated map rather than a game-engine
  scene.
- **Detail/spreadsheet view** (the Tab-key drill-down): JavaFX
  `TableView` for tabular history and JavaFX's built-in `Chart` classes
  (line/bar/pie/scatter) for graphs — no third-party charting library
  needed.
- **Simulation engine lives in its own module, fully decoupled from the
  UI module** (separate Gradle module within one multi-module build).
  Most of what's been designed so far (Need→Strategy→Resolution, unit
  variance, hierarchical stat aggregation, disasters/events) is pure
  formulas and state with no inherent UI dependency. Keeping the
  boundary clean makes the engine independently unit-testable and leaves
  room for a future headless/CLI mode (batch-running or tuning
  simulations without launching the UI) — not something to build now,
  just a reason to separate it from day one.
- **Testing: Kotest.** Runs on the same JUnit Platform (so IDE/Gradle
  tooling works normally) but adds property-based testing, which fits
  this design well — most of the system is randomized formulas with
  bounded variance (unit bands, regression risk, disaster rolls, the
  chaos constant), and property tests can assert invariants (e.g. "a
  regressed stat never drops below 0," "community-tier variance never
  exceeds its band") across thousands of random rolls rather than a
  handful of hand-picked examples.
- **Save/load format: JSON via kotlinx.serialization** — plain Kotlin
  data classes serialize with an annotation, no reflection-heavy setup.
  Chosen specifically so simulation configs are easy for people to read,
  hand-edit, and share. **The shareable config file is the seed plus
  every starting value** (society size, era, environment variables,
  starting traits, complexity level, unit count, etc.) — the full set of
  inputs needed to reproduce a run exactly, not the seed alone. Field-by-
  field schema now drafted — see "Shareable config/seed file schema"
  below.
- **Build tool**: Gradle with the Kotlin DSL — the idiomatic choice for
  a Kotlin project, with good JavaFX plugin support for packaging.
- **Repo conventions**: target the latest LTS JDK and latest stable
  Kotlin available whenever scaffolding actually begins (not pinned to a
  specific version now, since it will drift before then); GitHub Actions
  CI to build and run tests on every push/PR; Semantic Versioning,
  starting in `0.x.y` during early development and moving to `1.0.0`
  once a full simulation can be configured and run end to end.
- **License: MIT** (see `LICENSE` at the repo root) — chosen for the
  eventual open-source release.
- **Packaging/distribution**: `jlink`/`jpackage` to bundle the JVM +
  JavaFX runtime into a self-contained distributable, so end users don't
  need a separate Java install.
- **All randomness routed through a seeded PRNG** (variance rolls,
  disaster generation, regression checks, the chaos constant), rather
  than unseeded randomness — this makes a given simulation run fully
  reproducible from its seed + starting configuration. **The seed is a
  user-facing, definable value** — the user can set a specific seed
  before running (or let one be generated), and share it so someone else
  can watch the exact same society unfold.

### Shareable config/seed file schema

**Stores resolved input parameters, not a generated snapshot.** Loading
a config re-runs the same deterministic procedural generation (map,
starting units) from these inputs plus the seed, rather than storing
every already-generated per-cell/per-unit value — keeps the file small
and genuinely hand-editable, matching the "easy to read, hand-edit, and
share" goal. **Trade-off, accepted for v1**: reproducibility holds
within the same schema/engine version only — a future change to the
procedural generation algorithm would regenerate a different result
from an old file's same seed. No cross-version migration system is
planned; `schemaVersion` just lets the app detect and reject/flag an
incompatible old file rather than silently misbehaving.

**The config always stores fully-resolved values for every field,
regardless of what complexity tier produced them** — even a tier-1 run
where the user couldn't adjust anything still resolves to concrete
numbers, and those are what get stored. This keeps the schema uniform
(no sparse/conditional fields based on complexity); `complexityLevel` is
stored purely so **reopening a shared config in the setup screen
restores its original tier's adjustable ranges, and the user can edit
any value within that tier's scope before running** — loading a config
populates the same adaptive form a from-scratch setup would use, not an
immutable locked recipe.

Top-level fields (`SimulationConfig`, a `kotlinx.serialization` data
class):

| Field | Contents |
|---|---|
| `schemaVersion` | Engine/schema version this config was created under |
| `seed` | The user-facing, definable PRNG seed |
| `complexityLevel` | 1-10, per Complexity tier scale — for setup-screen re-editing, not to gate which other fields exist |
| `societySize` | Starting population count |
| `startingEra` | Starting era (exact era enum list still TBD — not designed elsewhere in this doc yet) |
| `environment` | Resolved water resources, land quality, wildlife, and weather pattern values; resolved terrain composition per element category (Topography, Ground composition, Vegetation cover, Water features); which terrain archetype (if any) it started from, kept only as display metadata since the resolved element values are what the simulation actually reads |
| `startingTraits` | Resolved starting values for whichever Personality/Trait and Capability/Knowledge stats are configurable at setup (which subset is configurable is itself complexity-gated, same as everything else under Complexity level — unified definition, not a special case) |
| `uniqueUnitCount` | The selected unit count |
| `map` | Map size (independent of population/complexity — see Setup / Configuration and Main map view → "Map size and grid resolution"); cell resolution itself isn't stored since it's a fixed engine constant, not a per-run setting |

## Setup / Configuration

- **Society size**: from as few as ~10 people to tens/hundreds of
  thousands.
- **Map size**: an **independently configurable** setup variable, not
  derived from population — lets density itself become meaningful (a
  tiny population in a vast wilderness, or a huge population crammed
  into a small territory), rather than every run's territory being
  auto-scaled to its population. See "Map size and grid resolution"
  under Visualization / UI → Main map view for how this becomes actual
  grid dimensions.
- **Starting era**: Caveman, Bronze Age, Modern, etc.
- **Environment variables**: water resources, land quality, wildlife,
  weather patterns, terrain, etc.
- **Society starting traits**: average intelligence, work ethic,
  religiosity, farming knowledge, technology knowledge, etc.
- **Complexity level**: a single slider/tier that governs how much manual
  control the user has across *multiple* systems at once:
  - **Environment/society variables**: low complexity = simple presets
    (e.g. "rainforest," "tundra," "desert," "tropical island," or famous
    real-world societies like Colonial America, Song Dynasty China,
    Ancient Egypt). Mid complexity = presets act as *starting values* the
    user can adjust within a limited range (e.g. complexity 5 = alter 10
    variables within ±20%). High complexity = fine-tune most/all
    variables, larger or unlimited ranges (e.g. complexity 8 = alter all
    variables, unlimited range). Highest complexity = little or no
    preset — the user sets nearly everything from scratch.
  - **Unique unit count** (see below): the *range* of selectable unit
    counts is gated by complexity — low complexity = small range of
    coarse-grained options, high complexity = wide range, up to
    (theoretically) one unit per person even at large population sizes.
    A soft performance warning should trigger above some unit-count
    threshold rather than a hard cap.

### Complexity level — unified definition

To keep this tractable to build, complexity level is defined as three
consistent mechanisms, applied the same way across every system in the
sim (environment, units, factions, tech/culture, disasters, etc.):

1. **User control over starting values** — how much of the initial
   configuration is hardcoded/preset vs. exposed for the user to adjust,
   and how wide a range they're allowed to adjust within. This covers
   environment/society variable presets and the unique unit count range
   described above.
2. **Cycle-to-cycle persistence of detailed values** — every formula
   always operates on the *full* detailed breakdown of its inputs (see
   "Complexity-gated stat granularity" under Technology & Culture
   Evolution, which is a general pattern, not tech-specific). Complexity
   only controls which of those detailed values are allowed to keep
   their own independently-drifted value from cycle to cycle, versus
   being reset/locked to a neutral baseline (the recalculated aggregate
   average) at the end of each cycle.
3. **Complexity-gated input subroutines** — complexity can enable an
   optional preprocessing subroutine that transforms a baseline value
   into a scoped **effective value** used for just one step of the main
   formula, without ever overwriting the baseline itself.
   Example: resolving a faction battle, low complexity feeds each side's
   raw baseline troop count straight into the combat formula. Higher
   complexity instead runs troop count through a terrain/proximity
   subroutine first — e.g. 100 soldiers, given the terrain, produces an
   effective strength of 50 — and *that* effective number is what feeds
   the combat-strength part of the formula. Critically, casualty
   percentages (and any other part of the resolution that depends on
   real population) are still computed against the **original baseline**
   troop count, never the subroutine's effective number — the subroutine
   output is scoped to the one calculation step it was produced for. This
   mirrors how per-calculation unit variance noise (see Population
   Model) never feeds back into a unit's persistent base stat.

### Complexity tier scale (1-10)

**Complexity is a fine-grained 1-10 numeric scale** (a slider, not a
handful of named modes), matching the illustrative examples already used
elsewhere in this doc. All three mechanisms above, plus every other
complexity-gated system (unique unit count range, third-party faction
intervention, custom terrain composition editing), scale up **together
and smoothly** across the same 10 tiers — there's one complexity number,
not a separate slider per system.

Four milestone tiers are locked in now, anchoring the ends and middle of
the scale; the exact numeric progression connecting them (variable
counts/ranges at tiers 2-4 and 6-7, subroutine unlock order, unit-count
range at each step) is intentionally left TBD rather than designing all
ten tiers' exact numbers now:

| Tier | Starting-value control | Persistent detail | Notes |
|---|---|---|---|
| 1 (lowest) | Fixed presets, no adjustment | 0% — every sub-stat resets to the recalculated aggregate each cycle | No complexity-gated subroutines active; coarsest unit-count options only |
| 5 (middle anchor) | ~10 variables adjustable within ±20% | Roughly half of sub-stats may persist independently | Matches the existing "complexity 5" example under Setup / Configuration |
| 8 | All variables adjustable, unlimited range | Most sub-stats persist independently | Matches the existing "complexity 8" example; this is also where **third-party faction intervention** unlocks, matching the existing "High complexity" language under Factions → Complexity gating on faction interactions |
| 10 (highest) | Little to no preset — user sets nearly everything from scratch | Aggregate becomes pure tracking/display, with zero effect on any individual detailed value | Matches the existing "Highest complexity" language under Setup / Configuration; also unlocks direct custom terrain-composition editing (see Terrain composition elements) and the widest unit-count range (theoretically one unit per person, soft performance warning still applies) |

Tiers 2-4, 6-7, and 9 exist on the scale and are understood to
interpolate smoothly between these anchors on every mechanism at once —
their exact numeric values are deferred to the same later pass as the
rest of the sim's concrete numeric tuning (terrain weights, severity
tiers, etc.).

### Terrain composition elements

The nine terrain archetypes (see Disasters & Events → Weight-sourcing
methodology) aren't atomic labels — each is a **weighted composition of
underlying terrain elements**, grouped into four categories. This is the
same preset/composition relationship the complexity system already uses
elsewhere (an archetype is just a preset starting mix); it also means a
future high-complexity "custom terrain" mode is just another application
of the existing complexity-gated preset mechanic, not a new system. Only
the element list is being locked in now — actual per-archetype weight
values (e.g. "Desert is 70% Sand, 20% Hills, 10% Rock") are deferred to
the full environment-variables pass.

- **Topography** (elevation/shape): Flat/Plains, Hills, Mountains,
  Valley/Canyon, Plateau
- **Ground composition** (surface material): Sand, Clay, Loam, Rock,
  Permafrost, Peat/Bog
- **Vegetation cover**: Barren/None, Grass/Scrubland, Forest/Trees,
  Moss/Tundra vegetation, Marsh/Mangrove
- **Water features** (geographic adjacency — distinct from the
  *"water resources"* environment variable, which is about abundance/
  reliability, not physical adjacency): Coastline, River/Lake-adjacent,
  Wetland-saturated, Landlocked. **Coastline and River/Lake-adjacent are
  computed boundary lines, not blended-area elements like the other two**
  — see "Spatial model: map grid and per-cell elements" ->
  "Coastline and River/Lake-adjacent are boundary-line features."

**Decided**: the disaster weight table is computed from underlying
element composition directly, not a flat per-archetype lookup — the
archetype name is setup-time UI sugar only. This is resolved in detail
under Disasters & Events → "Spatial model: map grid and per-cell
elements," which also introduces the map as a **grid of cells**, each
carrying its own local element composition rather than one composition
for the whole map.

**The map itself (grid resolution, procedural per-cell generation from
an archetype preset, how map size scales with society size/population,
and per-disaster spread-pattern formulas beyond the worked Avalanche
example) is flagged as a large, still mostly-open area of design to
come back to as its own focused pass** — see Open Questions.

## Population Model: Unique Units

Rather than simulating every individual at every scale, the population is
abstracted into a user-selected number of **unique units**, capped at the
society's total population:

- 10 people, 10 units → every person simulated individually.
- 100 people, 20 units → ~20 family units, randomly composed (singles,
  small families, large families, etc.).
- 200,000 people, 1,000 units → each unit represents a whole community.

### Variance bands

As unit size grows, the "hardness" of that unit's stats weakens — larger
units contain more internal diversity, so calculations against them get
noisier:

| Unit tier | Example variance range on a stat |
|---|---|
| Person | 0% — exact value used |
| Family | ~1–5% randomized swing |
| Community | ~5–20% randomized swing |

Example: a unit's farming knowledge base value is 52.
- As a person: always exactly 52 in calculations.
- As a family: randomized to something like 48–56 *each time it's used*.
- As a community: randomized to something like 40–64 *each time it's
  used*.

**Implementation notes:**
- v1 uses **discrete bands** (person/family/community-style tiers), not a
  continuous formula — see `FUTURE.md` for the deferred continuous-formula
  idea.
- The randomization is **recalculated every time the value is used** in a
  calculation — it is *not* rolled once and cached, and it does *not*
  feed back into the base value. The same unit's 52 farming knowledge
  produces fresh noise every calculation, but is still always centered on
  52.
- The **base value itself can still change** over time through
  persistent causes: education, surviving disasters, generational drift,
  etc. Only the base value changes persist — the per-calculation noise
  never does.

## Win / Loss Conditions

- **Death/failure**: population hits zero, or falls into an irrecoverable
  decline — below the threshold needed for regrowth — by the end of the
  simulation timer.
- **Survival**: life continues, but survival is not binary "win" — part
  of the point is observing *how* the society changed:
  - Did it stay unified and grow, or fracture into multiple factions?
  - If it fractured, how did the resulting factions treat each other —
    cooperation, exploitation, conquest?
  - Did technology/culture advance or regress — and was that regression
    actually bad, or a reasonable adaptation?
  - Is the society happier as a result of the path it took, independent
    of whether it "survived" in a strict population sense?

## Stat System — General Principles

Before locking in any concrete list of stats/traits, a few architectural
rules apply across all of them:

- **Flat and uniform under the hood.** Personality-style traits
  (intelligence, work ethic, religiosity, risk tolerance), capability/
  knowledge stats (farming knowledge, medical knowledge, tech knowledge),
  and current-condition readouts (hunger, happiness) may be organized
  into human-readable categories for documentation/UI clarity, but to the
  simulation's algorithms they are all just **flat, uniformly-typed
  values** — there is no special-cased "trait vs. capability" code path.
  Everything blends together and is used the same way by formulas.
- **Nothing is fixed except the chaos value.** Every stat — including
  personality traits — can shift over time due to any number of factors;
  there is no inherently static stat. Stats can also causally influence
  *each other's* regression/growth, not just their own: e.g. rising
  education/technology could increase work ethic's regression risk over
  time, simulating growing dependence on automation. The **sole
  exception** is the chaos constant itself: it is rolled once during
  initial simulation setup and then stays fixed for the entire run — it
  is never recalculated cycle to cycle like everything else.
- **Cross-level influence flows in every direction.** Building on
  Hierarchical Stat Aggregation & Cross-Level Influence: small
  units/factions can meaningfully influence large aggregates, and large
  aggregates can meaningfully influence small units/factions — influence
  is never assumed to be one-directional at any scale.
- **Default numeric scale is 1–100**, used as consistently as possible
  across all stats/traits for simplicity. Genuinely count-like
  quantities (population, raw resource stockpiles) are the practical
  exception and may use their own natural units instead.
- **No rigid trait/capability/state trichotomy.** An earlier idea to
  split stats into "growable capabilities," "fixed personality traits,"
  and "current-condition state" was considered and rejected — in
  practice these categories blend into each other, consistent with the
  "flat and uniform" and "nothing is fixed" principles above.
- **Concrete stat taxonomy is intentionally deferred** — not overlooked.
  The architecture above is meant to be locked in first so that whatever
  concrete list of stats/traits gets defined later slots into a stable,
  already-agreed structure. A first draft now exists — see "Master stat
  list (draft)" below — collecting every stat name already referenced
  informally elsewhere in this doc.

### Master stat list (draft)

Human-readable groupings only, per the "flat and uniform under the
hood" principle above — the simulation's algorithms treat every entry
the same way regardless of group. Not exhaustive; expect additions as
other systems (culture sub-stats, human-caused events, etc.) get
designed in more detail.

- **Personality/Trait**: Intelligence, Work Ethic, Religiosity, Risk
  Tolerance, Strength, Charisma, **Economic Ideology**, **Social
  Ideology** (Ideology is two stats, not one — following the political
  compass model: Economic Ideology as the left-right axis, Social
  Ideology as the authoritarian-libertarian axis)
- **Capability/Knowledge**: Farming Knowledge, Medical Knowledge,
  Technology Knowledge, Military Knowledge/Capacity, Communication
  Knowledge, Diplomatic Knowledge (an entity's own skill at diplomacy —
  distinct from Diplomatic Influence, which is a derived property of the
  pairwise relationship structure below, not a flat stat), Environmental
  Knowledge
- **Current-condition/State**: Hunger, Happiness, Health, Security
  (perceived threat level)
- **Aggregate/composite** (each backed by complexity-gated detailed
  sub-stats, per Complexity-gated stat granularity under Technology &
  Culture Evolution): Technology, Culture, Education Level

**Relationship/diplomatic data is explicitly *not* on this list.**
Faction-to-faction relationship strength and diplomatic influence
(already referenced under Factions and Hierarchical Stat Aggregation)
are inherently **pairwise** — faction A's standing with faction B
specifically — which doesn't fit the flat per-entity stat model above.
These live in a **separate relationship structure** instead; see
"Relationship structure (faction-to-faction)" below.

### Relationship structure (faction-to-faction)

- **Asymmetric**: A's relationship toward B and B's toward A are tracked
  separately and can differ — a small faction can distrust a powerful
  neighbor more than that neighbor distrusts it. This also matches the
  directional nuance already used elsewhere in this doc (epidemic
  spillover, upward/downward influence).
- **Multi-dimensional, not a single blended score**: for every ordered
  faction pair (A→B), three tracked dimensions, each on the sim's
  default 1-100 scale:
  - **Trust** — general goodwill/perceived reliability. Primary gate for
    aid-rendering and alliance willingness (the "relationship level"
    gate under Hierarchical Stat Aggregation → Downward influence).
  - **Trade** — economic relationship strength/volume. Feeds
    trade-seeking Strategy resolution under the Core faction decision
    loop.
  - **Military Tension** — hostility/perceived threat. Feeds the
    occurrence weighting for the "events against others" human-caused
    event category (War, Espionage).
- **Diplomatic Influence and ideological affinity are derived, not
  stored.** Diplomatic Influence is computed from this structure (exact
  formula TBD) rather than being its own tracked value. Ideological
  affinity between two factions is computed on the fly from their
  existing Economic/Social Ideology stats (a distance/similarity
  calculation) rather than needing a fourth stored dimension here —
  reuses data that already exists instead of duplicating it.
- **Updates flow through the same Need→Strategy→Resolution loop as
  everything else**, not a separate relationship-update system: every
  relevant interaction (trade, aid, conflict, espionage
  detection/attribution, treaty/alliance formation) adjusts one or more
  of the three dimensions as a standard output of that interaction's
  Resolution step. E.g. successful trade raises Trade (and likely a
  smaller Trust bump); a detected-and-resented covert action lowers
  Trust; war lowers Trust and raises Military Tension for both
  directions, plausibly asymmetrically depending on outcome (the loser's
  view of the winner likely shifts more than the reverse).
- **Resolved**: initialization on faction split — the new faction
  inherits its parent's existing values toward third parties, with its
  own relationship to the parent starting adjusted to reflect how
  contentious the split was; see Factions → "Faction formation
  (splitting)."

## Factions

Factions are the natural unit of multi-group dynamics (splitting,
merging, war, trade, exploitation) at *any* population scale — a large
society isn't simulated as one blob, it's simulated as one or more
factions, each composed of population units.

### Faction formation (splitting)

**Two independent trigger pathways**, coexisting rather than competing —
the same "two valid pathways to the same outcome" pattern already
established for war under Human-caused events:

1. **Unit-variance-driven (bottom-up)**: not a special hardcoded system —
   emerges naturally from the unit-variance math. If a unit's randomized
   roll on a value like happiness or ideology drifts far enough from its
   faction's norm, that's the mechanical trigger for it peeling off to
   found a new faction. Possible contributing factors: happiness
   divergence, geographic distance from the faction's center, resource
   competition, succession disputes, religious/ideological schisms.
2. **Political/social-stat-driven (top-down)**: the Faction Split entry
   in the Human-caused events self-event roster — a faction-level
   occurrence roll weighted by the faction's own stats (Charisma,
   Economic/Social Ideology, Religiosity, etc.), the same
   weighted-occurrence architecture as every other human-caused event,
   representing an acute political schism/coup-style fracture rather
   than gradual individual drift.

**Unified partitioning mechanic, regardless of which pathway triggered
the split**: once a split is triggered, which units end up in which
resulting faction is always resolved the same way — units are grouped
by whichever value(s) drove the divergence (e.g. a Social
Ideology-driven trigger partitions units roughly along their individual
Social Ideology values, above/below the parent faction's mean; a
geography-driven trigger partitions by distance from the faction
center). This reuses the per-unit data the unit-variance system already
tracks rather than needing a second, separate partitioning formula for
the top-down pathway.

**Relationship structure initialization on split** (resolving the
previously-open question under "Relationship structure
(faction-to-faction)"): the new (splinter) faction **inherits copies of
its parent's existing relationship values** toward third parties as a
starting point — the schism doesn't erase the society's prior history
with outside factions. The parent↔splinter relationship itself starts
**adjusted to reflect how contentious the split was**: a split triggered
by the top-down pathway with high Military Tension/ideological-conflict
inputs starts with a steep mutual Trust penalty; a split driven by the
bottom-up unit-variance pathway (gradual drift, resource competition)
starts comparatively milder. This reuses the triggering event's own
inputs as the contentiousness signal rather than needing a separate
formula.

### Core faction decision loop: Need → Strategy → Resolution

Every faction action — farming response, trade-seeking, war, negotiation,
espionage — runs through the same three-step loop, rather than each
interaction type being its own bespoke system:

1. **Need**: hard numbers establish what problem exists this cycle,
   independent of personality — hunger, resource deficit, security
   threat, ideological pressure, etc.
2. **Strategy**: the faction's personality/trait profile determines *how*
   it responds to that need. Examples:
   - Low food, high farming knowledge + low intelligence → farms harder,
     but poor at rationing/storage of surplus.
   - Low food, high farming knowledge + high intelligence → farms harder
     *and* rations/stores well.
   - Low food, low farming knowledge + high intelligence → seeks trade
     with surplus factions.
   - Low food, low farming knowledge + low intelligence → may suffer a
     Darwinistic decline, or (depending on other traits) resort to theft
     or conquest.
3. **Resolution**: hard numbers (run through the unit-variance system)
   plus the opposing faction's own numbers determine whether the chosen
   strategy actually succeeds.

This same loop governs war outcomes, negotiation success/terms (what
land/resources are gained or ceded for peace), alliance formation, and
espionage/political infiltration — infiltration does **not** require an
active war state to occur.

### Trait relevance by need domain

With the full Master stat list (draft) now in place, hand-authoring
every possible trait combination is combinatorially explosive (12+
Personality/Trait and Capability/Knowledge stats). Instead, each **Need
domain** has a small set of **relevant traits** that drive its Strategy
selection and Resolution — the same pattern the farming example above
already uses implicitly for the Hunger domain (Farming Knowledge +
Intelligence, not all 12 stats at once):

| Need domain | Relevant traits |
|---|---|
| Hunger / economic | Farming Knowledge, Work Ethic, Intelligence, Environmental Knowledge, Economic Ideology |
| Security / conflict | Strength, Military Knowledge/Capacity, Risk Tolerance, Intelligence |
| Diplomatic / social (negotiation, alliance-seeking, trade beyond pure economics) | Charisma, Diplomatic Knowledge, Communication Knowledge, Trust (relationship structure) |
| Ideological / religious (unrest, schism pressure) | Religiosity, Social Ideology, Economic Ideology |
| Medical / health | Medical Knowledge, Health, Education Level |
| Technological / cultural development | Technology Knowledge, Intelligence, Risk Tolerance, Economic Ideology |

**Worked example — Security domain** (same four-quadrant structure as
the farming example, now for a different domain):
- Security threat, high Strength + high Risk Tolerance + low Diplomatic
  Knowledge → confronts directly, accepting higher casualty risk.
- Security threat, high Strength + low Risk Tolerance → fortifies/
  defends rather than initiating engagement.
- Security threat, low Strength + high Charisma + high Diplomatic
  Knowledge → seeks negotiation/alliance instead of fighting.
- Security threat, low Strength + low Charisma + low everything else
  relevant → may suffer a Darwinistic decline, or become a vassal
  (echoing the existing Subjugation/vassalage outcome under Conflict,
  diplomacy, and covert action → Outcomes of conflict).

Exact weighting formulas for how each domain's relevant traits combine
(which trait dominates Strategy *choice* vs. which affects Resolution
*success probability*) are still TBD — this locks in *which traits
matter to which domain*, not their exact math, same pattern as
elsewhere in this doc.

### Need prioritization

When multiple needs compete in the same cycle (e.g. low food *and* a
security threat *and* ideological unrest at once), **personality traits
determine ranking**, not a fixed universal priority order:
- Low intelligence + strong farming skill + high religiosity → weights
  hunger and population growth heavily, deprioritizes
  education/medical/political development.
- High intelligence + low farming skill → ranks needs more effectively
  overall, but may prioritize trade and *smaller* population growth, even
  at the risk of population decline (real-world analogue: modern Japan).

### Conflict, diplomacy, and covert action

- **Combat**: success/failure driven by hard numbers (population,
  military capacity, tech) *and* personality traits, resolved through the
  variance system.
- **Negotiation**: willingness to negotiate, and success/terms of peace
  (land/resource gains or concessions) are also Need→Strategy→Resolution
  outcomes.
- **Alliances**: formed through the same loop — not a separate system.
- **Espionage / political infiltration**: available at any time, war or
  peace. Can affect a wide range of things in the target faction —
  ideology, political makeup, technology theft, intelligence-gathering
  about the target's true (hidden) numbers, etc.
  - **Detection**: whether the target notices the change happening (e.g.
    "our people are becoming more religious") is a separate roll from
    **attribution** (realizing another faction caused it).
  - If undetected/unattributed, the effect just quietly reshapes the
    target faction's internal numbers with no relationship consequence.
  - If detected and attributed, the target's reaction is itself another
    Need→Strategy→Resolution decision, filtered through personality: they
    might welcome the imposed change (relationship improves) or resent it
    (relationship deteriorates) — reaction is not automatically hostile.

### Complexity gating on faction interactions

- **Low complexity**: conflict/interaction outcomes driven mainly by
  proximity, troop/population counts, and technology level.
- **High complexity**: adds terrain considerations and third-party
  faction intervention — an uninvolved faction choosing to get involved
  politically, militarily, or covertly (subterfuge) in another pair's
  conflict. Unlocks at **complexity tier 8**, per Complexity tier scale
  (1-10).

### Third-party faction intervention

Not a separate system — an uninvolved faction's decision to intervene in
another pair's ongoing conflict is itself another instance of the
standard Need→Strategy→Resolution loop, run by the intervening faction
each cycle a nearby conflict is active:

- **Need**: assembled from the intervening faction's relationship to
  both combatants (Trust/Military Tension toward each, from the
  Relationship structure), its ideological affinity with either side
  (derived from Economic/Social Ideology, per the relationship
  structure's existing affinity calculation), and its own
  opportunity/threat perception (a decisive win by either side could
  threaten or benefit the third party). This echoes the existing
  "downward influence... overridden by need" pattern under Hierarchical
  Stat Aggregation — a faction can act on another's behalf even absent a
  strong prior relationship, if its own Need evaluation determines the
  stakes matter enough.
- **Strategy**: the intervening faction's trait profile (Risk Tolerance,
  Charisma/Diplomatic Knowledge, Military Knowledge/Capacity,
  Economic/Social Ideology — per Trait relevance by need domain) 
  determines the *form* intervention takes: military support for one
  side, diplomatic mediation, economic pressure, or covert support via
  espionage.
- **Resolution**: folds the third party's numbers into the
  **already-existing** combat/negotiation/espionage resolution
  machinery under Conflict, diplomacy, and covert action — intervention
  doesn't need its own resolution mechanic, just an additional
  participant in the existing one.

### Outcomes of conflict (loser's fate)

Multiple outcomes coexist rather than a single resolution type:
- **Full absorption**: losing faction's units join the winner, keeping
  their persistent base stats (this is how empires grow).
- **Partial absorption**: some units join the winner; others flee to form
  a refugee faction or join a third faction.
- **Extermination**: population loss with no absorption.
- **Subjugation/vassalage**: loser remains nominally independent but
  becomes a vassal — a *standing relationship state* (not a one-time
  event) involving recurring tribute/resource drain, with a possibility
  of rebellion later if the power gap narrows.

## Technology & Culture Evolution

Growth (or regression) in technology and culture uses the same
Need→Strategy→Resolution loop as everything else — it is not a separate
system. Hard numbers and needs establish pressure to develop in a given
area; personality traits (intelligence, ideology, risk tolerance, etc.)
determine *which* areas a faction actually invests in and how
effectively; resolution (with unit variance) determines how much growth
actually occurs in a cycle. This is the same mechanism that drives farming
response, trade-seeking, war, and diplomacy — direction and magnitude of
tech/culture change simply becomes one more output of the loop applied to
a different domain.

### Complexity-gated stat granularity (general pattern, not just tech)

Rather than having low complexity literally lack detailed sub-stats, the
**full detailed breakdown always exists in the data model** — complexity
only governs whether those sub-stats are allowed to diverge independently
from the tracked aggregate score, or get collapsed back into it each
cycle. This pattern is likely reusable anywhere the sim has an aggregate
score backed by finer detail (culture score, and potentially other
composite stats), not just technology:

- **Lowest complexity**: the aggregate (e.g. Technology = 50) is applied
  uniformly to every detailed sub-stat at the start of a cycle.
  Simulation formulas act on those (initially identical) detailed values
  during the cycle, causing them to diverge. At cycle end, the aggregate
  is recalculated as the average of the detailed values, and then *every*
  detailed value is reset back to that new aggregate for the next cycle —
  so no sub-stat differentiation ever persists between cycles.
- **Medium complexity**: a subset of detailed stats (however many the
  complexity tier unlocks) are allowed to persist their own independent
  value across cycles instead of resetting. Stats that remain locked
  still reset to the newly recalculated aggregate each cycle.
- **Highest complexity**: the aggregate is still calculated as the
  average of the detailed values, but purely for tracking/graphing/UI
  purposes — it has zero effect on any individual detailed value. Every
  detailed stat is fully independently tracked.

### Regression

At the end of each cycle, every stat undergoes an independent regression
check:

- A **regression risk** (probability that stat regresses at all this
  cycle) is computed from factors including: time elapsed since that
  stat's last regression event (models the "aging out" of skilled
  individuals — risk accumulates the longer it's been), the education
  level of the owning faction/society (better knowledge retention and
  replacement lowers risk), and a general **chaos constant** representing
  baseline entropy/randomness (see the generalized, cross-cutting version
  of this constant under Disasters & Events).
- Example mechanic: if the computed risk is 5%, the stat regresses this
  cycle if a 1–100 roll lands in the top 5% of the range (96–100).
- The same factors also govern regression **magnitude**, not just
  likelihood — e.g. higher education narrows the possible severity range
  when a regression does occur.
- A stat's own current strength is self-reinforcing: high skill in an
  area *reduces* both the chance and severity of regression in that same
  area, while an already-weak stat is *more* prone to further regression
  — a compounding "rich get richer, poor get poorer" dynamic.

## Hierarchical Stat Aggregation & Cross-Level Influence

Stats exist at three nested levels — **unit → faction → society** — and
values flow both up (aggregation) and down (influence on outcomes),
rather than higher levels being pure reporting rollups of lower ones.

### Upward aggregation is weighted, not just population-based

- A faction's stat is a **weighted** aggregate of its units' values; a
  society's stat is a weighted aggregate of its factions' (and
  transitively units') values.
- The weight isn't purely proportional to population size — influence and
  relationship strength matter too. Example: a mid-sized faction with a
  high religion score and strong diplomatic influence over other factions
  can outweigh a larger faction with a lower religion score and weak
  influence, when computing the society-wide religion number. The same
  weighting concept applies one level down: a unit's influence on its
  faction's score isn't purely proportional to that unit's population
  either.

### Downward influence on outcomes is gated by relationship, but can be overridden by need

- When resolving an effect on a specific unit or faction, the relevant
  higher-level number(s) can pull that outcome up or down — a faction
  with weak individual farming/intelligence stats can have its
  farming-related outcomes boosted by drawing on a strong society-wide
  farming stat.
- How much a higher level's strength can influence a lower-level outcome
  is gated by the **relationship level** between them: a strong
  relationship (faction-to-society or faction-to-faction) makes the
  higher-level entity more likely to render aid/support, pulling the
  outcome toward the stronger number; a weak relationship makes aid less
  likely, leaving the outcome closer to the lower entity's own local
  numbers.
- This relationship gate can be **overridden by need**, even under poor
  relationships: if a higher-level actor's own Need→Strategy→Resolution
  evaluation determines that a lower entity holds a scarce or critical
  capability, it may still render aid despite weak relations, because
  losing that capability would hurt the larger group. Rendering aid is
  itself just another instance of the same universal decision loop, run
  by the aid-giving entity.

## Visualization / UI (concept-level, not final)

- **Primary view**: a randomized map generated from all the environment,
  size, and starting-society variables. The user watches the society
  (and any factions it splits into) grow, shrink, and evolve on this map
  over time.
- Live-tracked high-level stats while watching: technology level,
  society happiness, hunger, population, etc.

### Overall window layout

Sidebar and bottom bar have distinct, non-overlapping purposes rather
than mixing stats and controls together:

- **Sidebar** (fixed-width, e.g. right side) — **informational only**:
  the live high-level stat readout (technology level, happiness, hunger,
  population, etc.) plus likely a faction list/legend. Scales cleanly if
  more stats get added later without crowding the map.
- **Bottom bar** (full width) — **controls only**: playback controls
  (play/pause/step, speed), current simulated year/cycle indicator.
- **Map** fills the remaining central space.
- The Tab-key **detail view** (see below) is a separate full view
  toggled on top of/in place of this layout, not a permanent part of it.

### Main map view

- **Map size and grid resolution**: map size (physical territory scale)
  is set independently of population (see Setup / Configuration).
  **Cell resolution — how much area one cell represents — is a fixed
  engine constant, not gated by complexity level**, unlike nearly
  everything else in this doc. A larger map size means proportionally
  **more cells at the same fixed resolution**, not fewer/coarser cells;
  complexity level has no effect on grid fidelity at all. The existing
  soft-performance-warning pattern (already used for high unit counts)
  applies here too — a very large map size produces a large cell count,
  which should warn rather than hard-cap.
- **Square grid cells for v1.** Pairs naturally with the existing
  elevation/gradient-driven disaster spread model. Hex grid (uniform
  adjacency distance in every direction, no diagonal ambiguity) is a
  deferred idea, not a v1 commitment — see `FUTURE.md`.
- **Occupancy vs. territory are distinct concepts**, both rendered as
  separate visual layers on the map:
  - **Occupancy**: which cells actually have units living in them. A
    cell can hold **multiple unique units simultaneously** — not a
    classic-4X one-unit-per-cell limit. Rendered as markers/density
    indicators on occupied cells only.
  - **Territory**: a faction's claimed/controlled boundary, which is
    broader than just its occupied cells — it can include unoccupied
    land the faction controls (farmland, wilderness, resource claims,
    buffer zones). Rendered as a translucent per-faction color tint or
    border outline across the whole claimed region, independent of
    where markers show actual occupancy within it.
  - This occupancy/territory split also clarifies disaster resolution
    at the cell level (see Disasters & Events → Spatial model): a
    disaster striking an occupied cell directly affects every unit
    anchored there (each still gets its own independent
    Need→Strategy→Resolution/variance resolution); a disaster striking
    an unoccupied-but-claimed cell affects the owning faction's
    resources/territory rather than any specific unit.
- **Cell rendering derives from terrain element composition**, not a
  single flat terrain-type color: base color from the dominant Ground
  composition element (e.g. Sand → tan, Rock → gray), a texture/icon
  overlay for Vegetation cover (tree symbols for Forest, grass texture
  for Grass/Scrubland), shading for Topography (hillshading for
  Hills/Mountains), and a distinct overlay tint for Water features
  (Coastline, River/Lake, Wetland). Exact color/icon mapping is TBD —
  this is an art-direction pass, not an architecture decision.
- **Disaster occurrences are shown as an overlay/highlight on the
  specific cells struck that cycle** (icon or highlight color),
  consistent with the already-decided redrawn-per-cycle (not
  continuously animated) JavaFX `Canvas` rendering approach.

### Playback controls

Bottom bar contents (see "Overall window layout"): play/pause/step,
speed control, and current year/cycle indicator, plus event-driven
auto-pause:

- **Auto-pause on significant events by default** — the sim pauses
  itself when a disaster, faction split, war, or other significant event
  occurs, so nothing gets missed if the user isn't watching closely at
  higher speeds. The user resumes manually (Play) after reviewing it.
- **Per-event-type configurable** — a settings toggle lets the user
  choose which event types trigger a pause (e.g. pause on disasters but
  not on minor faction squabbles).
- **Global override** — a separate top-level toggle can turn
  auto-pause off entirely, regardless of the per-type settings, for
  users who'd rather just watch the sim run uninterrupted and review
  everything after the fact via the detail/spreadsheet view's history.

### Detail / spreadsheet view

Toggled with Tab (see "Overall window layout" — this replaces/overlays
the main map layout rather than being a permanent part of it): a
spreadsheet-style breakdown of simulation history — fine-grained data
and graphs, likely at a per-faction, per-year granularity, given the
faction/unit-based data model.

**Navigation**: a persistent collapsible tree sidebar (Society →
Factions → Units) alongside a detail pane — selecting any node in the
tree updates the detail pane's tables/charts to that level, letting the
user jump directly to any faction or unit without drilling through
parents first. JavaFX `TreeView` is the natural fit alongside the
already-decided `TableView`/`Chart` classes for this view.

### Setup / configuration screen

**Complexity-first, then an adaptive form**: the user picks complexity
level as the very first step, and that choice then reveals a tailored
form showing only the fields/ranges that tier unlocks — society size,
starting era, environment variables (including terrain archetype
presets, and at higher complexity the underlying terrain composition
elements directly), society starting traits, and unique unit count
range, per the existing Complexity level — unified definition. This
reinforces complexity as the central knob of the whole setup process,
rather than one setting among many.

- **Changing complexity after entering values is an accepted rough
  edge**: since the form itself is generated from the complexity choice,
  lowering complexity after fields are already filled in can leave
  values outside the newly-allowed range. Exact behavior (clamp values
  back into range silently, warn and ask before applying, preserve
  hidden values in case the user raises complexity back up) is TBD.

### End-of-run summary screen

When a run ends (Win/Loss Conditions — population loss, or the
simulation timer expiring), playback stops and a **dedicated summary
screen replaces the map view** until dismissed:

- **Outcome**: survived vs. died, framed per Win/Loss Conditions as not
  strictly binary — the summary should communicate *how* the society
  changed, not just whether it lived.
- **Timeline/story recap** of major events across the run (disasters,
  faction splits, wars, technological/cultural shifts).
- **Key stat graphs** over the full run (population, happiness,
  technology, etc.), reusing the same JavaFX `Chart` classes as the
  Tab-key detail view.
- **Faction outcomes**: what the society fragmented into (if it did),
  and each faction's own fate.
- From here the user can presumably return to setup (e.g. to tweak and
  re-run) or drill into the full detail/spreadsheet view for anything
  the summary doesn't cover — exact navigation/actions available from
  this screen are TBD.

## Disasters & Events

### Natural disasters

- Each cycle, natural disasters have a chance to occur, **weighted by the
  aggregate terrain *elements*** across the map — not by the terrain
  archetype's name. The archetype label (Desert, Tundra, etc.) is purely
  a setup-time UI convenience/preset; the simulation's disaster math
  never reads it, only the underlying element composition it seeded. See
  "Spatial model: map grid and per-cell elements" below.
- Each disaster type also has its own **impact-scope weighting** — a
  separate distribution governing how localized or widespread a given
  occurrence is, now resolved spatially: given an occurrence, *which*
  grid cells actually get struck (and how severely) is its own roll
  driven by each cell's *local* element composition, not a uniform
  roll across the map. The same disaster type can still vary widely in
  scope (a typical tornado is localized, but a long-track tornado covers
  much more ground) — see the spatial model below for how that's
  resolved per-cell.
- **Multiple disasters can occur in the same cycle.**
- Disasters affect each other's weights within the same cycle — e.g. a
  hurricane occurring should sharply reduce that cycle's drought weight.
  A full cross-disaster weight-interaction mapping will need to be
  defined once disaster types are finalized; see "Secondary/cascading
  disasters" below for the example pairs recorded so far.
- A disaster occurring reduces the chance of that **same** disaster type
  repeating in the immediately following cycles, decaying back *up*
  toward its normal baseline weight over time (a cooldown/decay curve,
  not a hard lockout). **Exception: Earthquake** — an occurrence briefly
  *elevates* its own repeat-weight first (aftershocks) before decaying
  back *down* to baseline, the reverse of the general pattern.
  - The elevated repeat-weight governs only whether an aftershock
    *occurs* — it does **not** bias the aftershock toward the mainshock's
    strength. A separate, independent roll against the Richter/Moment
    Magnitude severity scale determines aftershock magnitude, and that
    roll is weighted **toward lower magnitudes than the mainshock**: per
    USGS, an aftershock matching or exceeding the mainshock's magnitude
    (foreshocks/pre-tremors excluded — those are a separate, unmodeled
    concept) happens only ~5% of the time. Exact shape of the
    below-mainshock portion of that distribution is still TBD.

**Disaster roster** (natural disasters only — human-caused events, see
below, use separate formulas): Drought, Sandstorm, Hurricane/Cyclone,
Flood, Earthquake, Wildfire, Blizzard, Tornado, Volcanic eruption,
Landslide, Tsunami, Avalanche, Sinkhole, Extreme heat, Meteor, and
Epidemic (with plant/crop, animal/livestock, and human variants sharing
the same mechanical scaffolding but hitting different stats — crop
epidemics hit farming yield, livestock epidemics hit food
supply/economy, human epidemics hit population/labor directly).

### Spatial model: map grid and per-cell elements

The map is a **grid of cells**, and the Terrain composition elements
(see Setup / Configuration) are tracked **per cell**, not just as one
composition for the whole map. A terrain archetype preset seeds the
*procedural generation* of per-cell element values across the grid (so
a "Mountain/highland" map ends up with individual cells whose elements
vary — some cells rolling heavy Mountains/Rock, others Hills or Valley —
rather than every cell being identical), and from that point on the
simulation only ever reads per-cell elements.

**Generation algorithm: noise-based (Perlin/simplex).** Each terrain
element category (Topography, Ground composition, Vegetation cover,
Water features) gets its own noise field across the grid, rather than
each cell independently rolling its elements with no relationship to
its neighbors — this produces natural-looking clusters and gradients
(a contiguous mountain range, a patch of Forest) instead of
salt-and-pepper noise. Consistent with the config schema's "inputs
only, regenerate deterministically" decision, the noise fields are
seeded from the run's own seed — the same config always procedurally
generates the identical map. Each field is biased/scaled toward the
archetype's target element-composition weights (see Weight-sourcing
methodology) once those are numerically defined; some cross-category
correlation (e.g. Mountains topography co-occurring with Rock ground
composition, matching how real reference regions actually look) is
desirable but left as a deferred refinement rather than specified now —
exact correlation mechanics are numeric-tuning-level detail, same
pattern as the rest of this doc.

**Zone shape is a real tuning target, not just zone size.** Confirmed by
eyeballing the raw two-element Perlin blend at scale (via the
`renderPreview` dev tool — see ASSETS.md): its boundary contours default
to long, thin, winding "spaghetti" shapes threading across the map, not
the chunky, roughly-blob-shaped regions a real terrain archetype should
read as (a contiguous desert region, a forest patch). **Only features that
are inherently linear in reality — rivers, canyons, coastlines — should
end up looking like that**; most element zones (ground composition types,
vegetation patches) should default to blobbier, more compact shapes. This
needs to be corrected as part of the same deferred numeric-tuning pass as
the archetype weights and cross-category correlation above (e.g. blending
in a second, lower-frequency noise field to bias toward broader/rounder
regions, or domain-warping the sample coordinates) — not solved now, but
recorded here so it isn't lost or mistaken for "working as intended" once
real map layouts (not just the isolated single-category preview tool) are
being judged.

**Coastline and River/Lake-adjacent are boundary-line features, not
blended-area elements like the rest of Water features.** The generic
"one noise field per category, blend the two nearest elements" model
(above) works for area-like elements — Wetland-saturated is a patch, like
a Forest/Trees zone — but breaks down for these two specifically:

- **Orientation is arbitrary and changes over short distances.** A real
  coastline or river doesn't have a "typical" angle the way a flat tile
  texture implicitly does, and it can bend sharply within the space of a
  single cell. A repeating texture tile (the approach every other element
  uses) can't represent that without visible seams or an obviously
  repeating pattern.
- **What's on each side varies enormously and independently of the line
  itself.** The land side could be any Ground composition/Vegetation
  combination; the water side isn't visually uniform either (a Caribbean
  coastline and the Mississippi River don't look alike) — a single flat
  "coastline" or "river" texture asset can't capture that variation, and
  trying to would mean an unbounded number of texture variants for every
  plausible land/water pairing.

**Decided instead**: Coastline and River/Lake-adjacent are **computed
boundary lines**, not weighted noise elements. Each is drawn as a line
between the two known points where it crosses its cell's border (the
matching points on whichever neighboring cells it connects to, so the
line is continuous across cell boundaries rather than independently
regenerated per cell) — plausibly with some randomized waviness so it
doesn't read as a perfectly straight edge. Rather than needing dedicated
"coastline"/"river" art at all, **each side of the line just renders using
whatever elements actually exist there already** — the land side reads as
its own local Ground composition/Vegetation blend, the water side reads
as a **water look matching whichever boundary type put it there** (see
"Water look by boundary type" below), not one flat tint for all water.
This also resolves a latent mismatch with how the rest of this doc already
treats Coastline/River: Tsunami's spread pattern already talks about
"travel along Coastline cells" and Epidemic's is
"Contact/connectivity-driven" — both already implicitly treat these as
adjacency/path concepts, not composition percentages, so this decision
brings the terrain-element model in line with how disaster mechanics were
already using it.

**Water look by boundary type**: **decided — water gets more than one
visual look, not a single flat tint.** At minimum, Coastline gets an
open-water/ocean look and River/Lake-adjacent gets a river/lake look,
each its own art (color/texture) rather than one generic "water" asset
covering every boundary type — directly addressing the Caribbean-vs-
Mississippi observation above. **Still open**: whether River/Lake-adjacent
itself should eventually split into two separate elements (a river and a
lake don't necessarily look alike either), or share one "river/lake" look
for now — not a blocker, since it only affects how many distinct water
looks exist, not the boundary-line mechanism itself.

**Wetland-saturated and Landlocked are unaffected** — Wetland is an area
element like any other (see ASSETS.md), and Landlocked is simply the
"no water feature here" baseline, same as Barren/None for Vegetation
cover.

**Not yet decided (design only for now, not implemented)**: the exact
line-generation algorithm (how entry/exit points get chosen and kept
consistent with neighboring cells so a coastline/river reads as one
continuous path across the whole map, how much waviness to add, how this
interacts with the per-cell rendering pipeline that otherwise samples
[TerrainField] continuously) is deferred, along with actually building it
— this section locks in the *approach*, not the algorithm, same pattern
as the rest of this doc's "spread pattern taxonomy, not exact spread
math" style decisions.

- **Occurrence (macro roll)**: whether a disaster type triggers this
  cycle at all is weighted by the aggregate of element values across
  the whole map (e.g. summed/averaged Coastline exposure map-wide
  drives overall hurricane occurrence chance).
- **Strike location (micro roll)**: given a disaster occurs, which
  cells it actually hits — and how severely — is resolved from each
  candidate cell's *own local* element composition, not a uniform
  roll. This is where the existing impact-scope weighting concept
  becomes spatial: scope is no longer an abstract number, it's the
  actual set of cells the disaster's spread pattern reaches.
- **Origination vs. spread are separate, and can point in different
  directions.** Worked example: **Avalanche** — origination weight is
  high at steep, high-elevation cells (Mountains + minimal Forest cover
  to anchor snowpack), but *impact* weight is low right at the origin
  and grows moving downhill as the slide gathers mass, following the
  local elevation gradient outward from the origin cell.

### Spread pattern taxonomy

Rather than a bespoke spread rule per disaster type, Avalanche's
origin-vs-spread structure generalizes into six reusable pattern types.
Every disaster type is tagged against one (or, for two types, a
composite of two):

| Pattern | Behavior | Disaster types |
|---|---|---|
| **Gradient flow** | Origin point; impact grows moving downhill along the local elevation gradient, away from the origin | Avalanche, Flood, Landslide, Volcanic eruption (lava component) |
| **Radial falloff** | Origin point; intensity decreases with radial distance in all directions | Earthquake, Meteor |
| **Directional track** | Origin point; moves along a path in a dominant direction (wind, storm movement), weakening over distance/time | Hurricane/Cyclone, Tornado, Wildfire, Volcanic eruption (ashfall component) |
| **Broad area** | No single origin point; affects a contiguous region weighted by terrain suitability, no directional growth | Drought, Extreme heat, Blizzard |
| **Point/localized** | Minimal-to-no spread beyond the origin cell or a tight cluster | Sinkhole |
| **Contact/connectivity-driven** | Spreads along occupancy/trade proximity between cells and factions, not a physical terrain gradient | Epidemic (all variants) |

- **Composites**: **Tsunami** combines Radial falloff (from the
  triggering Earthquake or Meteor's origin) with coastal-adjacency
  propagation (travels along Coastline cells, inland penetration weighted
  by low elevation/proximity to coast). **Volcanic eruption** combines
  Gradient flow (lava, downhill from the peak) with Directional track
  (ash, wind-driven) as two simultaneous spread effects from one
  origination event.
- **Sandstorm** is grouped with Wildfire under Directional track (both
  wind-driven), though its origination driver is Sand/Barren rather than
  Forest/Grass fuel.
- Exact per-pattern math (how quickly Gradient flow decays with
  distance, how wide a Directional track's corridor is, etc.) is still
  TBD — this locks in *which pattern* each disaster type uses, not the
  formulas themselves.

### Severity scales

Every disaster type has an associated intensity/magnitude scale that
feeds its damage and impact-scope calculations. Real-world named scales
are reused where one exists and is a good fit; disaster types without a
well-known public scale get simple in-sim severity tiers instead. Exact
tier boundaries/thresholds are still TBD.

| Disaster | Scale |
|---|---|
| Earthquake | Richter / Moment Magnitude |
| Tornado | Enhanced Fujita (EF) |
| Hurricane/Cyclone | Saffir-Simpson (Category 1-5) |
| Volcanic eruption | Volcanic Explosivity Index (VEI) |
| Avalanche | European Avalanche Danger Scale (1-5) |
| Extreme heat | Heat Index tiers |
| Meteor | Torino Scale (pre-impact hazard) informing an in-sim impact-energy tier at time of occurrence |
| Tsunami | Wave-height tiers, often derived from the triggering earthquake's magnitude |
| Blizzard | Regional Snowfall Index-style tiers |
| Drought | Palmer Drought Severity Index-style tiers |
| Flood, Wildfire, Landslide, Sinkhole | In-sim severity tiers — no single widely-adopted public scale exists |
| Epidemic (any variant) | In-sim contagion/severity tiers (spread rate × lethality, loosely inspired by R0/case-fatality-rate) |

### Secondary/cascading disasters

Generalizes the same-cycle weight-interaction rule above: one disaster
occurring doesn't just suppress/boost *its own* type's future weight —
it can raise the weight of *other* disaster types, in the same cycle or
following cycles, via the same acute/elevated-need mechanics as
"Needs generated by a disaster" below. Example pairs recorded so far
(not exhaustive — full mapping is still an open TBD item):

- **Hurricane/Cyclone** → ↑ Flood, Tornado, Landslide
- **Earthquake** → ↑ Tsunami (coastal terrain), Landslide, plus its own
  aftershock exception noted above
- **Volcanic eruption** → ↑ Earthquake, Wildfire, Tsunami (if coastal),
  Extreme heat (localized)
- **Wildfire** → ↑ Landslide (burn-scarred slopes, in following cycles),
  minor ↑ Sinkhole
- **Drought** → ↑ Wildfire, Extreme heat, Sinkhole
- **Flood** → ↑ Sinkhole, Landslide, Epidemic (waterborne disease)
- **Blizzard** → ↑ Avalanche
- **Extreme heat** → ↑ Wildfire, Drought, Epidemic (heat stress)
- **Meteor** (large impacts only) → ↑ Tsunami (ocean impact), Wildfire,
  Earthquake (seismic shock)
- **Generic rule**: any major disaster, regardless of type, ↑ Epidemic
  risk for several cycles afterward (population displacement, sanitation
  breakdown) — on top of the specific pairs above.

### Weight-sourcing methodology

Since disaster math now runs on terrain **elements**, not archetype
labels (see "Spatial model: map grid and per-cell elements"), the goal
of this methodology is to derive a per-*element* weight for each
disaster type, not a per-archetype one. Real-world historical data is
inherently regional, though, so getting there means comparing multiple
reference regions with *different element mixes* and statistically
isolating each individual element's contribution — e.g. Sahara and
Sonoran are both heavily Sand, but differ in Hills/Rock proportion;
comparing their sandstorm frequency against that difference helps
isolate what Sand alone contributes versus what Hills contributes.

- **EM-DAT** (CRED's international disaster database) as the primary
  source — broad country-level historical event counts across most
  disaster types, good for cross-region comparison.
- **USGS** earthquake/volcano catalogs as a supplement for geophysical
  events where EM-DAT is too coarse.
- **NOAA Storm Events Database** as a supplement for severe-weather
  granularity (tornado, blizzard) using a well-documented reference
  region.
- Each real-world reference region is itself broken down into its own
  approximate element composition (e.g. Sonoran ≈ mostly Sand + some
  Hills/Rock, minimal Forest), giving a set of (element mix →
  disaster-frequency) data points across regions to regress per-element
  weights from, rather than assigning one region's raw data directly to
  one archetype.
- The terrain archetype list below still matters — as the reference-data
  source set feeding that regression, and as the setup-time preset labels
  that seed procedural per-cell element generation — even though the
  simulation itself no longer keys disaster weights off the archetype
  name directly.

**Terrain archetype list (finalized) — reference regions for the element-weight regression above, and setup-time preset labels:**

| Terrain archetype | Reference region(s) |
|---|---|
| Desert | Sahara, Sonoran |
| Tundra | Siberian tundra, Arctic Canada |
| Tropical/Rainforest | Amazon basin, Congo basin |
| Temperate forest/plains | Central Europe, Eastern US |
| Coastal | US Gulf Coast, Southeast Asia coastline |
| Mountain/highland | Himalayas, Andes |
| Grassland/savanna | African savanna (Serengeti), US Great Plains |
| Island | Philippines, Caribbean (captures volcanic, hurricane, and earthquake exposure together) |
| Wetland/Swamp | Mississippi Delta, Sundarbans |

Wetland/Swamp was added specifically to give flood, sinkhole, and
epidemic disaster weights a terrain where they're the dominant risk,
rather than being spread thin across Coastal and Tropical/Rainforest.

### Epidemic mechanics

Epidemics (Crop, Livestock, Human — see the disaster roster above) get
two pieces of dedicated design beyond the general disaster machinery,
since disease behaves differently from an instant-impact event like an
earthquake or tornado:

**Variant roster and spillover.** Each variant is its own independent
roster entry — its own baseline occurrence weight, repeat-suppression/
decay curve, and severity/contagion roll — not one shared roll split
three ways. Cross-variant spillover is asymmetric and modeled through
the existing Secondary/cascading disasters mechanism rather than a new
system: Livestock epidemic meaningfully ↑ Human epidemic weight
(zoonotic spillover); Human → Livestock is much weaker (reverse
zoonosis is real but rare); Crop epidemic doesn't spill into the other
two directly (plant pathogens don't cross into animals/humans) — instead
it depresses food supply, which combines with the generic "any major
disaster raises epidemic risk" rule to mildly raise Human epidemic
weight through a famine/malnutrition pathway rather than a direct
pathogen jump. A spillover only affects whether the second epidemic
*occurs* — its severity is always its own independent roll, same
principle as the Earthquake aftershock exception above.

**Multi-cycle progression curve.** Unlike instant-impact disasters, an
active epidemic persists and evolves cycle to cycle rather than
resolving once. It's run through the same Need→Strategy→Resolution loop
as everything else each cycle it remains active, rather than a bespoke
epidemiological model:

1. **Need**: the epidemic's current case count/severity establishes this
   cycle's pressure.
2. **Strategy**: the affected faction's stat profile shapes its
   response — medicine/technology level, intelligence, and cultural
   acceptance of containment measures (a medically capable society can
   still respond poorly if its culture resists quarantine-style
   measures).
3. **Resolution**: this cycle's transmission change is resolved from
   strategy effectiveness, **natural immunity buildup** (grows the
   longer the epidemic has run, pulling transmission down over time),
   and the chaos constant — together producing the general real-world
   epidemic curve shape: an initial rise (weak containment, low
   immunity), a peak, then a decline as immunity and effective response
   begin to outweigh transmission.
4. **Mutation spikes**: each active cycle also carries a
   chaos-constant-weighted chance of a mutation event that resets or
   boosts the contagion rate, producing a renewed climb ("second wave")
   even after decline has begun — a distinct roll layered on top of the
   normal cycle-to-cycle resolution, not a new epidemic type or roster
   entry.

### Needs generated by a disaster

A disaster produces multiple effects at once, not just one:
1. **Direct resolution/damage** applied at the time of the event
   (population loss, resource/infrastructure destruction).
2. An **immediate acute need** (emergency food/water/shelter/medical
   demand), handled through the normal Need→Strategy→Resolution loop like
   any other need.
3. A **longer-duration elevated need** that persists and decays over
   several cycles after the initial event — e.g. a hurricane's immediate
   emergency passes, but food/shelter/medical strain remains elevated for
   some time afterward.

### Severity and recovery

A faction's relevant stats affect both how severely a disaster hits it
and how well/quickly it recovers, using the *same* variance-band and
complexity-gated subroutine machinery already defined elsewhere (see
Population Model and Complexity level — unified definition) rather than
a disaster-specific calculation system. Exact formulas are still TBD.

### Universal chaos constant (cross-cutting rule)

Generalizing the chaos constant introduced under Regression: **every
probability calculation in the simulation carries a non-zero floor**,
no matter how small. Nothing in the sim is ever a true 0% or 100%
outcome — even astronomically unlikely events (e.g. a 1-in-a-billion
chance) always remain technically possible.

### Human-caused events

Human-driven crises use the **same weighted-occurrence architecture as
natural disasters** (roster, cycle-based occurrence roll, cascading
interactions), but the weight source is different: instead of terrain
elements, weights are drawn from the faction/society's own **flat stats**
(Charisma, Technology, Hunger, Religiosity, etc. — see Master stat list)
for self events, and from each faction's stats plus the **pairwise
relationship structure** (see Master stat list) for events against
others. They are **not** additional entries in the natural-disaster
weight table — a separate roster and weight model, same underlying
architecture pattern.

**Roster, split into two categories:**

1. **Self events** — occur to/within one faction, not directly targeting
   another: Government Overthrow/Coup, Martial Law, Civil
   Unrest/Rebellion, Recession, Famine Deaths, Crime Wave, and a
   political/social-stat-driven **Faction Split** — a second,
   independent trigger pathway alongside the existing
   unit-variance-driven split mechanism, both coexisting; see Factions →
   "Faction formation (splitting)" for how they interact and how units
   are partitioned once triggered.
2. **Events against others** — inherently pairwise, involving two or
   more factions: War, Espionage, Treaty/Alliance offer, Unification.

**Occurrence vs. resolution reuses the natural-disaster pattern:**
- *Self events*: an occurrence roll (weighted by the faction's own
  stats) triggers the event, which produces direct effects and
  generates needs resolved through the normal Need→Strategy→Resolution
  loop — the same "Needs generated by a disaster" pattern (direct
  resolution, acute need, longer-duration elevated need), just with a
  stat-weighted trigger instead of a terrain-element-weighted one.
- *Events against others*: the occurrence roll (weighted by the
  relationship structure plus both factions' stats) determines whether a
  war/espionage/treaty situation arises between two factions this cycle.
  Once triggered, it resolves through the **already-existing** Core
  faction decision loop and Conflict, diplomacy, and covert action
  mechanics under Factions — this system supplies the *trigger*, not a
  separate resolution mechanic.

**Two valid pathways to the same outcomes.** A faction can also
*deliberately choose* war/conquest/negotiation as its Strategy in direct
response to an existing Need (already documented under Factions — e.g.
"low food, low farming knowledge + low intelligence → may... resort to
theft or conquest"). This human-caused event system adds a second,
probabilistic pathway to the same outcomes — background stat/
relationship pressure erupting into conflict or unrest without one
specific triggering need, the way a disaster "just happens" rather than
being chosen. Both coexist as valid causes of the same event types.

**Natural ↔ human-event feedback is now bidirectional, resolving a
previously-open question** — without needing an explicit disaster→
human-event weight table entry. Human factors (society/faction
conditions, decisions, policies) already **influence natural disaster
weights** (e.g. poor land stewardship raising drought/famine risk,
element-weighted). The reverse now falls out **naturally from the shared
flat-stat model**: a disaster's acute/elevated need (e.g. Hunger, per
"Needs generated by a disaster") directly raises the same Hunger stat
that weights Hunger-driven self events (Famine Deaths, Civil Unrest) —
no special-cased cross-reference table needed, since both systems read
the same underlying stats. Human→natural influence stays a separate,
explicit mechanism since it operates through a different pathway
(behavioral/policy factors, not shared stats).

## Open Questions / Not Yet Decided

- Behavior when the user lowers complexity level after already entering
  setup values outside the newly-allowed range (see Visualization / UI →
  "Setup / configuration screen"): silently clamp, warn first, or
  preserve hidden values in case complexity goes back up.
- Per-archetype terrain element composition weights (e.g. Desert's exact
  Sand/Hills/Rock mix) — element list is locked (see "Terrain
  composition elements"), actual weights deferred to the full
  environment-variables pass.
- **"Everything related to the map" is a big open area, flagged for its
  own dedicated design pass rather than being fully resolved piecemeal
  here.** It's decided that the map is a grid of cells, each carrying its
  own local terrain element composition, and that disaster occurrence is
  a two-layer roll (map-wide aggregate for whether a disaster triggers,
  per-cell local elements for where it strikes) — see "Spatial model: map
  grid and per-cell elements." Still open within that:
  - ~~Grid resolution/cell size, and how it scales with society
    size/population and map area.~~ — **resolved**: map size is
    independently configurable (not derived from population); cell
    resolution is a fixed engine constant, so map size changes cell
    *count*, not cell fineness; see Visualization / UI → Main map view →
    "Map size and grid resolution."
  - ~~The procedural generation algorithm that turns an archetype
    preset into per-cell element values across the grid.~~ —
    **resolved**: noise-based (Perlin/simplex), one field per element
    category, seeded from the run's own seed; see "Spatial model: map
    grid and per-cell elements." Cross-category correlation (e.g.
    Mountains co-occurring with Rock) is a deferred refinement.
  - How the map-wide aggregate is computed from per-cell values (simple
    average, population-weighted, something else).
  - ~~Per-disaster-type origination and spread-pattern formulas~~ —
    **resolved at the pattern level**: every disaster type is tagged
    against one of six reusable spread patterns (Gradient flow, Radial
    falloff, Directional track, Broad area, Point/localized,
    Contact/connectivity-driven); see "Spread pattern taxonomy." Exact
    per-pattern math (decay rate, track width, etc.) is still TBD.
  - ~~How the map's grid relates to the faction/unit population
    model~~ — **resolved**: units anchor to specific cells (multiple
    units may share a cell), faction territory is a broader, distinct
    claimed-region concept that can include unoccupied cells; see
    Visualization / UI → "Main map view."
  - ~~How this spatial layer interacts with complexity level~~ —
    **resolved**: it doesn't. Grid resolution is a fixed engine
    constant, not gated by complexity level, unlike nearly everything
    else in this doc; see "Map size and grid resolution."
  - Exact cell color/icon mapping per terrain element (art-direction
    pass, not architecture).
  - Map zoom/pan mechanics and camera controls.
  - Soft cap or visual-clutter handling when many units occupy the same
    cell (echoes the existing soft performance-warning pattern for high
    unit counts overall).
  - How disputed or overlapping faction territory claims over the same
    cell are resolved and rendered.
  - Square-to-hex migration path for the planned "2.0" grid upgrade.
  - ~~How Coastline/River-Lake-adjacent should be represented visually,
    given they can't be a flat repeating texture like every other
    element~~ — **resolved at the approach level**: computed boundary
    lines between known cell-border crossing points, each side rendering
    as whatever elements are actually there rather than dedicated art;
    see "Spatial model: map grid and per-cell elements" -> "Coastline and
    River/Lake-adjacent are boundary-line features." ~~Whether water
    itself needs more than one visual "look"~~ — **resolved**: yes, at
    least an ocean look (Coastline) and a river/lake look
    (River/Lake-adjacent) — see "Water look by boundary type." Still open
    within that: the exact line-generation/cross-cell-consistency
    algorithm, and whether River/Lake-adjacent should split into separate
    River and Lake elements/looks.
- Technology/Culture sub-stat breakdowns, and any further stats surfaced
  by other not-yet-designed systems — a first draft master list now
  exists; see Stat System → "Master stat list (draft)."
- Exact Diplomatic Influence derivation formula from the relationship
  structure's three dimensions (see "Relationship structure
  (faction-to-faction)") — structure itself (asymmetric,
  Trust/Trade/Military Tension) is locked.
- How the sim/user judges whether a given tech/culture regression was
  actually "bad" for the society vs. a reasonable adaptation.
- Exact weight formulas for human-caused event occurrence (which stats
  weight which self events; how relationship-structure values plus both
  factions' stats combine for events against others) — roster and
  architecture are locked, see "Human-caused events."
- Exact "how contentious was the split" formula translating a Faction
  Split's triggering inputs (Military Tension, etc.) into the specific
  parent↔splinter Trust penalty — the general rule (top-down/political
  triggers start worse than bottom-up/gradual ones) is locked; see
  Factions → "Faction formation (splitting)."
- Pull and normalize the actual EM-DAT/USGS/NOAA data for the finalized
  terrain archetype list into concrete baseline weights (list and
  reference regions are locked — see "Weight-sourcing methodology").
- Exact severity-scale tier boundaries/thresholds for disaster types
  using in-sim scales (flood, wildfire, landslide, sinkhole, epidemic,
  etc. — see "Severity scales").
- Exact decay curve shape for post-disaster repeat-suppression (and the
  separate aftershock elevation-then-decay curve for Earthquake), plus
  the exact shape of the below-mainshock portion of the aftershock
  magnitude distribution (the ~5% match-or-exceed floor is set; the rest
  of the curve isn't).
- Full cross-disaster weight-interaction mapping, with concrete
  magnitudes — draft example pairs recorded under "Secondary/cascading
  disasters," but not yet exhaustive or quantified.
- Exact weighting formula for how medicine/technology level, intelligence,
  and cultural containment-acceptance combine into epidemic Strategy
  effectiveness (see "Epidemic mechanics").
- Exact shape of the natural-immunity-buildup curve, and the
  chaos-constant-weighted probability/magnitude of a mutation-spike
  event.
- Exact formulas for how faction stats affect disaster severity and
  recovery speed.
- Exact weighting formulas for how a Need domain's relevant traits
  combine — which trait dominates Strategy choice vs. Resolution success
  probability (see "Trait relevance by need domain") — domain-to-trait
  mapping itself is locked.
- Alliance formation criteria in more detail.
- Exact weighting formula for third-party intervention's Need
  assembly (how relationship, ideological affinity, and
  opportunity/threat perception combine) — the general mechanism (an
  N→S→R instance run by the intervening faction, tier 8+) is locked; see
  Factions → "Third-party faction intervention."
- Exact discrete variance-band thresholds (what unit population ranges
  map to "person/family/community" tiers).
- Exact numeric progression for complexity tiers 2-4, 6-7, and 9 (variable
  counts/ranges, subroutine unlock order, unit-count range at each step)
  — tier count (10) and the milestone tiers (1, 5, 8, 10) are locked; see
  "Complexity tier scale (1-10)."
- Exact formula for combining population size and relationship/influence
  strength into a faction's weight on society-level stats (and a unit's
  weight on faction-level stats).
- Finalized starting-era enum (Caveman, Bronze Age, Modern, etc. were
  only ever given as illustrative examples under Setup / Configuration;
  no concrete list exists) — surfaced by drafting the config schema's
  `startingEra` field; see "Shareable config/seed file schema."
- Which specific formula steps across the sim (beyond combat
  terrain/proximity) warrant a complexity-gated effective-value
  subroutine vs. just using the raw baseline value directly.
