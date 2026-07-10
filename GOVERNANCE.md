# Governance

`cloud-itonami-isic-4659` is an OSS open-business blueprint for wholesale
of other machinery and equipment (precision machine tools, industrial
robots, and other precision manufacturing equipment not classified
elsewhere).

## Maintainers
Maintainers may merge changes that preserve these invariants:
- a machine-order whose jurisdiction has no official machine-tool
  export-control spec-basis can never be verified, dispatched or
  invoiced.
- the Precision Machinery Export Governor remains independent of the
  advisor.
- hard governor violations (a fabricated spec-basis, incomplete
  counterparty-diligence evidence, an uncleared counterparty credit, a
  missing contract, a machine whose own specifications cross the
  capability-control threshold with no classification on file, an
  order whose end-user/end-use is flagged for a military/WMD concern
  with no license resolution on file, an unresolved OFAC-style
  sanctions flag, a double dispatch or a double invoice) cannot be
  overridden by human approval.
- `capability-threshold-uncertified` and `military-end-use-unresolved`
  remain two INDEPENDENT checks -- neither is ever made conditional on
  the other.
- every intake, contract verification, dispatch, settlement and hold is
  auditable.
- counterparty, credit, capability-classification, and military-end-
  use/sanctions-screening data stays outside Git.

## Decision Records
Architecture decisions live in `docs/adr/`. Changes to the trust model,
storage contract, public business model, operator certification or license
should add or update an ADR.

## Operator Governance
Anyone may fork and operate independently. itonami.cloud certification is a
separate trust mark and should require security, audit and data-flow review.

Certified operators can lose certification for:
- bypassing dispatch or invoice-settlement policy checks
- mishandling counterparty, credit, capability-classification, or
  military-end-use/sanctions-screening data
- misrepresenting certification status
- failing to respond to security incidents
