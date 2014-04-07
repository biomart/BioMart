package org.biomart.queryEngine.v2;

import java.io.IOException;
import java.io.File;
import com.google.common.io.Files;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author jhsu
 */
public class ConnectionManagerTest {
    /*
     * Test a single connection
     */
    @Test
    public void testGetConnection() throws SQLException {
        ConnectionManager manager = new ConnectionManager();
        Connection conn = manager.getConnection("org.sqlite.JDBC",
                "jdbc:sqlite:test.db", null, null);

        Statement stat = conn.createStatement();
        stat.executeUpdate("drop table if exists people;");
        stat.executeUpdate("create table people (name);");
        PreparedStatement prep = conn.prepareStatement("insert into people values (?);");

        prep.setString(1, "foo");

        prep.executeUpdate();

        ResultSet rs = stat.executeQuery("select * from people;");

        if (rs.next()) {
            assertEquals("foo", rs.getString("name"));
        } else {
            fail("Update failed");
        }

        rs.close();
        conn.close();

        try {
            Files.deleteRecursively(new File("test.db"));
        } catch (IOException e) {
            // file removal failed
        }
    }

    /*
     * Test that multiple connnections work, and the number of pools created is
     * what is expected.
     */
    @Test
    public void testMultipleSources() throws SQLException {
        ConnectionManager manager = new ConnectionManager();
        Connection conn1 = manager.getConnection("org.sqlite.JDBC",
                "jdbc:sqlite:test1.db", null, null);
        Statement stat1 = conn1.createStatement();
        stat1.executeUpdate("drop table if exists people;");
        stat1.executeUpdate("create table people (name);");
        PreparedStatement prep1 = conn1.prepareStatement("insert into people values (?);");

        Connection conn2 = manager.getConnection("org.sqlite.JDBC",
                "jdbc:sqlite:test2.db", null, null);
        Statement stat2 = conn2.createStatement();
        stat2.executeUpdate("drop table if exists people;");
        stat2.executeUpdate("create table people (name);");
        PreparedStatement prep2 = conn2.prepareStatement("insert into people values (?);");

        Connection conn3 = manager.getConnection("org.sqlite.JDBC",
                "jdbc:sqlite:test3.db", null, null);
        Statement stat3 = conn3.createStatement();
        stat3.executeUpdate("drop table if exists people;");
        stat3.executeUpdate("create table people (name);");
        PreparedStatement prep3 = conn3.prepareStatement("insert into people values (?);");

        // Same as 1
        Connection conn4 = manager.getConnection("org.sqlite.JDBC",
                "jdbc:sqlite:test1.db", null, null);
        PreparedStatement prep4 = conn4.prepareStatement("insert into people values (?);");

        prep1.setString(1, "foo");
        prep1.executeUpdate();

        prep2.setString(1, "bar");
        prep2.executeUpdate();

        prep3.setString(1, "faz");
        prep3.executeUpdate();

        prep4.setString(1, "baz");
        prep4.executeUpdate();

        ResultSet rs1 = stat1.executeQuery("select * from people;");
        ResultSet rs2 = stat2.executeQuery("select * from people;");
        ResultSet rs3 = stat3.executeQuery("select * from people;");

        if (rs1.next()) {
            assertEquals("foo", rs1.getString("name"));
        } else {
            fail("Update failed");
        }

        if (rs1.next()) {
            assertEquals("baz", rs1.getString("name"));
        } else {
            fail("Update failed");
        }

        if (rs2.next()) {
            assertEquals("bar", rs2.getString("name"));
        } else {
            fail("Update failed");
        }

        if (rs3.next()) {
            assertEquals("faz", rs3.getString("name"));
        } else {
            fail("Update failed");
        }

        rs1.close();
        rs2.close();
        rs3.close();
        conn1.close();
        conn2.close();
        conn3.close();

        assertEquals(3, manager.getCurrentSize());

        try {
            Files.deleteRecursively(new File("test1.db"));
            Files.deleteRecursively(new File("test2.db"));
            Files.deleteRecursively(new File("test3.db"));
        } catch (IOException e) {
            // file removal failed
        }
    }
}
