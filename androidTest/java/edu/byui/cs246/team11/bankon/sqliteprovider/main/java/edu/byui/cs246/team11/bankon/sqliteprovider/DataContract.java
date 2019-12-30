package edu.byui.cs246.team11.bankon.sqliteprovider;

final class DataContract {

    public static final String TAG = DataContract.class.getName();

    private DataContract() {
        // Don't allow construction of this class
    }

    public static class Users {
        public static final String TABLE_NAME = "Users";
        public static final String COLUMN_USER_ID = "userId";
        public static final String COLUMN_NAME = "name";
        public static final String COLUMN_USER_TYPE = "userType";
        public static final String COLUMN_PIN = "pin";
        public static final String COLUMN_AUTHENTICATOR_SECRET = "authenticatorSecret";

        public static String[] COLUMNS_ALL = {
            COLUMN_USER_ID,
            COLUMN_NAME,
            COLUMN_USER_TYPE,
            COLUMN_PIN,
            COLUMN_AUTHENTICATOR_SECRET
        };
    }

    public static class UserAccounts {
        public static final String TABLE_NAME = "UserAccounts";
        public static final String COLUMN_USER_ID = "userId";
        public static final String COLUMN_BALANCE = "balance";
        public static final String COLUMN_PENDING_BALANCE = "pendingBalance";
        public static final String COLUMN_PENDING_REFRESH = "pendingRefresh";

        public static String[] COLUMNS_ALL = {
            COLUMN_USER_ID,
            COLUMN_BALANCE,
            COLUMN_PENDING_BALANCE,
            COLUMN_PENDING_REFRESH
        };
    }

    public static class Schedules {
        public static final String TABLE_NAME = "Schedules";
        public static final String COLUMN_SCHEDULE_ID = "scheduleId";
        public static final String COLUMN_START_DATE = "startDate";
        public static final String COLUMN_END_DATE = "endDate";

        public static String[] COLUMNS_ALL = {
            COLUMN_SCHEDULE_ID,
            COLUMN_START_DATE,
            COLUMN_END_DATE
        };
    }

    public static class Actions {
        public static final String TABLE_NAME = "Actions";
        private static final String SCHEDULE_JOIN = "LEFT JOIN " + Schedules.TABLE_NAME + " ON " + TABLE_NAME + "." + Actions.COLUMN_SCHEDULE_ID + " = " + Schedules.TABLE_NAME + "." + Schedules.COLUMN_SCHEDULE_ID;
        public static final String TABLE_NAME_FOR_QUERIES = TABLE_NAME + " " + SCHEDULE_JOIN;
        public static final String COLUMN_ACTION_ID = "actionId";
        public static final String COLUMN_ACTION_DATE = "actionDate";
        public static final String COLUMN_ACTION_STATUS = "actionStatus";
        public static final String COLUMN_ACTION_TYPE = "actionType";
        public static final String COLUMN_DESCRIPTION = "description";
        public static final String COLUMN_USER_ID = "userId";
        public static final String COLUMN_AMOUNT = "amount";
        public static final String COLUMN_SCHEDULE_ID = "scheduleId";

        public static final int ACTION_STATUS_ASSIGNED = 1;
        public static final int ACTION_STATUS_COMPLETED = 2;
        public static final int ACTION_STATUS_APPROVED = 3;
        public static final int ACTION_STATUS_CANCELLED = 4;

        public static final int ACTION_TYPE_CHORE = 1;
        public static final int ACTION_TYPE_GIFT = 2;
        public static final int ACTION_TYPE_PURCHASE = 3;

        public static String[] COLUMNS_ALL = {
            COLUMN_ACTION_ID,
            COLUMN_ACTION_DATE,
            COLUMN_ACTION_STATUS,
            COLUMN_ACTION_TYPE,
            COLUMN_DESCRIPTION,
            COLUMN_USER_ID,
            COLUMN_AMOUNT,
            TABLE_NAME + "." + COLUMN_SCHEDULE_ID,
            Schedules.COLUMN_START_DATE,
            Schedules.COLUMN_END_DATE
        };
    }
}
