import javax.swing.SwingUtilities;

public class PCInventorySalesSystem {
    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new InventoryWindow().setVisible(true));
    }
}