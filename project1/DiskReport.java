import java.io.IOException;

public class DiskReport {
    public static void main(String[] args) {
        try {
            // Initialize disk storage
            Disk disk = new Disk("disk_storage.dat");

            // Define constants
            int RECORD_SIZE = Record.RECORD_SIZE; // Size of each record in bytes
            int BLOCK_SIZE = Block.BLOCK_SIZE; // Size of a block in bytes
            int RECORDS_PER_BLOCK = Block.RECORDS_PER_BLOCK; // Number of records per block
            int DISK_SIZE = Disk.DISK_SIZE; // Total disk size in bytes
            int MAX_BLOCKS = Disk.MAX_BLOCKS; // Maximum number of blocks

            // Get actual number of blocks used
            int totalBlocksUsed = disk.getBlockCounter();
            int totalRecords = 0; // Will be calculated by iterating through blocks
            int recordsInLastBlock = 0;

            // Iterate through blocks to count records
            for (int blockID = 0; blockID < totalBlocksUsed; blockID++) {
                Block block = disk.readBlock(blockID);
                int recordCount = block.getRecordCount();
                totalRecords += recordCount;
                
                // Check if this is the last block
                if (blockID == totalBlocksUsed - 1) {
                    recordsInLastBlock = recordCount;
                }
            }

            // Calculate available blocks left
            int availableBlocksLeft = MAX_BLOCKS - totalBlocksUsed;

            // Print statistics
            System.out.println("===== Disk Storage Report =====");
            System.out.println("🔹 Record Size: " + RECORD_SIZE + " bytes");
            System.out.println("🔹 Block Size: " + BLOCK_SIZE + " bytes");
            System.out.println("🔹 Total Disk Size: " + DISK_SIZE + " bytes (" 
                    + (DISK_SIZE / 1024) + " KB, " + (DISK_SIZE / (1024 * 1024)) + " MB)");
            System.out.println("🔹 Total Blocks Available: " + MAX_BLOCKS);
            System.out.println("🔹 Blocks Used: " + totalBlocksUsed);
            System.out.println("🔹 Available Blocks Left: " + availableBlocksLeft);
            System.out.println("🔹 Total Records Stored: " + totalRecords);
            System.out.println("🔹 Records Per Block (max): " + RECORDS_PER_BLOCK);
            System.out.println("🔹 Records in Last Block: " + recordsInLastBlock);

            disk.close(); // Close disk file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
