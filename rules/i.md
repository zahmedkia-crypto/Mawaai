# roles/defaults/main.yml
# Auto-generated role skeleton after scanning the project structure
# Feel free to adjust names, paths and variables to match your conventions

# Role: common
# Applies to: all hosts
# Purpose: baseline configuration, packages, users, timezone, NTP, firewall baseline
common_packages:
  - curl
  - git
  - htop
  - vim
common_timezone: "UTC"
common_ntp_enabled: true
common_ntp_servers:
  - 0.pool.ntp.org
  - 1.pool.ntp.org
common_firewall_allow:
  - 22/tcp   # SSH
  - 80/tcp   # HTTP
  - 443/tcp  # HTTPS

# Role: docker
# Applies to: hosts in group docker_hosts
# Purpose: install & configure Docker, compose plugin, daemon.json, registries
docker_edition: "ce"
docker_version: "latest"
docker_compose_version: "v2.20.2"
docker_users: ["{{ ansible_user }}", "deploy"]
docker_insecure_registries: []
docker_registry_mirrors: []
docker_log_driver: "json-file"
docker_log_max_size: "100m"
docker_log_max_file: "3"

# Role: nginx
# Applies to: hosts in group webservers
# Purpose: install nginx, manage vhosts, SSL via certbot, rate-limiting, caching
nginx_remove_default_vhost: true
nginx_vhosts:
  - listen: "80"
    server_name: "{{ inventory_hostname }}"
    root: "/var/www/{{ inventory_hostname }}"
    index: "index.html"
    extra_parameters: |
      location / {
        try_files $uri $uri/ =404;
      }
nginx_ssl_enabled: true
nginx_certbot_email: "admin@example.com"
nginx_rate_limit_zone: "$binary_remote_addr"
nginx_rate_limit_rate: "10r/s"

# Role: postgresql
# Applies to: hosts in group db_primary, db_replica
# Purpose: install PostgreSQL, create databases/users, configure replication
postgresql_version: "15"
postgresql_databases:
  - name: app_prod
    owner: app_user
postgresql_users:
  - name: app_user
    password: "{{ vault_app_db_password }}"
postgresql_replication_enabled: "{{ 'db_replica' in group_names }}"
postgresql_replication_user: replicator
postgresql_replication_password: "{{ vault_pg_repl_password }}"

# Role: redis
# Applies to: hosts in group cache
# Purpose: install Redis, configure persistence, memory limits, auth
redis_version: "7"
redis_port: 6379
redis_bind: "0.0.0.0"
redis_password: "{{ vault_redis_password }}"
redis_maxmemory: "256mb"
redis_maxmemory_policy: "allkeys-lru"
redis_save:
  - "900 1"
  - "300 10"
  - "60 10000"

# Role: monitoring
# Applies to: hosts in group monitoring
# Purpose: node_exporter, prometheus, grafana stack, alerts
node_exporter_version: "1.6.1"
prometheus_version: "2.45.0"
grafana_version: "10.0.3"
prometheus_scrape_jobs:
  - job_name: "node"
    static_configs:
      - targets: "{{ groups['all'] | map('extract', hostvars, ['ansible_default_ipv4', 'address']) | list }}"
grafana_admin_password: "{{ vault_grafana_admin_password }}"

# Role: backup
# Applies to: hosts in group db_primary
# Purpose: pg_basebackup + wal-g to S3, retention policies
backup_s3_bucket: "my-project-backups"
backup_s3_region: "us-east-1"
backup_s3_access_key: "{{ vault_backup_s3_access_key }}"
backup_s3_secret_key: "{{ vault_backup_s3_secret_key }}"
backup_retention_days: 30
backup_cron_hour: "2"
backup_cron_minute: "30"

# Role: app
# Applies to: hosts in group app
# Purpose: deploy application code, systemd service, env vars, healthcheck
app_repo_url: "https://github.com/your-org/your-app.git"
app_repo_version: "main"
app_user: "app"
app_group: "app"
app_root: "/opt/app"
app_env:
  NODE_ENV: "production"
  DATABASE_URL: "postgres://app_user:{{ vault_app_db_password }}@{{ hostvars[groups['db_primary'][0]]['ansible_default_ipv4']['address'] }}:5432/app_prod"
  REDIS_URL: "redis://:{{ vault_redis_password }}@{{ hostvars[groups['cache'][0]]['ansible_default_ipv4']['address'] }}:6379/0"
app_port: 3000
app_healthcheck_path: "/health"
app_systemd_restart_policy: "always"
