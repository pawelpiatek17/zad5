/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package server;

import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;

import paczka.DziennikZdarzen;
import utils.ELogLevel;

/**
 *
 * @author Mariusz
 */
public class Server implements Runnable {

    /**
     * @param args the command line arguments
     * @throws java.io.IOException
     */
    private static int port;
    private static int portLogLvl;
    private static int nThreads = 0;
    private static JLabel nThreadsLabel;
    private static DziennikZdarzen dziennikZdarzen;
	public static void main(String[] args) throws IOException {
		dziennikZdarzen = new DziennikZdarzen();
		ServerSocket ssock = null;
		try {
		    Properties props = new Properties();
		    props.load(new FileInputStream("Server.properties"));
		    port=Integer.parseInt(props.getProperty("port"));
		    ssock = new ServerSocket(port);
		} catch(Exception e){
	        JOptionPane.showMessageDialog(null, "While binding port " + port + "\n" + e);
	        System.exit(1);
		}
	    JFrame mainWindow = new JFrame("Communicator server on port " + port);
	    mainWindow.setSize(400,80);
		mainWindow.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		Container interior = mainWindow.getContentPane();
		interior.setLayout(new GridLayout(1, 2));
	    interior.add(new JLabel("Active threads", JLabel.CENTER));
	    nThreadsLabel = new JLabel("0", JLabel.CENTER);
	    interior.add(nThreadsLabel);
	    Dimension dim = mainWindow.getToolkit().getScreenSize();
	    Rectangle abounds = mainWindow.getBounds();
	    mainWindow.setLocation((dim.width - abounds.width) / 2, (dim.height - abounds.height) / 2);
	    mainWindow.setVisible(true);
        for(;;) {
			Socket sock = ssock.accept();
			new Thread(new Server(sock)).start();
        }
    } 
    private Socket sock;
    private String login = null;
    private String sendTo = null;

    private Server(Socket sock) throws IOException {
        this.sock = sock;
    }
    @Override
    public void run() {
        changeNThreads(+1);
        try {
			PrintWriter out=new PrintWriter(sock.getOutputStream(), true);
			BufferedReader in=new BufferedReader(new InputStreamReader(sock.getInputStream()));
			dziennikZdarzen.addOutputDestination("servLog.txt");
            dziennikZdarzen.addConsoleOutput();
	        mainLoop:
	        for(;;) {
	            String s = null;
	            try {
	                s = in.readLine();
	            } catch(SocketException e) {
	                break;
	            }
	            if(s == null) {
	            	break;
	            }
	            
	            
	            /*
	                interpretation of a command/data sent from clients
	            */
	            if(s.charAt(0) == '/') {
	                StringTokenizer st = new StringTokenizer(s);
	                String cmd = st.nextToken();
	                switch(cmd) {
	                    case "/alert":
	                        dziennikZdarzen.setOutputLogLevel(ELogLevel.ALERT);
	                        break;
	                    case "/warning":
	                    	dziennikZdarzen.setOutputLogLevel(ELogLevel.WARNING);
	                        break;
	                    case "/info":
	                    	dziennikZdarzen.setOutputLogLevel(ELogLevel.INFO);
	                        break;
	                    case "/debug":
	                    	dziennikZdarzen.setOutputLogLevel(ELogLevel.DEBUG);
	                    	break;
	                    default:
	                        out.println("Unknown command " + cmd);
	                        break;
	                }
	            } else {
	            	dziennikZdarzen.alert(s, sock.getInetAddress());
	            } 
	        }
        } catch(IOException e) {}
        changeNThreads(-1);
    }
    
    private synchronized static void changeNThreads(int delta) {
        nThreads += delta;
        nThreadsLabel.setText("" + nThreads);
    } 
}
