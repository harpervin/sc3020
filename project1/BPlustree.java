import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

class Node {
    boolean isLeaf;
    List<Float> keys;
    List<Long> childrenOffsets; // Store child locations on disk for internal nodes
    List<List<PhysicalAddress>> data_pointers; // For leaf nodes
    long nextLeafOffset; // Offset to next leaf node
    long diskOffset;     // Node’s own location in file

    public static final int NODE_SIZE = 4096; // Same as block size

    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.childrenOffsets = new ArrayList<>();
        this.data_pointers = new ArrayList<>();
        this.nextLeafOffset = -1;
        this.diskOffset = -1;
    }

    public List<Float> getKeys() {
        return keys;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(NODE_SIZE);

        try {
            buffer.put((byte) (isLeaf ? 1 : 0)); // 1 for leaf, 0 for internal
            int keyCount = Math.min(keys.size(), (NODE_SIZE - buffer.position()) / Float.BYTES);
            buffer.putInt(keyCount); // Store the correct key count

            System.out.println("Keys being written: " + keys);
            System.out.println("Number of keys in node: " + keyCount);
            System.out.println("===========================================================");
            for (int i = 0; i < keyCount && buffer.remaining() >= Float.BYTES; i++) {
                buffer.putFloat(keys.get(i));
            }

            if (isLeaf) {
                // Use keyCount instead of data_pointers.size() to ensure alignment with keys.
                buffer.putInt(keyCount); // Number of data pointer lists should match keyCount
                for (int i = 0; i < keyCount; i++) {
                    List<PhysicalAddress> addressList = data_pointers.get(i);
                    buffer.putInt(addressList.size()); // Number of addresses for key i
                    for (PhysicalAddress address : addressList) {
                        byte[] addressBytes = address.toBytes();
                        buffer.put(addressBytes);
                    }
                }
            } else {
                buffer.putInt(childrenOffsets.size()); // Number of child nodes
                for (Long childOffset : childrenOffsets) {
                    if (buffer.remaining() < Long.BYTES)
                        break;
                    buffer.putLong(childOffset);
                }
            }

            if (buffer.remaining() >= Long.BYTES) {
                buffer.putLong(nextLeafOffset);
            } else {
                System.err.println("Warning: nextLeafOffset could not be stored.");
            }

        } catch (Exception e) {
            System.err.println("BufferOverflowException: " + e.getMessage());
        }

        return buffer.array();
    }

    public static Node fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);

        try {
            if (buffer.remaining() < 1)
                throw new IOException("Buffer is empty.");

            boolean isLeaf = buffer.get() == 1;
            if (buffer.remaining() < Integer.BYTES)
                throw new IOException("Missing key count.");
            int keyCount = buffer.getInt();

            Node node = new Node(isLeaf);

            for (int i = 0; i < keyCount && buffer.remaining() >= Float.BYTES; i++) {
                node.keys.add(buffer.getFloat());
            }

            if (isLeaf) {
                if (buffer.remaining() < Integer.BYTES)
                    throw new IOException("Missing data pointer count.");
                int dataPointerCount = buffer.getInt();

                for (int i = 0; i < dataPointerCount && buffer.remaining() >= Integer.BYTES; i++) {
                    int addressCount = buffer.getInt();
                    List<PhysicalAddress> addressList = new ArrayList<>();

                    for (int j = 0; j < addressCount && buffer.remaining() >= PhysicalAddress.SIZE; j++) {
                        byte[] addressBytes = new byte[PhysicalAddress.SIZE];
                        buffer.get(addressBytes);
                        addressList.add(PhysicalAddress.fromBytes(addressBytes));
                    }
                    node.data_pointers.add(addressList);
                }
            } else {
                if (buffer.remaining() < Integer.BYTES)
                    throw new IOException("Missing children count.");
                int childrenCount = buffer.getInt();

                for (int i = 0; i < childrenCount && buffer.remaining() >= Long.BYTES; i++) {
                    node.childrenOffsets.add(buffer.getLong());
                }
            }

            if (buffer.remaining() >= Long.BYTES) {
                node.nextLeafOffset = buffer.getLong();
            } else {
                node.nextLeafOffset = -1;
            }

            return node;
        } catch (Exception e) {
            System.err.println("Error reading node: " + e.getMessage());
            return null;
        }
    }
}

class BPlustree {
    private Node root;
    private int number_of_layers;
    private int number_of_nodes;
    private final int n;
    private Disk disk;
    private int blockCounter;

