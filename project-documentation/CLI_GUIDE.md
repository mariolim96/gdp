# gdporch CLI User Guide

This guide describes how to use the Command Line Interface (CLI) of the `gdporch` orchestrator. The CLI is designed for operators and administrators to trigger manual tasks, manage publications, and perform diagnostics.

## 1. How to run the CLI

The CLI is integrated into the Quarkus application. You can execute it in two ways:

### Using the Compiled Runner (Production)
```bash
java -jar target/quarkus-app/quarkus-run.jar [command] [options]
```

### Using Maven (Development)
```bash
./mvnw quarkus:dev -Dquarkus.args="[command] [options]"
```

---

## 2. Command Reference

### Root Command: `gdporch`
The base command for all operations. Use `--help` to see the available subcommands.
```bash
gdporch --help
```

### `dates`: Expected Dates Management (F01)
Used to calculate and populate the expected publication dates in the database.

| Option | Description | Mandatory |
|--------|-------------|-----------|
| `--generate` | Flag to trigger the generation | Yes |
| `--dataInizio` | Start date in `yyyy-MM-dd` format | Yes |
| `--dataFine` | End date in `yyyy-MM-dd` format | Yes |
| `--idTestata` | ID of a specific Testata to process | No |

**Example:**
```bash
gdporch dates --generate --dataInizio 2025-01-01 --dataFine 2025-12-31
```

---

### `editions`: Publication Management (F05)
Used to manage the life cycle and status of publications.

| Option | Description | Mandatory |
|--------|-------------|-----------|
| `--suspend` | Flag to suspend editions for a period | Yes |
| `--idTestata` | ID of the Testata to suspend | Yes |
| `--dataInizio` | Start of suspension period (`yyyy-MM-dd`) | Yes |
| `--dataFine` | End of suspension period (`yyyy-MM-dd`) | Yes |

**Example:**
```bash
gdporch editions --suspend --idTestata 123 --dataInizio 2025-08-01 --dataFine 2025-08-15
```

---

### `queue`: Transmission Queue Management (F21/F10)
Used to retry failed transmissions or force immediate processing of the pending queue.

| Option | Description | Mandatory |
|--------|-------------|-----------|
| `--retry <ID>` | Re-enqueue a failed transmission (uses `idLog`) | XOR |
| `--flush` | Trigger an immediate flush of the F10 queue | XOR |

**Example:**
```bash
# Retry a failed job
gdporch queue --retry 4567

# Force immediate submission of all 'READY' jobs
gdporch queue --flush
```

---

### `dam`: DAM LIBRA Interface (F20)
Used to query the status of jobs submitted to the DAM.

| Option | Description | Mandatory |
|--------|-------------|-----------|
| `--status <ID>` | Check status of a specific Job ID | Yes |

**Example:**
```bash
gdporch dam --status JOB_ABC_123
```

---

### `ops`: Maintenance and Diagnostics
Operational tools for troubleshooting and system control.

| Option | Description |
|--------|-------------|
| `--polling-enable` | Re-enable the automatic SFTP scan |
| `--polling-disable` | Pause the automatic SFTP scan (runtime only) |
| `--cleanup` | Trigger manual cleanup of staged files (stub) |
| `--health-db` | Verbose database health check |
| `--health-sftp` | Verbose SFTP health check with write test |

**Example:**
```bash
# Disable background jobs temporarily for maintenance
gdporch ops --polling-disable

# Check if the service can still write to SFTP
gdporch ops --health-sftp
```

---

## 3. Important Notes

> [!CAUTION]
> **Polling State Persistence**: The `--polling-disable` command only affects the **current running instance**. If the application is restarted, polling will revert to the default state (ENABLED).

> [!IMPORTANT]
> **Date Formats**: All date parameters must strictly follow the `yyyy-MM-dd` format (e.g., `2025-10-02`).

> [!TIP]
> **Help**: You can add `--help` to any subcommand to see its specific options (e.g., `gdporch dates --help`).
