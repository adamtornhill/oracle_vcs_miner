;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html
(ns oracle-vcs-miner.core
  (:require [instaparse.core :as insta]
            [clj-time.format :as tf]))

;;; This module is responsible for parsing version control data from 
;;; SQL files retrieved from an Oracle DB.

;; The entity name is extracted from the file name (always a 1 to 1 match).
;; The rest of the info is parsed from the header as described below.

;; The files contain a header that we just skip.
;; We start to parse from here:
;;
;; /**************************************************************************
;; **
;; **	File Name	:	some_name.sql
;; **
;; **	Project 	:	An important one
;; **
;; **	Author		:	xyz
;; **
;; **	Date Written	:	28/09/15
;; **
;; **	Version		:	174
;; **
;; **	Description	:	Pretty straightforward
;; **
;; ***************************************************************************
;; **			      VERSION CONTROL
;; ***************************************************************************
;; **   Date   * Author   * Change  * Description
;; ***************************************************************************
;; **	    *	       *	 *
;; ** 21/07/98 * abc  * T5193	 * Towards greatness
;; ** 10/09/98 * xyz  * T5449	 * Loads of bug fixes.
;;
;; Special care has to be taken when multiple changes occour on the 
;; same day since these changes are recorded _without_ the leading 
;; date field in that case! Here's how it looks:
;;
;; ** 22/09/99 * abc * T6754	 * This is the first change on a given day.
;; **	    * xyz    * T6481	 * Then I come along and make
;; **	    *	       *	 * one more change on the same day!

(defn vcs-start?
  [line]
  (re-matches #"^\*\*\s+VERSION CONTROL$" line))

(defn vcs-end?
  [line]
  (re-matches #"\*\*+/" line))

(def ^:const oracle-vcs-grammar
  "
  <entry>        = header changes*
  <header>       = <start-of-head> file-name <(#'.*' nl)*> <prelude>
  start-of-head  = #'^/\\*+' nl '**' nl
  file-name      =  <#'^\\*\\*\\s+File Name\\s+:\\s+'> #'[\\w_]+\\.sql' <nl>
  prelude        = <star-line> <header-info> <star-line> <empty-line>
  star-line      = #'^\\*+' nl
  header-info    = '**   Date   * Author   * Change  * Description' nl
  empty-line     = '**	    *	       *	 *' nl
  <changes>      = (change | (change <empty-line>))*
  change         = <begin-line> date <separator> author <separator> change-id <separator> <comment>
  date           = #'\\d{2}/\\d{2}/\\d{2}'
  author         = #'\\w+'
  change-id      = #'[\\w\\d]+'
  comment        = ((comment-text <nl?>) | (comment-text <nl> comment-lead))*
  <comment-lead> = '**	    *	       *	 * '
  <comment-text> = #'[^\\n]*'
  begin-line     = '** '
  separator      = #'\\s*\\*\\s*'
  nl             =  '\\n'")

(def oracle-parser (insta/parser oracle-vcs-grammar))

(defn parse
  [text]
  (insta/parse oracle-parser text))

(defn- start-of-header?
  [line]
  (re-matches #"^/\*+$" line))

(defn- end-of-header?
  [line]
  (re-matches #"^\*+/$" line))

(defn- scan-next-header
  [lines]
  (->> lines
       (drop-while (comp nil? start-of-header?))
       (split-with (comp nil? end-of-header?))))

(defn extract-all-vcs-headers-from
  [lines-left acc]
  (if (not (seq lines-left))
    acc
    (let [[header more-lines-left] (scan-next-header lines-left)]
      (recur more-lines-left (conj acc (into [] header))))))

(defn remove-non-vcs-info-from
  [headers]
  (filter #(some vcs-start? %) headers))

(defn extract-vcs-header-from
  [lines]
  (->> lines
       (drop-while (comp not vcs-start?))
       (drop 1) ; vcs-start
       (take-while (comp not vcs-end?))
       (clojure.string/join "\n")))

(def ^:private input-time-formatter (tf/formatter "dd/MM/yy"))
(def ^:private output-time-formatter (tf/formatters :year-month-day))

(defn as-output-time
  [v]
  (->> v
       (tf/parse input-time-formatter)
       (tf/unparse output-time-formatter)))

(defn as-identity-rows
  "Transforms the parse results into the identity format that 
   we use to calculate evolutionary metrics.
   The input looks like this:
    [:change [:date '21/07/98'] [:author 'abc'] [:change-id 'T5193']]"
  [v]
  (let [file-name (get-in v [0 1])
        changes (rest v)]
    (for [c changes
           :let [date (get-in c [1 1])
                 author (get-in c [2 1])
                 revision (get-in c [3 1])]]
      [author revision (as-output-time date) file-name])))

(defn as-csv
  [vs]
  (->> vs
       (map #(clojure.string/join "\n" %))
       (map parse)
       (map as-identity-rows)
       (apply concat)))

(defn as-csv-identity-from-full-file
  [file-name]
  (with-open [rdr (clojure.java.io/reader file-name)]
    (-> (line-seq rdr)
        (extract-all-vcs-headers-from [])
        remove-non-vcs-info-from
        as-csv)))
