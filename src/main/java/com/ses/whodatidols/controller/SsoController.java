package com.ses.whodatidols.controller;

import com.ses.whodatidols.repository.PersonRepository;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller
public class SsoController {

    private final PersonRepository personRepository;

    public SsoController(PersonRepository personRepository) {
        this.personRepository = personRepository;
    }

    /**
     * Cross-domain SSO Login endpoint.
     * Consumes an SSO Ticket issued by bldizi.com, creates a whodatidols session,
     * writes the wdiAuth cookie, and redirects to the target page.
     */
    @GetMapping("/sso-login")
    public ResponseEntity<Void> ssoLogin(
            @RequestParam("ticket") String ticket,
            @RequestParam(value = "redirect", defaultValue = "/") String redirect,
            HttpServletResponse response) {

        String safeRedirect = sanitizeRedirect(redirect);
        if (ticket == null || ticket.isBlank()) {
            return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(safeRedirect)).build();
        }

        try {
            Map<String, Object> result = personRepository.consumeSsoTicket(ticket.trim(), "whodatidols");
            Object successObj = result.get("success");
            boolean isSuccess = Boolean.TRUE.equals(successObj) ||
                    (successObj instanceof Number && ((Number) successObj).intValue() == 1);

            if (isSuccess && result.get("cookie") != null) {
                String cookieVal = result.get("cookie").toString();

                Cookie authCookie = new Cookie("wdiAuth", cookieVal);
                authCookie.setHttpOnly(true);
                authCookie.setPath("/");
                authCookie.setMaxAge(7 * 24 * 60 * 60);
                authCookie.setSecure(true);
                response.addCookie(authCookie);

                ResponseCookie springCookie = ResponseCookie.from("wdiAuth", cookieVal)
                        .httpOnly(true)
                        .path("/")
                        .maxAge(7 * 24 * 60 * 60)
                        .secure(true)
                        .sameSite("Lax")
                        .build();
                response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());
            }
        } catch (Exception e) {
            System.err.println("SSO Login error: " + e.getMessage());
        }

        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(safeRedirect)).build();
    }

    /**
     * Outbound SSO sync redirect:
     * When user on whodatidols wants to go to bldizi with active login state.
     */
    @GetMapping("/sso-sync")
    public ResponseEntity<Void> ssoSync(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestParam(value = "target", defaultValue = "bldizi") String target,
            @RequestParam(value = "redirect", defaultValue = "/") String redirect) {

        String targetBase = "https://bldizi.com";
        String encodedRedirect = URLEncoder.encode(redirect, StandardCharsets.UTF_8);

        if (cookie != null && !cookie.isBlank()) {
            try {
                String ticket = personRepository.createSsoTicket(cookie.trim(), target);
                if (ticket != null) {
                    return ResponseEntity.status(HttpStatus.FOUND)
                            .location(URI.create(targetBase + "/sso-login?ticket=" + ticket + "&redirect=" + encodedRedirect))
                            .build();
                }
            } catch (Exception ignored) {
            }
        }

        String fallbackUrl = targetBase + (redirect.startsWith("/") ? redirect : "/" + redirect);
        return ResponseEntity.status(HttpStatus.FOUND).location(URI.create(fallbackUrl)).build();
    }

    /**
     * REST API to generate an SSO Ticket.
     */
    @GetMapping("/api/sso/ticket")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> getSsoTicket(
            @CookieValue(name = "wdiAuth", required = false) String cookie,
            @RequestParam(value = "target", defaultValue = "bldizi") String target) {

        Map<String, Object> resp = new HashMap<>();
        if (cookie == null || cookie.isBlank()) {
            resp.put("success", false);
            resp.put("message", "Aktif oturum bulunamadı");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(resp);
        }

        String ticket = personRepository.createSsoTicket(cookie.trim(), target);
        if (ticket != null) {
            resp.put("success", true);
            resp.put("ticket", ticket);
            resp.put("target", target);
            return ResponseEntity.ok(resp);
        } else {
            resp.put("success", false);
            resp.put("message", "Bilet oluşturulamadı veya oturum geçersiz");
            return ResponseEntity.badRequest().body(resp);
        }
    }

    /**
     * REST API to exchange an SSO Ticket for a session cookie.
     */
    @PostMapping("/api/sso/exchange")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> exchangeSsoTicket(
            @RequestBody Map<String, String> body,
            HttpServletResponse response) {

        String ticket = body != null ? body.get("ticket") : null;
        if (ticket == null || ticket.isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Bilet parametresi zorunludur"));
        }

        Map<String, Object> result = personRepository.consumeSsoTicket(ticket.trim(), "whodatidols");
        Object successObj = result.get("success");
        boolean isSuccess = Boolean.TRUE.equals(successObj) ||
                (successObj instanceof Number && ((Number) successObj).intValue() == 1);

        if (isSuccess && result.get("cookie") != null) {
            String cookieVal = result.get("cookie").toString();

            Cookie authCookie = new Cookie("wdiAuth", cookieVal);
            authCookie.setHttpOnly(true);
            authCookie.setPath("/");
            authCookie.setMaxAge(7 * 24 * 60 * 60);
            authCookie.setSecure(true);
            response.addCookie(authCookie);

            ResponseCookie springCookie = ResponseCookie.from("wdiAuth", cookieVal)
                    .httpOnly(true)
                    .path("/")
                    .maxAge(7 * 24 * 60 * 60)
                    .secure(true)
                    .sameSite("Lax")
                    .build();
            response.addHeader(HttpHeaders.SET_COOKIE, springCookie.toString());

            return ResponseEntity.ok(result);
        } else {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(result);
        }
    }

    private String sanitizeRedirect(String redirect) {
        if (redirect == null || redirect.isBlank() || !redirect.startsWith("/") || redirect.startsWith("//")) {
            return "/";
        }
        return redirect;
    }
}
