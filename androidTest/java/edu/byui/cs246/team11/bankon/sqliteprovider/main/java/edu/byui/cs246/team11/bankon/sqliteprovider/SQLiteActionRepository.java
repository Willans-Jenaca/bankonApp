package edu.byui.cs246.team11.bankon.sqliteprovider;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import edu.byui.cs246.team11.bankon.core.Action;
import edu.byui.cs246.team11.bankon.core.ActionQuerySpecification;
import edu.byui.cs246.team11.bankon.core.ActionRepository;
import edu.byui.cs246.team11.bankon.core.ActionStatus;
import edu.byui.cs246.team11.bankon.core.ActionType;
import edu.byui.cs246.team11.bankon.core.RepositoryException;
import edu.byui.cs246.team11.bankon.core.RepositoryException.ObjectNotFoundException;
import edu.byui.cs246.team11.bankon.core.Schedule;
import edu.byui.cs246.team11.bankon.core.Utils;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.Actions;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.Schedules;
import edu.byui.cs246.team11.bankon.sqliteprovider.DataContract.UserAccounts;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

/**
 * Implementation of the ActionRepository interface using SQLite.
 */
public class SQLiteActionRepository extends BaseRepository implements ActionRepository {

    public static final String TAG = SQLiteActionRepository.class.getName();

    public SQLiteActionRepository(DatabaseHelper databaseHelper) {
        super(databaseHelper);
    }

    private int actionStatusToInt(ActionStatus actionStatus) throws RepositoryException {
        switch (actionStatus) {
            case ASSIGNED: return Actions.ACTION_STATUS_ASSIGNED;
            case COMPLETED: return Actions.ACTION_STATUS_COMPLETED;
            case APPROVED: return Actions.ACTION_STATUS_APPROVED;
            case CANCELLED: return Actions.ACTION_STATUS_CANCELLED;
        }
        throw new RepositoryException("Unhandled Action Status");
    }

    private ActionStatus intToActionStatus(int value) throws RepositoryException {
        switch (value) {
            case Actions.ACTION_STATUS_ASSIGNED: return ActionStatus.ASSIGNED;
            case Actions.ACTION_STATUS_COMPLETED: return ActionStatus.COMPLETED;
            case Actions.ACTION_STATUS_APPROVED: return ActionStatus.APPROVED;
            case Actions.ACTION_STATUS_CANCELLED: return ActionStatus.CANCELLED;
        }
        throw new RepositoryException("Unhandled Action Status");
    }

    private int actionTypeToInt(ActionType actionType) throws RepositoryException {
        switch (actionType) {
            case CHORE: return Actions.ACTION_TYPE_CHORE;
            case GIFT: return Actions.ACTION_TYPE_GIFT;
            case PURCHASE: return Actions.ACTION_TYPE_PURCHASE;
        }
        throw new RepositoryException("Unhandled Action Type");
    }

    private ActionType intToActionType(int value) throws RepositoryException {
        switch (value) {
            case Actions.ACTION_TYPE_CHORE: return ActionType.CHORE;
            case Actions.ACTION_TYPE_GIFT: return ActionType.GIFT;
            case Actions.ACTION_TYPE_PURCHASE: return ActionType.PURCHASE;
        }
        throw new RepositoryException("Unhandled Action Type");
    }

    /**
     * Checks the pending changes to an Action to determine if the user's account should be refreshed.
     */
    private void checkAccountBalance(SQLiteDatabase db, Action action, Action previousAction) {
        boolean needsRefresh = (previousAction == null)
            || (action.getActionStatus() != previousAction.getActionStatus())
            || (action.getAmount() != previousAction.getAmount());
        if (!needsRefresh) return;

        String actionsWhere = UserAccounts.COLUMN_USER_ID + " = ?";
        String[] actionsWhereArgs = { Integer.toString(action.getUserId()) };

        ContentValues values = new ContentValues();
        values.put(UserAccounts.COLUMN_PENDING_REFRESH, 1);

        db.update(
            UserAccounts.TABLE_NAME,
            values,
            actionsWhere,
            actionsWhereArgs
        );
    }

