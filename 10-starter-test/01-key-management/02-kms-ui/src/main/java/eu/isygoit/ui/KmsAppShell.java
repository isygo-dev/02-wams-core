package eu.isygoit.ui;

import com.vaadin.flow.component.page.AppShellConfigurator;
import com.vaadin.flow.component.page.Push;
import com.vaadin.flow.component.page.Viewport;
import com.vaadin.flow.server.PWA;

@PWA(
        name = "KMS",
        shortName = "KMS",
        description = "Key management system",
        themeColor = "#1676f3",
        backgroundColor = "#ffffff",
        iconPath = "icons/icon-512.png",
        offlinePath = "offline.html"
)
@Viewport("width=device-width, initial-scale=1.0")
@Push
public class KmsAppShell implements AppShellConfigurator {
}
