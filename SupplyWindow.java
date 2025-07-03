import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;

public class SupplyWindow extends JFrame {
    private JTextField dateField, quantityField;
    private JButton addSupplyButton;
    private String itemName;

    public SupplyWindow(String itemName) {
        this.itemName = itemName;
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
        add(new JLabel());
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
            int quantity = Integer.parseInt(quantityStr);
            if (quantity <= 0) throw new NumberFormatException();

            try (BufferedWriter writer = new BufferedWriter(new FileWriter("supply.txt", true))) {
                writer.write(itemName + "," + date + "," + quantity);
                writer.newLine();
                JOptionPane.showMessageDialog(this, "Supply added successfully.");
                this.dispose();
            } catch (IOException ex) {
                JOptionPane.showMessageDialog(this, "Error writing supply: " + ex.getMessage(), "File Error", JOptionPane.ERROR_MESSAGE);
            }

        } catch (NumberFormatException ex) {
            JOptionPane.showMessageDialog(this, "Quantity must be a valid positive number.", "Input Error", JOptionPane.WARNING_MESSAGE);
        }
    }
} 