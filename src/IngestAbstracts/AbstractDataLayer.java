// John Arquette
// Date: 11-16-2024
// FacultyResearchDB Data Layer for Handling Abstracts

import java.sql.*;
import java.util.HashMap;
import java.util.Map;

public class AbstractDataLayer {
    private Connection conn;
    private final String DEFAULT_DRIVER = "com.mysql.cj.jdbc.Driver";

    public AbstractDataLayer() {}

    public boolean connect(String databaseName, String userName, String password) {
        conn = null;
        String url = "jdbc:mysql://localhost/" + databaseName + "?serverTimezone=UTC";

        try {
            Class.forName(DEFAULT_DRIVER);
            conn = DriverManager.getConnection(url, userName, password);
            System.out.println("Connected to the database.");
        } catch (ClassNotFoundException cnfe) {
            System.out.println("No DB connection");
            System.out.println("Class not found");
            System.out.println("ERROR MESSAGE -> " + cnfe);
            return false;
        } catch (SQLException sqle) {
            System.out.println("ERROR SQL Exception in connect()");
            System.out.println("ERROR MESSAGE -> " + sqle);
            sqle.printStackTrace();
            return false;
        }

        return conn != null;
    }

    public int insertAbstract(String title, String abstractContent) {
        // Check if the abstract already exists
        String checkSql = "SELECT abstractID FROM Abstract WHERE title = ? AND abstractFile = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setString(1, title);
            checkStmt.setString(2, abstractContent);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // If an existing record is found, return the existing abstractID
                System.out.println("Abstract already exists: " + title);
                return rs.getInt("abstractID");
            }
        } catch (SQLException e) {
            System.out.println("ERROR checking for existing abstract: " + e.getMessage());
        }

        // Insert the abstract if it doesn't exist
        String insertSql = "INSERT INTO Abstract (title, abstractFile) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, title);
            ps.setString(2, abstractContent);
            ps.executeUpdate();

            ResultSet rs = ps.getGeneratedKeys();
            if (rs.next()) {
                return rs.getInt(1); // Return the generated abstractID
            }
        } catch (SQLException e) {
            System.out.println("ERROR inserting abstract: " + e.getMessage());
        }
        return -1;
    }

    public Map<String, Integer> getFacultyMap() {
        Map<String, Integer> facultyMap = new HashMap<>();
        String query = "SELECT facultyID, CONCAT(firstName, ' ', lastName) AS fullName FROM Faculty";
        try (Statement stmt = conn.createStatement(); ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) {
                int facultyID = rs.getInt("facultyID");
                String fullName = rs.getString("fullName");
                facultyMap.put(fullName, facultyID);
            }
        } catch (SQLException e) {
            System.out.println("ERROR retrieving faculty data: " + e.getMessage());
        }
        return facultyMap;
    }

    public void linkFacultyToAbstract(int facultyID, int abstractID) {
        // Check if the link already exists
        String checkSql = "SELECT * FROM Faculty_Abstract WHERE facultyID = ? AND abstractID = ?";
        try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
            checkStmt.setInt(1, facultyID);
            checkStmt.setInt(2, abstractID);
            ResultSet rs = checkStmt.executeQuery();
            if (rs.next()) {
                // If the link already exists, skip insertion
                System.out.println("Link between facultyID " + facultyID + " and abstractID " + abstractID + " already exists.");
                return;
            }
        } catch (SQLException e) {
            System.out.println("ERROR checking for existing link: " + e.getMessage());
        }

        // Insert the link if it doesn't exist
        String insertSql = "INSERT INTO Faculty_Abstract (facultyID, abstractID) VALUES (?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
            ps.setInt(1, facultyID);
            ps.setInt(2, abstractID);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.out.println("ERROR linking faculty to abstract: " + e.getMessage());
        }
    }

    public void close() {
        try {
            if (conn != null) conn.close();
        } catch (SQLException sqle) {
            System.out.println("ERROR IN METHOD close() -> " + sqle);
        }
    }
}