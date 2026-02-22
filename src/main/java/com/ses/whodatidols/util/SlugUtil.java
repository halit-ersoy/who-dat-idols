package com.ses.whodatidols.util;

import java.text.Normalizer;
import java.util.Locale;
import java.util.regex.Pattern;

public class SlugUtil {

    private static final Pattern NONLATIN = Pattern.compile("[^\\w-]");
    private static final Pattern WHITESPACE = Pattern.compile("[\\s]");

    public static String toSlug(String input) {
        if (input == null || input.isEmpty()) {
            return "";
        }

        String nowhitespace = WHITESPACE.matcher(input).replaceAll("-");
        String normalized = Normalizer.normalize(nowhitespace, Normalizer.Form.NFD);
        String slug = NONLATIN.matcher(normalized).replaceAll("");

        // Remove duplicate hyphens
        slug = slug.replaceAll("-{2,}", "-");

        // Remove leading and trailing hyphens
        slug = slug.replaceAll("^-|-$", "");

        return slug.toLowerCase(Locale.ENGLISH);
    }
}
