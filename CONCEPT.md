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
  baseline entropy/randomness.
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

## Open Questions / Not Yet Decided

- Concrete taxonomy of detailed technology/culture sub-stats (what the
  fine-grained breakdown actually consists of, e.g. agriculture,
  medicine, military, governance, philosophy/religion, art/craft).
- How the sim/user judges whether a given tech/culture regression was
  actually "bad" for the society vs. a reasonable adaptation.
- Disaster/event system (droughts, plagues, invasions — generation rules
  and interaction with unit variance and the regression mechanic).
- Concrete list of faction/unit personality traits and their exact
  interactions with strategy selection.
- Alliance formation criteria in more detail.
- Third-party intervention trigger conditions at high complexity.
- Exact discrete variance-band thresholds (what unit population ranges
  map to "person/family/community" tiers).
- Exact complexity-level tiering (number of levels, what each unlocks).
- Exact formula for combining population size and relationship/influence
  strength into a faction's weight on society-level stats (and a unit's
  weight on faction-level stats).
