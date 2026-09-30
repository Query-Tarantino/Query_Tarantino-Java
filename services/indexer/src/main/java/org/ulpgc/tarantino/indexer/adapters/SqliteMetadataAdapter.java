package org.ulpgc.tarantino.indexer.adapters;

import org.ulpgc.tarantino.indexer.model.Book;
import org.ulpgc.tarantino.indexer.ports.MetadataStorage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;

/** Table {@code books(book_id, title, author, language, path)} in {@code datamarts/metadata.db} */
public class SqliteMetadataAdapter implements MetadataStorage {

    private static final String CREATE_TABLE = """
            CREATE TABLE IF NOT EXISTS books (
                book_id  INTEGER PRIMARY KEY,
                title    TEXT,
                author   TEXT,
                language TEXT,
                path     TEXT NOT NULL
            )""";
    private static final String UPSERT = "INSERT OR REPLACE INTO books (book_id, title, author, language, path) VALUES (?, ?, ?, ?, ?)";

    private final Path database;

    public SqliteMetadataAdapter(Path database) {
        this.database = database;
    }

    @Override
    public void save(Book book) {
        try (Connection connection = connect(); PreparedStatement statement = connection.prepareStatement(UPSERT)) {
            statement.setInt(1, book.bookId());
            statement.setString(2, book.title());
            statement.setString(3, book.author());
            statement.setString(4, book.language());
            statement.setString(5, book.path().toString());
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException("Could not save metadata of book " + book.bookId(), e);
        }
    }

    private Connection connect() throws SQLException {
        try {
            Files.createDirectories(database.toAbsolutePath().getParent());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + database);
        try (Statement statement = connection.createStatement()) {
            statement.execute(CREATE_TABLE);
        }
        return connection;
    }
}
