import java.io.Serializable;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

public class Record implements Serializable{
    public static final int RECORD_SIZE = 26; // Optimized size: 26 bytes

    private int recordID; // 4 bytes
    private int gameDate; // 4 bytes - Stored as YYYYMMDD (4 bytes)
    private int teamIDHome; // 4 bytes
    private short ptsHome; // 2 bytes
    private short fgPctHome; // 2 bytes - Stored as (fgPct * 1000)
    private short ftPctHome; // 2 bytes - Stored as (ftPct * 1000)
    private short fg3PctHome; // 2 bytes - Stored as (fg3Pct * 1000)
    private short astHome; // 2 bytes
    private short rebHome; // 2 bytes
    private byte homeTeamWins; // 1 byte - 0 or 1
    private byte padding; // 1 byte - Ensures alignment

    public Record(int recordID, String gameDateStr, int teamIDHome, int ptsHome, float fgPctHome,
            float ftPctHome, float fg3PctHome, int astHome, int rebHome, int homeTeamWins) {
        this.recordID = recordID;
        this.gameDate = convertDateToInt(gameDateStr);
        this.teamIDHome = teamIDHome;
        this.ptsHome = (short) ptsHome;
        this.fgPctHome = (short) (fgPctHome * 1000);
        this.ftPctHome = (short) (ftPctHome * 1000);
        this.fg3PctHome = (short) (fg3PctHome * 1000);
        this.astHome = (short) astHome;
        this.rebHome = (short) rebHome;
        this.homeTeamWins = (byte) homeTeamWins;
        this.padding = 0; // Ensure 2-byte alignment
    }

    public static int getRecordSize() {
        return RECORD_SIZE;
    }

    public int getGameDate() {
        return gameDate;
    }

    public int getTeamIDHome() {
        return teamIDHome;
    }

    public short getPtsHome() {
        return ptsHome;
    }

    public short getFgPctHome() {
        return fgPctHome;
    }

    public short getFtPctHome() {
        return ftPctHome;
    }

    public short getFg3PctHome() {
        return fg3PctHome;
    }

    public short getAstHome() {
        return astHome;
    }

    public short getRebHome() {
        return rebHome;
    }

    public byte getHomeTeamWins() {
        return homeTeamWins;
    }

    public byte[] toBytes() {
        ByteBuffer buffer = ByteBuffer.allocate(RECORD_SIZE).order(ByteOrder.LITTLE_ENDIAN);
        buffer.putInt(recordID);
        buffer.putInt(gameDate);
        buffer.putInt(teamIDHome);
        buffer.putShort(ptsHome);
        buffer.putShort(fgPctHome);
        buffer.putShort(ftPctHome);
        buffer.putShort(fg3PctHome);
        buffer.putShort(astHome);
        buffer.putShort(rebHome);
        buffer.put(homeTeamWins);
        buffer.put(padding);
        return buffer.array();
    }

    public static Record fromBytes(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
        int recordID = buffer.getInt();
        int gameDate = buffer.getInt();
        int teamIDHome = buffer.getInt();
        short ptsHome = buffer.getShort();
        short fgPctHome = buffer.getShort();
        short ftPctHome = buffer.getShort();
        short fg3PctHome = buffer.getShort();
        short astHome = buffer.getShort();
        short rebHome = buffer.getShort();
        byte homeTeamWins = buffer.get();

        return new Record(recordID, convertIntToDate(gameDate), teamIDHome, ptsHome,
                fgPctHome / 1000.0f, ftPctHome / 1000.0f, fg3PctHome / 1000.0f,
                astHome, rebHome, homeTeamWins);
    }

    private static int convertDateToInt(String date) {
        String[] parts = date.split("/");
        if (parts.length == 3) {
            return Integer.parseInt(parts[2] + String.format("%02d", Integer.parseInt(parts[1]))
                    + String.format("%02d", Integer.parseInt(parts[0])));
        }
        return 0; // Return default if parsing fails
    }

    private static String convertIntToDate(int dateInt) {
        int year = dateInt / 10000;
        int month = (dateInt / 100) % 100;
        int day = dateInt % 100;
        return String.format("%d/%02d/%02d", day, month, year);
    }

    @Override
    public String toString() {
        return String.format(
                "Record { ID=%d, Date=%s, TeamID=%d, Pts=%d, FG%%=%.3f, FT%%=%.3f, FG3%%=%.3f, AST=%d, REB=%d, HOME_TEAM_WINS=%d }",
                recordID, convertIntToDate(gameDate), teamIDHome, ptsHome,
                fgPctHome / 1000.0f, ftPctHome / 1000.0f, fg3PctHome / 1000.0f,
                astHome, rebHome, homeTeamWins);
    }

    public int getRecordID() {
        return recordID;
    }

}
