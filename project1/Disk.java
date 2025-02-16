import java.io.IOException;
import java.io.RandomAccessFile;

class Disk {
    public static final int DISK_SIZE = 4 * 1024 * 1024;
    public static final int BLOCK_SIZE = 4096;
    public static final int MAX_BLOCKS = DISK_SIZE / BLOCK_SIZE;
    private RandomAccessFile diskFile;
    private int blockCounter;

    public Disk(String filePath) throws IOException {
        diskFile = new RandomAccessFile(filePath, "rw");
        blockCounter = countExistingBlocks();
    }

    // Count the number of blocks already written in the file
    private int countExistingBlocks() throws IOException {
        long fileSize = diskFile.length();
        return (int) (fileSize / BLOCK_SIZE);
    }

    public int getBlockCounter() {
        return blockCounter;
    }

    // Helper function to get the next available block ID
    public int findAvailableBlock() throws IOException {
        for (int blockID = 0; blockID < blockCounter; blockID++) {
            Block block = readBlock(blockID);
            if (block.getRecords().size() < Block.RECORDS_PER_BLOCK) {
                return blockID; // Found a block with space
            }
        }
        return blockCounter; // Return the next available block
    }

    public void writeBlock(Block block) throws IOException {
        int blockID = block.getBlockID();
        diskFile.seek((long) blockID * BLOCK_SIZE);
        diskFile.write(block.toBytes());
        blockCounter++;
    }

    public Block readBlock(int blockID) throws IOException {
        if (blockID >= MAX_BLOCKS) {
            throw new IOException("Invalid block index");
        }
        byte[] blockData = new byte[BLOCK_SIZE];
        diskFile.seek((long) blockID * BLOCK_SIZE);
        diskFile.readFully(blockData);
        return Block.fromBytes(blockData);
    }

    public void retrieveBlockData(int blockID) throws IOException {
        Block block = readBlock(blockID);
        System.out.println("Block ID: " + block.getBlockID());
        System.out.println("Number of records: " + block.getRecords().size());
        for (Record record : block.getRecords()) {
            System.out.println(record);
        }
    }

    public Record retrieveRecord(int recordID) throws IOException {
        for (int blockID = 0; blockID < blockCounter; blockID++) {
            Block block = readBlock(blockID);
            for (Record record : block.getRecords()) {
                if (record.getRecordID() == recordID) {
                    System.out.println("Record found in Block " + blockID + ": " + record);
                    return record;
                }
            }
        }
        System.out.println("Record not found.");
        return null;
    }

    public void close() throws IOException {
        diskFile.close();
    }
}
