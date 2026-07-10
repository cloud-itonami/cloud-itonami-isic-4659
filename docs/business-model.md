# Business Model: Wholesale of Other Machinery and Equipment

## Classification
- Repository: `cloud-itonami-isic-4659`
- ISIC Rev.5: `4659` — wholesale of other machinery and equipment
- Domain: `downstream/precision-machinery-wholesale`
- Social impact: export-control compliance, nonproliferation, transparency
- Governor: `:precision-machinery-export-governor`
- License: AGPL-3.0-or-later

## Scope
This actor covers machine-order intake through per-jurisdiction
counterparty-diligence / capability-classification / military-end-use /
sanctions regulatory verification, physical dispatch (moving a real
machine tool to a counterparty), and invoice settlement (the money side
of a precision-machinery-wholesale trade, custody / financial transfer).
It does **not**, by itself, hold any export authorization, license, or
operating authority required to run a precision-machinery-wholesale
business in a given jurisdiction, perform the actual physical warehouse
rigging/crane operation, or judge trading-book economics (fulfillment
routing and trading-book optimization is a follow-up slice, not this
R0). Whoever deploys a live instance supplies the jurisdiction-specific
operating authority, the real warehouse/rigging/ERP integrations, and
bears that jurisdiction's liability -- the software supplies the
governed, spec-cited, audited execution scaffold so the operator does
not have to build the compliance layer from scratch.

## Customer
- regional and independent machine-tool and industrial-equipment wholesalers
- distributors and trading houses leaving closed trade-compliance / ERP SaaS
- manufacturers of precision equipment selling through independent wholesale channels
- counterparties, banks and regulators who need an auditable, spec-cited
  trade record

## Offer
- machine-order intake and directory management
- per-jurisdiction contract / capability-classification / military-end-
  use / sanctions regulatory verification with an official spec-basis
  citation
- physical dispatch gated on full evidence, capability-threshold
  classification, military-end-use resolution, a credit-cleared
  counterparty, contract-terms on file and a passed sanctions screen
- invoice settlement (custody / financial transfer) with double-invoice
  prevention
- evidence checklisting (credit-clearance record, contract/PO,
  sanctions-screening record, military end-use/end-user screening
  record)
- sanctions and credit exception workflows
- role-based access and immutable audit ledger

## Revenue
- self-host setup fee
- managed hosting subscription per trader / warehouse
- support retainer with SLA
- ERP and accounts-receivable integration

## The `:precision-machinery-export-governor` Decision Rule

This blueprint's `:itonami.blueprint/governor` is
`:precision-machinery-export-governor`. It is the single authority that
stands between "a machine tool could be dispatched to a counterparty"
and "it is allowed to leave the wholesaler's control," and between "an
invoice could be settled" and "it is allowed to settle." Every rule it
enforces is traceable to the domain (Wholesale of Other Machinery and
Equipment, ISIC 4659) and to the three `:social-impact` tags in
`blueprint.edn` (`:export-control-compliance`, `:nonproliferation`,
`:transparency`).

This is the rule the companion contract test
(`test/machtooltrade/governor_contract_test.clj`) encodes end-to-end:
the MachToolTradeAdvisor never dispatches a machine tool or settles an
invoice the Precision Machinery Export Governor would reject,
`:delivery/dispatch` and `:invoice/settle` NEVER auto-commit at any
phase, `:order/intake` (no direct capital risk) MAY auto-commit when
clean, and every decision (commit OR hold) leaves exactly one ledger
fact.

**Authorizes a physical dispatch (`:delivery/dispatch`) or invoice
settlement (`:invoice/settle`) only when ALL of the following hold:**

1. **An official spec-basis citation exists for the jurisdiction** -- the
   governor will not authorize any `:contract/verify`, `:delivery/
   dispatch`, or `:invoice/settle` proposal whose jurisdiction has no
   entry in the `machtooltrade.facts` catalog (`:no-spec-basis`). This is
   the direct enforcement of `:transparency`: a jurisdiction whose
   machine-tool export-control requirements cannot be traced to an
   OFFICIAL public source is never guessed.
