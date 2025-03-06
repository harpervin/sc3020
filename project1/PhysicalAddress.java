import java.io.Serializable;

class PhysicalAddress implements Serializable{
    private Block block;
    private int blockID;
    private int recordindex;

    public PhysicalAddress(Block block, int recordindex) {
        this.block = block;
        this.blockID = block.getBlockID();
        this.recordindex = recordindex;
    }

    public Block getBlock() {
        return block;
    }

    public int getBlockNumber() {
        return blockID;
    }

    public int getIndex() {
        return recordindex;
    }

    @Override
    public String toString() {
        return "Block: " + blockID + ", record index: " + recordindex;
    }
}
