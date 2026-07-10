# ADR-0001: MachToolTradeAdvisor ⊣ Precision Machinery Export Governor architecture

## Status

Accepted. `cloud-itonami-isic-4659` published directly as `:implemented`
in the `kotoba-lang/industry` registry.

## Context

`cloud-itonami-isic-4659` publishes an OSS business blueprint for
wholesale of other machinery and equipment not classified elsewhere --
principal wholesale trading of industrial/precision machinery: machine
tools, industrial robots, and other precision manufacturing equipment
(machine-order intake, per-jurisdiction contract / capability-
classification / military-end-use / sanctions regulatory verification,
physical dispatch, and invoice settlement). Like every prior actor in
this fleet, the blueprint alone is not an implementation: this ADR
records the governed-actor architecture that establishes it as real,
tested code, following the same langgraph StateGraph + independent
Governor + Phase 0->3 rollout pattern established by
`cloud-itonami-isic-6511` (life insurance) and applied across the
PRINCIPAL wholesale-trading siblings: `cloud-itonami-isic-4671` (fuel
wholesale, single-commodity excise/sanctions focus), `cloud-itonami-
isic-4651` (computer-and-software wholesale, dual-use export-control
classification focus -- this build's CLOSEST thematic cousin, and the
sibling this build most deliberately differentiates from),
`cloud-itonami-isic-4663` (building-materials wholesale, type-gated
single-check lead-free-certification focus), and `cloud-itonami-isic-
4653` (ag-machinery wholesale, two-independent-product-property
certification focus).

ISIC 4659 is a PRINCIPAL trading model like all four siblings above --
the wholesaler takes title and resells. Its defining regulatory concern
is REAL and DIFFERENT IN KIND from the computer-and-software sibling's
own information/data-security focus: certain CNC (computer numerically
controlled) machine tools with advanced multi-axis simultaneous
contouring capability are controlled under the US Export Administration
Regulations Category 2 ("Materials Processing"), specifically ECCN
2B001 (machine tools and specially designed components/accessories),
because precision multi-axis machining capability (5-axis-or-more
simultaneous contouring, fine positioning-accuracy thresholds) is a
genuine proliferation concern: such machines can themselves manufacture
precision weapons components (centrifuge rotors/bellows for uranium
enrichment, missile-guidance and airframe components, submarine-hull
frames) to tolerances an ordinary machine tool cannot hold. This is why
machine tools are a HEADLINE, founding category of the multilateral
Wassenaar Arrangement (1996, the successor to COCOM, ~42 participating
states) Dual-Use List -- one of the oldest, most stable dual-use
control categories, predating the information-security category by
decades. The KEY structural difference from the computer-and-software
sibling: THAT build's concern is information/data-security technology
(can this device encrypt or process controlled information); THIS
build's concern is physical PRECISION-MANUFACTURING CAPABILITY (can
this machine tool itself be used to manufacture precision weapons
components).

Like every principal-trading sibling, this vertical has NO bespoke
domain capability library in `kotoba-lang` to wrap (verified: no
`kotoba-lang/machtooltrade`-style repo exists, and there is no generic
machine-tool-classification library either). This build therefore uses
self-contained domain logic -- but, UNLIKE most prior siblings (whose
domain checks are direct entity boolean reads), THIS build's defining
check requires a genuine pure PHYSICAL RANGE COMPUTATION (see Decision
2).

This blueprint's own `:itonami.blueprint/governor` keyword,
`:precision-machinery-export-governor`, is grep-verified UNIQUE among
the actor fleet repos checked (no `precision-machinery`/`machtooltrade`
match via local grep across the `cloud-itonami` org checkout at build
time) -- no naming-collision precedent question, a fresh independent
build.

## Decision

### Decision 1: fresh governor identity, no reuse precedent needed

`:precision-machinery-export-governor` is grep-verified unique across
the locally-checked-out `cloud-itonami` org at build time (no
`machtooltrade`/`precision-machinery-export-governor` match). This
build follows the SAME governed-actor architecture as every prior
actor, but with its own distinct governor identity.

### Decision 2: a genuine pure PHYSICAL range-check function -- a fleet first for the principal-trading cluster

