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

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;

import fr.uga.miashs.dciss.chatservice.common.Packet; //données à envoyer

public class GroupMsg implements PacketProcessor {

	private int groupId; //id du groupe, négatif
	private UserMsg owner; //proprietaire du groupe, càd celui qui le crée
	private Set<UserMsg> members; //ensemble des membres du groupe
	
	public GroupMsg(int groupId, UserMsg owner) { //un groupe est définit par son id et son proprietaire
		if (groupId>-1) throw new IllegalArgumentException("id must be less than 0");
		if (owner==null) throw new IllegalArgumentException("owner cannot be null");
		//si l'id est négatif ou si le proprietaire n'existe pas, lance une exception
		this.groupId=groupId;
		this.owner=owner;
		members=Collections.synchronizedSet(new HashSet<>());
		//permet d'utiliser un thread plusieurs fois en même temps
		//thread : une séquence d'instructions pouvant être exécutées indépendamment au sein d'un programme
		addMember(owner); //ajoute le proprietaire au groupe
	}
	
	public int getId() { //getter
		return groupId;
	}
	
	/**
	 * This method has to be used to add a member to the group.
	 * It update the bidirectional relationship, i.e. the user is added to the group and the the group is added to the user.
	 * @param s
	 * @return
	 */
	public boolean addMember(UserMsg s) { //s est un user
		return s!=null && members.add(s) && s.getGroups().add(this);
		//retourne vrai si l'user existe, ajouter l'user dans le groupe et met le groupe dans le repertoire de l'user
	}
	
	/**
	 * This method has to be used to remove a member from the group.
	 * It update the bidirectional relationship, i.e. the user is removed from the group and the the group is removed from the user.
	 * @param s
	 * @return
	 */
	public boolean removeMember(UserMsg s) {
		if (s.equals(owner)) return false; //si l'user est le owner on ne peut pas le retirer
		if (members.remove(s)) { //on retire le membre
			s.removeGroup(this); //on retire le groupe du repertoire de l'user
			return true;//retourne true pour dire que la suppression est effective 
		}
		return false;//faux si s n'est pas présent dans members
	}
	
	@Override
	public void process(Packet p) {
		// send packet to members except the sender.
		members.stream().filter(m->m.getId()!=p.srcId).forEach( m -> m.process(p));
		//créer un flux, p.srcId c'est l'expéditeur alors on ne lui envoie pas, puis envoie à chaque membre avec le foreach
	}
	
	// to be used carrefully, because it does not update birectional relationship in case of addition or removal.
	//setter, mais faire attention parce que update pas la relation complete
	protected Set<UserMsg> getMembers() {
		return members;
	}
	
	/*
	 * This method has to be called when removing a group in order to clean bidirectional membership.
	 */
	public void beforeDelete() {
		members.forEach(m->m.getGroups().remove(this));
		//avant de supprimer un groupe, permet de retirer le groupe du répertoire des membres
	}

}
