(ns machtooltrade.facts
  "Per-jurisdiction precision-machine-tool dual-use export-control
  regulatory catalog -- the G2-style spec-basis table the Precision
  Machinery Export Governor checks every `:contract/verify` proposal
  against ('did the advisor cite an OFFICIAL public source for THIS
  jurisdiction's machine-tool export-control regime, or did it invent
  one?').

  UNLIKE the computer-and-software-wholesale sibling's own
  `techtrade.facts` (whose defining concern is information/data-security
  technology -- can this device encrypt or process controlled
  information), THIS catalog's defining regulatory content is a
  PHYSICAL-CAPABILITY concern: certain multi-axis-simultaneous-
  contouring CNC machine tools can themselves manufacture precision
  weapons components (e.g. centrifuge rotors/bellows for uranium
  enrichment, missile-guidance and airframe components, submarine-hull
  frames) to tolerances that ordinary machine tools cannot hold. This
  is why machine tools are a HEADLINE, founding category of the
  multilateral Wassenaar Arrangement (1996, the successor to COCOM,
  ~42 participating states) Dual-Use List -- and remain one of the
  oldest, most stable dual-use control categories in the entire
  Wassenaar/CCL/Annex-I lineage, predating the information-security
  category by decades. [CONFIDENCE: high that this category and its
  multilateral lineage are real; see `docs/business-model.md`
  'Jurisdiction coverage (honest)' for the confidence gradient on the
  precise numeric axis-count/accuracy thresholds cited below.]

  Each entry below is a REAL jurisdiction with a REAL machine-tool
  export-control-classification regime:

  - USA (the PRIMARY regime for this vertical): the Bureau of Industry
    and Security (BIS)'s Export Administration Regulations (EAR, 15
    C.F.R. Parts 730-774). Numerically controlled machine tools and
    specially designed components/accessories classify under Commerce
    Control List (CCL) Category 2 (Materials Processing), headlined by
    ECCN 2B001 -- multi-axis-simultaneous-contouring machine tools
    (grinding/turning/milling machining centers) whose positioning
    accuracy (per ISO 230-2, the international machine-tool
    positioning-accuracy test standard) is finer than the ECCN's own
    threshold are CONTROLLED; an ordinary general-purpose machine tool
    below that threshold is EAR99 (reviewed, not on the control list --
    a REAL, valid outcome, distinct from 'never classified at all').
    INDEPENDENTLY of the item's own classification, the EAR's Military
    End User (MEU) Rule (15 C.F.R. §744.21, substantially expanded in
    the 2020s) is a CATCH-ALL: it can require a license for essentially
    ANY item subject to the EAR -- including an otherwise-EAR99,
    below-threshold general-purpose machine tool -- when the exporter
    knows (or has reason to know) the item is destined for a 'military
    end use' or a 'military end user' in a covered destination. This is
    a GENUINELY DIFFERENT regulatory mechanism from ECCN classification
    (see `machtooltrade.governor` namespace docstring for why this
    build carries TWO independent HARD checks, not one).
  - JPN: 経済産業省 (METI) 貿易経済協力局 安全保障貿易管理課 administers
    輸出貿易管理令 (Export Trade Control Order) 別表第一 (Appended Table
    1) -- Japan's OWN dual-use control list, Wassenaar-derived like the
    EAR's CCL. The classification act is 該非判定 (gaihi-hantei,
    'applicability determination'): does this specific machine tool
    fall within an Appended Table 1 item number covering numerically
    controlled machine tools, or not. [CONFIDENCE: moderate -- I am
    confident 別表第一 has a machine-tool-covering item number
    (Wassenaar Category 2 lineage), but I am NOT citing a specific item
    number here because I am not fully certain which one without
    independent verification; see honest-coverage note below.] Japan
    additionally operates its own catch-all control (キャッチオール規制,
    Export Trade Control Order Article 4), an end-use/end-user-based
    backstop structurally analogous to (but legally distinct from) the
    EAR's own Military End User Rule.
  - DEU: Bundesamt für Wirtschaft und Ausfuhrkontrolle (BAFA) enforces
    Regulation (EU) 2021/821 (the dual-use export-control recast,
    directly applicable in every EU member state) Annex I -- Category 2
    (Materials Processing), covering numerically controlled machine
    tools under the SAME 2B001-style item numbering as the EAR's CCL
    (both derive from the Wassenaar Arrangement Dual-Use List).
  - GBR: the Export Control Joint Unit (ECJU), Department for Business
    and Trade, administers the UK Strategic Export Control Lists under
    the Export Control Order 2008 (SI 2008/3231) -- Category 2
    (Materials Processing), structurally equivalent (same Wassenaar-
    derived numbering) to the EU Annex I list it diverged from.
  - ITA: since 11 August 2023 (Decreto Legislativo 15 dicembre 2017,
    n. 221, art. 4, current text -- the article's text was amended
    four times; this is the version in force with no end date as of
    verification), the Autorita' nazionale -- UAMA (Unita' per le
    Autorizzazioni dei Materiali di Armamento), housed within the
    Ministero degli Affari Esteri e della Cooperazione Internazionale
    (MAECI) and established by Legge 9 luglio 1990, n. 185, art. 7-bis,
    is the Autorita' competente for dual-use goods, implementing the
    SAME Regulation (EU) 2021/821 Annex I Category 2 that DEU's BAFA
    administers (both EU member states apply the directly-applicable
    EU regulation). UAMA is structurally distinct from BAFA/ECJU/METI
    in one respect worth flagging: it consolidates conventional-arms
    export licensing (Legge 185/1990) AND dual-use goods export
    licensing (D.Lgs. 221/2017) into a single authority, rather than
    a dual-use-only dedicated agency. [CONFIDENCE: high -- verified
    directly against the currently-in-force statutory text on
    Normattiva (the Italian State's official portal for legge vigente)
    and against MAECI's own UAMA page (fetched via the Internet
    Archive Wayback Machine because the live esteri.it triggers
    Radware bot-detection on automated fetches); see git history for
    the exact quoted text and retrieval trail.]
  - NLD: the Centrale Dienst voor In- en Uitvoer (CDIU, Central Import
    and Export Office), part of Belastingdienst/Douane (the Dutch
    Customs Administration), is the licensing/notification authority
    for strategic goods -- it grants the export/transit authorisations
    (or receives notifications) for dual-use and military goods, and
    handles classification requests for whether a given good falls
    within the control lists. Like DEU and ITA, the Netherlands is an
    EU member state and therefore applies the SAME directly-applicable
    Regulation (EU) 2021/821 (dual-use export-control recast) Annex I
    Category 2 (Materials Processing) as the substantive control list
    -- the Dutch implementing decree, the Besluit strategische
    goederen (Strategic Goods Decree, BWBR0024139), defines
    'Verordening producten voor tweeërlei gebruik' (the dual-use
    products Regulation) in its own Artikel 1 definitions section as
    exactly this recast regulation, citing it by its Official Journal
    reference '(PbEU 2021, L 206)' -- Regulation (EU) 2021/821 was
    itself published in OJ L 206 of 11 June 2021, confirming this is
    the SAME 2021 recast DEU/ITA cite, not the superseded 2009
    predecessor (Regulation (EC) 428/2009) that an older, unrelated
    government.nl overview page (last modified 2019, before the 2021
    recast existed) still references -- that stale page was NOT used
    as the basis for this entry. [CONFIDENCE: high -- the Besluit
    strategische goederen definition and the CDIU's role were both
    verified directly against primary Dutch-government sources: the
    decree's current consolidated text on wetten.overheid.nl (the
    Dutch State's official portal for geldende wet- en regelgeving,
    version in force since 2025-02-05), and the CDIU's own strategic-
    goods page (fetched via the Internet Archive Wayback Machine
    because the page has since moved/been retired on the live
    belastingdienst.nl site -- a 404, not a bot-detection block); see
    git history for the exact quoted text and retrieval trail.]

  The required-evidence set (credit-clearance record, contract/PO,
  sanctions-screening (OFAC/equivalent) record, military end-use/end-
  user screening record) mirrors the GENERIC counterparty-diligence
  evidence a precision-machinery wholesale compliance function demands
  before ANY order proceeds -- it deliberately does NOT include a
  capability-classification-record item: unlike the generic checklist
  items (procedural, the same shape regardless of what is being
  traded), this vertical's capability-threshold determination is
  evaluated by a DEDICATED governor check
  (`capability-threshold-uncertified-violations`) that INDEPENDENTLY
  RECOMPUTES the answer from the machine's own raw technical specs via
  a pure function (`machtooltrade.registry/capability-threshold-
  crossed?`), not a checklist item an advisor could merely claim was
  checked -- see `machtooltrade.governor` namespace docstring.

  Coverage is reported HONESTLY (see `coverage`), the same discipline
  every sibling actor's `facts` namespace uses: a jurisdiction not in
  this table has NO spec-basis, full stop -- the advisor must not
  fabricate one, and the governor holds if it tries.")

