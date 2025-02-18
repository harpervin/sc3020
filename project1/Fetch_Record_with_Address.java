import java.io.IOException;

public class Fetch_Record_with_Address {
    public static void main(String[] args) {
        try {
            Disk disk = new Disk("disk_storage.dat");

            // Assuming the address points to block 300 and the record is at index 2
            Block block = disk.readBlock(304);
            // max index is max number of records per block which is 88
            PhysicalAddress address = new PhysicalAddress(block, 1); // Block 300, Index 2
            System.err.println("Address: " + address);
            // Retrieve the record using the address
            Record record = disk.retrieveRecordByAddress(address);
            System.out.println("Retrieved Record: " + record);

            // Close the disk file
            disk.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}