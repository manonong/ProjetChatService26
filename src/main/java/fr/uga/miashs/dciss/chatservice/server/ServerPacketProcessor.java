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

import java.nio.ByteBuffer;
import java.util.logging.Logger;

import fr.uga.miashs.dciss.chatservice.common.Packet;

public class ServerPacketProcessor implements PacketProcessor {
	private final static Logger LOG = Logger.getLogger(ServerPacketProcessor.class.getName());
	private ServerMsg server;

	public ServerPacketProcessor(ServerMsg s) {
		this.server = s;
	}

	
    @Override
    public void process(Packet p) {

    // Création d'un buffer pour lire les données du paquet
    ByteBuffer buf = ByteBuffer.wrap(p.data);

    // Lecture du type d'action (1er byte du payload)
    byte type = buf.get();

    // GESTION DES GROUPES
    if (type == 1) { // création de groupe
        server.createGroup(p.srcId);
    }
    else if (type == 2) { // ajout d'un utilisateur à un groupe

        // lecture de l'id du groupe
        int groupId = buf.getInt();

        // lecture de l'id de l'utilisateur
        int userId = buf.getInt();

        // ajout via ServerMsg
        if (!server.addUserToGroup(userId, groupId)) {
            LOG.warning("Ajout utilisateur au groupe échoué");
        }
    }
    else if (type == 3) { // suppression d'un utilisateur d'un groupe

        // lecture de l'id du groupe
        int groupId = buf.getInt();

        // lecture de l'id de l'utilisateur
        int userId = buf.getInt();

        // suppression via ServerMsg
        if (!server.removeUserFromGroup(userId, groupId)) {
            LOG.warning("Suppression utilisateur du groupe échouée");
        }
    }
    else if (type == 4) { // suppression de groupe

        // lecture de l'id du groupe
        int groupId = buf.getInt();

        // suppression du groupe côté serveur
        if (!server.removeGroup(groupId)) {
            LOG.warning("Suppression du groupe échouée");
        }
    }

    // MESSAGES
    else if (type == 10) { // message utilisateur/groupe

        // lecture taille message
        int length = buf.getInt();

        // lecture contenu
        byte[] data = new byte[length];
        buf.get(data);

        // création d'un packet de message
        Packet msg = new Packet(p.srcId, p.destId, data);

        // routage normal (user / group / server)
        server.processPacket(msg);
    }

    // CAS INCONNU
    else {
        LOG.warning("Type de paquet inconnu : " + type);
    }
    }


	
	public void createGroup(int ownerId, ByteBuffer data) {
		int nb = data.getInt();
		GroupMsg g = server.createGroup(ownerId);
		for (int i = 0; i < nb; i++) {
			g.addMember(server.getUser(data.getInt()));
		}
	}

}
