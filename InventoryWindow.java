import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.*;
import java.time.LocalDate;
import java.util.Vector;

public class InventoryWindow extends JFrame {

    // UI Components
    private JTable table;
    private DefaultTableModel model;
    private JTextField nameField, priceField, quantityField, searchField;
    private JComboBox<String> typeCombo;
    private JButton addButton, editButton, deleteButton, switchToSalesButton, searchButton, addTypeButton, addSupplyButton, viewSupplyButton;

    // Unique ID tracker for new items
    private int currentId = 1;

    // File paths
    private static final String ITEM_FILE = "item.txt";
    private static final String TYPE_FILE = "type.txt";

    // Constructor: Sets up the entire inventory GUI
    public InventoryWindow() {
        setTitle("Inventory System");
        setSize(1000, 550);
        setDefaultCloseOperation(EXIT_ON_CLOSE);
        setLocationRelativeTo(null);

        // Initialize type dropdown and load types from file
        typeCombo = new JComboBox<>();
        loadTypes();

        // Table model (non-editable cells)
        model = new DefaultTableModel(new Object[]{"ID", "Type", "Name", "Price", "Quantity"}, 0) {
            @Override
            public boolean isCellEditable(int row, int column) {
                return false;
            }
        };

        table = new JTable(model);
        JScrollPane tableScroll = new JScrollPane(table);

        // Top input form panel
        JPanel inputPanel = new JPanel(new GridLayout(2, 6, 10, 10));
        nameField = new JTextField();
        priceField = new JTextField();
        quantityField = new JTextField();
        searchField = new JTextField();

        inputPanel.add(new JLabel("Type"));
        inputPanel.add(new JLabel("Name"));
        inputPanel.add(new JLabel("Price"));
        inputPanel.add(new JLabel("Quantity"));
        inputPanel.add(new JLabel("Search by ID"));
        inputPanel.add(new JLabel());

        inputPanel.add(typeCombo);
        inputPanel.add(nameField);
        inputPanel.add(priceField);
        inputPanel.add(quantityField);
        searchButton = new JButton("Search");
        inputPanel.add(searchField);
        inputPanel.add(searchButton);

        // Button panel at the bottom
        JPanel buttonPanel = new JPanel();
        addButton = new JButton("Add");
        editButton = new JButton("Edit");
        deleteButton = new JButton("Delete");
        switchToSalesButton = new JButton("Sales Mode");
        addTypeButton = new JButton("Add Type");
        addSupplyButton = new JButton("Add Supply");
        viewSupplyButton = new JButton("Supply Record");

        addSupplyButton.setVisible(false); // Hidden unless item is selected

        // Add buttons to panel
        buttonPanel.add(addSupplyButton);
        buttonPanel.add(viewSupplyButton);
        buttonPanel.add(addButton);
        buttonPanel.add(editButton);
        buttonPanel.add(deleteButton);
        buttonPanel.add(addTypeButton);
        buttonPanel.add(switchToSalesButton);

        // Layout UI components
        add(tableScroll, BorderLayout.CENTER);
        add(inputPanel, BorderLayout.NORTH);
        add(buttonPanel, BorderLayout.SOUTH);

        // Load existing inventory from file
        loadFromFile();

        // Button actions
        addButton.addActionListener(e -> addItem());
        editButton.addActionListener(e -> editItem());
        deleteButton.addActionListener(e -> deleteItem());
        switchToSalesButton.addActionListener(e -> switchToSales());
        addTypeButton.addActionListener(e -> addNewType());
        searchButton.addActionListener(e -> searchById());

        // Open SupplyWindow for selected item
        addSupplyButton.addActionListener(e -> {
            int row = table.getSelectedRow();
            if (row != -1) {
                String itemName = model.getValueAt(row, 2).toString(); // Get item name
                new SupplyWindow(itemName, model).setVisible(true);
            }
        });

        // Display supply record log
        viewSupplyButton.addActionListener(e -> {
            File file = new File("supply.txt");
            if (!file.exists()) {
                JOptionPane.showMessageDialog(this, "No supply records found.");
                return;
            }

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error reading supply records: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
                return;
            }

            JTextArea textArea = new JTextArea(content.toString());
            textArea.setEditable(false);
            JScrollPane scrollPane = new JScrollPane(textArea);
            scrollPane.setPreferredSize(new Dimension(500, 300));
            JOptionPane.showMessageDialog(this, scrollPane, "Supply Records", JOptionPane.INFORMATION_MESSAGE);
        });

