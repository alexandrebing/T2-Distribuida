import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Main {

    public static void main(String[] args) throws FileNotFoundException, IOException {
        if (args.length != 2) {
            System.out.println("Uso: java Main localhost <PORT>");
            return;
        }

        int myId = 0;
        int coordinatorPort = 0;
        boolean isCoordinator = false;
        ArrayList<Node> nodeList = new ArrayList<Node>();
        File file = new File("config.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        byte[] text = new byte[256];
        int maiorID = 0;
        String aux;


        long start = System.currentTimeMillis();
        long end = start + 25*1000; // 10 seconds * 1000 ms/sec

        //recebe a porta passada por parametro java localhost 3000
        int myPort = Integer.parseInt(args[1]); //3000
        // cria um socket datagrama
        DatagramSocket socket = new DatagramSocket(myPort);

       
        
        // Setup list of nodes accoriding to config.txt
        while ((aux = br.readLine()) != null) {
            String[] info = aux.split(" ");

            if (Integer.parseInt(info[2]) == myPort)
                myId = Integer.parseInt(info[0]);
            Node node = new Node(Integer.parseInt(info[0]), Integer.parseInt(info[2]), info[1]);

            if (node.ID > maiorID) {
                maiorID = node.ID;
                coordinatorPort = node.port;
            }
            nodeList.add(node);
        }   
        

        if (myId == maiorID){
             for (Node n : nodeList) {
                 n.coordinator = true;
             }
            System.out.println("sou o maior" + maiorID);
            isCoordinator = true;
        }
        

        br.close();

       
        // Send messages
        while (true) {

            // 1 Node is coordinator and only responds
            //condicao com que faz que o coordenador continue conectado por x tempo
            if (isCoordinator) {
            while (System.currentTimeMillis() < end)
                    {

                        
                            try {
                                // obtem a resposta
                                DatagramPacket pacoteRecebido = new DatagramPacket(text, text.length);
                                socket.setSoTimeout(500);
                                socket.receive(pacoteRecebido);
            
                                // mostra a resposta
                                String resposta = new String(pacoteRecebido.getData(), 0, pacoteRecebido.getLength());
                                System.out.println(resposta);
            
                                String answerText = "*Coordinator aqui - myId: " + myId;
                                text = answerText.getBytes();
                                InetAddress senderAddress = pacoteRecebido.getAddress();
                                int senderPort = pacoteRecebido.getPort();
            
                                DatagramPacket pacote = new DatagramPacket(text, text.length, senderAddress, senderPort);
                                socket.send(pacote);
                                
                            } catch (IOException e) {
                                System.out.print("."+end);
                                // socket.close();
                            }
            
                        }
                    }
                    if (isCoordinator) {
                        System.out.println("\ndesconectado coordenador");
                        socket.close();
                        System.exit(0);
                    }

            //  MARK nao sou o coordenador
            if (!isCoordinator) {
                
                // 2 Node is not coordinator and sends hello to coordinator
                String helloFromMessage = "Ola coord sou o id -  " + myId;
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
                    System.out.println("coordenador caiu, iniciando nova eleicao");
                    //nao consegui mandar para o coordenador
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
        int maiorIDEleicao = 0;
        for (Node node : nodeList) {
            // Send HI message
            if (node.ID > myId) {
                if (node.ID > maiorIDEleicao)
                    maiorIDEleicao = node.ID;
                    
                String helloFromMessage = "Oi quero ser o coordenador mas vc é maior que eu, vc ta ai? " + myId;
                text = helloFromMessage.getBytes();
                InetAddress address = InetAddress.getByName(node.address);
                DatagramPacket pacote = new DatagramPacket(text, text.length, address, node.port);

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
            System.out.println("esperando resposta de alguem");
        } catch (InterruptedException e) {
            socket.close();
        }

    }

}
/*  1- [x] Programa que faz leitura do arquivo txt e manda mensagem de “Hello” para todos 
    2- [x] Programa que seta uma das instâncias (id 0?) como host, 
    e as demais mandam mensagem apenas para ele, e ele só responde (Ping/pong)

    3- [x] Fazer função que desconecta o host após 10s e fazer com que os demais “percebam” a queda
    4- [x] Fazer função que, uma vez desconectado o host, faz cada cliente mandar
     o seu ID para cada cliente com id maior que o seu

    5- [ ] Fazer função que responde essa mensagem com um ok, caso esse cliente exista
    6- [ ] Se o cliente não receber resposta em x segundos, ele é o novo host
     e manda essa info para todos os clientes
    7- [ ] Repetir
*/