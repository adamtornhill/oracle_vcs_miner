(ns oracle-vcs-miner.core-test
  (:require [clojure.test :refer :all]
            [oracle-vcs-miner.core :as m])
  (:import [java.io BufferedReader StringReader]))

(deftest identifies-start-of-vcs-info
  (is (nil? (m/vcs-start? "CREATE OR REPLACE")))
  (is (nil? (m/vcs-start? "/**************************************************************************")))
  (is (m/vcs-start? "**			      VERSION CONTROL")))

(deftest identifies-end-of-vcs-info
  (is (nil? (m/vcs-end? "**	    *	       *	 *")))
  (is (m/vcs-end? "**************************************************************************/")))

(def input-sample
  "***************************************************************************
**   Date   * Author   * Change  * Description
***************************************************************************
**	    *	       *	 *
** 21/07/98 * abc  * T5193	 * Did something cool
**	    *	       *	 *
** 10/09/98 * xyz  * T5449	 * But this is so cool that
**	    *	       *	 * I need two lines to describe it.
")

(def complete-sample
  "CREATE OR REPLACE
PROCEDURE some_amazing_procedure
/**************************************************************************
**
**	File Name	:	some_cool_script.sql
**
**	Project 	:	World Domination
**
**	Author		:	xyz
**
**	Date Written	:	28/09/15
**
**	Version		:	7
**
**	Description	:	Just do it
**
***************************************************************************
**			      VERSION CONTROL
***************************************************************************
**   Date   * Author   * Change  * Description
***************************************************************************
**	    *	       *	 *
** 21/07/98 * abc  * T5193	 * Did something cool
** 10/09/98 * xyz  * T5449	 * But this is so cool that
**	    *	       *	 * I need two lines to describe it.
**************************************************************************/
(
 SELECT something FROM anywhere
")

(def input-seq (line-seq (BufferedReader. (StringReader. complete-sample))))

(deftest parses-vcs-header
  (is (= (m/parse input-sample)
         [[:change [:date "21/07/98"] [:author "abc"] [:change-id "T5193"]]
          [:change [:date "10/09/98"] [:author "xyz"] [:change-id "T5449"]]])))

(deftest extracts-vcs-header-from-content
  (is (= (m/extract-vcs-header-from input-seq)
         "***************************************************************************\n**   Date   * Author   * Change  * Description\n***************************************************************************\n**\t    *\t       *\t *\n** 21/07/98 * abc  * T5193\t * Did something cool\n** 10/09/98 * xyz  * T5449\t * But this is so cool that\n**\t    *\t       *\t * I need two lines to describe it.")))

(deftest converts-to-external-time-format
  (is (= (m/as-output-time "21/07/98")
         "1998-07-21"))
  (is (= (m/as-output-time "21/07/15")
         "2015-07-21")))

(deftest transforms-parse-results-to-identity-rows
  (is (= (m/as-identity-row "my.sql" [:change [:date "21/07/98"] [:author "abc"] [:change-id "T5193"]])
         ["abc" "T5193" "1998-07-21" "my.sql"])))
