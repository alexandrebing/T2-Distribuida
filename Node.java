public class Node {

    int ID;
    int port;
    String address;
    boolean isConnected = false;
    public Node(int ID, int port, String address){
        this.ID = ID;
        this.port = port;
        this.address = address;
    }

    public void setConnected(){
        this.isConnected = true;
    }
}