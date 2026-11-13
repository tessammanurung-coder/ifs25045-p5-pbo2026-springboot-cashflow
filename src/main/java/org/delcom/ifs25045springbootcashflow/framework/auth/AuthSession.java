package org.delcom.ifs25045springbootcashflow.framework.auth;

import com.vaadin.flow.server.VaadinRequest;
import com.vaadin.flow.server.VaadinResponse;
import com.vaadin.flow.server.VaadinService;
import com.vaadin.flow.server.VaadinServletRequest;
import com.vaadin.flow.server.VaadinServletResponse;
import com.vaadin.flow.server.VaadinSession;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.usecase.AuthUseCase;
import jakarta.servlet.http.Cookie;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.HexFormat;
import java.util.Optional;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * Sesi login: userId disimpan di VaadinSession dan, agar bertahan setelah refresh / tab baru,
 * ditulis juga ke cookie HttpOnly bertanda tangan HMAC-SHA256 ("id.kedaluwarsa.tandaTangan").
 * LOGGED_OUT_KEY mencegah cookie yang tersisa me-login-kan pengguna lagi setelah logout.
 */
@Component
public class AuthSession {
    public static final String LOGGED_OUT_KEY = "CASHFLOW_LOGGED_OUT";
    public static final String USER_ID_KEY = "CASHFLOW_USER_ID";
    public static final String COOKIE_NAME = "CASHFLOW_AUTH";
    private static final int COOKIE_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;
    private static final String HMAC = "HmacSHA256";

    private final AuthUseCase auth;
    private final byte[] secret;

    public AuthSession(AuthUseCase auth, @Value("${app.session-secret:}") String sessionSecret) {
        this.auth = auth;
        if (sessionSecret == null || sessionSecret.isBlank()) {
            this.secret = new byte[32];
            new SecureRandom().nextBytes(this.secret);
        } else {
            this.secret = sessionSecret.getBytes(StandardCharsets.UTF_8);
        }
    }

    /** Pengguna yang sedang login (dari sesi, atau dari cookie bila sesi baru dan belum logout). */
    public Optional<User> currentUser() {
        VaadinSession session = VaadinSession.getCurrent();
        if (session == null) {
            return Optional.empty();
        }
        Object stored = session.getAttribute(USER_ID_KEY);
        if (stored instanceof Integer id) {
            Optional<User> user = auth.findUser(id);
            if (user.isPresent()) {
                return user;
            }
            session.setAttribute(USER_ID_KEY, null);
        }
        if (Boolean.TRUE.equals(session.getAttribute(LOGGED_OUT_KEY))) {
            return Optional.empty();
        }
        Optional<User> fromCookie = readCookieToken().flatMap(this::verify).flatMap(auth::findUser);
        fromCookie.ifPresent(user -> session.setAttribute(USER_ID_KEY, user.getId()));
        return fromCookie;
    }

    public void login(User user) {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(USER_ID_KEY, user.getId());
        session.setAttribute(LOGGED_OUT_KEY, null);
        long expiry = Instant.now().getEpochSecond() + COOKIE_MAX_AGE_SECONDS;
        writeCookie(sign(user.getId(), expiry), COOKIE_MAX_AGE_SECONDS);
    }

    public void logout() {
        VaadinSession session = VaadinSession.getCurrent();
        session.setAttribute(USER_ID_KEY, null);
        session.setAttribute(LOGGED_OUT_KEY, Boolean.TRUE);
        writeCookie("", 0);
    }

    // ---- cookie ----

    private Optional<String> readCookieToken() {
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (!(request instanceof VaadinServletRequest servletRequest)) {
            return Optional.empty();
        }
        Cookie[] cookies = servletRequest.getHttpServletRequest().getCookies();
        if (cookies == null) {
            return Optional.empty();
        }
        for (Cookie cookie : cookies) {
            if (COOKIE_NAME.equals(cookie.getName()) && !cookie.getValue().isBlank()) {
                return Optional.of(cookie.getValue());
            }
        }
        return Optional.empty();
    }

    private void writeCookie(String value, int maxAge) {
        VaadinResponse response = VaadinService.getCurrentResponse();
        VaadinRequest request = VaadinService.getCurrentRequest();
        if (!(response instanceof VaadinServletResponse servletResponse)) {
            return;
        }
        Cookie cookie = new Cookie(COOKIE_NAME, value);
        cookie.setPath("/");
        cookie.setMaxAge(maxAge);
        cookie.setHttpOnly(true);
        cookie.setAttribute("SameSite", "Lax");
        cookie.setSecure(request instanceof VaadinServletRequest r && r.getHttpServletRequest().isSecure());
        servletResponse.getHttpServletResponse().addCookie(cookie);
    }

    // ---- token ----

    String sign(int userId, long expiryEpochSeconds) {
        String payload = userId + "." + expiryEpochSeconds;
        return payload + "." + hmac(payload);
    }

    /** Mengembalikan userId bila tanda tangan valid dan belum kedaluwarsa. */
    Optional<Integer> verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            return Optional.empty();
        }
        String payload = parts[0] + "." + parts[1];
        boolean validSignature = MessageDigest.isEqual(
                hmac(payload).getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8));
        if (!validSignature) {
            return Optional.empty();
        }
        try {
            long expiry = Long.parseLong(parts[1]);
            if (expiry < Instant.now().getEpochSecond()) {
                return Optional.empty();
            }
            return Optional.of(Integer.valueOf(parts[0]));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }

    private String hmac(String payload) {
        try {
            Mac mac = Mac.getInstance(HMAC);
            mac.init(new SecretKeySpec(secret, HMAC));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (java.security.GeneralSecurityException e) {
            throw new IllegalStateException("HMAC tidak tersedia", e);
        }
    }
}
