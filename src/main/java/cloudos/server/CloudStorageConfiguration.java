package cloudos.server;

import cloudos.cslib.storage.CsStorageEngine;
import cloudos.cslib.storage.CsStorageEngineFactory;
import cloudos.cslib.storage.s3.S3StorageEngineConfig;

public class CloudStorageConfiguration extends S3StorageEngineConfig {

    public CsStorageEngine getStorageEngine() {
        try {
            return CsStorageEngineFactory.build(this);
        } catch (Exception e) {
            throw new IllegalStateException("Error building storage engine: "+e, e);
        }
    }

}
