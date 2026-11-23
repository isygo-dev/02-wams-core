package eu.isygoit.ui;

import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.page.AppShellConfigurator;

/**
 * Application shell of the complete core UI: loads the styles specific to this application
 * ({@code frontend/styles/complete.scss}) on top of the shared core styles every module layout already loads.
 */
@CssImport("./styles/complete.scss")
public class CompleteUiAppShell implements AppShellConfigurator {
}
