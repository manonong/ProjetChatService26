/*
 * Copyright (c) 2024.  Jerome David. Univ. Grenoble Alpes.
 * This file is part of DcissChatService.
 *
 * DcissChatService is free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 *
 * DcissChatService is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package fr.uga.miashs.dciss.chatservice.client;

import java.io.*;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;	//new
import java.sql.ResultSet;			//new
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

import fr.uga.miashs.dciss.chatservice.common.Packet;

/**
 * Manages the connection to a ServerMsg. Method startSession() is used to
 * establish the connection. Then messages can be send by a call to sendPacket.
 * The reception is done asynchronously (internally by the method receiveLoop())
 * and the reception of a message is notified to MessagesListeners. To register
 * a MessageListener, the method addMessageListener has to be called. Session
 * are closed thanks to the method closeSession().
 */
public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;					// Socket de l'utlisateur (est null == jamais connecté OU session fermée)
	private DataOutputStream dos;		//DOS est pour les flux SORTANT
	private DataInputStream dis;		// DIS est pour les flux ENTRANT

	private int identifier;				//Identifiant de l'utlisateur (est 0 == jamais connecté)

	private List<MessageListener> mListeners;
	private List<ConnectionListener> cListeners;



	/**
	 * Create a client with an existing id, that will connect to the server at the
	 * given address and port
	 * 
	 * @param id      The client id
	 * @param address The server address or hostname
	 * @param port    The port number
	 */

	public ClientMsg(int id, String address, int port) {
		if (id < 0)
			throw new IllegalArgumentException("id must not be less than 0");
		if (port <= 0)
			throw new IllegalArgumentException("Server port must be greater than 0");
		serverAddress = address;
		serverPort = port;
		identifier = id;
		mListeners = new ArrayList<>();
		cListeners = new ArrayList<>();
	}



	/**
	 * Create a client without id, the server will provide an id during the the
	 * session start
	 * 
	 * @param address The server address or hostname
	 * @param port    The port number
	 */
	public ClientMsg(String address, int port) {
		this(0, address, port);
	}



	/**
	 * Register a MessageListener to the client. It will be notified each time a
	 * message is received.
	 * 
	 * @param l
	 */
	public void addMessageListener(MessageListener l) {
		if (l != null)
			mListeners.add(l);
	}
	protected void notifyMessageListeners(Packet p) {
		mListeners.forEach(x -> x.messageReceived(p));
	}
	


	/**
	 * Register a ConnectionListener to the client. It will be notified if the connection  start or ends.
	 * 
	 * @param l
	 */
	public void addConnectionListener(ConnectionListener l) {
		if (l != null)
			cListeners.add(l);
	}
	protected void notifyConnectionListeners(boolean active) {
		cListeners.forEach(x -> x.connectionEvent(active));
	}



	public int getIdentifier() {		// Récupère l'id en mémoire de l'objet d'instance
		return identifier;
	}


	private int chargerIdLocal(){		// Récupère l'id de l'utilisateur dans la base de données (codé pour SQL)
    	try {
			Connection cnx = DriverManager.getConnection("jdbc:sqlite:client.db");		//*******POTENTIELLEMENT À CHANGER LE CHEMIN --VOIR SELON LA BDD***********//
			cnx.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS Session (userId INTEGER PRIMARY KEY)");
        
			ResultSet res = cnx.createStatement().executeQuery("SELECT userId FROM Session");
			if (res.next()) return res.getInt(1);

    	} catch (SQLException e) {
			// TODO Auto-generated catch block
        	e.printStackTrace();
    	}
			return 0;
	}

	private void sauvegarderIdLocal(int id){
    	try {
		Connection cnx = DriverManager.getConnection("jdbc:sqlite:client.db")
        cnx.createStatement().executeUpdate("CREATE TABLE IF NOT EXISTS Session (userId INTEGER PRIMARY KEY)");

        PreparedStatement ps = cnx.prepareStatement("INSERT OR REPLACE INTO Session VALUES (?)");
        ps.setInt(1, id);	// -- Paramètre la requête SQL 		// paramètre 1 : indique quel ? il faut remplacer dans la requête SQL / paramètre 2 : par quoi il faut le remplacer
        ps.executeUpdate();	// -- Mise en jour de la requête

    	} catch (SQLException e) {
			// TODO Auto-generated catch block
        	e.printStackTrace();
    	}
	}

	


	/**
	 * Method to be called to establish the connection.
	 * 
	 * @throws UnknownHostException
	 * @throws IOException
	 */
	public void startSession() throws UnknownHostException {
		if (s == null || s.isClosed()) {	
		//Sécurité pour ne pas ouvrir plusieurs sockets vers le serveur pour un même utilisateur

			try {
				s = new Socket(serverAddress, serverPort); 			// Créer la connection réseau (TCP) entre deux machines -- Classe Java "Socket" --
				dos = new DataOutputStream(s.getOutputStream());	// Prépare l'envoi -- DataOutputStream est un wrapper (permet utiliser dos.writeInt(), write(bytes), writeUTF("string") ) --
				dis = new DataInputStream(s.getInputStream());		// Prépare la réception -- DataInputStream est un wrapper (permet utliser dis.readInt(), readFully(bytes), readUTF("string") )
			//DOS est pour les flux SORTANT // DIS est pour les flux ENTRANT

				identifier = chargerIdLocal(); 		// Récupère l'id si il existe déjà

				dos.writeInt(identifier);	// 
				dos.flush();

				if (identifier == 0 ) { 		// -- 0 car un int Nao//
					identifier = dis.readInt();
					sauvegarderIdLocal(identifier);		// Sauvegarde l'id si nouveau
				}

				// start the receive loop
				new Thread(() -> receiveLoop()).start();
				notifyConnectionListeners(true);
			} catch (IOException e) {
				//e.printStackTrace();
				// error, close session
				closeSession();
			}
		}
	}





	/**
	 * Send a packet to the specified destination (etiher a userId or groupId)
	 * 
	 * @param destId the destinatiion id
	 * @param data   the data to be sent
	 */
	public void sendPacket(int destId, byte[] data) {
		try {
			synchronized (dos) {
				dos.writeInt(destId);
				dos.writeInt(data.length);
				dos.write(data);
				dos.flush();
			}
		} catch (IOException e) {
			// error, connection closed
			closeSession();
		}
		
	}



	/**
	 * Start the receive loop. Has to be called only once.
	 */
	private void receiveLoop() {
		try {
			while (s != null && !s.isClosed()) {

				int sender = dis.readInt();
				int dest = dis.readInt();
				int length = dis.readInt();
				byte[] data = new byte[length];
				dis.readFully(data);
				notifyMessageListeners(new Packet(sender, dest, data));

			}
		} catch (IOException e) {
			// error, connection closed
		}
		closeSession();
	}



	public void closeSession() {
		try {
			if (s != null)
				s.close();
		} catch (IOException e) {
		}
		s = null;
		notifyConnectionListeners(false);
	}





