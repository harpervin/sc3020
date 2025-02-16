import java.io.IOException;

public class RetrieveData {
    public static void main(String[] args) {

        Disk disk = null;
        try {
            disk = new Disk("disk_storage.dat");

            disk.retrieveBlockData(301);
            System.out.println("----------------------------------");

            disk.retrieveRecord(26552);
        } catch (IOException e) {
            e.printStackTrace();

        }
    }
}
