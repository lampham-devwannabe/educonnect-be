package com.sep.educonnect.util;

import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MockHelper {
    public static void mockSecurityContext(String username) {
        Authentication authentication = mock(Authentication.class);
        Mockito.lenient().when(authentication.getName()).thenReturn(username);

        SecurityContext securityContext = mock(SecurityContext.class);
        Mockito.lenient().when(securityContext.getAuthentication()).thenReturn(authentication);

        SecurityContextHolder.setContext(securityContext);
    }

    public static void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    public static <T> ArgumentCaptor<T> argumentCaptor(Class<T> clazz) {
        return ArgumentCaptor.forClass(clazz);
    }

    public static void verifyTimes(Object mock, int times, org.mockito.verification.VerificationMode mode) {
        Mockito.verify(mock, mode);
    }
}