2. **The jurisdiction's required evidence is fully on file** -- for a
   dispatch or invoice the order's jurisdiction must have been verified
   with a complete counterparty-diligence evidence checklist on record:
   the credit-clearance record, the contract / purchase order, the
   sanctions-screening (OFAC / equivalent) record, and the military
   end-use/end-user screening record (`:evidence-incomplete`).
3. **The counterparty's credit has been cleared** -- the governor reads
   the dedicated `:credit-cleared?` fact on the order and refuses to
   dispatch when credit has NOT been cleared (`:credit-uncleared`).
   Evaluated at `:delivery/dispatch`.
4. **Contract-terms are on file** -- the governor refuses to dispatch
   when no `:contract-terms` are recorded for the order
   (`:contract-missing`). A machine tool never leaves the wholesaler's
   control against an undocumented trade. Evaluated at `:delivery/
   dispatch`.
5. **AXIS 1 -- Capability threshold**: the governor INDEPENDENTLY
   RE-COMPUTES whether the machine's own raw specifications
   (`:simultaneous-axes`, `:positioning-accuracy-micrometers`) cross the
   ECCN-2B001-style capability threshold. If they do, a formal
   capability-classification determination must be on file
   (`:capability-classification-on-file?` true), or the dispatch is
   held (`:capability-threshold-uncertified`). A machine below the
   threshold needs no classification record at all -- this check is a
   no-op for it.
6. **AXIS 2 -- Military end-use, evaluated INDEPENDENTLY of Axis 1**:
   the governor reads dedicated `:military-end-use-flagged?`/
   `:military-end-use-license-authorized?` facts, REGARDLESS of the
   machine's own capability classification. If the order's end-user/
   end-use has been flagged for a military/WMD concern and no license
   resolution is on file, the dispatch is held
   (`:military-end-use-unresolved`) -- even if the machine's own specs
   never cross the Axis 1 threshold. This is the EAR's Military End
   User Rule (15 C.F.R. §744.21) in action: it is a CATCH-ALL, not
   conditioned on the item's own classification.
7. **The counterparty has passed OFAC / equivalent sanctions screening**
   -- the governor reads the dedicated `:sanctions-screened?` fact and
   treats an unresolved sanctions-screening flag as a HARD, un-
   overridable hold (`:counterparty-sanctions-flag-unresolved`).
   Evaluated UNCONDITIONALLY at both `:delivery/dispatch` and
   `:invoice/settle`.
8. **The order has not already been dispatched, and the invoice has not
   already been settled** -- a double dispatch of the same order is
   refused off a dedicated `:dispatched?` fact, and a double invoice off
   a dedicated `:invoiced?` fact (never a `:status` value)
   (`:already-dispatched` / `:already-invoiced`).

**Rejects (HOLD, un-overridable, never even reaches a human) when any of
the above fail.** A proposal with no spec-basis, incomplete evidence, an
uncleared counterparty credit, no contract-terms on file, an
uncertified above-threshold machine, an unresolved military-end-use
flag, an unresolved sanctions-screening flag, or a double dispatch/
invoice is held at the governor node -- a human approver cannot
override these, by construction.

**Always escalates to a human (never auto-commits) for `:delivery/
dispatch` and `:invoice/settle`**, even when every check above is
clean. Physically dispatching a real machine tool and settling a real
invoice (real money moving between counterparty and trader) are the two
real-world actuation events this actor performs; both are always a
human trading supervisor's call. This is enforced by TWO independent
layers that agree on purpose: the governor's confidence / actuation SOFT
gate (a `:delivery/dispatch` / `:invoice/settle` stake always
escalates) and `machtooltrade.phase`'s phase table, which never puts
either op in any phase's `:auto` set.

## Why Axis 1 and Axis 2 are evaluated INDEPENDENTLY, not sequentially

