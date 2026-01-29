import java.util.Objects;

public class Ingredient {
    private Integer id;
    private String name;
    private CategoryEnum category;
    private Double selling_price;

    public Ingredient() {
    }

    public Ingredient(Integer id) {
        this.id = id;
    }

    public Ingredient(Integer id, String name, CategoryEnum category, Double selling_price) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.selling_price = selling_price;
    }


    public Integer getId() {
        return id;
    }

    public void setId(Integer id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public CategoryEnum getCategory() {
        return category;
    }

    public void setCategory(CategoryEnum category) {
        this.category = category;
    }

    public Double getSellingPrice() {
        return selling_price;
    }

    public void setSellingPrice(Double price) {
        this.selling_price = price;
    }


    @Override
    public boolean equals(Object o) {
        if (o == null || getClass() != o.getClass()) return false;
        Ingredient that = (Ingredient) o;
        return Objects.equals(id, that.id) && Objects.equals(name, that.name) && category == that.category && Objects.equals(selling_price, that.selling_price) ;
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, name, category, selling_price);
    }

    @Override
    public String toString() {
        return "Ingredient{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", category=" + category +
                ", selling_price=" + selling_price +
                '}';
    }
}
