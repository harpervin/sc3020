import java.io.IOException;

public class DiskReport {
    public static void main(String[] args) {
        try {
            // Initialize disk storage
            Disk disk = new Disk("disk_storage.dat");

            // Define constants
            int RECORD_SIZE = Record.RECORD_SIZE; // Size of each record in bytes
            int BLOCK_SIZE = Block.BLOCK_SIZE; // Size of a block in bytes
            int BLOCK_HEADER_SIZE = Block.HEADER_SIZE; // Block header size
            int RECORDS_PER_BLOCK = Block.RECORDS_PER_BLOCK; // 97 records per block
            int DISK_SIZE = Disk.DISK_SIZE; // Total disk size
            int MAX_BLOCKS = Disk.MAX_BLOCKS; // Maximum number of blocks

            // Get the total number of records stored dynamically
            // int totalRecords = disk.getTotalRecords();
            // int totalBlocks = (int) Math.ceil((double) totalRecords / RECORDS_PER_BLOCK);

            // Print statistics
            System.out.println("===== Disk Storage Report =====");
            System.out.println("Size of a Record: " + RECORD_SIZE + " bytes");
            // System.out.println("Total Number of Records: " + totalRecords);
            System.out.println("Number of Records per Block: " + RECORDS_PER_BLOCK);
            // System.out.println("Total Number of Blocks Used: " + totalBlocks);
            System.out.println("Total Disk Size: " + DISK_SIZE + " bytes (" + (DISK_SIZE / 1024) + " KB, " + (DISK_SIZE / (1024 * 1024)) + " MB)");
            System.out.println("Total Blocks Available in Disk: " + MAX_BLOCKS);

            disk.close(); // Close disk file
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
