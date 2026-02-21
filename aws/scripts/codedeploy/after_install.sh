#!/bin/bash
set -euo pipefail

# Reloads daemon to load/update service definition
systemctl daemon-reload

# Fail fast with a clear message when required runtime config is absent.
test -f /etc/games/service.env

# Enable the service
systemctl enable games