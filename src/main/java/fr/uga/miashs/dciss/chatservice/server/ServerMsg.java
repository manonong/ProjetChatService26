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

package fr.uga.miashs.dciss.chatservice.server;

import java.io.*;
import java.net.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

import java.util.*;

public class ServerMsg {
	
	private final static Logger LOG = Logger.getLogger(ServerMsg.class.getName());
	public final static int SERVER_CLIENTID = 0;

	private transient ServerSocket serverSock;
	private transient boolean started;
	private transient ExecutorService executor;
	private transient ServerPacketProcessor sp;
	
	// maps pour associer les id aux users et groupes
	private Map<Integer, UserMsg> users;
	private Map<Integer, GroupMsg> groups;
	
	// séquences pour générer les identifiant d'utilisateurs et de groupe
	private AtomicInteger nextUserId;
	private AtomicInteger nextGroupId;



	public ServerMsg(int port) throws IOException {
		serverSock = new ServerSocket(port);
		started = false;
		users = new ConcurrentHashMap<>();
		groups = new ConcurrentHashMap<>(); 
		nextUserId = new AtomicInteger(1);
		nextGroupId = new AtomicInteger(-1);
		sp = new ServerPacketProcessor(this);
		executor = Executors.newCachedThreadPool();	} //le nombre clients réactif est limité par le nb de processeurs sur certaines machines
	
	public GroupMsg createGroup(int ownerId) {
		UserMsg owner = users.get(ownerId);
		if (owner==null) throw new ServerException("User with id="+ownerId+" unknown. Group creation failed.");
		int id = nextGroupId.getAndDecrement();
		GroupMsg res = new GroupMsg(id,owner);
		groups.put(id, res);
		LOG.info("Group "+res.getId()+" created");
		return res;
	}
	

	// Retourne un groupe à partir de son identifiant
    public GroupMsg getGroup(int groupId) {
    return groups.get(groupId);
    }


	// Ajoute un utilisateur à un groupe
    public boolean addUserToGroup(int userId, int groupId) {

    // récupération de l'utilisateur
    UserMsg u = users.get(userId);

    // récupération du groupe
    GroupMsg g = groups.get(groupId);

    // vérification de l'existence des deux éléments
    if (u == null || g == null) return false;

    // ajout via la logique du groupe (relation bidirectionnelle gérée dans GroupMsg)
    return g.addMember(u);
    }



	// Retire un utilisateur d'un groupe
    public boolean removeUserFromGroup(int userId, int groupId) {

    // récupération de l'utilisateur
    UserMsg u = users.get(userId);

    // récupération du groupe
    GroupMsg g = groups.get(groupId);

    // vérification de l'existence des deux éléments
    if (u == null || g == null) return false;

    // suppression via la logique du groupe
    return g.removeMember(u);
    }




	public boolean removeGroup(int groupId) {
		GroupMsg g =groups.remove(groupId);
		if (g==null) return false;
		g.beforeDelete();
		return true;
	}
	
	public boolean removeUser(int userId) {
		UserMsg u =users.remove(userId);
		if (u==null) return false;
		u.beforeDelete();
		return true;
	}
	
	public UserMsg getUser(int userId) {
		return users.get(userId);
	}
	
	// À implémenter — restaure un UserMsg depuis la sérialisation
	private UserMsg chargerUserDepuisSauvegarde(int userId) {
   	 	// TODO : désérialiser depuis la base de données SQL
    	return null;
	}

	// Methode utilisée pour savoir quoi faire d'un paquet
	// reçu par le serveur
	public void processPacket(Packet p) {

    // Référence vers le processeur qui va traiter le paquet
    PacketProcessor pp = null;

    // Cas 1 : message destiné à un groupe
    if (p.destId < 0) { //message de groupe

        // can be send only if sender is member
        // Récupération de l'expéditeur du message
        UserMsg sender = users.get(p.srcId);

        // Récupération du groupe destinataire
        GroupMsg g = groups.get(p.destId);

        // Vérification de sécurité : éviter NullPointerException et accès non autorisé
        if (g != null && sender != null) {

            // Vérifie que l'expéditeur appartient bien au groupe
            if (g.getMembers().contains(sender)) {

                // Le groupe devient responsable du traitement du paquet
                pp = g;
            }
        }
    }

    // Cas 2 : message entre utilisateurs
    else if (p.destId > 0) { // message entre utilisateurs

        // Récupération de l'utilisateur destinataire
        pp = users.get(p.destId);
    }

    // Cas 3 : message de gestion pour le serveur
    else { // message de gestion pour le serveur

        // Le serveur traite directement ce type de message
        pp = sp;
    }

    // Exécution du traitement si un processeur valide a été trouvé
    if (pp != null) {

        // Délégation du traitement du paquet
        pp.process(p);
    }
	}




	/**
	 * 
	 */
	public void start() {
		started = true;
		while (started) {
			try {
				// Le serveur attend une connexion d'un client :
				Socket s = serverSock.accept();
				// Prépare les canaux de d'entrée et de sortie :
				DataInputStream dis = new DataInputStream(s.getInputStream());
				DataOutputStream dos = new DataOutputStream(s.getOutputStream());

				//*** LECTURE DE L'ID *** --lit l'identifiant du client
				int userId = dis.readInt();

				//*** GESTION DES ID *** --là où on galère
				if (userId == 0) { 
					// *** NOUVEAU CLIENT ***
    				// id non défini (0) -- on crée un nouvel utilisateur
					userId = nextUserId.getAndIncrement();
					dos.writeInt(userId);
					dos.flush();
					users.put(userId, new UserMsg(userId, this));
				}

				UserMsg x = users.get(userId);
					// *** CLIENT CONNU ***
					// si x != null ici -- l'utilisateur est déjà en mémoire, reconnexion directe

					//*** CLIENT CONNU MAIS ABSENT DE LA MEMOIRE *** --vérification de si l'utilisateur est en mémoire (SQL) 
					// on tente de le restaurer depuis la sérialisation
				if (x == null) {
    				x = chargerUserDepuisSauvegarde(userId); 
    				if (x != null) {	
					//Si avec la restauration on a retrouvé l'utilisateur : on met à jour la HashMap (clé, valeur)
        				users.put(userId, x);
    				}
				}

				final UserMsg y = x;	
				//!!! *** On est obligé de faire car on utilise des fonctions anonymes par la suite ***
				// et ça fout la merde sinon x/

				if (y!= null && x.open(s)) {
					// *** CONNEXION ETABLIE *** (nouveau, connu ou restauré)
					LOG.info(userId + " connected");
					executor.submit(() -> y.receiveLoop());		// lancement boucle de reception --recevoir les messages
					executor.submit(() -> y.sendLoop());		// lancement boucle d'envoi --envoyer des messages
				
					// si l'identifiant existe ou est nouveau alors deux "taches"/boucles sont lancées en parrallèle :
					// une pour recevoir les messages du client, 
					// une pour envoyer des messages au client
					// les deux boucles sont gérées au niveau de la classe UserMsg


				} else { 	// *** EJECTION *** -- id invalide (MÉCHANT PAS BEAU essaye de s'infiltrer en testant de id au pif) ou restauration échouée
					s.close();
				}

			} catch (IOException e) {
				LOG.info("Close server");
				e.printStackTrace();
			}
		}
	}

	public void stop() {
		started = false;
		try {
			serverSock.close();
			users.values().forEach(s -> s.close());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void main(String[] args) throws IOException {
		ServerMsg s = new ServerMsg(1666);
		s.start();
	}

}
