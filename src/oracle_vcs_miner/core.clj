;;; Copyright (C) 2013 Adam Tornhill
;;;
;;; Distributed under the GNU General Public License v3.0,
;;; see http://www.gnu.org/licenses/gpl.html
(ns oracle-vcs-miner.core
  (:require [instaparse.core :as insta]))

;;; This module is responsible for parsing version control data from 
;;; SQL files retrieved from an Oracle DB.

;; The files contain a header that we just skip.
;; We start to parse from here:
;;
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

(def ^:const git-grammar
  
  "
    entry     = <prelude*> prelude changes (* covers pull requests *)
    <prelude> = <separator> rev <separator> date <separator> author <nl>
    rev       =  #'[\\da-f]+'
    author    =  #'[^\\n]*'
    date      =  #'\\d{4}-\\d{2}-\\d{2}'
    changes   =  change*
    change    =  added <tab> deleted <tab> file <nl>
    added     =  numstat
    deleted   =  numstat
    <numstat> =  #'[\\d-]*' (* binary files are presented with a dash *)
    file      =  #'.+'
    separator = '--'
    ws        =  #'\\s'
    tab       =  #'\\t'
    nl        =  '\\n'")

