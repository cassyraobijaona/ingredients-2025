import java.sql.Timestamp;
import java.time.Instant;
import java.util.Objects;

public class StockMovement {

    private Integer id;
    private Ingredient ingredient;
    private Double quantity;
    private String unit;
    private Timestamp movementDate;
    private Instant movementTime;
    private StockMovementTypeEnum movementType;


    public StockMovement(Integer id, Ingredient ingredient, Double quantity, String unit, Timestamp movementDate, Instant movementTime, StockMovementTypeEnum movementType) {
        this.id = id;
        this.ingredient = ingredient;
        this.quantity = quantity;
        this.unit = unit;
        this.movementDate = movementDate;
        this.movementTime = movementTime;
        this.movementType = movementType;
    }

    public StockMovement(int id, Ingredient ingredient, double quantity, String unit, Timestamp movementDate) {
    }

    public StockMovement() {

    }

    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public Ingredient getIngredient() {
        return ingredient;
    }

    public void setIngredient(Ingredient ingredient) {
        this.ingredient = ingredient;
    }

    public Double getQuantity() {
        return quantity;
    }

    public void setQuantity(Double quantity) {
        this.quantity = quantity;
    }

    public String getUnit() {
        return unit;
    }

    public void setUnit(String unit) {
        this.unit = unit;
    }

    public Timestamp getMovementDate() {
        return movementDate;
    }

    public void setMovementDate(Timestamp movementDate) {
        this.movementDate = movementDate;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        StockMovement that = (StockMovement) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "StockMovement{" +
                "id=" + id +
                ", ingredient=" + ingredient +
                ", quantity=" + quantity +
                ", unit='" + unit + '\'' +
                ", movementDate=" + movementDate +
                '}';
    }

    public int getIngredientId() {
        return ingredient.getId();
    }

    public StockMovementTypeEnum getMovementType() {
        return movementType;
    }

    public void setIngredientId(Integer id) {
        if (this.ingredient != null) {
            this.ingredient.setId(id);
        }
    }
        public void setMovementType (StockMovementTypeEnum movementType){
            this.movementType = movementType;
        }
}

