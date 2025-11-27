package com.pugking4.spotifytracker.api.data;
import java.sql.*;

public class DatabaseWrapper {
    private final static Connection db;
    private final static String url = "jdbc:postgresql://localhost:5433/track-database";
    private final static String username = "pugking4";
    private final static String password = "apples";


    static {
        try {
            db = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