Every prior principal-trading sibling's domain checks are direct entity
BOOLEAN reads: fuel-wholesale's credit/contract/sanctions, computer-and-
software-wholesale's credit/contract/ECCN-presence/license/sanctions/
denied-party, building-materials-wholesale's lead-free certificate,
ag-machinery-wholesale's emissions/ROPS certificates -- none of these
require ANY arithmetic; each is "is this fact true/false/present." This
build's `capability-threshold-uncertified-violations` (see Decision 4)
is different IN KIND: whether a machine tool is itself export-
controlled is determined by whether its OWN raw technical
specifications (`:simultaneous-axes`, `:positioning-accuracy-
micrometers`) cross a real regulatory threshold -- ECCN 2B001's
headline multi-axis-simultaneous-contouring-plus-positioning-accuracy
criteria. The governor therefore hosts a genuine pure PHYSICAL range-
check function, `machtooltrade.registry/capability-threshold-
crossed?`, the SAME shape as the crude-extraction sibling's own
reservoir-pressure/annular-pressure/water-cut/H2S range checks
(`crude.registry`) -- a fleet first for the principal-trading
(wholesale) cluster specifically, none of whose siblings before this
one needed to host a range-check function at all. Like the crude-
extraction sibling's own checks, missing spec data is treated
CONSERVATIVELY as crossing the threshold (cannot verify NOT controlled
-> treat as controlled), not as "unknown, therefore fine."

**Confidence gradient (important, and documented in code, not just
here):** I have HIGH confidence that simultaneous-axis count and
positioning accuracy (per ISO 230-2, the international machine-tool
positioning-accuracy test standard) are the real, load-bearing
technical parameters ECCN 2B001 actually turns on, and HIGH confidence
that the regulatory CATEGORY (dual-use control of numerically
controlled machine tools, headlined by 2B001, Wassenaar-derived across
US/EU/UK/Japan) is real. I have LOW confidence in the SPECIFIC numeric
threshold values reproduced in `capability-threshold-crossed?` (>= 5
simultaneous axes AND <= 6 micrometers positioning accuracy) -- these
are an illustrative SIMPLIFIED composite standing in for 2B001's real
(and considerably more granular -- separate sub-paragraphs and
thresholds per machine-tool TYPE: grinding, turning, milling/machining-
center, etc.) structure, NOT a verified reproduction of the current
regulatory text. This is documented in the function's own docstring and
in `docs/business-model.md`'s "Jurisdiction coverage (honest)" section,
and flagged again in `SECURITY.md`'s Production Guidance -- a real
deployment MUST replace these thresholds with a verified, current,
machine-type-specific reproduction (or a maintained classification
service) before relying on this function for a real export decision.

### Decision 3: TWO-member actuation set -- `:delivery/dispatch`, `:invoice/settle`; no deemed-export analog

Unlike the computer-and-software-wholesale sibling's THREE-member
high-stakes set (`#{:delivery/dispatch :technology/release :invoice/
settle}` -- the deemed-export doctrine means a software/technology
release can be a controlled event even with no physical shipment),
this vertical has exactly TWO real-world actuation events, matching
the fuel-wholesale/building-materials/ag-machinery siblings' own dual-
actuation shape: `:delivery/dispatch` (physical dispatch of a real
machine tool) and `:invoice/settle` (the money side), applied
SEQUENTIALLY to the SAME `machine-order`. A machine tool is a physical
good; its export event IS its physical cross-border dispatch, full
stop -- there is no non-physical "release" channel analogous to
releasing source code or technical data to a foreign national. (A real
deployment MIGHT eventually need to reason about deemed-export-style
exposure for machine-tool TECHNICAL DRAWINGS/blueprints shared with a
foreign-national engineer, but that is explicitly out of scope for this
R0 -- see `docs/business-model.md` Scope -- and would be a genuinely
separate follow-up op, not retrofitted onto `:delivery/dispatch`.)

### Decision 4: the TWO-AXIS design -- `capability-threshold-uncertified` and `military-end-use-unresolved`, genuinely INDEPENDENT, not sequential; the defining design decision of this build

