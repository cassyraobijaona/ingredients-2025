import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private Double totalHT;
    private Double totalTTC;
    private OrderStatusEnum status;
    private OrderTypeEnum type;
    private List<DishOrder> dishOrders;

    public Order() {
    }

    public Order(Integer id, String reference, Instant creationDatetime, Double totalHT, Double totalTTC,
                 OrderStatusEnum status, OrderTypeEnum type, List<DishOrder> dishOrders) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
        this.totalHT = totalHT;
        this.totalTTC = totalTTC;
        this.status = status;
        this.type = type;
        this.dishOrders = dishOrders;
    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getReference() {
        return reference;
    }

    public void setReference(String reference) {
        this.reference = reference;
    }

    public Instant getCreationDatetime() {
        return creationDatetime;
    }

    public void setCreationDatetime(Instant creationDatetime) {
        this.creationDatetime = creationDatetime;
    }

    public Double getTotalHT() {
        return totalHT;
    }

    public void setTotalHT(Double totalHT) {
        this.totalHT = totalHT;
    }

    public Double getTotalTTC() {
        return totalTTC;
    }

    public void setTotalTTC(Double totalTTC) {
        this.totalTTC = totalTTC;
    }

    public OrderStatusEnum getStatus() {
        return status;
    }

    public void setStatus(OrderStatusEnum status) {
        this.status = status;
    }

    public OrderTypeEnum getType() {
        return type;
    }

    public void setType(OrderTypeEnum type) {
        this.type = type;
    }

    public List<DishOrder> getDishOrders() {
        return dishOrders;
    }

    public void setDishOrders(List<DishOrder> dishOrders) {
        this.dishOrders = dishOrders;
    }


    @Override
    public String toString() {
        return "Order{" +
                "id=" + id +
                ", reference='" + reference + '\'' +
                ", creationDatetime=" + creationDatetime +
                ", status=" + status +
                ", type=" + type +
                ", dishOrders=" + dishOrders +
                '}';
    }


    public Double getTotalAmountWithoutVat() {
        throw new RuntimeException("Not implemented");
    }

    public Double getTotalAmountWithVat() {
        throw new RuntimeException("Not implemented");
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id) &&
                Objects.equals(reference, order.reference) &&
                Objects.equals(creationDatetime, order.creationDatetime) &&
                Objects.equals(dishOrders, order.dishOrders) &&
                status == order.status &&
                type == order.type;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime, status, type, dishOrders);
    }
}
