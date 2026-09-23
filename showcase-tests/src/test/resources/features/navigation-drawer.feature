@skip-jvm
Feature: Material side navigation on TeaVM
  Scenario: Reopen the drawer and dismiss it with the overlay
    Given the Material navigation pattern "sidenav_drawer" is open
    Then the drawer reopens and its overlay closes it
