package com.sep.educonnect.service.unit;

import com.sep.educonnect.entity.Subject;
import com.sep.educonnect.service.I18nService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.math.BigDecimal;
import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("I18nService Unit Tests")
class I18nServiceTest {

    @Mock
    private MessageSource messageSource;

    @InjectMocks
    private I18nService i18nService;

    @AfterEach
    void tearDown() {
        LocaleContextHolder.resetLocaleContext();
    }

    @Test
    @DisplayName("Should return localized message")
    void should_returnLocalizedMessage() {
        LocaleContextHolder.setLocale(Locale.US);
        when(messageSource.getMessage(eq("greeting"), any(), eq("greeting"), eq(Locale.US)))
                .thenReturn("Hello");

        String msg = i18nService.msg("greeting");
        assertEquals("Hello", msg);
    }

    @Test
    @DisplayName("Should return key when message not found")
    void should_returnKeyWhenMessageNotFound() {
        LocaleContextHolder.setLocale(Locale.US);

        when(messageSource.getMessage(eq("unknown.key"), any(), eq("unknown.key"), eq(Locale.US)))
                .thenReturn("unknown.key");

        String msg = i18nService.msg("unknown.key");

        assertEquals("unknown.key", msg);
    }

    @Test
    @DisplayName("Should return multiple localized messages")
    void should_returnMultipleMessages() {
        LocaleContextHolder.setLocale(Locale.FRANCE);
        when(messageSource.getMessage(eq("a"), any(), eq("a"), eq(Locale.FRANCE))).thenReturn("A");
        when(messageSource.getMessage(eq("b"), any(), eq("b"), eq(Locale.FRANCE))).thenReturn("B");

        Map<String, String> messages = i18nService.getMessages("a", "b");
        assertEquals("A", messages.get("a"));
        assertEquals("B", messages.get("b"));
    }

    @Test
    @DisplayName("Should fallback to key when message not found")
    void should_fallbackToKeyWhenMessageMissing() {
        LocaleContextHolder.setLocale(Locale.GERMANY);

        when(messageSource.getMessage(eq("missing"), any(), eq("missing"), eq(Locale.GERMANY)))
                .thenReturn("missing");

        Map<String, String> messages = i18nService.getMessages("missing");

        assertEquals("missing", messages.get("missing"));
    }

    @Test
    @DisplayName("Should format price with currency")
    void should_formatPrice() {
        LocaleContextHolder.setLocale(Locale.US);
        String formatted = i18nService.formatPrice(BigDecimal.valueOf(1234.5), "USD");
        assertTrue(formatted.contains("$"));
    }

    @Test
    @DisplayName("Should format price using Vietnamese default currency when no currency provided")
    void should_formatPriceWithVietnameseDefaultCurrency() {
        LocaleContextHolder.setLocale(new Locale("vi", "VN")); // Default currency: VND

        String formatted = i18nService.formatPrice(BigDecimal.valueOf(15000), null);

        // VND symbol is ₫ and usually appears at the end (e.g., "15.000 ₫")
        assertTrue(formatted.contains("₫"));
    }

    @Test
    @DisplayName("Should get localized field with fallback")
    void should_getLocalizedFieldWithFallback() {
        Subject subject = Subject.builder()
                .nameEn("Math")
                .nameVi("Toán")
                .build();

        LocaleContextHolder.setLocale(Locale.US);
        assertEquals("Math", i18nService.getLocalizedField(subject, "name"));

        LocaleContextHolder.setLocale(Locale.forLanguageTag("vi"));
        assertEquals("Toán", i18nService.getLocalizedField(subject, "name"));
    }

    @Test
    @DisplayName("Should fallback when localized field does not exist")
    void should_fallbackWhenFieldMissing() {
        class Dummy {
            public String titleVi = "Tiêu đề";
        }

        LocaleContextHolder.setLocale(Locale.US); // Tìm field titleEn nhưng không tồn tại, sẽ fallback về titleVi
        Dummy dummy = new Dummy();

        String result = i18nService.getLocalizedField(dummy, "title");

        assertEquals("Tiêu đề", result); // fallback
    }
}
