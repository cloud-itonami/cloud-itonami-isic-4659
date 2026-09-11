(ns machtooltrade.governor-contract-test
  "The governor contract as executable tests. The single invariant
  under test:

    MachToolTradeAdvisor never dispatches a machine tool or settles an
    invoice the Precision Machinery Export Governor would reject,
    `:delivery/dispatch`/`:invoice/settle` NEVER auto-commit at any
    phase, `:order/intake` (no direct capital/export-control risk) MAY
    auto-commit when clean, and every decision (commit OR hold) leaves
    exactly one ledger fact.

  This file ALSO proves the fleet-differentiating claim from
  `machtooltrade.governor`'s namespace docstring end-to-end: this
  build's TWO-AXIS design (`capability-threshold-uncertified` /
  `military-end-use-unresolved`) is genuinely INDEPENDENT, not
  sequential (contrast the computer-and-software-wholesale sibling's
  own classify-then-license split) and not two properties of the SAME
  machine (contrast the ag-machinery-wholesale sibling's own engine-
  powered/ride-on pair). `mo-5` (above-threshold, ordinary end-user)
  and `mo-6` (below-threshold, flagged end-user) are the load-bearing
  pair: each holds on a DIFFERENT rule, and each explicitly does NOT
  also trigger the OTHER rule -- proving the two checks are evaluated
  independently of each other, not as two branches of one combined
  rule. `mo-8` (both conditions present) proves the two checks are not
  even mutually exclusive. `mo-9` (both conditions present, both
  resolved) proves a controlled-AND-flagged order can still clear."
  (:require [clojure.test :refer [deftest is testing]]
            [langgraph.graph :as g]
            [machtooltrade.store :as store]
            [machtooltrade.operation :as op]))

(defn- fresh []
  (let [db (store/seed-db)]
    [db (op/build db)]))

(def operator {:actor-id "op-1" :actor-role :export-compliance-officer :phase 3})

(defn- exec-op [actor tid request context]
  (g/run* actor {:request request :context context} {:thread-id tid}))

(defn- approve! [actor tid]
  (g/run* actor {:approval {:status :approved :by "op-1"}} {:thread-id tid :resume? true}))

(defn- verify!
  "Walks `subject` through contract verify -> approve, leaving a
  contract assessment on file. Uses distinct thread-ids per call
  site by suffixing `tid-prefix`."
  [actor tid-prefix subject]
  (exec-op actor (str tid-prefix "-verify") {:op :contract/verify :subject subject} operator)
  (approve! actor (str tid-prefix "-verify")))

(deftest clean-intake-auto-commits
  (let [[db actor] (fresh)
        res (exec-op actor "t1"
                  {:op :order/intake :subject "mo-1"
                   :patch {:id "mo-1" :counterparty "Ridgeline Precision Manufacturing Inc"}} operator)]
    (is (= :commit (get-in res [:state :disposition])))
    (is (= "Ridgeline Precision Manufacturing Inc" (:counterparty (store/machine-order db "mo-1"))) "SSoT actually updated")
    (is (= 1 (count (store/ledger db))))))

(deftest contract-verify-always-needs-approval
  (testing "contract verify is never in any phase's :auto set -- always human approval, even when clean"
    (let [[db actor] (fresh)
          res (exec-op actor "t2" {:op :contract/verify :subject "mo-1"} operator)]
      (is (= :interrupted (:status res)))
      (let [r2 (approve! actor "t2")]
        (is (= :commit (get-in r2 [:state :disposition])))
        (is (some? (store/assessment-of db "mo-1")))))))

(deftest fabricated-jurisdiction-is-held
  (testing "a contract/verify proposal with no official spec-basis -> HOLD, never reaches a human"
    (let [[db actor] (fresh)
          res (exec-op actor "t3"
                    {:op :contract/verify :subject "mo-2"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:no-spec-basis} (-> (store/ledger db) first :basis)))
      (is (nil? (store/assessment-of db "mo-2")) "no assessment written"))))

