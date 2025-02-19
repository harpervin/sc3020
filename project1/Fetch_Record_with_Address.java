import java.io.IOException;

public class Fetch_Record_with_Address {
    public static void main(String[] args) {
        try {
            Disk disk = new Disk("disk_storage.dat");

            // Assuming the address points to block 169 and the record is at index 2
            Block block = disk.readBlock(169);
            // Max of 170 blocks - last block index is 169
            // Max 157 Records per block - last record index per block is 156
            // Last block index 169, last record index 18
            PhysicalAddress address = new PhysicalAddress(block, 18); // Block 300, Index 2
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