//--- LE MAIN POUR LES TESTS -------------------------------------
	public static void main(String[] args) throws UnknownHostException, IOException, InterruptedException {
		ClientMsg c = new ClientMsg("localhost", 1666);

		// add a dummy listener that print the content of message as a string
		c.addMessageListener(p -> System.out.println(p.srcId + " says to " + p.destId + ": " + new String(p.data)));
		
		// add a connection listener that exit application when connection closed
		c.addConnectionListener(active ->  {if (!active) System.exit(0);});

		c.startSession();
		System.out.println("Vous êtes : " + c.getIdentifier());

		// Thread.sleep(5000);

		// l'utilisateur avec id 4 crée un grp avec 1 et 3 dedans (et lui meme)
		if (c.getIdentifier() == 4) {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dos = new DataOutputStream(bos);

			// byte 1 : create group on server
			dos.writeByte(1);

			// nb members
			dos.writeInt(2);
			// list members
			dos.writeInt(1);
			dos.writeInt(3);
			dos.flush();

			c.sendPacket(0, bos.toByteArray());

		}
		
		

		Scanner sc = new Scanner(System.in);
		String lu = null;
		while (!"\\quit".equals(lu)) {
			try {
				System.out.println("A qui voulez vous écrire ? ");
				int dest = Integer.parseInt(sc.nextLine());

				System.out.println("Votre message ? ");
				lu = sc.nextLine();
				c.sendPacket(dest, lu.getBytes());
			} catch (InputMismatchException | NumberFormatException e) {
				System.out.println("Mauvais format");
			}

		}


		/*
		 * int id =1+(c.getIdentifier()-1) % 2; System.out.println("send to "+id);
		 * c.sendPacket(id, "bonjour".getBytes());
		 * 
		 * 
		 * Thread.sleep(10000);
		 */

		c.closeSession();

	}

}
