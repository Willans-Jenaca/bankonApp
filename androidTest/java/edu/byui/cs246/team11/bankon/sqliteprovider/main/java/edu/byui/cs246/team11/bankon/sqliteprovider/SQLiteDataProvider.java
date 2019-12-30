package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.DataProvider;
import edu.byui.cs246.team11.bankon.core.UserAccountRepository;
import edu.byui.cs246.team11.bankon.core.UserRepository;


public class SQLiteDataProvider implements DataProvider {

    public static final String TAG = SQLiteDataProvider.class.getName();

    private DatabaseHelper databaseHelper;

    public SQLiteDataProvider(Context context) {
        databaseHelper = new DatabaseHelper(context);
    }

    @Override
    public ActionRepository getActionRepository() {
        return new SQLiteActionRepository(databaseHelper);
    }

    @Override
    public UserRepository getUserRepository() {
        return new SQLiteUserRepository(databaseHelper);
    }

    @Override
    public UserAccountRepository getUserAccountRepository() {
        return new SQLiteUserAccountRepository(databaseHelper);
    }
}
