import java.io.IOException;

public class LoadBPlusTree {
    public static void main(String[] args) {
        try {
            Disk disk = new Disk("disk_storage.dat");

            // Read the root offset of the B+ Tree from disk
            long rootOffset = disk.readBPlusTreeRoot();

            if (rootOffset <= 0) {
                System.out.println("No B+ Tree found on disk.");
            } else {
                BPlustree tree = new BPlustree(340, disk);
                tree.loadTreeFromDisk(); // Load the tree from disk

                if (tree.getRoot() != null) {
                    System.out.println("B+ Tree loaded successfully!");
                    System.out.println("Root Keys: " + tree.getRoot().keys);
                } else {
                    System.out.println("Failed to load B+ Tree from disk.");
                }
            }

            disk.close();
        } catch (IOException e) {
            System.out.println("Error reading B+ Tree from disk: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
