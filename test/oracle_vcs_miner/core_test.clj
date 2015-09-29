(ns oracle-vcs-miner.core-test
  (:require [clojure.test :refer :all]
            [oracle-vcs-miner.core :as m]))

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
** 10/09/98 * xyz  * T5449	 * But this is so cool that
**	    *	       *	 * I need two lines to describe it.
")

(deftest parses-vcs-header
  (is (= (m/parse input-sample)
         [[:change [:date "21/07/98"] [:author "abc"] [:change-id "T5193"]]
          [:change [:date "10/09/98"] [:author "xyz"] [:change-id "T5449"]]])))
