import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class DataRetriever {
    public Dish findDishById(Integer id) {
        DBConnection dbConnection = new DBConnection();
        Connection connection = dbConnection.getConnection();
        try {
            PreparedStatement preparedStatement = connection.prepareStatement(
                    """
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
                dish.setSellingPrice(resultSet.getObject("selling_price") == null
                        ? null : resultSet.getDouble("selling_price"));
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
            PreparedStatement ps = connection.prepareStatement(
                    """
                    
                            SELECT di.id AS di_id, di.required_quantity, di.unit,
                           i.id AS i_id, i.name, i.category, i.selling_price
                                                FROM dish_ingredient di
                                                JOIN ingredient i ON di.
                            ingredient_id = i.id
                                                WHERE di.dish_id = ?
                    """
            );
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Ingredient ingredient = new Ingredient(
                        rs.getInt("i_id"),
                        rs.getString("name"),
                        CategoryEnum.valueOf(rs.getString("category")),
                        rs.getDouble("selling_price")
                );
                DishIngredient di = new DishIngredient(
                        rs.getInt("di_id"),
                        ingredient,
                        rs.getDouble("required_quantity"),
                        rs.getString("unit")
                );
                list.add(di);
            }
            dbConnection.closeConnection(connection);
            return
            list;

    } catch
    (


    SQLException e) {
        throw new RuntimeException(e
    );
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
                try (PreparedStatement ps = conn.prepareStatement(
                    upsertDishSql)) {
                    if (toSave.getId()
                !=

                null) {
                        ps.setInt(1
                    , toSave.getId());
                    } else {
                        ps.setInt(1, getNextSerialValue(conn, "dish",
                "id")

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

                    try (ResultSet rs = ps.
            executeQuery()) {
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
        if (newIngredients == null ||
                    newIngredients.isEmpty()) {
            return List.of();
        }
        List<Ingredient> savedIngredients = new ArrayList<>(
                    );
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


    private String getSerialSequenceName(Connection conn, String tableName, String columnName)
            throws SQLException {

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

    private int getNextSerialValue(Connection conn, String tableName, String columnName)
            throws SQLException {

        String sequenceName = getSerialSequenceName(conn, tableName, columnName);
        if (sequenceName == null) {
            throw new IllegalArgumentException(
                    "Any sequence found for " + tableName + "." + columnName
            );
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
        String setValSql = String.format(
                "SELECT setval('%s', (SELECT COALESCE(MAX(%s), 0) FROM %s) + 1)",
                sequenceName, columnName, tableName
        );

        try (PreparedStatement ps = conn.prepareStatement(setValSql)) {
            ps.execute();
        }
    }



    private void deleteDishIngredients(Connection conn, Integer dishId) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(
                "DELETE FROM dish_ingredient WHERE dish_id = ?"
        )) {
            ps.
                setInt(1, dishId);
            ps.executeUpdate();
        }
    }


    private void
                insertDishIngredients(
            Connection conn,
            Integer dishId,
            List<DishIngredient> dishIngredients
    ) throws SQLException {

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

}