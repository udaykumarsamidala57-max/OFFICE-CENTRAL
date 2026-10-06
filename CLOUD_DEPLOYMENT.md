# Cloud deployment

Build the container from the project root with `docker build -t officecentral .` and deploy the image to your cloud provider. The Docker build packages the app with Maven while skipping tests, so it does not need database credentials during image creation.

Configure these environment variables in the provider's secret/environment settings:

- `DB_HOST`: optional override; defaults to the configured MySQL hostname (host only, no JDBC scheme)
- `DB_PORT`: optional override; defaults to the configured MySQL port
- `DB_USER`: optional override; defaults to the configured MySQL username
- `DB_PASSWORD`: MySQL password (required; store as a cloud secret)
- `PORT`: HTTP port assigned by the provider (defaults to 8080)
- `SESSION_COOKIE_SECURE`: set to `true` when HTTPS terminates at the cloud proxy

The application connects to the existing company databases (`inventory`, `SANPOLY_INVENTORY`, `SANPOLY_INVENTORY2`) through `DBUtil1`. Ensure the configured database user can reach and access all three databases. Keep database credentials out of source control and container build arguments.

The database account, network allowlist, and required application tables must be configured in the managed MySQL service before the app is deployed.


If your cloud provider builds from source instead of using Docker, use mvn clean package. Do not pass pom.xml as a Maven profile (-P pom.xml); Maven profiles are declared by the project and pom.xml is the project descriptor, not a profile name.


