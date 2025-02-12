import java.util.HashMap;
import java.util.Map;

class MappingTable {
    private Map<Integer, PhysicalAddress> recordMap; // Maps Record ID → Physical Address

    public MappingTable() {
        recordMap = new HashMap<>();
    }

    public void addMapping(int recordID, PhysicalAddress address) {
        recordMap.put(recordID, address);
    }

    public PhysicalAddress getAddress(int recordID) {
        return recordMap.get(recordID);
    }

    public void removeMapping(int recordID) {
        recordMap.remove(recordID);
    }

    @Override
    public String toString() {
        return recordMap.toString();
    }
}
