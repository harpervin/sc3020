import java.nio.ByteBuffer;

/**
 * Attribute Sizes in Memory (Binary Representation)
 * We set attributes as fixed-length fields
 * 
 * | Attribute    | Data Type  | Size (Bytes) | Fixed / Variable |
 * |--------------|------------|--------------|------------------|
 * | recordID     | int        | 4            | Fixed     |
 * | gameDate     | String     | 10           | Fixed     |
 * | teamIDHome   | int        | 4            | Fixed     |
 * | ptsHome      | int        | 4            | Fixed     |
 * | fgPctHome    | float      | 4            | Fixed     |
 * | ftPctHome    | float      | 4            | Fixed     |
 * | fg3PctHome   | float      | 4            | Fixed     |
 * | astHome      | int        | 4            | Fixed     |
 * | rebHome      | int        | 4            | Fixed     |
 * | homeTeamWins | int        | 4            | Fixed     |
 * | Total        |            | 46 bytes     |           |
 */

public class Record {
    public static final int RECORD_HEADER_SIZE = 4;  // Stores Record ID
    public static final int RECORD_SIZE = 46;  // 46 Bytes

    private int recordID;
    private String gameDate;
    private int teamIDHome;
    private int ptsHome;
    private float fgPctHome;
    private float ftPctHome;
    private float fg3PctHome;
    private int astHome;
    private int rebHome;
    private int homeTeamWins;

    public Record(int recordID, String gameDate, int teamIDHome, int ptsHome, float fgPctHome,
                  float ftPctHome, float fg3PctHome, int astHome, int rebHome, int homeTeamWins) {
        this.recordID = recordID;
        this.gameDate = gameDate;
        this.teamIDHome = teamIDHome;
        this.ptsHome = ptsHome;
        this.fgPctHome = fgPctHome;
        this.ftPctHome = ftPctHome;
        this.fg3PctHome = fg3PctHome;
        this.astHome = astHome;
        this.rebHome = rebHome;
        this.homeTeamWins = homeTeamWins;
    }

    public int getRecordID() {
        return recordID;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE);
        buffer.putInt(recordID); // **Record header (Record ID)**
        buffer.put(gameDate.getBytes());
        buffer.putInt(teamIDHome);
        buffer.putInt(ptsHome);
        buffer.putFloat(fgPctHome);
        buffer.putFloat(ftPctHome);
        buffer.putFloat(fg3PctHome);
        buffer.putInt(astHome);
        buffer.putInt(rebHome);
        buffer.putInt(homeTeamWins);
        return buffer.array();
    }

    public static Record fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes);
        int recordID = buffer.getInt();  // Read Record Header (Record ID)
        byte[] dateBytes = new byte[10];
        buffer.get(dateBytes);
        String gameDate = new String(dateBytes).trim();
        int teamIDHome = buffer.getInt();
        int ptsHome = buffer.getInt();
        float fgPctHome = buffer.getFloat();
        float ftPctHome = buffer.getFloat();
        float fg3PctHome = buffer.getFloat();
        int astHome = buffer.getInt();
        int rebHome = buffer.getInt();
        int homeTeamWins = buffer.getInt();

        return new Record(recordID, gameDate, teamIDHome, ptsHome, fgPctHome, ftPctHome, fg3PctHome, astHome, rebHome, homeTeamWins);
    }
}