        // Handle table row selection
        table.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                int row = table.getSelectedRow();
                if (row != -1) {
                    addSupplyButton.setVisible(true);
                    typeCombo.setSelectedItem(model.getValueAt(row, 1).toString());
                    nameField.setText(model.getValueAt(row, 2).toString());
                    priceField.setText(model.getValueAt(row, 3).toString());
                    quantityField.setText(model.getValueAt(row, 4).toString());
                }
            }
        });

        // Save inventory when program closes
        Runtime.getRuntime().addShutdownHook(new Thread(this::saveToFile));
    }

    // Add item or increase quantity if it exists
    private void addItem() {
        if (validateFields()) {
            String name = nameField.getText().trim();
            boolean updated = false;

            // Check if item already exists
            for (int i = 0; i < model.getRowCount(); i++) {
                if (model.getValueAt(i, 2).toString().equalsIgnoreCase(name)) {
                    int currentQty = Integer.parseInt(model.getValueAt(i, 4).toString());
                    int addedQty = Integer.parseInt(quantityField.getText().trim());
                    model.setValueAt(currentQty + addedQty, i, 4);
                    logUpdate(name, addedQty);
                    updated = true;
                    break;
                }
            }

            // Add new item if not existing
            if (!updated) {
                model.addRow(new Object[]{
                        String.format("%03d", currentId++),
                        typeCombo.getSelectedItem().toString(),
                        name,
                        priceField.getText().trim(),
                        quantityField.getText().trim()
                });
            }
            clearFields();
            addSupplyButton.setVisible(false);
        }
    }

    // Log item restocking activity
    private void logUpdate(String name, int qty) {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("log.txt", true))) {
            writer.write(name + ", added " + qty + " on " + LocalDate.now());
            writer.newLine();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Edit selected item
    private void editItem() {
        int row = table.getSelectedRow();
        if (row != -1 && validateFields()) {
            String newName = nameField.getText().trim();

            // Prevent duplicate names
            for (int i = 0; i < model.getRowCount(); i++) {
                if (i != row && model.getValueAt(i, 2).toString().equalsIgnoreCase(newName)) {
                    JOptionPane.showMessageDialog(this, "Item with this name already exists.", "Duplicate Item", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }

            // Update fields
            model.setValueAt(typeCombo.getSelectedItem().toString(), row, 1);
            model.setValueAt(newName, row, 2);
            model.setValueAt(priceField.getText().trim(), row, 3);
            model.setValueAt(quantityField.getText().trim(), row, 4);
            clearFields();
        } else if (row == -1) {
            JOptionPane.showMessageDialog(this, "Select a row to edit.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Delete selected item
    private void deleteItem() {
        int row = table.getSelectedRow();
        if (row != -1) {
            int confirm = JOptionPane.showConfirmDialog(this, "Are you sure you want to delete this item?", "Confirm Deletion", JOptionPane.YES_NO_OPTION);
            if (confirm == JOptionPane.YES_OPTION) {
                model.removeRow(row);
                clearFields();
            }
        } else {
            JOptionPane.showMessageDialog(this, "Select a row to delete.", "No Row Selected", JOptionPane.WARNING_MESSAGE);
        }
    }

    // Switch to SalesWindow view
    private void switchToSales() {
        saveToFile(); // Save changes before switching
        SalesWindow salesWindow = new SalesWindow(this, model);
        salesWindow.setVisible(true);
        this.setVisible(false);
    }

    // Add new item type to dropdown and save to file
    private void addNewType() {
        String newType = JOptionPane.showInputDialog(this, "Enter new type:");
        if (newType != null && !newType.trim().isEmpty()) {
            typeCombo.addItem(newType.trim());
            try (BufferedWriter writer = new BufferedWriter(new FileWriter(TYPE_FILE, true))) {
                writer.write(newType.trim());
                writer.newLine();
            } catch (IOException e) {
                JOptionPane.showMessageDialog(this, "Error saving type: " + e.getMessage());
            }
        }
    }

    // Search item by ID
    private void searchById() {
        String id = searchField.getText().trim();
        for (int i = 0; i < model.getRowCount(); i++) {
            if (model.getValueAt(i, 0).toString().equals(id)) {
                table.setRowSelectionInterval(i, i);
                table.scrollRectToVisible(table.getCellRect(i, 0, true));
                return;
            }
        }
        JOptionPane.showMessageDialog(this, "Item ID not found.");
    }

    // Validate item input fields
    private boolean validateFields() {
        if (nameField.getText().trim().isEmpty() || priceField.getText().trim().isEmpty() || quantityField.getText().trim().isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields must be filled.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        try {
            double price = Double.parseDouble(priceField.getText().trim());
            if (price < 0) throw new NumberFormatException();
            int qty = Integer.parseInt(quantityField.getText().trim());
            if (qty < 0) throw new NumberFormatException();
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Price must be a valid number and Quantity must be a valid integer.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return false;
        }
        return true;
    }

    // Clear input fields and reset selection
    private void clearFields() {
        nameField.setText("");
        priceField.setText("");
        quantityField.setText("");
        typeCombo.setSelectedIndex(0);
        table.clearSelection();
    }

    // Save inventory items to file
    void saveToFile() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(ITEM_FILE))) {
            for (int i = 0; i < model.getRowCount(); i++) {
                Vector<?> row = model.getDataVector().elementAt(i);
                writer.write(String.join(",",
                        row.get(0).toString(), row.get(1).toString(), row.get(2).toString(),
                        row.get(3).toString(), row.get(4).toString()));
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving inventory: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load inventory items from file
    private void loadFromFile() {
        File file = new File(ITEM_FILE);
        if (!file.exists()) return;
        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            model.setRowCount(0);
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 5) {
                    model.addRow(new Object[]{parts[0], parts[1], parts[2], parts[3], parts[4]});
                    int parsedId = Integer.parseInt(parts[0]);
                    if (parsedId >= currentId) currentId = parsedId + 1;
                }
            }
        } catch (IOException | NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Error loading inventory: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Load item types from file or use default types
    private void loadTypes() {
        File file = new File(TYPE_FILE);
        if (file.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    typeCombo.addItem(line.trim());
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String[] defaultTypes = {
                    "System Unit", "PC Set", "Mouse", "Keyboard", "Monitor", "PC Case",
                    "SSD", "GPU", "CPU", "Motherboard", "RAM", "Fan", "PSU", "Headphone"
            };
            for (String type : defaultTypes) {
                typeCombo.addItem(type);
            }
        }
    }

    // Entry point: launch the application
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryWindow().setVisible(true));
    }
}
