#!/bin/bash
set -euo pipefail

# Deploy the unit file to the correct systemd location for locally-managed services
cp /opt/games/games.service /etc/systemd/system/games.service
chown root:root /etc/systemd/system/games.service
chmod 644 /etc/systemd/system/games.service

systemctl daemon-reload
systemctl enable games
systemctl restart games