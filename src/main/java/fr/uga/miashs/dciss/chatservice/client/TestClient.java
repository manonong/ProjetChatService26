/*
 * Copyright (c) 2026.  Jerome David. Univ. Grenoble Alpes.
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
import java.nio.file.*;
import java.util.*;

public class TestClient {


    public static void main(String[] args) {

        // initialiser base de donnee
        DatabaseManager.initDatabase();

        ClientMsg client = new ClientMsg("localhost", 1666);

        client.addMessageListener(p -> {
            try {
                ByteArrayInputStream bis = new ByteArrayInputStream(p.data);
                DataInputStream dis = new DataInputStream(bis);

                int type = dis.readInt();

                if (type == ClientMsg.TYPE_TEXT) {
                    String msg = dis.readUTF();
                    System.out.println(p.srcId + " says: " + msg);

                } else if (type == ClientMsg.TYPE_FILE) {

                    String filename = dis.readUTF();
                    int size = dis.readInt();

                    byte[] fileBytes = new byte[size];
                    dis.readFully(fileBytes);

                    new File("downloads").mkdirs();

                    String path = "downloads/" + System.currentTimeMillis() + "_" + filename;

                    Files.write(Paths.get(path), fileBytes);

                    System.out.println("Fichier reçu: " + path);
                }

            } catch (Exception e) {
                e.printStackTrace();
            }
        });

        client.startSession();
        System.out.println("Mon ID: " + client.getIdentifier());

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
                    client.sendFileMessage(dest, path);
                } else {
                    System.out.println("Message ?");
                    String msg = sc.nextLine();
                    client.sendTextMessage(dest, msg);
                }
                if ("\\history".equalsIgnoreCase(type)) {
                    new MessageDAO().showAllMessages();
                    continue;
                }

            } catch (Exception e) {
                System.out.println("Erreur");
            }
        }
    }
}