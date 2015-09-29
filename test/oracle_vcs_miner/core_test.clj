(ns oracle-vcs-miner.core-test
  (:require [clojure.test :refer :all]
            [oracle-vcs-miner.core :as m]))

(def input-sample
  "/**************************************************************************
**
**	File Name	:       some_file.sql
**
**	Project 	:	Some System
**
**	Author		:	xyz
**
**	Date Written	:	28/09/15
**
**	Version		:	174
**
**	Description	:	This is a good one
**
***************************************************************************
**   Date   * Author   * Change  * Description
***************************************************************************
**	    *	       *	 *
** 21/07/98 * abc  * T5193	 * Did something cool
**	    *	       *	 *
** 10/09/98 * xyz  * T5449	 * But this is so cool that
**	    *	       *	 * I need two lines to describe it.
")

(deftest parses-vcs-header
  (is (= (m/parse input-sample)
         [[:file-name "some_file.sql"]
          [:change [:date "21/07/98"] [:author "abc"] [:change-id "T5193"]]
          [:change [:date "10/09/98"] [:author "xyz"] [:change-id "T5449"]]])))

(deftest converts-to-external-time-format
  (is (= (m/as-output-time "21/07/98")
         "1998-07-21"))
  (is (= (m/as-output-time "21/07/15")
         "2015-07-21")))

(deftest transforms-parse-results-to-identity-rows
  (is (= (m/as-identity-row "my.sql" [:change [:date "21/07/98"] [:author "abc"] [:change-id "T5193"]])
         ["abc" "T5193" "1998-07-21" "my.sql"])))
