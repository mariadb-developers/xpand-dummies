import mariadb

sqlInsertOrders = "insert into orders (customer_id, order_date, order_created, entered_by) values (?, curdate(), curtime(), ?)"
sqlInsertItems = "insert into order_item (order_id, line_num, product_id, description) values (?,?,?,?)"
sqlQueryOrdersItems = "select o.order_id, o.customer_id, o.order_date, o.order_created, o.entered_by, i.item_id, i.line_num, i.product_id, i.description from orders o inner join order_item i on o.order_id = i.order_id"
sqlDeleteItem = "delete from order_item where order_id = ? and line_num = ?"

# Connect w/o specifying a database & with the default of autocommit=False
connection = mariadb.connect(
    host="YOURHOST",
    port=YOURPORT,
    ssl_ca="/path/to/your/skysql_chain.pem",
    user="YOURUSER",
    password="PASSWORD$",
    database="orders",
    )

# Each connection needs at least one cursor associated with it in order to
# make changes and request results. This cursor is created with the option
# that allows some per-row access using a named tuple.  There is also an
# alternate option of dictionary=True or the default of normal tuples.
cursor = connection.cursor(named_tuple=True)

cursor.execute("SET NAMES utf8")

print("inserting order")
cursor.execute(
    sqlInsertOrders,
    (1,"andy"))

cursor.execute("select last_insert_id() as insert_id");
orderid = cursor.fetchone()[0];
print(orderid);
print("inserting items")
cursor.executemany(
    sqlInsertItems, [
        (orderid, 1,1,"box of chocolates"),
        (orderid, 2,2,"flowers")
    ])

connection.commit()

print("listing order items");
cursor.execute(
    sqlQueryOrdersItems
)

for x in range(0,cursor.rowcount):
    row = cursor.fetchone()
    print(row) 

print("deleting second item");
cursor.execute(
    sqlDeleteItem,
    (orderid,2))

connection.commit()

print("listing order items (should be once less)");
cursor.execute(
    sqlQueryOrdersItems
)


for x in range(0,cursor.rowcount):
    row = cursor.fetchone()
    print(row) 

