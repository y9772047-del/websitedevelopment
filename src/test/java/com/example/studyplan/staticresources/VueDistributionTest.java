package com.example.studyplan.staticresources;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.regex.Pattern;

import static org.assertj.core.api.Assertions.assertThat;

class VueDistributionTest {
    private static final Path STATIC = Path.of("src/main/resources/static");
    private static final Pattern ASSET_REFERENCE = Pattern.compile("(?:src|href)=\"(/assets/[^\"]+)\"");

    @Test
    void vueEntryReferencesExistingBuildAssets() throws Exception {
        String index = Files.readString(STATIC.resolve("index.html"));
        assertThat(index).contains("id=\"app\"");
        var assets = ASSET_REFERENCE.matcher(index).results().map(match -> match.group(1)).toList();
        assertThat(assets).hasSizeGreaterThanOrEqualTo(2);
        for (String asset : assets) {
            assertThat(Files.isRegularFile(STATIC.resolve(asset.substring(1))))
                    .as("Vue entry references %s", asset)
                    .isTrue();
        }
    }

    @Test
    void legacyPagesAndScriptsAreAbsentFromTheDistribution() {
        assertThat(Files.exists(STATIC.resolve("pages"))).isFalse();
        assertThat(Files.exists(STATIC.resolve("js"))).isFalse();
        assertThat(Files.exists(STATIC.resolve("css"))).isFalse();
    }
}