Unlike a checklist-style "has SOME export process been completed"
check, this vertical's two domain-defining checks read COMPLETELY
DISJOINT fact sets: Axis 1 reads only the machine's own physical
specifications; Axis 2 reads only the transaction's end-user/end-use
flag and its resolution. Neither is conditional on the other. This
matters in practice: a below-threshold, otherwise-ordinary machine tool
sold to a counterparty flagged as a military end-user is REAL and
genuinely controlled under the EAR's Military End User Rule (15 C.F.R.
§744.21) -- a catch-all that applies REGARDLESS of the item's own
classification, including to EAR99 items. A governor that only checked
Axis 1 would wrongly clear this order. Symmetrically, an above-
threshold, uncertified machine sold to an ordinary commercial buyer with
no end-use concern at all must still hold on Axis 1 alone. See
`docs/adr/0001-architecture.md` Decision 4 for the full design
reasoning and `test/machtooltrade/governor_contract_test.clj`'s
`mo-5`/`mo-6`/`mo-8`/`mo-9` fixtures for the executable proof of
independence in both directions.

## Required Technologies

`blueprint.edn`'s `:itonami.blueprint/required-technologies` for this
business, and what each one is actually load-bearing for here (not a
generic capability list):

| Technology | What it is FOR in Wholesale of Other Machinery and Equipment |
|---|---|
| `:identity` | Trader, trading-supervisor, export-compliance-officer and counterparty identity plus role-based access, so the governor's sign-off is tied to *who* authorized a dispatch or invoice, not just *that* someone did. |
| `:forms` | Structured intake for machine-order booking, per-jurisdiction evidence capture (credit-clearance record, contract/PO, sanctions-screening record, military end-use/end-user screening record), and capability-classification / military-end-use exception submission -- the data the Decision Rule above actually evaluates comes in through these forms. |
| `:dmn` | Encodes the `:precision-machinery-export-governor` Decision Rule itself (spec-basis, evidence completeness, credit-clearance, contract-on-file, the two-axis capability/military-end-use design, sanctions-screening, the double-actuation guards, the actuation gate) as an evaluable decision table rather than code buried in application logic -- this is what makes the governor auditable and swappable per-deployment. |
| `:bpmn` | Orchestrates the intake -> verify -> dispatch -> settle -> audit loop end-to-end (see `docs/operator-guide.md`) across machine-order intake, contract verification, physical dispatch, and invoice settlement, including the sanctions / military-end-use escalation gate. |
| `:audit-ledger` | The immutable record of every verification, dispatch, invoice, capability-classification determination, military-end-use flag, sanctions flag, and hold -- this is what "an auditable, spec-cited trade record for every dispatch and invoice" (Trust Controls, below) actually means in practice, and the evidence an operator needs if a dispatch or an invoice is later disputed by a counterparty or regulator. |
| `:optimization` | Fulfillment routing and trading-book optimization -- selects the profitable fulfillment strategy for a warehouse. This R0 build deliberately scopes optimization OUT (see README `Business-process coverage`); the capability is correctly marked required, the integration is a follow-up slice. |

There is NO bespoke `:machtooltrade` capability library in this stack:
most checks (credit-clearance, contract-on-file, sanctions-screening,
military-end-use resolution) are direct entity reads in
`machtooltrade.governor`, on top of the generic identity/forms/dmn/bpmn/
audit-ledger stack -- but the capability-threshold check is a genuine
pure range-check computation (`machtooltrade.registry/capability-
threshold-crossed?`), a fleet first for the principal-trading cluster
(see Capability layer below and `docs/adr/0001-architecture.md`
Decision 2).

## Trust Controls
- a jurisdiction with no official spec-basis can never be verified,
  dispatched, or invoiced against
- a dispatch never starts with incomplete counterparty-diligence
  evidence
- a dispatch never starts with an uncleared counterparty credit, no
  contract-terms on file, an uncertified above-threshold machine, or an
  unresolved military-end-use flag
- an invoice never settles against an unresolved sanctions-screening flag
- capability, military-end-use and sanctions flags cannot be silently
  suppressed