(def catalog
  "iso3 -> requirement map. `:required-evidence` is the GENERIC
  counterparty-diligence evidence set (credit-clearance record,
  contract/PO, sanctions-screening record, military end-use/end-user
  screening record); `:legal-basis` / `:owner-authority` / `:provenance`
  are the G2 citation the governor requires before any
  `:contract/verify` proposal can commit. `:classification-list` names
  the SPECIFIC control-list category this jurisdiction uses to classify
  a machine tool's own capability."
  {"USA" {:name "USA"
          :owner-authority "Bureau of Industry and Security (BIS), U.S. Department of Commerce"
          :legal-basis "Export Administration Regulations (15 C.F.R. Parts 730-774); Military End User (MEU) Rule (15 C.F.R. §744.21)"
          :classification-list "Commerce Control List (CCL), Category 2 (Materials Processing), ECCN 2B001 (numerically controlled machine tools and specially designed components/accessories); or EAR99"
          :provenance "https://www.bis.doc.gov/"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "JPN" {:name "JPN"
          :owner-authority "経済産業省 (METI) 貿易経済協力局 安全保障貿易管理課"
          :legal-basis "輸出貿易管理令 (Export Trade Control Order)"
          :classification-list "輸出貿易管理令別表第一 (Appended Table 1) 該非判定 (gaihi-hantei) -- 工作機械 (machine tools) 該当項番; キャッチオール規制 (Article 4 catch-all)"
          :provenance "https://www.meti.go.jp/policy/anpo/"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "DEU" {:name "DEU"
          :owner-authority "Bundesamt für Wirtschaft und Ausfuhrkontrolle (BAFA)"
          :legal-basis "Regulation (EU) 2021/821 (dual-use export-control recast)"
          :classification-list "Annex I, Category 2 (Materials Processing) -- numerically controlled machine tools, Wassenaar-derived shared numbering with the US CCL's 2B001"
          :provenance "https://www.bafa.de/"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "GBR" {:name "GBR"
          :owner-authority "Export Control Joint Unit (ECJU), Department for Business and Trade"
          :legal-basis "Export Control Order 2008 (SI 2008/3231)"
          :classification-list "UK Strategic Export Control Lists, Category 2 (Materials Processing)"
          :provenance "https://www.gov.uk/guidance/beginners-guide-to-export-controls"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "ITA" {:name "ITA"
          :owner-authority "Autorità nazionale -- UAMA (Unità per le Autorizzazioni dei Materiali di Armamento), Ministero degli Affari Esteri e della Cooperazione Internazionale (MAECI)"
          :legal-basis "Regulation (EU) 2021/821 (dual-use export-control recast, directly applicable); Decreto Legislativo 15 dicembre 2017, n. 221, art. 4 (national competent-authority designation, text in force since 11 August 2023); Legge 9 luglio 1990, n. 185, art. 7-bis (establishes UAMA at MAECI)"
          :classification-list "Annex I, Category 2 (Materials Processing) -- numerically controlled machine tools, same Wassenaar-derived 2B001-lineage item numbering as DEU/GBR/USA"
          :provenance "http://web.archive.org/web/20250922093830/https://www.esteri.it/it/ministero/struttura/uama/"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "NLD" {:name "NLD"
          :owner-authority "Centrale Dienst voor In- en Uitvoer (CDIU), Belastingdienst/Douane (Dutch Customs Administration)"
          :legal-basis "Regulation (EU) 2021/821 (dual-use export-control recast, directly applicable); Besluit strategische goederen (Strategic Goods Decree, BWBR0024139), Artikel 1-4c"
          :classification-list "Annex I, Category 2 (Materials Processing) -- numerically controlled machine tools, same Wassenaar-derived 2B001-lineage item numbering as DEU/GBR/ITA/USA"
          :provenance "http://web.archive.org/web/20250712135758/https://www.belastingdienst.nl/wps/wcm/connect/bldcontenten/belastingdienst/customs/safety_health_economy_and_environment/cdiu_cluster/strategic_goods/strategic_goods"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}})

(defn spec-basis
  "The jurisdiction's requirement map, or nil -- nil means NO spec-basis,
  and the governor must hold any proposal that tries to dispatch or
  settle an invoice on it."
  [iso3]
  (get catalog iso3))

(defn coverage
  "Honest coverage report: how many of the requested jurisdictions
  actually have a spec-basis entry. Never report a missing jurisdiction
  as covered."
  ([] (coverage (keys catalog)))
  ([iso3s]
   (let [have (filter catalog iso3s)
         missing (remove catalog iso3s)]
     {:requested (count iso3s)
      :covered (count have)
      :covered-jurisdictions (vec (sort have))
      :missing-jurisdictions (vec (sort missing))
      :note (str "cloud-itonami-isic-4659 R0: " (count catalog)
                 " jurisdictions seeded with an official spec-basis. "
                 "This is a starting catalog, not a survey of all ~194 "
                 "jurisdictions -- extend `machtooltrade.facts/catalog`, "
                 "never fabricate a jurisdiction's requirements.")})))

(defn required-evidence-satisfied?
  "Does `submitted` (a set/coll of evidence keywords or strings) satisfy
  every evidence item listed for `iso3`? Missing spec-basis -> never
  satisfied."
  [iso3 submitted]
  (when-let [{:keys [required-evidence]} (spec-basis iso3)]
    (let [need (count required-evidence)
          have (count (filter (set submitted) required-evidence))]
      (= need have))))

(defn evidence-checklist [iso3]
  (:required-evidence (spec-basis iso3) []))
