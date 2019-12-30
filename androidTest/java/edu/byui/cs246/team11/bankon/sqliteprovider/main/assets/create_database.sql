-- Users Table

CREATE TABLE Users (
    userId INTEGER PRIMARY KEY,
    name TEXT,
    userType INTEGER,
    pin TEXT,
    authenticatorSecret TEXT
);

-- UserAccounts Table

CREATE TABLE UserAccounts (
    userId INTEGER REFERENCES Users(userId) ON DELETE CASCADE,
    balance NUMERIC,
    pendingBalance NUMERIC,
    pendingRefresh BOOLEAN,
    CONSTRAINT userId_unique UNIQUE (userId)
);

-- Schedules Table

CREATE TABLE Schedules (
    scheduleId INTEGER PRIMARY KEY,
    startDate TEXT,
    endDate TEXT
);

-- Actions Table

CREATE TABLE Actions (
    actionId INTEGER PRIMARY KEY,
    actionDate TEXT,
    actionStatus INTEGER,
    actionType INTEGER,
    description TEXT,
    userId INTEGER REFERENCES Users(userId) ON DELETE CASCADE,
    amount NUMERIC,
    scheduleId INTEGER REFERENCES Schedules(scheduleId)
);