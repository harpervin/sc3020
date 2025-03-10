import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

class Node implements Serializable{
    // True for leaf nodes, False for internal nodes
    boolean isLeaf; 

    // The keys stored in this node
    List<Float> keys; 

    List<List<PhysicalAddress>> data_pointers; 

    // Children nodes (for internal nodes)
    List<Node> children; 

    // Link to the next leaf node
    Node next; 

    // Constructor to initialize a node
    public Node(boolean isLeaf) {
        this.isLeaf = isLeaf;
        this.keys = new ArrayList<>();
        this.data_pointers = new ArrayList<>();
        this.children = new ArrayList<>();
        this.next = null;
    }
}

class BPlustree implements Serializable {
    // Root node of the tree
    private Node root;
    private int number_of_layers;
    private int number_of_nodes;    

    public Node getRoot() {
        return this.root;
    }

    public int getNumberOfLayers() {
        return this.number_of_layers;
    }

    public int getNumberOfNodes() {
        return this.number_of_nodes;
    }
  
    // Maximum number of keys per node
    private final int n; 

    // Constructor to initialize the B+ Tree
    public BPlustree(int n) {
        this.root = new Node(true);
        this.n = n;
        this.number_of_layers = 0;
        this.number_of_nodes = 0;
    }
    
    public void serializeTree(String fileName) throws IOException {
        try (FileOutputStream fileOut = new FileOutputStream(fileName);
                ObjectOutputStream out = new ObjectOutputStream(fileOut)) {
            out.writeObject(this);
        }
    }

    public static BPlustree deserializeTree(String fileName) throws IOException, ClassNotFoundException {
        BPlustree tree;
        try (FileInputStream fileIn = new FileInputStream(fileName);
                ObjectInputStream in = new ObjectInputStream(fileIn)) {
            tree = (BPlustree) in.readObject();
        }
        return tree;
    }

