package eu.isygoit.ui;

import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
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
 * <p>To add a new module, simply add it to the {@link #MODULES} list below.
 * The landing page will automatically include it with proper styling and
 * i18n support.</p>
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
@PageTitle("Platform - Choose Your Area")
@PermitAll
public class LandingView extends BaseLandingView implements BeforeEnterObserver {

    /**
     * Single source of truth for all modules.
     * Add new modules here and they'll automatically appear on the landing page.
     */
    private static final List<ModuleInfo> MODULES = List.of(
            new ModuleInfo("KMS", "kms", VaadinIcon.KEY),
            new ModuleInfo("IMS", "ims", VaadinIcon.USERS),
            new ModuleInfo("MMS", "mms", VaadinIcon.ENVELOPE),
            new ModuleInfo("DMS", "dms", VaadinIcon.FILE_O),
            new ModuleInfo("SMS", "sms", VaadinIcon.DATABASE),
            new ModuleInfo("CMS", "cms", VaadinIcon.CALENDAR)
    );

    @Override
    protected List<ModuleInfo> getModules() {
        return MODULES;
    }
}