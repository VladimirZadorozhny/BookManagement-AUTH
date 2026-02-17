-- Create 10 users for concurrency test
INSERT INTO users(name, email, password, active)
VALUES ('Concurrency Test User 1', 'conc.test1@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 2', 'conc.test2@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 3', 'conc.test3@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 4', 'conc.test4@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 5', 'conc.test5@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 6', 'conc.test6@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 7', 'conc.test7@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 8', 'conc.test8@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 9', 'conc.test9@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true),
       ('Concurrency Test User 10', 'conc.test10@example.com', '{bcrypt}$2a$10$W8Fh/h9nADK75zl/zXWAeOsq43iLzwrtbeLi/HnZdYwUhzjbOT2Ra', true);
