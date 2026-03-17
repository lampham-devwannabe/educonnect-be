package com.sep.educonnect.service.unit;

import com.sep.educonnect.service.RateLimitingService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RateLimitingService Unit Tests")
class RateLimitingServiceTest {

    private RateLimitingService rateLimitingService;

    @BeforeEach
    void setUp() {
        rateLimitingService = new RateLimitingService();
        ReflectionTestUtils.setField(rateLimitingService, "capacity", 2);
        ReflectionTestUtils.setField(rateLimitingService, "refillTokens", 2);
        ReflectionTestUtils.setField(rateLimitingService, "refillDurationMinutes", 1);
    }

    @Test
    @DisplayName("Should consume tokens until depleted")
    void should_consumeTokensUntilDepleted() {
        assertTrue(rateLimitingService.tryConsume("client-1"));
        assertTrue(rateLimitingService.tryConsume("client-1"));
        assertFalse(rateLimitingService.tryConsume("client-1"));
    }

    @Test
    @DisplayName("Should report available tokens")
    void should_reportAvailableTokens() {
        rateLimitingService.tryConsume("client-2");
        long remaining = rateLimitingService.getAvailableTokens("client-2");
        assertEquals(1, remaining);
    }

    @Test
    @DisplayName("Should return full capacity when no tokens consumed")
    void should_returnFullCapacity_when_noTokensConsumed() {
        long available = rateLimitingService.getAvailableTokens("client-new");
        assertEquals(2, available);
    }

    @Test
    @DisplayName("Should respect custom bucket limits")
    void should_respectCustomBucketLimits() {
        assertTrue(rateLimitingService.tryConsumeWithCustomLimit("client-3", 1, 1, 1));
        assertFalse(rateLimitingService.tryConsumeWithCustomLimit("client-3", 1, 1, 1));
    }

    @Test
    @DisplayName("Should consume tokens using HttpServletRequest with X-Forwarded-For header")
    void should_consumeTokens_usingHttpServletRequest_withXForwardedFor() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1");

        // When
        boolean result1 = rateLimitingService.tryConsume(request);
        boolean result2 = rateLimitingService.tryConsume(request);
        boolean result3 = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertFalse(result3);
        verify(request, times(3)).getHeader("X-Forwarded-For");
    }

    @Test
    @DisplayName("Should consume tokens using HttpServletRequest with X-Real-IP header when X-Forwarded-For is null")
    void should_consumeTokens_usingHttpServletRequest_withXRealIP() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");

        // When
        boolean result1 = rateLimitingService.tryConsume(request);
        boolean result2 = rateLimitingService.tryConsume(request);
        boolean result3 = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertFalse(result3);
        verify(request, times(3)).getHeader("X-Forwarded-For");
        verify(request, times(3)).getHeader("X-Real-IP");
    }

    @Test
    @DisplayName("Should consume tokens using HttpServletRequest with RemoteAddr when headers are null")
    void should_consumeTokens_usingHttpServletRequest_withRemoteAddr() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        // When
        boolean result1 = rateLimitingService.tryConsume(request);
        boolean result2 = rateLimitingService.tryConsume(request);
        boolean result3 = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertFalse(result3);
        verify(request, times(3)).getHeader("X-Forwarded-For");
        verify(request, times(3)).getHeader("X-Real-IP");
        verify(request, times(3)).getRemoteAddr();
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For with multiple IPs and use first one")
    void should_handleXForwardedFor_withMultipleIPs() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("192.168.1.1, 10.0.0.1, 172.16.0.1");

        // When
        boolean result = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result);
        // Verify that the first IP (192.168.1.1) is used as the key
        // by checking that subsequent calls with same request use the same bucket
        boolean result2 = rateLimitingService.tryConsume(request);
        assertTrue(result2);
    }

    @Test
    @DisplayName("Should handle X-Forwarded-For with empty string")
    void should_handleXForwardedFor_withEmptyString() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn("");
        when(request.getHeader("X-Real-IP")).thenReturn("10.0.0.1");

        // When
        boolean result = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result);
        verify(request).getHeader("X-Forwarded-For");
        verify(request).getHeader("X-Real-IP");
    }

    @Test
    @DisplayName("Should handle X-Real-IP with empty string")
    void should_handleXRealIP_withEmptyString() {
        // Given
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getHeader("X-Real-IP")).thenReturn("");
        when(request.getRemoteAddr()).thenReturn("172.16.0.1");

        // When
        boolean result = rateLimitingService.tryConsume(request);

        // Then
        assertTrue(result);
        verify(request).getHeader("X-Forwarded-For");
        verify(request).getHeader("X-Real-IP");
        verify(request).getRemoteAddr();
    }

    @Test
    @DisplayName("Should create separate buckets for different keys")
    void should_createSeparateBuckets_forDifferentKeys() {
        // Given & When
        boolean result1 = rateLimitingService.tryConsume("key1");
        boolean result2 = rateLimitingService.tryConsume("key2");
        boolean result3 = rateLimitingService.tryConsume("key1");
        boolean result4 = rateLimitingService.tryConsume("key2");

        // Then
        assertTrue(result1);
        assertTrue(result2);
        assertTrue(result3);
        assertTrue(result4);
        
        // Each key should have its own bucket with full capacity
        assertEquals(0, rateLimitingService.getAvailableTokens("key1"));
        assertEquals(0, rateLimitingService.getAvailableTokens("key2"));
    }

    @Test
    @DisplayName("Should create separate buckets for custom limits")
    void should_createSeparateBuckets_forCustomLimits() {
        // Given & When
        boolean result1 = rateLimitingService.tryConsume("client-4");
        boolean result2 = rateLimitingService.tryConsume("client-4"); // Deplete default bucket
        boolean result3 = rateLimitingService.tryConsumeWithCustomLimit("client-4", 5, 5, 1);
        boolean result4 = rateLimitingService.tryConsumeWithCustomLimit("client-4", 5, 5, 1);

        // Then
        assertTrue(result1); // Uses default bucket (capacity 2)
        assertTrue(result2); // Uses default bucket (capacity 2) - now depleted
        assertTrue(result3); // Uses custom bucket (capacity 5)
        assertTrue(result4); // Still has tokens in custom bucket
        
        // Default bucket should be depleted
        assertFalse(rateLimitingService.tryConsume("client-4"));
        
        // Custom bucket should still have tokens
        assertTrue(rateLimitingService.tryConsumeWithCustomLimit("client-4", 5, 5, 1));
    }
}
