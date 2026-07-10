# Operator Guide

## First Deployment
1. Register traders, warehouses, machine-orders, and trading
   supervisors / export-compliance officers.
2. Import machine-order, counterparty, credit, capability-
   classification, military-end-use and sanctions history.
3. Seed the per-jurisdiction spec-basis catalog (`machtooltrade.facts`)
   for the jurisdictions you actually trade in, citing real official
   sources only.
4. Replace the illustrative capability-threshold numeric parameters in
   `machtooltrade.registry/capability-threshold-crossed?` with a
   verified, current, machine-type-specific reproduction of the actual
   control-list text (or a maintained classification service) -- do
   NOT deploy against real export decisions with the illustrative R0
   thresholds. See `docs/adr/0001-architecture.md` Decision 2.
5. Run read-only spec-basis validation per jurisdiction.
6. Configure sanctions / military-end-use escalation and accounts-
   receivable accounts.
7. Publish a dry-run dispatch/invoice and audit export.

## Minimum Trading Controls
- spec-basis validation before any verification, dispatch, or invoice
- full counterparty-diligence evidence (credit-clearance record,
  contract/PO, sanctions-screening record, military end-use/end-user
  screening record) before any dispatch
- credit-clearance and contract-on-file checks before any dispatch
- capability-threshold classification check (AXIS 1) before any dispatch
- military-end-use resolution check (AXIS 2, independent of Axis 1)
  before any dispatch
- sanctions-screening checks before any dispatch or invoice
- military-end-use / sanctions escalation gate
- audit export for every dispatch, invoice, and hold
- backup manual dispatch and invoicing process

## A Day in the Life: Intake → Verify → Dispatch → Settle → Audit

Wholesale of Other Machinery and Equipment (ISIC 4659,
`cloud-itonami-isic-4659`) runs on the same intake / advise / govern /
decide / commit-or-hold loop as every itonami blueprint, but here the
loop is concrete: a regional machine-tool wholesaler needs to bring a
machine-order (say, a 5-axis simultaneous CNC machining center sale to
a counterparty in the United States) from intake through contract
verification to a physical dispatch and an invoice settlement. Walking
through one order, end to end:

1. **Intake.** The trader books the machine-order through `:forms`:
   order-id, item-description, machine-category, simultaneous-axes,
   positioning-accuracy-micrometers, counterparty, price, contract-
   terms, jurisdiction, and the order's own diligence record (credit-
   cleared?, sanctions-screened?, military-end-use-flagged?). This
   creates a machine-order record at `:order/intake` status. The
   MachToolTradeAdvisor only normalizes the patch; it does not invent
   the order-id, counterparty, jurisdiction, technical specifications,
   or any commercial/diligence value.
2. **Verify.** The MachToolTradeAdvisor drafts a per-jurisdiction
   contract / sanctions / military-end-use evidence checklist
   (`:contract/verify`) from `machtooltrade.facts`, citing the
   jurisdiction's official spec-basis (owner authority, legal basis,
   provenance, classification-list) and listing the required evidence
   (credit-clearance record, contract/PO, sanctions-screening record,
   military end-use/end-user screening record). The
   `:precision-machinery-export-governor` sign-off gate must clear: it
   checks the jurisdiction actually has an official spec-basis on file
   (never invent one). A jurisdiction with no spec-basis is a HARD
   hold at the governor node -- it never even reaches a human. This
   verification always escalates to a human for approval; it is never
   auto.
3. **Dispatch.** Before a machine tool can leave the wholesaler's
   control, the `:precision-machinery-export-governor` sign-off gate
   runs the full HARD check set against the order's own ground truth:
   the spec-basis exists, the evidence checklist is complete, the
   counterparty's credit has been cleared, contract-terms are on file,
   AND, on the TWO independent axes this vertical is defined by:
   - **Axis 1 (capability)**: the governor independently RE-COMPUTES
     whether the machine's own raw specifications cross the ECCN-2B001-
     style capability threshold. If they do, a formal capability-
     classification determination must be on file, or dispatch holds.
   - **Axis 2 (military end-use, evaluated INDEPENDENTLY of Axis 1)**:
     the governor checks whether this order's end-user/end-use has
     been flagged for a military/WMD concern (the EAR's Military End
     User Rule, 15 C.F.R. §744.21) and, if so, whether a license
     resolution is on file -- REGARDLESS of the machine's own
     capability classification. A below-threshold, otherwise-ordinary
     machine sold to a flagged military end-user STILL holds here.

   Any failure is a HARD hold that a human cannot override. If every
   check is clean, the proposal STILL always escalates to a human
   trading supervisor / export-compliance officer -- a `:delivery/
   dispatch` never auto-commits at any phase. On approval, the
   dispatch record is drafted (`<JURISDICTION>-DISPATCH-000001`) and
   the order's `:dispatched?` flag is set.
