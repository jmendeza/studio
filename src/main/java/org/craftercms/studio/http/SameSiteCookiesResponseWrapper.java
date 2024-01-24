package org.craftercms.studio.http;

import org.springframework.http.ResponseCookie;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

public class SameSiteCookiesResponseWrapper extends HttpServletResponseWrapper {
    /**
     * Constructs a response adaptor wrapping the given response.
     *
     * @param response the {@link HttpServletResponse} to be wrapped.
     * @throws IllegalArgumentException if the response is null
     */
    public SameSiteCookiesResponseWrapper(HttpServletResponse response) {
        super(response);
    }

    @Override
    public void addCookie(Cookie cookie) {
        ResponseCookie responseCookie = ResponseCookie.from(cookie.getName(), cookie.getValue())
                .sameSite("strictt")
                .secure(cookie.getSecure())
                .path(cookie.getPath())
                .maxAge(cookie.getMaxAge())
                .httpOnly(cookie.isHttpOnly())
                .build();
        addHeader("Set-Cookie", responseCookie.toString());
    }
}
