package fr.uga.miashs.dciss.chatservice.db;

import java.sql.*;

public class DatabaseManager {

    private Connection cnx;

    public DatabaseManager() {
        try {
            cnx = DriverManager.getConnection("jdbc:derby:target/chatDB;create=true");
            initTables();
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void initTables() throws SQLException {
        Statement stmt = cnx.createStatement();

        try {
            stmt.executeUpdate(
                "CREATE TABLE Groups (groupId INT PRIMARY KEY, ownerId INT)"
            );
        } catch (SQLException ignored) {}

        try {
            stmt.executeUpdate(
                "CREATE TABLE GroupMembers (groupId INT, userId INT, PRIMARY KEY (groupId, userId))"
            );
        } catch (SQLException ignored) {}
    }

    // ===== GROUP =====
    public void insertGroup(int groupId, int ownerId) {
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO Groups VALUES (?, ?)"
            );
            ps.setInt(1, groupId);
            ps.setInt(2, ownerId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ===== MEMBER =====
    public void insertMember(int groupId, int userId) {
        try {
            PreparedStatement ps = cnx.prepareStatement(
                "INSERT INTO GroupMembers VALUES (?, ?)"
            );
            ps.setInt(1, groupId);
            ps.setInt(2, userId);
            ps.executeUpdate();
            ps.close();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
