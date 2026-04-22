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

import fr.uga.miashs.dciss.chatservice.common.Packet;
import fr.uga.miashs.dciss.chatservice.common.db.*;

import java.io.*;
import java.net.Socket;
import java.util.*;
import java.nio.file.*;

public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket s;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;

	public static final int TYPE_TEXT = 1;
	public static final int TYPE_FILE = 2;

	private List<MessageListener> listeners = new ArrayList<>();

	public ClientMsg(String address, int port) {
		this(0, address, port);
	}

	public ClientMsg(int id, String address, int port) {
		this.identifier = id;
		this.serverAddress = address;
		this.serverPort = port;
	}

	public void addMessageListener(MessageListener l) {
		listeners.add(l);
	}

	private void notifyListeners(Packet p) {
		listeners.forEach(l -> l.messageReceived(p));
	}

	public void startSession() {
		try {
			s = new Socket(serverAddress, serverPort);
			dos = new DataOutputStream(s.getOutputStream());
			dis = new DataInputStream(s.getInputStream());

			dos.writeInt(identifier);
			dos.flush();

			if (identifier == 0) {
				identifier = dis.readInt();
			}

			new Thread(this::receiveLoop).start();

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private void receiveLoop() {
		MessageDAO messageDAO = new MessageDAO();
		FileDAO fileDAO = new FileDAO();

		try {
			while (true) {
				int src = dis.readInt();
				int dest = dis.readInt();
				int len = dis.readInt();

				byte[] data = new byte[len];
				dis.readFully(data);

				Packet p = new Packet(src, dest, data);
				notifyListeners(p);

				// 👉 decode
				DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(data));
				int type = dis2.readInt();

				if (type == TYPE_TEXT) {
					String msg = dis2.readUTF();
					messageDAO.saveTextMessage(src, dest, msg);

				} else if (type == TYPE_FILE) {

					String filename = dis2.readUTF();
					int size = dis2.readInt();

					byte[] fileBytes = new byte[size];
					dis2.readFully(fileBytes);

					String path = "downloads/" + System.currentTimeMillis() + "_" + filename;
					new File("downloads").mkdirs();
					Files.write(Paths.get(path), fileBytes);

//					fileDAO.saveFile(src, dest, filename, path);
//					messageDAO.saveFileMessage(src, dest, filename, path);
				}
			}
		} catch (Exception e) {
			System.out.println("Connexion fermée");
		}
	}

	public void sendPacket(int destId, byte[] data) {
		try {
			dos.writeInt(destId);
			dos.writeInt(data.length);
			dos.write(data);
			dos.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendTextMessage(int destId, String msg) {
		try {
			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dosLocal = new DataOutputStream(bos);

			dosLocal.writeInt(TYPE_TEXT);
			dosLocal.writeUTF(msg);

			sendPacket(destId, bos.toByteArray());

			new MessageDAO().saveTextMessage(identifier, destId, msg);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void sendFileMessage(int destId, String path) {
		try {
			File file = new File(path);
			byte[] fileBytes = Files.readAllBytes(file.toPath());

			ByteArrayOutputStream bos = new ByteArrayOutputStream();
			DataOutputStream dosLocal = new DataOutputStream(bos);

			dosLocal.writeInt(TYPE_FILE);
			dosLocal.writeUTF(file.getName());
			dosLocal.writeInt(fileBytes.length);
			dosLocal.write(fileBytes);

			sendPacket(destId, bos.toByteArray());

			new MessageDAO().saveFileMessage(identifier, destId, file.getName(), path);

		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public int getIdentifier() {
		return identifier;
	}
	public static void main(String[] args) {

		// initialiser bdd
		fr.uga.miashs.dciss.chatservice.common.db.DatabaseManager.initDatabase();

		ClientMsg c = new ClientMsg("localhost", 1666);

		// accepter les infos
		c.addMessageListener(p -> {
			try {
				ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
				DataInputStream dis = new DataInputStream(bis);

				int type = dis.readInt();

				if (type == TYPE_TEXT) {
					String msg = dis.readUTF();
					System.out.println(p.srcId + " says: " + msg);

					new fr.uga.miashs.dciss.chatservice.common.db.MessageDAO()
							.saveTextMessage(p.srcId, p.destId, msg);

				} else if (type == TYPE_FILE) {

					String filename = dis.readUTF();
					int size = dis.readInt();

					byte[] fileBytes = new byte[size];
					dis.readFully(fileBytes);

					File folder = new File("downloads");
					if (!folder.exists()) folder.mkdirs();

					String newName = System.currentTimeMillis() + "_" + filename;
					String path = "downloads/" + newName;

					Files.write(Paths.get(path), fileBytes);

					System.out.println("Fichier reçu de " + p.srcId + " : " + newName);

					new fr.uga.miashs.dciss.chatservice.common.db.FileDAO()
							.saveFile(p.srcId, p.destId, filename, path);

					new fr.uga.miashs.dciss.chatservice.common.db.MessageDAO()
							.saveFileMessage(p.srcId, p.destId, filename, path);
				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		});

		// acceder le lien
		c.startSession();
		System.out.println("Votre ID : " + c.getIdentifier());

		Scanner sc = new Scanner(System.in);

		while (true) {
			try {
				System.out.println("Destinataire ?");
				int dest = Integer.parseInt(sc.nextLine());

				System.out.println("Message (m) ou fichier (f) ?");
				String type = sc.nextLine();

				if ("f".equalsIgnoreCase(type)) {
					System.out.println("Chemin du fichier ?");
					String path = sc.nextLine();
					c.sendFileMessage(dest, path);

				} else {
					System.out.println("Message ?");
					String msg = sc.nextLine();
					c.sendTextMessage(dest, msg);
				}

			} catch (Exception e) {
				System.out.println("Erreur");
			}
		}
	}
}
