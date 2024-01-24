package org.craftercms.studio.http;

import com.google.common.net.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.filter.GenericFilterBean;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

public class SameSiteCookiesFilter extends GenericFilterBean {

    private final String SESSION_COOKIE_NAME = "JSESSIONID";

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        Cookie[] cookies = ((HttpServletRequest) request).getCookies();
        if (cookies != null && cookies.length > 0) {
            List<Cookie> cookieList = Arrays.asList(cookies);
            cookieList.stream()
                    .filter(cookie -> SESSION_COOKIE_NAME.equals(cookie.getName())).findFirst().ifPresent(sessionCookie -> {
                        resp.setHeader(HttpHeaders.SET_COOKIE, getSameSiteCookie(sessionCookie, ((HttpServletRequest) request).getContextPath()).toString());
                    });
        }
        chain.doFilter(request, response);
    }

    private ResponseCookie getSameSiteCookie(Cookie cookie, String contextPath) {
        return ResponseCookie.from(cookie.getName(), cookie.getValue())
                .sameSite("strict")
                .secure(true)
                .path(contextPath)
                .maxAge(cookie.getMaxAge())
                .httpOnly(true)
                .build();
    }
}
