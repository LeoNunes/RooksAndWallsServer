#!/bin/bash
set -euo pipefail

bash "$(dirname "$0")/sanity_check.sh" "application_stop"

# Stop the service
systemctl stop games || true