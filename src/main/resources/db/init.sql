DROP TABLE IF EXISTS user;

CREATE TABLE user (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL,
    password TEXT NOT NULL DEFAULT '123456',
    email TEXT UNIQUE DEFAULT '',
    create_time DATETIME DEFAULT CURRENT_TIMESTAMP
);

INSERT INTO user (username, password, email) VALUES
('zhangsan', '123456', 'zhangsan@test.com'),
('lisi', '654321', 'lisi@test.com');
