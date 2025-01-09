package io.github.isagroup.services.updaters;

import java.util.LinkedHashMap;
import java.util.Map;

import io.github.isagroup.exceptions.UpdateException;
import io.github.isagroup.exceptions.VersionException;

public class YamlUpdater {

    private static final Map<Version, Updater> updaters = new LinkedHashMap<>();

    static {
        updaters.put(Version.V1_0, new V10ToV11Updater(null));
        updaters.put(Version.V1_1, new V11ToV20Updater(updaters.get(Version.V1_0)));
        updaters.put(Version.V2_0, new V20ToV21Updater(updaters.get(Version.V1_1)));
    }

    public static void update(Map<String, Object> configFile) throws UpdateException {

        if (configFile.get("version") == null) {
            throw new VersionException("The version field of the pricing must not be null or undefined. Please ensure that the version field is present and correctly formatted");
        }

        Version version = Version.version(configFile.get("version"));
        if (updaters.get(version) == null) {
            return;
        }
        updaters.get(Version.V2_0).update(configFile);
    }
}
