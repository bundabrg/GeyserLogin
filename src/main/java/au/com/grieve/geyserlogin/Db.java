/*
 * GeyserLogin - Log in as a different username to Geyser
 * Copyright (C) 2020 GeyserLogin Developers
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package au.com.grieve.geyserlogin;


import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Provide Database Functions
 */
public class Db {
    private final Connection conn;

    public Db(File file) throws SQLException {
        conn = DriverManager.getConnection(String.format("jdbc:sqlite:%s", file.getAbsolutePath()));
        initialize();
        createTables();
    }

    private void initialize() {
        try {
            conn.createStatement().execute("PRAGMA foreign_keys = ON;");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    private void createTables() {
        try {
            conn.createStatement().execute("CREATE TABLE IF NOT EXISTS logins(" +
                    "  id INTEGER PRIMARY_KEY, " +
                    "  user_uuid VARCHAR, " +
                    "  username VARCHAR NOT NULL, " +
                    "  used DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP " +
                    ");"
            );
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    /**
     * Return the last 5 logins of a user from newset used to oldest
     */
    public List<String> getRecentLogins(UUID uuid) {
        List<String> ret = new ArrayList<>();

        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT username " +
                    "FROM logins " +
                    "WHERE user_uuid = ? " +
                    "ORDER BY used DESC " +
                    "LIMIT 5;");

            stmt.setString(1, uuid.toString());

            ResultSet rs = stmt.executeQuery();
            while (rs.next()) {
                ret.add(rs.getString(1));
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ret;
    }

    /**
     * Add a new login to history or update an existing one
     */
    public void addLogin(UUID uuid, String login) {
        try {
            PreparedStatement stmt = conn.prepareStatement("SELECT id " +
                    "FROM logins " +
                    "WHERE username = ?");
            stmt.setString(1, login);

            ResultSet rs = stmt.executeQuery();
            if (rs.next()) {
                stmt = conn.prepareStatement("UPDATE logins " +
                        "SET used = DATETIME('now') " +
                        "WHERE id = ?;");
                stmt.setInt(1, rs.getInt(1));
            } else {
                stmt = conn.prepareStatement("INSERT INTO logins(user_uuid, username) " +
                        "VALUES(?,?);");
                stmt.setString(1, uuid.toString());
                stmt.setString(2, login);
            }
            stmt.execute();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


}
