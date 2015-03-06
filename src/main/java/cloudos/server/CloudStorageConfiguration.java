package cloudos.server;

import cloudos.cslib.storage.CsStorageEngine;
import cloudos.cslib.storage.CsStorageEngineFactory;
import cloudos.cslib.storage.s3.S3StorageEngineConfig;

import static org.cobbzilla.util.daemon.ZillaRuntime.die;

public class CloudStorageConfiguration extends S3StorageEngineConfig {

    public CsStorageEngine getStorageEngine() {
        try {
            return CsStorageEngineFactory.build(this);
        } catch (Exception e) {
            return die("Error building storage engine: " + e, e);
        }
    }

}