    public BPlustree(int n, Disk disk) {
        this.root = new Node(true);
        this.n = n;
        this.number_of_layers = 0;
        this.number_of_nodes = 0;
        this.disk = disk;
        this.blockCounter = 210;
    }

    public Node getRoot() {
        return this.root;
    }

    public int getNumberOfLayers() {
        return this.number_of_layers;
    }

    public int getNumberOfNodes() {
        return this.number_of_nodes;
    }

    public void loadTreeFromDisk() throws IOException {
        long rootOffset = disk.readBPlusTreeRoot();
        if (rootOffset > 0) {
            this.root = readNodeFromDisk(rootOffset);
            System.out.println("B+ Tree successfully loaded from disk!");
        } else {
            System.out.println("No B+ Tree found on disk.");
        }
    }

    // Write Node to Disk
    public long writeNodeToDisk(Node node) throws IOException {
        int blockID = blockCounter + 1;
        blockCounter++;
        node.diskOffset = (long) blockID * Disk.BLOCK_SIZE; // Convert block ID to offset
        System.out.println("Writing Node at Offset: " + node.diskOffset);
        disk.writeNodeToDisk(node.diskOffset, node.toBytes());
        return node.diskOffset;
    }

    // Read Node from Disk
    public Node readNodeFromDisk(long nodeOffset) throws IOException {
        byte[] nodeData = disk.readNodeFromDisk(nodeOffset);
        return Node.fromBytes(nodeData);
    }