4. **Settle.** Once the machine has actually been dispatched, the
   invoice is settled (`:invoice/settle`): the money side of the trade,
   custody / financial transfer. The governor re-checks the spec-basis,
   the evidence completeness, the sanctions screening, and that this
   order's invoice has not already been settled. As with the dispatch,
   a clean invoice STILL always escalates to a human trading
   supervisor -- `:invoice/settle` never auto-commits. On approval the
   invoice record is drafted (`<JURISDICTION>-INVOICE-000001`) and the
   order's `:invoiced?` flag is set.
5. **Audit.** The verification, the dispatch sign-off, the dispatch
   record, the invoice sign-off, and the invoice record are all
   appended to the `:audit-ledger` -- immutable and exportable, so a
   counterparty or regulatory dispute can be traced back to the exact
   spec-basis citation, evidence checklist, capability-classification
   determination, military-end-use resolution, and supervisor sign-off
   that authorized the dispatch and invoice. If something is wrong with
   the counterparty (a credit deterioration, a military-end-use flag, a
   sanctions hit, a contract gap), that gets raised as a flag and routed
   through the escalation gate instead of being silently suppressed --
   a dispatch for that order then waits on governor sign-off of the
   flag's resolution.

Any deviation from this loop is exactly what the Trust Controls in
`docs/business-model.md` exist to catch: an order verified against a
fabricated spec-basis, a dispatch started with incomplete evidence, an
uncleared counterparty credit or a contract gap, an above-threshold
machine dispatched without a capability-classification record, a
flagged military end-user's order dispatched without license
resolution, a sanctions screening suppressed to force a dispatch
through, or an invoice posted without a human sign-off.

## Feel the Decision Gate: `clojure -M:dev:run`

This vertical has no companion playable prototype. The fastest hands-on
way to feel why the `:precision-machinery-export-governor` gate exists
-- and why it needs TWO independent checks, not one -- is the bundled
demo, which walks clean machine orders through intake → verify →
dispatch → settle (each dispatch/settle pausing for human approval) and
then exercises every HARD-hold failure mode in isolation:

- a jurisdiction with no official spec-basis → HOLD (`:no-spec-basis`),
- a counterparty whose credit has not been cleared → HOLD
  (`:credit-uncleared`),
- an order with no contract-terms on file → HOLD (`:contract-missing`),
- an ABOVE-threshold machine (5 axes, 3 micrometer accuracy) sold to an
  ORDINARY commercial end-user, never capability-classified → HOLD
  (`:capability-threshold-uncertified`) -- AXIS 1 alone,
- a BELOW-threshold, otherwise-ordinary machine sold to a FLAGGED
  military end-user → HOLD (`:military-end-use-unresolved`) -- AXIS 2
  alone, DESPITE clearing the capability-threshold check cleanly,
- a counterparty that has not passed sanctions screening → HOLD
  (`:counterparty-sanctions-flag-unresolved`),
- an above-threshold machine AND a flagged military end-user together
  → BOTH checks fire on the SAME order, proving the two axes are not
  mutually exclusive,
- a double dispatch of the same order → HOLD (`:already-dispatched`),
- a double invoice of the same order → HOLD (`:already-invoiced`).

Each HOLD settles at the governor node and never reaches a human
approver -- the same failure mode the audit ledger is built to catch and
the minimum trading controls above are built to prevent. It is not a
substitute for those controls, but it is the fastest way for a new
operator (or a reviewer) to feel, hands-on, why the gate needs TWO
independent axes rather than one.

## Certification
Certified operators must prove spec-basis-grounded verification,
evidence-backed dispatch readiness (credit-clearance, contract-on-file,
capability-classification, military-end-use resolution, sanctions-
screening), and human review for every dispatch- and invoice-affecting
action.
