@skip-jvm
Feature: Original Material tables on TeaVM
  Background:
    Given the Material showcase page "button" is open

  Scenario Outline: The <table> table renders after navigation and remounting
    Then the original "<table>" table renders after remounting
    Examples:
      | table |
      | standard |
      | paged |
      | categorized |
      | frozen |
      | customized |
      | infinite |

  Scenario: Expand and restore a table
    Then expansion fills the viewport and restores page scrolling

  Scenario: Page through every record
    Then the pager reaches the last row and returns to the first

  Scenario: Navigate pages with the keyboard
    Then the original pager responds to keyboard controls

  Scenario: Sort and edit table rows
    Then sorting and row controls update the standard table

  Scenario: Hide and show a column
    Then the first column can be hidden and restored

  Scenario: Select a row and change its density
    Then selection and density use the original option handlers

  Scenario: Open and close categorized rows
    Then categorized rows can be opened and closed

  Scenario: Load the next window of infinite rows
    Then scrolling loads another bounded row window
