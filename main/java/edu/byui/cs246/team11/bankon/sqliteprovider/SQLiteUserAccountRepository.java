package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.UserAccount;
import edu.byui.cs246.team11.bankon.core.UserAccountRepository;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.Actions;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.UserAccounts;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.Users;

/**
 * Implementation of the UserAccountRepository interface using SQLite.
 */
public class SQLiteUserAccountRepository extends BaseRepository implements UserAccountRepository {

    public static final String TAG = SQLiteUserAccountRepository.class.getName();

    public SQLiteUserAccountRepository(DatabaseHelper databaseHelper) {
        super(databaseHelper);
    }

    /**
     * Calculates the balances for a user if required and returns the current balances.
     */
    @Override
    public UserAccount getUserAccount(int userId) throws RepositoryException {
        if (userId == 0) return null;
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        try {
            UserAccount userAccount = internalGetUserAccount(db, userId);
            if (userAccount == null) {
                refreshUserAccount(db, userId);
                userAccount = internalGetUserAccount(db, userId);
            }
            return userAccount;
        } finally {
            db.close();
        }
    }

    /**
     * Internal method that gets the current account for the specified user.
     * @return UserAccount if available or null.
     */
    private UserAccount internalGetUserAccount(SQLiteDatabase db, int userId) throws RepositoryException {
        String where = UserAccounts.COLUMN_USER_ID + " = ?";
        String[] whereArgs = { Integer.toString(userId) };
        Cursor cursor = db.query(
            UserAccounts.TABLE_NAME,
            UserAccounts.COLUMNS_ALL,
            where,
            whereArgs,
            null,
            null,
            null
        );
        try {
            if (!cursor.moveToFirst()) return null;
            if (cursor.getInt(cursor.getColumnIndexOrThrow(UserAccounts.COLUMN_PENDING_REFRESH)) == 1) return null;
            return new UserAccountLoader().ObjectFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    /**
     * Recalculates and saves the balances for the specified user.
     */
    private void refreshUserAccount(SQLiteDatabase db, int userId) {
        String completed = Integer.toString(Actions.ACTION_STATUS_COMPLETED);
        String approved = Integer.toString(Actions.ACTION_STATUS_APPROVED);
        String usersUserId = Users.TABLE_NAME + "." + Users.COLUMN_USER_ID;
        String actionsUserId = Actions.TABLE_NAME + "." + Actions.COLUMN_USER_ID;
        String sql = "REPLACE INTO " + UserAccounts.TABLE_NAME
                   + "  SELECT "
                   + "    " + usersUserId + ","
                   + "    SUM("
                   + "      CASE WHEN " + Actions.COLUMN_ACTION_STATUS + " = " + approved
                   + "           THEN " + Actions.COLUMN_AMOUNT
                   + "           ELSE 0 END) AS " + UserAccounts.COLUMN_BALANCE + ","
                   + "    SUM("
                   + "      CASE WHEN " + Actions.COLUMN_ACTION_STATUS + " = " + completed
                   + "           THEN " + Actions.COLUMN_AMOUNT
                   + "           ELSE 0 END) AS " + UserAccounts.COLUMN_PENDING_BALANCE + ","
                   + "    0 AS pendingRefresh"
                   + "  FROM " + Users.TABLE_NAME
                   + "  LEFT JOIN " + Actions.TABLE_NAME
                   + "         ON " + actionsUserId + " = " + usersUserId
                   + "        AND " + Actions.COLUMN_ACTION_STATUS + " IN (" + completed + "," + approved + ")"
                   + "  WHERE " + usersUserId + " = ?"
                   + "  GROUP BY " + usersUserId;
        String[] args = { Integer.toString(userId) };
        db.execSQL(sql, args);
    }

    /**
     * Class to deserialize UserAccount objects based on a data cursor.
     */
    private class UserAccountLoader extends ObjectLoader<UserAccount> {

        @Override
        protected UserAccount FromCursor(Cursor cursor) throws RepositoryException {
            UserAccount userAccount = new UserAccount();
            userAccount.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(UserAccounts.COLUMN_USER_ID)));
            userAccount.setBalance(cursor.getFloat(cursor.getColumnIndexOrThrow(UserAccounts.COLUMN_BALANCE)));
            userAccount.setPendingBalance(cursor.getFloat(cursor.getColumnIndexOrThrow(UserAccounts.COLUMN_PENDING_BALANCE)));
            return userAccount;
        }
    }
}
