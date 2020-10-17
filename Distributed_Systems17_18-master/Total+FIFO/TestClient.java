import java.util.*;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.Scanner;
import java.io.FileReader;
public class TestClient {

    BufferedReader in;
    PrintWriter out;

    private static HashSet<Group> mygroups = new HashSet<Group>();
    private static Group writegroup;
    private static int numberofmem=0;
    private int id;
    private String username;
    private String udpAddress;
    private String PORT;
    private UDPServer chatserver;
    private String check ;
    private static HashMap<String,Integer> messageIdpergroup = new HashMap<String,Integer>() ; //groupname , messageId
    private static HashMap<String,HashMap<String,Integer>> vectorpergroup = new HashMap<String, HashMap<String,Integer>>(); // groupname, vector= <clientname, messageId>
    private static LinkedList<String> holdBackQueue = new LinkedList<String>();
    private static HashMap<Integer, HashMap<Integer,Integer>> seqnumberspermessage = new HashMap<Integer, HashMap<Integer, Integer>>();

    private static HashMap<String, Integer> seqnumber = new HashMap<String, Integer>();

    /////////////////////////////
    private static class UDPServer implements Runnable {
        private int id;
        private String username;
        private String PORT;
        private String UDPAddress;

        public UDPServer(int id, String username, String PORT, String UDPAddress) {
            this.id = id;
            this.username = username;
            this.PORT = PORT;
            this.UDPAddress = UDPAddress;
            //this.run(Integer.parseInt(PORT));
        }


