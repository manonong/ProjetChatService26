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

package fr.uga.miashs.dciss.chatservice.common;

import java.sql.*;

public class ExempleConnexionDB {

	public static void main(String[] args) {

		try {
			Connection cnx = DriverManager.getConnection("jdbc:derby:target/sample;create=true");

			// eviter l'erreur quand creer le table
			try {
				cnx.createStatement().executeUpdate(
						"CREATE TABLE MsgUser (id INT PRIMARY KEY, nickname VARCHAR(20))"
				);
				System.out.println("Table created");
			} catch (SQLException e) {
				System.out.println("Table already exists, skip creation");
			}

			// inserer les datas
			PreparedStatement pstmt = cnx.prepareStatement("INSERT INTO MsgUser VALUES (?,?)");

			pstmt.setInt(1, 35);
			pstmt.setString(2, "titi");

			try {
				pstmt.executeUpdate();
			} catch (SQLException e) {
				System.out.println("Data already exists, skip insert");
			}

			// rechercher les donnees
			ResultSet res = cnx.createStatement().executeQuery("SELECT * FROM MsgUser");

			while (res.next()) {
				System.out.println(res.getInt(1) + " - " + res.getString(2));
			}

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
}
