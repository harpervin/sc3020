import java.io.Serializable;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.io.IOException;
// class Node {

//     // True for leaf nodes, False for internal nodes
//     boolean isLeaf; 

//     // The keys stored in this node
//     List<Float> keys; 

//     List<List<PhysicalAddress>> data_pointers; 

//     // Children nodes (for internal nodes)
//     List<Node> children; 

//     // Link to the next leaf node

//     Node next; 

//     // Constructor to initialize a node
//     public Node(boolean isLeaf) {
//         this.isLeaf = isLeaf;
//         this.keys = new ArrayList<>();
//         this.data_pointers = new ArrayList<>();
//         this.children = new ArrayList<>();
//         this.next = null;
//     }
// }

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

class Node {
    boolean isLeaf;
    List<Float> keys;
    List<Long> childrenOffsets; // Store child locations on disk
    List<List<PhysicalAddress>> data_pointers; // For leaf nodes
    long nextLeafOffset; // Offset to next leaf node
    long diskOffset; // Node’s own location in file

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
            System.out.println("Number of keys in leaf: " + keyCount);
            System.out.println("===========================================================");
            for (int i = 0; i < keyCount && buffer.remaining() >= Float.BYTES; i++) {
                buffer.putFloat(keys.get(i));
            }

            if (isLeaf) {
                buffer.putInt(data_pointers.size()); // Number of records
                for (List<PhysicalAddress> addressList : data_pointers) {
                    if (buffer.remaining() < Integer.BYTES)
                        break;
                    buffer.putInt(addressList.size()); // Number of addresses per key
                    for (PhysicalAddress address : addressList) {
                        byte[] addressBytes = address.toBytes();
                        if (buffer.remaining() < addressBytes.length)
                            break;
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

    // **Write Node to Disk**
    public long writeNodeToDisk(Node node) throws IOException {
        int blockID = blockCounter + 1;
        blockCounter++;
        node.diskOffset = (long) blockID * Disk.BLOCK_SIZE; // Convert block ID to offset
        System.out.println("Writing Node at Offset: " + node.diskOffset);
        disk.writeNodeToDisk(node.diskOffset, node.toBytes());
        return node.diskOffset;
    }

    // **Read Node from Disk**
    public Node readNodeFromDisk(long nodeOffset) throws IOException {
        byte[] nodeData = disk.readNodeFromDisk(nodeOffset);
        return Node.fromBytes(nodeData);
    }

    // **Bulk Load B+ Tree**
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
            
            long nodeOffset = writeNodeToDisk(leaf);
            leafOffsets.add(nodeOffset);
        }

        // Build Parent Nodes as before...
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

    
        //writing offset
        disk.writeBPlusTreeRootOffset();

        
    }
}    
