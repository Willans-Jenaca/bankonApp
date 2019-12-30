package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.ActionStatus;
import edu.byui.cs246.team11.bankon.core.ActionType;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.Action;
import edu.byui.cs246.team11.bankon.core.User;
import edu.byui.cs246.team11.bankon.core.UserAccount;
import edu.byui.cs246.team11.bankon.core.UserAccountRepository;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import static edu.byui.cs246.team11.bankon.sqliteprovider.TestDataObjectUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SQLiteUserAccountRepositoryTests {
    private DatabaseHelper databaseHelper;
    private ActionRepository actionRepository;
    private UserAccountRepository userAccountRepository;
    private UserRepository userRepository;
    private User user1;
    private User user2;

    // Setup/Teardown Routines

    @Before
    public void setup() throws RepositoryException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        databaseHelper = new DatabaseHelper(context);
        userAccountRepository = new SQLiteUserAccountRepository(databaseHelper);
        actionRepository = new SQLiteActionRepository(databaseHelper);
        userRepository = new SQLiteUserRepository(databaseHelper);
        user1 = addUser(userRepository);
        user2 = addUser(userRepository);
    }

    @After
    public void tearDown() {
        userRepository = null;
        actionRepository = null;
        userAccountRepository = null;
        databaseHelper.close();
    }

// Tests

    @Test
    public void userAccountDoesNotExist() throws RepositoryException {
        assertUserAccount(user1, 0F, 0F);
    }

    @Test
    public void userAccountNoBalance() throws RepositoryException {
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1);
        assertUserAccount(user1, 0F, 0F);
    }

    @Test
    public void userBalance() throws RepositoryException {
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1);
        addAction(actionRepository, user1, ActionStatus.APPROVED, 1);
        assertUserAccount(user1, 1F, 0F);
    }

    @Test
    public void userMixedBalances() throws RepositoryException {
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1);
        addAction(actionRepository, user1, ActionStatus.COMPLETED, 2);
        addAction(actionRepository, user1, ActionStatus.APPROVED, 3);
        addAction(actionRepository, user1, ActionStatus.CANCELLED, 4);
        assertUserAccount(user1, 3F, 2F);
    }

    @Test
    public void userNegativeAmount() throws RepositoryException {
        addAction(actionRepository, user1, ActionType.PURCHASE, ActionStatus.APPROVED, -1);
        addAction(actionRepository, user1, ActionStatus.APPROVED, 3);
        addAction(actionRepository, user1, ActionType.PURCHASE, ActionStatus.COMPLETED, -4);
        addAction(actionRepository, user1, ActionStatus.COMPLETED, 8);
        assertUserAccount(user1, 2F, 4F);
    }

    @Test
    public void changeStatus() throws RepositoryException {
        Action action = addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1);
        assertUserAccount(user1, 0F, 0F);
        changeActionStatus(actionRepository, action, ActionStatus.COMPLETED);
        assertUserAccount(user1, 0F, 1F);
        changeActionStatus(actionRepository, action, ActionStatus.APPROVED);
        assertUserAccount(user1, 1F, 0F);
        changeActionStatus(actionRepository, action, ActionStatus.CANCELLED);
        assertUserAccount(user1, 0F, 0F);
    }

    @Test
    public void changeAmount() throws RepositoryException {
        Action action = addAction(actionRepository, user1, ActionStatus.APPROVED, 1);
        assertUserAccount(user1, 1F, 0F);
        action.setAmount(99);
        actionRepository.saveAction(action);
        assertUserAccount(user1, 99F, 0F);
    }

    @Test
    public void userPendingBalance() throws RepositoryException {
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1);
        addAction(actionRepository, user1, ActionStatus.COMPLETED, 1);
        assertUserAccount(user1, 0F, 1F);
    }

    // Supporting Methods

    private void assertUserAccount(User user, float expectedBalance, float expectedPendingBalance) throws RepositoryException {
        UserAccount userAccount = userAccountRepository.getUserAccount(user.getUserId());
        assertNotNull("UserAccount is null", userAccount);
        assertEquals("UserAccount.userId", user.getUserId(), userAccount.getUserId());
        assertEquals("UserAccount.balance", expectedBalance, userAccount.getBalance(), 0F);
        assertEquals("UserAccount.pendingBalance", expectedPendingBalance, userAccount.getPendingBalance(), 0F);
    }

}
