# WebLogic Client Setup Guide

This guide provides detailed instructions for installing the Oracle WebLogic client library required by the Oracle JMS Simulator.

## Why is this needed?

The Oracle JMS Simulator connects to Oracle WebLogic JMS servers using the WebLogic JNDI API. The WebLogic client JAR contains the necessary classes (like `weblogic.jndi.WLInitialContextFactory`) to establish this connection.

Oracle does not publish the WebLogic client JAR to public Maven repositories due to licensing restrictions. You must obtain it from your WebLogic Server installation.

## Prerequisites

- Oracle WebLogic Server installed (any version: 12.2.1, 14.1.1, etc.)
- Maven 3.6+ installed
- Access to the WebLogic installation directory

## Option 1: Full Client (wlfullclient.jar) - Recommended

The full client includes all WebLogic client classes and is the most compatible option.

### Step 1: Generate wlfullclient.jar

1. Open a terminal/command prompt
2. Navigate to your WebLogic Server installation:
   ```bash
   cd $WL_HOME/server/lib
   ```
   Where `$WL_HOME` is your WebLogic installation directory, e.g.:
   - Linux: `/opt/oracle/middleware/wlserver`
   - Windows: `C:\Oracle\Middleware\wlserver`

3. Generate the full client JAR:
   ```bash
   java -jar wljarbuilder.jar
   ```

4. This creates `wlfullclient.jar` in the same directory (typically 70-100 MB)

### Step 2: Install to Local Maven Repository

Install the generated JAR to your local Maven repository:

```bash
mvn install:install-file \
  -Dfile=wlfullclient.jar \
  -DgroupId=com.oracle.weblogic \
  -DartifactId=wlfullclient \
  -Dversion=14.1.1.0 \
  -Dpackaging=jar
```

**Important:** Replace `14.1.1.0` with your actual WebLogic version:
- WebLogic 12.2.1.x → use `12.2.1.4`
- WebLogic 14.1.1.x → use `14.1.1.0`
- Check your version: `cd $WL_HOME && cat registry.xml | grep "component name=\"WebLogic Server\""`

### Step 3: Add Dependency to pom.xml

Edit `oracle-jms-simulator/pom.xml` and add the following dependency after the H2 dependency:

```xml
<!-- Oracle WebLogic Client -->
<dependency>
    <groupId>com.oracle.weblogic</groupId>
    <artifactId>wlfullclient</artifactId>
    <version>14.1.1.0</version>
</dependency>
```

Replace `14.1.1.0` with the version you used in Step 2.

## Option 2: Thin Client (wlthint3client.jar) - Lightweight

The thin client is smaller (5-10 MB) and suitable for most use cases.

### Step 1: Locate wlthint3client.jar

The thin client is pre-built and located at:
```bash
$WL_HOME/server/lib/wlthint3client.jar
```

### Step 2: Install to Local Maven Repository

```bash
mvn install:install-file \
  -Dfile=$WL_HOME/server/lib/wlthint3client.jar \
  -DgroupId=com.oracle.weblogic \
  -DartifactId=wlthint3client \
  -Dversion=14.1.1.0 \
  -Dpackaging=jar
```

Replace `$WL_HOME` with your actual path and adjust the version as needed.

### Step 3: Add Dependency to pom.xml

Edit `oracle-jms-simulator/pom.xml` and add:

```xml
<!-- Oracle WebLogic Thin Client -->
<dependency>
    <groupId>com.oracle.weblogic</groupId>
    <artifactId>wlthint3client</artifactId>
    <version>14.1.1.0</version>
</dependency>
```

## Verification

After installing the WebLogic client JAR and adding the dependency:

1. Rebuild the application:
   ```bash
   cd oracle-jms-simulator
   mvn clean package
   ```

2. Run the application:
   ```bash
   java -jar target/oracle-jms-simulator-1.0.0.jar
   ```

3. In the application:
   - Enter your connection details
   - Click "Test Connection"
   - You should see "Connection test successful" if everything is configured correctly

## Troubleshooting

### Maven install command fails

**Error:** `mvn: command not found`

**Solution:** Install Maven or use the full path to mvn executable

---

**Error:** File not found

**Solution:** Verify the path to the JAR file. Use `ls -la` (Linux/Mac) or `dir` (Windows) to confirm the file exists.

### Build fails after adding dependency

**Error:** `Could not find artifact com.oracle.weblogic:wlfullclient:jar:14.1.1.0`

**Solution:** 
1. Verify the JAR was installed to local Maven repository (check `~/.m2/repository/com/oracle/weblogic/`)
2. Ensure the version in pom.xml matches the version used in `mvn install:install-file`

### Connection still fails with NoInitialContextException

**Error:** `Cannot instantiate class: weblogic.jndi.WLInitialContextFactory`

**Possible causes:**
1. Dependency not added to pom.xml
2. Application not rebuilt after adding dependency
3. Wrong artifact ID or version in pom.xml

**Solution:**
1. Verify dependency is in pom.xml
2. Run `mvn clean package` to rebuild
3. Check that the WebLogic client JAR is included in the built JAR:
   ```bash
   jar tf target/oracle-jms-simulator-1.0.0.jar | grep weblogic
   ```

## Which Option Should I Choose?

| Feature | Full Client (wlfullclient.jar) | Thin Client (wlthint3client.jar) |
|---------|-------------------------------|----------------------------------|
| Size | 70-100 MB | 5-10 MB |
| Compatibility | Best | Good |
| Generation Required | Yes (run wljarbuilder) | No (pre-built) |
| Recommended For | Production, Complex scenarios | Development, Simple scenarios |

**Recommendation:** Use the thin client (wlthint3client.jar) for development and testing. Use the full client (wlfullclient.jar) if you encounter any compatibility issues.

## Alternative: System Classpath

If you cannot modify the pom.xml or prefer not to install to Maven repository, you can add the WebLogic client JAR to the classpath when running:

```bash
java -cp "target/oracle-jms-simulator-1.0.0.jar:$WL_HOME/server/lib/wlthint3client.jar" \
  org.springframework.boot.loader.JarLauncher
```

Note: This approach is not recommended as it's less portable.

## Support

If you continue to have issues:
1. Check the console logs for detailed error messages
2. Verify your WebLogic Server is running and accessible
3. Ensure network connectivity to the WebLogic Server
4. Test connection using WebLogic's built-in tools first
