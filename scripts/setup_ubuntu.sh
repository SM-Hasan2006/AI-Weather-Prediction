#!/usr/bin/env bash
# Installs the tools needed on Ubuntu 22.04 / 24.04. Run:  bash scripts/setup_ubuntu.sh
set -euo pipefail
sudo apt update
sudo apt install -y openjdk-21-jdk maven nodejs npm python3 python3-venv python3-pip mysql-server apache2-utils curl git
echo; echo "Versions installed:"
java -version 2>&1 | head -1; mvn -v | head -1; node -v; npm -v; python3 --version; mysql --version
echo; echo "Expected: Java 21, Maven 3.8+, Node 18+, Python 3.10+, MySQL 8."