- the same order can never be dispatched or invoiced twice
- a dispatch or invoice never auto-commits; both always need a human
  trading supervisor
- every dispatch and invoice (commit OR hold) leaves exactly one
  immutable ledger fact
- counterparty, credit, capability-classification and military-end-use
  data stays outside Git

## Implementation notes (`:implemented`)

The Decision Rule above is implemented faithfully by
`machtooltrade.governor` as seven HARD checks (a human approver cannot
override them) plus one SOFT gate:

- `spec-basis-violations` -- the spec-basis check above, evaluated on
  every `:contract/verify`, `:delivery/dispatch`, and `:invoice/settle`.
- `evidence-incomplete-violations` -- the evidence-completeness check
  above, for `:delivery/dispatch` / `:invoice/settle`.
- `credit-uncleared-violations` -- the counterparty-credit check above;
  evaluated on every `:delivery/dispatch`.
- `contract-missing-violations` -- the contract-on-file check above;
  evaluated on every `:delivery/dispatch`.
- `capability-threshold-uncertified-violations` -- AXIS 1, a genuine
  computed range check (`machtooltrade.registry/capability-threshold-
  crossed?`), evaluated on every `:delivery/dispatch`.
- `military-end-use-unresolved-violations` -- AXIS 2, evaluated
  COMPLETELY INDEPENDENTLY of Axis 1, on every `:delivery/dispatch`.
- `counterparty-sanctions-flag-unresolved-violations` -- the sanctions-
  screening check above; evaluated unconditionally on both `:delivery/
  dispatch` and `:invoice/settle`.
- `already-dispatched-violations` / `already-invoiced-violations` -- the
  double-actuation guards above, off dedicated `:dispatched?` /
  `:invoiced?` booleans (never a `:status` value).
- the confidence floor / actuation SOFT gate -- low confidence, OR a
  `:delivery/dispatch` / `:invoice/settle` stake, escalates to a human;
  and `machtooltrade.phase` independently never auto-commits either op
  at any phase.

`:delivery/dispatch` and `:invoice/settle` are the two real-world
actuation events (`#{:delivery/dispatch :invoice/settle}`), applied
SEQUENTIALLY to the SAME machine-order (dispatch first, invoice
settlement later). Neither ever auto-commits at any phase. Fulfillment
routing and trading-book optimization (the `:optimization` line above)
is a follow-up slice, not in this R0 build -- see README
`Business-process coverage`.

## Capability layer

