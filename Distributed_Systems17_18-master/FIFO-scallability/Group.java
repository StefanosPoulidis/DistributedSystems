import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashSet;
import java.util.Iterator;

public class Group {
    private String groupname;
    private HashSet<TestClient> clients = new HashSet<TestClient>();

    public boolean existsClient(int id){
        for( TestClient i: this.clients){
            if( i.getID()==id)
                return true;
        }
        return false;
    }
    public Group(){

    }
    public Group(String x){
        this.groupname = x;
    }

    public String getGroupname(){
        return this.groupname;
    }
    public HashSet<TestClient> getClients(){
        return this.clients;
    }
    public int getClientsLen(){
        return this.clients.size();
    }


    public void setGroupname(String x){
        this.groupname= x;
    }
    public void setClients(HashSet<TestClient> x){
        this.clients = x;
    }

    public void addClient(TestClient x){
        this.clients.add(x);
    }

    public void removeClient(int id){
        synchronized (clients) {
            for (Iterator<TestClient> it = clients.iterator(); it.hasNext();) {
                TestClient i = it.next();
                if(i.getID()==id){
                    it.remove();
                }
            }
        }
    }

}
