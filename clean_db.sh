#!/bin/bash
# Quizzi — Delete all data from the database
# Usage: ./clean_db.sh
#
# Truncates every table in the quizzi database (preserves schema).
# Uses the same credentials as DBUtil.java.

set -e

DB_HOST="localhost"
DB_PORT="3306"
DB_NAME="quizzi"
DB_USER="root"
DB_PASS="quizzi123"

echo "This will DELETE ALL DATA from the '$DB_NAME' database."
read -rp "Are you sure? (y/N): " confirm
if [[ "$confirm" != "y" && "$confirm" != "Y" ]]; then
    echo "Aborted."
    exit 0
fi

mysql -h "$DB_HOST" -P "$DB_PORT" -u "$DB_USER" -p"$DB_PASS" "$DB_NAME" <<'SQL'
SET FOREIGN_KEY_CHECKS = 0;

TRUNCATE TABLE responses;
TRUNCATE TABLE players;
TRUNCATE TABLE game_sessions;
TRUNCATE TABLE questions;
TRUNCATE TABLE quizzes;
TRUNCATE TABLE users;

SET FOREIGN_KEY_CHECKS = 1;
SQL

echo "Done — all tables in '$DB_NAME' have been cleaned."
