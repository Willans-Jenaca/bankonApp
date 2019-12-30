package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import edu.byui.cs246.team11.bankon.core.Action;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.ActionStatus;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.RepositoryException.ObjectNotFoundException;
import edu.byui.cs246.team11.bankon.core.User;
import edu.byui.cs246.team11.bankon.core.UserAccount;
import edu.byui.cs246.team11.bankon.core.UserAccountRepository;
import edu.byui.cs246.team11.bankon.core.UserQuerySpecification;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import edu.byui.cs246.team11.bankon.core.UserType;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static edu.byui.cs246.team11.bankon.sqliteprovider.TestDataObjectUtils.addAction;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SQLiteUserRepositoryTests {
    private DatabaseHelper databaseHelper;
    private UserRepository userRepository;

    private final String USER_NAME = "John Doe";
    private final String PIN = "123456";
    private final String AUTHENTICATOR_SECRET = "SECRET";

    // Setup/Teardown Routines

    @Before
    public void setup() {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        databaseHelper = new DatabaseHelper(context);
        userRepository = new SQLiteUserRepository(databaseHelper);
    }

    @After
    public void tearDown() {
        userRepository = null;
        databaseHelper.close();
    }

    // Tests

    @Test
    public void addUser() throws RepositoryException {
        User user = addUser(1);
        assertEquals("Saved User ID", 1, user.getUserId());

        user = userRepository.getUser(1);
        assertUserFields(user, 1, 1);
    }

    @Test
    public void deleteUser() throws RepositoryException {
        addUser(1);

        User user = userRepository.getUser(1);
        assertUserFields(user, 1, 1);

        userRepository.deleteUser(user);

        user = userRepository.getUser(1);
        assertNull("User is not null", user);
    }

    @Test
    public void deleteUserWithDependents() throws RepositoryException {
        ActionRepository actionRepository = new SQLiteActionRepository(databaseHelper);
        UserAccountRepository userAccountRepository = new SQLiteUserAccountRepository(databaseHelper);

        addUser(1);

        User user = userRepository.getUser(1);
        assertUserFields(user, 1, 1);

        addAction(actionRepository, user, ActionStatus.COMPLETED, 5);
        assertNotNull("Action is null", actionRepository.getAction(1));
        assertNotNull("UserAccount is null", userAccountRepository.getUserAccount(1));

        userRepository.deleteUser(user);

        assertNull("User is not null", userRepository.getUser(1));
        assertNull("Action is not null", actionRepository.getAction(1));
        assertNull("UserAccount is not null", userAccountRepository.getUserAccount(1));
    }

    @Test(expected=ObjectNotFoundException.class)
    public void deleteUserDoesNotExist() throws RepositoryException {
        User user = newUser(1);
        user.setUserId(123456);
        userRepository.deleteUser(user);
    }

    @Test
    public void getUserDoesNotExist() throws RepositoryException {
        User user = userRepository.getUser(123456);
        assertNull("User is not null", user);
    }

    @Test
    public void getUsers() throws RepositoryException {
        for(int i = 0; i < 10; i++) {
            User user = addUser(i + 1);
            assertEquals("Saved User ID", i + 1, user.getUserId());
        }

        List<User> list = userRepository.getUsers(new UserQuerySpecification());
        assertNotNull("List is null", list);
        assertEquals("List.size", 10, list.size());
        for(int i = 0; i < 10; i++) {
            User user = list.get(i);
            assertUserFields(user, i + 1, i + 1);
        }
    }

    @Test
    public void getUsersFilterByUserType() throws RepositoryException {
        for(int i = 0; i < 4; i++) {
            addUser(i + 1);
        }

        // Check Children
        UserQuerySpecification spec = new UserQuerySpecification();
        spec.setUserTypes(new UserType[]{ UserType.CHILD });
        List<User> list = userRepository.getUsers(spec);
        assertNotNull("Child List is null", list);
        assertEquals("Child List.size", 2, list.size());
        for(int i = 0; i < 2; i++) {
            User user = list.get(i);
            int suffix = (i * 2) + 1;
            assertUserFields(user, suffix, suffix, "Child");
        }

        // Check Parent
        spec = new UserQuerySpecification();
        spec.setUserTypes(new UserType[]{ UserType.PARENT });
        list = userRepository.getUsers(spec);
        assertNotNull("Parent List is null", list);
        assertEquals("Parent List.size", 2, list.size());
        for(int i = 0; i < 2; i++) {
            User user = list.get(i);
            int suffix = (i * 2) + 2;
            assertUserFields(user, suffix, suffix, "Parent");
        }

        // Check Mixed
        spec = new UserQuerySpecification();
        spec.setUserTypes(new UserType[]{ UserType.PARENT, UserType.CHILD });
        list = userRepository.getUsers(spec);
        assertNotNull("Mixed List is null", list);
        assertEquals("Mixed List.size", 4, list.size());
        for(int i = 0; i < 4; i++) {
            User user = list.get(i);
            assertUserFields(user, i + 1, i + 1);
        }
    }

    @Test
    public void getUsersNoneReturned() throws RepositoryException {
        List<User> list = userRepository.getUsers(new UserQuerySpecification());
        assertNotNull("List is null", list);
        assertEquals("List.size", 0, list.size());
    }

    @Test
    public void updateUser() throws RepositoryException {
        User user = addUser(1);
        assertEquals("Saved User ID", 1, user.getUserId());

        setUserFields(user, 2);
        userRepository.saveUser(user);

        user = userRepository.getUser(1);
        assertUserFields(user, 1, 2);
    }

    @Test(expected=ObjectNotFoundException.class)
    public void updateUserDoesNotExist() throws RepositoryException {
        User user = newUser(1);
        user.setUserId(123456);
        userRepository.saveUser(user);
    }

    // Supporting Methods

    private void assertUserFields(User user, int expectedId, int expectedSuffix, String msg) {
        String msgSuffix = (msg == "" ? "" : msg + " ");
        assertNotNull(msgSuffix + "User is null", user);
        assertEquals(msgSuffix + "User.UserID", expectedId, user.getUserId());
        assertEquals(msgSuffix + "User.Name", USER_NAME + String.format("%02d", expectedSuffix), user.getName());
        assertEquals(msgSuffix + "User.Pin", PIN + Integer.toString(expectedSuffix), user.getPin());
        if (expectedSuffix % 2 == 0) {
            assertEquals(msgSuffix + "User.UserType", UserType.PARENT, user.getUserType());
        } else {
            assertEquals(msgSuffix + "User.UserType", UserType.CHILD, user.getUserType());
        }
        assertEquals(msgSuffix + "User.AuthenticatorSecret", AUTHENTICATOR_SECRET + Integer.toString(expectedSuffix), user.getAuthenticatorSecret());
    }

    private void assertUserFields(User user, int expectedId, int expectedSuffix) {
        assertUserFields(user, expectedId, expectedSuffix, "");
    }

    private void setUserFields(User user, int suffix) {
        user.setName(USER_NAME + String.format("%02d", suffix));
        user.setPin(PIN + Integer.toString(suffix));
        if (suffix % 2 == 0) {
            user.setUserType(UserType.PARENT);
        } else {
            user.setUserType(UserType.CHILD);
        }
        user.setAuthenticatorSecret(AUTHENTICATOR_SECRET + Integer.toString(suffix));
    }

    private User newUser(int suffix) throws RepositoryException {
        User user = new User();
        setUserFields(user, suffix);
        return user;
    }

    private User addUser(int suffix) throws RepositoryException {
        User user = newUser(suffix);
        userRepository.saveUser(user);
        return user;
    }

}