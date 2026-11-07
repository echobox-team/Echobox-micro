package com.EchoBox.config;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

/**
 * Adds X-Served-By-Pod header to every response and exposes /notifications/instance
 * to demonstrate Horizontal Scalability and Load Balancing in Amazon EKS.
 */
@Component
@RestController
public class InstanceController extends OncePerRequestFilter {

    private String getPodName() {
        String hostname = System.getenv("HOSTNAME");
        if (hostname != null && !hostname.isBlank()) {
            return hostname;
        }
        try {
            return InetAddress.getLocalHost().getHostName();
        } catch (Exception e) {
            return "ms-notification-local";
        }
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        response.addHeader("X-Served-By-Pod", getPodName());
        filterChain.doFilter(request, response);
    }

    @GetMapping("/notifications/instance")
    public Map<String, String> getInstanceInfo() {
        return Map.of(
                "service", "ms-notification",
                "language", "Java 21 (Spring Boot 3.5)",
                "broker", "RabbitMQ (echobox.notifications)",
                "podName", getPodName()
        );
    }
}
