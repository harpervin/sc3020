import java.io.*;
import java.util.*;

public class LoadFileOnDisk {
    public static void main(String[] args) throws ClassNotFoundException {
        Disk disk = null;
        Scanner scanner = null;
        ArrayList<Map.Entry<Float, PhysicalAddress>> listOfAddressPairs = new ArrayList<>();

        try {
            // Initialize Disk (Binary File Storage)
            disk = new Disk("disk_storage.dat");

            // Read games.txt file
            scanner = new Scanner(new File("games.txt"));

            // Skip the first line (header)
            if (scanner.hasNextLine()) {
                System.out.println("Skipping header: " + scanner.nextLine());
            }

            int recordID = 1;
            int blockID = disk.findAvailableBlock();
            Block block = new Block(blockID);

            while (scanner.hasNextLine()) {
                String line = scanner.nextLine().trim();
                if (line.isEmpty()) continue; // Skip empty lines

                String[] data = line.split("\t", -1); // Preserve empty values if present

                // Skip rows with empty values or incorrect field count
                if (Arrays.stream(data).anyMatch(String::isEmpty) || data.length != 9) {
                    System.out.println("Skipping malformed row: " + Arrays.toString(data));
                    continue;
                }

                try {
                    Record record = new Record(
                            recordID++,
                            data[0], Integer.parseInt(data[1]), Integer.parseInt(data[2]),
                            Float.parseFloat(data[3]), Float.parseFloat(data[4]),
                            Float.parseFloat(data[5]), Integer.parseInt(data[6]),
                            Integer.parseInt(data[7]), Integer.parseInt(data[8])
                    );

                    // Key for B+ Tree
                    Float fgPctHome = Float.parseFloat(data[3]);

                    if (!block.isFull()) {
                        PhysicalAddress address = block.addRecord(record);
                        listOfAddressPairs.add(new AbstractMap.SimpleEntry<>(fgPctHome, address));
                    } else {
                        disk.writeBlock(block);
                        blockID = disk.findAvailableBlock();
                        block = new Block(blockID);
                        PhysicalAddress address = block.addRecord(record);
                        listOfAddressPairs.add(new AbstractMap.SimpleEntry<>(fgPctHome, address));
                    }

                } catch (NumberFormatException e) {
                    System.out.println("Skipping row with invalid data: " + line);
                }
            }

            // Write the last block if it contains records
            if (!block.getRecords().isEmpty()) {
                disk.writeBlock(block);
            }

            // Sort addresses before bulk-loading the B+ Tree
            listOfAddressPairs.sort(Map.Entry.comparingByKey());

            // **Build & Store B+ Tree in Disk**
            BPlustree tree = new BPlustree(340, disk); // Pass disk to tree constructor
            tree.bulk_loading(listOfAddressPairs);
            System.out.println("B+ Tree built successfully!");
            System.out.println("Root Keys: " + tree.getRoot().getKeys());

            // **Report Statistics**
            System.out.println("Total Records: " + (recordID - 1));
            System.out.println("Records per Block: " + Block.RECORDS_PER_BLOCK);
            System.out.println("Total Blocks Used: " + disk.getBlockCounter());

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (scanner != null) scanner.close();
                if (disk != null) {
                    System.out.println("Closing disk...");
                    disk.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
