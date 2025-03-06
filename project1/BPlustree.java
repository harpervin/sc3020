import java.io.Serializable;
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

            // Debug: Print keys before writing
            System.out.println("Keys being written: " + keys);

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

            // Debug: Print keys after reading
            System.out.println("Keys read: " + node.keys);

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
        List<Long> leafOffsets = new ArrayList<>();
        int key_position_within_node = 0;
        float prevKey = -1;

        // Create and Store Leaf Nodes
        Node curLeaf = new Node(true);
        for (Map.Entry<Float, PhysicalAddress> entry : data) {
            float curKey = entry.getKey();

            if (prevKey == curKey) {
                curLeaf.data_pointers.get(key_position_within_node - 1).add(entry.getValue());
            } else if (key_position_within_node < n) {
                curLeaf.keys.add(curKey);
                curLeaf.data_pointers.add(new ArrayList<>());
                curLeaf.data_pointers.get(key_position_within_node).add(entry.getValue());
                key_position_within_node++;
                prevKey = curKey;
            } else {
                long nodeOffset = writeNodeToDisk(curLeaf); // Store node to disk
                leafOffsets.add(nodeOffset);

                curLeaf = new Node(true);
                curLeaf.keys.add(curKey);
                curLeaf.data_pointers.add(new ArrayList<>());
                curLeaf.data_pointers.get(0).add(entry.getValue());
                key_position_within_node = 1;
                prevKey = curKey;
            }
        }

        if (!curLeaf.keys.isEmpty()) {
            long nodeOffset = writeNodeToDisk(curLeaf);
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
                        parent.keys.add(child.keys.get(0));
                    }
                }

                long parentOffset = writeNodeToDisk(parent);
                newOffsets.add(parentOffset);
            }

            parentOffsets = newOffsets;
        }

        // **Store Root Node**
        root = readNodeFromDisk(parentOffsets.get(0));

        // Before writing the root, verify its keys and offset
        System.out.println("Root Node Keys Before Writing: " + root.keys);
        System.out.println("Root Node Disk Offset Before Writing: " + root.diskOffset);
        long rootOffset = writeNodeToDisk(root);
        System.out.println("Root Offset Successfully Written: " + rootOffset);
        disk.writeBPlusTreeRoot(rootOffset);

    }

}

// class BPlustree {

// // Root node of the tree
// private Node root;
// private int number_of_layers;
// private int number_of_nodes;

// public Node getRoot() {
// return this.root;
// }

// public int getNumberOfLayers() {
// return this.number_of_layers;
// }

// public int getNumberOfNodes() {
// return this.number_of_nodes;
// }

// // Maximum number of keys per node
// private final int n;

// // Constructor to initialize the B+ Tree
// public BPlustree(int n) {
// this.root = new Node(true);
// this.n = n;
// this.number_of_layers = 0;
// this.number_of_nodes = 0;
// }

// public void bulk_loading(List<Map.Entry<Float, PhysicalAddress>> data){
// // Find number of unique keys
// // Create a Set to store unique keys
// Set<Float> uniqueKeys = new HashSet<>();

// // Iterate through the data and add the first element of each array to the
// Set
// for (Map.Entry<Float, PhysicalAddress> entry : data) {
// // System.out.println(entry.getKey());
// uniqueKeys.add(entry.getKey());
// }

// System.out.println("Unique Keys: " + uniqueKeys);

// // Get the count of unique keys
// int uniqueKeyCount = uniqueKeys.size();
// System.out.println("Number of unique keys: " + uniqueKeyCount);

// // Create Leaf Nodes
// int numberOfLeafNodes = (int) Math.floor((double) uniqueKeyCount / this.n) +
// 1;
// System.out.println("Number of leaf nodes: " + numberOfLeafNodes);
// int numberOfKeysLastNode = uniqueKeyCount % this.n;
// System.out.println("Number of keys in last node: " + numberOfKeysLastNode);

// ArrayList<Node> list_of_leafs = new ArrayList<Node>();

// for (int leaf = 0; leaf < numberOfLeafNodes; leaf++) {
// list_of_leafs.add(new Node(true));
// }

// // Set NextLeafNode
// for (int i = 0; i < numberOfLeafNodes - 1; i++) {
// list_of_leafs.get(i).next = list_of_leafs.get(i + 1);
// // System.out.println(i + " next: " + list_of_leafs.get(i).next);
// }

// int key_position_within_node = 0;
// int cur_leaf_index = 0;
// float curKey = 0;
// Node curLeaf = null;
// float prevKey = -10000; // set impossible value

// for (Map.Entry<Float, PhysicalAddress> entry : data) {
// curKey = entry.getKey();
// curLeaf = list_of_leafs.get(cur_leaf_index);

// // Case where current key value is same as previous key (i.e. Duplicates)
// if (prevKey == curKey){
// System.out.println("Entering duplicates");
// System.out.println("Duplicate: " + entry);
// curLeaf.data_pointers.get(key_position_within_node-1).add(entry.getValue());
// // Add address to existing list
// }

// // Case where last node may have too little keys
// // Stop second last node from filling to full
// // Let both nodes have n [number of keys that second last would have taken] +
// Number of keys last node would have taken
// // And divide the sum by 2 to split evenly
// else if (
// cur_leaf_index == numberOfLeafNodes - 2 // We are checking 2nd last node
// && numberOfKeysLastNode < (this.n + 1) / 2
// && key_position_within_node > (Math.ceil((this.n + numberOfKeysLastNode) /
// 2))-1)
// {
// System.out.println("Enter last node condition");
// System.out.println("Key position when entering last node: "+
// key_position_within_node);
// // Move to last leaf node
// cur_leaf_index ++ ;
// curLeaf = list_of_leafs.get(cur_leaf_index);
// // Start from first key position in last node
// key_position_within_node = 0;
// curLeaf.keys.add(key_position_within_node, (float) curKey);
// curLeaf.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new
// key --> new list of addresses
// curLeaf.data_pointers.get(key_position_within_node).add(entry.getValue()); //
// Append that the address to the list
// key_position_within_node++; // Move to next insert position
// prevKey = curKey;
// }