This is the decision that most distinguishes this vertical from BOTH
of its closest precedents, and it required a genuinely NEW shape, not a
variant of either.

**Contrast with the computer-and-software sibling (`techtrade.
governor`): SEQUENTIAL vs. INDEPENDENT.** That build's defining split,
`eccn-classification-missing-violations` then `license-required-
unauthorized-violations`, is a SEQUENTIAL, DEPENDENT pair: you cannot
even ask the second question ("is a license required, and is one on
file?") until the first ("has this item been classified at all?") has
an answer, and the second check is a literal no-op unless the first
already passed. Both checks are ALSO about the SAME kind of fact: the
item's own classification/license posture (see that build's own
namespace docstring and ADR Decision 4).

**Contrast with the ag-machinery sibling (`agmachtrade.governor`):
TWO-KINDS-OF-FACT vs. TWO-PROPERTIES-OF-ONE-MACHINE.** That build's own
two-independent-checks precedent (`emissions-certificate-missing` /
`rops-certification-missing`, gated on `:engine-powered?`/`:ride-on?`)
IS genuinely independent (proven directly analogous to this build's own
proof discipline) -- but both checks are STILL about the SAME kind of
fact: product-certification status, gated by two PROPERTIES OF THE SAME
MACHINE. Neither reads anything about who is buying the machine or why.

**This build's own shape: one item-property-gated axis, one
transaction-context-gated axis, evaluated with NO shared gating fact
at all.** A precision-machine-tool wholesaler's real exposure genuinely
splits across TWO DIFFERENT KINDS of fact, not two evidentiary arms of
one determination and not two properties of one machine:

