import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Main {
    static long start = System.currentTimeMillis();
    static long end = start + 25 * 1000; // 25 seconds * 1000 ms/sec

    static boolean isCoordinator = false;
    static boolean resetTimer = false;

    public static void setCoordinator(boolean isCoordinatorr) {
        isCoordinator = isCoordinatorr;
    }

    public static boolean getCoordinator() {
        return isCoordinator;
    }

    static DatagramSocket socket;

    public static void main(String[] args) throws FileNotFoundException, IOException {

        if (args.length != 2) {
            System.out.println("Use: java Main localhost <PORT>");
            return;
        }

        int myId = 0;
        int coordinatorPort = 0;
        // boolean isCoordinator = false;
        boolean coordinatorOnline = false;
        boolean electionStarted = false;

        ArrayList<Node> nodeList = new ArrayList<Node>();
        File file = new File("config.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        int maiorID = 0;
        String aux;

        long start = System.currentTimeMillis();
        long end = start + 25 * 1000; // 25 seconds * 1000 ms/sec

        // recebe a porta passada por parametro java localhost 3000
        int myPort = Integer.parseInt(args[1]);
        // cria um socket datagrama
        socket = new DatagramSocket(myPort);

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

        if (myId == maiorID) {
            for (Node n : nodeList) {
                n.coordinator = true;
            }
            System.out.println("sou o coordenador - " + maiorID);
            resetTimer = false;
            setCoordinator(true);
        }

        br.close();

        // Send messages
        while (true) {

            // 1 Node is coordinator and only responds
            // condicao com que faz que o coordenador continue conectado por x tempo
            if (getCoordinator()) {
                if (resetTimer) {
                    start = System.currentTimeMillis();
                    end = start + 25 * 1000; // 25 seconds * 1000 ms/sec
                    resetTimer = false;
                }
                while (System.currentTimeMillis() < end) {
                    try {
                        // obtem a resposta
                        DatagramPacket message = receiveMessage();

                        // mostra a resposta
                        String resposta = new String(message.getData(), 0, message.getLength());
                        System.out.println(resposta);
                        if (resposta.equals("Coordenador online?")) {
                            sendMessage("Eu sou o coordenador", message.getAddress(), message.getPort());
                        } else {
                            sendMessage("*Coordinator aqui - myId: " + myId, message.getAddress(), message.getPort());
                        }

                    } catch (IOException e) {
                        System.out.print("*");

                    }

                } // passou 10s e saiu do while
                System.out.println("\ndesconectado coordenador");
                socket.close();
                System.exit(0);
            }

            // MARK nao sou o coordenador
            else {

                // Um coordenador é conhecido
                if (coordinatorOnline) {
                    // 2 Node is not coordinator and sends hello to coordinator
                    sendMessage("\nOla coord sou o id -  " + myId, InetAddress.getByName("localhost"), coordinatorPort);

                    try {
                        // 2.1 Coordinator responde
                        // obtem a resposta
                        DatagramPacket message = receiveMessage();

                        // mostra a resposta
                        String resposta = new String(message.getData(), 0, message.getLength());
                        System.out.println(resposta);
                    }
                    // 2.2 Coordinator does not respond
                    catch (IOException e) {
                        System.out.println("coordenador caiu, iniciando nova eleicao");
                        // nao consegui mandar para o coordenador
                        Election(nodeList, socket, myId, isCoordinator);
                    }

                    try {
                        Thread.currentThread();
                        Thread.sleep(2000);
                    } catch (InterruptedException e) {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }
                    // COOORDENADOR NÃO É CONHECIDO
                } else {

                    // Pergunta se tem um coordenador
                    if (!electionStarted) {
                        
                        for (Node node : nodeList) {
                            if (node.port != myPort) {
                                sendMessage("Coordenador online?", InetAddress.getByName("localhost"), node.port);
                            }

                        }
                    }

                    try {
                        // Recebe mensagem
                        DatagramPacket message = receiveMessage();
                        String resposta = new String(message.getData(), 0, message.getLength());
                        System.out.println(resposta);
                        // verifica se a mensagem é do coordenador
                        if (resposta.equals("Eu sou o coordenador")) {
                            for (Node node : nodeList) {
                                if (node.port == message.getPort()) {
                                    coordinatorPort = node.port;
                                    coordinatorOnline = true;
                                }

                            }
                        }

                    }
                    // 2.2 Coordinator does not respond
                    catch (IOException e) {
                        System.out.print(".");
                    }

                }

            }

        }
    }



    static void Election(ArrayList<Node> nodeList, DatagramSocket socket, int myId, boolean isCoordinator)
            throws IOException {
        System.out.println(myId + "----");
        int maiorIDEleicao = 0;
        for (Node node : nodeList) {
            // Send HI message
            if (node.ID > myId) {
                if (node.ID > maiorIDEleicao) {
                    maiorIDEleicao = node.ID;
                }
                sendMessage("sou o novo coordenador", InetAddress.getByName("localhost"), node.port);
                System.out.println(myId + ": enviei mensagem para " + node.ID);
            }

        }//end for
        while (System.currentTimeMillis() < end) {
        try {
            // obtem a resposta
            DatagramPacket message = receiveMessage();

            // mostra a resposta
            String resposta = new String(message.getData(), 0, message.getLength());
            System.out.println(resposta + "recebendo resposta");
            if (resposta.contains("Eu sou o coordenador")){
                System.out.println("identificado novo coordenador");
                return;
            }
            Thread.currentThread();
            Thread.sleep(2000);


        } catch (IOException e) {
        
            System.out.println("esperando resposta de alguem");

        } catch (InterruptedException e) {
            socket.close();
        }
    }
    setCoordinator(true);
    resetTimer = true;
    }

    static void sendMessage(String message, InetAddress address, int port) throws IOException {
        String answerText = message;
        byte[] text = new byte[256];
        text = answerText.getBytes();
        InetAddress senderAddress;
        senderAddress = address;
        int senderPort = port;

        DatagramPacket pacote = new DatagramPacket(text, text.length, senderAddress, senderPort);

        socket.send(pacote);
    }

    static DatagramPacket receiveMessage() throws IOException {
        byte[] text = new byte[256];
        DatagramPacket pacote = new DatagramPacket(text, text.length);
        try {
            socket.setSoTimeout(500);
        } catch (SocketException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        socket.receive(pacote);
        return pacote;

    }

}
/*
 * 1- [x] Programa que faz leitura do arquivo txt e manda mensagem de “Hello”
 * para todos 2- [x] Programa que seta uma das instâncias (id 0?) como host, e
 * as demais mandam mensagem apenas para ele, e ele só responde (Ping/pong)
 * 
 * 3- [x] Fazer função que desconecta o host após 10s e fazer com que os demais
 * “percebam” a queda 4- [x] Fazer função que, uma vez desconectado o host, faz
 * cada cliente mandar o seu ID para cada cliente com id maior que o seu
 * 
 * 5- [ ] Fazer função que responde essa mensagem com um ok, caso esse cliente
 * exista 6- [ ] Se o cliente não receber resposta em x segundos, ele é o novo
 * host e manda essa info para todos os clientes 7- [ ] Repetir
 */
