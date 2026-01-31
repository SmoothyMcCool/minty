# minty
Modular Intelligence for Next-gen Tasking by You

# Installing, Configuring and Running

> **Note:** The instructions below assume a specific local setup.
> Adjust the paths, IP addresses and other values to match your own environment.

---

## Assumptions

| Item | Description |
|------|-------------|
| **MariaDB** | Runs in its own VM (or locally). |
| **Ollama** | Latest version installed. |
| **Apache Maven** | 3.9, `mvn` in `PATH`. |
| **Java** | 21 + (Java 24 upgrade will be coming soon). |
| **DBeaver** | DB administration tool. |
| **Tomcat** | Use version 11. Installed in `d:\projects\Tomcat`. |
| **Minty code** | Located in `d:\projects\Minty`. |
| **IDE** | Spring Tool Suite (STS) for Java; VS Code for TypeScript. |
| **Mvn4w** | Provides Node.js/NPM environment. |

> If you **don’t** run Tomcat and NPM locally, you’ll need to enable HTTPS in Tomcat and adjust your Angular setup accordingly.
> Without HTTPS, browser calls to the Web Crypto API will fail, breaking Minty functionality.
> See: <https://developer.mozilla.org/en-US/docs/Web/API/Web_Crypto_API>

---

## Tomcat

1. **Download & unzip** Apache Tomcat into `d:\projects\Tomcat`.
2. If you want debugging from Eclipse/STS, edit the startup script:

```bat
:: Tomcat/bin/startup.bat(Windows) or Tomcat/bin/startup.sh (Linux)
...
:doneSetArgs

JPDA_OPTS="-agentlib:jdwp=transport=dt_socket,address=8000,server=y,suspend=n"

call "%EXECUTABLE%" jpda start %CMD_LINE_ARGS%

:end
```

---

## Source Code

Clone or download the Minty source to `d:\projects\Minty`.

---

## Database

I run MariaDB in a Ubuntu VM, but a local installation works fine.

1. Install MariaDB following the instructions in `mariadb install.txt`.
2. If you run MariaDB locally, you won’t need to tweak the database settings for non‑local access.

---

## Minty Development Setup

Create a working folder, e.g.:

```
d:\projects\Minty\working
```

### Importing Projects into STS

1. **Right‑click** the *Package Explorer* → **Import…** → **Existing Maven Projects** → **Next**.
2. Browse to `{minty code location}/webapp/backend/solution`.
3. Select the folder, check *Select All*, then **Finish**.
4. Repeat for `{minty code location}/webapp/backend/parent`.

### Configuring the `bundle` Project

Edit `src/main/resources/application.yaml`:

| Property | Purpose | Example |
|----------|---------|---------|
| `output.directory` | Path to the working folder | `d:/projects/Minty` |
| `db.url` | MariaDB JDBC URL (update IP) | `jdbc:mariadb://localhost:3306/Minty` |
| `db.password` | DB password | `yourPassword` |
| `ollama.uri` | Ollama endpoint | `http://localhost:11434` |
| `ollama.chatmodels` | List of chat models | See application.yaml |
| `ollama.defaultModel` | Default chat model | `gpt-oss:20b` |
| `ollama.conversationNamingModel` | Model for conversation titles | `gemma3:4b` |
| `ollama.embedding.model` | Embedding model | `nomic-embed-text` |
| `secret` | Decryption key for DB | `superSecretKey` |

> If you change `secret`, you must reinstall the database or data will become unreadable.

### Configuring the `parent` Project

Edit `pom.xml`:

| Property | Purpose | Example |
|----------|---------|---------|
| `<output.directory>` | Must match `application.yaml` | `d:/projects/Minty` |
| `<tomcat.base>` | Path to your Tomcat installation | `d:/projects/Tomcat` |

> The Minty WAR will be assembled and automatically deployed to Tomcat.

---

## Frontend Build

Open a terminal (e.g., Git Bash) and navigate:

```bash
cd {Minty code location}/webapp/frontend
```

### Production Build

```bash
ng build
```

* The built assets are placed into the backend’s **bundle**.
* If you’re deploying this way, build the frontend **before** the backend so that the WAR automatically contains the frontend.

> **Important:** Edit `webapp/frontend/deploy/index.html`:

```html
<base href="/Minty/">
```

> (Change from `<base href="/">`.)

### Local Debug / Test

```bash
ng serve --proxy-config proxy.conf.json
```

* Runs a local dev server that proxies API calls to the backend.
* The server auto‑reloads on code changes.
* You may need to adjust `proxy.conf.json` (e.g., switch from `http` to `https`).

---

## Backend Build

**Prerequisites (If building in STS):**

- Maven runtime set to the one installed earlier.
- Java 21 (or newer) as the JDK.

**Commands**

```bash
# In {minty code location}/webapp/backend/parent
mvn install

# In {minty code location}/webapp/backend/api
mvn install

# In {minty code location}/webapp/backend/solution
mvn clean package

# For any additional plugin projects
mvn clean package
```

> You only need to run `mvn install` for `parent` and `api` once, unless those projects change.

---

## Running the Backend

```bash
# From the Tomcat root
{tomcat root}/bin/startup.bat
```

Tomcat will deploy the Minty WAR and redeploy automatically on rebuild.

---

## Running the Frontend

No additional steps are required beyond the build instructions above.
If you used `ng serve`, the frontend will be available automatically.

---

## Accessing Minty
For the Debug build, in your browser:
`http://localhost:4200`
This will access Minty via the Angular Web Server.

If you did the production build:
`http://<Tomcat Server IP>>:8080/Minty` for HTTP (bad idea this will be broken)
or
`https://<Tomcat Server IP>>:8443/Minty` for HTTPS
If you run in this way, you must configure certificates in Tomcat. See https://tomcat.apache.org/tomcat-11.0-doc/ssl-howto.html#SSL_and_Tomcat

Recommendation: If running locally, just use the Angular Web Server approach.

