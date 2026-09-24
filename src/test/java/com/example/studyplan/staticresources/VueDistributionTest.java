package com.example.studyplan.staticresources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class VueDistributionTest {
    private static final Path STATIC = Path.of("src/main/resources/static");

    @Test
    void springBootServesBuiltVueEntryInsteadOfLegacyPages() throws Exception {
        String index = Files.readString(STATIC.resolve("index.html"));
        assertThat(index).contains("id=\"app\"", "assets/");
        assertThat(Files.isDirectory(STATIC.resolve("assets"))).isTrue();
        assertThat(Files.exists(STATIC.resolve("pages"))).isFalse();
        assertThat(Files.exists(STATIC.resolve("js"))).isFalse();
        assertThat(Files.exists(STATIC.resolve("css"))).isFalse();
    }
}
