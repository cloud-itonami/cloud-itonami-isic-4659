(ns machtooltrade.store
  "SSoT for the precision-machinery-wholesale actor, behind a `Store`
  protocol so the backend is a swap, not a rewrite -- the same seam
  every prior `cloud-itonami-isic-*` actor in this fleet uses.

    - `MemStore`     -- atom of EDN. The deterministic default for
                        dev/tests/demo (no deps).
    - `DatomicStore` -- backed by `langchain.db`, a Datomic-API-compatible
                        EAV store (datalog q / pull / upsert). Pure `.cljc`,
                        so it runs offline AND can be pointed at a real
                        Datomic Local or a kotoba-server pod by swapping
                        `langchain.db`'s `:db-api` (see langchain.kotoba-db).

  Both implement the same protocol and pass the same contract
  (test/machtooltrade/store_contract_test.clj), which is the whole
  point: the actor, the Precision Machinery Export Governor and the
  audit ledger never know which SSoT they run on.

  Like the fuel-wholesale / building-materials-wholesale / ag-
  machinery-wholesale siblings (and unlike the computer-and-software-
  wholesale sibling's THREE-member actuation set), this vertical has
  exactly TWO real-world actuation events, applied SEQUENTIALLY to the
  SAME `machine-order`: `:delivery/dispatch` (physical dispatch of a
  real machine tool) and `:invoice/settle` (the money side). There is
  no analog to the computer-and-software sibling's deemed-export
  `:technology/release` op here -- a machine tool is a physical good;
  its export event IS its physical cross-border dispatch, full stop.
  Dedicated double-actuation-guard booleans (`:dispatched?`/
  `:invoiced?`, never a `:status` value) prevent re-running either
  against the same order.

  The ledger stays append-only on every backend: which machine-order
  was verified for a jurisdiction with no official spec-basis, which
  machine's OWN specifications crossed the capability threshold with
  no classification on file, which order's end-user/end-use was
  flagged for a military/WMD concern with no license resolution on
  file, which counterparty had an unresolved sanctions flag, which
  order was dispatched or invoiced, on what jurisdictional and
  technical basis, approved by whom -- always a query over an
  immutable log."
  (:require #?(:clj  [clojure.edn :as edn]
               :cljs [cljs.reader :as edn])
            [machtooltrade.registry :as registry]
            [langchain.db :as d]))

(defprotocol Store
  (machine-order [s id])
  (all-machine-orders [s])
  (assessment-of [s machine-order-id] "committed contract assessment, or nil")
  (ledger [s])
  (dispatch-history [s] "the append-only machine-dispatch history (machtooltrade.registry drafts)")
  (invoice-history [s] "the append-only invoice history (machtooltrade.registry drafts)")
  (next-dispatch-sequence [s jurisdiction] "next dispatch-number sequence for a jurisdiction")
  (next-invoice-sequence [s jurisdiction] "next invoice-number sequence for a jurisdiction")
  (machine-order-already-dispatched? [s machine-order-id] "has this order already been dispatched?")
  (machine-order-already-invoiced? [s machine-order-id] "has this order's invoice already been settled?")
  (commit-record! [s record] "apply a committed op's record to the SSoT")
  (append-ledger! [s fact]   "append one immutable decision fact")
  (with-machine-orders [s machine-orders] "replace/seed the machine-order directory (map id->machine-order)"))

;; ----------------------------- demo data -----------------------------

(defn- base-order
  "The neutral, clean machine-order shape (every field in its safe
  state, and BELOW the capability threshold), so each demo order below
  isolates exactly ONE failure mode by overriding a small, targeted
  field set. `:simultaneous-axes` 3 / `:positioning-accuracy-
  micrometers` 15 is a deliberately ORDINARY general-purpose 3-axis
  machining center -- well clear of the illustrative 2B001-style
  threshold (`machtooltrade.registry/capability-threshold-crossed?`),
  so `:capability-classification-on-file?` being false here is
  harmless (the check is a no-op for an uncontrolled machine, the SAME
  'EAR99 needs no classification record' real-world posture the EAR
  itself recognizes)."
  [overrides]
  (merge {:id "mo-1" :order-id "MO-2026-0001"
          :item-description "3-axis CNC vertical machining center, general-purpose"
          :machine-category :cnc-machining-center
          :simultaneous-axes 3 :positioning-accuracy-micrometers 15
          :capability-classification-on-file? false
          :destination-country "USA" :end-user "Ridgeline Precision Manufacturing Inc"
          :counterparty "Ridgeline Precision Manufacturing Inc"
          :military-end-use-flagged? false :military-end-use-license-authorized? false
          :price 240000.00 :contract-terms "FOB origin, net 45 days"
          :credit-cleared? true :sanctions-screened? true
          :dispatched? false :invoiced? false
          :jurisdiction "USA" :status :intake
          :dispatch-number nil :invoice-number nil}
         overrides))

