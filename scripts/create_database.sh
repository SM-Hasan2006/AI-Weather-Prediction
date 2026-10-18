#!/usr/bin/env bash
# Creates the MySQL database and an application user. Tables are created by Flyway when the backend starts.
# Usage:  bash scripts/create_database.sh      (uses DB_PASSWORD from .env)
set -euo pipefail
source "$(dirname "$0")/_env.sh"
: "${DB_PASSWORD:?Set DB_PASSWORD in .env first}"
sudo mysql <<SQL
CREATE DATABASE IF NOT EXISTS ai_weather CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${DB_USERNAME:-weather_app}'@'localhost' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON ai_weather.* TO '${DB_USERNAME:-weather_app}'@'localhost';
FLUSH PRIVILEGES;
SQL
echo "Database ai_weather ready."