1. **CAPABILITY** (the item's own physical specification):
   `capability-threshold-uncertified-violations` re-COMPUTES (see
   Decision 2) whether the machine's raw `:simultaneous-axes`/
   `:positioning-accuracy-micrometers` cross the ECCN-2B001-style
   threshold, and fires when they do AND no formal capability-
   classification determination is on file
   (`:capability-classification-on-file?` false).
2. **END-USE / END-USER** (who is buying it, and why): the EAR's
   Military End User (MEU) Rule (15 C.F.R. §744.21, substantially
   expanded in the 2020s) is a CATCH-ALL that can require a license for
   essentially ANY item subject to the EAR -- INCLUDING an otherwise-
   EAR99, below-threshold general-purpose machine tool -- when the
   exporter knows (or has reason to know) the item is destined for a
   military end use or a listed military end user in a covered
   destination. `military-end-use-unresolved-violations` evaluates this
   OFF `:military-end-use-flagged?`/`:military-end-use-license-
   authorized?`, reading NEITHER `:simultaneous-axes` NOR
   `:positioning-accuracy-micrometers` NOR `:capability-classification-
   on-file?` AT ALL.

Neither check's firing condition depends on the other's outcome, and
there is no shared gating fact the way `:potable-water-contact?` gates
BOTH evidentiary sub-facts of the building-materials sibling's single
folded check. Three design options were considered:

- **Option A (rejected): fold into ONE check**, the way the building-
  materials sibling folds its NSF/372-test + NSF/61-certificate pair
  into one `lead-free-certification-missing` rule. Rejected: that fold
  is correct there because both sub-facts are evidentiary arms of the
  SAME determination (a compliant certificate cannot exist without the
  underlying test, and the test alone is not a compliant certification)
  -- but capability and military-end-use are NOT two arms of one
  determination here. A below-threshold machine sold to a flagged
  military end-user is REAL and genuinely controlled (see the EAR
  §744.21 fact pattern above) even though NOTHING about its own
  capability classification is at issue; folding these into one rule
  would erase that a compliance officer asks two SEPARATE real
  questions ("is the item itself controlled by capability?" and
  "is this specific sale controlled by who it's going to, regardless of
  the item?") and would erase the distinction from the audit ledger.
- **Option B (rejected): model this as a SEQUENTIAL pair**, mirroring
  the computer-and-software sibling's classify-then-license shape (e.g.
  "is capability-classified?" then, only if classified, "is the end-use
  authorized?"). Rejected: this would be factually WRONG for this
  vertical -- the EAR's Military End User Rule does NOT require the
  item to be classified/controlled first; it is a catch-all that
  applies to items regardless of their own classification, INCLUDING
  EAR99 items. Making the end-use check conditional on the capability
  check firing (or passing) first would silently DROP the real-world
  scenario `mo-6` exists to prove: an otherwise-uncontrolled machine
  tool sold to a flagged military end-user.
- **Option C (chosen): two FULLY INDEPENDENT checks**, each reading its
  own disjoint fact set, neither conditional on the other, each with
  its own dedicated fixtures proving independence in BOTH directions.
  `machtooltrade.store/demo-data`'s `mo-5` (above-threshold, ordinary
  end-user, no flag at all) and `mo-6` (deliberately the SAME below-
  threshold spec as the happy-path `mo-1`, sold to a flagged military
  end-user) are the load-bearing pair:
  `test/machtooltrade/governor_contract_test.clj`'s
  `capability-threshold-uncertified-is-held-and-unoverridable` (mo-5)
  asserts the military-end-use rule did NOT also fire; `military-end-
  use-unresolved-is-a-genuinely-different-failure-mode-from-capability-
  threshold` (mo-6) asserts the capability rule did NOT also fire.
  `mo-8` (BOTH conditions present, both unresolved) proves the two
  checks are not even mutually exclusive -- they fire TOGETHER on the
  same order (`both-axes-fire-independently-when-both-conditions-
  present`). `mo-9` (BOTH conditions present, BOTH properly resolved)
  proves a controlled-AND-flagged order CAN still clear when both axes
  are independently satisfied
  (`controlled-and-flagged-order-clears-when-both-axes-are-resolved`).

**Deliberately NOT adding a third, denied-party-list-style check.** The
computer-and-software sibling's own fleet-first
`denied-party-list-flag-unresolved-violations` is a NAMED-PARTY-LIST
screening mechanism (BIS Entity List / Denied Persons List), distinct
from generic OFAC-style sanctions screening because a counterparty can
clear OFAC while remaining Entity-Listed. This build does NOT re-add an
equivalent third check, because the Military End User Rule is ITSELF a
genuinely DIFFERENT mechanism again from named-party-list screening: it
is a CATCH-ALL that can apply based on KNOWLEDGE of end-use/end-user
even when NO party is individually listed anywhere. Re-adding a denied-
party check here would not capture a NEW failure mode this vertical is
distinctively exposed to beyond what the military-end-use axis already
covers; the generic `counterparty-sanctions-flag-unresolved-violations`
(OFAC-style, reused verbatim from every sibling) is retained for the
ordinary blocked-party case.

### Decision 5: `machtooltrade.facts` cites Category 2 ("Materials Processing"), not Category 4/5 -- and is explicit about a lower confidence gradient than prior siblings' own catalogs

`techtrade.facts` (the computer-and-software sibling) cites the SPECIFIC
classification-list provision each jurisdiction uses for computers/
peripherals/software: CCL Category 4/Category 5 Part 2. This vertical's
catalog instead cites CCL Category 2 ("Materials Processing"), headlined
by ECCN 2B001, and the Wassenaar-derived equivalents (EU Annex I
Category 2; UK Strategic Export Control Lists Category 2; Japan's
輸出貿易管理令別表第一). UNLIKE the computer-and-software sibling's
catalog (which cites specific ECCNs like 5A002/5D002/5E002 with
reasonably high confidence), THIS catalog is explicit that:
- confidence is HIGH that each jurisdiction's Category 2/materials-
  processing regime exists and covers machine tools (Wassenaar lineage,
  decades-old, stable);
- confidence is MODERATE-TO-LOW on the PRECISE item-number citation
  within Japan's 輸出貿易管理令別表第一 (I deliberately do NOT cite a
  specific item number I am not independently certain of -- see
  `machtooltrade.facts` docstring);
