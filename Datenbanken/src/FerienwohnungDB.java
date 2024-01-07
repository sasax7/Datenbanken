
import java.sql.Connection;
import java.sql.Date;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;

import oracle.jdbc.OracleDriver;
public class FerienwohnungDB {
    private Connection connection;

    public FerienwohnungDB(String url, String user, String password) throws SQLException {
        DriverManager.registerDriver(new OracleDriver());
        this.connection = DriverManager.getConnection(url, user, password);
        this.connection.setTransactionIsolation(Connection.TRANSACTION_SERIALIZABLE);
        this.connection.setAutoCommit(false);
    }

public ResultSet getCountries() throws SQLException {
    Statement stmt = connection.createStatement();
    return stmt.executeQuery("SELECT DISTINCT NameLand FROM Ferienwohnung");
}

public ResultSet getEquipment() throws SQLException {
    Statement stmt = connection.createStatement();
    return stmt.executeQuery("SELECT DISTINCT AusstattungNamebes FROM Besitzt");
}

public List<String[]> searchFerienwohnungen(String country, Date arrivalDate, Date departureDate, String equipment) throws SQLException {
    List<String[]> results = new ArrayList<>();

    String query = "SELECT f.fwName AS FerienwohnungsName, COALESCE(AVG(b.Sternanzahl), 0) AS DurchschnittlicheBewertung " +
               "FROM Ferienwohnung f " +
               "JOIN Besitzt bs ON f.fwName = bs.FerienwohnungNamebes " +
               "LEFT JOIN Buchung bu ON f.fwName = bu.NameFerienwohnung " +
               "LEFT JOIN Bewertung b ON bu.Buchungsnummer = b.Buchungsnummer " +
               "WHERE bs.AusstattungNamebes = ? AND f.NameLand = ? " +
               "AND f.fwName NOT IN ( " +
               "    SELECT bu.NameFerienwohnung " +
               "    FROM Buchung bu " +
               "    WHERE (bu.AnfangsDatum >= ? AND bu.AnfangsDatum <= ?) " +
               "       OR (bu.Enddatum >= ? AND bu.Enddatum <= ?) " +
               "       OR (? >= bu.AnfangsDatum AND ? <= bu.Enddatum) " +
               ") " +
               "GROUP BY f.fwName " +
               "ORDER BY 2 DESC";

    try (PreparedStatement stmt = this.connection.prepareStatement(query)) {
        
        stmt.setString(1, equipment); 
        stmt.setString(2, country);  
        stmt.setDate(3, arrivalDate);  
        stmt.setDate(4, departureDate);  
        stmt.setDate(5, arrivalDate); 
        stmt.setDate(6, departureDate);  
        stmt.setDate(7, arrivalDate); 
        stmt.setDate(8, departureDate); 
        ResultSet rs = stmt.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getString(i + 1); 
            }
            results.add(row);
        }
    }
    return results;
}

    



public boolean checkConnection() {
    try {
        Statement stmt = connection.createStatement();
        ResultSet rs = stmt.executeQuery("SELECT 1 FROM dual");
        return rs.next();
    } catch (SQLException ex) {
        ex.printStackTrace();
        return false;
    }
}

    public void bookFerienwohnung(String customerEmail, String apartmentName, Date startDate, Date endDate) throws SQLException {
    try {
        connection.setAutoCommit(false);

   
        String countQuery = "SELECT COUNT(*) FROM Buchung";
        PreparedStatement countStmt = connection.prepareStatement(countQuery);
        ResultSet rs = countStmt.executeQuery();
        rs.next();
        int buchungsnummer = rs.getInt(1) * 7;

       
        Date buchungsdatum = new java.sql.Date(System.currentTimeMillis());

        String query = "INSERT INTO Buchung (Email, NameFerienwohnung, AnfangsDatum, Enddatum, Buchungsdatum, Buchungsnummer) VALUES (?, ?, ?, ?, ?, ?)";
        PreparedStatement stmt = connection.prepareStatement(query);
        stmt.setString(1, customerEmail);
        stmt.setString(2, apartmentName);
        stmt.setDate(3, startDate);
        stmt.setDate(4, endDate);
        stmt.setDate(5, buchungsdatum);
        stmt.setInt(6, buchungsnummer);
        stmt.executeUpdate();

        connection.commit();
    } catch (SQLException e) {
        connection.rollback();
        throw e;
    } finally {
        connection.setAutoCommit(true);
    }
}
public List<String[]> getCustomerBookings(String customerEmail) throws SQLException {
    List<String[]> bookings = new ArrayList<>();

    String query = "SELECT * FROM Buchung WHERE Email = ?";
    try (PreparedStatement stmt = this.connection.prepareStatement(query)) {
        stmt.setString(1, customerEmail);
        ResultSet rs = stmt.executeQuery();
        ResultSetMetaData rsmd = rs.getMetaData();
        int columnCount = rsmd.getColumnCount();

        while (rs.next()) {
            String[] row = new String[columnCount];
            for (int i = 0; i < columnCount; i++) {
                row[i] = rs.getString(i + 1); 
            }
            bookings.add(row);
        }
    }

    return bookings;
}
}