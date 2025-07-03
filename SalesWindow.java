import javax.swing.*;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableModel;
import javax.swing.table.TableColumnModel;
import java.awt.*;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Vector;

public class SalesWindow extends JFrame {
    private JTable table;
    private DefaultTableModel inventoryModel;
    private DefaultTableModel salesDisplayModel;
    private JButton processSaleButton, salesReportButton, switchToInventoryButton;
    private List<SaleRecord> salesList = new ArrayList<>();

    private static final String SALES_FILE_NAME = "sales_records.txt";

    public SalesWindow(JFrame inventoryWindow, DefaultTableModel sharedModel) {
        setTitle("Sales System");
        setSize(1000, 500);
        setLocationRelativeTo(null);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);

        this.inventoryModel = sharedModel;

        String[] columnNames = {"Select", "ID", "Type", "Name", "Price", "Sale Quantity", "Available Quantity"};
        salesDisplayModel = new DefaultTableModel(columnNames, 0) {
            @Override
            public Class<?> getColumnClass(int columnIndex) {
                if (columnIndex == 0) return Boolean.class;
                if (columnIndex == 5) return String.class;
                return super.getColumnClass(columnIndex);
            }

            @Override
            public boolean isCellEditable(int row, int column) {
                return column == 0 || column == 5;
            }
        };

        table = new JTable(salesDisplayModel);
        JScrollPane scrollPane = new JScrollPane(table);

        TableColumnModel tcm = table.getColumnModel();
        tcm.getColumn(0).setMaxWidth(50);
        tcm.getColumn(0).setCellRenderer(table.getDefaultRenderer(Boolean.class));
        tcm.getColumn(0).setCellEditor(table.getDefaultEditor(Boolean.class));

        JTextField saleQtyInput = new JTextField();
        saleQtyInput.addKeyListener(new KeyAdapter() {
            @Override
            public void keyTyped(KeyEvent e) {
                char c = e.getKeyChar();
                if (!Character.isDigit(c) && c != KeyEvent.VK_BACK_SPACE && c != KeyEvent.VK_DELETE) {
                    e.consume();
                }
            }
        });
        tcm.getColumn(5).setCellEditor(new DefaultCellEditor(saleQtyInput));

        loadSalesDisplayTable();
        loadSalesRecords();

        processSaleButton = new JButton("Process Sale");
        salesReportButton = new JButton("Sales Report");
        switchToInventoryButton = new JButton("Inventory Mode");

        JPanel buttonPanel = new JPanel();
        buttonPanel.add(processSaleButton);
        buttonPanel.add(salesReportButton);
        buttonPanel.add(switchToInventoryButton);

        add(scrollPane, BorderLayout.CENTER);
        add(buttonPanel, BorderLayout.SOUTH);

        processSaleButton.addActionListener(e -> processSale());
        salesReportButton.addActionListener(e -> showSalesReport());
        switchToInventoryButton.addActionListener(e -> {
            ((InventoryWindow) inventoryWindow).saveToFile();
            inventoryWindow.setVisible(true);
            this.dispose();
        });

