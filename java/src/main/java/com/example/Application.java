package com.example;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Properties;

public class Application {

	private static Connection connection;

	private static final String SQL_INSERT_ORDERS = """
			INSERT INTO ORDERS (customer_id, order_date, order_created, entered_by)
			VALUES (?, CURDATE(), CURTIME(), ?)
			""";
	private static final String SQL_INSERT_ITEMS = """
			INSERT INTO order_item (order_id, line_num, product_id, description)
			VALUES (?,?,?,?)
			""";
	private static final String SQL_QUERY_ORDERS_ITEMS = """
			SELECT
				o.order_id, o.customer_id, o.order_date, o.order_created, o.entered_by,
				i.item_id, i.line_num, i.product_id, i.description
			FROM orders o
			INNER JOIN order_item i ON o.order_id = i.order_id
			""";
	private static final String SQL_DELETE_ITEM = """
			DELETE FROM order_item
			WHERE order_id = ? AND line_num = ?
			""";

	/**
	 * Entry point of the application. Opens and closes the database connection.
	 *
	 * @param args (not used)
	 * @throws SQLException if an error occurs when interacting with the database
	 */
	public static void main(String[] args) throws SQLException {
		try {
			initDatabaseConnection();
			System.out.println("now about to insert rows");
			long orderId = insertRows();
			System.out.println("about to query rows");
			queryRows();
			System.out.println("about to delete a row");
			deleteRow(orderId, 2);
			System.out.println("about to query rows (line num 2 should be missing)");
			queryRows();

		} finally {
			if (connection != null) {
				closeDatabaseConnection();
			}
		}
	}

	private static long insertRows() throws SQLException {
		try (PreparedStatement stmtOrders = connection.prepareStatement(SQL_INSERT_ORDERS,
				Statement.RETURN_GENERATED_KEYS);
				PreparedStatement stmtItems = connection.prepareStatement(SQL_INSERT_ITEMS);) {

			stmtOrders.setInt(1, 1);
			stmtOrders.setString(2, "andy");
			stmtOrders.executeUpdate();
			try (ResultSet rs = stmtOrders.getGeneratedKeys()) {
				rs.next();
				long orderId = rs.getLong(1);
				System.out.println("Order ID was " + orderId);
				stmtItems.setLong(1, orderId);
				stmtItems.setInt(2, 1);
				stmtItems.setInt(3, 1);
				stmtItems.setString(4, "box of chocolates");
				stmtItems.executeUpdate();
				stmtItems.setLong(1, orderId);
				stmtItems.setInt(2, 2);
				stmtItems.setInt(3, 2);
				stmtItems.setString(4, "flowers");
				stmtItems.executeUpdate();
				return orderId;
			}
		}
	}

	private static void queryRows() throws SQLException {
		try (Statement stmtQuery = connection.createStatement()) {
			try (ResultSet rs = stmtQuery.executeQuery(SQL_QUERY_ORDERS_ITEMS)) {
				while (rs.next()) {
					var output = String.format("""
							~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
							order_id: %d
							customer_id: %d
							order_date: %tD
							order_created: %tD
							entered_by: %s
							item_id: %d
							line_num: %d 
							product_id: %d
							description: %s
							""",
							rs.getLong("ORDER_ID"),
							rs.getInt("CUSTOMER_ID"),
							rs.getDate("ORDER_DATE"),
							rs.getDate("ORDER_CREATED"),
							rs.getString("ENTERED_BY"),
							rs.getLong("ITEM_ID"),
							rs.getInt("LINE_NUM"),
							rs.getInt("PRODUCT_ID"),
							rs.getString("DESCRIPTION"));

					System.out.println(output);
				}
			}
		}
	}

	private static void deleteRow(long orderId, int lineNum) throws SQLException {
		System.out.println("deleting row");
		try (PreparedStatement stmt = connection.prepareStatement(SQL_DELETE_ITEM)) {
			stmt.setLong(1, orderId);
			stmt.setInt(2, lineNum);
			stmt.executeUpdate();
		}
	}

	private static void initDatabaseConnection() throws SQLException {
		System.out.println("Connecting to the database...");
		Properties connConfig = new Properties();
		connConfig.setProperty("user", "YOURUSER");
		connConfig.setProperty("password", "PASSWORD$");
		connConfig.setProperty("useSsl", "true");
		connConfig.setProperty("sslMode", "verify-full");
		connConfig.setProperty("serverSslCert", "/path/to/your/skysql_chain.pem");

		connection = DriverManager.getConnection(
				"jdbc:mariadb://YOURSERVER:YOURPORT/orders",
				connConfig);
		System.out.println("Connection valid: " + connection.isValid(5));
	}

	private static void closeDatabaseConnection() throws SQLException {
		System.out.println("Closing database connection...");
		connection.close();
		System.out.println("Connection valid: " + connection.isValid(5));
	}
}
