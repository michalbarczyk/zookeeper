import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CountDownLatch;

public class DescendantWatcher implements Watcher {

    private final CountDownLatch connectedSignal;
    private final ZooKeeper zoo;
    private final String path;
    private final Monitor monitor;

    public DescendantWatcher(CountDownLatch connectedSignal, ZooKeeper zoo, String path, Monitor monitor) {
        this.zoo = zoo;
        this.connectedSignal = connectedSignal;
        this.path = path;
        this.monitor = monitor;
    }

    @Override
    public void process(WatchedEvent we) {

        try {
            if (we.getType() == Event.EventType.NodeChildrenChanged) {

                List<String> children = zoo.getChildren(path, new DescendantWatcher(connectedSignal, zoo, path, monitor));
                for (String child : children) {
                    String childPath = Monitor.getChildPath(this.path, child);
                    zoo.getChildren(childPath, new DescendantWatcher(connectedSignal, zoo, childPath, monitor));
                }

                System.out.println("aktualna liczba potomków 'z': " + monitor.getNoOfDescendants("/z"));
            }

        } catch (Exception ex) {
            System.out.println(ex.getMessage());
        }
    }





}
