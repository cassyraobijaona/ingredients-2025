public class DishIngredient {
    private Integer id;
    private Ingredient ingredient;
    private Double requiredQuantity;
    private String unit;
    private Dish dish;


    public DishIngredient() {

        }

    public DishIngredient(Integer id,
                          Ingredient ingredient,
                          Double requiredQuantity,
                          String unit, Dish dish) {
        this.id = id;
        this.ingredient = ingredient;
        this.requiredQuantity = requiredQuantity;
        this.unit = unit;
        this.dish = dish;
    }
    public Integer getId() { return id; }
    public Ingredient getIngredient() { return ingredient; }
    public Double getRequiredQuantity() { return requiredQuantity; }
    public String getUnit() { return unit; }
    public Dish getDish() { return dish; }
    public void setId(Integer id) { this.id = id; }
    public void setIngredient(Ingredient ingredient) { this.ingredient = ingredient; }
    public void setRequiredQuantity(Double requiredQuantity) { this.requiredQuantity = requiredQuantity; }
    public void setUnit(String unit) { this.unit = unit; }
    public void setDish(Dish dish) { this.dish = dish; }
}
