(ns machtooltrade.registry-test
  (:require [clojure.test :refer [deftest is]]
            [machtooltrade.registry :as r]))

;; ----------------------------- capability-threshold-crossed? (pure range check) -----------------------------
;;
;; UNLIKE every prior principal-trading sibling (whose domain checks are
;; direct entity BOOLEAN reads, with NO range-check functions to host in
;; their own registry -- see each sibling's own registry docstring),
;; this vertical's registry hosts a genuine pure PHYSICAL range-check
;; function, the SAME shape as the crude-extraction sibling's own
;; reservoir-pressure/annular-pressure/water-cut/H2S checks.

(deftest below-threshold-machine-is-not-capability-controlled
  (is (false? (r/capability-threshold-crossed? 3 15)) "3 axes / 15um is an ordinary general-purpose machine")
  (is (false? (r/capability-threshold-crossed? 4 10)) "4 axes still below the illustrative 5-axis threshold")
  (is (false? (r/capability-threshold-crossed? 5 10)) "5 axes but coarser than 6um accuracy -- both conditions must hold"))

(deftest above-threshold-machine-is-capability-controlled
  (is (true? (r/capability-threshold-crossed? 5 3)) "5 simultaneous axes AND finer than 6um -- crosses the illustrative threshold")
  (is (true? (r/capability-threshold-crossed? 6 1)) "more axes, finer accuracy -- still controlled"))

(deftest missing-spec-data-is-conservatively-treated-as-controlled
  (is (true? (r/capability-threshold-crossed? nil 3)) "missing axis count -> cannot verify NOT controlled")
  (is (true? (r/capability-threshold-crossed? 5 nil)) "missing accuracy -> cannot verify NOT controlled")
  (is (true? (r/capability-threshold-crossed? nil nil))))

;; ----------------------------- register-dispatch-record -----------------------------

(deftest dispatch-is-a-draft-not-a-real-dispatch
  (let [result (r/register-dispatch-record "mo-1" "USA" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest dispatch-assigns-dispatch-number
  (let [result (r/register-dispatch-record "mo-1" "USA" 7)]
    (is (= (get result "dispatch_number") "USA-DISPATCH-000007"))
    (is (= (get-in result ["record" "machine_order_id"]) "mo-1"))
    (is (= (get-in result ["record" "kind"]) "machine-dispatch-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest dispatch-validation-rules
  (is (thrown? Exception (r/register-dispatch-record "" "USA" 0)))
  (is (thrown? Exception (r/register-dispatch-record "mo-1" "" 0)))
  (is (thrown? Exception (r/register-dispatch-record "mo-1" "USA" -1))))

;; ----------------------------- register-invoice-record -----------------------------

(deftest invoice-is-a-draft-not-a-real-invoice
  (let [result (r/register-invoice-record "mo-1" "USA" 0)]
    (is (nil? (get-in result ["certificate" "proof"])))
    (is (= (get-in result ["certificate" "issued_by_registry"]) false))
    (is (= (get-in result ["certificate" "status"]) "draft-unsigned"))))

(deftest invoice-assigns-invoice-number
  (let [result (r/register-invoice-record "mo-1" "USA" 7)]
    (is (= (get result "invoice_number") "USA-INVOICE-000007"))
    (is (= (get-in result ["record" "machine_order_id"]) "mo-1"))
    (is (= (get-in result ["record" "kind"]) "machine-invoice-draft"))
    (is (= (get-in result ["record" "immutable"]) true))))

(deftest invoice-validation-rules
  (is (thrown? Exception (r/register-invoice-record "" "USA" 0)))
  (is (thrown? Exception (r/register-invoice-record "mo-1" "" 0)))
  (is (thrown? Exception (r/register-invoice-record "mo-1" "USA" -1))))

(deftest history-is-append-only
  (let [c1 (r/register-dispatch-record "mo-1" "USA" 0)
        hist (r/append [] c1)
        c2 (r/register-dispatch-record "mo-2" "USA" 1)
        hist2 (r/append hist c2)]
    (is (= 2 (count hist2)))
    (is (= "USA-DISPATCH-000000" (get-in hist2 [0 "record_id"])))
    (is (= "USA-DISPATCH-000001" (get-in hist2 [1 "record_id"])))))
