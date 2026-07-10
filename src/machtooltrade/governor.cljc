(ns machtooltrade.governor
  "Precision Machinery Export Governor -- the independent compliance
  layer that earns the MachToolTradeAdvisor the right to commit. The
  LLM has no notion of jurisdictional machine-tool export-control law,
  whether a counterparty's credit has actually been cleared, whether
  contract terms are actually on file, whether THIS specific machine's
  own raw technical specifications actually cross a real capability-
  control threshold, whether this order's end-user/end-use has
  actually been screened and resolved for a military/WMD concern,
  whether OFAC / equivalent sanctions screening has actually been
  passed, or when an act stops being a draft and becomes a real
  physical dispatch or a real invoice settlement, so this MUST be a
  separate system able to *reject* a proposal and fall back to HOLD.

  Like every principal-trading sibling's own governor, this precision-
  machinery-wholesale vertical has NO pre-existing machine-tool-trading
  capability library to delegate to -- so the domain checks (credit-
  clearance, contract-on-file, capability-threshold, military-end-use,
  sanctions-screening) are evaluated directly here: MOST are direct
  entity boolean reads off the `machine-order` record, but the
  capability-threshold check is a genuine computed range check (see
  `machtooltrade.registry/capability-threshold-crossed?`) -- a fleet
  first for the principal-trading cluster.

  `:itonami.blueprint/governor` is `:precision-machinery-export-
  governor`, grep-verified UNIQUE fleet-wide -- no naming-collision
  precedent question, a fresh independent build following the SAME
  governed-actor architecture (langgraph StateGraph + independent
  Governor + Phase 0->3 rollout) established by `cloud-itonami-isic-
  6511` and applied by the fuel-wholesale (`cloud-itonami-isic-4671`),
  computer-and-software-wholesale (`cloud-itonami-isic-4651`),
  building-materials-wholesale (`cloud-itonami-isic-4663`) and
  ag-machinery-wholesale (`cloud-itonami-isic-4653`) siblings.

  ============================================================
  CRITICAL STRUCTURAL DIFFERENCE from the computer-and-software-
  wholesale sibling's own domain-defining check (its CLOSEST thematic
  cousin -- both are export-classification actors): a TWO-AXIS design,
  not a two-step SEQUENTIAL pipeline.
  ============================================================

  `techtrade.governor`'s defining split -- `eccn-classification-
  missing-violations` then `license-required-unauthorized-violations`
  -- is a SEQUENTIAL, DEPENDENT pair: you cannot even ask the second
  question ('is a license required, and is one on file?') until the
  first ('has this item been classified at all?') has an answer, and
  the second check is a literal no-op unless the first already passed
  (see that namespace's own docstring). Both checks are ALSO about the
  SAME kind of fact: the item's own classification/license posture.

  A precision-machine-tool wholesaler's REAL exposure does not reduce
  to that shape, because export control here operates on TWO
  GENUINELY INDEPENDENT axes that vary SEPARATELY and are evaluated
  WITHOUT reference to each other:

  1. CAPABILITY (the item's own physical specifications): certain
     multi-axis-simultaneous-contouring CNC machine tools can
     themselves manufacture precision weapons components (centrifuge
     rotors, missile-guidance parts, submarine-hull frames) to
     tolerances an ordinary machine tool cannot hold -- this is why
     ECCN 2B001 (US Commerce Control List, Category 2 'Materials
     Processing') and its Wassenaar-derived equivalents (EU Annex I
     Category 2, UK Strategic Export Control Lists Category 2, Japan's
     輸出貿易管理令別表第一) gate control on the machine's OWN
     simultaneous-axis count and positioning accuracy (per ISO 230-2).
     `capability-threshold-uncertified-violations` below re-COMPUTES
     this from the machine's raw specs (`machtooltrade.registry/
     capability-threshold-crossed?`) rather than trusting a
     pre-set classification flag.
  2. END-USE / END-USER (who is buying it, and why): the EAR's
     Military End User (MEU) Rule (15 C.F.R. §744.21, substantially
     expanded in the 2020s) is a CATCH-ALL that can require a license
     for essentially ANY item subject to the EAR -- INCLUDING an
     otherwise-EAR99, below-threshold general-purpose machine tool --
     when the exporter knows (or has reason to know) the item is
     destined for a military end use or a listed military end user in
     a covered destination. `military-end-use-unresolved-violations`
     below evaluates this axis off dedicated `:military-end-use-
     flagged?`/`:military-end-use-license-authorized?` facts,
     COMPLETELY INDEPENDENTLY of the machine's own axis-count/
     positioning-accuracy specs.

  THESE TWO AXES ARE NOT SEQUENTIAL AND NOT CO-DEPENDENT: a BELOW-
  threshold machine (clears check 1 cleanly) sold to a FLAGGED
  military end-user is STILL held, by check 2 ALONE, precisely because
  a real-world EAR99 machine tool really can be controlled this way
  under §744.21 even though it never crosses the headline capability
  threshold; an ABOVE-threshold machine (fails check 1) sold to an
  ordinary commercial end-user with no end-use concern at all is held
  by check 1 ALONE, with check 2 never even engaging. `machtooltrade.
  store/demo-data`'s `mo-5` (above-threshold, ordinary end-user) and
  `mo-6` (below-threshold, flagged end-user) are the load-bearing pair
  that proves this independence directly; `mo-8` (BOTH conditions
  present) proves the two checks are not even mutually exclusive --
  they can and do fire TOGETHER on the same order; `mo-9` (BOTH
  conditions present, BOTH properly resolved) proves a controlled-AND-
  flagged order can still clear when both axes are independently
  satisfied. See `docs/adr/0001-architecture.md` Decision 4 for the
  full reasoning, including why this is a THIRD distinct shape in the
  fleet -- neither the computer-and-software sibling's sequential
  split, nor the ag-machinery sibling's own two-independent-checks
  precedent (`agmachtrade.governor`'s `:engine-powered?`/`:ride-on?`
  pair), which are BOTH product-certification checks gated by TWO
  PROPERTIES OF THE SAME MACHINE -- this build's two checks are gated
  by two genuinely DIFFERENT KINDS of fact: one computed from the
  item's own physical specification, one read from the transaction's
  end-use/end-user context, and neither's firing condition depends on
  the other's outcome or on any shared gating fact.

  This build deliberately does NOT add a third, denied-party-list-
  style screening check (the computer-and-software sibling's own
  fleet-first `denied-party-list-flag-unresolved-violations`, a NAMED-
  PARTY-LIST screening mechanism): the Military End User Rule is
  ITSELF a genuinely different mechanism again from named-party-list
  screening -- it is a CATCH-ALL that can apply based on KNOWLEDGE of
  end-use/end-user even when no party is individually listed anywhere,
  so re-adding a denied-party check here would not capture a new
  failure mode this vertical is distinctively exposed to; the generic
  `counterparty-sanctions-flag-unresolved-violations` (OFAC-style,
  reused verbatim from every sibling) is retained for the ordinary
  blocked-party case.

  Seven checks, in priority order, ALL HARD violations: a human
  approver CANNOT override them. The confidence/actuation gate is
  SOFT: it asks a human to look (low confidence / actuation), and the
  human may approve -- but see `machtooltrade.phase`: for `:stake
  :delivery/dispatch`/`:invoice/settle` (a real dispatch or invoice
  settlement) NO phase ever allows auto-commit either. Two independent
  layers agree that actuation is always a human call.

    1. Spec-basis                  -- did the jurisdiction proposal cite
                                       an OFFICIAL source
                                       (`machtooltrade.facts`), or invent
                                       one?
    2. Evidence incomplete         -- for `:delivery/dispatch`/
                                       `:invoice/settle`, has the
                                       jurisdiction actually been
                                       verified with a full GENERIC
                                       counterparty-diligence evidence
                                       checklist on file (credit-
                                       clearance record, contract/PO,
                                       sanctions-screening record,
                                       military end-use/end-user
                                       screening record)? Deliberately
                                       does NOT include the capability-
                                       threshold determination itself --
                                       that is check 5 below, re-
                                       COMPUTED independently, not a
                                       checklist item.
    3. Credit uncleared            -- for `:delivery/dispatch`, the
                                       counterparty's credit has NOT been
                                       cleared (the leasing collateral-
                                       coverage discipline, applied to
                                       counterparty credit). Evaluated
                                       before dispatch.
    4. Contract missing            -- for `:delivery/dispatch`, no
                                       contract-terms are on file for the
                                       order. Evaluated before dispatch.
    5. Capability-threshold
       uncertified                   -- for `:delivery/dispatch`, the
                                       machine's OWN raw specifications
                                       (simultaneous-axis count,
                                       positioning accuracy) cross the
                                       ECCN-2B001-style capability
                                       threshold
                                       (`machtooltrade.registry/
                                       capability-threshold-crossed?`,
                                       re-COMPUTED here, never trusted
                                       from an advisor claim) AND no
                                       formal capability-classification
                                       determination is on file. AXIS 1
                                       -- see namespace docstring.
    6. Military end-use unresolved -- for `:delivery/dispatch`, this
                                       order's end-user/end-use has been
                                       FLAGGED for a military/WMD concern
                                       (the EAR's Military End User Rule,
                                       15 C.F.R. §744.21, or an
                                       equivalent jurisdiction's own
                                       catch-all) AND no license/
                                       authorization resolving that flag
                                       is on file. AXIS 2, evaluated
                                       COMPLETELY INDEPENDENTLY of check
                                       5 -- see namespace docstring.
    7. Counterparty sanctions flag
       unresolved                    -- for `:delivery/dispatch` and
                                       `:invoice/settle`, the counterparty
                                       has NOT passed OFAC / equivalent
                                       sanctions screening -- a HARD, un-
                                       overridable hold. Evaluated
                                       UNCONDITIONALLY at both actuation
                                       ops.
    8. Confidence floor / actuation
       gate                          -- LLM confidence below threshold,
                                       OR the op is `:delivery/dispatch`/
                                       `:invoice/settle` (REAL acts)
                                       -> escalate.

  Two more guards, double-dispatch/double-invoice prevention, are
  enforced but NOT listed as numbered HARD checks above because they
  need no upstream comparison at all -- `already-dispatched-
  violations`/`already-invoiced-violations` refuse to dispatch/invoice
  the SAME machine-order twice, off dedicated `:dispatched?`/
  `:invoiced?` facts (never a `:status` value) -- the SAME 'check a
  dedicated boolean, not status' discipline every prior governor's
  guards establish, informed by `cloud-itonami-isic-6492`'s status-
  lifecycle bug (ADR-2607071320)."
  (:require [machtooltrade.facts :as facts]
            [machtooltrade.registry :as registry]
            [machtooltrade.store :as store]))

(def confidence-floor 0.6)

(def high-stakes
  "Stakes grave enough to always require a human, even when clean. A
  physical cross-border dispatch of a real machine tool and settling a
  real invoice (real money moving between counterparty and wholesaler)
  are the TWO real-world actuation events this actor performs -- a
  two-member set, matching the fuel-wholesale/building-materials/
  ag-machinery siblings' own dual-actuation shape, NOT the computer-
  and-software sibling's own three-member set (there is no deemed-
  export analog for a physical machine tool -- see `machtooltrade.
  store` namespace docstring)."
  #{:delivery/dispatch :invoice/settle})

;; ----------------------------- checks -----------------------------

(defn- spec-basis-violations
  "A `:contract/verify` (or `:delivery/dispatch`/`:invoice/settle`)
  proposal with no spec-basis citation is a HARD violation -- never
  invent a jurisdiction's machine-tool export-control requirements."
  [{:keys [op]} proposal]
  (when (contains? #{:contract/verify :delivery/dispatch :invoice/settle} op)
    (let [value (:value proposal)]
      (when (or (empty? (:cites proposal))
                (and (contains? value :spec-basis) (nil? (:spec-basis value))))
        [{:rule :no-spec-basis
          :detail "公式spec-basisの引用が無い提案は法域要件として扱えない"}]))))

(defn- evidence-incomplete-violations
  "For `:delivery/dispatch`/`:invoice/settle`, the jurisdiction's
  required GENERIC counterparty-diligence evidence (credit-clearance
  record, contract/PO, sanctions-screening record, military end-use/
  end-user screening record) must actually be satisfied -- do not
  trust the advisor's self-reported confidence alone. Deliberately
  does NOT check the capability-threshold determination -- that is
  `capability-threshold-uncertified-violations` below, a dedicated
  COMPUTED check rather than a checklist item."
  [{:keys [op subject]} st]
  (when (contains? #{:delivery/dispatch :invoice/settle} op)
    (let [mo (store/machine-order st subject)
          assessment (store/assessment-of st subject)]
      (when-not (and assessment
                     (facts/required-evidence-satisfied?
                      (:jurisdiction mo) (:checklist assessment)))
        [{:rule :evidence-incomplete
          :detail "法域の必要書類(信用審査記録/契約書またはPO/制裁スクリーニング記録/軍事最終用途スクリーニング記録)が充足していない状態での提案"}]))))

(defn- credit-uncleared-violations
  "For `:delivery/dispatch`, refuses to move a real machine tool to a
  counterparty whose credit has NOT been cleared -- counterparty
  credit not cleared (the leasing collateral-coverage discipline,
  applied to counterparty credit). Evaluated ahead of any physical
  handoff."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [mo (store/machine-order st subject)]
      (when (not (true? (:credit-cleared? mo)))
        [{:rule :credit-uncleared
          :detail (str subject " の取引先信用審査(credit-clearance)が未了 -- 出荷提案は進められない")}]))))

(defn- contract-missing-violations
  "For `:delivery/dispatch`, refuses to move a real machine tool when
  no contract-terms are on file for the order."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [mo (store/machine-order st subject)]
      (when (or (nil? (:contract-terms mo)) (= "" (:contract-terms mo)))
        [{:rule :contract-missing
          :detail (str subject " に契約条項(contract-terms)の記録が無い -- 出荷提案は進められない")}]))))

(defn- capability-threshold-uncertified-violations
  "AXIS 1 (see namespace docstring). For `:delivery/dispatch`,
  INDEPENDENTLY RE-COMPUTES whether this machine's own raw
  specifications (`:simultaneous-axes`, `:positioning-accuracy-
  micrometers`) cross the ECCN-2B001-style capability threshold via
  `machtooltrade.registry/capability-threshold-crossed?` -- never
  trusts an advisor's classification claim. Fires ONLY when the
  computed answer is true AND no formal capability-classification
  determination is on file (`:capability-classification-on-file?`
  false). A machine whose specs do NOT cross the threshold needs no
  classification record at all (the real-world EAR99 posture) -- this
  check is then a no-op regardless of `:capability-classification-on-
  file?`'s value, so `mo-1`'s ordinary 3-axis machine and `mo-6`'s
  ALSO-ordinary 3-axis machine (sold to a flagged military end-user)
  both clear THIS check cleanly, proving axis 2 below fires
  independently of axis 1."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [mo (store/machine-order st subject)]
      (when (and (registry/capability-threshold-crossed?
                  (:simultaneous-axes mo) (:positioning-accuracy-micrometers mo))
                 (not (true? (:capability-classification-on-file? mo))))
        [{:rule :capability-threshold-uncertified
          :detail (str subject " (軸数=" (:simultaneous-axes mo)
                       ", 位置決め精度=" (:positioning-accuracy-micrometers mo) "μm) は"
                       "ECCN 2B001相当の能力しきい値を超えるが、正式な能力分類の記録が無い -- "
                       "出荷提案は進められない")}]))))

(defn- military-end-use-unresolved-violations
  "AXIS 2 (see namespace docstring). For `:delivery/dispatch`, this
  order's end-user/end-use has been FLAGGED for a military/WMD concern
  (`:military-end-use-flagged?` true -- the EAR's Military End User
  Rule, 15 C.F.R. §744.21, or an equivalent jurisdiction's own
  catch-all) AND no license/authorization resolving that flag is on
  file (`:military-end-use-license-authorized?` false). Evaluated
  COMPLETELY INDEPENDENTLY of `capability-threshold-uncertified-
  violations` above -- reads NEITHER `:simultaneous-axes` NOR
  `:positioning-accuracy-micrometers` NOR `:capability-classification-
  on-file?` at all. This is what makes `mo-6` (an ordinary, BELOW-
  threshold machine that clears check 5 cleanly) still HOLD here: a
  real EAR99 machine tool really can be controlled this way when sold
  to a listed/flagged military end-user, independent of its own raw
  capability."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (let [mo (store/machine-order st subject)]
      (when (and (true? (:military-end-use-flagged? mo))
                 (not (true? (:military-end-use-license-authorized? mo))))
        [{:rule :military-end-use-unresolved
          :detail (str subject " の需要者/最終用途が軍事・大量破壊兵器関連としてフラグ済みで、"
                       "許可(EAR §744.21相当)の記録が無い -- 出荷提案は進められない")}]))))

(defn- counterparty-sanctions-flag-unresolved-violations
  "For `:delivery/dispatch` and `:invoice/settle`, an unresolved
  sanctions-screening flag -- the counterparty has NOT passed OFAC /
  equivalent sanctions screening -- is a HARD, un-overridable hold.
  Evaluated UNCONDITIONALLY at both actuation ops: neither the machine
  nor money moves against an unscreened counterparty."
  [{:keys [op subject]} st]
  (when (contains? high-stakes op)
    (let [mo (store/machine-order st subject)]
      (when (not (true? (:sanctions-screened? mo)))
        [{:rule :counterparty-sanctions-flag-unresolved
          :detail (str subject " の取引先制裁スクリーニング(OFAC等)が未了 -- 出荷・請求提案は進められない")}]))))

(defn- already-dispatched-violations
  "For `:delivery/dispatch`, refuses to dispatch the SAME machine-order
  twice, off a dedicated `:dispatched?` fact (never a `:status` value)."
  [{:keys [op subject]} st]
  (when (= op :delivery/dispatch)
    (when (store/machine-order-already-dispatched? st subject)
      [{:rule :already-dispatched
        :detail (str subject " は既に出荷済み")}])))

(defn- already-invoiced-violations
  "For `:invoice/settle`, refuses to settle the SAME machine-order's
  invoice twice, off a dedicated `:invoiced?` fact (never a `:status`
  value)."
  [{:keys [op subject]} st]
  (when (= op :invoice/settle)
    (when (store/machine-order-already-invoiced? st subject)
      [{:rule :already-invoiced
        :detail (str subject " は既に請求済み")}])))

(defn check
  "Censors a MachToolTradeAdvisor proposal against the governor rules.
  Returns {:ok? bool :violations [..] :confidence c :escalate? bool
  :high-stakes? bool :hard? bool}."
  [request _context proposal st]
  (let [hard (into []
                   (concat (spec-basis-violations request proposal)
                           (evidence-incomplete-violations request st)
                           (credit-uncleared-violations request st)
                           (contract-missing-violations request st)
                           (capability-threshold-uncertified-violations request st)
                           (military-end-use-unresolved-violations request st)
                           (counterparty-sanctions-flag-unresolved-violations request st)
                           (already-dispatched-violations request st)
                           (already-invoiced-violations request st)))
        conf (:confidence proposal 0.0)
        low? (< conf confidence-floor)
        stakes? (boolean (high-stakes (:stake proposal)))
        hard? (boolean (seq hard))]
    {:ok?          (and (not hard?) (not low?) (not stakes?))
     :violations   hard
     :confidence   conf
     :hard?        hard?
     :escalate?    (and (not hard?) (or low? stakes?))
     :high-stakes? stakes?}))

(defn hold-fact
  "The audit fact written when a proposal is rejected (HOLD)."
  [request context verdict]
  {:t          :governor-hold
   :op         (:op request)
   :actor      (:actor-id context)
   :subject    (:subject request)
   :disposition :hold
   :basis      (mapv :rule (:violations verdict))
   :violations (:violations verdict)
   :confidence (:confidence verdict)})
