package cloudos.dao;

import cloudos.model.ServiceKey;
import cloudos.service.RootyService;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.cobbzilla.util.io.FileUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Repository;
import rooty.toots.service.ServiceKeyHandler;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;

@Repository @Slf4j
public class ServiceKeyDAO  {

    public static final String PUB_SUFFIX = ".pub";
    public static final FilenameFilter KEY_FILTER = new FilenameFilter() {
        @Override
        public boolean accept(File dir, String name) {
            return name.startsWith(ServiceKeyHandler.KEYNAME_PREFIX) && name.endsWith(ServiceKeyHandler.KEYNAME_SUFFIX+ PUB_SUFFIX);
        }
    };

    @Autowired private RootyService rooty;

    @Getter(value= AccessLevel.PROTECTED, lazy=true) private final ServiceKeyHandler handler = initHandler();
    private ServiceKeyHandler initHandler() { return rooty.getHandler(ServiceKeyHandler.class); }

    public ServiceKey findByName (String name) {
        final File keyfile = new File(getHandler().getServiceDir(), ServiceKeyHandler.keyName(name)+".pub");
        return keyfile.exists() ? (ServiceKey) new ServiceKey().setPublicKey(FileUtil.toStringOrDie(keyfile)).setName(name) : null;
    }

    public List<ServiceKey> findAll() {
        final File[] keyFiles = new File(getHandler().getServiceDir()).listFiles(KEY_FILTER);
        final List<ServiceKey> keys = new ArrayList<>();
        if (keyFiles != null) {
            for (File f : keyFiles) {
                final String basename = f.getName().substring(0, f.getName().length() - PUB_SUFFIX.length());
                final String keyname = ServiceKeyHandler.baseKeyName(basename);
                keys.add((ServiceKey) new ServiceKey().setPublicKey(FileUtil.toStringOrDie(f)).setName(keyname));
            }
        }
        return keys;
    }
}
