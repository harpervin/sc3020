import java.io.*;

public class Task3 {
    public static void main(String[] args) {
        System.out.println("=== Brute Force Linear Scan ===");
        long startTime = System.currentTimeMillis(); // Start time
        BruteForceLinearScan.performScan();
        long endTime = System.currentTimeMillis(); // End time
        System.out.println("Brute Force Linear Scan Time: " + (endTime - startTime) + " ms");

        System.out.println("\n=== B+ Tree Retrieval ===");
        try {
            startTime = System.currentTimeMillis(); // Start time
            new BplusTreeQuery().retrieveTreeFromDiskAndQuery();
            endTime = System.currentTimeMillis(); // End time
            System.out.println("B+ Tree Query Time: " + (endTime - startTime) + " ms");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

class BruteForceLinearScan {
    public static void performScan() {
        try {
            Disk disk = new Disk("disk_storage.dat");
            long treeOffset;
            try (DataInputStream metaIn = new DataInputStream(new FileInputStream("metadata.dat"))) {
                treeOffset = metaIn.readLong();
            }

            int totalRecordsFound = 0;
            int blockSize = 4096;
            int maxBlocksToScan = (int) Math.ceil((double) treeOffset / blockSize);
            int readBlockCounter = 0;
            float sum = 0;

            int blockID;
            for (blockID = 0; blockID < maxBlocksToScan; blockID++) {
                Block block = disk.readBlock(blockID);
                readBlockCounter++;
                for (Record record : block.getRecords()) {
                    if (record.getFgPctHome() >= 600 && record.getFgPctHome() <= 900) {
                        totalRecordsFound++;
                        sum += record.getFgPctHome();
                        // System.out.println(record.getFgPctHome()); // Uncomment to debug
                    }
                }
            }
            float average = sum / totalRecordsFound / 1000;
            System.out.println("Average: " + average);
            System.out.println("Total records found: " + totalRecordsFound);
            System.out.println("Number of blocks accessed: " + readBlockCounter);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}

class BplusTreeQuery {
    public void retrieveTreeFromDiskAndQuery() throws Exception {
        long treeOffset, treeLength;
        try (DataInputStream metaIn = new DataInputStream(new FileInputStream("metadata.dat"))) {
            treeOffset = metaIn.readLong();
            treeLength = metaIn.readLong();
        }

        byte[] treeBytes = new byte[(int) treeLength];

        try (RandomAccessFile raf = new RandomAccessFile("disk_storage.dat", "r")) {
            raf.seek(treeOffset);
            raf.readFully(treeBytes);
        }

        try (ByteArrayInputStream bais = new ByteArrayInputStream(treeBytes);
                ObjectInputStream ois = new ObjectInputStream(bais)) {
            BPlustree tree = (BPlustree) ois.readObject();
            System.out.println("B+ tree successfully retrieved.");
            tree.search_range(0.600, 0.900, tree.getRoot(), new Disk("disk_storage.dat"));
        }

    }
}