- confidence is LOW on the precise numeric axis-count/positioning-
  accuracy thresholds (see Decision 2's confidence-gradient discussion,
  which applies to the CODE-level range-check function, not just the
  catalog's prose citation).

This honest confidence gradient is itself a novel documentation
discipline for this build, beyond the "coverage is honest" discipline
every sibling's `facts` namespace already establishes -- it distinguishes
between "I am confident this regulatory CATEGORY exists" and "I am
confident in this EXACT NUMBER," which prior siblings' catalogs did not
need to separate as sharply because their own domain checks were
boolean presence/absence, not numeric thresholds.

### Decision 6: dedicated dual double-actuation-guard booleans

`:dispatched?` / `:invoiced?` are TWO dedicated booleans on the
`machine-order` record, never a single `:status` value -- the same
discipline every prior governor's guards establish, informed by
`cloud-itonami-isic-6492`'s real status-lifecycle bug
(ADR-2607071320).

### Decision 7: Store protocol, MemStore + DatomicStore parity

`machtooltrade.store/Store` is implemented by both `MemStore` (atom-
backed, default for dev/tests/demo) and `DatomicStore` (`langchain.db`-
backed), proven to satisfy the same contract in
`test/machtooltrade/store_contract_test.clj`. Like the computer-and-
software sibling's own store, this one round-trips KEYWORD-valued
fields (`:machine-category`, `:status`) through an EDN-string encoding
(the `:kw` field kind) rather than storing them as bare strings.
`store_contract_test.clj`'s `datomic-empty-store-is-usable` asserts
`:machine-category` reads back as a keyword specifically. The ledger
stays append-only on every backend: which machine-order was verified
for a jurisdiction with no official spec-basis, which machine's own
specifications crossed the capability threshold with no classification
on file, which order's end-user/end-use was flagged for a military/WMD
concern with no license resolution on file, which counterparty had an
unresolved sanctions flag, which order was dispatched or invoiced, on
what jurisdictional and technical basis, approved by whom -- always a
query over an immutable log.

### Decision 8: Phase 0->3 with `:delivery/dispatch`/`:invoice/settle` NEVER auto

`machtooltrade.phase`'s phase table puts `:order/intake` (no direct
capital or export-control risk) in phase 3's `:auto` set as its only
member; `:delivery/dispatch` and `:invoice/settle` are deliberately
ABSENT from every phase's `:auto` set, including phase 3 -- a permanent
structural fact. `machtooltrade.governor`'s high-stakes gate enforces
the same invariant independently: two layers agree that actuation is
always a human trading supervisor / export-compliance officer's call.

### Decision 9: `:robotics false`, reasoned specifically -- NOT a default, a genuine engineering distinction from the metal-wholesale sibling's own crane-automation claim

`:itonami.blueprint/robotics` is `false`, a deliberate, reasoned call --
NOT a default carried over from the general-trading/company-
incorporation siblings' own "pure intermediation, no physical act at
all" reasoning (this vertical DOES have a genuine physical dispatch
act), and NOT a copy of the most recently built sibling either. The
metal-wholesale sibling (`cloud-itonami-isic-4662`) reasons `:robotics
true` for automated overhead crane / stacker-reclaimer handling of BULK
metal stock (coils, plates, ingots) -- goods with UNIFORM, predictable
geometry well-suited to programmable automated grapples/electromagnets.
Precision machine tools are the OPPOSITE case: they are heavy (often
several tonnes), high-value, and IRREGULAR in mass distribution and
center of gravity, each requiring a bespoke, load-specific rigging plan
(sling/lift-point placement per the manufacturer's own rigging diagram)
before any crane can safely hoist it. This is a well-documented,
distinct specialized trade (machinery moving/rigging) precisely
BECAUSE off-the-shelf automated crane/grapple systems -- mature and
genuinely autonomous for uniform bulk stock -- are not well suited to
irregular, load-specific heavy-machinery rigging without bespoke
fixtures and a licensed rigger's real-time judgment. Two options were
considered:

- **Option A (rejected): `:robotics true`, mirroring the metal-
  wholesale sibling's crane-automation claim uniformly.** Rejected:
  this would retrofit a robotics claim from a DIFFERENT commodity shape
  (uniform bulk stock) onto a vertical whose physical handling reality
  is genuinely different (irregular, load-specific rigging) -- the SAME
  "do not retrofit a robotics claim where none exists" discipline the
  general-trading and company-incorporation siblings establish, but
  reasoned here for a positive, domain-specific engineering reason
  rather than "pure intermediation."
  - **Option B (chosen): `:robotics false`, reasoned specifically
  around load-specific rigging-engineering constraints.** This is the
  honest call: a genuine physical dispatch act exists (unlike pure-
  intermediation siblings), but it is NOT robotically automated in the
  fleet's load-bearing sense (an autonomous system performing the
  physical act, gated by the governor) BECAUSE of a real, documented
  engineering distinction from uniform-bulk-commodity handling, not
  because no physical act exists at all. `:required-technologies`
  correspondingly omits `:robotics`
  (`[:identity :forms :dmn :bpmn :audit-ledger :optimization]`),
  matching the ag-machinery sibling's own `:robotics false` blueprint
  shape.

