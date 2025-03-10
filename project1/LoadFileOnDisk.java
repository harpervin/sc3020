import java.io.*;
import java.util.*;

public class LoadFileOnDisk {
    public static void main(String[] args) throws ClassNotFoundException {
        Disk disk = null;
        Scanner scanner = null;

        // List to store addresses for the B+ tree
        ArrayList<Map.Entry<Float, PhysicalAddress>> listOfAddressPairs = new ArrayList<>();

        try {
            // Initialize disk
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
                if (line.isEmpty()) continue;

                String[] data = line.split("\t", -1);

                // Skip rows with missing fields
                if (data.length != 9 || Arrays.stream(data).anyMatch(String::isEmpty)) {
                    System.out.println("Skipping malformed row: " + Arrays.toString(data));
                    continue;
                }

                try {
                    Record record = new Record(
                            recordID++, data[0],
                            Integer.parseInt(data[1]), Integer.parseInt(data[2]),
                            Float.parseFloat(data[3]), Float.parseFloat(data[4]),
                            Float.parseFloat(data[5]), Integer.parseInt(data[6]),
                            Integer.parseInt(data[7]), Integer.parseInt(data[8])
                    );

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

                } catch (NumberFormatException ignored) {}
            }

            Collections.sort(listOfAddressPairs, Comparator.comparing(Map.Entry::getKey));

            BPlustree tree = new BPlustree(7);
            tree.bulk_loading(listOfAddressPairs);
            tree.serializeTree("bplustree.dat");
            System.out.println("Number of Layers : " + tree.getNumberOfLayers());
            System.out.println("Number of Nodes : " + tree.getNumberOfNodes());
            System.out.println("root : " + tree.getRoot());
            System.out.println("root keys : " + tree.getRoot().keys);

            // Store tree into disk
            File treeFile = new File("bplustree.dat");
            byte[] treeBytes = new byte[(int) treeFile.length()];
            FileInputStream treeFileIn = new FileInputStream(treeFile);
            treeFileIn.read(treeBytes);
            treeFileIn.close();

            RandomAccessFile raf = new RandomAccessFile("disk_storage.dat", "rw");
            long treeOffset = raf.length();
            raf.seek(treeOffset);
            raf.write(treeBytes);
            raf.close();

            long treeLength = treeBytes.length;

            try (DataOutputStream metaOut = new DataOutputStream(new FileOutputStream("metadata.dat"))) {
                metaOut.writeLong(treeOffset);
                metaOut.writeLong(treeLength);
            }

            System.out.println("B+ tree stored at offset: " + treeOffset + " with length: " + treeLength);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (scanner != null) scanner.close();
                if (disk != null) disk.close();
            } catch (IOException ignored) {}
        }
    }
}
