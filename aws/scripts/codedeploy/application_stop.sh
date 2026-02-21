#!/bin/bash
set -euo pipefail

# Stop the service
systemctl stop games || true