package org.ulpgc.tarantino.query.adapters;

import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.ports.MetadataReader;

import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/** Reads {@code datamarts/metadata.db} */
public class SqliteMetadataReader implements MetadataReader {

    private static final String BY_ID = "SELECT book_id, title, author, language, path FROM books WHERE book_id = ?";
    private static final String BY_AUTHOR =
            "SELECT book_id, title, author, language, path FROM books WHERE author LIKE ? ORDER BY book_id";

    private final Path database;

    public SqliteMetadataReader(Path database) {
        this.database = database;
    }

    @Override
    public Optional<BookMetadata> findById(int bookId) {
        List<BookMetadata> books = query(BY_ID, statement -> statement.setInt(1, bookId));
        return books.stream().findFirst();
    }

    /** Case-insensitive substring match on the author name. */
    @Override
    public List<BookMetadata> findByAuthor(String author) {
        return query(BY_AUTHOR, statement -> statement.setString(1, "%" + author + "%"));
    }

    private List<BookMetadata> query(String sql, Binder binder) {
        if (!Files.exists(database)) {
            return List.of();
        }
        try (Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
             PreparedStatement statement = connection.prepareStatement(sql)) {
            binder.bind(statement);
            List<BookMetadata> books = new ArrayList<>();
            try (ResultSet rows = statement.executeQuery()) {
                while (rows.next()) {
                    books.add(new BookMetadata(rows.getInt("book_id"), rows.getString("title"),
                            rows.getString("author"), rows.getString("language"), Path.of(rows.getString("path"))));
                }
            }
            return books;
        } catch (SQLException e) {
            throw new IllegalStateException("Could not query " + database, e);
        }
    }

    @FunctionalInterface
    private interface Binder {
        void bind(PreparedStatement statement) throws SQLException;
    }
}
