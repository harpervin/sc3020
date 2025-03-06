import java.io.IOException;
import java.io.RandomAccessFile;

public class TestBPlusTree {
    public static final long ROOT_POINTER_OFFSET = 4194304 - Long.BYTES; // 4194304 - 8

    public static void main(String[] args) {
        Disk disk = null;
        try {
            // Initialize the Disk object with your storage file.
            disk = new Disk("disk_storage.dat");

            // Option 1: If your Disk class provides a method to read the B+ tree root,
            // you can use it directly:
            // long rootOffset = disk.readBPlusTreeRoot();
            //
            // Option 2: Alternatively, read the root offset manually from the file.
            long rootOffset = readRootOffsetManually("disk_storage.dat");
            System.out.println("B+ Tree Root Offset: " + rootOffset);

            // Create the B+ tree object (use your tree order, here 340 as in your example)
            BPlustree tree = new BPlustree(340, disk);

            // Load the root node from disk using the root offset.
            Node root = tree.readNodeFromDisk(rootOffset);
            System.out.println("B+ Tree Root Keys: " + root.getKeys());

            // Optionally, perform a sample range search.
            double lowerBound = 0.600;
            double upperBound = 0.900;
            System.out.println("Performing range search for keys between " + lowerBound + " and " + upperBound);

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (disk != null) {
                try {
                    disk.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    /**
     * Reads the B+ tree root offset from the disk file at the specified offset.
     * This method assumes that the root offset is stored as a long (8 bytes)
     * at position ROOT_POINTER_OFFSET.
     *
     * @param fileName the disk file name (e.g., "disk_storage.dat")
     * @return the long value representing the B+ tree root offset
     * @throws IOException if an I/O error occurs
     */
    private static long readRootOffsetManually(String fileName) throws IOException {
        try (RandomAccessFile raf = new RandomAccessFile(fileName, "r")) {
            raf.seek(ROOT_POINTER_OFFSET);
            return raf.readLong();
        }
    }
}
