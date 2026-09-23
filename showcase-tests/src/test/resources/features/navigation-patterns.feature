@skip-jvm
Feature: Full-page Material navigation patterns on TeaVM
  Scenario Outline: The <pattern> pattern uses local resources
    Given the Material navigation pattern "<pattern>" is open
    Then the pattern renders with local navigation and images

    Examples:
      | pattern |
      | navbar_default |
      | navbar_fixed |
      | navbar_tall |
      | navbar_extend |
      | navbar_tab |
      | navbar_shrink |
      | navbar_tab_push |
      | sidenav_fixed |
      | sidenav_drawer |
      | sidenav_drawer_header |
      | sidenav_push |
      | sidenav_push_header |
      | sidenav_card |
      | sidenav_mini |
      | sidenav_mini_expandable |
      | sidenav_edge |
      | sidenav_colaps |
      | sidenav_content |