## Alternatives considered

- **Wrapping a bespoke `kotoba-lang/machtooltrade` capability library.**
  Considered and explicitly ruled out: no such library exists.
- **Modeling the capability-threshold check as a boolean presence/
  absence read (mirroring the computer-and-software sibling's `:eccn`
  nil-check) instead of a computed range check.** Considered and
  rejected: this vertical's real regulatory mechanic genuinely turns on
  the machine's OWN raw specifications crossing a threshold, not on
  whether SOME classification act was performed -- see Decision 2.
  Modeling it as a boolean would lose the "independently re-verify
  physical ground truth, never trust a self-reported flag" discipline
  the crude-extraction sibling's own range checks establish.
- **A SEQUENTIAL classify-then-license split (Decision 4 Option B).**
  Considered and rejected: factually wrong for the EAR's Military End
  User Rule, which is a catch-all independent of the item's own
  classification.
- **Folding capability-threshold and military-end-use into ONE check
  (Decision 4 Option A).** Considered and rejected: the two facts are
  not evidentiary arms of one determination; folding them would erase
  a real distinction and the fixture `mo-6` exists specifically to
  prove is load-bearing.
- **A third, denied-party-list-style screening check.** Considered and
  rejected -- see Decision 4's closing paragraph: the Military End User
  Rule is itself a genuinely different mechanism from named-party-list
  screening, so re-adding a third check would not capture a distinctly
  new failure mode.
- **A THREE-member actuation set, mirroring the computer-and-software
  sibling's deemed-export shape.** Considered and rejected -- see
  Decision 3: a machine tool is a physical good with no non-physical
  release channel analogous to source-code/technical-data release.
- **Defaulting `:robotics` to `true`** (matching the metal-wholesale
  sibling uniformly) **or reflexively to `false`** (matching the
  general-trading sibling's "pure intermediation" reasoning, which does
  not actually apply here since a real physical dispatch act exists).
  Considered and rejected in favor of the specifically-reasoned `false`
  in Decision 9 -- neither borrowed default is honest about this
  vertical's own physical-handling reality.
- **Building fulfillment routing and trading-book optimization in this
  R0.** Rejected in favor of a scoped R0 slice (the `:optimization`
  capability is correctly marked required, the integration is a
  follow-up), consistent with this fleet's 'extending coverage is
  additive' convention.

## Consequences

- Fresh independent actor in this fleet, following the SAME governed-
  actor architecture as every prior sibling.
- Establishes the fleet's first genuine pure PHYSICAL range-check
  function in the principal-trading (wholesale) cluster
  (`capability-threshold-crossed?`), the same shape as the crude-
  extraction sibling's own range checks but applied to export-control
  capability rather than well safety.
- Establishes the fleet's first TWO-AXIS domain-defining design where
  the two checks are gated by GENUINELY DIFFERENT KINDS of fact (item
  physical specification vs. transaction end-use/end-user context),
  contrasted explicitly with the computer-and-software sibling's own
  SEQUENTIAL, same-kind-of-fact split and the ag-machinery sibling's
  own two-properties-of-one-machine split -- a template for any future
  vertical whose defining regulatory concern genuinely spans an item-
  property axis and an independent transaction-context axis.
- Establishes an explicit confidence-gradient documentation discipline
  (regulatory-category existence vs. precise numeric thresholds),
  beyond the "honest coverage" discipline every sibling's own `facts`
  namespace already establishes.
