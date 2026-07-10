# Security Policy

This project handles capability-classification, military-end-use-
resolution, counterparty-credit and sanctions-screening workflows.
Treat vulnerabilities as potentially high impact even when the demo
data is synthetic.

## Do Not Disclose Publicly

Report privately before opening public issues for:

- credential exposure
- real counterparty, credit, capability-classification, or military-
  end-use/trade data exposure
- authorization bypass
- Precision Machinery Export Governor bypass
- audit-ledger tampering
- over-disclosure in reports or exports
- tenant isolation failures

## Reporting

Use GitHub private vulnerability reporting when available for the repository.
If that is unavailable, contact the repository maintainers through the
cloud-itonami organization before publishing details.

Include:

- affected commit or version
- reproduction steps
- expected and actual behavior
- impact on counterparty data, capability-classification/military-end-
  use integrity, or audit logging
- suggested fix, if known

## Production Guidance

- Store secrets outside Git.
- Keep real counterparty, credit, capability-classification and
  military-end-use/sanctions-screening data outside this repository.
- Run policy tests before deployment.
- Export and review audit logs regularly.
- Use least privilege for operators and service accounts.
- Replace the illustrative capability-threshold numeric parameters in
  `machtooltrade.registry/capability-threshold-crossed?` with a
  verified, current, machine-type-specific reproduction of the actual
  control-list text (or a maintained classification service) before
  relying on this actor for a real export decision -- see that
  function's own docstring for the confidence caveat.
