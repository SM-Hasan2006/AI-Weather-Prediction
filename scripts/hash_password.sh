#!/usr/bin/env bash
# Prints a BCrypt hash for ADMIN_PASSWORD_BCRYPT. Usage: bash scripts/hash_password.sh 'your-strong-password'
set -euo pipefail
[ $# -eq 1 ] || { echo "Usage: $0 'password'"; exit 1; }
htpasswd -bnBC 10 "" "$1" | tr -d ':\n' | sed 's/^\$2y/$2a/'; echo
