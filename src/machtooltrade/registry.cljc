(ns machtooltrade.registry
  "Pure-function machine-dispatch + invoice record construction -- an
  append-only precision-machinery-wholesale book-of-record draft -- AND
  the pure capability-threshold range-check function the Precision
  Machinery Export Governor calls to INDEPENDENTLY re-verify a
  machine's own physical specifications before any dispatch.

  UNLIKE every principal-trading sibling whose domain checks are direct
  entity BOOLEAN reads (fuel-wholesale's credit/contract/sanctions,
  computer-and-software-wholesale's credit/contract/ECCN-presence/
  license/sanctions/denied-party, building-materials-wholesale's lead-
  free certificate, ag-machinery-wholesale's emissions/ROPS
  certificates -- none of which require ANY arithmetic, only 'is this
  fact true/false/present'), THIS vertical's defining check requires an
  actual COMPUTATION: whether a machine tool is itself export-
  controlled is determined by whether its OWN raw technical
  specifications (simultaneous-axis count, positioning accuracy) cross
  a real regulatory threshold (ECCN 2B001's headline multi-axis-
  simultaneous-contouring + positioning-accuracy criteria) -- not by
  reading a pre-set classification flag the advisor could simply
  assert. This makes `capability-threshold-crossed?` below a genuine
  pure PHYSICAL range-check function, the SAME shape as the crude-
  extraction sibling's own reservoir-pressure/annular-pressure/water-
  cut/H2S range checks (`crude.registry`) -- a fleet-first for the
  principal-trading (wholesale) cluster specifically, none of whose
  siblings before this one needed to host a range-check function at
  all (see each sibling's own registry docstring: 'RECORD CONSTRUCTION
  ONLY -- no pure range checks to host here').

  Like every sibling actor's registry, there is no single international
  reference-number standard for a machine-dispatch or invoice record --
  every operator/jurisdiction assigns its own reference format. This
  namespace does NOT invent one beyond a jurisdiction-scoped sequence
  number; it validates the record's required fields, the same honest,
  non-fabricating discipline `machtooltrade.facts` uses.

  This namespace is pure data + pure functions -- no I/O, no network
  call to any real warehouse-management/rigging/ERP/billing system. It
  builds the RECORD an operator would keep, not the act of dispatching a
  real machine tool or settling a real invoice itself (that is
  `machtooltrade.operation`'s `:delivery/dispatch`/`:invoice/settle`,
  always human-gated -- see README `Actuation`)."
  (:require [clojure.string :as str]))

(defn- unsigned-certificate
  "Every certificate this actor produces is UNSIGNED -- signature is
  the operator's act, not this actor's. See README `Actuation`."
  [kind subject record-id]
  {"@context" ["https://www.w3.org/ns/credentials/v2"]
   "type" ["VerifiableCredential" kind]
   "credentialSubject" {"id" subject "record" record-id}
   "proof" nil
   "issued_by_registry" false
   "status" "draft-unsigned"})

(defn- zero-pad [n w]
  (let [s (str n)]
    (str (apply str (repeat (max 0 (- w (count s))) "0")) s)))

;; ----------------------------- capability-threshold range check (pure) -----------------------------
;;
;; The Precision Machinery Export Governor calls this to INDEPENDENTLY
;; re-verify a machine's own recorded physical specifications against
;; the ECCN 2B001-style capability threshold before authorizing a
;; dispatch -- it does NOT trust an advisor's self-reported
;; classification claim. [CONFIDENCE NOTE: the illustrative numeric
;; thresholds below (>= 5 simultaneous axes AND <= 6 micrometers
;; positioning accuracy) are a SIMPLIFIED composite standing in for
;; ECCN 2B001's real (and considerably more granular -- separate
;; sub-paragraphs and thresholds per machine-tool TYPE: grinding,
;; turning, milling/machining-center, etc.) structure. I have HIGH
;; confidence that simultaneous-axis count and positioning accuracy
;; (per ISO 230-2) are the real, load-bearing technical parameters
;; 2B001 actually turns on; I have LOW confidence in the specific
;; numeric threshold values reproduced here, which are illustrative
;; for this R0's fixtures/tests, NOT a verified reproduction of the
;; current regulatory text -- see `docs/business-model.md`
;; 'Jurisdiction coverage (honest)' for the full confidence gradient. A
;; real deployment MUST replace these thresholds with a verified,
;; current, machine-type-specific reproduction of the actual control
;; list text (or better, a callout to a maintained classification
;; service) before relying on this function for a real export
;; decision.
(defn capability-threshold-crossed?
  "Does this machine's OWN raw technical specification cross the
  ECCN-2B001-style capability threshold that makes it export-
  controlled BY ITS OWN CAPABILITY (independent of who it is being
  sold to -- see `machtooltrade.governor`'s `military-end-use-
  unresolved-violations` for the SEPARATE, independent end-use/end-
  user axis)? Missing spec data -> true (cannot verify the machine is
  NOT controlled, so treat conservatively as controlled -- the SAME
  'missing data -> violation' conservative discipline the crude-
  extraction sibling's own range checks establish)."
  [simultaneous-axes positioning-accuracy-micrometers]
  (cond
    (or (nil? simultaneous-axes) (nil? positioning-accuracy-micrometers)) true
    (and (>= simultaneous-axes 5) (<= positioning-accuracy-micrometers 6)) true
    :else false))

;; ----------------------------- record construction -----------------------------

(defn register-dispatch-record
  "Validate + construct the MACHINE-DISPATCH registration DRAFT -- the
  operator's own legal act of dispatching a real machine tool to a
  counterparty. Pure function -- does not touch any real warehouse-
  management, rigging or ERP system; it builds the RECORD an operator
  would keep. `machtooltrade.governor` independently re-verifies the
  counterparty's credit-clearance, contract-on-file, capability-
  threshold classification, military end-use/end-user resolution,
  sanctions-screening and evidence-completeness ground truth, and
  blocks a double-dispatch of the same machine-order, before this is
  ever allowed to commit."
  [machine-order-id jurisdiction sequence]
  (when-not (and machine-order-id (not= machine-order-id ""))
    (throw (ex-info "machine-dispatch: machine_order_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "machine-dispatch: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "machine-dispatch: sequence must be >= 0" {})))
  (let [dispatch-number (str (str/upper-case jurisdiction) "-DISPATCH-" (zero-pad sequence 6))
        record {"record_id" dispatch-number
                "kind" "machine-dispatch-draft"
                "machine_order_id" machine-order-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "dispatch_number" dispatch-number
     "certificate" (unsigned-certificate "MachineDispatch" dispatch-number dispatch-number)}))

(defn register-invoice-record
  "Validate + construct the INVOICE registration DRAFT -- the operator's
  own legal act of settling a real precision-machinery-wholesale
  invoice (the money side of the trade, custody/financial transfer).
  Pure function -- does not touch any real billing or accounts-
  receivable system; it builds the RECORD an operator would keep.
  `machtooltrade.governor` independently re-verifies the sanctions-
  screening and evidence-completeness ground truth, and blocks a
  double-invoice of the same machine-order, before this is ever allowed
  to commit."
  [machine-order-id jurisdiction sequence]
  (when-not (and machine-order-id (not= machine-order-id ""))
    (throw (ex-info "machine-invoice: machine_order_id required" {})))
  (when-not (and jurisdiction (not= jurisdiction ""))
    (throw (ex-info "machine-invoice: jurisdiction required" {})))
  (when (< sequence 0)
    (throw (ex-info "machine-invoice: sequence must be >= 0" {})))
  (let [invoice-number (str (str/upper-case jurisdiction) "-INVOICE-" (zero-pad sequence 6))
        record {"record_id" invoice-number
                "kind" "machine-invoice-draft"
                "machine_order_id" machine-order-id
                "jurisdiction" jurisdiction
                "immutable" true}]
    {"record" record "invoice_number" invoice-number
     "certificate" (unsigned-certificate "MachineInvoice" invoice-number invoice-number)}))

(defn append [history result]
  (conj (vec history) (get result "record")))
