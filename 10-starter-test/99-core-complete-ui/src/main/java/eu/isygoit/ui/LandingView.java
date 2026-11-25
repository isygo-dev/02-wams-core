package eu.isygoit.ui;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Image;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.Route;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.landing.BaseLandingView;
import jakarta.annotation.security.PermitAll;

import java.util.List;

/**
 * Landing page implementation that displays all available modules.
 *
 * <p>This view extends {@link BaseLandingView} and provides the list of
 * modules to display. Each module is automatically rendered as a card
 * with its icon, title, description, features, and an enter button.</p>
 *
 * <p>To add a new module, add it to {@link #getModules()}. The landing page
 * will automatically include it with proper styling and i18n support.</p>
 *
 * <p>Each call to {@link #getModules()} creates fresh Vaadin components.
 * Vaadin components must not be stored in a static collection or shared
 * between component trees / UI instances.</p>
 *
 * <p>i18n keys used for each module (where {@code <key>} is the module key):
 * <ul>
 *   <li>{@code common.landing.<key>.title} - Module title</li>
 *   <li>{@code common.landing.<key>.description} - Module description</li>
 *   <li>{@code common.landing.<key>.feature.1} - First feature</li>
 *   <li>{@code common.landing.<key>.feature.2} - Second feature</li>
 *   <li>{@code common.landing.<key>.feature.3} - Optional third feature</li>
 *   <li>{@code common.landing.<key>.button} - Button text</li>
 * </ul>
 * </p>
 */
@Route("landing")
@PermitAll
public class LandingView extends BaseLandingView {

    /**
     * Returns all modules displayed on the landing page.
     *
     * <p>This method can be invoked by the {@link BaseLandingView}
     * constructor, before this subclass has initialized its instance fields.
     * Therefore, it deliberately relies only on local values and creates
     * new icon components on every invocation.</p>
     *
     * @return fresh module definitions with fresh Vaadin icon components
     */
    @Override
    protected List<ModuleInfo> getModules() {
        return List.of(
                new ModuleInfo("KMS", "kms", VaadinIcon.KEY.create()),
                new ModuleInfo("IMS", "ims", VaadinIcon.USERS.create()),
                new ModuleInfo("MMS", "mms", VaadinIcon.ENVELOPE.create()),
                new ModuleInfo("DMS", "dms", VaadinIcon.FILE_O.create()),
                new ModuleInfo("SMS", "sms", VaadinIcon.DATABASE.create()),
                new ModuleInfo("CMS", "cms", VaadinIcon.CALENDAR.create()),
                new ModuleInfo("PLAN", "plan", VaadinIcon.TREE_TABLE.create())

                /*
                 * Custom image examples:
                 *
                 * new ModuleInfo(
                 *     "EDYRA Pilot",
                 *     "edyra-pilot",
                 *     createModuleImage("icons/edyra-pilot.svg", "EDYRA Pilot")
                 * ),
                 * new ModuleInfo(
                 *     "Custom PNG",
                 *     "custom",
                 *     createModuleImage("icons/custom.png", "Custom module")
                 * )
                 */
        );
    }

    @Override
    public String getPageTitle() {
        return I18n.t("common.landing.page.title");
    }
}
