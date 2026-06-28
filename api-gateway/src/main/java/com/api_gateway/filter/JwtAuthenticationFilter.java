package com.api_gateway.filter;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import java.io.IOException;
import java.util.*;

@Component
public class JwtAuthenticationFilter implements Filter {

    @Value("${jwt.secret:404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970}")
    private String secret;

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;

        String path = httpRequest.getRequestURI();

        // Skip JWT validation for OPTIONS preflight requests
        if ("OPTIONS".equalsIgnoreCase(httpRequest.getMethod())) {
            chain.doFilter(request, response);
            return;
        }

        // Skip JWT validation for public paths
        if (path.startsWith("/api/auth/login") || path.startsWith("/api/auth/managers") || path.startsWith("/actuator")) {
            chain.doFilter(request, response);
            return;
        }

        String authHeader = httpRequest.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Missing or invalid Authorization header\"}");
            return;
        }

        String token = authHeader.substring(7);
        try {
            Algorithm algorithm = Algorithm.HMAC256(secret);
            DecodedJWT jwt = JWT.require(algorithm).build().verify(token);

            Long userId = jwt.getClaim("id").asLong();
            String username = jwt.getSubject();
            String role = jwt.getClaim("role").asString();
            Long managerId = jwt.getClaim("managerId").asLong();
            String kycStatus = jwt.getClaim("kycStatus").asString();
            Long hrId = jwt.getClaim("hrId").asLong();

            // Wrap request to add headers
            HeaderMapRequestWrapper requestWrapper = new HeaderMapRequestWrapper(httpRequest);
            if (userId != null) requestWrapper.addHeader("X-User-Id", String.valueOf(userId));
            if (username != null) requestWrapper.addHeader("X-User-Name", username);
            if (role != null) requestWrapper.addHeader("X-User-Role", role);
            if (managerId != null) requestWrapper.addHeader("X-User-Manager-Id", String.valueOf(managerId));
            if (kycStatus != null) requestWrapper.addHeader("X-User-Kyc-Status", kycStatus);
            if (hrId != null) requestWrapper.addHeader("X-User-Hr-Id", String.valueOf(hrId));

            chain.doFilter(requestWrapper, response);
        } catch (Exception e) {
            httpResponse.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{\"error\": \"Invalid or expired token\"}");
        }
    }

    public static class HeaderMapRequestWrapper extends HttpServletRequestWrapper {
        private final Map<String, String> headerMap = new HashMap<>();

        public HeaderMapRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        public void addHeader(String name, String value) {
            headerMap.put(name, value);
        }

        @Override
        public String getHeader(String name) {
            String headerValue = headerMap.get(name);
            if (headerValue != null) {
                return headerValue;
            }
            return super.getHeader(name);
        }

        @Override
        public Enumeration<String> getHeaderNames() {
            List<String> names = Collections.list(super.getHeaderNames());
            names.addAll(headerMap.keySet());
            return Collections.enumeration(names);
        }

        @Override
        public Enumeration<String> getHeaders(String name) {
            List<String> values = Collections.list(super.getHeaders(name));
            if (headerMap.containsKey(name)) {
                values = new ArrayList<>(values);
                values.add(headerMap.get(name));
            }
            return Collections.enumeration(values);
        }
    }
}
