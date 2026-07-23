(ns machtooltrade.facts-test
  (:require [clojure.test :refer [deftest is]]
            [machtooltrade.facts :as facts]))

(deftest usa-has-a-spec-basis
  (is (some? (facts/spec-basis "USA")))
  (is (string? (:provenance (facts/spec-basis "USA"))))
  (is (string? (:classification-list (facts/spec-basis "USA")))))

(deftest all-four-seeded-jurisdictions-have-required-evidence-and-a-classification-list
  ;; every seeded jurisdiction actually has a real required-evidence set
  ;; AND a real classification-list citation reported honestly here
  (doseq [iso3 ["USA" "JPN" "DEU" "GBR"]]
    (is (seq (facts/evidence-checklist iso3)) (str iso3 " required-evidence"))
    (is (string? (:classification-list (facts/spec-basis iso3))) (str iso3 " classification-list"))))

(deftest nld-has-a-spec-basis
  (is (some? (facts/spec-basis "NLD")))
  (is (string? (:provenance (facts/spec-basis "NLD"))))
  (is (string? (:classification-list (facts/spec-basis "NLD")))))

(deftest coverage-includes-nld-alongside-all-others
  (let [report (facts/coverage ["USA" "JPN" "DEU" "GBR" "ITA" "NLD"])]
    (is (= 6 (:covered report)))
    (is (= ["DEU" "GBR" "ITA" "JPN" "NLD" "USA"] (:covered-jurisdictions report)))))

(deftest che-has-a-spec-basis
  (is (some? (facts/spec-basis "CHE")))
  (is (string? (:provenance (facts/spec-basis "CHE"))))
  (is (string? (:classification-list (facts/spec-basis "CHE")))))

(deftest coverage-includes-che-alongside-all-others
  (let [report (facts/coverage ["USA" "JPN" "DEU" "GBR" "ITA" "NLD" "CHE"])]
    (is (= 7 (:covered report)))
    (is (= ["CHE" "DEU" "GBR" "ITA" "JPN" "NLD" "USA"] (:covered-jurisdictions report)))))

(deftest unknown-jurisdiction-has-no-fabricated-spec-basis
  (is (nil? (facts/spec-basis "ATL"))))

(deftest coverage-never-reports-a-missing-jurisdiction-as-covered
  (let [report (facts/coverage ["USA" "ATL" "GBR"])]
    (is (= 2 (:covered report)))
    (is (= ["ATL"] (:missing-jurisdictions report)))
    (is (= ["GBR" "USA"] (:covered-jurisdictions report)))))

(deftest required-evidence-satisfied-needs-every-item
  (let [all (facts/evidence-checklist "USA")]
    (is (facts/required-evidence-satisfied? "USA" all))
    (is (not (facts/required-evidence-satisfied? "USA" (rest all))))
    (is (not (facts/required-evidence-satisfied? "ATL" all)) "no spec-basis -> never satisfied")))

(deftest evidence-checklist-deliberately-excludes-capability-classification
  ;; unlike the general-trading sibling's own checklist, this vertical's
  ;; capability-threshold determination is its OWN dedicated, COMPUTED
  ;; governor check, not a checklist item -- see machtooltrade.governor
  ;; and machtooltrade.registry/capability-threshold-crossed?.
  (doseq [iso3 ["USA" "JPN" "DEU" "GBR"]]
    (is (not-any? #(re-find #"(?i)eccn|capability|2b001" %) (facts/evidence-checklist iso3))
        (str iso3 " checklist should not mention capability classification"))))

(deftest every-seeded-jurisdiction-requires-military-end-use-screening
  ;; the generic evidence checklist requires a military end-use/end-user
  ;; screening RECORD (was it screened at all?) -- distinct from the
  ;; dedicated military-end-use-unresolved HARD check (was a flag, if
  ;; any, actually resolved?), see machtooltrade.governor.
  (doseq [iso3 ["USA" "JPN" "DEU" "GBR"]]
    (is (some #(re-find #"(?i)military" %) (facts/evidence-checklist iso3))
        (str iso3 " checklist should require military end-use screening"))))