    public void bulk_loading(List<Map.Entry<Float, PhysicalAddress>> data){
        
        // Check for unique leaf key values
        Set<Float> unique_leaf_values = new HashSet<>();
        for (Map.Entry<Float, PhysicalAddress> entry : data) {
            // System.out.println(entry.getKey());
            unique_leaf_values.add(entry.getKey());
        }

        System.out.println("Unique leaf values: " + unique_leaf_values);

        // Get the count of unique keys
        int num_of_unique_leaf_keys = unique_leaf_values.size();
        System.out.println("Number of unique leaf values: " + num_of_unique_leaf_keys);


        // Create Leaf Nodes
        int leaf_node_count = (int) Math.floor((double) num_of_unique_leaf_keys / this.n) + 1;
        System.out.println("Number of leaf nodes: " + leaf_node_count);
        int last_leaf_num_of_keys = num_of_unique_leaf_keys % this.n;
        System.out.println("Number of keys in last node: " + last_leaf_num_of_keys);

        ArrayList<Node> list_of_leafs = new ArrayList<Node>();

        for (int leaf = 0; leaf < leaf_node_count; leaf++) {
            list_of_leafs.add(new Node(true));
        }

        // Set NextLeafNode
        for (int i = 0; i < leaf_node_count - 1; i++) {
            list_of_leafs.get(i).next = list_of_leafs.get(i + 1);
            // System.out.println(i + " next: " + list_of_leafs.get(i).next);
        }

        int key_position_within_node = 0, cur_leaf_index = 0;
        float cur_key_value = 0;
        Node cur_leaf_node = null; 
        float previous_key_check = -99999; 
        
        for (Map.Entry<Float, PhysicalAddress> entry : data) {
            cur_key_value = entry.getKey();
            cur_leaf_node = list_of_leafs.get(cur_leaf_index);
        
            // Case where current key value is same as previous key (i.e. Duplicates)
            if (previous_key_check == cur_key_value){
                // System.out.println("Entering duplicates");
                // System.out.println("Duplicate: " + entry);
                cur_leaf_node.data_pointers.get(key_position_within_node-1).add(entry.getValue()); // Add address to existing list
            }

            // Case where last node may have too little keys 
            // Stop second last node from filling to full 
            // Let both nodes have n [number of keys that second last would have taken] + Number of keys last node would have taken 
            // And divide the sum by 2 to split evenly 
            else if (
                cur_leaf_index == leaf_node_count - 2  // We are checking 2nd last node 
                && last_leaf_num_of_keys < (this.n + 1) / 2
                && key_position_within_node > (Math.ceil((this.n + last_leaf_num_of_keys) / 2))-1)
            {
                System.out.println("Enter last node condition");
                System.out.println("Key position when entering last node: "+ key_position_within_node);
                // Move to last leaf node 
                cur_leaf_index ++ ; 
                cur_leaf_node = list_of_leafs.get(cur_leaf_index);
                // Start from first key position in last node
                key_position_within_node = 0;
                cur_leaf_node.keys.add(key_position_within_node, (float) cur_key_value);
                cur_leaf_node.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new key --> new list of addresses
                cur_leaf_node.data_pointers.get(key_position_within_node).add(entry.getValue()); // Append that the address to the list
                key_position_within_node++; // Move to next insert position
                previous_key_check = cur_key_value;
            }
                

            // Case where the current node not full
            // Fill leaf information (key and data pointers )
            else if (key_position_within_node<n){
                cur_leaf_node.keys.add(key_position_within_node, (float) cur_key_value);
                cur_leaf_node.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new key --> new list of addresses
                cur_leaf_node.data_pointers.get(key_position_within_node).add(entry.getValue()); // Append that the address to the list
                key_position_within_node++; // Move to next insert position
                previous_key_check = cur_key_value;
            }
            
            // Case where current node is full 
            else{
                cur_leaf_index++; 
                System.out.println("Enter current node full condition");
                System.out.println("Key position when entering current node full condition: "+ key_position_within_node);
                cur_leaf_node = list_of_leafs.get(cur_leaf_index); // Move to the next node
                key_position_within_node = 0; // Start from key position 0 in the next node
                cur_leaf_node.keys.add(key_position_within_node, (float) cur_key_value); // Set the current key value 
                cur_leaf_node.data_pointers.add(new ArrayList<PhysicalAddress>()); // Everytime new key --> new list of addresses
                cur_leaf_node.data_pointers.get(key_position_within_node).add(entry.getValue()); // Append that the address to the list
                key_position_within_node++; // Move to next insert position
                previous_key_check = cur_key_value;
            }

        }

        // testing
        for (int i = 0; i < leaf_node_count; i++) {
            System.out.println("Keys and pointers for " + i + "th leaf: " + list_of_leafs.get(i).keys + "\n");
            System.out.println("Number of keys in " + i + "th leaf: " + list_of_leafs.get(i).keys.size() + "\n");

        }

        int previous_number_of_nodes = leaf_node_count; //set the number of L0 nodes
        ArrayList<Node> previous_node_list = list_of_leafs; //create the L1 list of nodes

        // int current_number_of_nodes = 0;
        
        
        // while (current_number_of_nodes > 0)

        // int current_number_of_nodes = (int) Math.ceil((double) previous_number_of_nodes / (this.n + 1));
        // ArrayList<Node> list_of_L1_nodes = new ArrayList<Node>(); //create the L1 list of nodes
        int layer = 1;
        int total_nodes = 0;
        // Create 1st Level Child Nodes
        while(previous_number_of_nodes > 1){

            int current_number_of_nodes = (int) Math.ceil((double) previous_number_of_nodes / (this.n + 1));
            ArrayList<Node> current_node_list = new ArrayList<Node>(); //create the L1 list of nodes
            
            //Initialise the variables
            key_position_within_node = 0; //initialize the key position within the node
            cur_key_value = 0; //initialize the current key
            int cur_L1_index = 0; //initialize the current L1 index
            int numberOfKeysLastL1Node = (previous_number_of_nodes % (this.n + 1))-1;
            if (numberOfKeysLastL1Node == -1) { //if the last L1 node is full the above calculation will give -1
                numberOfKeysLastL1Node = this.n;
            } 

            //Initialise L1 Nodes
            for(int childL1 = 0; childL1 < current_number_of_nodes; childL1++) { //create the L1 nodes
                current_node_list.add(new Node(false));
            }

            Node curL1 = current_node_list.get(0); //get first L1 node

            for (int leaf = 0; leaf < previous_number_of_nodes; leaf++) { //iterate through the leaf nodes
                
                // Balancing the last two nodes
                if (cur_L1_index == current_number_of_nodes - 2 && numberOfKeysLastL1Node < (this.n + 1) / 2 && key_position_within_node > (Math.ceil((this.n + numberOfKeysLastL1Node) / 2)) - 1) { //balance the last two nodes
                    cur_L1_index++; //move to the next L1 node
                    curL1 = current_node_list.get(cur_L1_index); //get the current L1 node
                    key_position_within_node = 0; //reset the key position within the node
                    curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a child to the L1 node
                    
                }
                
                // To skip key of every first leaf node
                else if (key_position_within_node == 0 && curL1.children.isEmpty() ) { //skip the first key
                    key_position_within_node = 0; //reset the key position within the node
                    curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a child to the L1 node
                }
                
                // Normal Case where the current node not full
                else {
                    cur_key_value = previous_node_list.get(leaf).keys.get(0); //get the key of the leaf node
                    curL1.keys.add(key_position_within_node, (float) cur_key_value); //add the key to the L1 node
                    curL1.children.add(previous_node_list.get(leaf)); //add the leaf node as a child to the L1 node
                    key_position_within_node++; //move to the next key position
                }
                
                // Check if the current L1 node is full and move to the next L1 node
                if (key_position_within_node == this.n && leaf < previous_number_of_nodes - 1) { //get next L1 node if current L1 node is full
                    cur_L1_index++; //move to the next L1 node
                    curL1 = current_node_list.get(cur_L1_index); //get the current L1 node
                    key_position_within_node = 0; //reset the key position within the node
                }
                
            }
            

            for (int i = 0; i < current_number_of_nodes; i++) {
                System.out.println("Keys and children for " + i + "th node @ layer " + layer +" : "  + current_node_list.get(i).keys + "\n" + current_node_list.get(i).children + "\n");

            }

            layer++;

            total_nodes += previous_number_of_nodes;
            previous_number_of_nodes = current_number_of_nodes; //set the number of L0 nodes
            previous_node_list = current_node_list; //create the L1 list of nodes
        }
        
        this.root = previous_node_list.get(0); //set the root node
        this.number_of_layers = layer;
        this.number_of_nodes = total_nodes + 1; //add the root node to the total number of nodes

    }

