package com.pravin.maintenance_app.security;

import com.pravin.maintenance_app.entity.User;
import com.pravin.maintenance_app.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class MustChangePasswordFilterTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private HttpServletRequest request;

    @Mock
    private HttpServletResponse response;

    @Mock
    private FilterChain filterChain;

    @InjectMocks
    private MustChangePasswordFilter filter;

    @BeforeEach
    void setUp() {
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void testOptionsRequest_BypassesFilter() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("OPTIONS");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testAuthEndpoints_BypassesFilter() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/auth/change-password");
        when(request.getContextPath()).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testSwaggerEndpoints_BypassesFilter() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/v3/api-docs");
        when(request.getContextPath()).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testUnauthenticatedRequest_BypassesFilter() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users/me/dashboard");
        when(request.getContextPath()).thenReturn("");

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    void testAuthenticatedUser_MustChangePasswordFalse_Proceeds() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users/me/dashboard");
        when(request.getContextPath()).thenReturn("");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "9876543210",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setMobileNumber("9876543210");
        user.setMustChangePassword(false);

        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(user));

        filter.doFilterInternal(request, response, filterChain);

        verify(filterChain).doFilter(request, response);
    }

    @Test
    void testAuthenticatedUser_MustChangePasswordTrue_BlockedWith403() throws ServletException, IOException {
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/users/me/dashboard");
        when(request.getContextPath()).thenReturn("");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "9876543210",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setMobileNumber("9876543210");
        user.setMustChangePassword(true);

        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(user));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);

        assertTrue(stringWriter.toString().contains("Password change required before accessing the application"));
    }

    @Test
    void testAuthenticatedUser_MustChangePasswordTrue_ProtectedPaymentEndpoint_BlockedWith403()
            throws ServletException, IOException {
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/api/payments");
        when(request.getContextPath()).thenReturn("");

        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                "9876543210",
                null,
                List.of(new SimpleGrantedAuthority("ROLE_USER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        User user = new User();
        user.setMobileNumber("9876543210");
        user.setMustChangePassword(true);

        when(userRepository.findByMobileNumber("9876543210")).thenReturn(Optional.of(user));

        StringWriter stringWriter = new StringWriter();
        PrintWriter printWriter = new PrintWriter(stringWriter);
        when(response.getWriter()).thenReturn(printWriter);

        filter.doFilterInternal(request, response, filterChain);

        verify(response).setStatus(HttpServletResponse.SC_FORBIDDEN);
        verify(response).setContentType("application/json");
        verify(filterChain, never()).doFilter(request, response);

        assertTrue(stringWriter.toString().contains("Password change required before accessing the application"));
    }
}