This vertical is SELF-CONTAINED: there is no `kotoba-lang/machtooltrade`
to delegate export-classification validation to. Most checks (credit-
clearance, contract-on-file, sanctions-screening, military-end-use
resolution) are direct entity reads in `machtooltrade.governor` (off
dedicated `:credit-cleared?` / `:contract-terms` / `:sanctions-
screened?` / `:military-end-use-flagged?` / `:military-end-use-license-
authorized?` facts on the `machine-order` record). UNLIKE every prior
principal-trading sibling, this vertical's `capability-threshold-
uncertified` check needs a genuine pure PHYSICAL range-check function
(`machtooltrade.registry/capability-threshold-crossed?`, reading
`:simultaneous-axes` and `:positioning-accuracy-micrometers`) -- the
SAME shape as the crude-extraction sibling's own reservoir-pressure/
annular-pressure/water-cut/H2S range checks, a fleet first for the
principal-trading (wholesale) cluster (see `docs/adr/0001-
architecture.md` Decision 2).

## Jurisdiction coverage (honest)

`machtooltrade.facts/catalog` currently seeds 4 jurisdictions with an
official spec-basis, each a REAL regime: the United States (BIS/EAR,
Commerce Control List Category 2 "Materials Processing", ECCN 2B001,
plus the Military End User Rule 15 C.F.R. §744.21), Japan (METI 安全保障
貿易管理課, 輸出貿易管理令別表第一), Germany/EU (BAFA, Regulation (EU)
2021/821 Annex I Category 2), and the United Kingdom (ECJU, Export
Control Order 2008, UK Strategic Export Control Lists Category 2). This
is a starting catalog to prove the governor contract end-to-end, not a
claim of global coverage (4 of ~194 jurisdictions worldwide).

**Confidence gradient (read before relying on this catalog for a real
decision) -- this is NOT uniform across every citation in this build,
and the code and docs are explicit about where confidence drops:**

- **HIGH confidence**: each seeded jurisdiction has a REAL dual-use
  export-control regime covering numerically controlled machine tools
  (Commerce Control List Category 2 / ECCN 2B001 and its Wassenaar-
  derived EU/UK/Japan equivalents are a real, decades-old, stable
  category -- the founding category of the Wassenaar Arrangement, 1996,
  successor to COCOM). HIGH confidence that the EAR's Military End
  User Rule (15 C.F.R. §744.21) is a real, distinct, catch-all
  regulatory mechanism, substantially expanded in the 2020s.
- **MODERATE-TO-LOW confidence**: the SPECIFIC item number within
  Japan's 輸出貿易管理令別表第一 (Appended Table 1) that covers machine
  tools -- `machtooltrade.facts` deliberately does NOT cite a specific
  item number here, because I am not independently certain of it
  without verification against the current official text.
- **LOW confidence**: the PRECISE numeric thresholds in
  `machtooltrade.registry/capability-threshold-crossed?` (>= 5
  simultaneous axes AND <= 6 micrometer positioning accuracy). These
  are an illustrative SIMPLIFIED composite for this R0's fixtures/
  tests -- ECCN 2B001's real structure is considerably more granular
  (separate sub-paragraphs and thresholds per machine-tool type:
  grinding, turning, milling/machining-center, etc.), and I have NOT
  independently verified the current exact values. A real deployment
  MUST replace these with a verified, current, machine-type-specific
  reproduction (or a maintained classification service) before relying
  on this actor for a real export decision -- see `SECURITY.md`
  Production Guidance.

Adding a jurisdiction, or tightening these numeric thresholds, is
additive: extend `machtooltrade.facts/catalog` or `machtooltrade.
registry/capability-threshold-crossed?`, citing a real official
source -- never fabricate a jurisdiction's requirements or a threshold
value to make coverage look bigger or more precise than it is.

## Maturity

`:implemented` -- `MachToolTradeAdvisor` + `Precision Machinery Export
Governor` run as real, tested code (`clojure -M:dev:test`), promoted
directly as `:implemented`, following the SAME governed-actor
architecture as the other prior actors across this fleet, with its own
distinct, independently-named governor and its own two-axis capability/
military-end-use design. See `docs/adr/0001-architecture.md` for the
history and design.

## Robotics Premise

`blueprint.edn` sets `:itonami.blueprint/robotics false`. This is a
deliberate, reasoned call, not a default: a genuine physical dispatch
act exists in this vertical (unlike a pure-intermediation business such
as the general-trading sibling), but precision machine tools are heavy,
high-value, and IRREGULAR in mass distribution and center of gravity,
requiring a bespoke, load-specific rigging plan (sling/lift-point
placement per the manufacturer's own rigging diagram) before any crane
can safely hoist one. This is a well-documented, distinct specialized
trade (machinery moving/rigging) precisely because off-the-shelf
automated crane/grapple systems -- mature and genuinely autonomous for
UNIFORM bulk stock like the metal-wholesale sibling's own coils, plates
and ingots -- are not well suited to irregular, load-specific heavy-
machinery rigging without bespoke fixtures and a licensed rigger's
real-time judgment. See `docs/adr/0001-architecture.md` Decision 9 for
the full reasoning, including why this is genuinely different from
both the metal-wholesale sibling's own `:robotics true` crane-
automation claim and the general-trading sibling's own `:robotics
false` "pure intermediation, no physical act at all" reasoning (a
genuine physical act DOES exist here -- it is just not robotically
automated, for a specific engineering reason).
