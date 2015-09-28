(ns oracle-vcs-miner.core-test
  (:require [clojure.test :refer :all]
            [oracle-vcs-miner.core :as m]))

(deftest identifies-start-of-vcs-info
  (is (nil? (m/vcs-start? "CREATE OR REPLACE")))
  (is (nil? (m/vcs-start? "/**************************************************************************")))
  (is (m/vcs-start? "**			      VERSION CONTROL")))
