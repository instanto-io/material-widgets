@skip-jvm
Feature: Material showcase theme on TeaVM
  Scenario: Switch the shell and a table to dark mode and back
    Given the Material showcase page "button" is open
    Then dark theme covers the shell and table and returns to light
