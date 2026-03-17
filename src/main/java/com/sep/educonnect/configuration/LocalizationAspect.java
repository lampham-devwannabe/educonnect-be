package com.sep.educonnect.configuration;

import com.sep.educonnect.helper.AutoLocalize;
import com.sep.educonnect.service.I18nService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.util.Collection;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class LocalizationAspect {

    private final I18nService i18nService;

    @Around("@annotation(autoLocalize)")
    public Object localizeResponse(ProceedingJoinPoint joinPoint, AutoLocalize autoLocalize) throws Throwable {
        // Execute the original method
        Object result = joinPoint.proceed();

        if (result == null) {
            return result;
        }

        // Process the result based on annotation settings
        if (autoLocalize.localizeFields()) {
            result = localizeFields(result);
        }

        if (autoLocalize.formatPrices()) {
            result = formatPrices(result);
        }

        return result;
    }

    private Object localizeFields(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // Handle collections
            if (obj instanceof Collection<?> collection) {
                collection.forEach(this::localizeFields);
                return obj;
            }

            // Handle arrays
            if (obj.getClass().isArray()) {
                Object[] array = (Object[]) obj;
                for (Object item : array) {
                    localizeFields(item);
                }
                return obj;
            }

            // Handle single objects
            localizeObjectFields(obj);

        } catch (Exception e) {
            log.warn("Failed to localize fields for object: {}", obj.getClass().getSimpleName(), e);
        }

        return obj;
    }

    private void localizeObjectFields(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName();

            // Check if this is a localized field pattern (ends with En or Vi)
            if (fieldName.endsWith("En") || fieldName.endsWith("Vi")) {
                String prefix = fieldName.substring(0, fieldName.length() - 2);

                // Check if we have both En and Vi versions
                if (hasLocalizedFields(clazz, prefix)) {
                    // Set a localized version of this field
                    String localizedValue = i18nService.getLocalizedField(obj, prefix);
                    if (localizedValue != null) {
                        // Create a new field for the localized version (without En/Vi suffix)
                        try {
                            Field localizedField = clazz.getDeclaredField(prefix);
                            localizedField.setAccessible(true);
                            localizedField.set(obj, localizedValue);
                        } catch (NoSuchFieldException e) {
                            // If no generic field exists, we can add it dynamically or skip
                            log.debug("No generic field '{}' found for localization", prefix);
                        }
                    }
                }
            }
        }
    }

    private boolean hasLocalizedFields(Class<?> clazz, String prefix) {
        try {
            clazz.getDeclaredField(prefix + "En");
            clazz.getDeclaredField(prefix + "Vi");
            return true;
        } catch (NoSuchFieldException e) {
            return false;
        }
    }

    private Object formatPrices(Object obj) {
        if (obj == null) {
            return null;
        }

        try {
            // Handle collections
            if (obj instanceof Collection<?> collection) {
                collection.forEach(this::formatPrices);
                return obj;
            }

            // Handle arrays
            if (obj.getClass().isArray()) {
                Object[] array = (Object[]) obj;
                for (Object item : array) {
                    formatPrices(item);
                }
                return obj;
            }

            // Handle single objects
            formatObjectPrices(obj);

        } catch (Exception e) {
            log.warn("Failed to format prices for object: {}", obj.getClass().getSimpleName(), e);
        }

        return obj;
    }

    private void formatObjectPrices(Object obj) throws IllegalAccessException {
        Class<?> clazz = obj.getClass();
        Field[] fields = clazz.getDeclaredFields();

        for (Field field : fields) {
            field.setAccessible(true);
            String fieldName = field.getName().toLowerCase();

            // Check if this field represents a price/money value
            if (isPriceField(fieldName) && field.getType() == BigDecimal.class) {
                BigDecimal value = (BigDecimal) field.get(obj);
                if (value != null) {
                    // Format the price according to current locale
                    String formattedPrice = i18nService.formatPrice(value, null);

                    // Try to find a corresponding formatted field (e.g., priceFormatted)
                    try {
                        Field formattedField = clazz.getDeclaredField(fieldName + "Formatted");
                        formattedField.setAccessible(true);
                        if (formattedField.getType() == String.class) {
                            formattedField.set(obj, formattedPrice);
                        }
                    } catch (NoSuchFieldException e) {
                        log.debug("No formatted field found for price field: {}", fieldName);
                    }
                }
            }
        }
    }

    private boolean isPriceField(String fieldName) {
        return fieldName.contains("price") ||
                fieldName.contains("cost") ||
                fieldName.contains("amount") ||
                fieldName.contains("fee") ||
                fieldName.contains("money");
    }
}
