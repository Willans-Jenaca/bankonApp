package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.RepositoryException.ObjectNotFoundException;
import edu.byui.cs246.team11.bankon.core.User;
import edu.byui.cs246.team11.bankon.core.UserQuerySpecification;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import edu.byui.cs246.team11.bankon.core.UserType;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.Users;
import java.util.ArrayList;
import java.util.List;

public class SQLiteUserRepository extends BaseRepository implements UserRepository {

    public static final String TAG = SQLiteUserRepository.class.getName();

    public SQLiteUserRepository(DatabaseHelper databaseHelper) {
        super(databaseHelper);
    }

    private final int USER_TYPE_PARENT = 1;
    private final int USER_TYPE_CHILD = 2;

    private int userTypeToInt(UserType userType) throws RepositoryException {
        switch (userType) {
            case PARENT: return USER_TYPE_PARENT;
            case CHILD: return USER_TYPE_CHILD;
        }
        throw new RepositoryException("Unhandled User Type");
    }

    private UserType intToUserType(int value) throws RepositoryException {
        switch (value) {
            case USER_TYPE_PARENT: return UserType.PARENT;
            case USER_TYPE_CHILD: return UserType.CHILD;
        }
        throw new RepositoryException("Unhandled User Type");
    }

    @Override
    public User getUser(int userId) throws RepositoryException {
        if (userId == 0) return null;
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try {
            String where = Users.COLUMN_USER_ID + " = ?";
            String[] whereArgs = { Integer.toString(userId) };
            Cursor cursor = db.query(
                Users.TABLE_NAME,
                Users.COLUMNS_ALL,
                where,
                whereArgs,
                null,
                null,
                null
            );
            try {
                return new UserLoader().ObjectFromCursor(cursor);
            } finally {
                cursor.close();
            }
        } finally {
            db.close();
        }
    }

    @Override
    public List<User> getUsers(UserQuerySpecification spec) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try {
            String where = "";
            ArrayList<String> whereArgs = new ArrayList<String>();
            String orderBy = Users.COLUMN_NAME;

            // Filter by UserType
            UserType[] userTypes = spec.getUserTypes();
            if (userTypes.length > 0) {
                where += "(";
                for (int i = 0; i < userTypes.length; i++) {
                    if (i > 0) where += " OR ";
                    where += Users.COLUMN_USER_TYPE + " = ?";
                    whereArgs.add(Integer.toString(userTypeToInt(userTypes[i])));
                }
                where += ")";
            }

            Cursor cursor = db.query(
                Users.TABLE_NAME,
                Users.COLUMNS_ALL,
                where,
                whereArgs.toArray(new String[0]),
                null,
                null,
                orderBy
            );
            try {
                return new UserLoader().ObjectsFromCursor(cursor);
            } finally {
                cursor.close();
            }
        } finally {
            db.close();
        }
    }

    @Override
    public void deleteUser(User user) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        try {
            String where = Users.COLUMN_USER_ID + " = ?";
            String[] whereArgs = { Integer.toString(user.getUserId()) };

            int count = db.delete(
                Users.TABLE_NAME,
                where,
                whereArgs
            );

            if (count == 0) {
                throw new ObjectNotFoundException(User.class, user.getUserId());
            }
        } finally {
            db.close();
        }
    }

    @Override
    public void saveUser(User user) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        try {
            ContentValues values = new ContentValues();
            values.put(Users.COLUMN_NAME, user.getName());
            values.put(Users.COLUMN_PIN, user.getPin());
            values.put(Users.COLUMN_USER_TYPE, userTypeToInt(user.getUserType()));
            values.put(Users.COLUMN_AUTHENTICATOR_SECRET, user.getAuthenticatorSecret());

            if (user.getUserId() == 0) {
                Long id = db.insertOrThrow(Users.TABLE_NAME, null, values);
                user.setUserId(id.intValue());
            } else {
                String where = Users.COLUMN_USER_ID + " = ?";
                String[] whereArgs = { Integer.toString(user.getUserId()) };

                int count = db.update(
                    Users.TABLE_NAME,
                    values,
                    where,
                    whereArgs
                );

                if (count == 0) {
                    throw new ObjectNotFoundException(User.class, user.getUserId());
                }
            }
        } finally {
            db.close();
        }
    }

    private class UserLoader extends ObjectLoader<User> {
        @Override
        protected User FromCursor(Cursor cursor) throws RepositoryException {
            User user = new User();
            user.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(Users.COLUMN_USER_ID)));
            user.setName(cursor.getString(cursor.getColumnIndexOrThrow(Users.COLUMN_NAME)));
            user.setPin(cursor.getString(cursor.getColumnIndexOrThrow(Users.COLUMN_PIN)));
            user.setUserType(intToUserType(cursor.getInt(cursor.getColumnIndexOrThrow(Users.COLUMN_USER_TYPE))));
            user.setAuthenticatorSecret(cursor.getString(cursor.getColumnIndexOrThrow(Users.COLUMN_AUTHENTICATOR_SECRET)));
            return user;
        }
    }
}
