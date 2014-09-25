package cloudos.server;

import org.cobbzilla.wizard.server.RestServerLifecycleListener;

public class CloudOsServerListener implements RestServerLifecycleListener<CloudOsServer> {

    @Override public CloudOsServer beforeStart(CloudOsServer server) { return server; }

    @Override
    public void onStart(CloudOsServer server) {
        final int port = server.getConfiguration().getHttp().getPort();

        // todo: start system default browser with this URL
        System.out.println("http://localhost:" + port + "/");
    }

    @Override public void onStop(CloudOsServer server) {}

}
