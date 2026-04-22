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
import fr.uga.miashs.dciss.chatservice.common.model.FileRecord;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class FileDAO {

    public void saveFileRecord(int senderId, int receiverId, String filename, String filePath) {
        String sql =
                "INSERT INTO files(sender_id, receiver_id, filename, file_path)"+
                "VALUES (?, ?, ?, ?)";

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, senderId);
            ps.setInt(2, receiverId);
            ps.setString(3, filename);
            ps.setString(4, filePath);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new RuntimeException("Erreur saveFileRecord", e);
        }
    }

    public List<FileRecord> getAllFiles() {
        String sql = "SELECT * FROM files ORDER BY created_at DESC, id DESC";
        List<FileRecord> records = new ArrayList<>();

        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                FileRecord record = new FileRecord();
                record.setId(rs.getInt("id"));
                record.setSenderId(rs.getInt("sender_id"));
                record.setReceiverId(rs.getInt("receiver_id"));
                record.setFilename(rs.getString("filename"));
                record.setFilePath(rs.getString("file_path"));
                record.setCreatedAt(rs.getString("created_at"));
                records.add(record);
            }

        } catch (SQLException e) {
            throw new RuntimeException("Erreur getAllFiles", e);
        }

        return records;
    }
}