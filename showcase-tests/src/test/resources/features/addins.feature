@skip-jvm
Feature: Original Material addins on TeaVM
  Scenario Outline: The <addin> example works after navigation
    Given the Material showcase page "addins-<addin>" is open
    Then the original addin renders and survives remounting

    Examples:
      | addin |
      | autocomplete |
      | avatar |
      | bubble |
      | camera |
      | carousel |
      | circularprogress |
      | combobox |
      | countup |
      | cropper |
      | cutouts |
      | dnd |
      | docviewer |
      | emptystates |
      | fileuploader |
      | iconmorph |
      | inputmask |
      | livestamp |
      | masonry |
      | menubar |
      | overlay |
      | pathanimator |
      | rating |
      | richeditor |
      | scrollfire |
      | signature |
      | splitpanel |
      | steppers |
      | subheaders |
      | swipeable |
      | timepickers |
      | treeview |
      | waterfall |
      | webp |
      | window |
