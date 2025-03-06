import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;

public class test {
    // Assuming you have retrieved treeOffset and treeLength from your metadata
    public void retrieveTree() throws Exception {
        // Start the timer
        long startTime = System.currentTimeMillis();
        long treeOffset = 696320;  // Using long for file offset
        int treeLength = 1376853;
        byte[] treeBytes = new byte[treeLength];
        Disk disk = null;

        disk = new Disk("disk_storage.dat");

        // Read the tree bytes from disk_storage.dat at the given offset
        try (RandomAccessFile raf = new RandomAccessFile("disk_storage.dat", "r")) {
            raf.seek(treeOffset);
            raf.readFully(treeBytes);
        }
        
        // Deserialize the byte array back into a BPlustree object
        try (ByteArrayInputStream bais = new ByteArrayInputStream(treeBytes);
             ObjectInputStream ois = new ObjectInputStream(bais)) {
            BPlustree tree = (BPlustree) ois.readObject();
            System.out.println("B+ tree successfully retrieved and deserialized.");
            // You can now work with the tree object as needed.
            tree.check_leaf_connections(tree.getRoot());
                  
            tree.search_range(0.600, 0.900, tree.getRoot(), disk);
            long endTime = System.currentTimeMillis();
            System.out.println("Time taken to access the block: " + (endTime - startTime) + " ms");

        }
    }

    public static void main(String[] args) {
        try {
            new test().retrieveTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