(deftest dispatch-without-assessment-is-held
  (testing "delivery/dispatch before any contract verification -> HOLD (evidence incomplete)"
    (let [[db actor] (fresh)
          res (exec-op actor "t4" {:op :delivery/dispatch :subject "mo-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:evidence-incomplete} (-> (store/ledger db) first :basis))))))

(deftest credit-uncleared-is-held-and-unoverridable
  (testing "a counterparty whose credit has not been cleared -> HOLD, and never reaches request-approval -- the leasing collateral-coverage discipline applied to counterparty credit"
    (let [[db actor] (fresh)
          _ (verify! actor "t5pre" "mo-3")
          res (exec-op actor "t5" {:op :delivery/dispatch :subject "mo-3"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:credit-uncleared} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest contract-missing-is-held-and-unoverridable
  (testing "an order with no contract-terms on file -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t6pre" "mo-4")
          res (exec-op actor "t6" {:op :delivery/dispatch :subject "mo-4"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:contract-missing} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest capability-threshold-uncertified-is-held-and-unoverridable
  (testing "an above-threshold machine (5 axes, 3um), NEVER capability-classified, sold to an ORDINARY commercial end-user with NO military-end-use flag -> HOLD :capability-threshold-uncertified, AXIS 1 ALONE"
    (let [[db actor] (fresh)
          mo (store/machine-order db "mo-5")]
      (is (false? (:military-end-use-flagged? mo)) "mo-5 has no end-use flag at all -- the capability axis alone must catch this")
      (let [_ (verify! actor "t7pre" "mo-5")
            res (exec-op actor "t7" {:op :delivery/dispatch :subject "mo-5"} operator)]
        (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
        (is (not= :interrupted (:status res)))
        (is (some #{:capability-threshold-uncertified} (-> (store/ledger db) last :basis)))
        (is (not (some #{:military-end-use-unresolved} (-> (store/ledger db) last :basis)))
            "mo-5 has no end-use flag -- the end-use check must NOT fire for this order")
        (is (empty? (store/dispatch-history db)))))))

(deftest military-end-use-unresolved-is-a-genuinely-different-failure-mode-from-capability-threshold
  (testing "a BELOW-threshold, ordinary machine (SAME spec as mo-1, clears the capability check cleanly) sold to a FLAGGED military end-user -> HOLD :military-end-use-unresolved, AXIS 2 ALONE, proving the two axes are independent"
    (let [[db actor] (fresh)
          mo (store/machine-order db "mo-6")]
      (is (= 3 (:simultaneous-axes mo)) "mo-6 is deliberately the SAME ordinary spec as mo-1")
      (is (= (:simultaneous-axes (store/machine-order db "mo-1")) (:simultaneous-axes mo)))
      (is (true? (:military-end-use-flagged? mo)))
      (let [_ (verify! actor "t8pre" "mo-6")
            res (exec-op actor "t8" {:op :delivery/dispatch :subject "mo-6"} operator)]
        (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
        (is (not= :interrupted (:status res)))
        (is (some #{:military-end-use-unresolved} (-> (store/ledger db) last :basis)))
        (is (not (some #{:capability-threshold-uncertified} (-> (store/ledger db) last :basis)))
            "mo-6 clears the capability-threshold check cleanly -- an unresolved military-end-use flag on an otherwise-uncontrolled machine must STILL hold, independent of capability")
        (is (empty? (store/dispatch-history db)))))))

(deftest both-axes-fire-independently-when-both-conditions-present
  (testing "an above-threshold machine AND a flagged military end-user, BOTH unresolved -> BOTH :capability-threshold-uncertified AND :military-end-use-unresolved fire on the SAME order -- the two axes are not mutually exclusive"
    (let [[db actor] (fresh)
          _ (verify! actor "t9pre" "mo-8")
          res (exec-op actor "t9" {:op :delivery/dispatch :subject "mo-8"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:capability-threshold-uncertified} (-> (store/ledger db) last :basis)))
      (is (some #{:military-end-use-unresolved} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest controlled-and-flagged-order-clears-when-both-axes-are-resolved
  (testing "an above-threshold machine AND a flagged military end-user, BOTH properly resolved (classified AND licensed) -> clean, always escalates for human approval (never hold, never auto)"
    (let [[db actor] (fresh)
          mo (store/machine-order db "mo-9")]
      (is (true? (:capability-classification-on-file? mo)))
      (is (true? (:military-end-use-license-authorized? mo)))
      (let [_ (verify! actor "t10pre" "mo-9")
            r1 (exec-op actor "t10" {:op :delivery/dispatch :subject "mo-9"} operator)]
        (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
        (let [r2 (approve! actor "t10")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:dispatched? (store/machine-order db "mo-9")))))))))

(deftest counterparty-sanctions-flag-unresolved-is-held-and-unoverridable
  (testing "a counterparty that has not passed OFAC / equivalent sanctions screening -> HOLD, and never reaches request-approval"
    (let [[db actor] (fresh)
          _ (verify! actor "t11pre" "mo-7")
          res (exec-op actor "t11" {:op :delivery/dispatch :subject "mo-7"} operator)]
      (is (= :hold (get-in res [:state :disposition])) "settles immediately, no interrupt")
      (is (not= :interrupted (:status res)))
      (is (some #{:counterparty-sanctions-flag-unresolved} (-> (store/ledger db) last :basis)))
      (is (empty? (store/dispatch-history db))))))

(deftest delivery-dispatch-always-escalates-then-human-decides
  (testing "a clean, fully-verified, credit-cleared, contract-on-file, screened, below-threshold order still ALWAYS interrupts for human approval -- :delivery/dispatch is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t12pre" "mo-1")
          r1 (exec-op actor "t12" {:op :delivery/dispatch :subject "mo-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, dispatch record drafted"
        (let [r2 (approve! actor "t12")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:dispatched? (store/machine-order db "mo-1"))))
          (is (= 1 (count (store/dispatch-history db))) "one draft dispatch record"))))))

(deftest invoice-settle-always-escalates-then-human-decides
  (testing "a clean, already-dispatched order still ALWAYS interrupts for human approval -- :invoice/settle is never auto"
    (let [[db actor] (fresh)
          _ (verify! actor "t13pre" "mo-1")
          _ (exec-op actor "t13dispatch" {:op :delivery/dispatch :subject "mo-1"} operator)
          _ (approve! actor "t13dispatch")
          r1 (exec-op actor "t13" {:op :invoice/settle :subject "mo-1"} operator)]
      (is (= :interrupted (:status r1)) "pauses for human approval even when governor-clean")
      (testing "approve -> commit, invoice record drafted"
        (let [r2 (approve! actor "t13")]
          (is (= :commit (get-in r2 [:state :disposition])))
          (is (true? (:invoiced? (store/machine-order db "mo-1"))))
          (is (= 1 (count (store/invoice-history db))) "one draft invoice record"))))))

(deftest delivery-dispatch-double-dispatch-is-held
  (testing "dispatching the same machine-order twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t14pre" "mo-1")
          _ (exec-op actor "t14a" {:op :delivery/dispatch :subject "mo-1"} operator)
          _ (approve! actor "t14a")
          res (exec-op actor "t14" {:op :delivery/dispatch :subject "mo-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-dispatched} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/dispatch-history db))) "still only the one earlier dispatch"))))

(deftest invoice-settle-double-invoice-is-held
  (testing "settling the same machine-order's invoice twice -> HOLD on the second attempt"
    (let [[db actor] (fresh)
          _ (verify! actor "t15pre" "mo-1")
          _ (exec-op actor "t15dispatch" {:op :delivery/dispatch :subject "mo-1"} operator)
          _ (approve! actor "t15dispatch")
          _ (exec-op actor "t15a" {:op :invoice/settle :subject "mo-1"} operator)
          _ (approve! actor "t15a")
          res (exec-op actor "t15" {:op :invoice/settle :subject "mo-1"} operator)]
      (is (= :hold (get-in res [:state :disposition])))
      (is (some #{:already-invoiced} (-> (store/ledger db) last :basis)))
      (is (= 1 (count (store/invoice-history db))) "still only the one earlier invoice"))))

(deftest every-decision-leaves-one-ledger-fact
  (testing "write-only-through-ledger: N operations -> N ledger facts"
    (let [[db actor] (fresh)]
      (exec-op actor "a" {:op :order/intake :subject "mo-1"
                          :patch {:id "mo-1" :counterparty "Ridgeline Precision Manufacturing Inc"}} operator)
      (exec-op actor "b" {:op :contract/verify :subject "mo-2"} operator)
      (is (= 2 (count (store/ledger db)))
          "one commit + one hold, both recorded"))))
