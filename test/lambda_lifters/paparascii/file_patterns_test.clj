(ns lambda-lifters.paparascii.file-patterns-test
  (:require [clojure.test :refer :all]
            [lambda-lifters.paparascii.file-patterns :as fp]))

(deftest test-matches-pattern-extensions
  (testing "Pattern matching with file extensions"
    (is (fp/matches-pattern? "script.sh" "*.sh")
        "Should match .sh extension")
    (is (fp/matches-pattern? "test.cgi" "*.cgi")
        "Should match .cgi extension")
    (is (fp/matches-pattern? "program.bb" "*.bb")
        "Should match .bb extension")
    (is (not (fp/matches-pattern? "script.py" "*.sh"))
        "Should not match wrong extension")
    (is (not (fp/matches-pattern? "test.cgi" "*.sh"))
        "Should not match different extension")))

(deftest test-matches-pattern-exact-names
  (testing "Pattern matching with exact filenames"
    (is (fp/matches-pattern? "my-script" "my-script")
        "Should match exact filename")
    (is (fp/matches-pattern? "deploy" "deploy")
        "Should match exact filename without extension")
    (is (not (fp/matches-pattern? "my-script" "other-script"))
        "Should not match different filename")
    (is (not (fp/matches-pattern? "my-script.sh" "my-script"))
        "Should not match if extension added")))

(deftest test-matches-pattern-wildcard
  (testing "Pattern matching with wildcard"
    (is (fp/matches-pattern? "anything" "*")
        "Wildcard should match any filename")
    (is (fp/matches-pattern? "script.sh" "*")
        "Wildcard should match files with extensions")
    (is (fp/matches-pattern? "no-extension" "*")
        "Wildcard should match files without extensions")))

(deftest test-matches-any-pattern
  (testing "Matching against multiple patterns"
    (is (fp/matches-any-pattern? "script.sh" ["*.sh" "*.cgi" "*.bb"])
        "Should match first pattern in list")
    (is (fp/matches-any-pattern? "test.cgi" ["*.sh" "*.cgi" "*.bb"])
        "Should match middle pattern in list")
    (is (fp/matches-any-pattern? "program.bb" ["*.sh" "*.cgi" "*.bb"])
        "Should match last pattern in list")
    (is (not (fp/matches-any-pattern? "script.py" ["*.sh" "*.cgi" "*.bb"]))
        "Should not match if no patterns match")
    (is (fp/matches-any-pattern? "deploy" ["deploy" "*.sh"])
        "Should match exact name in pattern list")
    (is (not (fp/matches-any-pattern? "test.py" []))
        "Empty pattern list should match nothing")))

(deftest test-filename-from-path
  (testing "Extracting filename from paths"
    (is (= "script.sh" (fp/filename-from-path "scripts/script.sh"))
        "Should extract from Unix path")
    (is (= "test.cgi" (fp/filename-from-path "scripts\\test.cgi"))
        "Should extract from Windows path")
    (is (= "deploy" (fp/filename-from-path "bin/deploy"))
        "Should extract filename without extension")
    (is (= "script.sh" (fp/filename-from-path "dir1/dir2/dir3/script.sh"))
        "Should extract from deeply nested path")
    (is (= "script.sh" (fp/filename-from-path "script.sh"))
        "Should handle filename without path")))

(deftest test-should-be-executable
  (testing "Determining if files should be executable"
    (let [patterns ["*.sh" "*.cgi" "*.bb" "deploy"]]
      (is (fp/should-be-executable? "scripts/test.sh" patterns)
          "Should allow .sh file")
      (is (fp/should-be-executable? "cgi-bin/form.cgi" patterns)
          "Should allow .cgi file")
      (is (fp/should-be-executable? "scripts/runner.bb" patterns)
          "Should allow .bb file")
      (is (fp/should-be-executable? "bin/deploy" patterns)
          "Should allow exact match")
      (is (not (fp/should-be-executable? "scripts/malicious.py" patterns))
          "Should reject non-matching extension")
      (is (not (fp/should-be-executable? "scripts/hack.exe" patterns))
          "Should reject unlisted extension")
      (is (not (fp/should-be-executable? "random-script" patterns))
          "Should reject non-matching name"))))

(deftest test-should-be-executable-security
  (testing "Security: safe defaults for edge cases"
    (is (not (fp/should-be-executable? "script.sh" nil))
        "Should reject when patterns is nil (secure by default)")
    (is (not (fp/should-be-executable? "script.sh" []))
        "Should reject when patterns is empty (secure by default)")
    (is (not (fp/should-be-executable? "" ["*.sh"]))
        "Should handle empty filename gracefully")))

(deftest test-should-be-executable-wildcard
  (testing "Wildcard pattern allows everything"
    (is (fp/should-be-executable? "anything.xyz" ["*"])
        "Wildcard should allow any file")
    (is (fp/should-be-executable? "no-extension" ["*"])
        "Wildcard should allow files without extension")
    (is (fp/should-be-executable? "malicious.exe" ["*"])
        "Wildcard allows even dangerous extensions (use with caution!)")))

(deftest test-default-site-config-patterns
  (testing "Default site configuration patterns from site-config-defaults.edn"
    (let [default-patterns ["*.cgi" "*.sh" "*.bb"]]
      (is (fp/should-be-executable? "script.sh" default-patterns)
          "Default should allow shell scripts")
      (is (fp/should-be-executable? "form.cgi" default-patterns)
          "Default should allow CGI scripts")
      (is (fp/should-be-executable? "runner.bb" default-patterns)
          "Default should allow Babashka scripts")
      (is (not (fp/should-be-executable? "script.py" default-patterns))
          "Default should reject Python scripts")
      (is (not (fp/should-be-executable? "script.pl" default-patterns))
          "Default should reject Perl scripts")
      (is (not (fp/should-be-executable? "program.rb" default-patterns))
          "Default should reject Ruby scripts")
      (is (not (fp/should-be-executable? "malware.exe" default-patterns))
          "Default should reject executables"))))

(defn run-all-tests []
  (println "\n🧪 Running File Pattern Validation Tests\n")
  (let [results (run-tests 'lambda-lifters.paparascii.file-patterns-test)]
    (println "\n📊 Test Summary:")
    (println "  Tests run:" (:test results))
    (println "  Assertions:" (:pass results))
    (println "  Failures:" (:fail results))
    (println "  Errors:" (:error results))
    (if (and (zero? (:fail results)) (zero? (:error results)))
      (println "\n✅ All tests passed! File pattern validation is working correctly.")
      (println "\n❌ Some tests failed. Check output above."))
    results))
