package fr.uga.miashs.dciss.chatservice.client;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.IOException;
import java.net.Socket;
import java.nio.file.Files;
import java.util.function.Consumer;

public class ClientMsg {

    private String serverAddress;
    private int serverPort;

    private Socket socket;
    private DataOutputStream dos;
    private DataInputStream dis;

    private int identifier;

    public static final int TYPE_TEXT = 1;
    public static final int TYPE_FILE = 2;

    // 👉 新增：监听器
    private Consumer<String> messageListener;

    public ClientMsg(String address, int port) {
        this(0, address, port);
    }

    public ClientMsg(int id, String address, int port) {
        this.identifier = id;
        this.serverAddress = address;
        this.serverPort = port;
    }

    // ======================
    // Getter（解决报错）
    // ======================
    public int getIdentifier() {
        return identifier;
    }

    // ======================
    // Listener（解决报错）
    // ======================
    public void addMessageListener(Consumer<String> listener) {
        this.messageListener = listener;
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

                    String fullMsg = src + " says: " + msg;

                    // 👉 控制台输出
                    System.out.println(fullMsg);

                    // 👉 触发监听器
                    if (messageListener != null) {
                        messageListener.accept(fullMsg);
                    }

                } else if (type == TYPE_FILE) {
                    String filename = dis2.readUTF();
                    int size = dis2.readInt();

                    byte[] fileBytes = new byte[size];
                    dis2.readFully(fileBytes);

                    new File("downloads").mkdirs();
                    String path = "downloads/" + System.currentTimeMillis() + "_" + filename;

                    Files.write(new File(path).toPath(), fileBytes);

                    String info = "File received: " + path;
                    System.out.println(info);

                    if (messageListener != null) {
                        messageListener.accept(info);
                    }
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendFileMessage(int destId, String path) {
        try {
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

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
	public static void main(String[] args) {

    ClientMsg client = new ClientMsg("localhost", 1666);
    client.startSession();

    System.out.println("Your ID: " + client.getIdentifier());

    java.util.Scanner sc = new java.util.Scanner(System.in);

    while (true) {
        try {
            System.out.println("\n=== MENU ===");
            System.out.println("1. Send message");
            System.out.println("2. Send file");

            int choice = Integer.parseInt(sc.nextLine());

            if (choice == 1) {
                System.out.println("Dest ID?");
                int dest = Integer.parseInt(sc.nextLine());

                System.out.println("Message?");
                String msg = sc.nextLine();

                client.sendTextMessage(dest, msg);

            } else if (choice == 2) {
                System.out.println("Dest ID?");
                int dest = Integer.parseInt(sc.nextLine());

                System.out.println("File path?");
                String path = sc.nextLine();

                client.sendFileMessage(dest, path);
            }

        } catch (Exception e) {
            System.out.println("Error");
        }
    }
}
}