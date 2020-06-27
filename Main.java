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

        br.close();

        // Send messages
        while (true) {

            for (Node node : nodeList) {
                // Send HI message
                String helloFromMessage = "Hello from " + myId;
                text = helloFromMessage.getBytes();
                InetAddress address = InetAddress.getByName(node.address);
                packtBytes = node.address.getBytes();
                DatagramPacket pacote = new DatagramPacket(text, text.length, address, node.port);
                // //InetSocketAddress address = new InetSocketAddress("127.0.0.1", 4500);
                socket.send(pacote);
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

			} catch (InterruptedException e) {
                //System.out.print(".");
                socket.close();
			}
        }
    }
}