package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class DatabaseHelperTests {
    private DatabaseHelper databaseHelper;

    // Setup/Teardown Routines

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        databaseHelper = new DatabaseHelper(context);
    }

    @After
    public void tearDown() {
        databaseHelper.close();
    }

// Tests

    @Test
    public void splitScript() {
        String comment0 = "-- Create Table\n";
        String command0 = "CREATE TABLE table1 (\n field int\n);";
        String comment1 = "-- Create Table\n";
        String command1 = "CREATE TABLE table2 (\n field int\n);";
        String script = comment0 + command0 + "\n" + comment1 + command1 + "\n";
        String[] commands = databaseHelper.splitScript(script);
        assertEquals("Length", 2, commands.length);
        assertEquals("Command[0]", command0, commands[0]);
        assertEquals("Command[1]", command1, commands[1]);
    }

}