(defn demo-data
  "A small, self-contained machine-order set covering both actuation
  lifecycles (dispatch, invoice settlement) plus the Precision
  Machinery Export Governor's own checks, so the actor + tests run
  offline. Each violation order isolates exactly ONE failure mode (the
  rest stay clean) following the 'exercise the failure mode directly,
  never only via a happy-path actuation' discipline every sibling
  governor's demo data establishes.

  `mo-5` and `mo-6` are the load-bearing pair that proves this build's
  TWO-AXIS design is genuinely independent, not sequential or
  co-dependent (contrast the computer-and-software sibling's
  SEQUENTIAL classify-then-license split, where the second check is a
  no-op unless the first already passed):
    - `mo-5`: an ABOVE-threshold machine (5 simultaneous axes, 3
      micrometer positioning accuracy) sold to an ORDINARY commercial
      end-user with NO military-end-use flag at all -- caught SOLELY
      by the capability-threshold check, proving the capability axis
      fires independently of end-use.
    - `mo-6`: a deliberately BELOW-threshold machine (3 axes, 15
      micrometers -- the SAME ordinary spec as `mo-1`, which clears the
      capability check cleanly) sold to a FLAGGED military end-user --
      caught SOLELY by the military-end-use check, proving the
      end-use axis fires independently of, and DESPITE clearing, the
      capability-threshold check. This is the core novel claim: an
      otherwise-EAR99 machine tool can STILL be controlled under the
      EAR's Military End User Rule (15 C.F.R. §744.21) even though its
      own raw specs never cross the headline capability threshold.
  `mo-8` further proves the two checks are not mutually exclusive: an
  above-threshold machine AND a flagged military end-user, both
  unresolved, fire BOTH checks on the SAME order. `mo-9` proves a
  controlled-AND-flagged order can still clear when BOTH axes are
  properly resolved (classified and licensed)."
  []
  {:machine-orders
   (into {}
         (for [o [(base-order {:id "mo-1" :order-id "MO-2026-0001"})

                  (base-order {:id "mo-2" :order-id "MO-2026-0002"
                               :counterparty "Atlantis Fabrication Works"
                               :end-user "Atlantis Fabrication Works"
                               :jurisdiction "ATL"})

                  (base-order {:id "mo-3" :order-id "MO-2026-0003"
                               :counterparty "Cedar Tooling Supply Co"
                               :end-user "Cedar Tooling Supply Co"
                               :credit-cleared? false})

                  (base-order {:id "mo-4" :order-id "MO-2026-0004"
                               :counterparty "Delta Precision Works BV"
                               :end-user "Delta Precision Works BV"
                               :contract-terms nil})

                  (base-order {:id "mo-5" :order-id "MO-2026-0005"
                               :item-description "5-axis simultaneous CNC machining center, sub-micron finishing"
                               :counterparty "Eagle Aerostructures SA"
                               :end-user "Eagle Aerostructures SA"
                               :simultaneous-axes 5 :positioning-accuracy-micrometers 3
                               :capability-classification-on-file? false})

                  (base-order {:id "mo-6" :order-id "MO-2026-0006"
                               :counterparty "Falcon State Ordnance Works"
                               :end-user "Falcon State Ordnance Works"
                               :military-end-use-flagged? true
                               :military-end-use-license-authorized? false})

                  (base-order {:id "mo-7" :order-id "MO-2026-0007"
                               :counterparty "Granite Machine Traders"
                               :end-user "Granite Machine Traders"
                               :sanctions-screened? false})

                  (base-order {:id "mo-8" :order-id "MO-2026-0008"
                               :item-description "5-axis simultaneous CNC machining center, sub-micron finishing"
                               :counterparty "Harbor Defense Fabrication Works"
                               :end-user "Harbor Defense Fabrication Works"
                               :simultaneous-axes 5 :positioning-accuracy-micrometers 3
                               :capability-classification-on-file? false
                               :military-end-use-flagged? true
                               :military-end-use-license-authorized? false})

                  (base-order {:id "mo-9" :order-id "MO-2026-0009"
                               :item-description "5-axis simultaneous CNC machining center, sub-micron finishing"
                               :counterparty "Indigo Allied Defense Logistics KK"
                               :end-user "Indigo Allied Defense Logistics KK"
                               :simultaneous-axes 5 :positioning-accuracy-micrometers 3
                               :capability-classification-on-file? true
                               :military-end-use-flagged? true
                               :military-end-use-license-authorized? true})]]
           [(:id o) o]))})

