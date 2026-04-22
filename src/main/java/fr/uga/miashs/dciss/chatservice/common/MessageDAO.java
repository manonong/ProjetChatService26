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

package fr.uga.miashs.dciss.chatservice.common;

import fr.uga.miashs.dciss.chatservice.common.db.DatabaseManager;
import fr.uga.miashs.dciss.chatservice.common.model.Message;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class MessageDAO {

    public void saveTextMessage(int senderId, int receiverId, String content) {
        String sql =
                "INSERT INTO messages(sender_id, receiver_id, content, type, file_path, filename)"+
                "VALUES (?, ?, ?, ?, NULL, NULL);";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, content);
            ps.setString(4, "TEXT");
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur saveTextMessage", e);
        }
    }

    public void saveFileMessage(int senderId, int receiverId, String filename, String filePath) {
        String sql =
                "INSERT INTO messages(sender_id, receiver_id, content, type, file_path, filename)"+
                "VALUES (?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, "[FILE] " + filename);
            ps.setString(4, "FILE");
            ps.setString(5, filePath);
            ps.setString(6, filename);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur saveFileMessage", e);
        }
    }

    public List<Message> getConversation(int userA, int userB) {
        String sql =
                "SELECT * FROM messages"+
                "WHERE (sender_id = ? AND receiver_id = ?)"+
                   "OR (sender_id = ? AND receiver_id = ?)"+
                "ORDER BY created_at ASC, id ASC";


        List<Message> messages = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, userA);
            ps.setInt(2, userB);
            ps.setInt(3, userB);
            ps.setInt(4, userA);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    Message m = new Message();
                    m.setId(rs.getInt("id"));
                    m.setSenderId(rs.getInt("sender_id"));
                    m.setReceiverId(rs.getInt("receiver_id"));
                    m.setContent(rs.getString("content"));
                    m.setType(rs.getString("type"));
                    m.setFilePath(rs.getString("file_path"));
                    m.setFilename(rs.getString("filename"));
                    m.setCreatedAt(rs.getString("created_at"));
                    messages.add(m);
                }
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur getConversation", e);
        }

        return messages;
    }
}