# /etc/nginx/sites-available -> /etc/nginx/sites-enabled

# Redirect all HTTP traffic to HTTPS
server {
    listen 80;
    server_name s3.example.com cdn.example.com s3-console.example.com;
    return 301 https://$host$request_uri;
}

# Cloudflare CF-Connecting-IP request header = original client IP
# Why can't blindly trust CF-Connecting-IP ?
# if an attacker finds the server public IP and bypasses Cloudflare entirely, an attacker can spoof (fake) the CF-Connecting-IP
# the attacker can then bypass rate limiting by rotating the IP address set in the CF-Connecting-ip (DoS attacks)
# Nginx must not blindly trust CF-Connecting-IP from any connection:
# trust CF-Connecting-IP if connection comes from official clouflare servers, otherwise ignore the CF-Connecting-IP header
# set_real_ip_from IP real_ip_header CF-Connecting-IP;
# Bypass of Rate Limiting & DoS Protection
# Cloudflare official servers (https://www.cloudflare.com/ips-v4)
set_real_ip_from 173.245.48.0/20;
set_real_ip_from 103.21.244.0/22;
set_real_ip_from 103.22.200.0/22;
set_real_ip_from 103.31.4.0/22;
set_real_ip_from 141.101.64.0/18;
set_real_ip_from 108.162.192.0/18;
set_real_ip_from 190.93.240.0/20;
set_real_ip_from 188.114.96.0/20;
set_real_ip_from 197.234.240.0/22;
set_real_ip_from 198.41.128.0/17;
set_real_ip_from 162.158.0.0/15;
set_real_ip_from 104.16.0.0/13;
set_real_ip_from 104.24.0.0/14;
set_real_ip_from 172.64.0.0/13;
set_real_ip_from 131.0.72.0/22;
real_ip_header CF-Connecting-IP;

# or use Cloudflare Tunnel (cloudflared)
# if server closes all open ports (port 80, 443, 9000, and 9001 ...) => cannot physically connect to the server from the internet
# server can then establish an outbound tunnel directly to cloudflare so it is impossible to bypass cloudflare
# trust CF-Connecting-IP to set_real_ip_from without listing set_real_ip_from

# Connection: upgrade explicitly tells the server to read the Upgrade header to see what protocol to updgrade to.
# if server receives: Connection: upgrade but the Upgrade header is missing or empty
# Some HTTP servers rejects the request with 400 BAD REQUEST or drop the connection

# if Upgrade header is missing (='') then Connection: close (eg: client sends standard HTTP request)
# if Upgrade header is set, then Connection: upgrade (eg: client wants a websocket connection and sends Upgrade: websocket)
map $http_upgrade $connection_upgrade {
    default upgrade;
    ''      close;
}

# connection pool: Instead of opening/closing a TCP socket for every single request
# Nginx caches open TCP connections for reuse (up to 32 idle cached connections per worker, No limit on active TCP connections)
upstream rustfs_api {
    server 127.0.0.1:9000;
    keepalive 32;
}

# S3 API
server {
    listen 443 ssl http2;
    server_name s3.example.com;
    ssl_certificate /etc/letsencrypt/live/s3.example.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/s3.example.com/privkey.pem; # managed by Certbot

    # limit max upload size to 100 MB
    # cloudflare limits = 100 MB (100 MB on free plan, up to 500 MB on Entreprise plan)
    client_max_body_size 100M;

 # Disable buffering to allow streaming directly to rustfs
    # By default, Nginx reads the entire file upload into a local buffer/temporary disk file before sending the first byte to rustfs
    # this doubles the disk I/O, fills disk space and causes client upload timeouts
    proxy_buffering off;
    proxy_request_buffering off;

    # Extend timeouts for long-running uploads/downloads
    # idle timeouts = inactivity timer between consecutive network packet transmissions
    # the time Nginx waits to establish connection with rustfs (takes ms, default=60s)
    # proxy_connect_timeout 60s;

    # clouflare response timeout = 100s
    # the time Nginx waits between 2 consecutive packet transmissions in a write operation
    proxy_send_timeout 120s;
    # the time Nginx waits between 2 consecutive packet transmissions in a read operation
    proxy_read_timeout 120s;

    location / {
        proxy_pass http://rustfs_api; # use keepalive pool
        # $host=original domain name request by the client without port
        # $http_host includes port if specified = HOST request header value as it is
        # must use exact HOST value because S3 SDK, AWS CLI signs the request, so if Nginx transforms the request header HOST
        # it will be rejected (403 SignatureDoesNotMatch)
        proxy_set_header Host $http_host;

        proxy_set_header X-Real-IP $remote_addr;

        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        proxy_set_header X-Forwarded-Proto https;

        # Keep alive connection with upstream backend
        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}

# CDN
server {
    listen 443 ssl http2;
    server_name cdn.example.com;
    ssl_certificate /etc/letsencrypt/live/cdn.example.com/fullchain.pem;
    ssl_certificate_key /etc/letsencrypt/live/cdn.example.com/privkey.pem;

    proxy_send_timeout 120s;
    proxy_read_timeout 120s;

    # Only allow read-only GET/HEAD access
    location / {
        limit_except GET HEAD {
            deny all;
        }

        proxy_pass http://rustfs_api; # Forwards /app-public/... to 127.0.0.1:9000
        proxy_set_header Host $http_host;
        proxy_set_header X-Real-IP $remote_addr;
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
        proxy_set_header X-Forwarded-Proto https;

        proxy_http_version 1.1;
        proxy_set_header Connection "";
    }
}

# S3 Console
server {
    listen 443 ssl http2;
    server_name s3-console.example.com;
    ssl_certificate /etc/letsencrypt/live/s3-console.example.com/fullchain.pem; # managed by Certbot
    ssl_certificate_key /etc/letsencrypt/live/s3-console.example.com/privkey.pem; # managed by Certbot

    client_max_body_size 100M;

    # keep idle websocket connection alive for 120s (default=60s)
    # Cloudflare WS idle timeout = 100s
    # so Cloudflare is always the source of truth, never nginx.
    proxy_read_timeout 120s;

    proxy_send_timeout 120s;

    location / {
        proxy_pass http://127.0.0.1:9001;

        # Set Host request header to the original domain name requested by the client (console.s3.example.com)
        # Default=$proxy_host=127.0.0.1:9001
        proxy_set_header Host $host;

        # Set X-Real-IP request header to the client IP address
        # Default=None => every request looks like it orginated from 127.0.0.1 (the reverse proxy itself)
        proxy_set_header X-Real-IP $remote_addr;

        # Set X-Forwarded-For request header by appending the client's IP to the chain of proxy IPs
        # eg: a request trace = Client -> Cloudflare -> Nginx, X-Forwarded-For = client IP, cloudflare IP
        # Since real_ip_header is set to CF-Connecting-IP, $remote_addr is already the real client IP:
        # X-Forwarded-For = client IP, client IP (X-Forwarded-For=Client IP, $remote_addr)
        proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;

        # Set X-Forwarded-Proto header to https
        # Locally on the server, the connection between Nginx and S3(rustfs) is plain HTTP
        # rustfs believe requests are unsecure and generates "http://" links
        # so this explicitly set that the client-server connection is over HTTPS
        proxy_set_header X-Forwarded-Proto https;

        # WebSocket support
        proxy_http_version 1.1;
        proxy_set_header Upgrade $http_upgrade;
        proxy_set_header Connection $connection_upgrade;
    }
}