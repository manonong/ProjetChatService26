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

package fr.uga.miashs.dciss.chatservice.common.db;

import java.sql.*;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:chat.db";

    public static Connection getConnection() throws SQLException {
        try {
            Class.forName("org.sqlite.JDBC"); // important!!
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
        return DriverManager.getConnection("jdbc:sqlite:chat.db");
    }

    public static void initDatabase() {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            String messagesTable =
                    "CREATE TABLE IF NOT EXISTS messages (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "sender_id INTEGER, " +
                            "receiver_id INTEGER, " +
                            "content TEXT, " +
                            "type TEXT, " +
                            "file_path TEXT, " +
                            "filename TEXT, " +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ");";

            String filesTable =
                    "CREATE TABLE IF NOT EXISTS files (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                            "sender_id INTEGER, " +
                            "receiver_id INTEGER, " +
                            "filename TEXT, " +
                            "file_path TEXT, " +
                            "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                            ");";

            stmt.execute(messagesTable);
            stmt.execute(filesTable);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    public void showAllMessages() {

        String sql = "SELECT sender_id, receiver_id, content, created_at FROM messages";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("===== CHAT HISTORY =====");

            while (rs.next()) {
                int sender = rs.getInt("sender_id");
                int receiver = rs.getInt("receiver_id");
                String content = rs.getString("content");
                String time = rs.getString("created_at");

                System.out.println("[" + time + "] " + sender + " -> " + receiver + " : " + content);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}