package org.mystudying.bookmanagementauth.repositories;

import jakarta.persistence.LockModeType;
import org.mystudying.bookmanagementauth.domain.Author;
import org.mystudying.bookmanagementauth.domain.Book;
import org.mystudying.bookmanagementauth.dto.BookDetailDto;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    List<Book> findByYear(int year);

    Page<Book> findByYear(int year, Pageable pageable);

    List<Book> findByAuthor_Id(Long authorId);

    Page<Book> findByAuthor_Id(Long authorId, Pageable pageable);

    List<Book> findByGenres_Id(Long genreId);

    Page<Book> findByGenres_Id(Long genreId, Pageable pageable);

    List<Book> findByGenres_NameIgnoreCase(String name);

    Page<Book> findByGenres_NameIgnoreCase(String name, Pageable pageable);


    @Query("SELECT b FROM Book b JOIN b.author a WHERE a.name = :authorName ORDER BY b.title")
    List<Book> findByAuthorName(@Param("authorName") String authorName);

    @Query(value = "SELECT b FROM Book b JOIN b.author a WHERE a.name = :authorName",
            countQuery = "SELECT count(b) FROM Book b JOIN b.author a WHERE a.name = :authorName")
    Page<Book> findByAuthorName(@Param("authorName") String authorName, Pageable pageable);

    @Query("SELECT b FROM Book b WHERE (:available = true AND b.available > 0) OR (:available = false AND b.available = 0) ORDER BY b.title")
    List<Book> findByAvailability(@Param("available") boolean available);

    @Query(value = "SELECT b FROM Book b WHERE (:available = true AND b.available > 0) OR (:available = false AND b.available = 0)",
            countQuery = "SELECT count(b) FROM Book b WHERE (:available = true AND b.available > 0) OR (:available = false AND b.available = 0)")
    Page<Book> findByAvailability(@Param("available") boolean available, Pageable pageable);

    Optional<Book> findByTitle(String title);

    @Query("SELECT b FROM Book b JOIN b.bookings bk WHERE bk.user.id = :userId AND bk.returnedAt IS NULL ORDER BY b.title")
    List<Book> findBooksByUserId(@Param("userId") long userId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT b FROM Book b WHERE b.id = :id")
    Optional<Book> findAndLockById(@Param("id") long id);

    @Query("SELECT new org.mystudying.bookmanagementauth.dto.BookDetailDto(b.id, b.title, b.year, b.available, a.name, a.id) " +
            "FROM Book b JOIN b.author a WHERE b.id = :id")
    Optional<BookDetailDto> findBookDetailsById(@Param("id") long id);


    List<Book> findByTitleContainingOrderByTitle(String title);

    Page<Book> findByTitleContaining(String title, Pageable pageable);

    @Query("SELECT b FROM Book b JOIN b.author a WHERE a.name LIKE %:authorName% ORDER BY b.title")
    List<Book> findByAuthorNameContaining(@Param("authorName") String authorName);

    @Query(value = "SELECT b FROM Book b JOIN b.author a WHERE a.name LIKE %:authorName%",
            countQuery = "SELECT count(b) FROM Book b JOIN b.author a WHERE a.name LIKE %:authorName%")
    Page<Book> findByAuthorNameContaining(@Param("authorName") String authorName, Pageable pageable);

    boolean existsByAuthor(Author author);

    boolean existsByGenres_Id(Long genreId);


    @Modifying
    @Query("""
            UPDATE Book b
            SET b.available = b.available - 1
            WHERE b.id = :id AND b.available > 0
            """)
    int decrementAvailableIfInStock(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Book b
            SET b.available = b.available + 1
            WHERE b.id = :id
            """)
    int incrementAvailable(@Param("id") Long id);

    @Modifying
    @Query("""
            UPDATE Book b
            SET b.available = b.available + :amount
            WHERE b.id = :id
            """)
    int incrementAvailableBy(@Param("id") Long id, @Param("amount") int amount);

    @Modifying
    @Query("""
            UPDATE Book b
            SET b.available = b.available - :amount
            WHERE b.id = :id AND b.available >= :amount
            """)
    int decrementAvailableBy(@Param("id") Long id, @Param("amount") int amount);

    @Query("SELECT COUNT(b) FROM Book b JOIN b.genres g WHERE g.id = :genreId")
    long countBooksByGenreId(Long genreId);
}

