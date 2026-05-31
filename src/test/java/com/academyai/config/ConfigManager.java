package com.academyai.config;

import org.aeonbits.owner.ConfigFactory;

public class ConfigManager {

    private static FrameworkConfig config;

    private ConfigManager() {}

    public static FrameworkConfig getConfig() {
        if (config == null) {
            config = ConfigFactory.create(FrameworkConfig.class, System.getProperties());
        }
        return config;
    }
}
