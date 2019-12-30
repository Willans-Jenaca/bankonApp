package edu.byui.cs246.team11.bankon.sqliteprovider;

import edu.byui.cs246.team11.bankon.core.Action;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.ActionStatus;
import edu.byui.cs246.team11.bankon.core.ActionType;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.Schedule;
import edu.byui.cs246.team11.bankon.core.User;
import edu.byui.cs246.team11.bankon.core.UserRepository;
import edu.byui.cs246.team11.bankon.core.UserType;
import java.util.Calendar;

final class TestDataObjectUtils {

    private TestDataObjectUtils() { }

    static Action addAction(ActionRepository repo, User user, ActionStatus actionStatus, float amount) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionStatus(actionStatus);
        action.setAmount(amount);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user, Calendar actionDate) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionDate(actionDate);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user, Calendar actionDate, ActionStatus actionStatus) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionDate(actionDate);
        action.setActionStatus(actionStatus);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user, ActionType actionType) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionType(actionType);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user, ActionType actionType, ActionStatus actionStatus, float amount) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionType(actionType);
        action.setActionStatus(actionStatus);
        action.setAmount(amount);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user, ActionType actionType, Calendar actionDate) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setActionStatus(ActionStatus.APPROVED);
        action.setActionType(actionType);
        action.setActionDate(actionDate);
        repo.saveAction(action);
        return action;
    }

    static Action addAction(ActionRepository repo, User user) throws RepositoryException {
        return addAction(repo, user, ActionStatus.ASSIGNED, 1F);
    }

    static Action addAction(ActionRepository repo, User user, String description) throws RepositoryException {
        Action action = newDefaultAction(user);
        action.setDescription(description);
        repo.saveAction(action);
        return action;
    }

    static User addUser(UserRepository userRepository) throws RepositoryException {
        User user  = newDefaultUser();
        userRepository.saveUser(user);
        return user;
    }

    static void changeActionStatus(ActionRepository actionRepository, Action action, ActionStatus actionStatus) throws RepositoryException {
        action.setActionStatus(actionStatus);
        actionRepository.saveAction(action);
    }

    static Action newDefaultAction(User user) {
        Action action = new Action();
        setDefaultActionFields(action, user);
        return action;
    }

    static User newDefaultUser() {
        User user  = new User();
        setDefaultUserFields(user);
        return user;
    }

    static void setActionFields(Action action, User user, Calendar actionDate, ActionStatus actionStatus, ActionType actionType, String description, float amount) {
        action.setActionDate(actionDate);
        action.setActionStatus(actionStatus);
        action.setActionType(actionType);
        action.setDescription(description);
        action.setUserId(user.getUserId());
        action.setAmount(amount);
    }

    static void setDefaultActionFields(Action action, User user) {
        setActionFields(
            action,
            user,
            Calendar.getInstance(),
            ActionStatus.ASSIGNED,
            ActionType.CHORE,
            "Action",
            0f
        );
    }

    static void setScheduleFields(Schedule schedule, Calendar startDate, Calendar endDate) {
        schedule.setStartDate(startDate);
        schedule.setEndDate(endDate);
    }

    static void setDefaultUserFields(User user) {
        user.setName("Name");
        user.setPin("1234");
        user.setUserType(UserType.CHILD);
        user.setAuthenticatorSecret("");
    }

}
