import java.io.*;
import java.util.Scanner;

public class LoadFileOnDisk {
    public static void main(String[] args) {
        Disk disk = null;
        Scanner scanner = null;
        // totalRecords is used to count how many records were successfully stored.
        try {
            // Initialize Disk and Mapping Table
            disk = new Disk("disk_storage.dat");
            // MappingTable mappingTable = new MappingTable();

            // Read games.txt file
            scanner = new Scanner(new File("games.txt"));

            // Skip the first line (header)
            if (scanner.hasNextLine()) {
                System.out.println("Skipping header: " + scanner.nextLine());
            }

            int recordID = 1; // Auto-generated Record ID
            int blockID = disk.findAvailableBlock(); // Start from the next available block
            System.out.println("Starting block ID: " + blockID);
            Block block = new Block(blockID);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim(); // Trim whitespace
                if (line.isEmpty())
                    continue; // Skip empty lines

                String[] data = line.split("\t");
                if (data.length != 9) { // Skip rows with missing fields
                    // System.out.println("Skipping malformed row: " + line);
                    continue;
                }

                // Skip rows containing any empty value
                boolean hasEmptyField = false;
                for (String value : data) {
                    if (value.trim().isEmpty()) {
                        hasEmptyField = true;
                        break;
                    }
                }

                if (hasEmptyField) {
                    // System.out.println("Skipping row with empty values: " + line);
                    continue;
                }

                try {
                    // Convert line to a Record object.
                    // Note: recordID is auto-incremented.

                    Record record = new Record(
                            recordID++, // recordID
                            data[0], // gameDate
                            Integer.parseInt(data[1]), // teamIDHome
                            Integer.parseInt(data[2]), // ptsHome
                            Float.parseFloat(data[3]), // fgPctHome
                            Float.parseFloat(data[4]), // ftPctHome
                            Float.parseFloat(data[5]), // fg3PctHome
                            Integer.parseInt(data[6]), // astHome
                            Integer.parseInt(data[7]), // rebHome
                            Integer.parseInt(data[8]) // homeTeamWins
                    );

                    // Store the Record in the current Block.
                    // If the block is full, write it to disk and start a new one.
                    if (!block.addRecord(record)) { // Block is full
                        disk.writeBlock(block); // Write full block to disk
                        blockID = disk.findAvailableBlock(); // Get next available block ID
                        System.out.println("Previous block full, added new block ID: " + blockID);
                        block = new Block(blockID); // Create a new block
                        block.addRecord(record); // Add record to the new block
                    }

                    // Compute Physical Address and Store in MappingTable.
                    // recordIndex is the index of the record in the current block.
                    // int recordIndex = block.getRecords().size() - 1; // Last inserted index
                    // PhysicalAddress address = new PhysicalAddress(blockID, recordIndex * Record.RECORD_SIZE);
                    // mappingTable.addMapping(recordID - 1, address); // recordID-1 gives the current record's ID

                } catch (NumberFormatException e) {
                    // System.out.println("Skipping row with invalid data: " + line);
                }
            }

            // Write the last block if it contains any records.
            if (!block.getRecords().isEmpty()) {
                disk.writeBlock(block);
            }

            System.out.println("Games data successfully stored in disk!");

            // ---------------------------
            // Report Statistics
            // ---------------------------
            System.out.println("Record size: " + Record.RECORD_SIZE + " bytes");

            // Total number of records stored.
            System.out.println("Total number of records stored: " + (recordID-1));

            System.out.println("Number of records stored per block: " + Block.RECORDS_PER_BLOCK);

            System.out.println("Total number of blocks used for storing the data: " + disk.getBlockCounter());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            // Close resources safely
            try {
                if (scanner != null) {
                    scanner.close();
                }
                if (disk != null) {
                    disk.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
