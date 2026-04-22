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

import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.UnknownHostException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;	//new
import java.sql.ResultSet;			//new
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.InputMismatchException;
import java.util.List;
import java.util.Scanner;

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

				if (action==2) { //gérer un groupe
					try {
						System.out.println("Tapez 1 pour créer un groupe");
						System.out.println("Tapez 2 pour quitter un groupe");				
						System.out.println("Tapez 3 pour gérer un groupe existant dont vous être propriétaire");
						int actionGroupe = Integer.parseInt(sc.nextLine()); //récupere la valeur 

						if (actionGroupe==1) { //créer un groupe
							try {
								ByteArrayOutputStream bos = new ByteArrayOutputStream(); //on rajoute une place dans le buffer pour le groupe
								DataOutputStream dos = new DataOutputStream(bos);
								// byte 1 : create group on server
								dos.writeByte(1);

								System.out.println("Nom du groupe ?");
								String nomGroupe = sc.nextLine(); 
								///////////TODO voir lien BDD

								System.out.println("Combien de personnes voulez-vous ajouter ?");
								int nbrMembre = Integer.parseInt(sc.nextLine());								
								if(nbrMembre<=0){throw new IllegalArgumentException("Doit être positif");}
								dos.writeInt(nbrMembre); //reserve les bits avec le nbr de places

								System.out.println("Qui voulez-vous ajouter :");
								//avec les id
								for(int i=1; i<=nbrMembre; i++){//demande le meme nombre d'id qu'annoncé avant
									int idMembre = Integer.parseInt(sc.next());
									if (idMembre==c.getIdentifier()) { throw new IllegalArgumentException("ne peut pas s'ajouter soi-même");}
									dos.writeInt(idMembre);
									System.out.println(idMembre+" a été ajouté");
								} 
								System.out.println(nomGroupe +" a été crée");

								c.sendPacket(0, bos.toByteArray());
								
							} catch (InputMismatchException | NumberFormatException e) {
								System.out.println("Mauvais format");
							}
							
						}

						if (actionGroupe==2) {//quitter un groupe
							try {
								ByteArrayOutputStream bos = new ByteArrayOutputStream(); //on rajoute une place dans le buffer pour le groupe
								DataOutputStream dos = new DataOutputStream(bos);								
								//voir les groupes dont l'user est membre, en selectionner un 

								dos.writeByte(2); 

								System.out.println("id du groupe que vous souhaitez quitter");
								int idGroup = Integer.parseInt(sc.nextLine());
								dos.writeInt(idGroup);
								dos.flush();

								//demande confirmation
								System.out.println("Souhaitez-vous quitter ce groupe ?"); //rajouter nom groupe TODO
								System.out.println("1 : oui				0 : non");
								if(Integer.parseInt(sc.nextLine())==1){
									c.sendPacket(0, bos.toByteArray());
								}							
							} catch (Exception e) {
								System.out.println("Mauvais format");
							}
							
						}				
						
						if (actionGroupe==3) {//gérer un groupe existant avec les droits owner
							try {
								//1ere étape choisir le groupe, TODO puis :

								System.out.println("Tapez 1 pour ajouter un utilisateur");
								System.out.println("Tapez 2 pour supprimer un utilisateur");				
								System.out.println("Tapez 3 pour modifier le nom du groupe");
								System.out.println("Tapez 4 pour transferer le droit de propriété du groupe");
								System.out.println("Tapez 5 pour supprimer le groupe");
								int actionGroupeAdmin = Integer.parseInt(sc.nextLine()); //récupere la valeur 

								if (actionGroupeAdmin==1) {//ajouter un utilisateur
									try {
										
									} catch (Exception e) {
										// TODO: handle exception
									}
								}
								
								if (actionGroupeAdmin==2) {//supprimer un utilisateur
									try {
										
									} catch (Exception e) {
										// TODO: handle exception
									}
								}

								if (actionGroupeAdmin==3) {//modifier nom du groupe
									try {
										
									} catch (Exception e) {
										// TODO: handle exception
									}
								}

								if (actionGroupeAdmin==4) {//transferer le droit de propriété du groupe
									try {
										
									} catch (Exception e) {
										// TODO: handle exception
									}
								}

								if (actionGroupeAdmin==5) {//supprimer le groupe
									try {
										
									} catch (Exception e) {
										// TODO: handle exception
									}
								}

							} catch (Exception e) {
								// TODO: handle exception
							}
						}
					}catch (Exception e) {
						// TODO: handle exception
					}
				}
		
				if (action==3) {//gestion des contacts
					try{
						System.out.println("Tapez 1 pour ajouter un contact");
						System.out.println("Tapez 2 pour supprimer un contact");				
						System.out.println("Tapez 3 pour modifier le nom d'un contact");	
						int actionContact = Integer.parseInt(sc.nextLine()); //récupere la valeur 

						if(actionContact==1){ //ajouter un contact
							try {
								
							} catch (Exception e) {
								// TODO: handle exception
							}
						}

						if(actionContact==2){ //supprimer un contact
							try {
								
							} catch (Exception e) {
								// TODO: handle exception
							}
						}	
							
						if(actionContact==3){ //modifier un contact
							try {
								
							} catch (Exception e) {
								// TODO: handle exception
							}
						}

						
					} catch (Exception e) {
						// TODO: handle exception
					}
				}
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
