package com.pravin.maintenance_app.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.http.HttpMethod;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import java.util.List;

import com.pravin.maintenance_app.security.JwtAuthenticationFilter;
import com.pravin.maintenance_app.security.MustChangePasswordFilter;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthenticationFilter;
        private final MustChangePasswordFilter mustChangePasswordFilter;

        public SecurityConfig(
                JwtAuthenticationFilter jwtAuthenticationFilter,
                MustChangePasswordFilter mustChangePasswordFilter) {
                this.jwtAuthenticationFilter = jwtAuthenticationFilter;
                this.mustChangePasswordFilter = mustChangePasswordFilter;
        }

        @Bean
        public PasswordEncoder passwordEncoder() {
                return new BCryptPasswordEncoder();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration configuration = new CorsConfiguration();
                configuration.setAllowedOriginPatterns(List.of("*"));
                configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                configuration.setAllowedHeaders(List.of("*"));
                configuration.setAllowCredentials(true);
                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", configuration);
                return source;
        }

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

                http
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .csrf(csrf -> csrf.disable())

                                .authorizeHttpRequests(auth -> auth
                                                .requestMatchers(
                                                                "/api/auth/register",
                                                                "/api/auth/login",
                                                                "/v3/api-docs/**",
                                                                "/swagger-ui/**",
                                                                "/swagger-ui.html")
                                                .permitAll()

                                                .requestMatchers(
                                                                "/api/cash-payments/admin/pending",
                                                                "/api/cash-payments/*/verify",
                                                                "/api/cash-payments/*/reject",
                                                                "/api/payments/admin/pending",
                                                                "/api/payments/admin/verified",
                                                                "/api/payments/*/verify",
                                                                "/api/payments/*/reject")
                                                .hasRole("ADMIN")

                                                .requestMatchers(
                                                                HttpMethod.POST,
                                                                "/api/maintenance")
                                                .hasRole("ADMIN")

                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/payment-allocations")
                                                .hasRole("ADMIN")

                                                .requestMatchers(HttpMethod.POST,
                                                                "/api/rooms")
                                                .hasRole("ADMIN")

                                                .requestMatchers(HttpMethod.PATCH,
                                                                "/api/rooms/*/exemption")
                                                .hasRole("ADMIN")
                                                
                                                .requestMatchers(
                                                                "/api/admin/**")
                                                .hasRole("ADMIN")

                                                .anyRequest().authenticated())

                                .exceptionHandling(exception -> exception
                                                .authenticationEntryPoint((request, response, authException) -> {
                                                        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
                                                        response.setContentType("application/json");

                                                        String message = "Authentication required";
                                                        if (request.getAttribute("invalid_jwt") != null) {
                                                                message = "Invalid or expired JWT";
                                                        }

                                                        response.getWriter()
                                                                        .write(String.format("""
                                                                                        {
                                                                                            "status": 401,
                                                                                            "message": "%s"
                                                                                        }
                                                                                        """, message));
                                                })
                                                .accessDeniedHandler((request, response, accessDeniedException) -> {
                                                        response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                                                        response.setContentType("application/json");

                                                        response.getWriter()
                                                                        .write("""
                                                                                        {
                                                                                            "status": 403,
                                                                                            "message": "You are not allowed to access this resource"
                                                                                        }
                                                                                        """);
                                                }))

                                .addFilterBefore(
                                                jwtAuthenticationFilter,
                                                UsernamePasswordAuthenticationFilter.class)
                                .addFilterAfter(
                                                mustChangePasswordFilter,
                                                JwtAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public AuthenticationManager authenticationManager(
                        AuthenticationConfiguration configuration) throws Exception {

                return configuration.getAuthenticationManager();
        }
}