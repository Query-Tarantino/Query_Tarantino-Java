package org.ulpgc.tarantino.query;

import org.ulpgc.tarantino.query.commands.SearchCommand;

public class Main {

    public static void main(String[] args) {
        SearchCommand search = QueryFactory.searchCommand(QueryConfig.fromEnvironment());
        System.out.println(search.execute(String.join(" ", args)));
    }
}
