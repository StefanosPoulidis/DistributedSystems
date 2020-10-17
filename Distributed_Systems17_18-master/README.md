A CLI(Command Line Interface) chatroom with a distributed peer-to-peer client system and a TCP Tracker. 
Clients are ordering the incoming messages using two well known distributed ordering algorithms:FIFO and Total Ordering with FIFO. Clients communicate with the tracker using 
TCP messages(control messages) and clients communicate with each other using UDP messages(team messages).
<br>Available Client commands:
1) !lg : list all active group names
2) !lm [group-name] : lists all client names who belong to the [group-name] group
3) !j [group-name] : client joins the [group-name] group
4) !w [group-name] : client chooses the group which his messages will be sent
5) !e [group-name] : client leave the [group-name] group
6) !q : client exits from the application