;; ----------------------------- shared commit logic -----------------------------

(defn- dispatch-order!
  "Backend-agnostic `:order/mark-dispatched` -- looks up the
  machine-order via the protocol and drafts the machine-dispatch
  record, and returns {:result .. :machine-order-patch ..} for the
  caller to persist."
  [s machine-order-id]
  (let [mo (machine-order s machine-order-id)
        seq-n (next-dispatch-sequence s (:jurisdiction mo))
        result (registry/register-dispatch-record machine-order-id (:jurisdiction mo) seq-n)]
    {:result result
     :machine-order-patch {:dispatched? true
                           :dispatch-number (get result "dispatch_number")}}))

(defn- invoice-order!
  "Backend-agnostic `:order/mark-invoiced` -- looks up the
  machine-order via the protocol and drafts the invoice record, and
  returns {:result .. :machine-order-patch ..} for the caller to
  persist."
  [s machine-order-id]
  (let [mo (machine-order s machine-order-id)
        seq-n (next-invoice-sequence s (:jurisdiction mo))
        result (registry/register-invoice-record machine-order-id (:jurisdiction mo) seq-n)]
    {:result result
     :machine-order-patch {:invoiced? true
                           :invoice-number (get result "invoice_number")}}))

;; ----------------------------- MemStore (default) -----------------------------

(defrecord MemStore [a]
  Store
  (machine-order [_ id] (get-in @a [:machine-orders id]))
  (all-machine-orders [_] (sort-by :id (vals (:machine-orders @a))))
  (assessment-of [_ machine-order-id] (get-in @a [:assessments machine-order-id]))
  (ledger [_] (:ledger @a))
  (dispatch-history [_] (:dispatches @a))
  (invoice-history [_] (:invoices @a))
  (next-dispatch-sequence [_ jurisdiction] (get-in @a [:dispatch-sequences jurisdiction] 0))
  (next-invoice-sequence [_ jurisdiction] (get-in @a [:invoice-sequences jurisdiction] 0))
  (machine-order-already-dispatched? [_ machine-order-id] (boolean (get-in @a [:machine-orders machine-order-id :dispatched?])))
  (machine-order-already-invoiced? [_ machine-order-id] (boolean (get-in @a [:machine-orders machine-order-id :invoiced?])))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :order/upsert
      (swap! a update-in [:machine-orders (:id value)] merge value)

      :contract-assessment/set
      (swap! a assoc-in [:assessments (first path)] payload)

      :order/mark-dispatched
      (let [machine-order-id (first path)
            {:keys [result machine-order-patch]} (dispatch-order! s machine-order-id)
            jurisdiction (:jurisdiction (machine-order s machine-order-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:dispatch-sequences jurisdiction] (fnil inc 0))
                       (update-in [:machine-orders machine-order-id] merge machine-order-patch)
                       (update :dispatches registry/append result))))
        result)

      :order/mark-invoiced
      (let [machine-order-id (first path)
            {:keys [result machine-order-patch]} (invoice-order! s machine-order-id)
            jurisdiction (:jurisdiction (machine-order s machine-order-id))]
        (swap! a (fn [state]
                   (-> state
                       (update-in [:invoice-sequences jurisdiction] (fnil inc 0))
                       (update-in [:machine-orders machine-order-id] merge machine-order-patch)
                       (update :invoices registry/append result))))
        result)
      nil)
    s)
  (append-ledger! [_ fact] (swap! a update :ledger conj fact) fact)
  (with-machine-orders [s machine-orders] (when (seq machine-orders) (swap! a assoc :machine-orders machine-orders)) s))

