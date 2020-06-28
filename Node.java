public class Node {

    int ID;
    int port;
    String address;
    Boolean coordinator = false;
    public Node(int ID, int port, String address){
        this.ID = ID;
        this.port = port;
        this.address = address;
    }

    public void setCoordinator(boolean isCoordinator){
        this.coordinator = !this.coordinator;
    }
}