package eu.isygoit.security;

import eu.isygoit.helper.UrlHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.logout.LogoutHandler;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

/**
 * The type Logout service.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class LogoutService implements LogoutHandler {


    @Override
    public void logout(
            HttpServletRequest request,
            HttpServletResponse response,
            Authentication authentication
    ) {
        final String authHeader = UrlHelper.getJwtFromRequest(request);
        final String jwt;
        if (!StringUtils.hasText(authHeader) || !authHeader.startsWith("Bearer ")) {
            return;
        }
        jwt = authHeader.substring(7);
    }
}
