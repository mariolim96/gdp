# GDP Database Dockerization

The database for the **GDP — Giornali del Piemonte** project has been dockerized using PostgreSQL 15, as specified in the technical requirements.

## Files Created

- `docker-compose.yml`: Defines the PostgreSQL service, environment variables, and volumes.
- `db/scripts/init.sql`: The initialization script (base on your DDL) that runs automatically on first container startup.

## Database Configuration

| Configuration | Value |
|---------------|-------|
| **Image** | `postgres:15-alpine` |
| **Database Name** | `gdp_db` |
| **Username** | `gdp_user` |
| **Password** | `gdp_password` |
| **Port Mapping** | `5432:5432` |

## How to Run

1.  Ensure you have Docker and Docker Compose installed.
2.  Open a terminal in the project root directory.
3.  Run the following command:

    ```bash
    docker compose up -d
    ```

4.  The database will be initialized with the tables, indexes, and seed data from `db/scripts/init.sql`.

## Note on DDL Fixes
I noticed a small typo in the original `GDP--STD-02-V01-DDL.sql` at line 23 (`DROP TABLE IF EXISTS GDP_' CASCADE`) which was missing the table name and semicolon. This has been corrected in `db/scripts/init.sql` to correctly drop the `GDP_MAIL` table.

## Steps to connect in DBeaver:
Open DBeaver and click on the "New Database Connection" icon (the plug with a plus sign).
Select PostgreSQL from the list of database types and click Next.
Fill in the fields using the table above:
Host: localhost
Port: 5432
Database: gdp_db
Username: gdp_user
Password: gdp_password
Click "Test Connection".
Note: If DBeaver asks to download the PostgreSQL drivers, click Download.
Once the test is successful, click Finish.
TIP

Make sure your Docker container is running by executing docker-compose up -d in your terminal before trying to connect.