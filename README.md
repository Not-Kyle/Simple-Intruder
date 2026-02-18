# Simple Intruder V1
![Java Version](https://img.shields.io/badge/Java-22%2B-orange)
![Release](https://img.shields.io/github/v/release/Not-Kyle/Simple-Intruder?color=blue)
![Response Time](https://img.shields.io/badge/Latency-2--4ms-green)

**Simple Intruder** is a high-performance memory safe profile intelligence tool for ROBLOX, utitlizing its current API's to generate comprehesive dossiers on any user.

## Features
* **Deep Dossier Generation:** Extracts full user metadata and history.
* **Blazing Performance:** Optimized network calls with **2ms–4ms** response times.
* **Modern Tech Stack:** Built with Java 22, Jackson for JSON parsing, and Log4j for logging.
  
---

## Setup Guide (Windows Only)

### 1. Install Java 22+
You need **OpenJDK 22** or newer to run this dossier tool.
1. Download the latest version from [JDK.java.net](https://jdk.java.net/25/).
2. Extract the folder to `C:\Program Files\Java\jdk-25`.

### 2. Set Environment Variables
1. Search Windows for **"Edit the system environment variables"** and open it.
2. Click **Environment Variables**.
3. Under **System variables**, click **New**:
   - **Variable Name:** `JAVA_HOME`
   - **Variable Value:** `C:\Program Files\Java\jdk-25`
4. Find **Path** in the list, click **Edit**, click **New**, and type: `%JAVA_HOME%\bin`
5. Click **OK** on everything and restart your terminal.

---

## How to Run
1. Go to the [Releases](https://github.com/Not-Kyle/Simple-Intruder/releases) page.
2. Download the `SimpleIntruderV1.jar`.
3. Open your terminal (CMD) and type `java -jar` followed by a space.
4. Drag the downloaded `.jar` file into the terminal window and press **Enter**.

```bash
java -jar "C:\path\to\your\SimpleIntruderV1.jar"
