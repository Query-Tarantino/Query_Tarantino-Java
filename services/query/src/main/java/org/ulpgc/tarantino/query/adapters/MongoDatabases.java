package org.ulpgc.tarantino.query.adapters;

import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import com.mongodb.client.MongoDatabase;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

final class MongoDatabases {

    private static final String DATABASE = "tarantino";
    private static final Map<String, MongoClient> CLIENTS = new ConcurrentHashMap<>();

    private MongoDatabases() {
    }

    static MongoDatabase database(String connectionUri) {
        return CLIENTS.computeIfAbsent(connectionUri, MongoClients::create).getDatabase(DATABASE);
    }
}
