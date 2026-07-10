(ns machtooltrade.store-contract-test
  "The Store contract, run against BOTH backends. Proving MemStore and
  the Datomic-backed (langchain.db) store satisfy the same contract is
  what makes 'swap the SSoT for Datomic / kotoba-server' a
  configuration change, not a rewrite -- see `cloud-itonami-isic-6511`'s
  `underwriting.store-contract-test` for the same pattern on the
  sibling actor."
  (:require [clojure.test :refer [deftest is testing]]
            [machtooltrade.store :as store]))

(defn- backends []
  [["MemStore" (store/seed-db)] ["DatomicStore" (store/datomic-seed-db)]])

(deftest read-parity
  (doseq [[label s] (backends)]
    (testing label
      (is (= "USA" (:jurisdiction (store/machine-order s "mo-1"))))
      (is (= "Ridgeline Precision Manufacturing Inc" (:counterparty (store/machine-order s "mo-1"))))
      (is (= :cnc-machining-center (:machine-category (store/machine-order s "mo-1"))))
      (is (= 3 (:simultaneous-axes (store/machine-order s "mo-1"))))
      (is (= 15 (:positioning-accuracy-micrometers (store/machine-order s "mo-1"))))
      (is (= "ATL" (:jurisdiction (store/machine-order s "mo-2"))))
      (is (false? (:credit-cleared? (store/machine-order s "mo-3"))) "mo-3 credit not cleared")
      (is (nil? (:contract-terms (store/machine-order s "mo-4"))) "mo-4 no contract-terms")
      (is (= 5 (:simultaneous-axes (store/machine-order s "mo-5"))) "mo-5 above-threshold")
      (is (= 3 (:positioning-accuracy-micrometers (store/machine-order s "mo-5"))))
      (is (false? (:capability-classification-on-file? (store/machine-order s "mo-5"))))
      (is (= 3 (:simultaneous-axes (store/machine-order s "mo-6"))) "mo-6 deliberately below-threshold")
      (is (true? (:military-end-use-flagged? (store/machine-order s "mo-6"))))
      (is (false? (:military-end-use-license-authorized? (store/machine-order s "mo-6"))))
      (is (false? (:sanctions-screened? (store/machine-order s "mo-7"))) "mo-7 sanctions not screened")
      (is (true? (:military-end-use-flagged? (store/machine-order s "mo-8"))) "mo-8 both conditions present")
      (is (= 5 (:simultaneous-axes (store/machine-order s "mo-8"))))
      (is (true? (:capability-classification-on-file? (store/machine-order s "mo-9"))) "mo-9 both conditions resolved")
      (is (true? (:military-end-use-license-authorized? (store/machine-order s "mo-9"))))
      (is (false? (:dispatched? (store/machine-order s "mo-1"))))
      (is (false? (:invoiced? (store/machine-order s "mo-1"))))
      (is (= ["mo-1" "mo-2" "mo-3" "mo-4" "mo-5" "mo-6" "mo-7" "mo-8" "mo-9"]
             (mapv :id (store/all-machine-orders s))))
      (is (nil? (store/assessment-of s "mo-1")))
      (is (= [] (store/ledger s)))
      (is (= [] (store/dispatch-history s)))
      (is (= [] (store/invoice-history s)))
      (is (zero? (store/next-dispatch-sequence s "USA")))
      (is (zero? (store/next-invoice-sequence s "USA")))
      (is (false? (store/machine-order-already-dispatched? s "mo-1")))
      (is (false? (store/machine-order-already-invoiced? s "mo-1"))))))

(deftest write-and-ledger-parity
  (doseq [[label s] (backends)]
    (testing label
      (testing "partial upsert merges, preserving untouched fields"
        (store/commit-record! s {:effect :order/upsert
                                 :value {:id "mo-1" :counterparty "Ridgeline Precision Manufacturing Inc"}})
        (is (= "Ridgeline Precision Manufacturing Inc" (:counterparty (store/machine-order s "mo-1"))))
        (is (= "USA" (:jurisdiction (store/machine-order s "mo-1"))) "unrelated field preserved"))
      (testing "contract-assessment payloads commit and read back"
        (store/commit-record! s {:effect :contract-assessment/set :path ["mo-1"]
                                 :payload {:jurisdiction "USA" :checklist ["a" "b"]}})
        (is (= {:jurisdiction "USA" :checklist ["a" "b"]} (store/assessment-of s "mo-1"))))
      (testing "machine dispatch drafts a record and advances the dispatch sequence"
        (store/commit-record! s {:effect :order/mark-dispatched :path ["mo-1"]})
        (is (= "USA-DISPATCH-000000" (get (first (store/dispatch-history s)) "record_id")))
        (is (= "machine-dispatch-draft" (get (first (store/dispatch-history s)) "kind")))
        (is (true? (:dispatched? (store/machine-order s "mo-1"))))
        (is (= 1 (count (store/dispatch-history s))))
        (is (= 1 (store/next-dispatch-sequence s "USA")))
        (is (true? (store/machine-order-already-dispatched? s "mo-1"))))
      (testing "invoice settlement drafts a record and advances the invoice sequence"
        (store/commit-record! s {:effect :order/mark-invoiced :path ["mo-1"]})
        (is (= "USA-INVOICE-000000" (get (first (store/invoice-history s)) "record_id")))
        (is (= "machine-invoice-draft" (get (first (store/invoice-history s)) "kind")))
        (is (true? (:invoiced? (store/machine-order s "mo-1"))))
        (is (= 1 (count (store/invoice-history s))))
        (is (= 1 (store/next-invoice-sequence s "USA")))
        (is (true? (store/machine-order-already-invoiced? s "mo-1"))))
      (testing "ledger is append-only and order-preserving"
        (store/append-ledger! s {:op :a :disposition :commit})
        (store/append-ledger! s {:op :b :disposition :hold})
        (is (= [:commit :hold] (mapv :disposition (store/ledger s))))))))

(deftest datomic-empty-store-is-usable
  (let [s (store/datomic-store)]
    (is (nil? (store/machine-order s "nope")))
    (is (= [] (store/all-machine-orders s)))
    (is (= [] (store/ledger s)))
    (is (= [] (store/dispatch-history s)))
    (is (= [] (store/invoice-history s)))
    (is (zero? (store/next-dispatch-sequence s "USA")))
    (is (zero? (store/next-invoice-sequence s "USA")))
    (store/with-machine-orders s {"x" {:id "x" :order-id "MO-X"
                                       :item-description "Test item" :machine-category :cnc-machining-center
                                       :simultaneous-axes 3 :positioning-accuracy-micrometers 15
                                       :capability-classification-on-file? false
                                       :destination-country "GBR" :end-user "c" :counterparty "c"
                                       :military-end-use-flagged? false :military-end-use-license-authorized? false
                                       :price 1000.0 :contract-terms "FCA warehouse, net 30 days"
                                       :credit-cleared? true :sanctions-screened? true
                                       :dispatched? false :invoiced? false
                                       :jurisdiction "USA" :status :intake
                                       :dispatch-number nil :invoice-number nil}})
    (is (= "c" (:counterparty (store/machine-order s "x"))))
    (is (= :cnc-machining-center (:machine-category (store/machine-order s "x"))) "keyword field round-trips through DatomicStore")))
