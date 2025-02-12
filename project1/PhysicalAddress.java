class PhysicalAddress {
    private int blockNumber;
    private int offset;

    public PhysicalAddress(int blockNumber, int offset) {
        this.blockNumber = blockNumber;
        this.offset = offset;  // Record offset within block
    }

    public int getBlockNumber() {
        return blockNumber;
    }

    public int getOffset() {
        return offset;
    }

    @Override
    public String toString() {
        return "Block: " + blockNumber + ", Offset: " + offset;
    }
}
