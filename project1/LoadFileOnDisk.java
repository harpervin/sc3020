import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Map;
import java.util.Scanner;
import java.util.AbstractMap;

public class LoadFileOnDisk {
    public static void main(String[] args) throws ClassNotFoundException {
        Disk disk = null;
        Scanner scanner = null;

        // Create List of Addresses
        ArrayList<Map.Entry<Float, PhysicalAddress>> listOfAddressPairs = new ArrayList<>();

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

                String[] data = line.split("\t", -1); // Preserve empty values if present

                // Skip rows if any cell is empty
                if (java.util.Arrays.stream(data).anyMatch(String::isEmpty)) {
                    System.out.println("Skipping row with empty values: " + java.util.Arrays.toString(data));
                    continue;
                }

                if (data.length != 9) { // Skip rows with missing fields
                    System.out.println("Skipping malformed row: " + java.util.Arrays.toString(data));
                    continue;
                }

                System.out.println("Valid row: " + java.util.Arrays.toString(data));

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
                    // key for the btree
                    Float fgPctHome = Float.parseFloat(data[3]);
                    // Store the Record in the current Block.


                    if (!block.isFull()) { // Block is not full

                        PhysicalAddress address = block.addRecord(record);
                        // create the list of address + key for each record store in disk
                        listOfAddressPairs.add(new AbstractMap.SimpleEntry<>(fgPctHome, address));
                        // use to build the btree
                        // System.err.println(address);
                    }
                    // If the block is full, write it to disk and start a new one.

                    else {
                        disk.writeBlock(block); // Write full block to disk
                        blockID = disk.findAvailableBlock(); // Get next available block ID
                        System.out.println("Previous block full, added new block ID: " + blockID);
                        block = new Block(blockID); // Create a new block
                        PhysicalAddress address = block.addRecord(record);
                        System.err.println(address);// Add record to the new block}
                        listOfAddressPairs.add(new AbstractMap.SimpleEntry<>(fgPctHome, address));

                    }

                } catch (NumberFormatException e) {
                    // System.out.println("Skipping row with invalid data: " + line);
                }
            }

            // check if addresspair is correct
            // System.out.println("list of address pair"+listOfAddressPairs);
            Collections.sort(listOfAddressPairs, new Comparator<Map.Entry<Float, PhysicalAddress>>() {
                @Override
                public int compare(Map.Entry<Float, PhysicalAddress> entry1, Map.Entry<Float, PhysicalAddress> entry2) {
                    return entry1.getKey().compareTo(entry2.getKey());  // Compare by Float value
                }
            });

            BPlustree tree = new BPlustree(7);
            tree.bulk_loading(listOfAddressPairs);
            
            System.out.println("Number of Layers : " + tree.getNumberOfLayers());
            System.out.println("Number of Nodes : " + tree.getNumberOfNodes());
            System.out.println("root : " + tree.getRoot());
            System.out.println("root keys : " + tree.getRoot().keys);
            tree.serializeTree("bplustree.dat");
            tree = BPlustree.deserializeTree("bplustree.dat");

            tree.check_leaf_connections(tree.getRoot());

            tree.search_range(0.600, 0.900, tree.getRoot(), disk);

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
            System.out.println("Total number of records stored: " + (recordID - 1));

            System.out.println("Number of records stored per block: " + Block.RECORDS_PER_BLOCK);

            System.out.println("Total number of blocks used for storing the data: " + disk.getBlockCounter());
            // Read the bplustree.dat file into a byte array
FileInputStream treeFileIn = new FileInputStream("bplustree.dat");
byte[] treeBytes = treeFileIn.readAllBytes();
treeFileIn.close();

// Open disk_storage.dat and append the tree data
RandomAccessFile raf = new RandomAccessFile("disk_storage.dat", "rw");
long treeOffset = raf.length();  // This is where the tree data will be stored
raf.seek(treeOffset);
raf.write(treeBytes);
raf.close();

// Now, store treeOffset and treeBytes.length in your metadata for later access
System.out.println("B+ tree stored at offset: " + treeOffset + " with length: " + treeBytes.length);


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
