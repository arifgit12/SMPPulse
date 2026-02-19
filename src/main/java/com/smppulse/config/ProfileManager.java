package com.smppulse.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.Constructor;
import org.yaml.snakeyaml.representer.Representer;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Path;

public class ProfileManager {

    private static final Logger log = LoggerFactory.getLogger(ProfileManager.class);
    private TestProfile currentProfile;

    public ProfileManager() {
        this.currentProfile = new TestProfile();
    }

    public TestProfile getCurrentProfile() {
        return currentProfile;
    }

    public void setCurrentProfile(TestProfile profile) {
        this.currentProfile = profile;
    }

    public void saveProfile(Path filePath) throws IOException {
        DumperOptions options = new DumperOptions();
        options.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        options.setPrettyFlow(true);
        options.setIndent(2);

        Representer representer = new Representer(options);
        representer.getPropertyUtils().setSkipMissingProperties(true);

        Yaml yaml = new Yaml(representer, options);

        try (Writer writer = Files.newBufferedWriter(filePath)) {
            yaml.dump(currentProfile, writer);
        }
        log.info("Profile saved to {}", filePath);
    }

    public TestProfile loadProfile(Path filePath) throws IOException {
        LoaderOptions loaderOptions = new LoaderOptions();
        loaderOptions.setAllowDuplicateKeys(false);

        Constructor constructor = new Constructor(TestProfile.class, loaderOptions);
        Yaml yaml = new Yaml(constructor);

        try (Reader reader = Files.newBufferedReader(filePath)) {
            currentProfile = yaml.load(reader);
        }
        log.info("Profile loaded from {}", filePath);
        return currentProfile;
    }

    public TestProfile loadDefaultProfile() {
        try (InputStream is = getClass().getResourceAsStream("/default-profile.yaml")) {
            if (is != null) {
                LoaderOptions loaderOptions = new LoaderOptions();
                Constructor constructor = new Constructor(TestProfile.class, loaderOptions);
                Yaml yaml = new Yaml(constructor);
                currentProfile = yaml.load(is);
                log.info("Default profile loaded");
                return currentProfile;
            }
        } catch (Exception e) {
            log.warn("Could not load default profile, using built-in defaults", e);
        }
        currentProfile = new TestProfile();
        return currentProfile;
    }
}