- `MemStore` || `DatomicStore` parity is proven by
  `test/machtooltrade/store_contract_test.clj`, including keyword-field
  round-trip parity (`:machine-category`).
- 40+ tests pass; lint is clean; the demo (`clojure -M:dev:run`) walks
  one clean below-threshold dispatch + invoice lifecycle, one clean
  controlled-and-resolved dispatch + invoice lifecycle, and every HARD-
  hold scenario end-to-end, including the load-bearing capability/
  military-end-use independence pair and the both-axes-fire-together
  compound case.
- `blueprint.edn`'s `:robotics false` is a reasoned, domain-specific
  call documented in README and `docs/business-model.md`, distinguished
  explicitly from the metal-wholesale sibling's own crane-automation
  claim on real engineering grounds (uniform bulk stock vs. irregular
  load-specific rigging), not a default carried over from either
  extreme sibling precedent.

## References

- `cloud-itonami-isic-6511/docs/adr/0001-architecture.md` (origin of the
  general governed-actor architecture pattern)
- `cloud-itonami-isic-4671/docs/adr/0001-architecture.md` (fuel-
  wholesale sibling; origin of the sequential dual-actuation shape and
  the self-contained-domain-logic pattern this build follows)
- `cloud-itonami-isic-4651/docs/adr/0001-architecture.md` (computer-and-
  software-wholesale sibling; this build's closest thematic cousin --
  both are export-classification actors -- and the sibling whose
  SEQUENTIAL classify-then-license split this build's Decision 4
  deliberately does NOT follow, building a genuinely independent
  two-axis design instead)
- `cloud-itonami-isic-4663/docs/adr/0001-architecture.md` (building-
  materials-wholesale sibling; origin of the fold-two-evidentiary-arms-
  into-one-rule shape this build's Decision 4 Option A deliberately
  does NOT follow)
- `cloud-itonami-isic-4653/docs/adr/0001-architecture.md` (ag-machinery-
  wholesale sibling; origin of the two-independent-product-property
  check shape this build's Decision 4 contrasts against, and origin of
  the `:robotics false` blueprint shape this build's Decision 9
  matches)
- `cloud-itonami-isic-0610` (`crude.registry`; origin of the pure
  physical range-check-function pattern this build's Decision 2
  follows)
- 15 C.F.R. Parts 730-774 (Export Administration Regulations); Commerce
  Control List Category 2 (Materials Processing), ECCN 2B001
  (numerically controlled machine tools and specially designed
  components/accessories); 15 C.F.R. §744.21 (Military End User Rule)
  (USA, Bureau of Industry and Security, U.S. Department of Commerce)
- Wassenaar Arrangement (1996, successor to COCOM, ~42 participating
  states), Dual-Use List Category 2 (Materials Processing) -- the
  multilateral lineage shared by the US CCL, EU Annex I, UK Strategic
  Export Control Lists and Japan's 輸出貿易管理令別表第一 machine-tool
  entries (cited generally; precise current numeric thresholds not
  independently verified for this R0 -- see Decision 2 and 5)
- 輸出貿易管理令 (Export Trade Control Order) 別表第一 (Appended Table
  1); 該非判定 (gaihi-hantei) classification; キャッチオール規制
  (Article 4 catch-all control) (Japan, METI 貿易経済協力局 安全保障貿易
  管理課) -- specific item-number citation NOT independently verified,
  see `machtooltrade.facts`
- Regulation (EU) 2021/821 (dual-use export-control recast), Annex I
  Category 2 (Materials Processing) (EU; Germany, BAFA)
- Export Control Order 2008 (SI 2008/3231); UK Strategic Export Control
  Lists, Category 2 (Materials Processing) (UK, ECJU)
- OFAC sanctions programs (31 C.F.R. Chapter V) (US, Treasury) -- cited
  for the generic `counterparty-sanctions-flag-unresolved` check, the
  SAME mechanism every sibling in this fleet re-verifies
- ISO 230-2 (Test code for machine tools -- Part 2: Determination of
  accuracy and repeatability of positioning) -- the international
  standard for the positioning-accuracy parameter this build's
  capability-threshold check reads
