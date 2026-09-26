@skip-jvm
Feature: Material addin interactions on TeaVM
  Background:
    Given the Material showcase page "button" is open

  Scenario: Crop a local image
    Then the cropper exports its local image

  Scenario: Queue a local file without a service
    Then the selected file stays in the local queue

  Scenario: Leave camera before permission resolves
    Then a late camera stream is stopped after leaving

  Scenario: Set and read rich text
    Then the editor sets and reads the supplied HTML

  Scenario: Clear and insert text in the rich editor
    Then the editor clears and inserts text through its original controls

  Scenario: Publish the selected rating
    Then the rating publishes its value event

  Scenario: Draw, export and clear a signature
    Then a signature can be exported and cleared

  Scenario: Complete the stepper
    Then the stepper completes

  Scenario: Open and close a floating window
    Then the window opens and closes through its original controls

  Scenario: Navigate the carousel with its method buttons
    Then the carousel moves to the requested slide

  Scenario: Expand, collapse and select a tree item
    Then the tree controls reveal and select its original items
