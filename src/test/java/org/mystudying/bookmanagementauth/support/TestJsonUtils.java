package org.mystudying.bookmanagementauth.support;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/**
 * Utility for loading JSON test payloads from src/test/resources.
 */
public final class TestJsonUtils {

    private TestJsonUtils() {
    }

    public static String readJsonFile(String filename) throws IOException {
        return new ClassPathResource(filename).getContentAsString(StandardCharsets.UTF_8);
    }
}
