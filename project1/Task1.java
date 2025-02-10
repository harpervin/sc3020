import java.io.*;
import java.util.*;
import java.nio.ByteBuffer;

/**
 * Attribute Sizes in Memory (Binary Representation)
 * We set attributes as fixed-length fields
 * 
 * | Attribute    | Data Type  | Size (Bytes) | Fixed / Variable |
 * |--------------|------------|--------------|------------------|
 * | gameDate     | String     | 10           | Fixed     |
 * | teamIDHome   | int        | 4            | Fixed     |
 * | ptsHome      | int        | 4            | Fixed     |
 * | fgPctHome    | float      | 4            | Fixed     |
 * | ftPctHome    | float      | 4            | Fixed     |
 * | fg3PctHome   | float      | 4            | Fixed     |
 * | astHome      | int        | 4            | Fixed     |
 * | rebHome      | int        | 4            | Fixed     |
 * | homeTeamWins | int        | 4            | Fixed     |
 * | Total        |            | 42 bytes     |           |
 */

class Record implements Serializable {
    static final int RECORD_SIZE = 42; // Fixed record size in bytes
    String gameDate;
    int teamIDHome, ptsHome, astHome, rebHome, homeTeamWins;
    float fgPctHome, ftPctHome, fg3PctHome;
    
    public Record(String gameDate, int teamIDHome, int ptsHome, float fgPctHome, float ftPctHome, float fg3PctHome, int astHome, int rebHome, int homeTeamWins) {
        this.gameDate = gameDate;
        this.teamIDHome = teamIDHome;
        this.ptsHome = ptsHome;
        this.fgPctHome = fgPctHome;
        this.ftPctHome = ftPctHome;
        this.fg3PctHome = fg3PctHome;
        this.astHome = astHome;
        this.rebHome = rebHome;
        this.homeTeamWins = homeTeamWins;
    }
    
    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
        buffer.put(gameDate.getBytes());
        buffer.putInt(teamIDHome);
        buffer.putInt(ptsHome);
        buffer.putFloat(fgPctHome);
        buffer.putFloat(ftPctHome);
        buffer.putFloat(fg3PctHome);
        buffer.putInt(astHome);
        buffer.putInt(rebHome);
        buffer.putInt(homeTeamWins);
        return buffer.array();
    }
}

class Block {
    static final int BLOCK_SIZE = 4096;
    List<Record> records;
    
    public Block() {
        records = new ArrayList<>();
    }
    
    public boolean addRecord(Record record) {
        if (records.size() * Record.RECORD_SIZE < BLOCK_SIZE) {
            records.add(record);
            return true;
        }
        return false;
    }
}

class Disk {
    private List<Block> blocks;
    private int totalRecordsStored = 0; // Track actual records stored

    public Disk() {
        blocks = new ArrayList<>();
    }

    public void writeRecord(Record record) {
        if (blocks.isEmpty() || !blocks.get(blocks.size() - 1).addRecord(record)) {
            Block newBlock = new Block();
            newBlock.addRecord(record);
            blocks.add(newBlock);
        }
        totalRecordsStored++; // Increment exact count
    }

    public int getBlockCount() {
        return blocks.size();
    }

    public int getTotalRecordsStored() {
        return totalRecordsStored;
    }
}


class StorageManager {
    private static final String FILE_NAME = "games.txt";
    private Disk disk;
    
    public StorageManager() {
        disk = new Disk();
    }
    
    public void loadRecords() throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(FILE_NAME))) {
            String line;
            br.readLine(); // Skip header
            while ((line = br.readLine()) != null) {
                String[] values = line.split("\t");
    
                // Ensure each value is valid before parsing
                if (values.length < 9) {
                    System.err.println("Skipping malformed line: " + line);
                    continue;
                }
    
                try {
                    String gameDate = values[0];
                    int teamIDHome = !values[1].isEmpty() ? Integer.parseInt(values[1]) : 0;
                    int ptsHome = !values[2].isEmpty() ? Integer.parseInt(values[2]) : 0;
                    float fgPctHome = !values[3].isEmpty() ? Float.parseFloat(values[3]) : 0.0f;
                    float ftPctHome = !values[4].isEmpty() ? Float.parseFloat(values[4]) : 0.0f;
                    float fg3PctHome = !values[5].isEmpty() ? Float.parseFloat(values[5]) : 0.0f;
                    int astHome = !values[6].isEmpty() ? Integer.parseInt(values[6]) : 0;
                    int rebHome = !values[7].isEmpty() ? Integer.parseInt(values[7]) : 0;
                    int homeTeamWins = !values[8].isEmpty() ? Integer.parseInt(values[8]) : 0;
    
                    Record record = new Record(gameDate, teamIDHome, ptsHome, fgPctHome, ftPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
                    disk.writeRecord(record);
                } catch (NumberFormatException e) {
                    System.err.println("Skipping invalid data: " + line);
                }
            }
        }
    }
    
    
    public void reportStorageStatistics() {
        int recordSize = Record.RECORD_SIZE;
        int recordsPerBlock = Block.BLOCK_SIZE / recordSize;
        int numBlocks = disk.getBlockCount();
        int actualRecordsStored = disk.getTotalRecordsStored(); // Get exact count
    
        System.out.println("Record size: " + recordSize + " bytes");
        System.out.println("Records per block: " + recordsPerBlock);
        System.out.println("Total number of blocks used: " + numBlocks);
        System.out.println("Total records stored: " + actualRecordsStored);
    }
    
}

public class Task1 {
    public static void main(String[] args) throws IOException {
        StorageManager storageManager = new StorageManager();
        storageManager.loadRecords();
        storageManager.reportStorageStatistics();
    }
}