// // Case where the current node not full
// // Fill leaf information (key and data pointers )
// else if (key_position_within_node<n){
// curLeaf.keys.add(key_position_within_node, (float) curKey);
// curLeaf.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new
// key --> new list of addresses
// curLeaf.data_pointers.get(key_position_within_node).add(entry.getValue()); //
// Append that the address to the list
// key_position_within_node++; // Move to next insert position
// prevKey = curKey;
// }

// // Case where current node is full
// else{
// cur_leaf_index++;
// System.out.println("Enter current node full condition");
// System.out.println("Key position when entering current node full condition:
// "+ key_position_within_node);
// curLeaf = list_of_leafs.get(cur_leaf_index); // Move to the next node
// key_position_within_node = 0; // Start from key position 0 in the next node
// curLeaf.keys.add(key_position_within_node, (float) curKey); // Set the
// current key value
// curLeaf.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new
// key --> new list of addresses
// curLeaf.data_pointers.get(key_position_within_node).add(entry.getValue()); //
// Append that the address to the list
// key_position_within_node++; // Move to next insert position
// prevKey = curKey;
// }

// }

// // testing
// for (int i = 0; i < numberOfLeafNodes; i++) {
// System.out.println("Keys and pointers for " + i + "th leaf: " +
// list_of_leafs.get(i).keys + "\n");
// System.out.println("Number of keys in " + i + "th leaf: " +
// list_of_leafs.get(i).keys.size() + "\n");

// }

// int previous_number_of_nodes = numberOfLeafNodes; //set the number of L0
// nodes
// ArrayList<Node> previous_node_list = list_of_leafs; //create the L1 list of
// nodes

// // int current_number_of_nodes = 0;

// // while (current_number_of_nodes > 0)

// // int current_number_of_nodes = (int) Math.ceil((double)
// previous_number_of_nodes / (this.n + 1));
// // ArrayList<Node> list_of_L1_nodes = new ArrayList<Node>(); //create the L1
// list of nodes
// int layer = 1;
// int total_nodes = 0;
// // Create 1st Level Child Nodes
// while(previous_number_of_nodes > 1){

// int current_number_of_nodes = (int) Math.ceil((double)
// previous_number_of_nodes / (this.n + 1));
// ArrayList<Node> current_node_list = new ArrayList<Node>(); //create the L1
// list of nodes

// //Initialise the variables
// key_position_within_node = 0; //initialize the key position within the node
// curKey = 0; //initialize the current key
// int cur_L1_index = 0; //initialize the current L1 index
// int numberOfKeysLastL1Node = (previous_number_of_nodes % (this.n + 1))-1;
// if (numberOfKeysLastL1Node == -1) { //if the last L1 node is full the above
// calculation will give -1
// numberOfKeysLastL1Node = this.n;
// }

// //Initialise L1 Nodes
// for(int childL1 = 0; childL1 < current_number_of_nodes; childL1++) { //create
// the L1 nodes
// current_node_list.add(new Node(false));
// }

// Node curL1 = current_node_list.get(0); //get first L1 node

// for (int leaf = 0; leaf < previous_number_of_nodes; leaf++) { //iterate
// through the leaf nodes

// // Balancing the last two nodes
// if (cur_L1_index == current_number_of_nodes - 2 && numberOfKeysLastL1Node <
// (this.n + 1) / 2 && key_position_within_node > (Math.ceil((this.n +
// numberOfKeysLastL1Node) / 2)) - 1) { //balance the last two nodes
// cur_L1_index++; //move to the next L1 node
// curL1 = current_node_list.get(cur_L1_index); //get the current L1 node
// key_position_within_node = 0; //reset the key position within the node
// curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a
// child to the L1 node

// }

// // To skip key of every first leaf node
// else if (key_position_within_node == 0 && curL1.children.isEmpty() ) { //skip
// the first key
// key_position_within_node = 0; //reset the key position within the node
// curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a
// child to the L1 node
// }

// // Normal Case where the current node not full
// else {
// curKey = previous_node_list.get(leaf).keys.get(0); //get the key of the leaf
// node
// curL1.keys.add(key_position_within_node, (float) curKey); //add the key to
// the L1 node
// curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a
// child to the L1 node
// key_position_within_node++; //move to the next key position
// }

// // Check if the current L1 node is full and move to the next L1 node
// if (key_position_within_node == this.n && leaf < previous_number_of_nodes -
// 1) { //get next L1 node if current L1 node is full
// cur_L1_index++; //move to the next L1 node
// curL1 = current_node_list.get(cur_L1_index); //get the current L1 node
// key_position_within_node = 0; //reset the key position within the node
// }

// }

// for (int i = 0; i < current_number_of_nodes; i++) {
// System.out.println("Keys and children for " + i + "th node @ layer " + layer
// +" : " + current_node_list.get(i).keys + "\n" +
// current_node_list.get(i).children + "\n");

// }

// layer++;

// total_nodes += previous_number_of_nodes;
// previous_number_of_nodes = current_number_of_nodes; //set the number of L0
// nodes
// previous_node_list = current_node_list; //create the L1 list of nodes
// }

// this.root = previous_node_list.get(0); //set the root node
// this.number_of_layers = layer;
// this.number_of_nodes = total_nodes + 1; //add the root node to the total
// number of nodes

// }

// }
