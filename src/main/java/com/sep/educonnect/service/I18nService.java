package com.sep.educonnect.service;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Service;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class I18nService {
    private final MessageSource messageSource;

    /**
     * Get localized message by key
     */
    public String msg(String key, Object... args) {
        Locale locale = LocaleContextHolder.getLocale();
        return messageSource.getMessage(key, args, key, locale);
    }

    /**
     * Get multiple localized messages
     */
    public Map<String, String> getMessages(String... keys) {
        Locale locale = LocaleContextHolder.getLocale();
        Map<String, String> messages = new HashMap<>();
        for (String key : keys) {
            messages.put(key, messageSource.getMessage(key, null, key, locale));
        }
        return messages;
    }

    /**
     * Format price according to current locale
     */
    public String formatPrice(BigDecimal amount, String currency) {
        Locale locale = LocaleContextHolder.getLocale();
        NumberFormat formatter = NumberFormat.getCurrencyInstance(locale);
        if (currency != null) {
            formatter.setCurrency(Currency.getInstance(currency));
        }
        return formatter.format(amount);
    }

    /**
     * Gets the localized field value based on current locale
     * Vietnamese is default, English is used when explicitly requested
     */
    public String getLocalizedField(Object entity, String fieldPrefix) {
        if (entity == null || fieldPrefix == null) {
            return null;
        }

        Locale locale = LocaleContextHolder.getLocale();
        String fieldName = fieldPrefix + (isEnglish(locale) ? "En" : "Vi");

        try {
            Field field = entity.getClass().getDeclaredField(fieldName);
            field.setAccessible(true);
            String value = (String) field.get(entity);

            // If the localized field is null or empty, fallback to Vietnamese (default)
            if (value == null || value.trim().isEmpty()) {
                return getFallbackField(entity, fieldPrefix);
            }

            return value;
        } catch (Exception e) {
            // If field doesn't exist, fallback to Vietnamese (default)
            return getFallbackField(entity, fieldPrefix);
        }
    }

    /**
     * Gets the Vietnamese version as fallback (since Vietnamese is default)
     */
    private String getFallbackField(Object entity, String fieldPrefix) {
        try {
            Field fallbackField = entity.getClass().getDeclaredField(fieldPrefix + "Vi");
            fallbackField.setAccessible(true);
            return (String) fallbackField.get(entity);
        } catch (Exception ex) {
            return null;
        }
    }

    /**
     * Checks if current locale is English (since Vietnamese is now default)
     */
    private boolean isEnglish(Locale locale) {
        return "en".equalsIgnoreCase(locale.getLanguage());
    }
}