        salesDisplayModel.addTableModelListener(e -> {
            if (e.getColumn() == 5) {
                int row = e.getFirstRow();
                String saleQtyStr = salesDisplayModel.getValueAt(row, 5).toString();
                if (!saleQtyStr.isEmpty()) {
                    try {
                        int saleQty = Integer.parseInt(saleQtyStr);
                        int availableQty = Integer.parseInt(salesDisplayModel.getValueAt(row, 6).toString());

                        if (saleQty < 0) {
                            JOptionPane.showMessageDialog(this, "Sale quantity cannot be negative.", "Input Error", JOptionPane.WARNING_MESSAGE);
                            salesDisplayModel.setValueAt("", row, 5);
                        } else if (saleQty > availableQty) {
                            JOptionPane.showMessageDialog(this, "Sale quantity exceeds available quantity.", "Input Error", JOptionPane.WARNING_MESSAGE);
                            salesDisplayModel.setValueAt("", row, 5);
                        }
                    } catch (NumberFormatException ex) {
                        JOptionPane.showMessageDialog(this, "Please enter a valid number for sale quantity.", "Input Error", JOptionPane.WARNING_MESSAGE);
                        salesDisplayModel.setValueAt("", row, 5);
                    }
                }
            }
        });

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                saveSalesRecords();
                super.windowClosing(e);
            }
        });
    }

    private void loadSalesDisplayTable() {
        salesDisplayModel.setRowCount(0);
        for (int i = 0; i < inventoryModel.getRowCount(); i++) {
            Vector<?> rowData = inventoryModel.getDataVector().elementAt(i);
            salesDisplayModel.addRow(new Object[]{
                    false,
                    rowData.get(0),
                    rowData.get(1),
                    rowData.get(2),
                    rowData.get(3),
                    "",
                    rowData.get(4)
            });
        }
    }

    private void processSale() {
        List<SaleRecord> currentSaleItems = new ArrayList<>();
        double totalSaleAmount = 0.0;
        String currentDateTime = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));

        for (int i = 0; i < salesDisplayModel.getRowCount(); i++) {
            Boolean selected = (Boolean) salesDisplayModel.getValueAt(i, 0);
            if (selected) {
                String type = salesDisplayModel.getValueAt(i, 2).toString();
                String name = salesDisplayModel.getValueAt(i, 3).toString();
                double price = Double.parseDouble(salesDisplayModel.getValueAt(i, 4).toString());
                int availableQty = Integer.parseInt(salesDisplayModel.getValueAt(i, 6).toString());
                String saleQtyStr = salesDisplayModel.getValueAt(i, 5).toString();

                if (saleQtyStr.isEmpty()) {
                    JOptionPane.showMessageDialog(this, "Please enter a sale quantity for selected item: " + name, "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }

                try {
                    int saleQty = Integer.parseInt(saleQtyStr);

                    if (saleQty <= 0) {
                        JOptionPane.showMessageDialog(this, "Sale quantity for " + name + " must be greater than zero.", "Input Error", JOptionPane.WARNING_MESSAGE);
                        return;
                    }
                    if (saleQty > availableQty) {
                        JOptionPane.showMessageDialog(this, "Cannot sell " + saleQty + " units of " + name + ". Only " + availableQty + " available.", "Insufficient Stock", JOptionPane.WARNING_MESSAGE);
                        return;
                    }

                    double itemTotal = price * saleQty;
                    totalSaleAmount += itemTotal;
                    currentSaleItems.add(new SaleRecord(type, name, saleQty, price, itemTotal, currentDateTime));

                    for (int j = 0; j < inventoryModel.getRowCount(); j++) {
                        if (inventoryModel.getValueAt(j, 2).toString().equals(name)) {
                            int newQuantity = availableQty - saleQty;
                            inventoryModel.setValueAt(String.valueOf(newQuantity), j, 4);
                            break;
                        }
                    }
                } catch (NumberFormatException e) {
                    JOptionPane.showMessageDialog(this, "Invalid quantity for " + name + ". Please enter a valid number.", "Input Error", JOptionPane.WARNING_MESSAGE);
                    return;
                }
            }
        }

        if (currentSaleItems.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No items selected for sale.", "No Selection", JOptionPane.WARNING_MESSAGE);
            return;
        }

        salesList.addAll(currentSaleItems);
        loadSalesDisplayTable();
        JOptionPane.showMessageDialog(this, String.format("Sale processed successfully!\nTotal Amount: $%.2f", totalSaleAmount), "Sale Complete", JOptionPane.INFORMATION_MESSAGE);
        saveSalesRecords();
    }

    private void showSalesReport() {
        if (salesList.isEmpty()) {
            JOptionPane.showMessageDialog(this, "No sales records available yet.", "Sales Report", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        StringBuilder report = new StringBuilder("Sales Report:\n\n");
        double grandTotal = 0.0;

        for (SaleRecord record : salesList) {
            report.append("Sales ID: ").append(record.getSalesId()).append("\n");
            report.append("Date/Time: ").append(record.getSaleDateTime()).append("\n");
            report.append("   Type: ").append(record.getType()).append("\n");
            report.append("   Name: ").append(record.getName()).append("\n");
            report.append("   Quantity: ").append(record.getQuantity()).append("\n");
            report.append(String.format("   Unit Price: $%.2f\n", record.getUnitPrice()));
            report.append(String.format("   Total: $%.2f\n", record.getTotal()));
            report.append("--------------------\n");
            grandTotal += record.getTotal();
        }

        report.append(String.format("\nGrand Total Sales: $%.2f", grandTotal));

        JTextArea textArea = new JTextArea(report.toString());
        textArea.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(textArea);
        scrollPane.setPreferredSize(new Dimension(500, 400));
        JOptionPane.showMessageDialog(this, scrollPane, "Sales Report", JOptionPane.PLAIN_MESSAGE);
    }

    private void saveSalesRecords() {
        try (BufferedWriter writer = new BufferedWriter(new FileWriter(SALES_FILE_NAME))) {
            for (SaleRecord record : salesList) {
                writer.write(String.join(",",
                        record.getSalesId(),
                        record.getType(),
                        record.getName(),
                        String.valueOf(record.getQuantity()),
                        String.valueOf(record.getUnitPrice()),
                        String.valueOf(record.getTotal()),
                        record.getSaleDateTime()
                ));
                writer.newLine();
            }
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error saving sales records: " + e.getMessage(), "Save Error", JOptionPane.ERROR_MESSAGE);
        }
    }

    private void loadSalesRecords() {
        File file = new File(SALES_FILE_NAME);
        if (!file.exists()) return;

        try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
            String line;
            int maxId = 0;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",");
                if (parts.length == 7) {
                    try {
                        String salesId = parts[0];
                        String type = parts[1];
                        String name = parts[2];
                        int quantity = Integer.parseInt(parts[3]);
                        double unitPrice = Double.parseDouble(parts[4]);
                        double total = Double.parseDouble(parts[5]);
                        String saleDateTime = parts[6];

                        SaleRecord record = new SaleRecord(type, name, quantity, unitPrice, total, saleDateTime);
                        salesList.add(record);

                        int parsedId = Integer.parseInt(salesId);
                        if (parsedId > maxId) maxId = parsedId;

                    } catch (NumberFormatException e) {
                        System.err.println("Skipping malformed line in sales records file: " + line);
                    }
                }
            }
            SaleRecord.setNextId(maxId + 1);
        } catch (IOException e) {
            JOptionPane.showMessageDialog(this, "Error loading sales records: " + e.getMessage(), "Load Error", JOptionPane.ERROR_MESSAGE);
        }
    }
}
