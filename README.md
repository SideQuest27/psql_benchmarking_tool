# PostgreSQL Benchmarking Tool (Java + pgbench)

This project is a CLI-based Java benchmarking tool for PostgreSQL using pgbench.
It supports interactive input, batch workloads, and stores benchmark results in PostgreSQL.

The instructions below assume:
- Ubuntu / Linux environment
- No GUI
- PostgreSQL 16 running in Docker

---

## 📦 Prerequisites

You need:
- Java 17
- Maven
- Docker
- PostgreSQL 16 (via Docker - no separate installation needed)

> **Note:** This guide uses PostgreSQL entirely within Docker. You do **not** need to install PostgreSQL or pgbench separately on your host system.

---

## ☕ 1. Install Java 17

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven
```

Verify:
```bash
java --version
javac --version
mvn --version
```

Expected:
- openjdk 17.x
- javac 17.x

### 🔧 Configure Java Path (if needed)

If you have multiple Java versions installed or encounter `javac: command not found`, follow these steps:

**Step 1: Check available Java versions**
```bash
sudo update-alternatives --list javac
sudo update-alternatives --list java
```

Choose the first option (Java 17), then set the environment variables:
```bash
export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64
export PATH=$JAVA_HOME/bin:$PATH
```

**Step 2: Verify (this MUST change)**
```bash
which java
which javac
javac --version
java --version
```

Expected output:
- `/usr/lib/jvm/java-17-openjdk-amd64/bin/java`
- `/usr/lib/jvm/java-17-openjdk-amd64/bin/javac`
- javac 17.x
- openjdk 17.x

**Step 3: Make it permanent (recommended)**

Add this to the end of `~/.bashrc`:
```bash
echo 'export JAVA_HOME=/usr/lib/jvm/java-17-openjdk-amd64' >> ~/.bashrc
echo 'export PATH=$JAVA_HOME/bin:$PATH' >> ~/.bashrc
```

Then reload your shell:
```bash
source ~/.bashrc
```

---

## 🐳 2. Install Docker

```bash
sudo apt install -y docker.io
```

Enable and start Docker:
```bash
sudo systemctl start docker
sudo systemctl enable docker
```

Verify:
```bash
docker --version
```

---

## 🐘 3. Run PostgreSQL 16 in Docker

Run PostgreSQL with host network mode:
```bash
sudo docker run --name pg16 --network host \
  -e POSTGRES_PASSWORD=12345 \
  -v pgdata:/var/lib/postgresql/data \
  -d postgres:16
```

> **Note:** Using `--network host` allows PostgreSQL to be accessible on `localhost:5432` directly.

Verify it's running:
```bash
docker ps
```

You should see the `pg16` container in the list.

### 🔁 Restarting the DB (if it stops)

```bash
docker start pg16
```

Enable auto-restart (recommended):
```bash
docker update --restart unless-stopped pg16
```

---

## 🧪 4. Verify PostgreSQL Connectivity

Connect to PostgreSQL using Docker exec:
```bash
sudo docker exec -it pg16 psql -U postgres
```

If you see:
```
postgres=#
```

You're connected.

Exit with:
```
\q
```

---

## 📥 5. Clone the Project

Clone the repository from GitHub:
```bash
git clone https://github.com/SideQuest27/psql_benchmarking_tool.git
cd psql_benchmarking_tool
```

---

## 🛠️ 6. Setup pgbench Wrapper Script

Since we're using PostgreSQL in Docker, create a wrapper script for pgbench:

```bash
nano pgbench
```

Add the following content:
```bash
#!/bin/bash
sudo docker exec -i pg16 pgbench "$@"
```

Save and exit (`Ctrl+X`, then `Y`, then `Enter`).

Make it executable:
```bash
chmod +x pgbench
```

Get the full path (you'll need this for `application.properties`):
```bash
readlink -f pgbench
```

Copy the output path (e.g., `/home/username/psql_benchmarking_tool/pgbench`).

---

## ⚙️ 7. Configure application.properties

Edit the `application.properties` file:
```bash
nano src/main/resources/application.properties
```

Update with these values:
```properties
app.psql_host=localhost
app.psql_port=5432
app.psql_db_name=postgres
app.psql_user=postgres
app.psql_password=12345
app.psql_url=jdbc:postgresql://localhost:5432/postgres

# Use the full path from the readlink command above
app.pgbench_command=/home/username/psql_benchmarking_tool/pgbench

app.pgbench_scale_factor=400
```

> **Important:** Replace `/home/username/psql_benchmarking_tool/pgbench` with the actual path from the `readlink -f pgbench` command.

---

## 📦 8. Build the Project

From the project root (`psql_benchmarking_tool`), compile the project with all dependencies:
```bash
mvn clean compile
```

Expected:
```
BUILD SUCCESS
```

> **Note:** This step downloads all Maven dependencies and compiles your Java code. If you encounter `ClassNotFoundException` later, re-run this command.

---

## ▶️ 9. Run the Application

```bash
mvn exec:java -Dexec.mainClass="org.example.Main"
```

You'll be prompted for:
- batch mode
- reuse pgbench commands
- clients
- jobs
- runtime
- workload type
- protocol

---

## 🚨 Common Errors & Fixes

### ❌ ClassNotFoundException

Run:
```bash
mvn clean compile
```

Then:
```bash
mvn exec:java -Dexec.mainClass="org.example.Main"
```

### ❌ password authentication failed for user

Make sure:
- Docker container is running: `docker ps`
- Password is `12345`
- Port is `5432`
- User is `postgres`

### ❌ Connection refused or Cannot connect to database

Check if the container is running:
```bash
docker ps
```

If not running, start it:
```bash
docker start pg16
```

### ❌ Cannot run program pgbench

Make sure:
1. You created the `pgbench` wrapper script (Section 6)
2. The script is executable: `chmod +x pgbench`
3. The path in `application.properties` matches the output of `readlink -f pgbench`

Verify:
```bash
./pgbench --version
```

Expected:
```
pgbench (PostgreSQL) 16.x
```

---

## 🧹 10. Stop / Remove PostgreSQL (Optional)

Stop:
```bash
docker stop pg16
```

Remove (⚠️ This will delete all data):
```bash
docker rm pg16
```

Remove the volume (⚠️ This will permanently delete all database data):
```bash
docker volume rm pgdata
```

---

## ✅ Final Checklist

- ✔ Java 17
- ✔ Maven
- ✔ Docker
- ✔ PostgreSQL 16 running in Docker on port 5432
- ✔ pgbench wrapper script created and executable
- ✔ application.properties configured with correct paths
- ✔ `mvn exec:java` works

---

## 🚀 You're Done

Your PostgreSQL benchmarking tool is now fully operational in:
- GitHub Codespaces
- Ubuntu servers
- Remote CLI environments