    public void check_leaf_connections(Node root) {
        // Find the leftmost leaf node
        Node node = root;
        while (!node.isLeaf) {
            node = node.children.get(0); // Traverse down to the first leaf
        }
    
        // Traverse the linked leaf nodes using the `next` pointer
        while (node.next != null) {
            if (node.next == node) { // Self-reference check
                System.out.println("Error: Leaf node " + node.keys + " points to itself!");
                return;
            }
            
            System.out.println("Current Leaf: " + node.keys);
            System.out.println("Next Leaf: " + node.next.keys);
    
            node = node.next; // Move to the next leaf node
        }
    
        System.out.println("All leaf nodes are properly connected.");
    }    


    public void search_range(double lower, double higher, Node root, Disk disk) {
        float sum = 0; // to store the total value of FG_PCT_Home
        int count = 0; // to store the total number of records found
        int index_accesses = 0; // at root --> start w 1
        int data_block_accesses = 0;
        HashSet<Integer> unique_block_numbers = new HashSet<>();

        // Move down to the leaf level
        while (!root.isLeaf) {
            int i = 0;
            while (i < root.keys.size() && lower > root.keys.get(i)) {
                i++;
            }
            root = root.children.get(i); 
            index_accesses += 1; 
        }
    
        // Process leaf nodes
        while (root != null && root.isLeaf) {  
            for (int j = 0; j < root.keys.size(); j++) {
                if (root.keys.get(j) >= lower && root.keys.get(j) <= higher) {
                    List<PhysicalAddress> address = root.data_pointers.get(j);
                    // System.out.println("Address of record to fetch: " + address);
                    for (PhysicalAddress add : address) {
                        try {
                            data_block_accesses += 1;
                            unique_block_numbers.add(add.getBlockNumber());
                            Record record_to_fetch = disk.retrieveRecordByAddress(add);
                            sum += record_to_fetch.getFgPctHome();
                            count += 1;

                            // System.out.println("Found record in range search: " + record_to_fetch);
                        } catch (IOException e) {
                            System.err.println("Error retrieving record: " + e.getMessage());
                        }
                    }

                } else if (root.keys.get(j) > higher) {
                    System.out.println("Exceed upper bound!");
                    break;  
                }
            }
            // System.out.println("************* GOING NEXT *******************");
            // System.out.println("root.next: " + root.next);
            root = root.next;
            index_accesses += 1;
        }

        if (count > 0) {
            float avg = sum / count / 1000;
            System.out.println("Average: " + avg);
            System.out.println("Total records found: " + count);
            System.out.println("Number of index node accessed: " + index_accesses);
            // System.out.println("Number of data blocks accessed: " + data_block_accesses);
            System.out.println("Number of blocks accessed: " +
                    unique_block_numbers.size());
        } else {
            System.out.println("No records found in the given range.");
        }
    }

}
