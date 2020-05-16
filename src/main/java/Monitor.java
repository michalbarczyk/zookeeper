import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

public class Monitor {

    private ZooKeeper zoo;
    private Connection conn;
    private String zPath;
    private CountDownLatch connectedSignal;

    public static void main(String[] args) {

        if (args.length == 1)
            new Monitor(args[0]);
    }

    public Monitor(String externalApp) {

        handleUserInput();

        zPath = "/z";
        connectedSignal = new CountDownLatch(1);

        try {
            conn = new Connection();
            zoo = conn.connect("localhost");

            zoo.exists(zPath, new ZWatcher(connectedSignal, zoo, zPath, this, externalApp));

            System.out.println("App started");
            connectedSignal.await();

        } catch (Exception e) {
            System.out.println(e.getMessage());
        }
    }

    public void printTree() {

        try {
            Stat stat = zoo.exists(zPath, false);
            if (stat == null) {
                System.out.println("'z' does not exist -> tree cannot be printed");
                return;
            }
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        int noOfNodes = getNoOfDescendants(zPath);
        List<List<String>> treeMap = new ArrayList<>();

        int maxTreeHeight = 10;

        for (int i = 0; i < maxTreeHeight; i++) {
            treeMap.add(new LinkedList<>());
        }

        try {
            createTreeMap("z", "", treeMap, 0);
        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        List<String> levelsVisualised = treeMap.stream()
                .map(level -> visualiseLevel(5 * noOfNodes, level))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        levelsVisualised.forEach(System.out::println);

    }

    private void createTreeMap(String node, String parentPath, List<List<String>> treeMap, int level) throws KeeperException, InterruptedException {
        treeMap.get(level).add(node);
        String nodePath = getChildPath(parentPath, node);
        List<String> children = zoo.getChildren(nodePath, false);
        children.forEach(child -> {
            try {
                createTreeMap(child, nodePath, treeMap, level + 1);
            } catch (KeeperException | InterruptedException e) {
                e.printStackTrace();
            }
        });
    }

    private String visualiseLevel(int width, List<String> nodes) {

        if (nodes.size() == 0)
            return null;

        int distLength = (width) / (nodes.size() + 1);

        StringBuilder builder = new StringBuilder();

        nodes.forEach(node -> {
            String dist = IntStream.range(0, distLength)
                    .mapToObj(i -> " ")
                    .collect(Collectors.joining(""));

            builder.append(dist);

            builder.append(node);
        });

        return builder.toString();
    }


    public int getNoOfDescendants(String path) {

        Stat stat = null;
        List<String> children = null;

        try {
            stat = zoo.exists(path, false);
            children = zoo.getChildren(path, false);

        } catch (KeeperException | InterruptedException e) {
            e.printStackTrace();
        }

        return stat.getNumChildren() + children.stream()
                .map(child -> getChildPath(path, child))
                .map(this::getNoOfDescendants)
                .reduce(0, Integer::sum);
    }

    public static String getChildPath(String parentPath, String childName) {
        return parentPath + "/" + childName;
    }

    private void handleUserInput() {
        new Thread(() -> {
            BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
            while (true) {
                String line = null;
                try {
                    line = br.readLine();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                if (line.equals("q")) {
                    connectedSignal.countDown();
                    break;
                }
                if (line.equals("tree")) {
                    printTree();
                }
            }
        }).start();
    }
}