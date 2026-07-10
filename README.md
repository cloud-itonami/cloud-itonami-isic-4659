# cloud-itonami-isic-4659

Open Business Blueprint for **ISIC Rev.5 4659**: Wholesale of Other
Machinery and Equipment -- principal wholesale trading of industrial/
precision machinery (machine tools, industrial robots, and other
precision manufacturing equipment not classified elsewhere):
machine-order intake, per-jurisdiction counterparty-diligence /
capability-classification / military-end-use / sanctions regulatory
verification, physical dispatch, and invoice settlement.

This repository publishes a precision-machinery-wholesale actor as an
OSS business that any qualified operator can fork, deploy, run, improve
and sell, so a regional machine-tool wholesaler never surrenders
counterparty, credit, capability-classification and military-end-use
data to a closed trade-compliance / ERP SaaS.

Built on this workspace's
[`langgraph`](https://github.com/kotoba-lang/langgraph)
StateGraph runtime (portable `.cljc`, supervised superstep loop,
interrupts, Datomic/in-mem checkpoints) -- the same actor pattern as
every prior actor in this fleet -- here it is **MachToolTradeAdvisor ⊣
Precision Machinery Export Governor**. This blueprint's own
`:itonami.blueprint/governor` keyword,
`:precision-machinery-export-governor`, is a UNIQUE keyword fleet-wide
(grep-verified against the locally-checked-out fleet at build time) --
a fresh, independent build.

**Like the fuel-wholesale / computer-and-software-wholesale / building-
materials-wholesale / ag-machinery-wholesale siblings, this vertical is
SELF-CONTAINED**: there is no `kotoba-lang/machtooltrade` to delegate
export-classification validation to. Most domain checks (credit-
clearance, contract-on-file, sanctions-screening) are direct entity
boolean reads, but the vertical's DEFINING check,
`capability-threshold-uncertified`, is a genuine pure PHYSICAL range-
check computation over the machine's own raw specifications -- a fleet
first for the principal-trading cluster (see
`docs/adr/0001-architecture.md` Decision 2).

> **Why an actor layer at all?** An LLM is great at drafting an order
> summary, normalizing records, and reading a credit file -- but it
> has **no notion of which jurisdiction's machine-tool export-control
> regime is official, no license to dispatch a real machine tool or
> settle a real invoice, and no way to know on its own whether a
> machine's own technical specifications actually cross a real
> capability-control threshold, whether this order's end-user/end-use
> has actually been screened and resolved for a military/WMD concern,
> or whether OFAC / equivalent sanctions screening has actually been
> passed**. Letting it dispatch a machine tool or settle an invoice
> directly invites fabricated regulatory citations, an uncertified
> controlled machine leaving the wholesaler's control, a machine
> reaching a flagged military end-user, and an invoice settling
> against a sanctioned party -- exposing the operator to real
> enforcement and financial liability, for whoever runs it. This
> project seals the MachToolTradeAdvisor into a single node and wraps
> it with an independent **Precision Machinery Export Governor**, a
> human **approval workflow**, and an immutable **audit ledger**.

## Scope: what this actor does and does not do

This actor covers machine-order intake through contract / capability /
military-end-use / sanctions regulatory verification, physical
dispatch, and invoice settlement. It does **not**, by itself, hold any
export authorization, license, or operating authority required to run a
precision-machinery-wholesale business in a given jurisdiction, and it
does not claim to. It also does not perform the actual physical
warehouse rigging/crane operation, or route optimization itself, or
judge trading-book economics -- fulfillment/route optimization (the
blueprint's own `:optimization` technology) is a follow-up slice, not in
this R0. Whoever deploys and operates a live instance (a qualified
trading supervisor / export-compliance officer) supplies any
jurisdiction-specific operating authority, the real warehouse/rigging/
ERP dispatch integration, and the real ERP / accounts-receivable
integrations, and bears that jurisdiction's liability -- the software
supplies the governed, spec-cited, audited execution scaffold so that
operator does not have to build the compliance layer from scratch.

**The illustrative numeric thresholds in `machtooltrade.registry/
capability-threshold-crossed?` (>= 5 simultaneous axes AND <= 6
micrometer positioning accuracy) are NOT a verified reproduction of the
current ECCN 2B001 text** -- see `docs/adr/0001-architecture.md`
Decision 2 and `docs/business-model.md` "Jurisdiction coverage
(honest)" for the full confidence gradient. A real deployment MUST
replace these with a verified, current, machine-type-specific
reproduction before relying on this actor for a real export decision.

### Actuation

**Physically dispatching a real machine tool and settling a real
invoice are never autonomous, at any phase, by construction.** Two
independent layers enforce this (`machtooltrade.governor`'s
`:delivery/dispatch`/`:invoice/settle` high-stakes gate and
`machtooltrade.phase`'s phase table, which never puts either op in any
phase's `:auto` set) -- see `machtooltrade.phase`'s docstring and
`test/machtooltrade/phase_test.clj`'s
`delivery-dispatch-never-auto-at-any-phase`/
`invoice-settle-never-auto-at-any-phase`. The actor may draft, check
and recommend; a human trading supervisor / export-compliance officer
is always the one who actually dispatches a machine tool or settles an
invoice. A genuine TWO-member actuation shape
(`#{:delivery/dispatch :invoice/settle}`), applied SEQUENTIALLY to the
SAME machine-order -- unlike the computer-and-software sibling's own
THREE-member set (there is no deemed-export analog for a physical
machine tool; see `docs/adr/0001-architecture.md` Decision 3).

## The core contract

```
machine-order intake + jurisdiction facts (machtooltrade.facts, spec-cited)
        |
        v
   ┌───────────────────────┐   proposal      ┌──────────────────────────┐
   │ MachToolTradeAdvisor   │ ─────────────▶ │ Precision Machinery      │  (independent system)
   │ (sealed)               │  + citations    │ Export Governor          │
   └───────────────────────┘                 │ spec-basis · evidence-  │
          │                 commit ◀┼ incomplete · credit-uncleared ·│
          │                         │ contract-missing ·               │
    record + ledger        escalate ┼ capability-threshold-uncertified │
          │              (ALWAYS for│ · military-end-use-unresolved ·  │
          │       :delivery/        │ counterparty-sanctions-flag-      │
          │       dispatch/         │ unresolved · already-dispatched/  │
          │       :invoice/         │ invoiced                          │
          │       settle)           └──────────────────────────────────┘
          ▼
      human approval
```

**The MachToolTradeAdvisor never dispatches a machine tool or settles
an invoice the Precision Machinery Export Governor would reject, and
never does so without a human sign-off.** Hard violations (fabricated
regulatory requirements; unsupported evidence; an uncleared
counterparty credit; no contract-terms on file; a machine whose own
specifications cross the capability threshold with no classification
on file; an order whose end-user/end-use is flagged for a military/WMD
concern with no license resolution on file; an unresolved sanctions-
screening flag; a double dispatch/invoice) force **hold** and *cannot*
be approved past; a clean dispatch/invoice proposal still always routes
to a human.

**The TWO-AXIS design -- proven independent, not sequential.** Unlike
the computer-and-software sibling's SEQUENTIAL classify-then-license
split, this build's `capability-threshold-uncertified` and
`military-end-use-unresolved` checks are genuinely INDEPENDENT axes:
a below-threshold, otherwise-ordinary machine sold to a FLAGGED
military end-user still HOLDS (on the military-end-use check ALONE),
even though it clears the capability-threshold check cleanly; an
above-threshold machine sold to an ORDINARY commercial end-user still
HOLDS (on the capability-threshold check ALONE), even with no end-use
flag at all. See `docs/adr/0001-architecture.md` Decision 4 for the
full reasoning, and `test/machtooltrade/governor_contract_test.clj`'s
`mo-5`/`mo-6`/`mo-8`/`mo-9` fixtures for the executable proof.

## Run

```bash
clojure -M:dev:run     # walk clean dispatch + invoice lifecycles, plus every HARD-hold case (esp. the two-axis independence pair), through the actor
clojure -M:dev:test    # governor contract · phase invariants · store parity · registry conformance · facts coverage
clojure -M:lint        # clj-kondo (errors fail; CI mirrors this)
```

## Robotics premise

`blueprint.edn` sets `:itonami.blueprint/robotics false` -- a
deliberate, reasoned call, NOT a default. A genuine physical dispatch
act exists in this vertical (unlike a pure-intermediation business),
but precision machine tools are heavy, high-value and IRREGULAR in mass
distribution, requiring a bespoke, load-specific rigging plan (sling/
lift-point placement per the manufacturer's own rigging diagram) before
any crane can safely hoist one -- a real, distinct specialized trade
(machinery moving/rigging) precisely because off-the-shelf automated
crane/grapple systems (mature and genuinely autonomous for UNIFORM bulk
stock like the metal-wholesale sibling's own coils/plates/ingots) are
not well suited to irregular, load-specific heavy-machinery rigging
without bespoke fixtures and a licensed rigger's real-time judgment. See
`docs/adr/0001-architecture.md` Decision 9 and `docs/business-model.md`
Robotics Premise for the full reasoning.

## Open business

This repository is not only source code. It is a public, forkable
business model:

| Layer | What is open |
|---|---|
| OSS core | Actor runtime, Precision Machinery Export Governor, dispatch/invoice draft records, audit ledger |
| Business blueprint | Customer, offer, pricing, unit economics, sales motion |
| Operator playbook | How to fork, license, deploy and support the service in a jurisdiction |
| Trust controls | Governance, security reporting, actuation invariant, audit requirements |

See [`docs/business-model.md`](docs/business-model.md) and
[`docs/operator-guide.md`](docs/operator-guide.md) to start this as an
open business on itonami.cloud, and
[`docs/adr/0001-architecture.md`](docs/adr/0001-architecture.md) for the
full architecture and decision record.

## Capability layer

This blueprint resolves its technology stack via
[`kotoba-lang/industry`](https://github.com/kotoba-lang/industry) (ISIC
`4659`). This vertical is NOT backed by a separate bespoke domain
capability lib: most checks (credit-clearance, contract-on-file,
sanctions-screening, military-end-use resolution) are direct entity
reads in `machtooltrade.governor`, while the capability-threshold check
is a genuine pure range-check computation hosted in
`machtooltrade.registry`, on top of the generic identity/forms/dmn/bpmn/
audit-ledger stack.

## Layout

| File | Role |
|---|---|
| `src/machtooltrade/store.cljc` | **Store** protocol -- `MemStore` ‖ `DatomicStore` (`langchain.db`) + append-only audit ledger + dispatch AND invoice history (dual history). The double-actuation guards check dedicated `:dispatched?`/`:invoiced?` booleans rather than a `:status` value |
| `src/machtooltrade/registry.cljc` | Dispatch/invoice draft records AND the pure `capability-threshold-crossed?` range-check function -- a fleet first for the principal-trading cluster (see ADR Decision 2) |
| `src/machtooltrade/facts.cljc` | Per-jurisdiction machine-tool export-control-CLASSIFICATION-LIST catalog with an official spec-basis citation per entry, honest coverage reporting, explicit confidence gradient |
| `src/machtooltrade/machtooltradeadvisor.cljc` | **MachToolTradeAdvisor** -- `mock-advisor` ‖ `llm-advisor`; intake/contract-verification/dispatch/invoice proposals |
| `src/machtooltrade/governor.cljc` | **Precision Machinery Export Governor** -- 7 HARD checks (spec-basis · evidence-incomplete · credit-uncleared · contract-missing · capability-threshold-uncertified · military-end-use-unresolved · counterparty-sanctions-flag-unresolved) + 2 double-actuation guards + 1 soft (confidence/actuation gate) |
| `src/machtooltrade/phase.cljc` | **Phase 0→3** -- read-only → assisted intake → assisted verify → supervised (dispatch/invoice always human; order intake is the ONLY auto-eligible op) |
| `src/machtooltrade/operation.cljc` | **OperationActor** -- langgraph StateGraph |
| `src/machtooltrade/sim.cljc` | demo driver |
| `test/machtooltrade/*_test.clj` | governor contract (incl. two-axis independence proof) · phase invariants · store parity · registry conformance (incl. capability-threshold range-check unit tests) · facts coverage |

## Business-process coverage (honest)

This actor covers machine-order intake through contract / capability /
military-end-use / sanctions regulatory verification, physical
dispatch, and invoice settlement -- the core governed lifecycle:

| Covered | Not covered (out of scope for this R0) |
|---|---|
| Machine-order intake + per-jurisdiction evidence checklisting, HARD-gated on an official spec-basis citation (`:order/intake`/`:contract/verify`) | Real warehouse-management/rigging/ERP integration, fulfillment routing and trading-book economics |
| Physical dispatch, HARD-gated on full evidence, capability-threshold classification, military-end-use resolution, a credit-cleared counterparty, contract-terms on file, passed sanctions screening, and no double-dispatch (`:delivery/dispatch`) | Deemed-export-style exposure for machine-tool technical drawings/blueprints (a genuinely separate follow-up op, not this R0) |
| Invoice settlement, HARD-gated on full evidence, passed sanctions screening, and no double-invoice (`:invoice/settle`) | |
| Immutable audit ledger for every intake/verification/dispatch/invoice decision | |

Extending coverage is additive: add the next gate (e.g. a re-export
screening check) as its own governed op with its own HARD checks and
tests, following the SAME "an independent governor re-verifies against
the actor's own records before any real-world act" pattern this repo's
flagship ops already establish.

## Jurisdiction coverage (honest)

`machtooltrade.facts/coverage` reports how many requested jurisdictions
actually have an official spec-basis in `machtooltrade.facts/catalog`
-- currently 4 seeded (USA, JPN, DEU, GBR) out of ~194 jurisdictions
worldwide. This is a starting catalog to prove the governor contract
end-to-end, not a claim of global coverage. Adding a jurisdiction is
additive: one map entry in `machtooltrade.facts/catalog`, citing a real
official source -- never fabricate a jurisdiction's requirements to
make coverage look bigger.

**Confidence gradient (read before relying on this catalog or the
capability-threshold function for a real decision):** confidence is
HIGH that each seeded jurisdiction's machine-tool dual-use export-
control category exists and covers numerically controlled machine
tools (Wassenaar-derived, decades-old, stable); confidence is MODERATE-
TO-LOW on Japan's precise Appended-Table-1 item number (deliberately
not cited); confidence is LOW on the exact numeric axis-count/
positioning-accuracy thresholds in `machtooltrade.registry/capability-
threshold-crossed?`, which are an illustrative simplified composite,
not a verified reproduction of the current ECCN 2B001 text. See
`docs/adr/0001-architecture.md` Decisions 2 and 5.

## Maturity

`:implemented` -- `MachToolTradeAdvisor` + `Precision Machinery Export
Governor` run as real, tested code (see `Run` above), following the
SAME governed-actor architecture as the other prior actors across this
fleet, with its own distinct, independently-named governor and its own
two-axis capability/military-end-use design (a fleet first, both in its
independent-axis shape and in hosting a genuine pure range-check
function). See `docs/adr/0001-architecture.md` for the history and
design.

## License

Code and implementation templates are AGPL-3.0-or-later.
