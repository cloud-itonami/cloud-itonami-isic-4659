(ns machtooltrade.render-html
  "Build-time HTML renderer for `docs/samples/operator-console.html`.

  Closes flagship checklist item 2 for this repo: `docs/samples/
  operator-console.html` existed here as a COMMITTED ARTEFACT WITH NO
  GENERATOR, which is exactly the shape a hand-written page takes. This
  namespace replaces it with a page produced by driving the REAL actor
  stack -- `machtooltrade.operation` (langgraph StateGraph) ->
  `machtooltrade.governor` (Precision Machinery Export Governor) ->
  `machtooltrade.store` (MemStore SSoT) -- exactly the way this repo's
  own `machtooltrade.sim` demo driver does (`clojure -M:dev:run`, run
  and confirmed BEFORE this file was written: its `mo-1`..`mo-9` ids do
  match `machtooltrade.store/demo-data`, so the scenario below was
  safely adapted from it rather than authored from scratch).

  EVERY entity, id, number, hold reason and approver on the rendered
  page is read back out of this run's actual governor verdicts, run
  audit channels and committed store registers. Nothing on the page is
  hand-typed domain data. Where a fact is NOT obtainable from the store
  the page says so explicitly rather than inventing it -- see
  `approver-attribution` below, which is COMPUTED from the persisted
  registers at render time instead of being narrated, so that if the
  store is later fixed the page corrects itself.

  Determinism: no timestamps or environment data reach the page, the
  scenario is a fixed sequence against a fixed seed, and every table is
  emitted in a fixed order (`store/all-machine-orders` sorts by id; the
  ledger and the dispatch/invoice registers are append-only vectors).
  Two consecutive runs are byte-identical -- verify by rendering twice
  into a fresh temp directory and diffing.

  Usage: `clojure -M:dev:render-html [out-file]`
  (default `docs/samples/operator-console.html`)."
  (:require [clojure.string :as str]
            [jp-go-dds.skin]
            [langgraph.graph :as g]
            [machtooltrade.facts :as facts]
            [machtooltrade.governor :as governor]
            [machtooltrade.operation :as op]
            [machtooltrade.phase :as phase]
            [machtooltrade.registry :as registry]
            [machtooltrade.store :as store]))

;; ----------------------------- operators -----------------------------

(def ^:private compliance-officer
  "The phase-3 operator that drives the main scenario."
  {:actor-id "op-1" :actor-role :export-compliance-officer :phase 3})

(defn- staged-operator
  "The SAME human role at an EARLIER rollout phase, used to demonstrate
  that a rollout-phase gate hold is a different thing from a governor
  refusal (see `machtooltrade.phase/gate`)."
  [ph]
  {:actor-id "op-2" :actor-role :export-compliance-officer :phase ph})

;; ----------------------------- scenario driver -----------------------------
;;
;; Each step records what the REAL run produced, captured directly off
;; that run's own result map. Nothing is joined back together after the
;; fact -- in particular the approver is taken from the very run that
;; granted it, so there is no `[op subject]` join to get wrong (two
;; dispatch attempts against the same order really do share an
;; `[op subject]` key here: `mo-1` is dispatched once and then refused a
;; second time).

(def ^:private steps (atom []))

(defn- audit-of [r] (vec (get-in r [:state :audit])))

