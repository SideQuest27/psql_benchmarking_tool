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
- PostgreSQL 16 (via Docker)
- pgbench (inside the container or host)

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

## 🐘 2. Install PostgreSQL 16 Client (psql & pgbench)

### Add PostgreSQL APT Repository

```bash
sudo apt update
sudo apt install -y curl ca-certificates gnupg
```

Add the PostgreSQL GPG key:
```bash
curl -fsSL https://www.postgresql.org/media/keys/ACCC4CF8.asc \
| sudo gpg --dearmor -o /usr/share/keyrings/postgresql.gpg
```

Add the PostgreSQL repository:
```bash
echo "deb [signed-by=/usr/share/keyrings/postgresql.gpg] \
http://apt.postgresql.org/pub/repos/apt \
$(lsb_release -cs)-pgdg main" \
| sudo tee /etc/apt/sources.list.d/pgdg.list
```

### Install PostgreSQL 16 Client Tools

```bash
sudo apt update
sudo apt install -y postgresql-16 postgresql-client-16
```

This installs:
- `psql` - PostgreSQL interactive terminal
- `pgbench` - PostgreSQL benchmarking tool

Verify installation:
```bash
psql --version
pgbench --version
```

Expected:
```
psql (PostgreSQL) 16.x
pgbench (PostgreSQL) 16.x
```

---

## 🐳 3. Install Docker

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

## 🐘 4. Run PostgreSQL 16 in Docker

Run PostgreSQL once:
```bash
docker run -d \
  --name pg16 \
  -e POSTGRES_DB=benchmark \
  -e POSTGRES_USER=benchuser \
  -e POSTGRES_PASSWORD=benchpass \
  -p 15432:5432 \
  postgres:16
```

Verify it's running:
```bash
docker ps
```

You should see:
```
0.0.0.0:15432->5432/tcp
```

### 🔁 Restarting the DB (if it stops)

```bash
docker start pg16
```

Enable auto-restart (recommended):
```bash
docker update --restart unless-stopped pg16
```

---

## 🧪 5. Verify PostgreSQL Connectivity

```bash
psql -h localhost -p 15432 -U benchuser benchmark
```

Password:
```
benchpass
```

If you see:
```
benchmark=#
```

You're connected.

Exit with:
```
\q
```

---

## 🛠️ 6. pgbench Notes (IMPORTANT)

When PostgreSQL runs in Docker, pgbench **MUST** use TCP.
That means always include `-h localhost`.

❌ **Wrong:**
```bash
pgbench -p 15432 -U benchuser benchmark
```

✅ **Correct:**
```bash
pgbench -h localhost -p 15432 -U benchuser benchmark
```

This applies to:
- pgbench initialization (`-i`)
- pgbench benchmark runs

---

## ⚙️ 7. application.properties

Your file is supported, but paths must be Linux-compatible in Codespaces.

**Example (Docker / Linux):**
```properties
app.psql_host=localhost
app.psql_port=15432
app.psql_db_name=benchmark
app.psql_user=benchuser
app.psql_password=benchpass
app.psql_url=jdbc:postgresql://localhost:15432/benchmark

# pgbench should be available in PATH
app.pgbench_command=pgbench

app.pgbench_scale_factor=400
```

⚠️ **Windows paths like this will NOT work in Codespaces:**
```
C:\Program Files\PostgreSQL\16\bin\pgbench.exe
```

**Use:**
```properties
app.pgbench_command=pgbench
```

---

## 📦 8. Build the Project

From the project root, compile the project with all dependencies:
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
- Docker container is running
- Username/password match Docker env vars
- Port is 15432

### ❌ No such file or directory (.s.PGSQL.xxxx)

You forgot `-h localhost` in pgbench.

### ❌ Cannot run program pgbench

Make sure you completed **Section 2: Install PostgreSQL 16 Client**.

Or verify pgbench is in PATH:
```bash
which pgbench
```

---

## 🧹 10. Stop / Remove PostgreSQL (Optional)

Stop:
```bash
docker stop pg16
```

Remove:
```bash
docker rm pg16
```

---

## ✅ Final Checklist

- ✔ Java 17
- ✔ Maven
- ✔ Docker
- ✔ PostgreSQL 16 running on port 15432
- ✔ pgbench uses `-h localhost`
- ✔ Linux-compatible paths
- ✔ `mvn exec:java` works

---

## 🚀 You're Done

Your PostgreSQL benchmarking tool is now fully operational in:
- GitHub Codespaces
- Ubuntu servers
- Remote CLI environments
