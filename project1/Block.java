import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.SecureDirectoryStream;
import java.util.ArrayList;
import java.util.List;

class Block implements Serializable{
    public static final int BLOCK_SIZE = 4096; // 4KB block size
    public static final int BLOCK_ID_SIZE = 4; // 4 bytes for Block ID
    public static final int HEADER_SIZE = 8; // 4 bytes Block ID + 4 bytes Num Records
    public static final int RECORDS_PER_BLOCK = (BLOCK_SIZE - HEADER_SIZE) / Record.RECORD_SIZE;

    private List<Record> records;
    private int blockID;
    private ArrayList<Integer> availRecordIndex = new ArrayList<>(); // List of available record indexes in a block

    public Block(int blockID){
        this.blockID = blockID;
        this.records = new ArrayList<>();
        for (int i = 0; i < RECORDS_PER_BLOCK; i++) { // initially, all slots should be available for record to be
            // inserted into
            availRecordIndex.add(i);
        }
    }

    public int getBlockID() {
        return blockID;
    }

    public int getRecordCount() {
        return records.size();
    }

    public PhysicalAddress addRecord(Record record) {

        int recordindex = availRecordIndex.get(0);

        try {
            records.add(record);
        } catch (Exception e) {
            System.out.println("Error: Record could not be added to the block.");
            return null;
        }
        availRecordIndex.remove(0);
        return new PhysicalAddress(this, recordindex);

    }

    public boolean isFull() {
        if (availRecordIndex.size() == 0)
            return true;
        return false;
    }

    public List<Record> getRecords() {
        return records;
    }

    // Convert Block to Byte Array for Storage
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(blockID); // First 4 bytes → Block ID
        buffer.putInt(records.size()); // Next 4 bytes → Number of Records

        for (Record record : records) {
            buffer.put(record.toBytes()); // Write each record's bytes
        }

        return buffer.array();
    }

    // Convert Byte Array Back to Block
    public static Block fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int blockID = buffer.getInt(); // Read Block ID
        int numRecords = buffer.getInt(); // Read Number of Records

        Block block = new Block(blockID);
        for (int i = 0; i < numRecords; i++) {
            byte[] recordBytes = new byte[Record.RECORD_SIZE];
            buffer.get(recordBytes); // Read next record
            block.addRecord(Record.fromBytes(recordBytes)); // Convert bytes to Record
        }
        return block;
    }

    @Override
    public String toString() {
        return "Block ID: " + blockID + ", Records Stored: " + records.size();
    }
}