(defn- fact-of [r t] (first (filter #(= t (:t %)) (audit-of r))))

(defn- record-step!
  [{:keys [thread operator request note]} r]
  (let [audit (audit-of r)
        hold  (fact-of r :governor-hold)
        grant (fact-of r :approval-granted)
        req   (fact-of r :approval-requested)]
    (swap! steps conj
           {:seq          (count @steps)
            :thread       thread
            :actor        (:actor-id operator)
            :phase        (:phase operator)
            :op           (:op request)
            :subject      (:subject request)
            :note         note
            :disposition  (get-in r [:state :disposition])
            :verdict      (get-in r [:state :verdict])
            :record       (get-in r [:state :record])
            :violations   (vec (:violations hold))
            :phase-reason (or (:phase-reason hold) (when req (:reason req)))
            :approved-by  (:by grant)
            :audit        audit})
    r))

(defn- exec!
  ([actor ctx tid request] (exec! actor ctx tid request nil))
  ([actor operator tid request note]
   (record-step! {:thread tid :operator operator :request request :note note}
                 (g/run* actor {:request request :context operator}
                         {:thread-id tid}))))

(defn- approve!
  "Resume a paused run as a human approver. The resumed run is folded
  into the step the interrupt created, so the step carries both the
  escalation and its outcome."
  [actor tid by]
  (let [r (g/run* actor {:approval {:status :approved :by by}}
                  {:thread-id tid :resume? true})
        grant (fact-of r :approval-granted)]
    (swap! steps
           (fn [ss]
             (mapv (fn [s]
                     (if (= tid (:thread s))
                       (assoc s
                              :disposition (get-in r [:state :disposition])
                              :record      (get-in r [:state :record])
                              :approved-by (:by grant)
                              :audit       (into (:audit s) (audit-of r)))
                       s))
                   ss)))
    r))

(defn run-demo!
  "Drives a freshly seeded store through a scenario that reaches EVERY
  disposition this actor can produce, and returns `{:db .. :steps ..}`.

  Approved paths: `mo-1` (an ordinary 3-axis machining center, below
  the ECCN-2B001-style capability threshold) and `mo-9` (a controlled
  AND military-flagged order whose BOTH axes are properly resolved)
  each clear a full intake/verify/dispatch/settle lifecycle, every
  actuation escalating to a real human approval first.

  HARD governor refusals -- none of which ever reach a human -- cover
  all seven numbered checks plus both double-actuation guards. The
  load-bearing pair for this build's TWO-AXIS claim is `mo-5` (an
  ABOVE-threshold machine sold to an ORDINARY end-user, held by the
  capability axis ALONE) against `mo-6` (a BELOW-threshold machine with
  the SAME ordinary specification as the cleanly-dispatched `mo-1`,
  sold to a FLAGGED military end-user, held by the end-use axis ALONE);
  `mo-8` then fires BOTH axes on one order.

  Finally the same human role is replayed at earlier rollout phases so
  the page can show that a phase gate holding a governor-CLEAN proposal
  is a categorically different event from a governor refusal."
  []
  (reset! steps [])
  (let [db (store/seed-db)
        actor (op/build db)
        oc compliance-officer]

    ;; ---- mo-1: full clean lifecycle -------------------------------
    (exec! actor oc "mo1-intake"
           {:op :order/intake :subject "mo-1"
            :patch {:id "mo-1" :counterparty "Ridgeline Precision Manufacturing Inc"}}
           "phase-3 auto-commit: intake carries no capital or export-control risk")

    (exec! actor oc "mo1-verify" {:op :contract/verify :subject "mo-1"}
           "governor clean; phase 3 does not auto-commit a verification")
    (approve! actor "mo1-verify" "op-1")

    (exec! actor oc "mo1-dispatch" {:op :delivery/dispatch :subject "mo-1"}
           "actuation: never auto-commits at any phase")
    (approve! actor "mo1-dispatch" "op-1")

    (exec! actor oc "mo1-settle" {:op :invoice/settle :subject "mo-1"}
           "actuation: never auto-commits at any phase")
    (approve! actor "mo1-settle" "op-1")

    ;; ---- mo-2: no official spec-basis for its jurisdiction --------
    (exec! actor oc "mo2-verify" {:op :contract/verify :subject "mo-2"}
           "jurisdiction ATL is not in machtooltrade.facts -- requirements must never be invented")

    ;; ---- mo-3: counterparty credit not cleared --------------------
    (exec! actor oc "mo3-verify" {:op :contract/verify :subject "mo-3"} nil)
    (approve! actor "mo3-verify" "op-1")
    (exec! actor oc "mo3-dispatch" {:op :delivery/dispatch :subject "mo-3"}
           "credit-clearance is re-read from the order, not from the proposal")

    ;; ---- mo-4: no contract terms on file --------------------------
    (exec! actor oc "mo4-verify" {:op :contract/verify :subject "mo-4"} nil)
    (approve! actor "mo4-verify" "op-1")
    (exec! actor oc "mo4-dispatch" {:op :delivery/dispatch :subject "mo-4"}
           "no contract/PO recorded against the order")

    ;; ---- mo-5: AXIS 1 alone ---------------------------------------
    (exec! actor oc "mo5-verify" {:op :contract/verify :subject "mo-5"} nil)
    (approve! actor "mo5-verify" "op-1")
    (exec! actor oc "mo5-dispatch" {:op :delivery/dispatch :subject "mo-5"}
           "AXIS 1 alone: specs recomputed over the threshold, no classification on file, ordinary end-user")

    ;; ---- mo-6: AXIS 2 alone ---------------------------------------
    (exec! actor oc "mo6-verify" {:op :contract/verify :subject "mo-6"} nil)
    (approve! actor "mo6-verify" "op-1")
    (exec! actor oc "mo6-dispatch" {:op :delivery/dispatch :subject "mo-6"}
           "AXIS 2 alone: same ordinary specs as the cleanly-dispatched mo-1, but a flagged military end-user")

    ;; ---- mo-7: sanctions screening not passed ---------------------
    (exec! actor oc "mo7-verify" {:op :contract/verify :subject "mo-7"} nil)
    (approve! actor "mo7-verify" "op-1")
    (exec! actor oc "mo7-dispatch" {:op :delivery/dispatch :subject "mo-7"}
           "OFAC-equivalent screening unresolved -- checked at BOTH actuation ops")

    ;; ---- mo-8: both axes fire on one order ------------------------
    (exec! actor oc "mo8-verify" {:op :contract/verify :subject "mo-8"} nil)
    (approve! actor "mo8-verify" "op-1")
    (exec! actor oc "mo8-dispatch" {:op :delivery/dispatch :subject "mo-8"}
           "the two axes are not mutually exclusive -- both fire on the same order")

    ;; ---- mo-9: controlled AND flagged, both resolved -> clears ----
    (exec! actor oc "mo9-verify" {:op :contract/verify :subject "mo-9"} nil)
    (approve! actor "mo9-verify" "op-1")
    (exec! actor oc "mo9-dispatch" {:op :delivery/dispatch :subject "mo-9"}
           "above threshold AND flagged, but classified AND licensed -- clears both axes")
    (approve! actor "mo9-dispatch" "op-1")
    (exec! actor oc "mo9-settle" {:op :invoice/settle :subject "mo-9"} nil)
    (approve! actor "mo9-settle" "op-1")

    ;; ---- double-actuation guards ----------------------------------
    (exec! actor oc "mo1-dispatch-again" {:op :delivery/dispatch :subject "mo-1"}
           "double-dispatch guard, off a dedicated :dispatched? fact (never a :status value)")
    (exec! actor oc "mo1-settle-again" {:op :invoice/settle :subject "mo-1"}
           "double-invoice guard, off a dedicated :invoiced? fact")

    ;; ---- rollout-phase gate, NOT a governor refusal ---------------
    (exec! actor (staged-operator 0) "phase0-intake" {:op :order/intake :subject "mo-1"
                                                      :patch {:id "mo-1"}}
           "phase 0 is read-only: the governor never objected")
    (exec! actor (staged-operator 1) "phase1-verify" {:op :contract/verify :subject "mo-1"}
           "phase 1 enables intake only: the governor never objected")
    (exec! actor (staged-operator 2) "phase2-verify" {:op :contract/verify :subject "mo-1"}
           "phase 2 enables verification but never auto-commits it -- left pending, not approved")

    {:db db :steps @steps}))

;; ----------------------------- derived analysis -----------------------------

(defn- hard-hold?
  "A step the GOVERNOR refused. Two-stage on purpose: a rollout-phase
  gate also produces `:disposition :hold`, and its hold fact carries an
  EMPTY `:violations` vector, so counting holds alone would silently
  accept a page with no real refusal on it."
  [s]
  (and (= :hold (:disposition s))
       (true? (:hard? (:verdict s)))
       (seq (:violations s))
       (some #(seq (str (:rule %))) (:violations s))))

(defn- phase-gate-hold?
  "A step held by the ROLLOUT GATE while the governor was clean."
  [s]
  (and (= :hold (:disposition s))
       (not (true? (:hard? (:verdict s))))
       (empty? (:violations s))))

(defn- approved-step? [s] (some? (:approved-by s)))

(defn- hard-hold-rules [ss]
  (into (sorted-set) (mapcat #(map (comp name :rule) (:violations %)) ss)))

;; --- approver attribution: MEASURED from the registers, not narrated ---

(defn- approver-keys
  "Keys of `m` whose name mentions an approver, whatever the key type
  (the dispatch/invoice registers use string keys, the order and
  assessment maps use keywords)."
  [m]
  (when (map? m)
    (->> (keys m)
         (filter #(str/includes? (str/lower-case (if (keyword? %) (name %) (str %)))
                                 "approv"))
         (into (sorted-set-by (fn [a b] (compare (str a) (str b))))))))

(defn- register-for
  "The register a committed effect actually landed in, and the value
  persisted there -- read back out of the store AFTER the run."
  [db {:keys [effect subject]}]
  (case effect
    :contract-assessment/set
    {:register "assessments" :value (store/assessment-of db subject)}

    :order/upsert
    {:register "machine-orders" :value (store/machine-order db subject)}

    :order/mark-dispatched
    {:register "dispatches"
     :value (first (filter #(= subject (get % "machine_order_id"))
                           (store/dispatch-history db)))}

    :order/mark-invoiced
    {:register "invoices"
     :value (first (filter #(= subject (get % "machine_order_id"))
                           (store/invoice-history db)))}

    {:register "(none)" :value nil}))

(defn approver-attribution
  "For every step a human actually approved, COMPUTE whether that
  approver survived into the register the commit wrote. Deriving this
  rather than describing it means the page self-corrects if the store
  is later changed: a hard-coded 'this store loses approvers' note
  would become a lie the day someone fixes it.

  Silently omitting an approver would be dishonest in the other
  direction -- a reader could not tell 'nobody approved this' from 'the
  store did not keep who did'. So both the audit-trail approver and the
  register's own approver keys are shown side by side."
  [db steps]
  (for [s (filter approved-step? steps)
        :let [effect (get-in s [:record :effect])
              {:keys [register value]} (register-for db (assoc s :effect effect))
              ks (approver-keys value)]]
    {:op          (:op s)
     :subject     (:subject s)
     :effect      effect
     :audit-approver (:approved-by s)
     :register    register
     :persisted?  (boolean (seq ks))
     :keys-found  ks}))

(defn- ledger-retains-approver?
  "Does the append-only audit ledger itself carry the approver? Also
  measured, for the same reason."
  [db]
  (boolean (some #(seq (approver-keys %)) (store/ledger db))))

;; --- the two-axis independence claim, recomputed at render time ---

(defn axis-matrix
  "Re-runs the governor's OWN axis predicates over every seeded order,
  so the page's independence claim is computed here rather than
  asserted. Axis 1 calls the very function the governor calls
  (`registry/capability-threshold-crossed?`); axis 2 reads the same
  two end-use facts `military-end-use-unresolved-violations` reads."
  [db]
  (for [{:keys [id simultaneous-axes positioning-accuracy-micrometers
                capability-classification-on-file?
                military-end-use-flagged? military-end-use-license-authorized?]
         :as mo} (store/all-machine-orders db)
        :let [crossed? (registry/capability-threshold-crossed?
                        simultaneous-axes positioning-accuracy-micrometers)
              axis1 (and crossed? (not (true? capability-classification-on-file?)))
              axis2 (and (true? military-end-use-flagged?)
                         (not (true? military-end-use-license-authorized?)))]]
    {:id id
     :order-id (:order-id mo)
     :axes simultaneous-axes
     :accuracy positioning-accuracy-micrometers
     :crossed? crossed?
     :classified? (true? capability-classification-on-file?)
     :flagged? (true? military-end-use-flagged?)
     :licensed? (true? military-end-use-license-authorized?)
     :axis1-holds? axis1
     :axis2-holds? axis2}))

;; ----------------------------- html helpers -----------------------------

(defn- esc [x]
  (-> (str x)
      (str/replace "&" "&amp;")
      (str/replace "<" "&lt;")
      (str/replace ">" "&gt;")
      (str/replace "\"" "&quot;")))

(defn- kw [x] (if (keyword? x) (name x) (str x)))

(defn- code [x] (str "<code>" (esc (kw x)) "</code>"))

(defn- yes-no [b yes-cls no-cls yes-txt no-txt]
  (if b
    (str "<span class=\"" yes-cls "\">" yes-txt "</span>")
    (str "<span class=\"" no-cls "\">" no-txt "</span>")))

(defn- row [& cells]
  (str "        <tr>" (str/join (map #(str "<td>" % "</td>") cells)) "</tr>"))

(defn- table [headers rows]
  (str "    <table>\n"
       "      <thead><tr>"
       (str/join (map #(str "<th>" (esc %) "</th>") headers))
       "</tr></thead>\n"
       "      <tbody>\n"
       (if (seq rows) (str (str/join "\n" rows) "\n") "")
       "      </tbody>\n"
       "    </table>\n"))

(defn- section [title lede body]
  (str "  <section class=\"card\">\n"
       "    <h2>" (esc title) "</h2>\n"
       (if lede (str "    <p class=\"muted\">" lede "</p>\n") "")
       body
       "  </section>\n"))

;; ----------------------------- render -----------------------------

(defn- order-row [{:keys [id order-id item-description counterparty jurisdiction
                          simultaneous-axes positioning-accuracy-micrometers
                          price dispatched? invoiced? dispatch-number invoice-number]}]
  (row (code id) (esc order-id) (esc item-description) (esc counterparty)
       (esc jurisdiction)
       (str "<span class=\"num\">" (esc simultaneous-axes) "</span>")
       (str "<span class=\"num\">" (esc positioning-accuracy-micrometers) "</span>")
       (str "<span class=\"num\">" (esc price) "</span>")
       (if dispatched?
         (str "<span class=\"ok\">" (esc dispatch-number) "</span>")
         "<span class=\"muted\">not dispatched</span>")
       (if invoiced?
         (str "<span class=\"ok\">" (esc invoice-number) "</span>")
         "<span class=\"muted\">not settled</span>")))

(defn- disposition-cell [s]
  (cond
    (hard-hold? s)
    (str "<span class=\"critical\">HARD hold</span>")

    (phase-gate-hold? s)
    (str "<span class=\"warn\">phase gate</span>")

    (= :hold (:disposition s)) "<span class=\"critical\">hold</span>"
    (approved-step? s) "<span class=\"ok\">approved &amp; committed</span>"
    (= :commit (:disposition s)) "<span class=\"ok\">auto-committed</span>"
    (= :escalate (:disposition s)) "<span class=\"warn\">awaiting approval</span>"
    :else "<span class=\"muted\">-</span>"))

(defn- step-row [s]
  (row (str "<span class=\"num\">" (:seq s) "</span>")
       (str "<span class=\"num\">" (:phase s) "</span>")
       (esc (:actor s))
       (code (:op s))
       (code (:subject s))
       (disposition-cell s)
       (if-let [r (:phase-reason s)] (code r) "<span class=\"muted\">-</span>")
       (if (seq (:violations s))
         (str/join ", " (map #(str "<code>" (esc (kw (:rule %))) "</code>") (:violations s)))
         "<span class=\"muted\">none</span>")
       (if-let [by (:approved-by s)] (esc by) "<span class=\"muted\">-</span>")))

(defn- refusal-rows [steps]
  (for [s (filter hard-hold? steps)
        v (:violations s)]
    (row (code (:subject s)) (code (:op s))
         (str "<code>" (esc (kw (:rule v))) "</code>")
         (esc (:detail v))
         (str "<span class=\"num\">" (esc (:confidence (:verdict s))) "</span>"))))

(defn- phase-gate-rows [steps]
  (for [s (filter #(or (phase-gate-hold? %)
                       (and (= :escalate (:disposition %))
                            (= :phase-approval (:phase-reason %))
                            (not (approved-step? %))))
                  steps)]
    (row (str "<span class=\"num\">" (:phase s) "</span>")
         (esc (get-in phase/phases [(:phase s) :label]))
         (code (:op s)) (code (:subject s))
         (if (= :hold (:disposition s))
           "<span class=\"warn\">held by rollout gate</span>"
           "<span class=\"warn\">escalated by rollout gate</span>")
         (code (:phase-reason s))
         (yes-no (true? (:hard? (:verdict s))) "critical" "ok"
                 "governor also objected" "governor was clean"))))

(defn- approval-rows [steps]
  (for [s (filter approved-step? steps)]
    (row (code (:op s)) (code (:subject s)) (esc (:approved-by s))
         (code (get-in s [:record :effect]))
         (str "<span class=\"num\">" (esc (:confidence (:verdict s))) "</span>")
         (yes-no (true? (:high-stakes? (:verdict s))) "warn" "muted"
                 "high-stakes actuation" "not high-stakes"))))

(defn- attribution-rows [rows]
  (for [{:keys [op subject effect audit-approver register persisted? keys-found]} rows]
    (row (code op) (code subject) (code effect) (code register)
         (esc audit-approver)
         (if persisted?
           (str "<span class=\"ok\">retained</span> "
                (str/join " " (map #(str "<code>" (esc (if (keyword? %) (name %) (str %))) "</code>")
                                   keys-found)))
           "<span class=\"critical\">dropped by the store</span>"))))

(defn- axis-rows [rows]
  (for [{:keys [id order-id axes accuracy crossed? classified? flagged? licensed?
                axis1-holds? axis2-holds?]} rows]
    (row (code id) (esc order-id)
         (str "<span class=\"num\">" (esc axes) "</span>")
         (str "<span class=\"num\">" (esc accuracy) "</span>")
         (yes-no crossed? "warn" "muted" "over threshold" "below threshold")
         (yes-no classified? "ok" "muted" "classified" "no record")
         (yes-no flagged? "warn" "muted" "flagged" "not flagged")
         (yes-no licensed? "ok" "muted" "licensed" "no licence")
         (yes-no axis1-holds? "critical" "ok" "HOLDS" "clears")
         (yes-no axis2-holds? "critical" "ok" "HOLDS" "clears"))))

(defn- register-rows [records]
  (for [r records]
    (row (str "<code>" (esc (get r "record_id")) "</code>")
         (esc (get r "kind"))
         (str "<code>" (esc (get r "machine_order_id")) "</code>")
         (esc (get r "jurisdiction"))
         (yes-no (true? (get r "immutable")) "ok" "muted" "immutable" "mutable"))))

(defn- assessment-rows [db]
  (for [{:keys [id]} (store/all-machine-orders db)
        :let [a (store/assessment-of db id)]
        :when a]
    (row (code id) (esc (:jurisdiction a))
         (str "<span class=\"num\">" (count (:checklist a)) "</span>")
         (esc (:spec-basis a))
         (esc (:legal-basis a)))))

(defn- ledger-rows [db]
  (for [f (store/ledger db)]
    (row (code (:t f)) (code (:op f)) (code (:subject f)) (esc (:actor f))
         (code (:disposition f))
         (if (seq (:basis f))
           (esc (str/join "; " (map kw (:basis f))))
           "<span class=\"muted\">-</span>"))))

(defn- jurisdiction-rows []
  (for [[iso3 sb] (sort-by key facts/catalog)]
    (row (str "<code>" (esc iso3) "</code>")
         (esc (:owner-authority sb))
         (esc (:legal-basis sb))
         (esc (:classification-list sb))
         (str "<span class=\"num\">" (count (:required-evidence sb)) "</span>"))))

(defn render
  "Renders the whole console from a completed `run-demo!` result. Every
  cell traces to `db` (the committed SSoT) or to `steps` (the actual run
  results); nothing is hand-typed domain data."
  [{:keys [db steps]}]
  (let [orders    (store/all-machine-orders db)
        refusals  (filter hard-hold? steps)
        gated     (filter phase-gate-hold? steps)
        approvals (filter approved-step? steps)
        rules     (hard-hold-rules refusals)
        attrib    (approver-attribution db steps)
        attrib-ok (count (filter :persisted? attrib))
        axes      (axis-matrix db)]
    (str
     "<!DOCTYPE html>\n<html lang=\"en\"><head><meta charset=\"utf-8\">"
     "<meta name=\"viewport\" content=\"width=device-width, initial-scale=1, viewport-fit=cover\">"
     "<meta name=\"color-scheme\" content=\"light\"><meta name=\"theme-color\" content=\"#ffffff\">"
     "<title>cloud-itonami-isic-4659 &middot; machtooltrade &mdash; Operator Console</title>"
     "<style>" (jp-go-dds.skin/dds+skin) "</style></head><body>\n"

     "<header class=\"bar\">\n"
     "  <h1>Wholesale of other machinery and equipment (ISIC 4659) &mdash; Operator Console</h1>\n"
     "  <span class=\"badge\">read-only sample &middot; governor-gated &middot; dispatch and invoice settlement are always human-approved</span>\n"
     "</header>\n"
     "<main>\n"

     (section
      "This run"
      (str "Build-time output of <code>machtooltrade.render-html</code> "
           "(<code>clojure -M:dev:render-html</code>), which drives the real "
           "<code>machtooltrade.operation</code> StateGraph through "
           "<code>machtooltrade.governor</code> into <code>machtooltrade.store</code>. "
           "Every id, number, hold reason and approver below is read back out of that run. "
           "No timestamps are emitted, so the page is byte-identical across reruns from the same seed.")
      (table ["Measure" "Value"]
             [(row "Seeded machine orders" (str "<span class=\"num\">" (count orders) "</span>"))
              (row "Governed operations run" (str "<span class=\"num\">" (count steps) "</span>"))
              (row "HARD governor refusals (never reach a human)"
                   (str "<span class=\"num\">" (count refusals) "</span>"))
              (row "Distinct HARD refusal reasons"
                   (str "<span class=\"num\">" (count rules) "</span> &middot; "
                        (str/join " " (map #(str "<code>" (esc %) "</code>") rules))))
              (row "Rollout-gate holds (governor clean)"
                   (str "<span class=\"num\">" (count gated) "</span>"))
              (row "Human approvals granted"
                   (str "<span class=\"num\">" (count approvals) "</span>"))
              (row "Committed dispatch records"
                   (str "<span class=\"num\">" (count (store/dispatch-history db)) "</span>"))
              (row "Committed invoice records"
                   (str "<span class=\"num\">" (count (store/invoice-history db)) "</span>"))
              (row "Audit-ledger facts"
                   (str "<span class=\"num\">" (count (store/ledger db)) "</span>"))]))

     (section
      "Machine-order directory"
      (str "The committed SSoT after the run &mdash; <code>machtooltrade.store/all-machine-orders</code>. "
           "Dispatch and invoice numbers are jurisdiction-scoped sequences minted by "
           "<code>machtooltrade.registry</code> at commit time; an order with no number was never actuated.")
      (table ["Id" "Order" "Item" "Counterparty" "Juris." "Axes" "Accuracy (&micro;m)"
              "Price" "Dispatch" "Invoice"]
             (map order-row orders)))

     (section
      "Governed operation log"
      (str "One row per graph run. <code>machtooltrade.phase/gate</code> can only add caution to a "
           "governor verdict, never remove it, so a HARD refusal is terminal at every phase.")
      (table ["#" "Phase" "Actor" "Op" "Subject" "Disposition" "Gate reason" "Violations" "Approved by"]
             (map step-row steps)))

     (section
      "HARD governor refusals"
      (str "Refusals by the Precision Machinery Export Governor. These are <strong>not</strong> "
           "overridable and never reach a human approver &mdash; the graph routes straight from "
           "<code>:decide</code> to <code>:hold</code>. Each detail string is the governor's own "
           "output, not a restatement.")
      (table ["Subject" "Op" "Rule" "Governor detail" "Advisor confidence"]
             (refusal-rows steps)))

     (section
      "Rollout-phase gate (a different thing from a refusal)"
      (str "These proposals were <em>governor-clean</em>. They were held or escalated purely because "
           "the actor is running at an earlier rollout phase &mdash; the hold fact carries an "
           "<strong>empty</strong> violation list. Conflating this with a compliance refusal would "
           "overstate what the governor actually objected to, so it is tabulated separately.")
      (table ["Phase" "Label" "Op" "Subject" "Outcome" "Reason" "Governor's own verdict"]
             (phase-gate-rows steps)))

     (section
      "Two-axis export-control independence (recomputed here)"
      (str "Axis 1 is recomputed by calling the same "
           "<code>machtooltrade.registry/capability-threshold-crossed?</code> the governor calls; "
           "axis 2 reads the same two end-use facts the governor reads. The pair that carries the "
           "claim is <code>mo-5</code> (over threshold, ordinary end-user &mdash; axis 1 holds alone) "
           "against <code>mo-6</code> (the same ordinary specification as the cleanly dispatched "
           "<code>mo-1</code>, but a flagged military end-user &mdash; axis 2 holds alone). "
           "<code>mo-8</code> shows both firing together and <code>mo-9</code> shows both cleared.")
      (table ["Id" "Order" "Axes" "Accuracy (&micro;m)" "Capability" "Classification"
              "End-use" "Licence" "Axis 1" "Axis 2"]
             (axis-rows axes)))

     (section
      "Human approvals"
      (str "Every actuation escalated to a real human before committing. "
           "<code>:delivery/dispatch</code> and <code>:invoice/settle</code> are absent from every "
           "phase's auto set including phase 3, and the governor independently marks them "
           "high-stakes &mdash; two layers agree that actuation is always a human call.")
      (table ["Op" "Subject" "Approver" "Effect" "Advisor confidence" "Stakes"]
             (approval-rows steps)))

     (section
      "Approver attribution (measured, not asserted)"
      (str "For each approved commit, this table reads the register the commit actually wrote and "
           "reports whether an approver key survived there. It is computed at render time by walking "
           "the persisted values, so if the store is changed the page corrects itself. "
           "<strong>Measured on this run: " attrib-ok " of " (count attrib)
           " approved commits retain their approver.</strong> "
           (if (= attrib-ok (count attrib))
             "Every register kept it."
             (str "The gap is real and is shown rather than hidden: "
                  "<code>machtooltrade.store/commit-record!</code> reads <code>:payload</code> for "
                  "<code>:contract-assessment/set</code> (so the approver survives) but ignores it for "
                  "<code>:order/mark-dispatched</code> and <code>:order/mark-invoiced</code>, which "
                  "rebuild their record from <code>machtooltrade.registry</code> alone. The approver is "
                  "not lost to the system &mdash; it is present in the run's "
                  "<code>:approval-granted</code> audit fact, shown in the Approver column &mdash; "
                  "but it is not recoverable from the committed register. "
                  "The append-only audit ledger "
                  (if (ledger-retains-approver? db)
                    "does carry it."
                    "does <strong>not</strong> carry it either.")))
           " A reader can therefore tell &quot;nobody approved this&quot; apart from &quot;the store did "
           "not keep who did&quot;.")
      (table ["Op" "Subject" "Effect" "Register written" "Approver (audit trail)" "Approver in register"]
             (attribution-rows attrib)))

     (section
      "Machine-dispatch register"
      (str "Append-only dispatch drafts from <code>machtooltrade.store/dispatch-history</code>. "
           "Every certificate this actor produces is unsigned &mdash; signature is the operator's act, "
           "not the actor's.")
      (table ["Record id" "Kind" "Machine order" "Jurisdiction" "Immutability"]
             (register-rows (store/dispatch-history db))))

     (section
      "Invoice register"
      "Append-only invoice drafts from <code>machtooltrade.store/invoice-history</code> &mdash; the money side of the trade."
      (table ["Record id" "Kind" "Machine order" "Jurisdiction" "Immutability"]
             (register-rows (store/invoice-history db))))

     (section
      "Committed contract assessments"
      (str "Per-jurisdiction counterparty-diligence checklists committed by "
           "<code>:contract/verify</code>. The governor requires a satisfied checklist before either "
           "actuation op &mdash; it deliberately does <em>not</em> include the capability-threshold "
           "determination, which is recomputed separately.")
      (table ["Subject" "Jurisdiction" "Checklist items" "Spec basis" "Legal basis"]
             (assessment-rows db)))

     (section
      "Jurisdiction spec-basis catalogue"
      (str "<code>machtooltrade.facts/catalog</code> &mdash; the only jurisdictions this actor will "
           "accept a verification for. A proposal citing anything else is refused outright "
           "(<code>mo-2</code> above, jurisdiction <code>ATL</code>): requirements are never invented.")
      (table ["ISO3" "Owner authority" "Legal basis" "Classification list" "Required evidence"]
             (jurisdiction-rows)))

     (section
      "Append-only audit ledger"
      "Every decision fact this run wrote to the SSoT, in commit order."
      (table ["Fact" "Op" "Subject" "Actor" "Disposition" "Basis"]
             (ledger-rows db)))

     "</main>\n"
     "<footer class=\"footer\">\n"
     "  <p>Generated by <code>machtooltrade.render-html</code> from a live "
     "<code>machtooltrade.operation</code> run against <code>machtooltrade.store/seed-db</code>. "
     "Confidence note: the numeric capability thresholds in "
     "<code>machtooltrade.registry/capability-threshold-crossed?</code> are an illustrative composite "
     "standing in for ECCN 2B001's real, machine-type-specific structure &mdash; see "
     "<code>docs/business-model.md</code>. A real deployment must replace them with a verified "
     "reproduction of the current control-list text.</p>\n"
     "</footer>\n"
     "</body></html>\n")))

;; ----------------------------- entry point -----------------------------

(defn -main
  "Renders `docs/samples/operator-console.html` (or `args[0]`).

  Build-time invariant, enforced here rather than left to convention:
  the scenario MUST produce at least one HARD governor refusal, and at
  least one of those refusals MUST carry a non-empty violation. The
  second stage is not redundant -- `machtooltrade.phase/gate` also
  yields `:disposition :hold`, with an EMPTY `:violations` vector, so a
  naive hold count would be satisfied by a page whose governor never
  actually refused anything. It must also produce at least one approved
  commit, so the page shows both outcomes. Nothing is written when the
  invariant fails."
  [& args]
  (let [out (or (first args) "docs/samples/operator-console.html")
        {:keys [db steps] :as result} (run-demo!)
        refusals (filter hard-hold? steps)
        with-reason (filter #(some (fn [v] (seq (str (:rule v)))) (:violations %)) refusals)
        approvals (filter approved-step? steps)]
    (when (empty? refusals)
      (throw (ex-info "render-html: scenario produced no HARD governor hold -- refusing to write a console that cannot show a refusal"
                      {:steps (count steps)
                       :holds (count (filter #(= :hold (:disposition %)) steps))})))
    (when (empty? with-reason)
      (throw (ex-info "render-html: every HARD hold carried an empty violation list -- a phase gate, not a governor refusal"
                      {:hard-holds (count refusals)})))
    (when (empty? approvals)
      (throw (ex-info "render-html: scenario produced no approved commit -- the console must show both paths"
                      {:steps (count steps)})))
    (spit out (render result))
    (println "wrote" out
             (str "(" (count steps) " ops, "
                  (count refusals) " HARD holds over "
                  (count (hard-hold-rules refusals)) " distinct rules, "
                  (count approvals) " human approvals, "
                  (count (store/ledger db)) " ledger facts)"))))
