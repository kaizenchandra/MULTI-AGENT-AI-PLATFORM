#!/usr/bin/env sh
set -eu
cd "$(dirname "$0")/.."
if [ -e .env ]; then echo '.env already exists; leaving it unchanged.'; exit 0; fi
umask 077
{
 echo "DATABASE_PASSWORD=$(openssl rand -hex 24)"
 echo "LOCAL_USER_TOKEN=$(openssl rand -hex 32)"
 echo "LOCAL_APPROVER_TOKEN=$(openssl rand -hex 32)"
} > .env
echo 'Generated local-only credentials in .env (mode 600). Do not commit this file.'
