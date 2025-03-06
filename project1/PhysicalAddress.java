import java.nio.ByteBuffer;

class PhysicalAddress {
    private int blockID;  // Block number in disk
    private int recordIndex; // Index of record inside block

    public static final int SIZE = 8; // 4 bytes blockID + 4 bytes recordIndex

    public PhysicalAddress(int blockID, int recordIndex) {
        this.blockID = blockID;
        this.recordIndex = recordIndex;
    }

    public int getBlockID() {
        return blockID;
    }

    public int getRecordIndex() {
        return recordIndex;
    }

    @Override
    public String toString() {
        return "Block: " + blockID + ", Record Index: " + recordIndex;
    }

    // Convert PhysicalAddress to Byte Array for Storage
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(SIZE);
        buffer.putInt(blockID);
        buffer.putInt(recordIndex);
        return buffer.array();
    }

    // Convert Byte Array Back to PhysicalAddress
    public static PhysicalAddress fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int blockID = buffer.getInt();
        int recordIndex = buffer.getInt();
        return new PhysicalAddress(blockID, recordIndex);
    }
}
