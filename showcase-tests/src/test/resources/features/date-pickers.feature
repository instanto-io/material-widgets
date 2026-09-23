@skip-jvm
Feature: Material date pickers on TeaVM
  Background:
    Given the Material showcase page "datePicker" is open

  Scenario: Select a date from every picker after remounting
    Then each date picker selects a value after remounting

  Scenario: Deliver a selected date to the original handler
    Then the selected date reaches the original value handler
