import java.util.ArrayList;
import java.io.*;
import java.net.*;

public class Main {
    static long start = System.currentTimeMillis();
    static long end = start + 10 * 1000; // 25 seconds * 1000 ms/sec

    static boolean isCoordinator = false;
    static boolean resetTimer = false;
    static boolean electionStarted = false;
    static boolean coordinatorOnline = false;
    static boolean flagSafadona = false;
    static int myPort;
    static int myId;
    static int coordinatorPort;
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

        myId = 0;
        coordinatorPort = 0;
        electionStarted = false;

        ArrayList<Node> nodeList = new ArrayList<Node>();
        File file = new File("config.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        int maiorID = 0;
        String aux;

        start = System.currentTimeMillis();
        end = start + 10 * 1000; // 25 seconds * 1000 ms/sec

        myPort = Integer.parseInt(args[1]);
        socket = new DatagramSocket(myPort);

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


        while (true) {
            if (isCoordinator) {
                while (System.currentTimeMillis() < end) {
                    try {
                        // obtem a resposta
                        DatagramPacket message = receiveMessage();
                        
                        // mostra a resposta
                        String resposta = new String(message.getData(), 0, message.getLength());
                        int messageCode = getMessageCode(resposta);
                        if (messageCode == 1) {
                            sendMessage("0:Coordenador id " + myId, message.getAddress(), message.getPort());
                        } else if (messageCode == 2) {
                            //System.out.println(getMessageContent(resposta));
                            sendMessage("3:Coordinator aqui - myId:" + myId, message.getAddress(), message.getPort());
                        }

                    } catch (IOException e) {
                        System.out.print("*");

                    }

                } // passou 10s e saiu do while
                System.out.println("\ndesconectado coordenador");
                socket.close();
                System.exit(0);
            }

            else {
                if (coordinatorOnline) {

                    sendMessage("2:Ola coord sou o id -  " + myId, InetAddress.getByName("localhost"), coordinatorPort);

                    try {
                        DatagramPacket message = receiveMessage();

                        String resposta = new String(message.getData(), 0, message.getLength());
                        //System.out.println(resposta);
                    }
                    catch (IOException e) {
                        System.out.println("6:coordenador caiu, iniciando nova eleicao id " + myId);
                        // nao consegui mandar para o coordenador
                        coordinatorOnline = false;
                        resetTimer(2);
                    }

                } else {
                    if (!electionStarted && System.currentTimeMillis() < end) {
                        for (Node node : nodeList) {
                            if (node.port != myPort) {
                                sendMessage("1:Coordenador online?", InetAddress.getByName("localhost"), node.port);
                            }

                        }
                    } else if (electionStarted){
                        if (System.currentTimeMillis() < end){
                            // Election election = new Election();
                            // election.runElection();
                            sendMessageForBigger("4:Quero ser o coordenador - " + myId, nodeList);

                        } else {
                            System.out.println("\nEu sou o novo coordenador");
                            electionStarted = false;
                            sendMessageForAll("0:Novo coordenador escolhido, id = " + myId, nodeList);
                            isCoordinator = true;
                            resetTimer(10);
                        }
                    
                    } else {
                        electionStarted = true;

                        sendMessageForAll("6:Eleição começou", nodeList);
                        resetTimer(5);
                    }

                    try{
                        DatagramPacket message = receiveMessage();
                        String resposta = new String(message.getData(), 0, message.getLength());
                        int messageCode = getMessageCode(resposta);

                        if(messageCode == 0){
                            System.out.println(getMessageContent(resposta));
                            coordinatorOnline = true;
                            coordinatorPort = message.getPort();
                            electionStarted = false;
                        } else if (messageCode == 4){
                            int senderID = Integer.parseInt(getMessageContent(resposta).split(" - ")[1]);
                            if (senderID > myId){
                                System.out.println(senderID + " é maior que meu id " + myId);
                                electionStarted = false;
                                resetTimer(2);
                            } else {
                                sendMessage("5:Meu id é maior", message.getAddress(), message.getPort());
                            }
                        } else if (messageCode == 5){
                            System.out.println("\nAlguém com ID maior respondeu, me retirando da eleição");
                                electionStarted = false;
                                resetTimer(5);
                        } else if (messageCode == 6){
                            System.out.println(getMessageContent(resposta));
                            // Election election = new Election();
                            // election.runElection();
                            electionStarted = true;
                            resetTimer(5);
                        }

                    } catch (IOException e) {
                        System.out.print(".");
                    }



                }

            }

        }
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

    static void sendMessageForAll(String message, ArrayList<Node> nodeList) throws UnknownHostException, IOException {
        for (Node node : nodeList) {
            if (node.port != myPort){
                sendMessage(message, InetAddress.getByName(node.address), node.port);
            }
        }

    }

    static void sendMessageForBigger(String message, ArrayList<Node> nodeList) throws UnknownHostException, IOException {
        for (Node node : nodeList) {
            if (node.port > myPort){
                sendMessage(message, InetAddress.getByName(node.address), node.port);
            }
        }

    }

    static DatagramPacket receiveMessage() throws IOException {
        byte[] text = new byte[256];
        DatagramPacket pacote = new DatagramPacket(text, text.length);
        try {
            socket.setSoTimeout(500);
        } catch (SocketException e) {
            e.printStackTrace();
        }
        socket.receive(pacote);
        return pacote;

    }

    static int getMessageCode(String message){
        return Integer.parseInt(message.split(":")[0]);
    }

    static String getMessageContent(String message){
        return message.split(":")[1];
    }

    static void resetTimer(int seconds){
        start = System.currentTimeMillis();
        end = start + seconds * 1000; // 25 seconds * 1000 ms/sec
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
