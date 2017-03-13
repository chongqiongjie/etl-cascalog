;#*********************************
;# [intro]
;#   author=larluo@spiderdt.com
;#   func=partition algorithm for data warehouse
;#=================================
;# [param]
;#   tabname=staging table name
;#   prt_cols_str=ods partition cols
;#=================================
;# [caller]
;#   [PORG] bolome.dau
;#   [PORG] bolome.event
;#   [PORG] bolome.inventory
;#   [PORG] bolome.order
;#   [PORG] bolome.product_category
;#   [PORG] bolome.show
;#=================================
;# [version]
;#   v1_0=2017-01-20@zhuo {create}
;#*********************************

(ns cocacola.rpt.m-cocacola-score_gt_rural
  (:require [cascalog.api :refer [?- ??- <- ?<- ??<- stdout defmapfn mapfn defmapcatfn mapcatfn defaggregatefn aggregatefn cross-join select-fields]]
            [cascalog.logic.ops :as c]
            [taoensso.timbre :refer [info debug warn set-level!]]
            [clj-time.core :as t :refer [last-day-of-the-month-]]
            [clj-time.format :as tf]
            [clj-time.periodic :refer [periodic-seq]]
            [clojure.core.match :refer [match]]
            [cheshire.core :refer [generate-string]]
            [clojurewerkz.balagan.core :as tr :refer [extract-paths]]
            [common.trgx :refer :all]
            [clojure.string :as str])
  (:import [cascading.tuple Fields]
           [cascading.jdbc JDBCTap JDBCScheme]))

(set-level! :warn)

(def rural-score-tap-in (pg-tap "dw" "model.d_cocacola_score_rural" ["dw-dt" "period" "mbd" "bg" "bottler" "channel" "code" "item" "fact" "value" "abbrevation"]))
(def report-tap-out (pg-tap "ms" "report" ["dw-dt" "project" "category" "report" "selector" "selector-desc" "dimension-metrics"]))


(def score-dt-kv
  (<- [?mbd ?bg ?bottler ?channel !code !item ?fact ?abbr ?dw-dt-kv]
      (rural-score-tap-in :> ?dw-dt ?period ?mbd ?bg ?bottler ?channel !code !item ?fact ?value ?abbr)
      (collect-kv ?dw-dt ?value :> ?dw-dt-kv)))


(defn score-sliding  [[start-dt end-dt]]
  (<- [?dw-dt ?mbd ?bg ?bottler ?channel !code !item ?fact ?abbr ?value ?max-value !pp-value !vs-pp-value !last-dec-value !vs-last-dec-value !last-year-same-month-value !vs-last-year-same-month-value]
      (score-dt-kv :> ?mbd ?bg ?bottler ?channel !code !item ?fact ?abbr ?dw-dt-kv)
      ((c/comp split-rows mk-month-dts) start-dt end-dt :> ?dw-dt)
      ((c/juxt prev-last-day prev-last-month prev-same-month) ?dw-dt :> !prev-last-day !prev-last-month !last-year-same-month)
      ((mapfn [x]  (->> x vals (apply max))) ?dw-dt-kv :> ?max-value)
      (kv->lkp ?dw-dt-kv ?dw-dt !prev-last-day !prev-last-month !last-year-same-month :> ?value !pp-value !last-dec-value !last-year-same-month-value)
      ((mapfn [a b] (if (nil? b) nil (- a b))) ?value !pp-value :> !vs-pp-value)
      ((mapfn [a b] (if (nil? b) nil (- a b))) ?value !last-dec-value :> !vs-last-dec-value)
      ((mapfn [a b] (if (nil? b) nil (- a b))) ?value !last-year-same-month-value :> !vs-last-year-same-month-value)))


(defn score-gt_rural_bottler-report [[start-dt end-dt]]
  (<- [?dw-dt ?project ?category ?report ?selector ?selector-desc ?dimension-metrics]
      ((score-sliding [start-dt end-dt]) :> ?dw-dt ?mbd ?bg ?bottler ?channel !code !item ?fact ?abbr ?value ?max-value !pp-value !vs-pp-value !last-dec-value !vs-last-dec-value !last-year-same-month-value !vs-last-year-same-month-value)
      ((mapfn [bottler] (and (when-not (re-find #"\S*全体$" bottler) bottler)
                             (when-not (re-find #"\S*全国总体$" bottler) bottler))) ?bottler :> ?bottlers)
      (identity ["cocacola" "score" "GT_Rural" ""] :> ?project ?category ?report ?selector-desc)
      ((vars->pair [:period :button]) ?dw-dt "test" :> ?selector-edn)
      ((tr-dimension-metrics [:bg :bottler] [:abbr :score :pp_score :vs_pp_score :last_dec_score :vs_last_dec_score :last_year_same_month_score :vs_last_year_same_month_score]) ?bg ?bottlers ?abbr ?value !pp-value !vs-pp-value !last-dec-value !vs-last-dec-value !last-year-same-month-value !vs-last-year-same-month-value :> ?dimension-metrics-edn)
      ((c/each pair-edn->json) ?selector-edn ?dimension-metrics-edn :> ?selector ?dimension-metrics)))

#_(?<- (stdout)
        [?dw-dt ?project ?category ?report ?selector ?selector-desc ?dimension-metrics]
        ((score-gt_rural_bottler-report ["2015-01-01" "2016-01-01"]) :> ?dw-dt ?project ?category ?report ?selector ?selector-desc ?dimension-metrics))

(defn -main []
  (def dt-rng (report->next-dt report-tap-out "GT_Rural"))
  (prn {:dt-rng dt-rng :rpt "GT_Rural"} "running...")
  (try (?- report-tap-out (score-gt_rural_bottler-report dt-rng)) (catch Exception _))
  (prn {:dt-rng dt-rng} "done!"))
