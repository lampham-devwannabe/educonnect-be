package com.sep.educonnect.helper;

import com.sep.educonnect.service.I18nService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Locale;

@Component
@RequiredArgsConstructor
public class LocalizationHelper {
    private final I18nService i18nService;
    
    public String getLocalizedField(Object entity, String fieldPrefix) {
        return i18nService.getLocalizedField(entity, fieldPrefix);
    }
    
    public String formatPrice(BigDecimal amount, String currency) {
        return i18nService.formatPrice(amount, currency);
    }
}