    @Override
    public Action getAction(int actionId) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try {
            return internalGetAction(db, actionId);
        } finally {
            db.close();
        }
    }

    private Action internalGetAction(SQLiteDatabase db, int actionId) throws RepositoryException {
        if (actionId == 0) return null;
        String where = Actions.COLUMN_ACTION_ID + " = ?";
        String[] whereArgs = { Integer.toString(actionId) };
        Cursor cursor = db.query(
            Actions.TABLE_NAME_FOR_QUERIES,
            Actions.COLUMNS_ALL,
            where,
            whereArgs,
            null,
            null,
            null
        );
        try {
            return new ActionLoader().ObjectFromCursor(cursor);
        } finally {
            cursor.close();
        }
    }

    @Override
    public List<Action> getActions(ActionQuerySpecification spec) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getReadableDatabase();
        try {
            String where = "";
            ArrayList<String> whereArgs = new ArrayList<String>();
            String orderBy = Actions.COLUMN_ACTION_DATE;

            // Filter by ActionStatuses
            ActionStatus[] actionStatuses = spec.getActionStatuses();
            if (actionStatuses.length > 0) {
                if (where != "") where += " AND ";
                where += "(";
                for (int i = 0; i < actionStatuses.length; i++) {
                    if (i > 0) where += " OR ";
                    where += Actions.COLUMN_ACTION_STATUS + " = ?";
                    whereArgs.add(Integer.toString(actionStatusToInt(actionStatuses[i])));
                }
                where += ")";
            }

            // Filter by ActionTypes
            ActionType[] actionTypes = spec.getActionTypes();
            if (actionTypes.length > 0) {
                if (where != "") where += " AND ";
                where += "(";
                for (int i = 0; i < actionTypes.length; i++) {
                    if (i > 0) where += " OR ";
                    where += Actions.COLUMN_ACTION_TYPE + " = ?";
                    whereArgs.add(Integer.toString(actionTypeToInt(actionTypes[i])));
                }
                where += ")";
            }

            // Filter by ActionTypes
            Calendar startDate = (spec.getStartDate() != null ? (Calendar)spec.getStartDate().clone() : null);
            Calendar endDate = (spec.getEndDate() != null ? (Calendar)spec.getEndDate().clone() : null);
            if (startDate != null || endDate != null) {
                if (where != "") where += " AND ";
                String innerWhere = "(";
                if (startDate != null) {
                    Utils.setCalendarToStartOfDay(startDate);
                    innerWhere += Actions.COLUMN_ACTION_DATE + " >= ?";
                    whereArgs.add(calendarToString(startDate));
                }

                if (endDate != null) {
                    Utils.setCalendarToEndOfDay(endDate);
                    if (innerWhere != "(") innerWhere += " AND ";
                    innerWhere += Actions.COLUMN_ACTION_DATE + " <= ?";
                    whereArgs.add(calendarToString(endDate));
                }

                innerWhere += ")";

                // Include Past Due (Date < Now, ActionStatus=ASSIGNED)
                if (spec.getIncludePastDue()) {
                    Calendar startOfToday = Calendar.getInstance();
                    Utils.setCalendarToStartOfDay(startOfToday);
                    String pastDueWhere =
                        Actions.COLUMN_ACTION_STATUS + " = " + Actions.ACTION_STATUS_ASSIGNED +
                            " AND " + Actions.COLUMN_ACTION_DATE + " < ?";
                    innerWhere = "(" + innerWhere + " OR (" + pastDueWhere + "))";
                    whereArgs.add(calendarToString(startOfToday));
                }
                where += innerWhere;
            }

            // Filter by Description
            if (!spec.getDescription().isEmpty() ) {
                if (where != "") where += " AND ";
                where += "lower(" + Actions.COLUMN_DESCRIPTION + ") LIKE '%' || ? || '%'";
                whereArgs.add(spec.getDescription().toLowerCase());
            }

            // Filter by ScheduleId
            if (spec.getIncludeScheduledOnly() ) {
                if (where != "") where += " AND ";
                where += Actions.TABLE_NAME + "." + Actions.COLUMN_SCHEDULE_ID + " > 0";
            }

            // Filter by UserId
            if (spec.getUserId() > 0) {
                if (where != "") where += " AND ";
                where += Actions.COLUMN_USER_ID + " = ?";
                whereArgs.add(Integer.toString(spec.getUserId()));
            }

            Cursor cursor = db.query(
                Actions.TABLE_NAME_FOR_QUERIES,
                Actions.COLUMNS_ALL,
                where,
                whereArgs.toArray(new String[0]),
                null,
                null,
                orderBy
            );
            try {
                return new ActionLoader().ObjectsFromCursor(cursor);
            } finally {
                cursor.close();
            }
        } finally {
            db.close();
        }
    }

    @Override
    public void deleteAction(Action action) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        try {
            String where = Actions.COLUMN_ACTION_ID + " = ?";
            String[] whereArgs = { Integer.toString(action.getActionId()) };

            int count = db.delete(
                Actions.TABLE_NAME,
                where,
                whereArgs
            );

            if (count == 0) {
                throw new ObjectNotFoundException(Action.class, action.getActionId());
            }
        } finally {
            db.close();
        }
    }

    private void deleteSchedule(SQLiteDatabase db, int scheduleId) throws ObjectNotFoundException {
        if (scheduleId == 0) return;

        String actionsWhere = Actions.COLUMN_SCHEDULE_ID + " = ?";
        String[] actionsWhereArgs = {Integer.toString(scheduleId)};

        ContentValues values = new ContentValues();
        values.putNull(Actions.COLUMN_SCHEDULE_ID);

        db.update(
            Actions.TABLE_NAME,
            values,
            actionsWhere,
            actionsWhereArgs
        );

        String schedulesWhere = Actions.COLUMN_SCHEDULE_ID + " = ?";
        String[] schedulesWhereArgs = { Integer.toString(scheduleId) };

        int count = db.delete(
            Schedules.TABLE_NAME,
            schedulesWhere,
            schedulesWhereArgs
        );

        if (count == 0) {
            throw new ObjectNotFoundException(Schedule.class, scheduleId);
        }
    }

    private void removeScheduleFromActions(SQLiteDatabase db, int scheduleId) {
        String where = Actions.COLUMN_SCHEDULE_ID + " = ?";
        String[] whereArgs = {Integer.toString(scheduleId)};

        ContentValues values = new ContentValues();
        values.putNull(Actions.COLUMN_SCHEDULE_ID);

        db.update(
            Actions.TABLE_NAME,
            values,
            where,
            whereArgs
        );
    }

    @Override
    public void saveAction(Action action) throws RepositoryException {
        SQLiteDatabase db = databaseHelper.getWritableDatabase();
        try {
            db.beginTransaction();
            try {
                Action previousAction = internalGetAction(db, action.getActionId());
                int previousScheduleId = 0;
                if (previousAction != null && previousAction.getSchedule() != null) {
                    previousScheduleId = previousAction.getSchedule().getScheduleId();
                }

                ContentValues values = new ContentValues();
                values.put(Actions.COLUMN_ACTION_DATE, calendarToString(action.getActionDate()));
                values.put(Actions.COLUMN_ACTION_STATUS, actionStatusToInt(action.getActionStatus()));
                values.put(Actions.COLUMN_ACTION_TYPE, actionTypeToInt(action.getActionType()));
                values.put(Actions.COLUMN_DESCRIPTION, action.getDescription());
                values.put(Actions.COLUMN_USER_ID, action.getUserId());
                values.put(Actions.COLUMN_AMOUNT, action.getAmount());

                int currentScheduleId;
                if (action.getSchedule() != null && verifyCanSaveActionSchedule(db, action)) {
                    saveSchedule(db, action.getSchedule());
                    currentScheduleId = action.getSchedule().getScheduleId();
                } else {
                    currentScheduleId = 0;
                }
                values.put(Actions.COLUMN_SCHEDULE_ID, (currentScheduleId != 0 ? currentScheduleId : null));

                if (previousScheduleId != currentScheduleId) {
                    deleteSchedule(db, previousScheduleId);
                }

                if (action.getActionId() == 0) {
                    if (currentScheduleId > 0) {
                        removeScheduleFromActions(db, currentScheduleId);
                    }
                    Long id = db.insertOrThrow(Actions.TABLE_NAME, null, values);
                    action.setActionId(id.intValue());
                } else {
                    String where = Actions.COLUMN_ACTION_ID + " = ?";
                    String[] whereArgs = {Integer.toString(action.getActionId())};

                    int count = db.update(
                        Actions.TABLE_NAME,
                        values,
                        where,
                        whereArgs
                    );

                    if (count == 0) {
                        throw new ObjectNotFoundException(Action.class, action.getActionId());
                    }
                }
                checkAccountBalance(db, action, previousAction);
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            db.close();
        }
    }

    private void saveSchedule(SQLiteDatabase db, Schedule schedule) throws ObjectNotFoundException {
        ContentValues values = new ContentValues();
        values.put(Schedules.COLUMN_START_DATE, calendarToString(schedule.getStartDate()));
        values.put(Schedules.COLUMN_END_DATE, calendarToString(schedule.getEndDate()));

        if (schedule.getScheduleId() == 0) {
            Long id = db.insertOrThrow(Schedules.TABLE_NAME, null, values);
            schedule.setScheduleId(id.intValue());
        } else {
            String where = Schedules.COLUMN_SCHEDULE_ID + " = ?";
            String[] whereArgs = { Integer.toString(schedule.getScheduleId()) };

            int count = db.update(
                Schedules.TABLE_NAME,
                values,
                where,
                whereArgs
            );

            if (count == 0) {
                throw new ObjectNotFoundException(Schedule.class, schedule.getScheduleId());
            }
        }
    }

    private boolean verifyCanSaveActionSchedule(SQLiteDatabase db, Action action) {
        int currentActionId = action.getActionId();
        int scheduleId = action.getSchedule().getScheduleId();
        if (currentActionId == 0 || scheduleId == 0) return true;

        String where = Actions.COLUMN_SCHEDULE_ID + " = ? AND " + Actions.COLUMN_ACTION_ID + " != ?";
        String[] whereArgs = { Integer.toString(scheduleId), Integer.toString(currentActionId) };
        Cursor cursor = db.query(
            Actions.TABLE_NAME,
            new String[] { Actions.COLUMN_ACTION_ID },
            where,
            whereArgs,
            null,
            null,
            null
        );
        try {
            if (cursor.moveToFirst()) {
                int actionIdWithSchedule = cursor.getInt(cursor.getColumnIndex(Actions.COLUMN_ACTION_ID));
                return (actionIdWithSchedule == currentActionId);
            }
            return true;
        } finally {
            cursor.close();
        }
    }

    /**
     * Class to deserialize Schedule objects based on a data cursor.
     */
    private class ScheduleLoader extends ObjectLoader<Schedule> {
        @Override
        protected Schedule FromCursor(Cursor cursor) throws RepositoryException {
            int scheduleId = cursor.getInt(cursor.getColumnIndexOrThrow(Actions.COLUMN_SCHEDULE_ID));
            if (scheduleId == 0) {
                return null;
            }
            Schedule schedule = new Schedule();
            schedule.setScheduleId(scheduleId);
            schedule.setStartDate(stringToCalendar(cursor.getString(cursor.getColumnIndexOrThrow(Schedules.COLUMN_START_DATE))));
            schedule.setEndDate(stringToCalendar(cursor.getString(cursor.getColumnIndexOrThrow(Schedules.COLUMN_END_DATE))));
            return schedule;
        }
    }

    /**
     * Class to deserialize Action objects based on a data cursor including the dependent schedule object.
     */
    private class ActionLoader extends ObjectLoader<Action> {
        private ScheduleLoader scheduleLoader = new ScheduleLoader();

        @Override
        protected Action FromCursor(Cursor cursor) throws RepositoryException {
            Action action = new Action();
            action.setActionId(cursor.getInt(cursor.getColumnIndexOrThrow(Actions.COLUMN_ACTION_ID)));
            action.setActionDate(stringToCalendar(cursor.getString(cursor.getColumnIndexOrThrow(Actions.COLUMN_ACTION_DATE))));
            action.setActionStatus(intToActionStatus(cursor.getInt(cursor.getColumnIndexOrThrow(Actions.COLUMN_ACTION_STATUS))));
            action.setActionType(intToActionType(cursor.getInt(cursor.getColumnIndexOrThrow(Actions.COLUMN_ACTION_TYPE))));
            action.setDescription(cursor.getString(cursor.getColumnIndexOrThrow(Actions.COLUMN_DESCRIPTION)));
            action.setUserId(cursor.getInt(cursor.getColumnIndexOrThrow(Actions.COLUMN_USER_ID)));
            action.setAmount(cursor.getFloat(cursor.getColumnIndexOrThrow(Actions.COLUMN_AMOUNT)));
            action.setSchedule(scheduleLoader.FromCursor(cursor));
            return action;
        }
    }
}
