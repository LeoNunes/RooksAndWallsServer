#!/bin/bash
set -euo pipefail

bash "$(dirname "$0")/sanity_check.sh" "before_install"

# Create a 1 GB swap file if none is active. yum transactions (especially those
# pulling in large dependency trees) can OOM-kill themselves on small instances.
if [[ $(swapon --show | wc -l) -eq 0 ]]; then
  fallocate -l 1G /swapfile
  chmod 600 /swapfile
  mkswap /swapfile
  swapon /swapfile
fi

yum update -y

# Install EPEL repository for certbot
amazon-linux-extras install epel
amazon-linux-extras enable nginx1

# https://docs.aws.amazon.com/corretto/latest/corretto-17-ug/amazon-linux-install.html
yum install -y java-17-amazon-corretto-headless

yum install -y awscli
yum install -y nginx
yum install -y jq
yum install -y gettext
yum install -y certbot

# Create runtime config and ACME challenge directories.
mkdir -p /etc/games /var/www/certbot
chown root:root /etc/games
chmod 755 /etc/games

# Ensure nginx starts automatically on boot.
systemctl enable nginx || true