import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    public Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement("""
                    select dish.id as dish_id, dish.name as dish_name, dish_type, dish.selling_price 
                    FROM dish
                    where dish.id = ?;
                    """);
            preparedStatement.setInt(1, id);
            ResultSet resultSet = preparedStatement.executeQuery();
            if (resultSet.next()) {
                Dish dish = new Dish();
                dish.setId(resultSet.getInt("dish_id"));
                dish.setName(resultSet.getString("dish_name"));
                dish.setDishType(DishTypeEnum.valueOf(resultSet.getString("dish_type")));
                dish.setSellingPrice(resultSet.getObject("selling_price") == null ? null : resultSet.getDouble("selling_price"));
                dish.setDishIngredients(findDishIngredientByDishId(id));
                return dish;
            }
            dbConnection.closeConnection(connection);
            throw new RuntimeException("Dish not found " + id);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private List<DishIngredient> findDishIngredientByDishId(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        List<DishIngredient> list = new ArrayList<>();
        try {
            PreparedStatement ps = connection.prepareStatement("""
                    
                            SELECT di.id AS di_id, di.required_quantity, di.unit,
                           i.id AS i_id, i.name, i.category, i.selling_price
                                                FROM dish_ingredient di
                                                JOIN ingredient i ON di.
                            ingredient_id = i.id
                                                WHERE di.dish_id = ?
                    """);
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ingredient = new Ingredient(rs.getInt("i_id"), rs.getString("name"), CategoryEnum.valueOf(rs.getString("category")), rs.getDouble("selling_price"));
                DishIngredient di = new DishIngredient(rs.getInt("di_id"), ingredient, rs.getDouble("required_quantity"), rs.getString("unit"));
                list.add(di);
            }
            dbConnection.closeConnection(connection);
            return list;

        } catch (


                SQLException e) {
            throw new RuntimeException(e);
        }
    }


    Dish saveDish(Dish toSave) {
        String upsertDishSql = """
                        INSERT
                        INTO dish (id, selling_price,
                        name, dish_type)
                                    VALUES (?, ?, ?, ?::dish_type)
                        ON CONFLICT (id) DO UPDATE
                        SET name =
                        EXCLUD
                
                dish_type = EXCLUDED.dish_type,
                            selling_price = EXCLUDED.
                    selling_price
                
                    RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            Integer dishId;
            try (PreparedStatement ps = conn.prepareStatement(upsertDishSql)) {
                if (toSave.getId() !=

                        null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "dish", "id")

                    );
                }

                if (toSave.getSellingPrice() != null) {
                    ps.setDouble(2, toSave.getSellingPrice());
                } else {
                    ps.setNull(2, Types.DOUBLE);
                }

                ps

                        .setString(3, toSave.getName());
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


    public List<Ingredient> createIngredients(List<Ingredient> newIngredients) {
        if (newIngredients == null || newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>();
        DBConnection dbConnection = new DBConnection();
        Connection conn = dbConnection.getConnection();
        try {
            conn.setAutoCommit(false);
            String insertSql = """
                        INSERT INTO ingredient (id, name, category, selling_price)
                        VALUES (?, ?, ?::ingredient_category, ?)
                        RETURNING id
                    """;

            try (PreparedStatement ps = conn.prepareStatement(insertSql)) {
                for (Ingredient ingredient : newIngredients) {
                    ps.setInt(1, ingredient.getId());
                    ps.setString(2, ingredient.getName());
                    ps.setString(3, ingredient.getCategory().name());
                    ps.setDouble(4, ingredient.getPrice());


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


    private String getSerialSequenceName(Connection conn, String tableName, String columnName) throws SQLException {

        String sql = "SELECT pg_get_serial_sequence(?, ?)";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tableName);
            ps.setString(2, columnName);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return rs.getString(1);
                }
            }
        }
        return null;
    }

    private int getNextSerialValue(Connection conn, String tableName, String columnName) throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException("Any sequence found for " + tableName + "." + columnName);
        }
        updateSequenceNextValue(conn, tableName, columnName, sequenceName);

        String nextValSql = "SELECT nextval(?)";

        try (PreparedStatement ps = conn.prepareStatement(nextValSql)) {
            ps.setString(1, sequenceName);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1);
            }
        }
    }

    private void updateSequenceNextValue(Connection conn, String tableName, String


            columnName, String sequenceName) throws SQLException {
        String setValSql = String.format("SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s) + 1)", sequenceName, columnName, tableName);

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.execute();
        }
    }


    private void deleteDishIngredients(Connection conn, Integer dishId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("DELETE FROM dish_ingredient WHERE dish_id = ?")) {
            ps.setInt(1, dishId);
            ps.executeUpdate();
        }
    }


    private void insertDishIngredients(Connection conn, Integer dishId, List<DishIngredient> dishIngredients) throws SQLException {

        if (dishIngredients == null || dishIngredients.isEmpty()) {
            return;
        }

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


    public StockMovement saveStockMovement(StockMovement toSave) {
        String sql = """
                    INSERT INTO stock_movement (id, ingredient_id, quantity, unit, movement_date)
                    VALUES (?, ?, ?, ?, ?)
                    RETURNING id
                """;

        try (Connection conn = new DBConnection().getConnection()) {

            Integer movementId;

            try (PreparedStatement ps = conn.prepareStatement(sql)) {

                if (toSave.getId() != null) {
                    ps.setInt(1, toSave.getId());
                } else {
                    ps.setInt(1, getNextSerialValue(conn, "stock_movement", "id"));
                }

                ps.setInt(2, toSave.getIngredient().getId());
                ps.setDouble(3, toSave.getQuantity());
                ps.setString(4, toSave.getUnit());
                ps.setTimestamp(5, toSave.getMovementDate());

                try (ResultSet rs = ps.executeQuery()) {
                    rs.next();
                    movementId = rs.getInt(1);
                }
            }

            return findStockMovementById(movementId);

        } catch (SQLException e) {
            throw new RuntimeException(e);
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

        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                Ingredient ingredient = new Ingredient(
                        rs.getInt("ingredient_id"),
                        rs.getString("name"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        rs.getDouble("selling_price")
                );

                return new StockMovement(
                        rs.getInt("id"),
                        ingredient,
                        rs.getDouble("quantity"),
                        rs.getString("unit"),
                        rs.getTimestamp("movement_date")
                );
            }

            throw new RuntimeException("Stock movement not found " + id);

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    public Double getIngredientStock(Integer ingredientId) {
        String sql = """
                    SELECT COALESCE(SUM(quantity), 0) AS total
                    FROM stock_movement
                    WHERE ingredient_id = ?
                """;

        try (Connection conn = new DBConnection().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setInt(1, ingredientId);
            ResultSet rs = ps.executeQuery();
            rs.next();
            return rs.getDouble("total");

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }

    public boolean canPrepareDish(Integer dishId) {
        Dish dish = findDishById(dishId);

        if (dish.getDishIngredients() == null || dish.getDishIngredients().isEmpty()) {
            throw new RuntimeException("Dish has no ingredients");
        }

        for (DishIngredient di : dish.getDishIngredients()) {

            Integer ingredientId = di.getIngredient().getId();
            Double requiredQuantity = di.getRequiredQuantity();

            if (requiredQuantity == null) {
                throw new RuntimeException(
                        "Required quantity is missing for ingredient "
                                + di.getIngredient().getName()
                );
            }

            Double availableStock = getIngredientStock(ingredientId);

            if (availableStock < requiredQuantity) {
                return false;
            }
        }

        return true;
    }


    public void prepareDish(Integer dishId) {

        if (!canPrepareDish(dishId)) {
            throw new RuntimeException("Not enough stock to prepare this dish");
        }

        Dish dish = findDishById(dishId);

        try (Connection conn = new DBConnection().getConnection()) {
            conn.setAutoCommit(false);

            for (DishIngredient di : dish.getDishIngredients()) {

                StockMovement movement = new StockMovement();
                movement.setIngredientId(di.getIngredient().getId());
                movement.setQuantity(-di.getRequiredQuantity());
                movement.setMovementType(StockMovementTypeEnum.OUT);

                saveStockMovement(conn, movement);
            }

            conn.commit();

        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


    private void saveStockMovement(Connection conn, StockMovement movement)
            throws SQLException {

        String sql = """
                    INSERT INTO stock_movement (ingredient_id, quantity, movement_type)
                    VALUES (?, ?, ?::stock_movement_type)
                """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, movement.getIngredientId());
            ps.setDouble(2, movement.getQuantity());
            ps.setString(3, movement.getMovementType().name());
            ps.executeUpdate();
        }
    }

}