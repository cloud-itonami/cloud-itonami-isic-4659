(ns machtooltrade.sim
  "Demo driver -- `clojure -M:dev:run`. Walks a clean, ORDINARY (below
  the capability threshold) machine order through intake -> contract
  verification -> physical dispatch (escalate/approve/commit) ->
  invoice settlement (escalate/approve/commit), then exercises every
  HARD-hold scenario, MOST IMPORTANTLY the pair that proves this
  build's TWO-AXIS design is genuinely independent:

    - mo-5: an ABOVE-threshold machine (5 simultaneous axes, 3
      micrometer positioning accuracy) sold to an ORDINARY commercial
      end-user with NO military-end-use flag -> HOLD
      `:capability-threshold-uncertified` -- axis 1 fires ALONE.
    - mo-6: a deliberately BELOW-threshold machine (the SAME ordinary
      spec as mo-1) sold to a FLAGGED military end-user -> HOLD
      `:military-end-use-unresolved` -- axis 2 fires ALONE, DESPITE
      mo-6 clearing the capability-threshold check cleanly. This is
      the core novel claim: a real EAR99-posture machine tool can
      STILL be controlled under the EAR's Military End User Rule (15
      C.F.R. §744.21) even though its own raw specs never cross the
      headline capability threshold.
    - mo-8: BOTH conditions present (above-threshold AND flagged,
      both unresolved) -> BOTH checks fire on the SAME order,
      proving the two axes are not mutually exclusive.
    - mo-9: BOTH conditions present but BOTH properly resolved
      (classified AND licensed) -> clean, full dispatch+invoice
      lifecycle, proving a controlled-AND-flagged order CAN clear
      when both axes are independently satisfied.

  Also exercised: no spec-basis, credit-uncleared, contract-missing,
  counterparty-sanctions-flag-unresolved, a double dispatch, and a
  double invoice.

  Like every sibling actor's domain checks, this actor's checks
  (`credit-uncleared`, `contract-missing`,
  `capability-threshold-uncertified`, `military-end-use-unresolved`,
  `counterparty-sanctions-flag-unresolved`) are evaluated directly at
  `:delivery/dispatch` (and sanctions at `:invoice/settle` too) rather
  than via a separate screening op -- a real dispatch decision
  validates counterparty credit, contract-on-file, capability
  classification, military end-use resolution and sanctions screening
  at the point of the act itself, not as a discrete pre-screening
  ceremony. Each check is still exercised directly and independently
  below, one order per HARD-hold scenario, following the SAME
  'exercise the failure mode directly, never only via a happy-path
  actuation' discipline `parksafety`'s ADR-2607071922 Decision 5 and
  every sibling since establish."
  (:require [langgraph.graph :as g]
            [machtooltrade.store :as store]
            [machtooltrade.operation :as op]))

