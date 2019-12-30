package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.Context;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;

import edu.byui.cs246.team11.bankon.core.Action;
import edu.byui.cs246.team11.bankon.core.ActionQuerySpecification;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.ActionStatus;
import edu.byui.cs246.team11.bankon.core.ActionType;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.RepositoryException.ObjectNotFoundException;
import edu.byui.cs246.team11.bankon.core.Schedule;
import edu.byui.cs246.team11.bankon.core.User;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static edu.byui.cs246.team11.bankon.sqliteprovider.TestDataObjectUtils.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class SQLiteActionRepositoryTests {
    private DatabaseHelper databaseHelper;
    private ActionRepository actionRepository;
    private UserRepository userRepository;
    private User user1;
    private User user2;

    // Setup/Teardown Routines

    @Before
    public void setup() throws RepositoryException {
        Context context = InstrumentationRegistry.getTargetContext();
        context.deleteDatabase(DatabaseHelper.DATABASE_NAME);
        databaseHelper = new DatabaseHelper(context);
        actionRepository = new SQLiteActionRepository(databaseHelper);
        userRepository = new SQLiteUserRepository(databaseHelper);
        user1 = addUser(userRepository);
        user2 = addUser(userRepository);
    }

    @After
    public void tearDown() {
        actionRepository = null;
        databaseHelper.close();
    }

// Tests

    @Test
    public void addActionDefault() throws RepositoryException {
        Action action = newDefaultAction(user1);
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 1, action.getActionId());

        Action retrievedAction = actionRepository.getAction(1);
        assertActionFields(action, retrievedAction);
    }

    @Test
    public void addActionWithSchedule() throws RepositoryException {
        Calendar startDate = Calendar.getInstance();
        Calendar endDate = Calendar.getInstance();
        endDate.add(Calendar.MONTH, 1);

        Action action = newDefaultAction(user1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), startDate, endDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 1, action.getActionId());
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        Action retrievedAction = actionRepository.getAction(1);
        assertActionFields(action, retrievedAction);
    }

    @Test
    public void copyActionWithSchedule() throws RepositoryException {
        Calendar scheduleStartDate = Calendar.getInstance();
        Calendar scheduleEndDate = Calendar.getInstance();
        scheduleEndDate.add(Calendar.DATE, 14);

        Action action = newDefaultAction(user1);
        action.setActionStatus(ActionStatus.COMPLETED);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), scheduleStartDate, scheduleEndDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 1, action.getActionId());
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        Action cloneAction = action.clone();
        cloneAction.setActionId(0);
        cloneAction.getActionDate().add(Calendar.DATE, 7);
        actionRepository.saveAction(cloneAction);
        assertEquals("Saved Action ID", 2, cloneAction.getActionId());
        assertEquals("Saved Schedule ID", 1, cloneAction.getSchedule().getScheduleId());

        actionRepository.saveAction(action);

        Action retrievedAction = actionRepository.getAction(1);
        assertNull("Retrieved Action Schedule is not null", retrievedAction.getSchedule());

        retrievedAction = actionRepository.getAction(2);
        assertScheduleFields(cloneAction.getSchedule(), retrievedAction.getSchedule());
    }

    @Test
    public void deleteAction() throws RepositoryException {
        Action action = newDefaultAction(user1);
        actionRepository.saveAction(action);

        Action retrievedAction = actionRepository.getAction(1);
        assertActionFields(action, retrievedAction);

        actionRepository.deleteAction(action);

        retrievedAction = actionRepository.getAction(1);
        assertNull("Action is not null", retrievedAction);
    }

    @Test(expected=ObjectNotFoundException.class)
    public void deleteActionDoesNotExist() throws RepositoryException {
        Action action = new Action();
        action.setActionId(123456);
        setDefaultActionFields(action, user1);
        actionRepository.deleteAction(action);
    }

    @Test
    public void getActionDoesNotExist() throws RepositoryException {
        Action action = actionRepository.getAction(123456);
        assertNull("Action is not null", action);
    }

    @Test
    public void getActions() throws RepositoryException {
        List<Action> expectedList = new ArrayList<Action>();
        for(int i = 0; i < 10; i++) {
            Action action = TestDataObjectUtils.addAction(actionRepository, user1);
            expectedList.add(action);
        }

        List<Action> list = actionRepository.getActions(new ActionQuerySpecification());
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByActionStatus() throws RepositoryException {
        Action actionAssigned = addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1.0f);
        Action actionApproved = addAction(actionRepository, user2, ActionStatus.APPROVED, 2.0f);
        Action actionCancelled = addAction(actionRepository, user1, ActionStatus.CANCELLED, 3.0f);
        Action actionCompleted = addAction(actionRepository, user2, ActionStatus.COMPLETED, 4.0f);

        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setActionStatuses(new ActionStatus[] { ActionStatus.ASSIGNED });
        List<Action> list = actionRepository.getActions(spec);
        List<Action> expectedList = Arrays.asList(actionAssigned);
        assertActionFields(expectedList, list);

        spec.setActionStatuses(new ActionStatus[] { ActionStatus.ASSIGNED, ActionStatus.CANCELLED });
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionAssigned, actionCancelled);
        assertActionFields(expectedList, list);

        spec.setActionStatuses(new ActionStatus[] { ActionStatus.COMPLETED, ActionStatus.APPROVED });
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionApproved, actionCompleted);
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByActionType() throws RepositoryException {
        Action actionChore = addAction(actionRepository, user1, ActionType.CHORE);
        Action actionGift = addAction(actionRepository, user2, ActionType.GIFT);

        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setActionTypes(new ActionType[] { ActionType.CHORE });
        List<Action> list = actionRepository.getActions(spec);
        List<Action> expectedList = Arrays.asList(actionChore);
        assertActionFields(expectedList, list);

        spec.setActionTypes(new ActionType[] { ActionType.CHORE, ActionType.GIFT});
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionChore, actionGift);
        assertActionFields(expectedList, list);

        spec.setActionTypes(new ActionType[] { ActionType.PURCHASE });
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList();
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByDateRange() throws RepositoryException {
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MONTH, -1);
        Action actionAssignedLastMonth = addAction(actionRepository, user1, calendar, ActionStatus.ASSIGNED);
        Action actionCompletedLastMonth = addAction(actionRepository, user1, calendar, ActionStatus.COMPLETED);
        Action actionApprovedLastMonth = addAction(actionRepository, user1, calendar, ActionStatus.APPROVED);
        Action giftLastMonth = addAction(actionRepository, user1, ActionType.GIFT, calendar);
        Action purchaseLastMonth = addAction(actionRepository, user1, ActionType.PURCHASE, calendar);
        calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, 0);
        Action actionToday = addAction(actionRepository, user2, calendar);
        calendar = Calendar.getInstance();
        calendar.add(Calendar.DATE, 2);
        Action actionThisWeek = addAction(actionRepository, user2, calendar);
        calendar.add(Calendar.DATE, 9);
        Action actionNextWeek = addAction(actionRepository, user1, calendar);

        // Query this week
        calendar = calendar.getInstance();
        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setStartDate(calendar);
        calendar.add(Calendar.DATE, 7);
        spec.setEndDate(calendar);
        List<Action> list = actionRepository.getActions(spec);
        List<Action> expectedList = Arrays.asList(actionToday, actionThisWeek);
        assertActionFields(expectedList, list);

        // Query next week
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        calendar.add(Calendar.DATE, 7);
        spec.setStartDate(calendar);
        calendar.add(Calendar.DATE, 14);
        spec.setEndDate(calendar);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionNextWeek);
        assertActionFields(expectedList, list);

        // Query this week or next week
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        spec.setStartDate(calendar);
        calendar.add(Calendar.DATE, 14);
        spec.setEndDate(calendar);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionToday, actionThisWeek, actionNextWeek);
        assertActionFields(expectedList, list);

        // Query future including past due
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        spec.setStartDate(calendar);
        spec.setIncludePastDue(true);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionAssignedLastMonth, actionToday, actionThisWeek, actionNextWeek);
        assertActionFields(expectedList, list);

        // Query next month
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        calendar.add(Calendar.MONTH, 1);
        spec.setStartDate(calendar);
        calendar.add(Calendar.MONTH, 2);
        spec.setEndDate(calendar);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList();
        assertActionFields(expectedList, list);

        // Query with Start Date only
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        calendar.add(Calendar.DATE, 7);
        spec.setStartDate(calendar);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionNextWeek);
        assertActionFields(expectedList, list);

        // Query with End Date only
        spec = new ActionQuerySpecification();
        calendar = calendar.getInstance();
        spec.setEndDate(calendar);
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionAssignedLastMonth, actionCompletedLastMonth, actionApprovedLastMonth, giftLastMonth, purchaseLastMonth, actionToday);
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByDescription() throws RepositoryException {
        Action actionDoDishes = addAction(actionRepository, user1, "Do the dishes");
        Action actionCleanRoom = addAction(actionRepository, user1, "Clean your room");
        Action actionDustRoom = addAction(actionRepository, user1, "Dust your room");
        Action actionDryDishes = addAction(actionRepository, user1, "Dry the dishes");

        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setDescription("dishes");
        List<Action> list = actionRepository.getActions(spec);
        List<Action> expectedList = Arrays.asList(actionDoDishes, actionDryDishes);
        assertActionFields(expectedList, list);

        spec = new ActionQuerySpecification();
        spec.setDescription("Your Room");
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList(actionCleanRoom, actionDustRoom);
        assertActionFields(expectedList, list);

        spec = new ActionQuerySpecification();
        spec.setDescription("fishes");
        list = actionRepository.getActions(spec);
        expectedList = Arrays.asList();
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByScheduledOnly() throws RepositoryException {
        addAction(actionRepository, user1);
        Action actionScheduled = newDefaultAction(user1);
        actionScheduled.setSchedule(new Schedule());
        setScheduleFields(actionScheduled.getSchedule(), Calendar.getInstance(), Calendar.getInstance());
        actionRepository.saveAction(actionScheduled);
        addAction(actionRepository, user2);

        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setIncludeScheduledOnly(true);
        List<Action> list = actionRepository.getActions(spec);
        List<Action> expectedList = Arrays.asList(actionScheduled);
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsFilterByUserId() throws RepositoryException {
        List<Action> expectedList = new ArrayList<Action>();
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 1.0f);
        expectedList.add(addAction(actionRepository, user2, ActionStatus.ASSIGNED, 2.0f));
        addAction(actionRepository, user1, ActionStatus.ASSIGNED, 3.0f);
        expectedList.add(addAction(actionRepository, user2, ActionStatus.ASSIGNED, 4.0f));
        ActionQuerySpecification spec = new ActionQuerySpecification();
        spec.setUserId(user2.getUserId());
        List<Action> list = actionRepository.getActions(spec);
        assertActionFields(expectedList, list);
    }

    @Test
    public void getActionsNoneReturned() throws RepositoryException {
        List<Action> list = actionRepository.getActions(new ActionQuerySpecification());
        List<Action> expectedList = new ArrayList<Action>();
        assertActionFields(expectedList, list);
    }

    @Test
    public void removeSchedule() throws RepositoryException {
        Action action = newDefaultAction(user1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), Calendar.getInstance(), Calendar.getInstance());
        actionRepository.saveAction(action);
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        action.setSchedule(null);
        actionRepository.saveAction(action);

        Action retrievedAction = actionRepository.getAction(1);
        assertNull("Schedule is not null", retrievedAction.getSchedule());
    }

    @Test
    public void removeSchedules() throws RepositoryException {
        Action action = newDefaultAction(user1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), Calendar.getInstance(), Calendar.getInstance());
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 1, action.getActionId());
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        action.setActionId(0);
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 2, action.getActionId());
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        action.setSchedule(null);
        actionRepository.saveAction(action);

        Action retrievedAction = actionRepository.getAction(1);
        assertNull("Schedule is not null", retrievedAction.getSchedule());

        retrievedAction = actionRepository.getAction(2);
        assertNull("Schedule is not null", retrievedAction.getSchedule());
    }

    @Test
    public void replaceSchedule() throws RepositoryException {
        Calendar scheduleDate = Calendar.getInstance();
        Action action = newDefaultAction(user1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), scheduleDate, scheduleDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        scheduleDate.add(Calendar.MONTH, 1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), scheduleDate, scheduleDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Schedule ID", 2, action.getSchedule().getScheduleId());

        Action retrievedAction = actionRepository.getAction(1);
        assertScheduleFields(action.getSchedule(), retrievedAction.getSchedule());
    }

    @Test
    public void updateAction() throws RepositoryException {
        Action action = newDefaultAction(user1);
        actionRepository.saveAction(action);
        assertEquals("Saved Action ID", 1, action.getActionId());

        setActionFields(action,
            user1,
            Calendar.getInstance(),
            ActionStatus.COMPLETED,
            ActionType.PURCHASE,
            "Saved Action",
            5f
        );
        action.getActionDate().add(Calendar.MONTH, 1);
        actionRepository.saveAction(action);

        Action retrievedAction = actionRepository.getAction(1);
        assertActionFields(action, retrievedAction);
    }

    @Test(expected=ObjectNotFoundException.class)
    public void updateActionDoesNotExist() throws RepositoryException {
        Action action = newDefaultAction(user1);
        action.setActionId(123456);
        actionRepository.saveAction(action);
    }

    @Test
    public void updateSchedule() throws RepositoryException {
        Calendar scheduleDate = Calendar.getInstance();
        Action action = newDefaultAction(user1);
        action.setSchedule(new Schedule());
        setScheduleFields(action.getSchedule(), scheduleDate, scheduleDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        scheduleDate.add(Calendar.MONTH, 1);
        setScheduleFields(action.getSchedule(), scheduleDate, scheduleDate);
        actionRepository.saveAction(action);
        assertEquals("Saved Schedule ID", 1, action.getSchedule().getScheduleId());

        Action retrievedAction = actionRepository.getAction(1);
        assertScheduleFields(action.getSchedule(), retrievedAction.getSchedule());
    }

    // Supporting Methods

    private void assertActionFields(List<Action> expectedList, List<Action> actualList) {
        assertNotNull("List is null", actualList);
        assertEquals("List.size", expectedList.size(), actualList.size());
        for(int i = 0; i < expectedList.size(); i++) {
            assertActionFields(expectedList.get(i), actualList.get(i));
        }
    }

    private void assertActionFields(Action action, int expectedActionId, Calendar expectedActionDate, ActionStatus expectedActionStatus, ActionType expectedActionType, String expectedDescription, int expectedUserId, float expectedAmount) {
        assertNotNull("Action is null", action);
        assertEquals("Action.ActionID", expectedActionId, action.getActionId());
        assertEquals("Action.ActionDate", expectedActionDate, action.getActionDate());
        assertEquals("Action.ActionStatus", expectedActionStatus, action.getActionStatus());
        assertEquals("Action.ActionType", expectedActionType, action.getActionType());
        assertEquals("Action.Description", expectedDescription, action.getDescription());
        assertEquals("Action.UserID", expectedUserId, action.getUserId());
        assertEquals("Action.Amount", expectedAmount, action.getAmount(), 0F);
    }

    private void assertActionFields(Action expectedAction, Action action) {
        assertActionFields(action, expectedAction.getActionId(), expectedAction.getActionDate(), expectedAction.getActionStatus(), expectedAction.getActionType(), expectedAction.getDescription(), expectedAction.getUserId(), expectedAction.getAmount());
        assertScheduleFields(expectedAction.getSchedule(), action.getSchedule());
    }

    private void assertScheduleFields(Schedule schedule, int expectedScheduleId, Calendar expectedStartDate, Calendar expectedEndDate) {
        assertNotNull("Schedule is null", schedule);
        assertEquals("Schedule.ScheduleID", expectedScheduleId, schedule.getScheduleId());
        assertEquals("Schedule.StartDate", expectedStartDate, schedule.getStartDate());
        assertEquals("Schedule.EndDate", expectedEndDate, schedule.getEndDate());
    }

    private void assertScheduleFields(Schedule expectedSchedule, Schedule actualSchedule) {
        if (expectedSchedule == null) {
            assertNull("Schedule", actualSchedule);
        } else {
            assertScheduleFields(actualSchedule, expectedSchedule.getScheduleId(), expectedSchedule.getStartDate(), expectedSchedule.getEndDate());
        }
    }
}
