
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableModel;
public class FerienwohnungGUI {
    private JFrame frame;
    private JComboBox<String> countryDropdown;
    private JComboBox<String> equipmentDropdown;
    private JTextField arrivalDateField;
    private JTextField departureDateField;
    private JButton searchButton;
    private JButton bookButton;
    private FerienwohnungDB db;
    private JTable table;
    private DefaultTableModel tableModel;
    private JTextField ferienwohnungNameField;
    private JTable bookingsTable;
    private DefaultTableModel bookingsTableModel;
    private JButton loginButton;
    private String customerEmail;
     public FerienwohnungGUI() {
        
        frame = new JFrame("Ferienwohnung");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(600, 400);
        frame.setLayout(new FlowLayout());
        ferienwohnungNameField = new JTextField(20);
        JLabel countryLabel = new JLabel("Country:");
        countryDropdown = new JComboBox<>();
        JLabel equipmentLabel = new JLabel("Equipment:");
        equipmentDropdown = new JComboBox<>();
        JLabel arrivalDateLabel = new JLabel("Arrival Date:");
        arrivalDateField = new JTextField(10);
        JLabel departureDateLabel = new JLabel("Departure Date:");
        departureDateField = new JTextField(10);
        searchButton = new JButton("Search");
        bookButton = new JButton("Book");
        tableModel = new DefaultTableModel();
        table = new JTable(tableModel);
        String[] columnNames = {
            "Name", "Durchschnittliche Bewertung"
        };
        tableModel.setColumnIdentifiers(columnNames);
        countryDropdown.addItem("");  
        equipmentDropdown.addItem(""); 
        bookingsTableModel = new DefaultTableModel();
        bookingsTable = new JTable(bookingsTableModel);
        loginButton = new JButton("Login");

        table.getSelectionModel().addListSelectionListener(new ListSelectionListener(){
            public void valueChanged(ListSelectionEvent event) {
                int selectedRow = table.getSelectedRow();
                if (selectedRow >= 0) { 
                    String ferienwohnungName = (String) table.getValueAt(selectedRow, 0);
                    ferienwohnungNameField.setText(ferienwohnungName);
                }
            }
        });

        try {
            db = new FerienwohnungDB("jdbc:oracle:thin:@oracle19c.in.htwg-konstanz.de:1521:ora19c", "dbsys14", "3464");
            boolean isConnected = db.checkConnection();
            if (isConnected) {
                System.out.println("Connected to the database successfully.");
            } else {
                System.out.println("Failed to connect to the database.");
            }
            ResultSet countries = db.getCountries();
            while (countries.next()) {
                countryDropdown.addItem(countries.getString("NameLand"));
            }
            ResultSet equipment = db.getEquipment();
            while (equipment.next()) {
                equipmentDropdown.addItem(equipment.getString("AusstattungNamebes"));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

    searchButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String arrivalDateText = arrivalDateField.getText();
            String departureDateText = departureDateField.getText();
            Date arrivalDate = arrivalDateText.isEmpty() ? null : Date.valueOf(arrivalDateText);
            Date departureDate = departureDateText.isEmpty() ? null : Date.valueOf(departureDateText);
            String country = (String) countryDropdown.getSelectedItem();
            String equipment = (String) equipmentDropdown.getSelectedItem();

            List<String[]> searchResults = db.searchFerienwohnungen(country, arrivalDate, departureDate, equipment);
            tableModel.setRowCount(0);

            for (String[] row : searchResults) {
            
                for (String cell : row) {
                    System.out.print(cell + "\t");
                }
                System.out.println();
                tableModel.addRow(row);
            }
        } catch (SQLException sqlException) {
            sqlException.printStackTrace();
        } catch (Exception generalException) {
            generalException.printStackTrace();
        }
        frame.add(new JScrollPane(table)); 
        frame.setVisible(true);  
    }

});

loginButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
    
        JFrame loginFrame = new JFrame("Login");
        loginFrame.setSize(300, 100);
        loginFrame.setLayout(new FlowLayout());

    
        JTextField emailField = new JTextField(20);
        loginFrame.add(new JLabel("Email:"));
        loginFrame.add(emailField);

        JButton loginAction = new JButton("Login");
        loginFrame.add(loginAction);

        loginAction.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                customerEmail = emailField.getText();
                JOptionPane.showMessageDialog(null, "Logged in successfully!");
                loginFrame.dispose();
            }
        });

        loginFrame.setVisible(true); 
    }
});
        
bookButton.addActionListener(new ActionListener() {
    @Override
    public void actionPerformed(ActionEvent e) {
        try {
            String apartmentName = ferienwohnungNameField.getText();
            if (customerEmail == null || customerEmail.isEmpty()) {
                JOptionPane.showMessageDialog(null, "Please login first!");
                return;
            }
            db.bookFerienwohnung(
                    customerEmail, 
                    apartmentName,
                    Date.valueOf(arrivalDateField.getText()),
                    Date.valueOf(departureDateField.getText())
            );
            JOptionPane.showMessageDialog(null, "Successfully booked!");
        String[] columnNames = {
            "Buchungsnummer ", "Enddatum", "Buchungsdatum", "Anfangsdatum", "Kunde", "NameFerienwohnung"
        };
        bookingsTableModel.setColumnIdentifiers(columnNames);
       
            List<String[]> customerBookings = db.getCustomerBookings(customerEmail);
            bookingsTableModel.setRowCount(0);  
            for (String[] booking : customerBookings) {
                for (String cell : booking) {
                    System.out.print(cell + "\t");
                }
                System.out.println();
                bookingsTableModel.addRow(booking); 
            }

         
            JFrame bookingsFrame = new JFrame("Bookings");
            bookingsFrame.setSize(600, 400);
            bookingsFrame.add(new JScrollPane(bookingsTable));
            bookingsFrame.setVisible(true); 
        } catch (SQLException ex) {
            ex.printStackTrace();
        }
    }
});
        frame.add(countryLabel);
        frame.add(countryDropdown);
        frame.add(equipmentLabel);
        frame.add(equipmentDropdown);
        frame.add(arrivalDateLabel);
        frame.add(arrivalDateField);
        frame.add(departureDateLabel);
        frame.add(departureDateField);
        frame.add(searchButton);
        frame.add(bookButton);
        frame.add(table);
        frame.add(new JLabel("Ferienwohnung Name:"));
        frame.add(ferienwohnungNameField);
        frame.add(loginButton);
        frame.setVisible(true);
    }

   public static void main(String[] args) {
            new FerienwohnungGUI();
        }
 
}