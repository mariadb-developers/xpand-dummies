const mariadb = require("mariadb");
const fs = require("fs");

async function main() {
    const serverCert = fs.readFileSync("/path/to/your/skysql_chain.pem", "utf8");
    const sqlInsertOrders = "insert into orders (customer_id, order_date, order_created, entered_by) values (?, curdate(), curtime(), ?)";
    const sqlInsertItems = "insert into order_item (order_id, line_num, product_id, description) values (?,?,?,?)";
    const sqlQueryOrdersItems = "select o.order_id, o.customer_id, o.order_date, o.order_created, o.entered_by, i.item_id, i.line_num, i.product_id, i.description from orders o inner join order_item i on o.order_id = i.order_id";
    const sqlDeleteItem = "delete from order_item where order_id = ? and line_num = ?";

    let connection;
    console.log("start ");
    try {
        connection = await mariadb.createConnection({
					host: "YOURHOST",
					port: YOURPORT,
					ssl: { 
						ca: serverCert 
					},
					user: "",
					password: "PASSWORD$",
					database: "orders"
        });
	console.log("connected");

        // If this doesn't work with your DB, try using utf8mb4 instead:
        await connection.execute("SET NAMES utf8");

	let res = await connection.query(sqlInsertOrders, ['1','andy']);
	orderId=res.insertId;

	console.log("inserting items for "+orderId);
	await connection.query(sqlInsertItems, [orderId, 1, 1, "box of chocolates"]);
	await connection.query(sqlInsertItems, [orderId, 2, 2, "flowers"]);
	
	let rows = await connection.query(sqlQueryOrdersItems);
	console.log("returned: "+rows.length);
	rows.forEach(row => {
		console.log(row);
	});
	console.log("deleting a row");
	await connection.query(sqlDeleteItem, [orderId, 2]);
	console.log("this time there will be one less");
	rows = await connection.query(sqlQueryOrdersItems);
	console.log("returned: "+rows.length);
	rows.forEach(row => {
		console.log(row);
	});

    } catch (err) {
        console.log("catch");
        console.log(err);
    } finally {
        if (connection) await connection.end();
    }
}

main();