(defn seed-db
  "A MemStore seeded with the demo machine-order set. The deterministic default."
  []
  (->MemStore (atom (assoc (demo-data)
                           :assessments {}
                           :ledger [] :dispatch-sequences {} :dispatches []
                           :invoice-sequences {} :invoices []))))

;; ----------------------------- DatomicStore (langchain.db) -----------------------------

(def ^:private schema
  "DataScript/Datomic-style schema: only constraint attrs are declared.
  Map/compound values (assessment payloads, ledger facts, dispatch/
  invoice records) are stored as EDN strings so `langchain.db` doesn't
  expand them into sub-entities -- the same convention every sibling
  actor's store uses."
  {:machine-order/id                     {:db/unique :db.unique/identity}
   :assessment/machine-order-id          {:db/unique :db.unique/identity}
   :ledger/seq                           {:db/unique :db.unique/identity}
   :dispatch/seq                         {:db/unique :db.unique/identity}
   :invoice/seq                          {:db/unique :db.unique/identity}
   :dispatch-sequence/jurisdiction       {:db/unique :db.unique/identity}
   :invoice-sequence/jurisdiction        {:db/unique :db.unique/identity}})

(defn- enc [v] (pr-str v))
(defn- dec* [s] (when s (edn/read-string s)))

;; Every machine-order field is stored as its own Datomic attr so a
;; governor pull reads the exact ground truth (no blob decode). Boolean
;; fields are coerced on read so a missing attr reads back as false
;; (parity with MemStore). Keyword-valued fields round-trip via
;; `enc`/`dec*` (stored as an EDN string) so `:machine-category`/
;; `:status` survive the pull as keywords, not bare strings.
;; [field-key tx-attr kind] kind ∈ #{:plain :bool :kw}
(def ^:private machine-order-fields
  [[:id :machine-order/id :plain]
   [:order-id :machine-order/order-id :plain]
   [:item-description :machine-order/item-description :plain]
   [:machine-category :machine-order/machine-category :kw]
   [:simultaneous-axes :machine-order/simultaneous-axes :plain]
   [:positioning-accuracy-micrometers :machine-order/positioning-accuracy-micrometers :plain]
   [:capability-classification-on-file? :machine-order/capability-classification-on-file? :bool]
   [:destination-country :machine-order/destination-country :plain]
   [:end-user :machine-order/end-user :plain]
   [:counterparty :machine-order/counterparty :plain]
   [:military-end-use-flagged? :machine-order/military-end-use-flagged? :bool]
   [:military-end-use-license-authorized? :machine-order/military-end-use-license-authorized? :bool]
   [:price :machine-order/price :plain]
   [:contract-terms :machine-order/contract-terms :plain]
   [:credit-cleared? :machine-order/credit-cleared? :bool]
   [:sanctions-screened? :machine-order/sanctions-screened? :bool]
   [:dispatched? :machine-order/dispatched? :bool]
   [:invoiced? :machine-order/invoiced? :bool]
   [:jurisdiction :machine-order/jurisdiction :plain]
   [:status :machine-order/status :kw]
   [:dispatch-number :machine-order/dispatch-number :plain]
   [:invoice-number :machine-order/invoice-number :plain]])

(defn- machine-order->tx [mo]
  (reduce (fn [tx [k attr kind]]
            (let [v (get mo k)]
              (cond-> tx
                (some? v) (assoc attr (if (= kind :kw) (enc v) v)))))
          {:machine-order/id (:id mo)}
          machine-order-fields))

(def ^:private machine-order-pull (mapv second machine-order-fields))

(defn- pull->machine-order [m]
  (when (:machine-order/id m)
    (reduce (fn [mo [k attr kind]]
              (let [v (get m attr)]
                (cond
                  (= kind :bool)  (assoc mo k (boolean v))
                  (= kind :kw)    (cond-> mo (some? v) (assoc k (dec* v)))
                  (some? v)       (assoc mo k v)
                  :else           mo)))
            {:id (:machine-order/id m)}
            machine-order-fields)))

