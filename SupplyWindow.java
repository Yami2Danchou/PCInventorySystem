import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.*;
import java.time.LocalDate;

public class SupplyWindow extends JFrame {

    private JTextField dateField, quantityField;
    private JButton addSupplyButton;
    private String itemName;

    private DefaultTableModel inventoryModel; // reference to update table

    public SupplyWindow(String itemName, DefaultTableModel inventoryModel) {
        this.itemName = itemName;
        this.inventoryModel = inventoryModel;

        setTitle("Add Supply - " + itemName);
        setSize(300, 200);
        setLocationRelativeTo(null);
        setLayout(new GridLayout(4, 2, 10, 10));

        dateField = new JTextField(LocalDate.now().toString());
        quantityField = new JTextField();
        addSupplyButton = new JButton("Add Supply");

        add(new JLabel("Date:"));
        add(dateField);
        add(new JLabel("Quantity:"));
        add(quantityField);
        add(new JLabel()); // spacer
        add(addSupplyButton);

        addSupplyButton.addActionListener(this::addSupply);
    }

    private void addSupply(ActionEvent e) {
        String date = dateField.getText().trim();
        String quantityStr = quantityField.getText().trim();

        if (date.isEmpty() || quantityStr.isEmpty()) {
            JOptionPane.showMessageDialog(this, "All fields must be filled.", "Input Error", JOptionPane.WARNING_MESSAGE);
            return;
        }

        try {
            int addedQty = Integer.parseInt(quantityStr);
            if (addedQty <= 0) throw new NumberFormatException();

            // Append to supply.txt
            try (BufferedWriter writer = new BufferedWriter(new FileWriter("supply.txt", true))) {
                writer.write(itemName + "," + date + "," + addedQty);
                writer.newLine();
            }

            // Update inventory table model quantity
            boolean found = false;
            for (int i = 0; i < inventoryModel.getRowCount(); i++) {
                if (inventoryModel.getValueAt(i, 2).toString().equalsIgnoreCase(itemName)) {
                    int currentQty = Integer.parseInt(inventoryModel.getValueAt(i, 4).toString());
                    int newQty = currentQty + addedQty;
                    inventoryModel.setValueAt(String.valueOf(newQty), i, 4);
                    found = true;
                    break;
                }
            }

            // Optional: Save back to item.txt
            if (found) updateItemFile();

            JOptionPane.showMessageDialog(this, "Supply added and inventory updated.");
            this.dispose();

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid positive number.", "Input Error", JOptionPane.WARNING_MESSAGE);
        } catch (IOException ex) {
            JOptionPane.showMessageDialog(this, "Error updating inventory: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    // Rewrite item.txt with updated inventory
    private void updateItemFile() throws IOException {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter("item.txt"))) {
            for (int i = 0; i < inventoryModel.getRowCount(); i++) {
                writer.write(String.join(",",
                        inventoryModel.getValueAt(i, 0).toString(),
                        inventoryModel.getValueAt(i, 1).toString(),
                        inventoryModel.getValueAt(i, 2).toString(),
                        inventoryModel.getValueAt(i, 3).toString(),
                        inventoryModel.getValueAt(i, 4).toString()));
                writer.newLine();
            }
        }
    }
}
