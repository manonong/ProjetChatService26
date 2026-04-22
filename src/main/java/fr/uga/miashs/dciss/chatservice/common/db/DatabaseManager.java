package fr.uga.miashs.dciss.chatservice.common.db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public class DatabaseManager {

    private static final String URL = "jdbc:sqlite:chat.db";

    // 获取连接
    public static Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL);
    }

    // 初始化数据库
    public static void initDatabase() {

        String createMessagesTable =
                "CREATE TABLE IF NOT EXISTS messages (" +
                        "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                        "sender_id INTEGER, " +
                        "receiver_id INTEGER, " +
                        "content TEXT, " +
                        "type TEXT, " +
                        "created_at DATETIME DEFAULT CURRENT_TIMESTAMP" +
                        ");";

        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.execute(createMessagesTable);
            System.out.println("Database initialized ✅");

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 保存消息
    public static void saveMessage(int sender, int receiver, String content, String type) {

        String sql = "INSERT INTO messages (sender_id, receiver_id, content, type) VALUES (?, ?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, sender);
            ps.setInt(2, receiver);
            ps.setString(3, content);
            ps.setString(4, type);

            ps.executeUpdate();

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // 查看所有消息
    public static void showAllMessages() {

        String sql = "SELECT sender_id, receiver_id, content, created_at FROM messages ORDER BY created_at ASC";

        try (Connection conn = getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            System.out.println("\n===== CHAT HISTORY =====");

            while (rs.next()) {
                int sender = rs.getInt("sender_id");
                int receiver = rs.getInt("receiver_id");
                String content = rs.getString("content");
                String time = rs.getString("created_at");

                System.out.println("[" + time + "] " + sender + " -> " + receiver + " : " + content);
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}