import java.time.Instant;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        DataRetriever dataRetriever = new DataRetriever();

        try {
            // 🔎 Vérification : afficher un plat avec ses ingrédients
            Dish dishTest = dataRetriever.findDishById(4);
            System.out.println("Plat récupéré : " + dishTest);

            // 1️⃣ Récupérer des plats existants
            Dish dish1 = dataRetriever.findDishById(1);
            Dish dish2 = dataRetriever.findDishById(2);

            System.out.println("DEBUG dish1 : " + dish1);
            dish1.getDishIngredients().forEach(di ->
                    System.out.println("  DI ingredient = " + di.getIngredient())
            );
            System.out.println("DEBUG dish2 : " + dish2);
            dish2.getDishIngredients().forEach(di ->
                    System.out.println("  DI ingredient = " + di.getIngredient())
            );

            // 2️⃣ Créer les DishOrder
            DishOrder dishOrder1 = new DishOrder();
            dishOrder1.setDish(dish1);
            dishOrder1.setQuantity(2);

            DishOrder dishOrder2 = new DishOrder();
            dishOrder2.setDish(dish2);
            dishOrder2.setQuantity(1);

            // 3️⃣ Créer la commande
            Order order = new Order();
            order.setCreationDatetime(Instant.now());
            order.setDishOrders(List.of(dishOrder1, dishOrder2));

            // 4️⃣ Sauvegarder la commande
            Order savedOrder = dataRetriever.saveOrder(order, order);

            // 5️⃣ Affichage du résultat (preuve que ça marche)
            System.out.println("\n✅ Commande enregistrée avec succès !");
            System.out.println("Référence       : " + savedOrder.getReference());
            System.out.println("Date création   : " + savedOrder.getCreationDatetime());
            System.out.println("Total HT        : " + savedOrder.getTotalHT());
            System.out.println("Total TTC       : " + savedOrder.getTotalTTC());

            System.out.println("\nPlats commandés :");
            for (DishOrder doItem : savedOrder.getDishOrders()) {
                System.out.println(
                        "- " + doItem.getDish().getName()
                                + " | Quantité : " + doItem.getQuantity()
                                + " | Prix unitaire : " + doItem.getDish().getSellingPrice()
                );
            }

        } catch (RuntimeException e) {
            System.err.println("❌ Erreur lors de la création de la commande : " + e.getMessage());
        }
    }
}
