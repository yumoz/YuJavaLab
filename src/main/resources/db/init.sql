DROP TABLE IF EXISTS user;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS account;

CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL DEFAULT '123456',
    email TEXT UNIQUE DEFAULT '',
    user_type INTEGER NOT NULL DEFAULT 0,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    user_id INTEGER NOT NULL,
    order_no TEXT NOT NULL,
    amount REAL NOT NULL,
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE account (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    account_no TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    balance REAL NOT NULL DEFAULT 0
);

INSERT INTO user (username, password, email) VALUES
('zhangsan', '123456', 'zhangsan@test.com'),
('lisi', '654321', 'lisi@test.com');

INSERT INTO orders (user_id, order_no, amount) VALUES
(1, 'A1001', 199.00),
(1, 'A1002', 59.50),
(2, 'A1003', 88.00);

INSERT INTO account (account_no, name, balance) VALUES
('ACCT-10001', 'zhangsan', 1000.00),
('ACCT-10002', 'lisi', 500.00);
