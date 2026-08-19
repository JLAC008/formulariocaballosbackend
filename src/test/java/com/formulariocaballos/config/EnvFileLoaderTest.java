package com.formulariocaballos.config;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class EnvFileLoaderTest {
    @Test
    void loadsMissingPropertiesFromEnvFile() throws Exception {
        String key = "TEST_ENV_FILE_LOADER_VALUE";
        System.clearProperty(key);
        Path env = testEnvFile("loadsMissingPropertiesFromEnvFile");
        Files.writeString(env, "# comment\n" + key + "=loaded\n");

        EnvFileLoader.load(env);

        assertThat(System.getProperty(key)).isEqualTo("loaded");
        System.clearProperty(key);
    }

    @Test
    void doesNotOverrideExistingSystemProperties() throws Exception {
        String key = "TEST_ENV_FILE_LOADER_EXISTING";
        System.setProperty(key, "existing");
        Path env = testEnvFile("doesNotOverrideExistingSystemProperties");
        Files.writeString(env, key + "=from-file\n");

        EnvFileLoader.load(env);

        assertThat(System.getProperty(key)).isEqualTo("existing");
        System.clearProperty(key);
    }

    @Test
    void removesSimpleWrappingQuotes() throws Exception {
        String key = "TEST_ENV_FILE_LOADER_QUOTED";
        System.clearProperty(key);
        Path env = testEnvFile("removesSimpleWrappingQuotes");
        Files.writeString(env, key + "=\"quoted value\"\n");

        EnvFileLoader.load(env);

        assertThat(System.getProperty(key)).isEqualTo("quoted value");
        System.clearProperty(key);
    }

    private Path testEnvFile(String name) throws Exception {
        Path dir = Path.of("target", "env-file-loader-test", name);
        Files.createDirectories(dir);
        return dir.resolve(".env");
    }
}
