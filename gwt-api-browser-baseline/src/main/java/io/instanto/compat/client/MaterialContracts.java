package io.instanto.compat.client;

import com.google.gwt.core.client.Callback;
import com.google.gwt.core.client.EntryPoint;
import com.google.gwt.dom.client.Document;
import com.google.gwt.user.client.Window;
import io.instanto.gwt.testing.api.MaterialBrowserContracts;
import io.instanto.gwt.testing.api.MaterialChoiceContracts;
import io.instanto.gwt.testing.api.MaterialDomContracts;
import io.instanto.gwt.testing.api.MaterialEventContracts;
import io.instanto.gwt.testing.api.MaterialGwtContracts;
import io.instanto.gwt.testing.api.MaterialResourceContracts;

public class MaterialContracts implements EntryPoint {
  public void onModuleLoad() {
    Callback<Void, Throwable> completion =
        new Callback<Void, Throwable>() {
          @Override
          public void onSuccess(Void value) {
            Document.get().getBody().setAttribute("data-contract", "passed");
          }

          @Override
          public void onFailure(Throwable failure) {
            Document.get().getBody().setAttribute("data-contract-error", failure.toString());
            throw new RuntimeException(failure);
          }
        };
    switch (Window.Location.getParameter("contract")) {
      case "table-numbers":
        io.instanto.gwt.testing.api.MaterialTableContracts
            .numberPatternsPreserveValuesAndPrecision();
        break;
      case "table-dom":
        io.instanto.gwt.testing.api.MaterialTableContracts.tableRowsExposeLiveTypedCells();
        break;
      case "textfield-initialisation":
        io.instanto.gwt.testing.api.MaterialTextFieldContracts
            .initialisationHandlersKeepSourceAndCanBeRemoved();
        break;
      case "textfield-time-format":
        io.instanto.gwt.testing.api.MaterialTextFieldContracts
            .sharedAndClientTimeFormatsRoundTrip();
        break;
      case "textfield-keys":
        io.instanto.gwt.testing.api.MaterialTextFieldContracts
            .nativeKeyEventsPreserveModifiersAndRelativeElement();
        break;
      case "textfield-suggestions":
        io.instanto.gwt.testing.api.MaterialTextFieldContracts
            .suggestionsUseFactoriesAndDeliverSelectionWithoutValueChange();
        break;
      case "panel-order":
        MaterialDomContracts.panelsLoadAfterTheirChildrenAndUnloadBeforeThem();
        MaterialDomContracts.panelInsertionUsesContainerElementsAfterPluginWrapping();
        MaterialDomContracts.elementTextIgnoresPresentationAndPreservesLineBreaks();
        break;
      case "image-events":
        MaterialResourceContracts.imageLoadAndErrorHandlersFollowBrowserEvents(completion);
        return;
      case "browser-scroll":
        MaterialBrowserContracts.windowScrollEventsReportPositionAndCanBeRemoved(completion);
        return;
      case "browser-location":
        MaterialBrowserContracts.locationAndNavigatorReadTheHostWindow();
        MaterialBrowserContracts.sameDocumentNavigationCompletes(completion);
        return;
      case "browser-storage":
        MaterialBrowserContracts.storageSupportMatchesAvailableStores();
        break;
      case "browser-metadata":
        MaterialBrowserContracts.metadataAndFieldsetsUseNativeElements();
        break;
      case "browser-geometry":
        MaterialBrowserContracts.widgetCoordinatesAccountForScrolledContainers();
        break;
      case "suggestion-constructor":
        MaterialBrowserContracts.suggestionSubclassesCanUseTheDefaultConstructor();
        break;
      case "stylesheet-deferred":
        MaterialResourceContracts.queuedStylesFlushAutomatically(completion);
        return;
      case "script-load":
        MaterialResourceContracts.externalScriptCompletesAfterExecution(completion);
        return;
      case "script-failure":
        MaterialResourceContracts.failedScriptReportsDownloadException(completion);
        return;

      case "document-query":
        MaterialResourceContracts.documentQueriesRemainLive();
        break;
      case "stylesheet-order":
        MaterialResourceContracts.stylesheetQueuesFlushInGwtOrder();
        break;
      case "inline-script":
        MaterialResourceContracts.inlineScriptsExecuteImmediatelyAndRespectRemoval();
        break;
      case "script-nonce":
        MaterialResourceContracts.scriptNoncesPropagateFromTheTargetDocument();
        break;
      case "button-safe-html":
        MaterialChoiceContracts.buttonSafeHtmlUsesSubclassRendering();
        break;
      case "list-selection":
        MaterialChoiceContracts.listInsertionAndSelectionPreserveTextAndValues();
        break;
      case "list-direction":
        MaterialChoiceContracts.listDirectionWrappingDoesNotLeakIntoPublicText();
        break;
      case "label-direction":
        MaterialChoiceContracts.directionalLabelsRestoreContextAndEscapePlainText();
        break;
      case "radio-labels":
        MaterialChoiceContracts.radioConstructorsPreserveSafeHtmlAndDirection();
        break;
      case "suppression":
        MaterialEventContracts.throwableConstructorsInitializeSuppression();
        break;
      case "resources":
        MaterialDomContracts.typedResourceElementsRetainNativeProperties();
        break;
      case "options":
        MaterialDomContracts.optionViewsTrackInsertionSelectionAndDisabledState();
        break;
      case "attach":
        MaterialDomContracts.attachHandlerInterfaceReportsWidgetLifecycle();
        break;
      case "event-sources":
        MaterialEventContracts.eventSourcesFilterHandlersAndPreserveOrder();
        break;
      case "event-mutation":
        MaterialEventContracts.handlerChangesWaitForNestedDispatchToFinish();
        break;
      case "event-errors":
        MaterialEventContracts.handlerFailuresAreCollectedAfterAllHandlersRun();
        break;
      case "sanitization":
        MaterialEventContracts.sanitizationAllowsOnlyUpstreamMarkupSubset();
        break;
      case "values":
        MaterialGwtContracts.typedValuesReportErrorsAndRecover();
        break;
      case "direction":
        MaterialGwtContracts.textDirectionFollowsContentAndCanBeDisabled();
        break;
      case "selection":
        MaterialGwtContracts.inputSelectionTracksCursorAndRejectsInvalidRanges();
        break;
      case "dates":
        MaterialGwtContracts.javascriptDatesPreserveNativeRolloverAndInvalidValues();
        break;
      case "styles":
        MaterialGwtContracts.stylePropertiesRetainValuesAndClearIndependently();
        break;
      case "children":
        MaterialGwtContracts.mixedChildTraversalAndRemovalPreserveTheDom();
        break;
      case "input":
        MaterialGwtContracts.textBoxVisibleLengthUsesTheNativeInputSize();
        break;
      case "lifecycle":
        MaterialGwtContracts.openAndCloseEventsRetainTargetSourceAndDisposal();
        break;
      default:
        throw new IllegalArgumentException("Unknown contract");
    }
    Document.get().getBody().setAttribute("data-contract", "passed");
  }
}