(defrecord DatomicStore [conn]
  Store
  (machine-order [_ id]
    (pull->machine-order (d/pull (d/db conn) machine-order-pull [:machine-order/id id])))
  (all-machine-orders [_]
    (->> (d/q '[:find [?id ...] :where [?e :machine-order/id ?id]] (d/db conn))
         (map #(pull->machine-order (d/pull (d/db conn) machine-order-pull [:machine-order/id %])))
         (sort-by :id)))
  (assessment-of [_ machine-order-id]
    (dec* (d/q '[:find ?p . :in $ ?moid
                :where [?a :assessment/machine-order-id ?moid] [?a :assessment/payload ?p]]
              (d/db conn) machine-order-id)))
  (ledger [_]
    (->> (d/q '[:find ?s ?f :where [?e :ledger/seq ?s] [?e :ledger/fact ?f]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (dispatch-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :dispatch/seq ?s] [?e :dispatch/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (invoice-history [_]
    (->> (d/q '[:find ?s ?r :where [?e :invoice/seq ?s] [?e :invoice/record ?r]] (d/db conn))
         (sort-by first)
         (mapv (comp dec* second))))
  (next-dispatch-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :dispatch-sequence/jurisdiction ?j] [?e :dispatch-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (next-invoice-sequence [_ jurisdiction]
    (or (d/q '[:find ?n . :in $ ?j
              :where [?e :invoice-sequence/jurisdiction ?j] [?e :invoice-sequence/next ?n]]
            (d/db conn) jurisdiction)
        0))
  (machine-order-already-dispatched? [s machine-order-id]
    (boolean (:dispatched? (machine-order s machine-order-id))))
  (machine-order-already-invoiced? [s machine-order-id]
    (boolean (:invoiced? (machine-order s machine-order-id))))
  (commit-record! [s {:keys [effect path value payload]}]
    (case effect
      :order/upsert
      (d/transact! conn [(machine-order->tx value)])

      :contract-assessment/set
      (d/transact! conn [{:assessment/machine-order-id (first path) :assessment/payload (enc payload)}])

      :order/mark-dispatched
      (let [machine-order-id (first path)
            {:keys [result machine-order-patch]} (dispatch-order! s machine-order-id)
            jurisdiction (:jurisdiction (machine-order s machine-order-id))
            next-n (inc (next-dispatch-sequence s jurisdiction))]
        (d/transact! conn
                     [(machine-order->tx (assoc machine-order-patch :id machine-order-id))
                      {:dispatch-sequence/jurisdiction jurisdiction :dispatch-sequence/next next-n}
                      {:dispatch/seq (count (dispatch-history s)) :dispatch/record (enc (get result "record"))}])
        result)

      :order/mark-invoiced
      (let [machine-order-id (first path)
            {:keys [result machine-order-patch]} (invoice-order! s machine-order-id)
            jurisdiction (:jurisdiction (machine-order s machine-order-id))
            next-n (inc (next-invoice-sequence s jurisdiction))]
        (d/transact! conn
                     [(machine-order->tx (assoc machine-order-patch :id machine-order-id))
                      {:invoice-sequence/jurisdiction jurisdiction :invoice-sequence/next next-n}
                      {:invoice/seq (count (invoice-history s)) :invoice/record (enc (get result "record"))}])
        result)
      nil)
    s)
  (append-ledger! [s fact]
    (d/transact! conn [{:ledger/seq (count (ledger s)) :ledger/fact (enc fact)}])
    fact)
  (with-machine-orders [s machine-orders]
    (when (seq machine-orders) (d/transact! conn (mapv machine-order->tx (vals machine-orders)))) s))

(defn datomic-store
  "A DatomicStore (langchain.db backend) seeded from `data`
  ({:machine-orders ..}); empty when omitted."
  ([] (datomic-store {}))
  ([{:keys [machine-orders]}]
   (let [s (->DatomicStore (d/create-conn schema))]
     (with-machine-orders s machine-orders))))

(defn datomic-seed-db
  "A DatomicStore seeded with the demo machine-order set -- the
  Datomic-backed analog of `seed-db`, used to prove protocol parity."
  []
  (datomic-store (demo-data)))
