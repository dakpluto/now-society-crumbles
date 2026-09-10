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
  inputs needed to reproduce a run exactly, not the seed alone. Exact
  field-by-field schema and versioning are still TBD.
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

## Setup / Configuration

- **Society size**: from as few as ~10 people to tens/hundreds of
  thousands.
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
  continuous formula. A more exacting continuous scale is a candidate for
  a future "2.0" version.
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
  already-agreed structure.

## Factions

Factions are the natural unit of multi-group dynamics (splitting,
merging, war, trade, exploitation) at *any* population scale — a large
society isn't simulated as one blob, it's simulated as one or more
factions, each composed of population units.

### Faction formation (splitting)

Faction splits are not a special hardcoded system — they emerge
naturally from the unit-variance math: if a unit's randomized roll on a
value like happiness or ideology drifts far enough from its faction's
norm, that's the mechanical trigger for it peeling off to found a new
faction. Possible contributing factors: happiness divergence, geographic
distance from the faction's center, resource competition, succession
disputes, religious/ideological schisms.

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
  conflict.

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
- **Detail view** (e.g. toggled with Tab): a spreadsheet-style breakdown
  of simulation history — fine-grained data and graphs, likely at a
  per-faction, per-year granularity, given the faction/unit-based data
  model.

## Disasters & Events

### Natural disasters

- Each cycle, natural disasters have a chance to occur, **weighted by the
  terrain/environment types** configured at setup — e.g. desert terrain
  carries a high sandstorm weight and very low hurricane weight.
  Baseline weights per terrain/disaster pairing are sourced from
  real-world historical data (see "Weight-sourcing methodology" below)
  rather than invented arbitrarily.
- Each disaster type also has its own **impact-scope weighting** — a
  separate distribution governing how localized or widespread a given
  occurrence is. The same disaster type can vary widely (a typical
  tornado is localized, but a long-track tornado covers much more
  ground; a hurricane is usually wide-reaching but can also just brush a
  region) — scope is its own weighted roll, not a fixed property of the
  disaster type.
- **Multiple disasters can occur in the same cycle.**
- Disasters affect each other's weights within the same cycle — e.g. a
  hurricane occurring should sharply reduce that cycle's drought weight.
  A full cross-disaster weight-interaction mapping will need to be
  defined once disaster types are finalized; see "Secondary/cascading
  disasters" below for the example pairs recorded so far.
- A disaster occurring reduces the chance of that **same** disaster type
  repeating in the immediately following cycles, decaying back toward
  its normal baseline weight over time (a cooldown/decay curve, not a
  hard lockout). **Exception: Earthquake** — an occurrence briefly
  *elevates* its own repeat-weight first (aftershocks) before decaying
  down to baseline, the reverse of the general pattern.

**Disaster roster** (natural disasters only — human-caused events, see
below, use separate formulas): Drought, Sandstorm, Hurricane/Cyclone,
Flood, Earthquake, Wildfire, Blizzard, Tornado, Volcanic eruption,
Landslide, Tsunami, Avalanche, Sinkhole, Extreme heat, Meteor, and
Epidemic (with plant/crop, animal/livestock, and human variants sharing
the same mechanical scaffolding but hitting different stats — crop
epidemics hit farming yield, livestock epidemics hit food
supply/economy, human epidemics hit population/labor directly).

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

Because the sim's terrain types are stylized archetypes rather than
literal geographic regions, sourcing "real-world historical data" means
mapping each terrain archetype to representative real-world reference
region(s), then pulling historical disaster-frequency data for those
regions:

- **EM-DAT** (CRED's international disaster database) as the primary
  source — broad country-level historical event counts across most
  disaster types, good for cross-terrain comparison.
- **USGS** earthquake/volcano catalogs as a supplement for geophysical
  events where EM-DAT is too coarse.
- **NOAA Storm Events Database** as a supplement for severe-weather
  granularity (tornado, blizzard) using a well-documented reference
  region.
- Each terrain archetype maps to one or two real-world reference regions
  (e.g. Desert → Sahara + Sonoran; Tropical → Amazon basin + Southeast
  Asia); each region's per-disaster-type event count is normalized to
  events/decade, then converted to the sim's relative weight scale.
- Terrain archetype list (draft, TBD to finalize): Desert, Tundra,
  Tropical/Rainforest, Temperate forest/plains, Coastal,
  Mountain/highland, Grassland/savanna, Island.

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

### Human-caused events are a separate system

Human-driven crises — recessions, government collapse, rebellion, etc. —
use **different formulas and calculations** than natural disasters; they
are not additional entries in the natural-disaster weight table. Human
factors (society/faction conditions, decisions, policies) **can influence
the weights of natural disasters** (e.g. poor land stewardship raising
drought/famine risk), though it's not yet decided whether influence runs
the other direction too (natural disasters affecting human-event
likelihood).

## Open Questions / Not Yet Decided

- Concrete master list of all stats/traits (technology/culture sub-stats,
  personality traits, capability stats, etc.) — deliberately deferred
  until the sim's core architecture is settled; see Stat System —
  General Principles.
- How the sim/user judges whether a given tech/culture regression was
  actually "bad" for the society vs. a reasonable adaptation.
- Concrete taxonomy of human-caused event types and their own
  trigger/resolution formulas.
- Whether natural disasters feed back into human-caused event weights
  (bidirectional influence) or only human→natural as currently defined.
- Finalize the terrain archetype list and each archetype's real-world
  reference region(s) (draft list recorded under "Weight-sourcing
  methodology"); then actually pull and normalize the EM-DAT/USGS/NOAA
  data into concrete baseline weights.
- Exact severity-scale tier boundaries/thresholds for disaster types
  using in-sim scales (flood, wildfire, landslide, sinkhole, epidemic,
  etc. — see "Severity scales").
- Exact decay curve shape for post-disaster repeat-suppression (and the
  separate aftershock elevation-then-decay curve for Earthquake).
- Full cross-disaster weight-interaction mapping, with concrete
  magnitudes — draft example pairs recorded under "Secondary/cascading
  disasters," but not yet exhaustive or quantified.
- Mechanical differences between Epidemic's plant/animal/human variants
  beyond "which stat it hits" (e.g. do they share one contagion roll or
  three independent ones; can one variant trigger another, like a
  livestock epidemic jumping to humans).
- Exact formulas for how faction stats affect disaster severity and
  recovery speed.
- Exact interactions between specific personality traits and strategy
  selection (once a concrete stat list exists).
- Alliance formation criteria in more detail.
- Third-party intervention trigger conditions at high complexity.
- Exact discrete variance-band thresholds (what unit population ranges
  map to "person/family/community" tiers).
- Exact complexity-level tiering (number of levels, what each unlocks).
- Exact formula for combining population size and relationship/influence
  strength into a faction's weight on society-level stats (and a unit's
  weight on faction-level stats).
- Exact JSON schema for the shareable simulation config/seed file
  (field-by-field structure, versioning strategy) — decided that it holds
  the seed *plus* every starting value, not the seed alone; the concrete
  schema itself is still open.
- Which specific formula steps across the sim (beyond combat
  terrain/proximity) warrant a complexity-gated effective-value
  subroutine vs. just using the raw baseline value directly.
