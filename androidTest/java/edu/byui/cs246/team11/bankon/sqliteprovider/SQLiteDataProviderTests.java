package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.DataProvider;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SQLiteDataProviderTests {

    @Test
    public void getActionRepository() {
        Context context = InstrumentationRegistry.getTargetContext();
        DataProvider dataProvider = new SQLiteDataProvider(context);
        ActionRepository actionRepository = dataProvider.getActionRepository();
        assertNotNull("Returned null", actionRepository);
    }

    @Test
    public void getUserRepository() {
        Context context = InstrumentationRegistry.getTargetContext();
        DataProvider dataProvider = new SQLiteDataProvider(context);
        UserRepository userRepository = dataProvider.getUserRepository();
        assertNotNull("Returned null", userRepository);
    }
}
