package model;

import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.HashMap;

public class ConnectionHandler implements Runnable {
	
	private int port;
	private HashMap<Integer,Socket> slavelist;
	private static int slaveID = 0;
	
	public ConnectionHandler(int port) {
		this.port = port;
		slavelist = new HashMap<Integer,Socket>();
	}
	
	public void run() {
		ServerSocket serverSocket;
		try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server listening on port: "+ port);
            System.out.println("IP address: " + InetAddress.getLocalHost().getHostAddress());
            while (true){
                Socket clientSocket = serverSocket.accept();
                slaveID++;
                slavelist.put(new Integer(slaveID), clientSocket);
                System.out.println("Connection established to "+ clientSocket.getInetAddress() + ":" +
                								clientSocket.getPort() + "(" + "slave " + slaveID + ")");
            }
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
	}
	
	// retrieve slavelist
	public HashMap<Integer, Socket> getSlaveList() {
		return this.slavelist;
	}
}
