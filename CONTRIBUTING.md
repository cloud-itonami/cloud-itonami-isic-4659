# Contributing

`cloud-itonami-isic-4659` accepts contributions to the OSS blueprint, the
Precision Machinery Export Governor, decision-rule tests, documentation
and operator model.

## Development
The capability layer is SELF-CONTAINED. There is no pre-existing bespoke
precision-machinery-wholesale capability library to wrap; the
counterparty-credit / contract-on-file / capability-threshold / military-
end-use / sanctions-screening checks live directly in
`machtooltrade.governor` (and the pure capability-threshold range check
in `machtooltrade.registry`). This repo holds the business blueprint, the
langgraph-clj actor and the operator contracts.

```bash
clojure -M:dev:test
clojure -M:lint
```

## Rules
- Do not commit real counterparty, credit, capability-classification, or
  military-end-use/sanctions-screening data.
- Keep physical dispatch and invoice settlement behind the Precision
  Machinery Export Governor.
- Treat export-control workflows as high-risk: add tests for spec-basis,
  evidence completeness, credit clearance, contract-on-file, capability-
  threshold classification, military-end-use resolution, sanctions
  screening and audit logging.
- Keep `capability-threshold-uncertified` and `military-end-use-
  unresolved` as two INDEPENDENT checks -- do not fold them into one
  rule, and do not make either conditional on the other (see
  `docs/adr/0001-architecture.md` Decision 4 for why the two-axis
  independence is this build's defining design decision).
- If you touch `machtooltrade.registry/capability-threshold-crossed?`,
  keep its numeric thresholds clearly flagged as illustrative/uncertain
  in the docstring unless you have independently verified them against
  the current regulatory text -- never silently upgrade an unverified
  number to look authoritative.
- Never fabricate a jurisdiction's machine-tool export-control
  requirements in `machtooltrade.facts` -- cite a real official source
  or leave the jurisdiction out of the catalog.
- Document any new business-model or operator assumption in `docs/`.

## Pull Requests
PRs should describe: what behavior changed, which governor invariant is
affected, how it was tested, whether operator or certification docs need
updates.
