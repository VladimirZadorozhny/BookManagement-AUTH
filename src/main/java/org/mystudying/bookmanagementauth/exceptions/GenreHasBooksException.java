package org.mystudying.bookmanagementauth.exceptions;

public class GenreHasBooksException extends RuntimeException {
    public GenreHasBooksException(long id) {
        super("Cannot delete genre with id " + id + " because it is associated with books.");
    }
}
