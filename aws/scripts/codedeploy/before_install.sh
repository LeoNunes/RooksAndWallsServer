#!/bin/bash
set -euo pipefail

# https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/amazon-linux-install.html
yum install -y java-17-amazon-corretto-headless

# Ensure AWS CLI and jq are available for secret fetching in after_install
yum install -y awscli jq

# Create runtime config directory with safe ownership and permissions
mkdir -p /etc/games
chown root:root /etc/games
chmod 755 /etc/games