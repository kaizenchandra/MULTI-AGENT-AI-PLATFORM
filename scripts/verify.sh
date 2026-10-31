#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
mvn -B -ntp -f backend/pom.xml verify
npm --prefix frontend ci --no-audit --no-fund
npm --prefix frontend run build
npm --prefix frontend test
docker compose config --quiet
