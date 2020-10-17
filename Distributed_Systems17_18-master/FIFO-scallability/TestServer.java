import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Timer;
import java.util.TimerTask;


public class TestServer {
    private static final int PORT = 9001;
    private static HashSet<String> usernames = new HashSet<String>();
    private static HashSet<String> groupnames = new HashSet<String>();

    private static HashSet<Group> groups = new HashSet<Group>();
    private static HashSet<TestClient> clients = new HashSet<TestClient>();

    public static void main(String[] args) throws Exception {
        System.out.println("The chat server is running.");
        ServerSocket listener = new ServerSocket(PORT);
        try {
            while (true) {
                new Handler(listener.accept()).start();
            }
        } finally {
            listener.close();
        }
    }

    private static class Handler extends Thread {
        private String username;
        private Socket socket;
        private BufferedReader in;
        private PrintWriter out;

        public Handler(Socket socket) {
            this.socket = socket;
        }

        public void run() {
            try {

                in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);
                String line = in.readLine();
                if (line.equals("!register")) {
                    out.println("Welcome to our service!");
                    out.println("Please submit a username");
                    while (true) {
                        username = in.readLine();
                        if (username == null) {
                            return;
                        }
                        synchronized (usernames) {
                            if (!usernames.contains(username)) {
                                usernames.add(username);
                                break;
                            }
                        }
                        out.println("This username is in use. Please submit another one");
                    }
                    out.println("Username accepted!");
                    out.println("Please submit an address for your Server!");
                    String udpclient = in.readLine();
                    out.println("Please submit a port!");
                    String portclient = in.readLine();
                    out.println("Welcome " + username + " to our chatroom!");
                    int idclient = clients.size() + 1;
                    out.println(idclient);
                    TestClient myclient = new TestClient(username, udpclient, portclient, idclient);
                    myclient.setCheck("ALIVE");
                    clients.add(myclient);
                    if (idclient == 1) {
                        Timer timer = new Timer();
                        timer.scheduleAtFixedRate(new TimerTask() {
                            @Override
                            public void run() {
                                // your code- what do you want to do the timer every 6000 msec
                                for (Iterator<TestClient> it = clients.iterator(); it.hasNext(); ) {
                                    TestClient i = it.next();
                                    if (!i.getCheck().equals("ALIVE")) {
                                        for (Iterator<Group> it1 = groups.iterator(); it1.hasNext(); ) {
                                            Group j = it1.next();
                                            for (Iterator<TestClient> it3= j.getClients().iterator(); it3.hasNext();) {
                                                TestClient k = it3.next();
                                                try {
                                                    DatagramSocket mySocket = new DatagramSocket(null);
                                                    //DatagramSocket serverSocket = new DatagramSocket(null);
                                                    InetSocketAddress address = new InetSocketAddress("127.0.0.1", 9002);
                                                    mySocket.bind(address);
                                                    String message = "!allremove," + i.getID();
                                                    DatagramPacket datagramPacket = new DatagramPacket(
                                                            message.getBytes(),
                                                            message.length(), InetAddress.getByName(k.getUDP()),
                                                            Integer.parseInt(k.getPORT())
                                                    );
                                                    mySocket.send(datagramPacket);
                                                    mySocket.close();

                                                }catch(Exception e){

                                                }

                                            }
                                            j.removeClient(i.getID());
                                            if(j.getClients().isEmpty()){
                                                String groupname = j.getGroupname();
                                                it1.remove();
                                                groupnames.remove(groupname);
                                            }
                                        }
                                        it.remove();
                                        for(Iterator<String> it2 = usernames.iterator(); it2.hasNext();){
                                            String user = it2.next();
                                            if( user.equals(i.getUsername())){
                                                it2.remove();;
                                            }
                                        }

                                    }
                                }
                                for (Iterator<TestClient> it = clients.iterator(); it.hasNext(); ) {
                                    TestClient i = it.next();
                                    i.setCheck("NOTALIVE");
                                }
                            }
                        }, 2000, 10000);
                    }
                    System.out.println(myclient.getUsername() + " " + myclient.getUDP() + " " + myclient.getPORT() + " " + myclient.getID());

                } else if (line.startsWith("!alive")) {
                    String[] split = line.split(",");
                    int aliveid = Integer.parseInt(split[1]);
                    for (Iterator<TestClient> it = clients.iterator(); it.hasNext(); ) {
                        TestClient i = it.next();
                        if (i.getID() == aliveid) {
                            i.setCheck("ALIVE");
                        }
                    }
                } else if (line.equals("!lg")) {
                    synchronized (groupnames) {
                        if (groupnames.isEmpty()) {
                            out.println("There are no active groups!");
                        } else {
                            out.println(groupnames);
                        }
                    }
                } else if (line.startsWith("!lm")) {
                    String[] splitCom = line.split(" ");
                    String groupName = splitCom[1];
                    synchronized (groupnames) {
                        if (groupnames.contains(groupName)) {
                            String users = "members: ";
                            for (Group i : groups) {
                                if (i.getGroupname().equals(groupName)) {
                                    HashSet<TestClient> tempClients = i.getClients();
                                    for (TestClient j : tempClients) {
                                        users = users + "(" + j.getUsername() + ") ,";
                                    }
                                }
                            }
                            out.println(users.substring(0, users.length() - 1));
                        } else {
                            out.println("There is no group with this name!");
                        }
                    }
                } else if (line.startsWith("!j")) {
                    String[] splitLine = line.split(" ");
                    synchronized (groupnames) {
                        if (!groupnames.contains(splitLine[1])) {
                            groupnames.add(splitLine[1]);
                            System.out.println(groupnames);
                            Group group = new Group(splitLine[1]);
                            String user = in.readLine();
                            String udpadd = in.readLine();
                            String port = in.readLine();
                            int id = Integer.parseInt(in.readLine());
                            TestClient client = new TestClient(user, udpadd, port, id);
                            group.addClient(client);
                            groups.add(group);
                            HashSet<TestClient> len = group.getClients();
                            for (TestClient i : len) {
                                System.out.println(i.getUsername() + " " + i.getUDP() + " " + i.getPORT() + " " + i.getID());
                            }
                            int number = 1;
                            out.println(number);
                            for (TestClient i : len) {
                                out.println(i.getUsername() + "," + i.getUDP() + "," + i.getPORT() + "," + i.getID());
                            }
                        } else {
                            String requestedgroupname = splitLine[1];
                            Group group = new Group();
                            for (Group i : groups) {
                                if (i.getGroupname().equals(requestedgroupname)) {
                                    group = i;
                                }
                            }
                            String user = in.readLine();
                            String udpadd = in.readLine();
                            String port = in.readLine();
                            int id = Integer.parseInt(in.readLine());
                            if (group.existsClient(id)) {
                                out.println("You are already in this group!");
                            } else {
                                TestClient client = new TestClient(user, udpadd, port, id);
                                group.addClient(client);
                                HashSet<TestClient> len = group.getClients();
                                out.println(len.size());
                                for (TestClient i : len) {
                                    //System.out.println(i.getUsername() + "," + i.getUDP() + "," + i.getPORT() + "," + i.getID());
                                    out.println(i.getUsername() + "," + i.getUDP() + "," + i.getPORT() + "," + i.getID());
                                }
                            }
                        }
                    }
                } else if (line.startsWith("!e")) {
                    String[] splitLine = line.split(" ");
                    synchronized (groupnames) {
                        if (groupnames.contains(splitLine[1])) {
                            int idClient = Integer.parseInt(in.readLine());
                            boolean flag = false; // leei psemmata kai den einai sto group
                            for (Iterator<Group> it1 = groups.iterator(); it1.hasNext(); ) {
                                Group j = it1.next();
                                if (j.getGroupname().equals(splitLine[1])) {
                                    HashSet<TestClient> curClients = j.getClients();
                                    for (Iterator<TestClient> it = clients.iterator(); it.hasNext(); ) {
                                        TestClient i = it.next();
                                        if (i.getID() == idClient) {
                                            flag = true;
                                            j.removeClient(idClient);
                                            if (j.getClients().isEmpty()) {
                                                groupnames.remove(splitLine[1]);
                                                it1.remove();
                                            }
                                        }
                                    }
                                }
                            }
                            if (flag == false)
                                out.println("You can't leave a group that you aren't in!");
                            else
                                out.println("You left the group!");
                        } else {
                            out.println("There is no group with this name!");
                        }
                    }
                } else if (line.equals("!q")) {
                    int idClient = Integer.parseInt(in.readLine());
                    synchronized (usernames) {
                        String usertoRemove = "";
                        for (Iterator<TestClient> it2 = clients.iterator(); it2.hasNext(); ) {
                            TestClient i = it2.next();
                            if (i.getID() == idClient) {
                                usertoRemove = i.getUsername();
                                System.out.println(usertoRemove);
                                it2.remove();
                            }
                        }
                        for (Iterator<Group> it1 = groups.iterator(); it1.hasNext(); ) {
                            Group j = it1.next();
                            HashSet<TestClient> curClients = j.getClients();
                            String groupnameR = j.getGroupname();
                            for (Iterator<TestClient> it = curClients.iterator(); it.hasNext(); ) {
                                TestClient i = it.next();
                                if (i.getUsername().equals(usertoRemove)) {
                                    System.out.println("Yo");
                                    it.remove();
                                    if (j.getClients().isEmpty()) {
                                        groupnames.remove(groupnameR);
                                        it1.remove();
                                    }
                                    for (TestClient pap : j.getClients()) {
                                        System.out.println(pap.getUsername());
                                    }
                                }
                            }

                        }
                        usernames.remove(usertoRemove);
                    }
                    out.println("You quit!");
                } else {
                    System.out.println("FAIL: Command not supported is here !!!!");
                }
                try {
                    socket.close();
                } catch (IOException e) {
                }
            } catch (IOException e) {
                System.out.println(e);
            } finally {
                // This client is going down!  Remove its name and its print
                // writer from the sets, and close its socket.
                //if (username != null) {
                //usernames.remove(username);
                //}
                //if (out != null) {
                // writers.remove(out);
                // }
            }
        }
    }
}
