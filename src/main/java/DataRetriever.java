import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class DataRetriever {
    public Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();

        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    SELECT id AS dish_id, name AS dish_name, dish_type, selling_price
                    FROM dish
                    WHERE id = ?;
                    """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setSellingPrice(resultSet.getObject("selling_price") == null ? null : resultSet.getDouble("selling_price"));

                List<DishIngredient> dishIngredients = findDishIngredientByDishId(id, dish);
                dish.setDishIngredients(dishIngredients);

                dbConnection.closeConnection(connection);
                return dish;
            }

            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private List<DishIngredient> findDishIngredientByDishId(Integer dishId, Dish dish) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishIngredient> dishIngredients = new ArrayList<>();
        String sql = """
                SELECT di.ingredient_id, di.required_quantity, di.unit,
                       i.id AS ing_id, i.name, i.category, i.selling_price
                FROM dish_ingredient di
                JOIN ingredient i ON di.ingredient_id = i.id
                WHERE di.dish_id = ?
                """;

        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, dishId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ingredient = new Ingredient(
                        rs.getInt("ing_id"),
                        rs.getString("name"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        rs.getDouble("selling_price")
                );

                DishIngredient dishIngredient = new DishIngredient();
                dishIngredient.setIngredient(ingredient);
                dishIngredient.setDish(dish);
                dishIngredient.setRequiredQuantity(rs.getDouble("required_quantity"));
                dishIngredient.setUnit(rs.getString("unit"));

                dishIngredients.add(dishIngredient);
            }
            return dishIngredients;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                INSERT INTO dish (id, selling_price, name, dish_type)
                VALUES (?, ?, ?, ?::dish_type_enum)
                ON CONFLICT (id) DO UPDATE
                SET name = EXCLUDED.name,
                    dish_type = EXCLUDED.dish_type,
                    selling_price = EXCLUDED.selling_price
                RETURNING id;
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);
            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id"));
                }
                if (toSave.getSellingPrice() != null) {
                    ps.setDouble(2, toSave.getSellingPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }
                ps.setString(3, toSave.getName());
                ps.setString(4, toSave.getDishType().name());

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    dishId = rs.getInt(1);
                }
            }

            deleteDishIngredients(conn, dishId);
            insertDishIngredients(conn, dishId, toSave.getDishIngredients());

            conn.commit();
            return findDishById(dishId);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void deleteDishIngredients(Connection conn, Integer dishId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_ingredient WHERE dish_id = ?")) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }
    }

    private void insertDishIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients) throws SQLException {
        if (dishIngredients == null || dishIngredients.isEmpty()) return;

        String sql = """
                INSERT INTO dish_ingredient (dish_id, ingredient_id, required_quantity, unit)
                VALUES (?, ?, ?, ?)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            for (DishIngredient di : dishIngredients) {
                ps.setInt(1, dishId);
                ps.setInt(2, di.getIngredient().getId());
                if (di.getRequiredQuantity() != null) {
                    ps.setDouble(3, di.getRequiredQuantity());
                } else {
                    ps.setNull(3, Types.DOUBLE);
                }
                ps.setString(4, di.getUnit());
                ps.addBatch();
            }
            ps.executeBatch();
        }
    }

    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) return List.of();

        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getConnection();

        try {
            conn.setAutoCommit(false);
            String insertSql = """
                    INSERT INTO ingredient (id, name, category, selling_price)
                    VALUES (?, ?, ?::ingredient_category_enum, ?)
                    RETURNING id
                    """;

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    ps.setInt(1, ingredient.getId());
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getSellingPrice());

                    try (ResultSet rs = ps.executeQuery()) {
                        rs.next();
                        int generatedId = rs.getInt(1);
                        ingredient.setId(generatedId);
                        savedIngredients.add(ingredient);
                    }
                }
                conn.commit();
                return savedIngredients;
            } catch (SQLException e) {
                conn.rollback();
                throw new RuntimeException(e);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        } finally {
            dbConnection.closeConnection(conn);
        }
    }

    public Double getIngredientStock(Integer ingredientId) {
        String sql = "SELECT COALESCE(SUM(quantity),0) AS total FROM stock_movement WHERE ingredient_id = ?";
        try (Connection conn = new DBConnection().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getDouble("total");
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canPrepareDish(Dish dish, Integer quantity) {
        for (DishIngredient di : dish.getDishIngredients()) {
            double required = di.getRequiredQuantity() * quantity;
            double available = getIngredientStock(di.getIngredient().getId());
            if (available < required) return false;
        }
        return true;
    }

    public void prepareDish(Dish dish, Integer quantity, Connection conn) {
        if (!canPrepareDish(dish, quantity)) {
            throw new RuntimeException("Not enough stock to prepare this dish");
        }
        try {

            for (DishIngredient di : dish.getDishIngredients()) {
                StockMovement movement = new StockMovement();
                movement.setIngredientId(di.getIngredient().getId());
                movement.setQuantity(-di.getRequiredQuantity() * quantity);
                movement.setMovementType(StockMovementTypeEnum.OUT);
                saveStockMovement(conn, movement);
            }
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private void saveStockMovement(Connection conn, StockMovement movement) throws SQLException {
        String sql = """
                INSERT INTO stock_movement (ingredient_id, quantity, movement_type)
                VALUES (?, ?, ?::stock_movement_type_enum)
                """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movement.getIngredientId());
            ps.setDouble(2, movement.getQuantity());
            ps.setString(3, movement.getMovementType().name());
            ps.executeUpdate();
        }
    }

    public StockMovement findStockMovementById(Integer id) {
        String sql = """
                SELECT sm.id, sm.quantity, sm.unit, sm.movement_date,
                       i.id AS ingredient_id, i.name, i.category, i.selling_price
                FROM stock_movement sm
                JOIN ingredient i ON sm.ingredient_id = i.id
                WHERE sm.id = ?
                """;
        try (Connection conn = new DBConnection().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Ingredient ingredient = new Ingredient(
                        rs.getInt("ingredient_id"),
                        rs.getString("name"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        rs.getDouble("selling_price")
                );
                return new StockMovement(rs.getInt("id"), ingredient, rs.getDouble("quantity"),
                        rs.getString("unit"), rs.getTimestamp("movement_date"));
            }
            throw new RuntimeException("Stock movement not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public Order saveOrder(Order orderToSave, Order order) {
        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);


            if (orderToSave.getId() != null) {
                Order existingOrder = findOrderById(conn, orderToSave.getId());
                if (existingOrder.getStatus() == OrderStatusEnum.DELIVERED) {
                    throw new RuntimeException("Une commande livrée ne peut plus être modifiée !");
                }
            }


            for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                Dish dish = dishOrder.getDish();
                int quantityDish = dishOrder.getQuantity();
                for (DishIngredient di : dish.getDishIngredients()) {
                    if (di.getIngredient() == null) {
                        throw new RuntimeException("Ingredient null dans DishIngredient du plat " + dish.getName());
                    }
                    Double availableStock = getIngredientStock(di.getIngredient().getId());
                    Double requiredStock = di.getRequiredQuantity() * quantityDish;
                    if (availableStock < requiredStock) {
                        throw new RuntimeException("Stock insuffisant pour l'ingrédient : " + di.getIngredient().getName());
                    }
                }
            }

            int orderId;
            String reference;
            if (orderToSave.getId() != null) {
                orderId = orderToSave.getId();
                reference = orderToSave.getReference();
            } else {
                orderId = getNextSerialValue(conn, "orders", "id");
                reference = String.format("ORD%05d", orderId);
                orderToSave.setStatus(OrderStatusEnum.CREATED);
            }

            double totalHT = 0;
            for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                totalHT += dishOrder.getDish().getSellingPrice() * dishOrder.getQuantity();
            }
            double totalTTC = totalHT * 1.20;

            String upsertOrderSql = """
                    INSERT INTO orders (id, reference, creation_datetime, total_ht, total_ttc, status, type)
                    VALUES (?, ?, ?, ?, ?, ?::order_status_enum, ?::order_type_enum)
                    ON CONFLICT (id) DO UPDATE
                    SET total_ht = EXCLUDED.total_ht,
                        total_ttc = EXCLUDED.total_ttc,
                        status = EXCLUDED.status,
                        type = EXCLUDED.type
                    """;

            try (PreparedStatement ps = conn.prepareStatement(upsertOrderSql)) {
                ps.setInt(1, orderId);
                ps.setString(2, reference);
                ps.setTimestamp(3, Timestamp.from(orderToSave.getCreationDatetime()));
                ps.setDouble(4, totalHT);
                ps.setDouble(5, totalTTC);
                ps.setString(6, orderToSave.getStatus().name());
                ps.setString(7, orderToSave.getType().name());
                ps.executeUpdate();
            }

            try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_order WHERE order_id = ?")) {
                ps.setInt(1, orderId);
                ps.executeUpdate();
            }

            String insertDishOrderSql = "INSERT INTO dish_order (order_id, dish_id, quantity) VALUES (?, ?, ?)";
            try (PreparedStatement ps = conn.prepareStatement(insertDishOrderSql)) {
                for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                    ps.setInt(1, orderId);
                    ps.setInt(2, dishOrder.getDish().getId());
                    ps.setInt(3, dishOrder.getQuantity());
                    ps.addBatch();
                }
                ps.executeBatch();
            }


            for (DishOrder dishOrder : orderToSave.getDishOrders()) {
                prepareDish(dishOrder.getDish(), dishOrder.getQuantity(), conn);
            }

            conn.commit();
            return findOrderByReference(reference);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    private Order findOrderById(Connection conn, Integer orderId) throws SQLException {
        String sql = "SELECT id, reference, creation_datetime, total_ht, total_ttc, status, type FROM orders WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, orderId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                Order order = new Order();
                order.setId(rs.getInt("id"));
                order.setReference(rs.getString("reference"));
                order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
                order.setTotalHT(rs.getDouble("total_ht"));
                order.setTotalTTC(rs.getDouble("total_ttc"));
                order.setStatus(OrderStatusEnum.valueOf(rs.getString("status")));
                order.setType(OrderTypeEnum.valueOf(rs.getString("type")));
                return order;
            }
            throw new RuntimeException("Commande introuvable ID=" + orderId);
        }
    }

    public Order findOrderByReference(String reference) {
        String sql = "SELECT id, reference, creation_datetime, total_ht, total_ttc, status, type FROM orders WHERE reference = ?";
        try (Connection conn = new DBConnection().getConnection(); PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reference);
            ResultSet rs = ps.executeQuery();
            if (!rs.next()) throw new RuntimeException("Commande introuvable pour la référence : " + reference);

            Order order = new Order();
            order.setId(rs.getInt("id"));
            order.setReference(rs.getString("reference"));
            order.setCreationDatetime(rs.getTimestamp("creation_datetime").toInstant());
            order.setTotalHT(rs.getDouble("total_ht"));
            order.setTotalTTC(rs.getDouble("total_ttc"));
            order.setStatus(OrderStatusEnum.valueOf(rs.getString("status")));
            order.setType(OrderTypeEnum.valueOf(rs.getString("type")));


            String dishOrderSql = """
                    SELECT do.id, do.dish_id, do.quantity, d.name, d.selling_price, d.dish_type
                    FROM dish_order do
                    JOIN dish d ON do.dish_id = d.id
                    WHERE do.order_id = ?
                    """;
            try (PreparedStatement ps2 = conn.prepareStatement(dishOrderSql)) {
                ps2.setInt(1, order.getId());
                ResultSet rs2 = ps2.executeQuery();
                List<DishOrder> dishOrders = new ArrayList<>();
                while (rs2.next()) {
                    Dish dish = new Dish();
                    dish.setId(rs2.getInt("dish_id"));
                    dish.setName(rs2.getString("name"));
                    dish.setSellingPrice(rs2.getDouble("selling_price"));
                    dish.setDishType(DishTypeEnum.valueOf(rs2.getString("dish_type")));

                    DishOrder dishOrder = new DishOrder();
                    dishOrder.setId(rs2.getInt("id"));
                    dishOrder.setDish(dish);
                    dishOrder.setQuantity(rs2.getInt("quantity"));

                    dishOrders.add(dishOrder);
                }
                order.setDishOrders(dishOrders);
            }
            return order;
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {
        String sql = "SELECT pg_get_serial_sequence(?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) return rs.getString(1);
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {
        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null)
            throw new IllegalArgumentException("Any sequence found for " + tableName + "." + columnName);

        String setValSql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s),0)+1 FROM %s))", sequenceName, columnName, tableName);
        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.execute();
        }

        String nextValSql = "SELECT nextval(?)";
        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getInt(1);
        }
    }
}
