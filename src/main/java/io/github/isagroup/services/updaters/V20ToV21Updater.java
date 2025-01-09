package io.github.isagroup.services.updaters;

import java.util.Map;

import io.github.isagroup.exceptions.UpdateException;
import io.github.isagroup.exceptions.VersionException;

public class V20ToV21Updater extends VersionUpdater {

    public V20ToV21Updater(Updater updater) {
        super(Version.V2_0, updater);
    }

    @Override
    public void update(Map<String, Object> configFile) throws UpdateException {

        try {
            if (Version.version(configFile.get("version")).compare(this.getSource()) < 0) {
                super.update(configFile);

            }
        } catch (VersionException e) {
            throw new UpdateException(e.getMessage(), configFile);
        }

        configFile.put("version", "2.1");

    }

}
