package com.sep.educonnect.configuration;

import com.sep.educonnect.service.I18nService;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter;
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.LogoutSuccessHandler;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.io.IOException;
import java.util.List;

@Slf4j
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {
  private final String[] PUBLIC_ENDPOINTS = {
    "/api/users",
    "/api/auth/token",
    "/api/auth/introspect",
    "/api/auth/logout",
    "/api/auth/refresh",
    "/api/auth/forgot-password",
    "/api/auth/reset-password",
    "/api/auth/verification/**",
    "/v3/api-docs/**",
    "/swagger-ui/**",
    "/api/files/**",
    "/api/test-i18n/**",
    "api/search/tutors",
    "/api/search/tutors/*",
    "/api/logs/interaction",
    "/api/syllabus/and-subject",
    "/api/video-lessons/update-status",
    "/api/course",
    "/api/course/{id}",
    "/api/course/by-tutor",
    "/api/students/tutor-profile",
    "/api/booking/{courseId}",
    "/api/course-reviews/course/{courseId}/**",
    "/api/course/{courseId}",
    "/ws/**",
    "/api/wishlist/{courseId}/exists",
    "/api/top/**",
    "/api/payments/return",
    "/actuator/health",
    "/actuator/prometheus",
    "/api/students/schedule/{tutorId}",
    "api/ratings/tutor/**"
  };

  @Autowired private CustomJwtDecoder customJwtDecoder;

  @Bean
  public JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint(I18nService i18nService) {
    return new JwtAuthenticationEntryPoint(i18nService);
  }

  @Bean
  public SecurityFilterChain filterChain(
      HttpSecurity httpSecurity, JwtAuthenticationEntryPoint jwtAuthenticationEntryPoint)
      throws Exception {
    log.info("Configuring security filter chain");

    httpSecurity.authorizeHttpRequests(
        request -> {
          log.debug("Configuring authorization rules");
          request.requestMatchers(PUBLIC_ENDPOINTS).permitAll().anyRequest().authenticated();
        });

    httpSecurity.oauth2ResourceServer(
        oauth2 -> {
          log.debug("Configuring OAuth2 resource server with JWT");
          oauth2
              .jwt(
                  jwtConfigurer ->
                      jwtConfigurer
                          .decoder(customJwtDecoder)
                          .jwtAuthenticationConverter(jwtAuthenticationConverter()))
              .authenticationEntryPoint(jwtAuthenticationEntryPoint);
        });

    httpSecurity.cors(
        cors -> {
          log.debug("Configuring CORS");
          cors.configurationSource(corsConfigurationSource());
        });

    httpSecurity.csrf(AbstractHttpConfigurer::disable);

    // Thêm logging cho logout events
    httpSecurity.logout(
        logout -> {
          logout
              .logoutUrl("/api/auth/logout")
              .logoutSuccessHandler(logoutSuccessHandler())
              .invalidateHttpSession(true)
              .clearAuthentication(true);
          log.debug("Configuring logout handler");
        });

    log.info("Security filter chain configuration completed");
    return httpSecurity.build();
  }

  @Bean
  public CorsConfigurationSource corsConfigurationSource() {
    CorsConfiguration configuration = new CorsConfiguration();
    configuration.setAllowedOrigins(
        List.of(
            "https://educonnect.dev",
            "http://139.59.97.252",
            "http://localhost:5173",
            "http://localhost:3000",
            "http://localhost:8080",
            "https://api.educonnect.dev"));
    configuration.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
    configuration.setAllowedHeaders(List.of("*"));
    configuration.setAllowCredentials(true);

    UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
    source.registerCorsConfiguration("/**", configuration);
    return source;
  }

  @Bean
  JwtAuthenticationConverter jwtAuthenticationConverter() {
    JwtGrantedAuthoritiesConverter jwtGrantedAuthoritiesConverter =
        new JwtGrantedAuthoritiesConverter();
    jwtGrantedAuthoritiesConverter.setAuthorityPrefix("");

    JwtAuthenticationConverter jwtAuthenticationConverter = new JwtAuthenticationConverter();
    jwtAuthenticationConverter.setJwtGrantedAuthoritiesConverter(jwtGrantedAuthoritiesConverter);

    return jwtAuthenticationConverter;
  }

  @Bean
  PasswordEncoder passwordEncoder() {
    log.debug("Configuring BCrypt password encoder");
    return new BCryptPasswordEncoder(10);
  }

  @Bean
  public LogoutSuccessHandler logoutSuccessHandler() {
    return (request, response, authentication) -> {
      log.info("User logged out successfully from IP: {}", request.getRemoteAddr());
      response.setStatus(HttpServletResponse.SC_OK);
      response.getWriter().write("{\"message\":\"Logout successful\"}");
    };
  }
}
