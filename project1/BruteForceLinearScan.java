import java.io.IOException;
import java.util.concurrent.TimeUnit;

public class BruteForceLinearScan {
    public static void main(String[] args) throws ClassNotFoundException {
        try {
            // Initialize disk storage
            Disk disk = new Disk("disk_storage.dat");

            int totalBlocksInDisk = disk.getBlockCounter();
            int totalRecordsFound = 0;
            
            int blockID;
            long startTime = System.nanoTime();
            for (blockID = 0; blockID < totalBlocksInDisk; blockID++) {
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