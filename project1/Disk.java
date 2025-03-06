import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
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

    // public int findAvailableBlockForBPT(int startBlockID) throws IOException {
    //     for (int blockID = startBlockID; blockID < blockCounter; blockID++) {
    //         Block block = readBlock(blockID);
    
    //         if (!block.hasNode()) { // If block is empty (no node stored)
    //             return blockID;
    //         }
    //     }
    
    //     return blockCounter; // No empty block found, allocate a new one
    // }

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

    // Retrieve a specific record using its physical address
    public Record retrieveRecordByAddress(PhysicalAddress address) throws IOException {
        int blockID = address.getBlockID();
        Block block = this.readBlock(blockID);
        int recordIndex = address.getRecordIndex();

        if (recordIndex < 0 || recordIndex >= this.readBlock(blockID).getRecords().size()) {
            throw new IndexOutOfBoundsException("Invalid index for block " + blockID);
        }

        return block.getRecords().get(recordIndex);
        // if (index < 0 || index >= block.getRecords().size()) {
        //     throw new IndexOutOfBoundsException("Invalid index for block " + block.getBlockID());
        // }
        // return block.getRecords().get(index);
    }

    public void writeNodeToDisk(long offset, byte[] data) throws IOException {
        if (offset >= DISK_SIZE) {
            throw new IOException("Offset out of disk bounds.");
        }
        diskFile.seek(offset);
        diskFile.write(data);
    }
    
    public byte[] readNodeFromDisk(long offset) throws IOException {
        if (offset >= DISK_SIZE) {
            throw new IOException("Invalid block offset.");
        }
        byte[] data = new byte[Node.NODE_SIZE]; // Read full node size (4KB)
        diskFile.seek(offset);
        diskFile.readFully(data);
        return data;
    }
    
    // Store the B+ Tree root offset at the **fixed** end of the file.
public void writeBPlusTreeRoot(long rootOffset) throws IOException {
    long fixedOffset = DISK_SIZE - Long.BYTES; // Reserve last 8 bytes for root offset
    System.out.println("Writing Root Offset at fixed position: " + fixedOffset);
    
    diskFile.seek(fixedOffset); 
    diskFile.writeLong(rootOffset);
}

// Read the B+ Tree root offset from the **fixed** end of the file.
public long readBPlusTreeRoot() throws IOException {
    long fixedOffset = DISK_SIZE - Long.BYTES; // Reserved last 8 bytes
    diskFile.seek(fixedOffset);
    return diskFile.readLong();
}


    

    public void close() throws IOException {
        diskFile.close();
    }
}