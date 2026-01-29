import java.time.Instant;
import java.util.List;
import java.util.Objects;

public class Order {
    private Integer id;
    private String reference;
    private Instant creationDatetime;
    private Double totalHT;
    private Double totalTTC;
    private List<DishOrder> dishOrders;


    public Order() {
    }

    public Order(Integer id, String reference, Instant creationDatetime, Double totalHT, Double totalTTC, List<DishOrder> dishOrders) {
        this.id = id;
        this.reference = reference;
        this.creationDatetime = creationDatetime;
        this.totalHT = totalHT;
        this.totalTTC = totalTTC;
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

    public Double getTotalTTC() {
        return totalTTC;
    }

    public List<DishOrder> getDishOrders() {
        return dishOrders;
    }

    public void setTotalHT(Double totalHT) {
        this.totalHT = totalHT;
    }

    public void setTotalTTC(Double totalTTC) {
        this.totalTTC = totalTTC;
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
                ", dishOrder=" + dishOrders +
                '}';
    }

    Double getTotalAmountWithoutVat() {
        throw new RuntimeException("Not implemented");
    }

    Double getTotalAmountWithVat() {
        throw new RuntimeException("Not implemented");
    }


    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Order order)) return false;
        return Objects.equals(id, order.id) && Objects.equals(reference, order.reference) && Objects.equals(creationDatetime, order.creationDatetime) && Objects.equals(dishOrders, order.dishOrders);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, reference, creationDatetime, dishOrders);
    }

}