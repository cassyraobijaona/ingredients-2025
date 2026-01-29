import java.time.Instant;
import java.util.List;

public class Main {

    public static void main(String[] args) {

        DataRetriever dataRetriever = new DataRetriever();

        try {
            Dish dishTest = dataRetriever.findDishById(4);
            System.out.println("Plat récupéré : " + dishTest);

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

            DishOrder dishOrder1 = new DishOrder();
            dishOrder1.setDish(dish1);
            dishOrder1.setQuantity(2);

            DishOrder dishOrder2 = new DishOrder();
            dishOrder2.setDish(dish2);
            dishOrder2.setQuantity(1);

           Order order = new Order();
            order.setCreationDatetime(Instant.now());
            order.setType(OrderTypeEnum.EAT_IN);
            order.setStatus(OrderStatusEnum.CREATED);
            order.setDishOrders(List.of(dishOrder1, dishOrder2));

            Order savedOrder = dataRetriever.saveOrder(order, order);

            System.out.println("\n Commande enregistrée avec succès !");
            System.out.println("Référence       : " + savedOrder.getReference());
            System.out.println("Type            : " + savedOrder.getType());
            System.out.println("Statut          : " + savedOrder.getStatus());
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

             Order retrievedOrder = dataRetriever.findOrderByReference(savedOrder.getReference());
            System.out.println("\n✅ Commande récupérée : statut=" + retrievedOrder.getStatus() + ", type=" + retrievedOrder.getType());

             retrievedOrder.setStatus(OrderStatusEnum.READY);
            retrievedOrder.setType(OrderTypeEnum.TAKE_AWAY);
            Order updatedOrder = dataRetriever.saveOrder(retrievedOrder, retrievedOrder);
            System.out.println("\n✅ Commande mise à jour : statut=" + updatedOrder.getStatus() + ", type=" + updatedOrder.getType());

           updatedOrder.setStatus(OrderStatusEnum.DELIVERED);
            updatedOrder = dataRetriever.saveOrder(updatedOrder, updatedOrder);
            System.out.println("\n✅ Commande livrée : statut=" + updatedOrder.getStatus());

            try {
                updatedOrder.setType(OrderTypeEnum.EAT_IN);
                dataRetriever.saveOrder(updatedOrder, updatedOrder);
            } catch (RuntimeException e) {
                System.out.println("\n Tentative de modification après livraison : " + e.getMessage());
            }

        } catch (RuntimeException e) {
            System.err.println(" Erreur : " + e.getMessage());
        }
    }
}
