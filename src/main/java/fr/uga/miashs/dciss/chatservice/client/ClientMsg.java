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
import fr.uga.miashs.dciss.chatservice.common.db.DatabaseManager;
import fr.uga.miashs.dciss.chatservice.common.db.MessageDAO;

import java.io.*;
import java.net.Socket;
import java.nio.file.*;
import java.util.Scanner;

public class ClientMsg {

	private String serverAddress;
	private int serverPort;

	private Socket socket;
	private DataOutputStream dos;
	private DataInputStream dis;

	private int identifier;

	public static final int TYPE_TEXT = 1;
	public static final int TYPE_FILE = 2;

	public ClientMsg(String address, int port) {
		this(0, address, port);
	}

	public ClientMsg(int id, String address, int port) {
		this.identifier = id;
		this.serverAddress = address;
		this.serverPort = port;
	}

	// ======================
	// CONNECTION
	// ======================

	public void startSession() {
		try {
			socket = new Socket(serverAddress, serverPort);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());

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

	// ======================
	// RECEIVE
	// ======================

	private void receiveLoop() {
		try {
			while (true) {
				int src = dis.readInt();
				int dest = dis.readInt();
				int len = dis.readInt();

				byte[] data = new byte[len];
				dis.readFully(data);

				DataInputStream dis2 = new DataInputStream(new ByteArrayInputStream(data));
				int type = dis2.readInt();

				if (type == TYPE_TEXT) {
					String msg = dis2.readUTF();
					System.out.println(src + " says: " + msg);

				} else if (type == TYPE_FILE) {
					String filename = dis2.readUTF();
					int size = dis2.readInt();

					byte[] fileBytes = new byte[size];
					dis2.readFully(fileBytes);

					new File("downloads").mkdirs();
					String path = "downloads/" + System.currentTimeMillis() + "_" + filename;

					Files.write(Paths.get(path), fileBytes);

					System.out.println("File received: " + path);
				}
			}
		} catch (Exception e) {
			System.out.println("Connection closed");
		}
	}

	// ======================
	// SEND
	// ======================

	public void sendPacket(int destId, byte[] data) throws IOException {
		dos.writeInt(destId);
		dos.writeInt(data.length);
		dos.write(data);
		dos.flush();
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
			path = path.replace("\"", "");

			File file = new File(path);

			if (!file.exists() || file.isDirectory()) {
				System.out.println("Invalid file!");
				return;
			}

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

	// ======================
	// MAIN (USER INTERFACE)
	// ======================

	public static void main(String[] args) {

		DatabaseManager.initDatabase();

		ClientMsg client = new ClientMsg("localhost", 1666);
		client.startSession();

		System.out.println("Votre ID: " + client.identifier);

		Scanner sc = new Scanner(System.in);

		while (true) {
			try {
				System.out.println("\n=== MENU ===");
				System.out.println("1. Send message");
				System.out.println("2. Send file");
				System.out.println("3. Show history");

				int choice = Integer.parseInt(sc.nextLine());

				if (choice == 1) {
					System.out.println("Destinataire ?");
					int dest = Integer.parseInt(sc.nextLine());

					System.out.println("Message ?");
					String msg = sc.nextLine();

					client.sendTextMessage(dest, msg);

				} else if (choice == 2) {
					System.out.println("Destinataire ?");
					int dest = Integer.parseInt(sc.nextLine());

					System.out.println("File path ?");
					String path = sc.nextLine();

					client.sendFileMessage(dest, path);

				} else if (choice == 3) {
					new MessageDAO().showAllMessages();
				}

			} catch (Exception e) {
				System.out.println("Erreur");
			}
		}
	}
}