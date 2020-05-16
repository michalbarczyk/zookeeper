import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;

import java.io.IOException;

import java.util.concurrent.CountDownLatch;

public class ZWatcher implements Watcher {

    private final CountDownLatch connectedSignal;
    private final ZooKeeper zoo;
    private final String path;
    private final Monitor monitor;
    private final String externalApp;

    public ZWatcher(CountDownLatch connectedSignal, ZooKeeper zoo, String path, Monitor monitor, String externalApp) {
        this.zoo = zoo;
        this.connectedSignal = connectedSignal;
        this.path = path;
        this.monitor = monitor;
        this.externalApp = externalApp;
    }

    @Override
    public void process(WatchedEvent we) {

        try {

            if (we.getPath().equals("/z")) {

                if (we.getType() == Event.EventType.NodeDeleted) {
                    System.out.println("'z' node deleted");
                    killExternalApp();
                }
                if (we.getType() == Event.EventType.NodeCreated) {
                    System.out.println("'z' node created");
                    startExternalApp();
                    zoo.getChildren("/z", new DescendantWatcher(connectedSignal, zoo, "/z", monitor));
                }

            }

            zoo.exists(path, new ZWatcher(connectedSignal, zoo, path, monitor, externalApp));

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }

    private void startExternalApp()  {
        try {
            Runtime.getRuntime().exec(externalApp);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void killExternalApp() {

        try {
            Runtime.getRuntime().exec("taskkill /F /IM " + externalApp + ".exe");
        } catch (IOException e) {
            System.out.println(e.getMessage());
        }
    }
}
