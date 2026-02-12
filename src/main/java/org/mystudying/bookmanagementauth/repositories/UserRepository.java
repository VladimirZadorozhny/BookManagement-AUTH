package org.mystudying.bookmanagementauth.repositories;

import org.mystudying.bookmanagementauth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByName(String name);
    Optional<User> findByEmail(String email);

    @Query("SELECT u FROM User u LEFT JOIN FETCH u.roles WHERE u.email = :email")
    Optional<User> findByEmailWithRoles(@Param("email") String email);

    /**
     * @deprecated Since introduction of Booking entity.
     * Use BookingRepository instead.
     * Kept temporarily to avoid breaking existing tests.
     */
    @Deprecated
    @Query("SELECT u FROM User u JOIN u.bookings b WHERE b.returnedAt IS NULL GROUP BY u.id HAVING COUNT(b.id) > :count")
    List<User> findUsersWithMoreThanXBooks(@Param("count") long count);


    @Query("""
            SELECT DISTINCT u FROM User u LEFT JOIN FETCH u.bookings b WHERE u.id = :id
            """)
    Optional<User> findUserByIdWithBookings(@Param("id") long id);
}
