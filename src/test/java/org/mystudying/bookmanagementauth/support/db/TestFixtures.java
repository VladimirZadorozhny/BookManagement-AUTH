package org.mystudying.bookmanagementauth.support.db;

/**
 * Shared constants for test data, matching records in:
 * - insertTestRecords.sql
 * - insertConcurrentUsersTestRecords.sql
 * - insertUserLogicTestRecords.sql
 */
public final class TestFixtures {

    private TestFixtures() {}

    // --- Users ---
    public static final String USER_1_EMAIL = "test1@example.com";
    public static final String USER_1_NAME = "Test User 1";
    public static final String USER_2_EMAIL = "test2@example.com";
    public static final String USER_2_NAME = "Test User 2";
    public static final String USER_DELETE_EMAIL = "delete@example.com";
    public static final String USER_DELETE_NAME = "User For Deletion";
    public static final String USER_RENT_EMAIL = "rent@example.com";
    public static final String USER_RENT_NAME = "Rent User";
    
    public static final String ADMIN_EMAIL = "admin@library.com";
    
    public static final String LOGIC_USER_CLEAN = "clean@logic.test";
    public static final String LOGIC_USER_OVERDUE = "overdue@logic.test";
    public static final String LOGIC_USER_FINE = "fine@logic.test";

    // --- Books ---
    public static final String BOOK_1_TITLE = "Test Book 1";
    public static final String BOOK_2_TITLE = "Test Book 2";
    public static final String BOOK_DELETE_TITLE = "Book For Deletion";
    public static final String BOOK_RENTABLE_TITLE = "Rentable Book";
    
    public static final String LOGIC_BOOK_A = "Logic Book A";
    public static final String LOGIC_BOOK_B = "Logic Book B";
    public static final String LOGIC_BOOK_OVERDUE = "Overdue Book";
    public static final String LOGIC_BOOK_FINED = "Fined Book";

    // --- Authors ---
    public static final String AUTHOR_1_NAME = "Test Author 1";
    public static final String AUTHOR_2_NAME = "Test Author 2";
    public static final String AUTHOR_DELETE_NAME = "Author For Deletion";
    public static final String AUTHOR_LOGIC_NAME = "Logic Author";

    // --- Genres ---
    public static final String GENRE_1_NAME = "Test Genre 1";
    public static final String GENRE_2_NAME = "Test Genre 2";
    public static final String GENRE_3_NAME = "Test Genre 3";
    public static final String GENRE_LOGIC_NAME = "Logic Genre";

    // --- Passwords ---
    public static final String COMMON_PASSWORD = "password";
    public static final String ADMIN_PASSWORD = "admin";
}
