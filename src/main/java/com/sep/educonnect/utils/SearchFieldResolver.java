package com.sep.educonnect.utils;

import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public final class SearchFieldResolver {

    private SearchFieldResolver() {}

    // Take the field name based on the current locale for search
    public static String localized(String baseField) {
        if (baseField == null) {
            return null;
        }
        Locale locale = LocaleContextHolder.getLocale();
        String suffix = isEnglish(locale) ? ".en" : ".vi";
        return baseField + suffix;
    }

    private static boolean isEnglish(Locale locale) {
        return locale != null && "en".equalsIgnoreCase(locale.getLanguage());
    }
}