        public void run() {
            try {
                DatagramSocket serverSocket = new DatagramSocket(null);
                InetSocketAddress address = new InetSocketAddress(UDPAddress, Integer.parseInt(PORT));
                serverSocket.bind(address);
                byte[] receiveData = new byte[1024];
                //System.out.printf("Listening on udp:%s:%d%n", InetAddress.getLocalHost().getHostAddress(), Integer.parseInt(PORT));
                DatagramPacket receivePacket = new DatagramPacket(receiveData, receiveData.length);
                while (true) {
                    serverSocket.receive(receivePacket);
                    String sentence = new String(receivePacket.getData(), 0, receivePacket.getLength());
                    //System.out.println("RECEIVED: " + sentence);
                    if (sentence.startsWith("!addme")) {
                        String[] split = sentence.split(",");
                        //!addme,id,username,udp,port,group
                        int newid = Integer.parseInt(split[1]);
                        String newusername = split[2];
                        String newudpaddr = split[3];
                        String newport = split[4];
                        String grouptoaddclient = split[5];
                        TestClient newclient = new TestClient(newusername, newudpaddr, newport, newid);
                        for (Group j : mygroups) {
                            if (j.getGroupname().equals(grouptoaddclient)) {
                                j.addClient(newclient);
                            }
                        }
                        if (writegroup != null) {
                            if (writegroup.getGroupname().equals(grouptoaddclient)) {
                                writegroup.addClient(newclient);
                            }
                        }
                        vectorpergroup.get(grouptoaddclient).put(newusername,0);
                        DatagramSocket mySocket = new DatagramSocket(null);
                        InetSocketAddress address1 = new InetSocketAddress(UDPAddress, Integer.parseInt(PORT)+1);
                        mySocket.bind(address1);
                        String message = "!myvector," + id + "," + username + "," + grouptoaddclient+","+ messageIdpergroup.get(grouptoaddclient);
                        DatagramPacket datagramPacket = new DatagramPacket(
                                message.getBytes(),
                                message.length(), InetAddress.getByName(newudpaddr),
                                Integer.parseInt(newport)
                        );
                        mySocket.send(datagramPacket);
                        numberofmem++;
                        mySocket.close();
                    }else if(sentence.startsWith("!myvector")){
                        //System.out.println(sentence);
                        String[] split = sentence.split(",");
                        int idtochange = Integer.parseInt(split[1]);
                        String usertochange = split[2];
                        String groupwechange = split[3];
                        int vectoridtochange = Integer.parseInt(split[4]);
                        vectorpergroup.get(groupwechange).put(usertochange,vectoridtochange);
                    }else if (sentence.startsWith("!removeone")) {
                        String[] split = sentence.split(",");
                        //!removeone,id,group
                        int removedid = Integer.parseInt(split[1]);
                        String removedfromgroup = split[2];
                        for (Iterator<Group> it1 = mygroups.iterator(); it1.hasNext(); ) {
                            Group j = it1.next();
                            if (j.getGroupname().equals(removedfromgroup)) {
                                j.removeClient(removedid);

                            }
                        }
                        if (writegroup != null) {
                            if (writegroup.getGroupname().equals(removedfromgroup)) {
                                writegroup.removeClient(removedid);
                            }
                        }
                    } else if (sentence.startsWith("!allremove")) {
                        String[] split = sentence.split(",");
                        //!allremove,id
                        int removeId = Integer.parseInt(split[1]);
                        for (Iterator<Group> it = mygroups.iterator(); it.hasNext(); ) {
                            Group j = it.next();
                            j.removeClient(removeId);
                        }
                        if (writegroup != null) {
                            writegroup.removeClient(removeId);
                        }
                    } else if (sentence.startsWith("!message")) {
                        //System.out.println(sentence);
                        String[] split = sentence.split("!@#");
                        //!message!@#id!@#username!@#groupname!@#messageId!@#in GROUP user USER says: TEXT ( pou vazw to messageId gia na exw fifo sigoura )
                        String groupname = split[3];
                        String messageID = split[4];
                        seqnumber.put(groupname,seqnumber.get(groupname)+1);
                        String sentence1="!seq!@#"+groupname+"!@#"+messageID+"!@#"+seqnumber.get(groupname)+"!@#"+ id;
                        InetAddress IPAddress = receivePacket.getAddress();
                        DatagramPacket sendPacket = new DatagramPacket(sentence1.getBytes(), sentence1.length()
                                                    ,IPAddress , (receivePacket.getPort()-1));
                        serverSocket.send(sendPacket);
                        sentence = "undeliverable!@#"+ seqnumber.get(groupname)+"!@#"+id+"!@#"+sentence;
                        holdBackQueue.add(sentence);
                    }else if(sentence.startsWith("!seq")){
                        //System.out.println(sentence);
                        String[] split = sentence.split("!@#");
                        String groupname = split[1];
                        int messageID = Integer.parseInt(split[2]);
                        int seqnum = Integer.parseInt(split[3]);
                        int id= Integer.parseInt(split[4]);
                        seqnumberspermessage.get(messageID).put(id,seqnum);
                        Group temp= new Group();
                        for( Group j: mygroups){
                            if( j.getGroupname().equals(groupname)){
                                temp= j;
                            }
                        }
                        if( !temp.getClients().isEmpty() && temp.getClients().size()==seqnumberspermessage.get(messageID).size()){
                            int minId=id;
                            int maxSeq= seqnumber.get(groupname);
                            HashMap<Integer,Integer> tras= seqnumberspermessage.get(messageID);
                            for(HashMap.Entry<Integer,Integer> entry: tras.entrySet()){
                                if(maxSeq<= entry.getValue()) {
                                    maxSeq = entry.getValue();
                                    minId= entry.getKey();
                                }
                            }
                            for(HashMap.Entry<Integer,Integer> entry: tras.entrySet()){
                                if(maxSeq== entry.getValue()) {
                                    if(minId>=entry.getKey()){
                                        minId= entry.getKey();
                                    }
                                }
                            }
                            for(Iterator<Group> it=mygroups.iterator(); it.hasNext();) {
                                Group j = it.next();
                                for (TestClient i : j.getClients()) {
                                    DatagramSocket mySocket = new DatagramSocket(null);
                                    InetSocketAddress address1 = new InetSocketAddress(UDPAddress, (Integer.parseInt(PORT) + 1));
                                    mySocket.bind(address1);
                                    String sendmessage="!secmes!@#" +groupname+"!@#" + messageID+"!@#" +
                                            this.id +"!@#" + minId+"!@#" + maxSeq ;
                                    //System.out.println(sendmessage);
                                    DatagramPacket datagramPacket = new DatagramPacket(
                                            sendmessage.getBytes(),
                                            sendmessage.length(), InetAddress.getByName(i.getUDP()),
                                            Integer.parseInt(i.getPORT())
                                    );
                                    //long startTime2 = System.currentTimeMillis();
                                    //System.out.print("SENT at "+startTime2+" :");
                                    mySocket.send(datagramPacket);
                                    //numberofmem++;
                                    mySocket.close();
                                }
                            }
                        }
                    }else if(sentence.startsWith("!secmes")){
                        //System.out.println(sentence);
                        String[] split = sentence.split("!@#");
                        String groupname = split[1];
                        int messageID = Integer.parseInt(split[2]);
                        int idwhosent = Integer.parseInt(split[3]);
                        int k= Integer.parseInt(split[4]);
                        int sk = Integer.parseInt(split[5]);
                        if( seqnumber.get(groupname)<sk)
                            seqnumber.put(groupname, sk);
                        for(ListIterator<String> it=holdBackQueue.listIterator(); it.hasNext();){
                            String j = it.next();
                            String[] splithold = j.split("!@#");
                            String status= splithold[0];
                            int prevseq = Integer.parseInt(splithold[1]);
                            int mesIdinqueue = Integer.parseInt(splithold[7]);
                            int senderid = Integer.parseInt(splithold[4]);
                            if( senderid==idwhosent && messageID==mesIdinqueue){
                                String newstatus= "deliverable";
                                int newseq = sk;
                                int proprosedseq= k;
                                String newmes= newstatus+"!@#"+newseq+"!@#" +proprosedseq+"!@#"+splithold[3] +"!@#" +idwhosent+"!@#" +splithold[5]+"!@#"+groupname
                                        +"!@#" +messageID+"!@#"+ splithold[8];
                                it.remove();
                                it.add(newmes);
                            }
                        }
                        //System.out.println(sentence);
                        // vlepw ti mporw na deiksw
                        for(Iterator<String> it= holdBackQueue.iterator(); it.hasNext();){
                            String j = it.next();
                            String[] splithold = j.split("!@#");
                            if(splithold[0].equals("deliverable")){
                                String groupofmes = splithold[6];
                                int mesId = Integer.parseInt(splithold[7]);
                                String user= splithold[5];
                                String text = splithold[8];
                                int temp = vectorpergroup.get(groupname).get(user);
                                if (temp + 1 == mesId) {
                                    final long startTime1 = System.currentTimeMillis();
                                    System.out.print("RECEIVED at "+startTime1+" :");
                                    System.out.println(text);
                                    vectorpergroup.get(groupname).put(user, mesId);
                                    it.remove();
                                }else {

                                }
                            }
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println(e);
            }
            // should close serverSocket in finally block
        }


    }

/////////

    public void setUsername(String x) {
        this.username = x;
    }

    public String getUsername() {
        return this.username;
    }

    public String getUDP() {
        return this.udpAddress;
    }

    public String getPORT() {
        return this.PORT;
    }

    public int getID() {
        return this.id;
    }
    public void setCheck(String alive){
        this.check = alive;
    }
    public String getCheck(){
        return this.check;
    }

    public TestClient() {

    }

    public TestClient(String username, String udpAddress, String PORT, int id) {
        this.username = username;
        this.udpAddress = udpAddress;
        this.PORT = PORT;
        this.id = id;
    }

    /**
     * Connects to the server then enters the processing loop.
     */
    private void run() throws IOException {
        // Make connection and initialize streams
        System.out.println("Enter IP Address of the Server:");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        String serverAddress = br.readLine();
        System.out.println("Hello! To learn how to use our chat please type !help");
        while (true) {
            Scanner keyboard = new Scanner(System.in);
            String command = keyboard.nextLine();
            if (command.startsWith("!")) {
                // Process all messages from server, according to the protocol.
                if (command.equals("!help")) {
                    System.out.println("Available commands: !help, !register, !lm");
                } else if (command.equals("!register")) {
                    Socket socket = new Socket(serverAddress, 9001);
                    in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                    out = new PrintWriter(socket.getOutputStream(), true);
                    out.println(command);
                    System.out.println(in.readLine());
                    String line = in.readLine();
                    System.out.println(line);
                    String usernameIn = br.readLine();
                    out.println(usernameIn);
                    while (in.readLine().equals("This username is in use. Please submit another one")) {
                        System.out.println("This username is in use. Please submit another one");
                        usernameIn = br.readLine();
                        out.println(usernameIn);
                    }
                    System.out.println(in.readLine());
                    String myudp = br.readLine();
                    out.println(myudp);
                    System.out.println(in.readLine());
                    String port = br.readLine();
                    out.println(port);
                    System.out.println(in.readLine());
                    this.username = usernameIn;
                    this.udpAddress = myudp;
                    this.PORT = port;
                    this.id = Integer.parseInt(in.readLine());
                    this.chatserver = new UDPServer(this.id, this.username, this.PORT, this.udpAddress);
                    new Thread(this.chatserver).start();
                    Timer timer = new Timer();
                    timer.scheduleAtFixedRate(new TimerTask() {
                        @Override
                        public void run()   {
                            // your code- what do you want to do the timer every 6000 msec
                            try {
                                Socket socket1 = new Socket(serverAddress, 9001);
                                BufferedReader in1 = new BufferedReader(new InputStreamReader(socket1.getInputStream()));
                                PrintWriter out1 = new PrintWriter(socket1.getOutputStream(), true);

                                out1.println("!alive,"+id);
                                socket1.close();
                            }catch(IOException ie) {
                                ie.printStackTrace();
                            }

                        }
                    }, 1000, 10000);
                    System.out.println(this.username + " " + this.udpAddress + " " + this.PORT + " " + this.id);
                    socket.close();
                } else {
                    if (this.username == null) {
                        System.out.println("To use our services please register!");
                        continue;
                    } else {
                        //System.out.print("[" + this.username +"] > ");
                        if (command.equals("!lg")) {
                            // Print Group Names
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(command);
                            String line = in.readLine();
                            if (line.equals("There are no active groups!"))
                                System.out.println(line);
                            else {
                                String[] splitLine = line.split(",");
                                System.out.print("groups: ");
                                if (splitLine.length == 1) {
                                    System.out.println(line);
                                } else {
                                    for (int i = 0; i < splitLine.length; i++) {
                                        if (i == 0) {
                                            System.out.print(splitLine[i] + "] , ");
                                        } else if (i < splitLine.length - 1) {
                                            System.out.print("[" + splitLine[i].substring(1) + "] , ");
                                        } else {
                                            System.out.print("[" + splitLine[i].substring(1));
                                        }
                                    }
                                    System.out.println();
                                }
                            }
                            socket.close();
                        } else if (command.startsWith("!j")) {
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(command);
                            out.println(username);
                            out.println(udpAddress);
                            out.println(PORT);
                            out.println(id);
                            String answer = in.readLine();
                            if (answer.equals("You are already in this group!")) {
                                System.out.println(answer);
                            } else {
                                int number = Integer.parseInt(answer);
                                String[] splitLine = command.split(" ");
                                Group group = new Group(splitLine[1]);
                                this.messageIdpergroup.put(group.getGroupname() , 0);
                                this.vectorpergroup.put(group.getGroupname() , new HashMap<String,Integer>());
                                this.vectorpergroup.get(group.getGroupname()).put(username, 0);
                                this.seqnumber.put(group.getGroupname(), 0);
                                for (int i = 0; i < number; i++) {
                                    String clientinfo = in.readLine();
                                    String[] info = clientinfo.split(",");
                                    TestClient client = new TestClient(info[0], info[1], info[2], Integer.parseInt(info[3]));
                                    group.addClient(client);
                                }
                                this.messageIdpergroup.put(splitLine[1],0);
                                mygroups.add(group);
                                if (number > 1) {
                                    for (TestClient j : group.getClients()) {
                                        if (j.getID() != id) {
                                            DatagramSocket mySocket = new DatagramSocket(null);
                                            //DatagramSocket serverSocket = new DatagramSocket(null);
                                            InetSocketAddress address = new InetSocketAddress(this.getUDP(), Integer.parseInt(this.getPORT())+1);
                                            mySocket.bind(address);
                                            String message = "!addme," + id + "," + username + "," + udpAddress + "," + PORT + "," + group.getGroupname();
                                            DatagramPacket datagramPacket = new DatagramPacket(
                                                    message.getBytes(),
                                                    message.length(), InetAddress.getByName(j.getUDP()),
                                                    Integer.parseInt(j.getPORT())
                                            );
                                            mySocket.send(datagramPacket);
                                            numberofmem++;
                                            mySocket.close();
                                        }
                                    }
                                }
                                //for (Group j : mygroups) {
                                  //  for (TestClient i : j.getClients()) {
                                    //    System.out.println(i.getUsername() + " " + i.getID() + " " + i.getPORT() + " " + i.getUDP());
                                    //}
                                //}
                                socket.close();
                            }

                        } else if (command.startsWith("!w")) {
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            String[] splitCom = command.split(" ");
                            boolean flag = false;
                            for (Group j : mygroups) {
                                if (j.getGroupname().equals(splitCom[1])) {
                                    writegroup = j;
                                    flag = true;
                                    //for (TestClient i : writegroup.getClients()) {
                                      //  System.out.println(i.getUsername() + " " + i.getID() + " " + i.getPORT() + " " + i.getUDP());
                                    //}
                                }
                            }
                            if (flag == false)
                                System.out.println("If you want to write in a group, please first join it!");
                        } else if (command.startsWith(("!lm"))) {
                            String[] splitCom = command.split(" ");
                            if (command.equals("!lm") || splitCom[1].isEmpty()) {
                                System.out.println("Please insert a groupname!");
                                if (this.username != null) {
                                    System.out.print("[" + this.username + "] > ");
                                }
                                continue;
                            }
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(command);
                            String answer = in.readLine();
                            System.out.println(answer);
                            socket.close();
                        } else if (command.startsWith("!e")) {
                            String[] split = command.split(" ");
                            if (command.equals("!e") || split[1].isEmpty()) {
                                System.out.println("Please insert a groupname!");
                                if (this.username != null) {
                                    System.out.print("[" + this.username + "] > ");
                                }
                                continue;
                            }
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(command);
                            out.println(id);
                            System.out.println(in.readLine());
                            if (!mygroups.isEmpty()) {
                                for (Iterator<Group> it = mygroups.iterator(); it.hasNext(); ) {
                                    Group j = it.next();
                                    if (j.getGroupname().equals(split[1])) {
                                        for (TestClient i : j.getClients()) {
                                            if (i.getID() != id) {
                                                DatagramSocket mySocket = new DatagramSocket(null);
                                                //DatagramSocket serverSocket = new DatagramSocket(null);
                                                InetSocketAddress address = new InetSocketAddress(this.getUDP(), Integer.parseInt(this.getPORT())+1);
                                                mySocket.bind(address);
                                                String message = "!removeone," + id + "," + j.getGroupname();
                                                DatagramPacket datagramPacket = new DatagramPacket(
                                                        message.getBytes(),
                                                        message.length(), InetAddress.getByName(i.getUDP()),
                                                        Integer.parseInt(i.getPORT())
                                                );
                                                mySocket.send(datagramPacket);
                                                mySocket.close();
                                            }
                                        }
                                    }
                                }
                            }
                            if (!mygroups.isEmpty()) {

                                for (Iterator<Group> it = mygroups.iterator(); it.hasNext(); ) {
                                    Group j = it.next();
                                    if (j.getGroupname().equals(split[1])) {
                                        mygroups.remove(j);
                                        if (writegroup != null) {
                                            if (writegroup.getGroupname().equals(split[1])) {
                                                writegroup = null;
                                            }
                                        }
                                        break;
                                    }
                                }
                            }
                            socket.close();
                        } else if (command.equals("!q")) {
                            Socket socket = new Socket(serverAddress, 9001);
                            in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                            out = new PrintWriter(socket.getOutputStream(), true);
                            out.println(command);
                            out.println(id);
                            System.out.println(in.readLine());
                            if (!mygroups.isEmpty()) {
                                for (Iterator<Group> it = mygroups.iterator(); it.hasNext(); ) {
                                    Group j = it.next();
                                    for (TestClient i : j.getClients()) {
                                        if (i.getID() != id) {
                                            DatagramSocket mySocket = new DatagramSocket(null);
                                            //DatagramSocket serverSocket = new DatagramSocket(null);
                                            InetSocketAddress address = new InetSocketAddress(this.getUDP(), Integer.parseInt(this.getPORT())+1);
                                            mySocket.bind(address);
                                            String message = "!allremove," + id ;
                                            DatagramPacket datagramPacket = new DatagramPacket(
                                                    message.getBytes(),
                                                    message.length(), InetAddress.getByName(i.getUDP()),
                                                    Integer.parseInt(i.getPORT())
                                            );
                                            mySocket.send(datagramPacket);
                                            mySocket.close();
                                        }
                                    }

                                }
                            }
                            mygroups = null;
                            writegroup = null;
                            System.out.println(username + " thanks for using our chatroom!");
                            username = null;
                            socket.close();

                            break;
                        } else {
                            for (Group j : mygroups) {
                                for (TestClient i : j.getClients()) {
                                    System.out.println(i.getUsername() + " " + i.getID() + " " + i.getPORT() + " " + i.getUDP());
                                }
                            }
                            System.out.println("Command not implemented. Please type !help for more information");
                        }
                    }
                }
            } else {
                if( !command.isEmpty()) {
                    if (mygroups != null) {
                        if (writegroup != null) {
                            //System.out.println(command);
                            //if(command.equals("OK")) {

                                //BufferedReader bufferreader = new BufferedReader(new FileReader("C:\\Users\\HP-User\\IdeaProjects\\First\\src\\long_message.txt"));
                                //command = bufferreader.readLine();
                                //final long startTime = System.currentTimeMillis();
                                //while(command!=null) {
                                    String message = "in " + writegroup.getGroupname() + " user " + this.getUsername() + " says: " + command;
                                    messageIdpergroup.put(writegroup.getGroupname(), messageIdpergroup.get(writegroup.getGroupname()) + 1);
                                    seqnumberspermessage.put(messageIdpergroup.get(writegroup.getGroupname()), new HashMap<Integer,Integer>());
                                    String premessage = "!message!@#" +this.getID()+"!@#"+ this.getUsername() + "!@#" + writegroup.getGroupname() + "!@#" + messageIdpergroup.get(writegroup.getGroupname()) + "!@#";
                                    String sendmessage = premessage + message;
                                    for (TestClient i : writegroup.getClients()) {
                                        DatagramSocket mySocket = new DatagramSocket(null);
                                        InetSocketAddress address = new InetSocketAddress(this.getUDP(), Integer.parseInt(this.getPORT())+1);
                                        mySocket.bind(address);
                                        DatagramPacket datagramPacket = new DatagramPacket(
                                                sendmessage.getBytes(),
                                                sendmessage.length(), InetAddress.getByName(i.getUDP()),
                                                Integer.parseInt(i.getPORT())
                                        );
                                        long startTime2 = System.currentTimeMillis();
                                        System.out.print("SENT at "+startTime2+" :");
                                        mySocket.send(datagramPacket);
                                        //numberofmem++;
                                        mySocket.close();
                                    }
                                   // command = bufferreader.readLine();
                                //}
                                //final long endTime = System.currentTimeMillis();
                                //System.out.println("Total execution time: " + (endTime - startTime) );
                                //System.out.println(numberofmem);
                            //}
                        }else{
                            System.out.println("FIrst choose a group to write!");
                        }
                    }else{
                        System.out.println("FIrst choose a group to write!");
                    }
                }else{
                    //System.out.println("");
                }
            }
            if(username!=null){
                System.out.print("["+username+"]"+" > ");
            }
        }
    }

    public static void main(String[] args) throws Exception {
        TestClient client = new TestClient();
        client.run();
    }
}