    // Bulk Load B+ Tree
    public void bulk_loading(List<Map.Entry<Float, PhysicalAddress>> data) throws IOException {
        // Step 1: Preprocess to group duplicates by unique key
        List<Map.Entry<Float, List<PhysicalAddress>>> uniqueKeys = new ArrayList<>();
        Float prevKey = null;
        List<PhysicalAddress> addresses = new ArrayList<>();

        for (Map.Entry<Float, PhysicalAddress> entry : data) {
            
            float curKey = entry.getKey();
            if (prevKey != null && Float.compare(curKey, prevKey) == 0) {
                addresses.add(entry.getValue());
            } else {
                if (prevKey != null) {
                    uniqueKeys.add(new AbstractMap.SimpleEntry<>(prevKey, new ArrayList<>(addresses)));
                }
                prevKey = curKey;
                addresses.clear();
                addresses.add(entry.getValue());
            }
        }
        // Add the last unique key group
        if (prevKey != null) {
            uniqueKeys.add(new AbstractMap.SimpleEntry<>(prevKey, new ArrayList<>(addresses)));
        }

        // Step 2: Determine the balanced distribution across leaves
        int totalUniqueKeys = uniqueKeys.size();
        int numLeaves = (int) Math.ceil((double) totalUniqueKeys / n);
        int baseKeysPerLeaf = totalUniqueKeys / numLeaves;
        int remainder = totalUniqueKeys % numLeaves;

        List<Long> leafOffsets = new ArrayList<>();
        int index = 0;

        // Step 3: Create and store balanced leaf nodes
        for (int i = 0; i < numLeaves; i++) {
            int keysForThisLeaf = baseKeysPerLeaf + (i < remainder ? 1 : 0);
            Node leaf = new Node(true);
            
            for (int j = 0; j < keysForThisLeaf; j++) {
                
                Map.Entry<Float, List<PhysicalAddress>> uniqueEntry = uniqueKeys.get(index++);
                leaf.keys.add(uniqueEntry.getKey());
                leaf.data_pointers.add(uniqueEntry.getValue());
            }
            System.out.println("Leaf Node dataptr size: " + leaf.data_pointers.size());
            long nodeOffset = writeNodeToDisk(leaf);
            leafOffsets.add(nodeOffset);
        }

        // Build Parent Nodes
        List<Long> parentOffsets = new ArrayList<>(leafOffsets);
        while (parentOffsets.size() > 1) {
            List<Long> newOffsets = new ArrayList<>();
            for (int i = 0; i < parentOffsets.size(); i += n + 1) {
                Node parent = new Node(false);
                int end = Math.min(i + n + 1, parentOffsets.size());
                for (int j = i; j < end; j++) {
                    parent.childrenOffsets.add(parentOffsets.get(j));
                    if (j > i) {
                        Node child = readNodeFromDisk(parentOffsets.get(j));
                        parent.keys.add(child.keys.get(0)); // Use the first key of the child
                    }
                }
                long parentOffset = writeNodeToDisk(parent);
                newOffsets.add(parentOffset);
            }
            parentOffsets = newOffsets;
            
        }

        // Set the new root and write its offset to disk
        if (!parentOffsets.isEmpty()) {
            long rootOffset = parentOffsets.get(0);
            this.root = readNodeFromDisk(rootOffset);
            disk.writeBPlusTreeRootOffset(rootOffset);
        }
    }
// Corrected method to traverse and check leaf node connectivity using nextLeafOffset
public void check_leaf_connections(Node root) throws IOException {
    // Find the leftmost leaf node
    Node node = root;
    while (!node.isLeaf) {
        node = readNodeFromDisk(node.childrenOffsets.get(0));
    }

    // Traverse the linked leaf nodes using the nextLeafOffset pointer
    while (node.nextLeafOffset != -1) {
        Node nextNode = readNodeFromDisk(node.nextLeafOffset);
        if (nextNode == node) { // Self-reference check
            System.out.println("Error: Leaf node " + node.keys + " points to itself!");
            return;
        }
        
        System.out.println("Current Leaf: " + node.keys);
        System.out.println("Next Leaf: " + nextNode.keys);

        node = nextNode;
    }

    System.out.println("All leaf nodes are properly connected.");
}    
  /**
 * Performs a range query that reports only statistics rather than returning records.
 * Statistics reported:
 *   - The number of index nodes (internal nodes) accessed.
 *   - The number of data blocks (leaf nodes) accessed.
 *   - The average of the FG_PCT_home values (assumed to be stored in the key) 
 *     for all records within the given range.
 *   - The running time (in seconds) of the retrieval process.
 *
 * @param lowerBound the lower bound of the key range (inclusive)
 * @param upperBound the upper bound of the key range (inclusive)
 * @throws IOException if there is an error reading nodes from disk
 */
public void rangeQueryStatistics(double lowerBound, double upperBound) throws IOException {
    // Statistics counters
    int indexNodeAccessCount = 0;
    int dataBlockAccessCount = 0;
    double fgPctHomeSum = 0;
    int recordCount = 0;
    
    // Start timing the retrieval process
    long startTime = System.currentTimeMillis();
    
    // Step 1: Traverse from the root to the leaf node that could contain lowerBound.
    Node current = root;
    while (!current.isLeaf) {
        int i = 0;
        // Find the child pointer to follow based on lowerBound.
        while (i < current.keys.size() && lowerBound >= current.keys.get(i)) {
            i++;
        }
        indexNodeAccessCount++;

        long childOffset = current.childrenOffsets.get(i);
        current = readNodeFromDisk(childOffset);
    }
    
    // At this point, current is the first leaf node that may contain the lowerBound.
    // Step 2: Scan leaf nodes and update statistics.
    while (current != null) {
        for (int i = 0; i < current.keys.size(); i++) {
            float key = current.keys.get(i);
            
            if (key >= lowerBound && key <= upperBound) {
            
                List<PhysicalAddress> address = current.data_pointers.get(i);
                fgPctHomeSum += key;  // Using key as FG_PCT_home value
                for (PhysicalAddress add : address) {
                    try {
                        dataBlockAccessCount ++; 
                        // unique_block_numbers.add(add.getBlockNumber());
                        Record record_to_fetch = disk.retrieveRecordByAddress(add);
                        
                        System.out.println(record_to_fetch);
                    } catch (IOException e) {
                        System.err.println("Error retrieving record: " + e.getMessage());
                    }
                }
                recordCount++;
            } else if (key > upperBound) {
                // Since keys are sorted, if we've passed upperBound, stop scanning this leaf.
                break;
            }
        }
        // If the maximum key in the current leaf exceeds the upperBound, we may not need to continue.
        if (!current.keys.isEmpty() && current.keys.get(current.keys.size() - 1) > upperBound) {
            break;
        }
        // Move to the next leaf node if available.
        if (current.nextLeafOffset == -1) {
            break;
        }
        current = readNodeFromDisk(current.nextLeafOffset);
        indexNodeAccessCount++;  

    }
    
    // End timing the retrieval process.
    long endTime = System.currentTimeMillis();
    double runningTimeSeconds = (endTime - startTime);
    
    // Calculate average FG_PCT_home (if records were found)
    double averageFG_PCT_home = (recordCount > 0) ? fgPctHomeSum / recordCount : 0;
    
    // Report statistics (without returning the records)
    System.out.println("Index nodes accessed: " + indexNodeAccessCount);
    System.out.println("Data blocks accessed: " + dataBlockAccessCount);
    System.out.println("Average FG_PCT_home: " + averageFG_PCT_home);
    System.out.println("Retrieval process running time (ms): " + runningTimeSeconds);
}


}
