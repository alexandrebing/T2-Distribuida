import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length != 2) {
            System.out.println("Uso: java Main localhost <PORT>");
            return;
        }
        int myPort = Integer.parseInt(args[1]);
        int myId = 0;

        boolean isCoordinator = false;
        int coordinatorPort = 0;

        ArrayList<Node> nodeList = new ArrayList<Node>();

        File file = new File("config.txt");

        BufferedReader br = new BufferedReader(new FileReader(file));

        byte[] text = new byte[256];
        // cria um socket datagrama
        DatagramSocket socket = new DatagramSocket(myPort);

        byte[] packtBytes = new byte[256];

        String st;
        // Setup list of nodes accoriding to config.txt
        while ((st = br.readLine()) != null) {
            String[] info = st.split(" ");
            if (Integer.parseInt(info[2]) == myPort)
                myId = Integer.parseInt(info[0]);
            Node node = new Node(Integer.parseInt(info[0]), Integer.parseInt(info[2]), info[1]);
            nodeList.add(node);
        }

        if (myId == 7)
            isCoordinator = true;
        coordinatorPort = 4000;

        br.close();

        // Send messages
        while (true) {

            // 1 Node is coordinator and only responds
            if (isCoordinator) {
                // System.out.println("Hey Im coordinator!");
                try {
                    // obtem a resposta
                    DatagramPacket pacoteRecebido = new DatagramPacket(text, text.length);
                    socket.setSoTimeout(500);
                    socket.receive(pacoteRecebido);

                    // mostra a resposta
                    String resposta = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
                    System.out.println(resposta);

                    String answerText = "Coordinator here!";
                    text = answerText.getBytes();
                    InetAddress senderAddress = pacoteRecebido.getAddress();
                    int senderPort = pacoteRecebido.getPort();

                    DatagramPacket pacote = new DatagramPacket(text, text.length, senderAddress, senderPort);
                    socket.send(pacote);

                } catch (IOException e) {
                    System.out.print(".");
                    // socket.close();
                }

            }

            else {
                // 2 Node is not coordinator and sends hello to coordinator
                String helloFromMessage = "Hello from " + myId;
                text = helloFromMessage.getBytes();
                InetAddress address = InetAddress.getByName("localhost");
                DatagramPacket pacote = new DatagramPacket(text, text.length, address, coordinatorPort);
                socket.send(pacote);

                try {
                    // 2.1 Coordinator responde
                    // obtem a resposta
                    text = new byte[256];
                    pacote = new DatagramPacket(text, text.length);
                    socket.setSoTimeout(500);
                    socket.receive(pacote);

                    // mostra a resposta
                    String resposta = new String(pacote.getData(), 0, pacote.getLength());
                    System.out.println(resposta);
                    Thread.currentThread().sleep(2000);

                }
                // 2.2 Coordinator does not respond
                catch (IOException e) {

                    Election(nodeList, socket, myId);
                } catch (InterruptedException e) {
                    socket.close();
                }

            }

        }
    }

    static void Election(ArrayList<Node> nodeList, DatagramSocket socket, int myId) throws IOException {

        byte[] text = new byte[256];
        System.out.println(myId);

        for (Node node : nodeList) {
            // Send HI message
            if (node.ID > myId) {
                String helloFromMessage = "Hello from " + myId;
                text = helloFromMessage.getBytes();
                InetAddress address = InetAddress.getByName(node.address);
                DatagramPacket pacote = new DatagramPacket(text, text.length, address, node.port);
                // //InetSocketAddress address = new InetSocketAddress("127.0.0.1", 4500);
                System.out.println(myId + ": enviei mensagem para " + node.ID);
                socket.send(pacote);
            }

        }

        try {
            // obtem a resposta
            DatagramPacket pacote = new DatagramPacket(text, text.length);
            socket.setSoTimeout(500);
            socket.receive(pacote);

            // mostra a resposta
            String resposta = new String(pacote.getData(), 0, pacote.getLength());
            System.out.println(resposta);
            Thread.currentThread().sleep(2000);

        } catch (IOException e) {
            System.out.println(".");
        } catch (InterruptedException e) {
            socket.close();
        }

    }

}
