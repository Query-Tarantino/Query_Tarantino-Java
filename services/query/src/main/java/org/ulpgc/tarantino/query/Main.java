package org.ulpgc.tarantino.query;

import org.ulpgc.tarantino.query.commands.SearchCommand;
import org.ulpgc.tarantino.query.model.BookMetadata;
import org.ulpgc.tarantino.query.model.SearchResult;

public class Main {

    public static void main(String[] args) {
        SearchCommand search = QueryFactory.searchCommand(QueryConfig.fromEnvironment());
        SearchResult result = search.execute(String.join(" ", args));
        System.out.printf("%d result(s) for \"%s\"%n", result.books().size(), result.query());
        for (BookMetadata book : result.books()) {
            System.out.printf("  [%d] %s — %s (%s) %s%n",
                    book.bookId(), book.title(), book.author(), book.language(), book.path());
        }
    }
}
