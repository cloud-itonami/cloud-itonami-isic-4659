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
  - CHE: UNLIKE DEU/ITA/NLD, Switzerland is not an EU member state and
    therefore does not apply Regulation (EU) 2021/821 directly -- it is
    nonetheless a Wassenaar Arrangement participating state with its
    OWN national dual-use export-control regime: the Bundesgesetz
    ueber die Kontrolle zivil und militaerisch verwendbarer Gueter,
    besonderer militaerischer Gueter sowie strategischer Gueter
    (Gueterkontrollgesetz, GKG -- Federal Act on the Control of
    Dual-Use Goods, Specific Military Goods and Strategic Goods, Goods
    Control Act/GCA), SR 946.202, of 13 December 1996, and its
    implementing Gueterkontrollverordnung (Goods Control Ordinance,
    GCO), SR 946.202.1. The State Secretariat for Economic Affairs
    (SECO) is, in its own words, 'the licensing authority for the
    export of dual-use goods and for specific military goods'.
    Industrial products are classified as dual-use goods if they meet
    the technical control criteria in Annex 2 (Part 1 or Part 2) of
    the GCO -- like the EU's Annex I, the US CCL and Japan's Appended
    Table 1, this annex is itself Wassenaar-derived, so it carries the
    same 2B001-lineage machine-tool item numbering. Goods covered by
    the GCO annexes require a formal export licence under Article 3(1)
    GCO regardless of destination; Article 3(3) GCO extends the
    licence requirement to goods incorporating listed components as
    a main element or more than 25% of value; Article 3(4) GCO is
    Switzerland's own catch-all, requiring a licence for an otherwise-
    uncontrolled good the exporter knows or has reason to believe is
    intended for NBC-weapon development, manufacture, use, transfer or
    deployment -- structurally analogous to (but legally distinct
    from) the EAR's Military End User Rule and Japan's Article 4
    catch-all. [CONFIDENCE: high -- verified directly against SECO's
    own 'Legal Basis and Forms' page (a live, server-rendered
    seco.admin.ch page, not a Wayback fetch), which names SECO as
    licensing authority and cites GCA/GCO by their official CC
    (Classified Compilation) numbers 946.202 and 946.202.1 with
    fedlex.admin.ch links; the SECO page's own quoted text on Annex 2
    classification and Articles 3(1)/3(3)/3(4) GCO was independently
    re-checked verbatim against the raw fetched HTML before this entry
    was committed. The GCA/GCO statutory text itself on fedlex.admin.ch
    is a JS-required Angular shell (not bot-detection -- the page's own
    noscript warning states it needs a JavaScript-capable browser), so
    this entry relies on SECO's own authoritative restatement of the
    provisions rather than the raw statute text; see git history for
    the exact quoted text and retrieval trail.]
  - SWE: like DEU/ITA/NLD, Sweden is an EU member state and applies
    the SAME directly-applicable Regulation (EU) 2021/821 (dual-use
    export-control recast) as the substantive control list. Sweden's
    OWN supplementary national implementing statute, Lag (2000:1064)
    om kontroll av produkter med dubbla användningsområden och av
    tekniskt bistånd (Act (2000:1064) on the Control of Dual-Use
    Products and of Technical Assistance, as amended through SFS
    2024:840), is UNUSUAL among this catalog's entries in that its own
    Section 4 (4 §) NAMES the licensing authority directly in the
    statutory text itself: 'Frågor om tillstånd och förbud enligt
    Europaparlamentets och rådets förordning (EU) 2021/821, denna
    lag eller föreskrifter som har meddelats med stöd av lagen
    prövas av Inspektionen för strategiska produkter eller den
    myndighet som regeringen bestämmer' ('Questions concerning
    permits and prohibitions under Regulation (EU) 2021/821 ..., this
    Act, or regulations issued under this Act, are examined by the
    Inspectorate of Strategic Products (ISP) or the authority
    determined by the Government') -- unlike DEU/ITA/NLD/CHE, where
    the licensing authority is confirmed via the authority's OWN
    website rather than the statute naming it explicitly. [CONFIDENCE:
    high -- verified directly against the current consolidated text
    of Lag (2000:1064) on riksdagen.se (Sveriges riksdag's official
    Svensk författningssamling portal, a live, server-rendered page,
    HTTP 200, no bot-detection), which quotes SFS 2000:1064 Section 1
    ('Lagen innehåller kompletterande bestämmelser till ...
    förordning (EU) 2021/821 ...' -- 'The Act contains supplementary
    provisions to ... Regulation (EU) 2021/821') and Section 4 as
    above verbatim; cross-checked against ISP's own English-language
    'Our assignments' page (isp.se/eng/our-assignments/, also live and
    server-rendered), which independently confirms 'The ISP decides
    export cases regarding dual-use items.' See git history for the
    exact quoted text and retrieval trail.]
  - NOR: UNLIKE DEU/ITA/NLD/SWE, Norway is not an EU member state (it
    is EEA/EFTA) and therefore does not apply Regulation (EU) 2021/821
    directly -- but unlike CHE (which runs a fully independent national
    control list), Norway VOLUNTARILY adopts the EU's own control
    lists wholesale into its national regulation: 'Norway uses the
    EU's control lists to ensure that we understand and implement the
    control in a manner as consistent as possible with other European
    countries,' per DEKSA's own 'Control lists' page -- a THIRD
    distinct EU-relationship pattern this catalog now covers, alongside
    direct EU-regulation applicability (DEU/ITA/NLD/SWE) and a fully
    independent national list (CHE). The regulatory framework consists
    of the Export Control Act (Act of 18 December 1987 on control of
    the export of strategic goods, services, technology, etc.) and the
    Export Control Regulation (Regulation of 19 June 2013 No. 718
    relating to the export of defence-related products, dual-use
    items, technology and services, Ministry of Foreign Affairs). The
    Act's own operative text: 'goods and technology that may be
    significant for the development, production, or use of products
    for military purposes by other countries, or that can directly
    contribute to developing a country's military capabilities, as
    well as goods and technology that can be used to carry out
    terrorist acts, cf. the Penal Code paragraph 131, must not be
    exported from Norway without special permission.' The licensing
    authority is a BRAND-NEW agency: the Norwegian Agency for Export
    Control and Sanctions (DEKSA) -- established by Royal Resolution
    on 9 February 2024, with administrative responsibility for this
    regulatory framework transferred from the Ministry of Foreign
    Affairs to DEKSA only on 1 January 2025, per DEKSA's own 'Mandate
    and Responsibility' page. The Export Control Regulation's own
    Control List II (dual-use items) mirrors the EU's numbered
    category structure -- e.g. 'Category 6, which includes sensors and
    lasers' -- so machine tools fall under the same Wassenaar/EU-
    derived Category 2 (Materials Processing) numbering as DEU/GBR/
    ITA/NLD/SWE/USA. [CONFIDENCE: high -- verified directly against
    three live, server-rendered deksa.no pages (regulatory-framework,
    control-lists, mandate-and-responsibility -- all HTTP 200, no
    bot-detection), each quoted verbatim above. NOTE: regjeringen.no
    (the Norwegian government's own general portal, which used to host
    this material before DEKSA's 2024/2025 establishment) triggers a
    Cloudflare 'Just a moment' bot-detection challenge on automated
    fetches -- per this fleet's hard safety-floor rule that page was
    NOT bypassed and no Wayback Machine snapshot exists for it either;
    lovdata.no (Norway's official statute database) also blocked a
    search-endpoint request with a Varnish WAF 405 and 404'd on two
    guessed direct URLs for the Export Control Act, so this entry
    relies entirely on DEKSA's own authoritative restatement rather
    than the raw statute text. See git history for the exact quoted
    text and retrieval trail.]

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
                              "military end-use/end-user screening record"]}
   "CHE" {:name "CHE"
          :owner-authority "State Secretariat for Economic Affairs (SECO)"
          :legal-basis "Federal Act on the Control of Dual-Use Goods, Specific Military Goods and Strategic Goods (Goods Control Act, GCA), SR 946.202, of 13 December 1996; Ordinance on the Control of Dual-Use Goods, Specific Military Goods and Strategic Goods (Goods Control Ordinance, GCO), SR 946.202.1, Art. 3(1)/(3)/(4)"
          :classification-list "GCO Annex 2 (Part 1 or Part 2) -- dual-use goods, Wassenaar-derived shared numbering with the EU Annex I / US CCL 2B001; GCO Annex 3 -- specific military goods"
          :provenance "https://www.seco.admin.ch/en/industrial-goods-legal-basis"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "SWE" {:name "SWE"
          :owner-authority "Inspektionen för strategiska produkter (Inspectorate of Strategic Products, ISP)"
          :legal-basis "Regulation (EU) 2021/821 (dual-use export-control recast, directly applicable); Lag (2000:1064) om kontroll av produkter med dubbla användningsområden och av tekniskt bistånd (Act on the Control of Dual-Use Products and of Technical Assistance, as amended through SFS 2024:840), 4 §"
          :classification-list "Annex I, Category 2 (Materials Processing) -- numerically controlled machine tools, same Wassenaar-derived 2B001-lineage item numbering as DEU/GBR/ITA/NLD/USA"
          :provenance "https://www.riksdagen.se/sv/dokument-och-lagar/dokument/svensk-forfattningssamling/lag-20001064-om-kontroll-av-produkter-med_sfs-2000-1064/"
          :required-evidence ["credit-clearance record"
                              "contract/PO"
                              "sanctions-screening (OFAC/equivalent) record"
                              "military end-use/end-user screening record"]}
   "NOR" {:name "NOR"
          :owner-authority "Norwegian Agency for Export Control and Sanctions (DEKSA), established by Royal Resolution 9 February 2024, administrative responsibility transferred from the Ministry of Foreign Affairs on 1 January 2025"
          :legal-basis "Act of 18 December 1987 on control of the export of strategic goods, services, technology, etc. (Export Control Act); Regulation of 19 June 2013 No. 718 relating to the export of defence-related products, dual-use items, technology and services (Export Control Regulation), Ministry of Foreign Affairs"
          :classification-list "Control List II (dual-use items), an annex to the Export Control Regulation -- Norway adopts the EU's own control lists wholesale, so machine tools fall under the same Wassenaar/EU-derived Category 2 (Materials Processing) numbering as DEU/GBR/ITA/NLD/SWE/USA"
          :provenance "https://deksa.no/en/export-control/do-you-need-a-licence/regulatory-framework/"
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
