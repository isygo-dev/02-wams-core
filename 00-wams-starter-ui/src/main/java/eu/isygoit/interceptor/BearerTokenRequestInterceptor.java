package eu.isygoit.interceptor;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.server.VaadinSession;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;
import org.springframework.stereotype.Component;

@Component
@Scope(value = "singleton", proxyMode = ScopedProxyMode.TARGET_CLASS)  // allows injection of session-scoped bean
public class BearerTokenRequestInterceptor implements RequestInterceptor {

    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String ACCEPT_LANGUAGE_HEADER = "Accept-Language";
    private static final String BEARER_PREFIX = "Bearer ";

    @Override
    public void apply(RequestTemplate template) {
        UI ui = UI.getCurrent();
        if (ui != null) {
            template.header(ACCEPT_LANGUAGE_HEADER, ui.getLocale().toLanguageTag());
        }

        // Retrieve the token from the current VaadinSession
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            // Not in a Vaadin request context – skip adding the token
            return;
        }

        String token;
        session.lock();
        try {
            token = (String) session.getAttribute("accessToken");
        } finally {
            session.unlock();
        }
        if (token != null && !token.isEmpty()) {
            template.header(AUTHORIZATION_HEADER, BEARER_PREFIX + token);
        }
    }
}