(def operator {:actor-id "op-1" :actor-role :export-compliance-officer :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn -main [& _]
  (let [db (store/seed-db)
        actor (op/build db)]
    (println "== order/intake mo-1 (USA, ordinary 3-axis machine, clean) ==")
    (println (exec-op actor "t1" {:op :order/intake :subject "mo-1"
                                  :patch {:id "mo-1" :counterparty "Ridgeline Precision Manufacturing Inc"}} operator))

    (println "== contract/verify mo-1 (escalates -- human approves) ==")
    (println (exec-op actor "t2" {:op :contract/verify :subject "mo-1"} operator))
    (println (approve! actor "t2"))

    (println "== delivery/dispatch mo-1 (always escalates -- :delivery/dispatch) ==")
    (let [r (exec-op actor "t3" {:op :delivery/dispatch :subject "mo-1"} operator)]
      (println r)
      (println "-- human export-compliance officer approves --")
      (println (approve! actor "t3")))

    (println "== invoice/settle mo-1 (always escalates -- :invoice/settle) ==")
    (let [r (exec-op actor "t4" {:op :invoice/settle :subject "mo-1"} operator)]
      (println r)
      (println "-- human export-compliance officer approves --")
      (println (approve! actor "t4")))

    (println "== contract/verify mo-2 (no spec-basis -> HARD hold) ==")
    (println (exec-op actor "t5" {:op :contract/verify :subject "mo-2"} operator))

    (println "== contract/verify mo-3 (escalates -- sets up the credit-uncleared test) ==")
    (println (exec-op actor "t6" {:op :contract/verify :subject "mo-3"} operator))
    (println (approve! actor "t6"))

    (println "== delivery/dispatch mo-3 (credit not cleared -> HARD hold) ==")
    (println (exec-op actor "t7" {:op :delivery/dispatch :subject "mo-3"} operator))

    (println "== contract/verify mo-4 (escalates -- sets up the contract-missing test) ==")
    (println (exec-op actor "t8" {:op :contract/verify :subject "mo-4"} operator))
    (println (approve! actor "t8"))

    (println "== delivery/dispatch mo-4 (no contract-terms on file -> HARD hold) ==")
    (println (exec-op actor "t9" {:op :delivery/dispatch :subject "mo-4"} operator))

    (println "== contract/verify mo-5 (escalates -- sets up the capability-threshold-uncertified test) ==")
    (println (exec-op actor "t10" {:op :contract/verify :subject "mo-5"} operator))
    (println (approve! actor "t10"))

    (println "== delivery/dispatch mo-5 (5-axis/3μm machine, NEVER capability-classified, ORDINARY end-user -> HARD hold :capability-threshold-uncertified, AXIS 1 ALONE) ==")
    (println (exec-op actor "t11" {:op :delivery/dispatch :subject "mo-5"} operator))

    (println "== contract/verify mo-6 (escalates -- sets up the military-end-use-unresolved test) ==")
    (println (exec-op actor "t12" {:op :contract/verify :subject "mo-6"} operator))
    (println (approve! actor "t12"))

    (println "== delivery/dispatch mo-6 (ORDINARY 3-axis machine -- clears the capability check -- but sold to a FLAGGED military end-user -> HARD hold :military-end-use-unresolved, AXIS 2 ALONE, DISTINCT from mo-5's hold) ==")
    (println (exec-op actor "t13" {:op :delivery/dispatch :subject "mo-6"} operator))

    (println "== contract/verify mo-7 (escalates -- sets up the sanctions test) ==")
    (println (exec-op actor "t14" {:op :contract/verify :subject "mo-7"} operator))
    (println (approve! actor "t14"))

    (println "== delivery/dispatch mo-7 (sanctions screening not passed -> HARD hold) ==")
    (println (exec-op actor "t15" {:op :delivery/dispatch :subject "mo-7"} operator))

    (println "== contract/verify mo-8 (escalates -- sets up the BOTH-axes-fire-together test) ==")
    (println (exec-op actor "t16" {:op :contract/verify :subject "mo-8"} operator))
    (println (approve! actor "t16"))

    (println "== delivery/dispatch mo-8 (above-threshold AND flagged military end-user, BOTH unresolved -> BOTH :capability-threshold-uncertified AND :military-end-use-unresolved fire on the SAME order) ==")
    (println (exec-op actor "t17" {:op :delivery/dispatch :subject "mo-8"} operator))

    (println "== contract/verify mo-9 (escalates -- clean controlled-and-flagged order, BOTH axes resolved) ==")
    (println (exec-op actor "t18" {:op :contract/verify :subject "mo-9"} operator))
    (println (approve! actor "t18"))

    (println "== delivery/dispatch mo-9 (above-threshold AND flagged, but capability-classified AND military-authorized -> clean, always escalates -- :delivery/dispatch) ==")
    (let [r (exec-op actor "t19" {:op :delivery/dispatch :subject "mo-9"} operator)]
      (println r)
      (println "-- human export-compliance officer approves --")
      (println (approve! actor "t19")))

    (println "== invoice/settle mo-9 ==")
    (let [r (exec-op actor "t20" {:op :invoice/settle :subject "mo-9"} operator)]
      (println r)
      (println (approve! actor "t20")))

    (println "== delivery/dispatch mo-1 AGAIN (double-dispatch -> HARD hold) ==")
    (println (exec-op actor "t21" {:op :delivery/dispatch :subject "mo-1"} operator))

    (println "== invoice/settle mo-1 AGAIN (double-invoice -> HARD hold) ==")
    (println (exec-op actor "t22" {:op :invoice/settle :subject "mo-1"} operator))

    (println "== audit ledger ==")
    (doseq [f (store/ledger db)] (println f))

    (println "== draft machine-dispatch records ==")
    (doseq [r (store/dispatch-history db)] (println r))

    (println "== draft invoice records ==")
    (doseq [r (store/invoice-history db)] (println r))))
