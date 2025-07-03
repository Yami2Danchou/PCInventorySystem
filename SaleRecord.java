import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SaleRecord {
    private static int nextId = 1; // Static counter
    private final String salesId; // Unique sales ID (e.g., 001, 002, ...)
    private final String type;
    private final String name;
    private final int quantity;
    private final double unitPrice;
    private final double total;
    private final String saleDateTime;

    public SaleRecord(String type, String name, int quantity, double unitPrice, double total, String saleDateTime) {
        this.salesId = String.format("%03d", nextId++);
        this.type = type;
        this.name = name;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.total = total;
        this.saleDateTime = saleDateTime;
    }

    // Add this method to reset counter if needed (e.g., on program load)
    public static void setNextId(int next) {
        nextId = next;
    }

    // Getter methods
    public String getSalesId() { return salesId; }
    public String getType() { return type; }
    public String getName() { return name; }
    public int getQuantity() { return quantity; }
    public double getUnitPrice() { return unitPrice; }
    public double getTotal() { return total; }
    public String getSaleDateTime() { return saleDateTime; }
}
