import java.io.IOException;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.RandomAccessFile;

public class Main {
    public static void main(String[] args) {
        System.out.println("=== Brute Force Linear Scan ===");
        BruteForceLinearScan.performScan();

        System.out.println("\n=== B+ Tree Retrieval ===");
        try {
            new Test().retrieveTree();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class BruteForceLinearScan {
    public static void performScan() {
        try {
            // Initialize disk storage
            Disk disk = new Disk("disk_storage.dat");

            int totalBlocksInDisk = disk.getBlockCounter();
            int totalRecordsFound = 0;
            
            // Define treeOffset in bytes (the scan should stop before this offset)
            long treeOffset = 696320L;
            
            // Retrieve block size (replace with a constant if not available, e.g., int blockSize = 4096;)
            int blockSize = 4096;
            
            // Calculate the maximum block index to scan
            // Blocks whose starting offset (blockID * blockSize) is less than treeOffset will be scanned.
            int maxBlocksToScan = (int) Math.ceil((double) treeOffset / blockSize);
            // Ensure we do not exceed the total number of blocks in the disk
            maxBlocksToScan = Math.min(maxBlocksToScan, totalBlocksInDisk);
            
            int blockID;
            long startTime = System.nanoTime();
            for (blockID = 0; blockID < maxBlocksToScan; blockID++) {
                Block block = disk.readBlock(blockID);
                int totalRecordsInBlock = block.getRecords().size();
                for (int recordID = 0; recordID < totalRecordsInBlock; recordID++) {
                    Record record = block.getRecords().get(recordID);
                    if (record.getFgPctHome() >= 600 && record.getFgPctHome() <= 900) {
                        totalRecordsFound++;
                        System.out.println("Record found: " + record);
                        System.out.println(record.getFgPctHome());
                    }
                }
            }

            System.out.println("Total blocks accessed: " + blockID);
            System.out.println("Total records found: " + totalRecordsFound);
            long endTime  = System.nanoTime();
            long nanoseconds = endTime - startTime; 
            System.out.println(nanoseconds / 1000000 + " milliseconds");

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class Test {
    // Assuming you have retrieved treeOffset and treeLength from your metadata
    public void retrieveTree() throws Exception {
        // Start the timer
        long startTime = System.currentTimeMillis();
        long treeOffset = 696320;  // Using long for file offset
        int treeLength = 1376853;
        byte[] treeBytes = new byte[treeLength];
        Disk disk = new Disk("disk_storage.dat");

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
            // Work with the tree as needed.
            tree.check_leaf_connections(tree.getRoot());
            tree.search_range(0.600, 0.900, tree.getRoot(), disk);
            long endTime = System.currentTimeMillis();
            System.out.println("Time taken to access the block: " + (endTime - startTime) + " ms");
        }
    }
}
