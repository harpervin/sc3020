import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Block {
    public static final int BLOCK_SIZE = 4096; // 4KB block
    public static final int BLOCK_ID_SIZE = 4; // 4 bytes for Block ID
    public static final int HEADER_SIZE = 8;   // 4 bytes Block ID + 4 bytes Num Records
    public static final int RECORDS_PER_BLOCK = (BLOCK_SIZE - HEADER_SIZE) / Record.RECORD_SIZE;

    private List<Record> records;
    private int blockID;

    public Block(int blockID) {
        this.blockID = blockID;
        this.records = new ArrayList<>();
    }

    public int getBlockID() {
        return blockID;
    }

    public boolean addRecord(Record record) {
        if (records.size() < RECORDS_PER_BLOCK) {
            records.add(record);
            return true;
        }
        return false; // Block is full
    }

    public List<Record> getRecords() {
        return records;
    }

    // Convert Block to Byte Array for Storage
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(BLOCK_SIZE);
        buffer.putInt(blockID);       // First 4 bytes → Block ID
        buffer.putInt(records.size()); // Next 4 bytes → Number of Records

        for (Record record : records) {
            buffer.put(record.toBytes()); // Write each record's bytes
        }

        return buffer.array();
    }

    // Convert Byte Array Back to Block
    public static Block fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
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
