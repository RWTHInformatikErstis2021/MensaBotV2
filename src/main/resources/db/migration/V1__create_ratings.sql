CREATE TABLE IF NOT EXISTS ratings (
    userId BIGINT,
    meal VARCHAR(100),
    city VARCHAR(50),
    rating INTEGER,
    PRIMARY KEY (userId, city, meal)
);
