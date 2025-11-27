package com.[REDACTED].spotifytracker.api.data;
import java.sql.*;

public class DatabaseWrapper {
    private final static Connection db;
    private final static String url = "jdbc:postgresql://localhost:5433/track-database";
    private final static String username = "[REDACTED]";
    private final static String [REDACTED];


    static {
        try {
            db = DriverManager.getConnection(url, username, password);
        } catch (SQLException e) {
            throw new RuntimeException(e);
        }
    }


}
