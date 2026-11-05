package eu.isygoit.ui.ims.views.profile;

import com.vaadin.flow.router.Route;
import com.vaadin.flow.spring.annotation.SpringComponent;
import com.vaadin.flow.spring.annotation.RouteScope;
import eu.isygoit.remote.ims.ProfileService;
import eu.isygoit.ui.common.profile.ProfilePageView;
import eu.isygoit.ui.ims.layout.ImsMainLayout;
import jakarta.annotation.security.PermitAll;

@SpringComponent
@RouteScope
@Route(value = "profile", layout = ImsMainLayout.class)
@PermitAll
public class ImsProfilePageView extends ProfilePageView {

    public ImsProfilePageView(ProfileService profileService) {
        super(profileService);
    }
}
