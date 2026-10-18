package com.aiweather.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/** Simple fixed-window limiter per client address (protects the upstream weather quota and admin login). */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RateLimitFilter extends OncePerRequestFilter {

    private record Window(long startMinute, int count) {}

    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();
    private final int limit;

    public RateLimitFilter(AppProperties props) {
        this.limit = props.rateLimit().requestsPerMinute();
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return request.getRequestURI().startsWith("/actuator/health");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        long minute = System.currentTimeMillis() / 60_000;
        if (windows.size() > 10_000) {
            windows.entrySet().removeIf(e -> e.getValue().startMinute() < minute);
        }
        Window w = windows.merge(req.getRemoteAddr(), new Window(minute, 1),
                (old, fresh) -> old.startMinute() == minute ? new Window(minute, old.count() + 1) : fresh);
        if (w.count() > limit) {
            res.setStatus(429);
            res.setHeader("Retry-After", "60");
            res.setContentType("application/json");
            res.getWriter().write("{\"status\":429,\"code\":\"rate_limited\",\"message\":\"Too many requests. Please wait a minute and try again.\"}");
            return;
        }
        chain.doFilter(req, res);
    }
}
