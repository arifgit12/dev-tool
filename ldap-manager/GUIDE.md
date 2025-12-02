# LDAP Manager - Complete User Guide

## Table of Contents

1. [Introduction](#introduction)
2. [Installation](#installation)
3. [Quick Start](#quick-start)
4. [Application Overview](#application-overview)
5. [Tab 1: Connections](#tab-1-connections)
6. [Tab 2: User Search](#tab-2-user-search)
7. [Tab 3: Advanced Testing](#tab-3-advanced-testing)
8. [Tab 4: Test Server](#tab-4-test-server)
9. [Adding Users to Embedded Server](#adding-users-to-embedded-server)
10. [Common Scenarios](#common-scenarios)
11. [Troubleshooting](#troubleshooting)
12. [API Reference](#api-reference)

---

## Introduction

LDAP Manager is a comprehensive Spring Boot desktop application for managing, testing, and validating LDAP/Active Directory connections and users. It includes:

- **Connection Management**: Configure and test multiple LDAP servers
- **User Search & Authentication**: Search users and validate credentials
- **Advanced Testing**: 8 specialized LDAP operations
- **Embedded Test Server**: Built-in LDAP server for testing without external dependencies

---

## Installation

### Prerequisites

- Java 17 or higher
- Maven 3.6+ (for building from source)

### Running the Application

**Option 1: Using Maven**
```bash
cd ldap-manager
mvn spring-boot:run
```

**Option 2: Using JAR**
```bash
cd ldap-manager
mvn clean package -DskipTests
java -jar target/ldap-manager-1.0.0.jar
```

**Option 3: Run Scripts**
```bash
# Windows
run.bat

# Linux/Mac
chmod +x run.sh
./run.sh
```

---

## Quick Start

### Testing Without External LDAP Server

1. **Start the application**
2. Go to **"Test Server"** tab
3. Click **"Start Server"** (uses default settings)
4. Click **"Add Sample Data"**
5. Click **"Create Connection Config"**
6. Go to **"User Search"** tab
7. Select **"Embedded Test Server"** from dropdown
8. Try searching or authenticating:
   - Username: `jdoe`
   - Password: `password123`

### Connecting to Real LDAP Server

1. Go to **"Connections"** tab
2. Click **"Add"**
3. Fill in connection details
4. Click **"Test Connection"**
5. Click **"Save"**
6. Go to **"User Search"** tab
7. Select your connection and start testing

---

## Application Overview

The application has 4 main tabs:

| Tab | Purpose | Use Case |
|-----|---------|----------|
| **Connections** | Manage LDAP server connections | Add, edit, test, delete connections |
| **User Search** | Search and authenticate users | Validate credentials, search users |
| **Advanced Testing** | Specialized LDAP operations | Test groups, retrieve attributes, bulk operations |
| **Test Server** | Embedded LDAP server | Test without external LDAP server |

---

## Tab 1: Connections

### Purpose
Manage multiple LDAP server connections and their configurations.

### Features

#### Add Connection
1. Click **"Add"** button
2. Fill in the form:

| Field | Description | Example |
|-------|-------------|---------|
| Name | Friendly name for connection | "Production AD" |
| Host | LDAP server hostname/IP | `ad.company.com` |
| Port | LDAP port | `389` (LDAP) or `636` (LDAPS) |
| Base DN | Base distinguished name | `dc=company,dc=com` |
| Bind Username/DN | Admin/service account | `admin@company.com` or `cn=admin,dc=company,dc=com` |
| Password | Account password | Your password |
| Use SSL | Enable for secure connection | Check for LDAPS |
| User Search Base | Where to search for users | `ou=users,dc=company,dc=com` |
| User Search Filter | LDAP filter for users | `(sAMAccountName={0})` or `(uid={0})` |
| Group Search Base | Where to search for groups | `ou=groups,dc=company,dc=com` |
| Group Search Filter | LDAP filter for groups | `(member={0})` |

3. Click **"Save"**

#### Base DN Helper

The **"Convert"** button next to Base DN automatically converts domain names to DN format:
- `company.com` → `dc=company,dc=com`
- `local.aam.sa` → `dc=local,dc=aam,dc=sa`

#### Test Connection

Select a connection and click **"Test Connection"** to verify connectivity.

#### Edit/Delete Connection

- Select connection → **"Edit"** to modify
- Select connection → **"Delete"** to remove (with confirmation)

### Examples

#### Active Directory (Simple Username)
```
Name: Company AD
Host: ad.company.com
Port: 389
Base DN: dc=company,dc=com
Bind Username/DN: administrator@company.com
Password: YourPassword
User Search Filter: (sAMAccountName={0})
Group Search Filter: (member={0})
```

#### Active Directory (Full DN)
```
Name: Company AD
Host: ad.company.com
Port: 389
Base DN: dc=company,dc=com
Bind Username/DN: cn=ldap_reader,ou=service_accounts,dc=company,dc=com
Password: YourPassword
User Search Filter: (sAMAccountName={0})
Group Search Filter: (member={0})
```

#### OpenLDAP
```
Name: OpenLDAP Server
Host: ldap.company.com
Port: 389
Base DN: dc=company,dc=com
Bind Username/DN: cn=admin,dc=company,dc=com
Password: YourPassword
User Search Filter: (uid={0})
Group Search Filter: (memberUid={0})
```

---

## Tab 2: User Search

### Purpose
Search for users and validate their credentials against LDAP server.

### Features

#### Connection Selection
Select active connection from dropdown at the top.

#### Search Users
1. Enter search term (name, username, email)
2. Click **"Search Users"** or press Enter
3. Results appear in table below
4. Click on a result to view details

#### Authenticate User
1. Enter username and password
2. Click **"Authenticate"**
3. View success/failure message
4. User details load automatically on success

#### View User Details
When you select a user, the bottom panel shows:
- **Basic Information**: DN, username, email, phone, etc.
- **Group Memberships**: All groups the user belongs to
- **All Attributes**: Complete LDAP attribute list

#### Export User Data
- **Copy to Clipboard**: Copy basic info as text
- **Export to Excel**: Save complete user data to .xlsx file with 3 sheets:
  - Basic Info
  - Groups
  - All Attributes

---

## Tab 3: Advanced Testing

### Purpose
Perform specialized LDAP operations for testing and debugging.

### Available Operations

#### 1. Authenticate User
Test user authentication with username and password.

**Input:**
- Username
- Password

**Output:**
- Success/Failure status

**Use Case:** Validate credentials without searching

#### 2. Retrieve User Details
Get all LDAP attributes for a specific user.

**Input:**
- Username

**Output:**
- All attributes and their values

**Use Case:** Debug user data, verify attributes

#### 3. Check User Exists
Quickly verify if a user exists in the directory.

**Input:**
- Username

**Output:**
- YES/NO result

**Use Case:** Validation before creating accounts

#### 4. Search Users by Name (with Mobile)
Search users by name pattern and show mobile numbers.

**Input:**
- Search term (supports wildcards)

**Output:**
- Display name, mobile, email

**Use Case:** Build contact lists

#### 5. Search Groups
Find groups by name pattern.

**Input:**
- Group name/pattern

**Output:**
- Group name, description, members

**Use Case:** Audit group memberships

#### 6. Get Group Members with Mobiles
Retrieve mobile numbers for all members of specified groups.

**Input:**
- Group IDs (one per line)

**Output:**
- Group members with mobile numbers

**Use Case:** Extract contact information for groups

#### 7. Retrieve Specific User Attributes
Get specific or all attributes for a user.

**Input:**
- Username
- Attribute name (optional)

**Output:**
- Requested attribute(s)

**Use Case:** Debug specific attributes

#### 8. Test Connection with Custom Credentials
Test connection using any username/password.

**Input:**
- Username
- Password

**Output:**
- Connection status and details

**Use Case:** Test different credentials

---

## Tab 4: Test Server

### Purpose
Run a lightweight embedded LDAP server for testing without external dependencies.

### Quick Start

1. **Configure Server** (or use defaults):
   - Port: `10389`
   - Base DN: `dc=example,dc=com`
   - Admin Password: `admin123`

2. **Start Server**: Click "Start Server"

3. **Add Data**: Click "Add Sample Data"

4. **Create Connection**: Click "Create Connection Config"

5. **Test**: Switch to other tabs to use it

### Server Configuration

| Setting | Description | Default | Custom Example |
|---------|-------------|---------|----------------|
| Port | LDAP listener port | 10389 | 10389 |
| Base DN | Root DN | dc=example,dc=com | dc=local,dc=aam,dc=sa |
| Admin Password | Admin account password | admin123 | YourSecurePassword |

### Sample Data

The "Add Sample Data" button creates:

**5 Users** (password: `password123`):
- `jdoe` - John Doe (Software Engineer, IT)
- `jsmith` - Jane Smith (Project Manager, Management)
- `bwilson` - Bob Wilson (Database Administrator, IT)
- `abrown` - Alice Brown (Business Analyst, Business)
- `cdavis` - Charlie Davis (DevOps Engineer, IT)

**4 Groups**:
- Developers: John Doe, Bob Wilson, Charlie Davis
- Managers: Jane Smith
- IT Department: John Doe, Bob Wilson, Charlie Davis
- All Employees: All users

### Adding Custom Users

Use the "Add Custom LDIF Entry" section to add your own data.

### Operations

| Button | Action | When to Use |
|--------|--------|-------------|
| Start Server | Start the LDAP server | Before any testing |
| Stop Server | Stop the LDAP server | When done testing |
| Add Sample Data | Load 5 users and 4 groups | Quick test setup |
| Clear All Data | Remove all entries | Start fresh |
| Create Connection Config | Add server to connections list | Connect other tabs |
| Add Custom Entry | Add LDIF you provide | Custom test data |

### Server Information

When running, the status area shows:
```
Server Status: RUNNING
Listen Address: localhost:10389
Base DN: dc=local,dc=aam,dc=sa
Admin DN: cn=admin,dc=local,dc=aam,dc=sa
Protocol: LDAP (non-SSL)

Connection String: ldap://localhost:10389
```

---

## Adding Users to Embedded Server

### Method 1: Use Sample Data (Fastest)

Click **"Add Sample Data"** button. This automatically creates users with your configured Base DN.

### Method 2: Add Custom Users via LDIF

#### Step 1: Ensure Organizational Units Exist

First time setup - add organizational units:

```ldif
dn: ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: organizationalUnit
ou: users

dn: ou=groups,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: organizationalUnit
ou: groups
```

> **Note:** These are created automatically when you click "Add Sample Data"

#### Step 2: Add Individual Users

**Basic User Example:**

```ldif
dn: cn=Ahmed Ali,ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Ahmed Ali
sn: Ali
givenName: Ahmed
uid: aali
mail: ahmed.ali@local.aam.sa
userPassword: test123
mobile: +966-555-1234
telephoneNumber: +966-555-1234
title: Senior Developer
departmentNumber: IT
description: Senior Software Developer
```

**Complete User Example:**

```ldif
dn: cn=Fatima Mohammed,ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Fatima Mohammed
sn: Mohammed
givenName: Fatima
uid: fmohammed
mail: fatima.mohammed@local.aam.sa
userPassword: secure123
mobile: +966-555-5678
telephoneNumber: +966-555-5678
employeeNumber: 2001
title: Project Manager
departmentNumber: Management
description: Senior Project Manager - IT Division
l: Riyadh
postalAddress: Building 5, Floor 3
```

#### Step 3: Add Multiple Users at Once

```ldif
dn: cn=Khalid Hassan,ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Khalid Hassan
sn: Hassan
givenName: Khalid
uid: khassan
mail: khalid.hassan@local.aam.sa
userPassword: pass123
mobile: +966-555-9999
title: Database Administrator
departmentNumber: IT

dn: cn=Sara Ahmed,ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Sara Ahmed
sn: Ahmed
givenName: Sara
uid: sahmed
mail: sara.ahmed@local.aam.sa
userPassword: pass123
mobile: +966-555-7777
title: Business Analyst
departmentNumber: Business

dn: cn=Mohammed Ibrahim,ou=users,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Mohammed Ibrahim
sn: Ibrahim
givenName: Mohammed
uid: mibrahim
mail: mohammed.ibrahim@local.aam.sa
userPassword: pass123
mobile: +966-555-6666
title: DevOps Engineer
departmentNumber: IT
```

#### Step 4: Create Groups

**Department Group:**

```ldif
dn: cn=Engineering,ou=groups,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: groupOfNames
cn: Engineering
description: Engineering Department
member: cn=Ahmed Ali,ou=users,dc=local,dc=aam,dc=sa
member: cn=Khalid Hassan,ou=users,dc=local,dc=aam,dc=sa
member: cn=Mohammed Ibrahim,ou=users,dc=local,dc=aam,dc=sa
```

**Project Group:**

```ldif
dn: cn=Project Alpha,ou=groups,dc=local,dc=aam,dc=sa
objectClass: top
objectClass: groupOfNames
cn: Project Alpha
description: Project Alpha Team Members
member: cn=Ahmed Ali,ou=users,dc=local,dc=aam,dc=sa
member: cn=Fatima Mohammed,ou=users,dc=local,dc=aam,dc=sa
member: cn=Sara Ahmed,ou=users,dc=local,dc=aam,dc=sa
```

### LDIF Field Reference

#### Required Fields

| Field | Description | Example |
|-------|-------------|---------|
| `dn` | Distinguished Name (unique identifier) | `cn=Ahmed Ali,ou=users,dc=local,dc=aam,dc=sa` |
| `objectClass` | Entry type definition | `top`, `person`, `inetOrgPerson` |
| `cn` | Common Name | `Ahmed Ali` |
| `sn` | Surname | `Ali` |

#### Common Optional Fields

| Field | Description | Example |
|-------|-------------|---------|
| `givenName` | First name | `Ahmed` |
| `uid` | User ID (username) | `aali` |
| `mail` | Email address | `ahmed.ali@company.com` |
| `userPassword` | Password for authentication | `securePass123` |
| `mobile` | Mobile phone number | `+966-555-1234` |
| `telephoneNumber` | Office phone | `+966-555-1234` |
| `title` | Job title | `Senior Developer` |
| `departmentNumber` | Department | `IT` |
| `employeeNumber` | Employee ID | `1001` |
| `description` | Additional info | `Senior Software Engineer` |
| `l` | Location/City | `Riyadh` |
| `postalAddress` | Physical address | `Building 5, Floor 3` |

### Testing Your Users

After adding users:

1. **Go to User Search tab**
2. **Select "Embedded Test Server"**
3. **Test Authentication:**
   ```
   Username: aali
   Password: test123
   ```
4. **Test Search:**
   ```
   Search: Ahmed
   ```

---

## Common Scenarios

### Scenario 1: Quick Test Without External LDAP

**Goal:** Test the application immediately without setting up LDAP server.

**Steps:**
1. Start application
2. Go to "Test Server" tab
3. Click "Start Server"
4. Click "Add Sample Data"
5. Click "Create Connection Config"
6. Go to "User Search" tab
7. Select "Embedded Test Server"
8. Authenticate with `jdoe` / `password123`

**Time:** < 2 minutes

### Scenario 2: Connect to Active Directory

**Goal:** Connect to your company's Active Directory.

**Steps:**
1. Go to "Connections" tab
2. Click "Add"
3. Fill in:
   ```
   Name: Company AD
   Host: ad.company.com
   Port: 389
   Base DN: dc=company,dc=com
   Bind Username: ldap_reader@company.com
   Password: YourPassword
   User Search Filter: (sAMAccountName={0})
   ```
4. Click "Test Connection"
5. If successful, click "Save"
6. Go to "User Search" tab
7. Select your connection
8. Start searching/authenticating

### Scenario 3: Extract Contact List from Groups

**Goal:** Get mobile numbers for all members of specific groups.

**Steps:**
1. Go to "Advanced Testing" tab
2. Select operation: "Get Group Members with Mobiles"
3. Enter group names (one per line):
   ```
   Developers
   IT Department
   Managers
   ```
4. Click "Execute"
5. Copy results or export

### Scenario 4: Validate User Exists Before Creation

**Goal:** Check if username already exists before creating new account.

**Steps:**
1. Go to "Advanced Testing" tab
2. Select operation: "Check User Exists"
3. Enter username to check
4. Click "Execute"
5. View YES/NO result

### Scenario 5: Debug User Attributes

**Goal:** See all LDAP attributes for a user to debug issues.

**Steps:**
1. Go to "User Search" tab
2. Search for user
3. Click on user in results
4. View "All Attributes" section in details panel
5. Use "Export to Excel" for detailed analysis

### Scenario 6: Create Custom Test Environment

**Goal:** Create specific test users for your application.

**Steps:**
1. Go to "Test Server" tab
2. Configure with your domain structure
3. Click "Start Server"
4. Click "Add Sample Data" (for organizational structure)
5. Use "Add Custom LDIF Entry" to add your specific users
6. Click "Create Connection Config"
7. Test with your custom data

---

## Troubleshooting

### Connection Issues

#### Problem: "Connection failed" when testing connection

**Solutions:**
1. Verify host and port are correct
2. Check firewall rules
3. Verify SSL settings (port 636 requires SSL)
4. Test connectivity: `telnet hostname port`
5. Check network connectivity

#### Problem: "Invalid DN syntax" error

**Solution:**
- Ensure Base DN is in correct format: `dc=example,dc=com`
- NOT: `example.com`
- Use the "Convert" button next to Base DN field

### Authentication Issues

#### Problem: "Authentication failed" for known user

**Solutions:**
1. Verify username format:
   - AD: Try `username@domain.com` or `DOMAIN\username`
   - OpenLDAP: Use `uid`
2. Check password (case-sensitive)
3. Verify user search filter matches your LDAP schema:
   - AD: `(sAMAccountName={0})`
   - OpenLDAP: `(uid={0})`
4. Ensure bind DN has permission to search

#### Problem: Can't authenticate with embedded server

**Solution:**
1. Verify you added the user (click "Add Sample Data")
2. Check username matches `uid` in LDIF
3. Verify password matches `userPassword` in LDIF
4. Check connection configuration is correct

### Search Issues

#### Problem: No search results found

**Solutions:**
1. Verify User Search Base is correct
2. Check User Search Filter syntax
3. Ensure bind DN has search permissions
4. Try broader search term
5. Check user actually exists in that base DN

#### Problem: "Search Base not found" error

**Solution:**
- Verify Search Base DN exists in LDAP
- Check spelling and format
- For embedded server, ensure you clicked "Add Sample Data" first

### Embedded Server Issues

#### Problem: Can't start embedded server

**Solutions:**
1. Check port is not in use:
   ```bash
   netstat -an | grep 10389
   ```
2. Try different port
3. Check Java version (requires Java 17+)
4. Restart application

#### Problem: Added users but can't find them

**Solutions:**
1. Verify Base DN in user DN matches server Base DN
2. Check you added organizational units first (`ou=users`)
3. Verify LDIF syntax is correct
4. Check server is still running

### General Issues

#### Problem: Application won't start

**Solutions:**
1. Verify Java 17+ is installed: `java -version`
2. Check Maven build succeeded
3. Look for errors in console output
4. Try: `mvn clean package -DskipTests`

#### Problem: UI elements not responding

**Solution:**
1. Check if operation is running (look for "Executing..." text)
2. Wait for completion
3. Restart application if frozen

---

## API Reference

### LDAP Connection Model

```java
LdapConnection {
    String id;              // Unique identifier
    String name;            // Display name
    String host;            // LDAP server hostname
    int port;               // LDAP port (389 or 636)
    String baseDn;          // Base DN (dc=example,dc=com)
    String userDn;          // Bind username/DN
    String password;        // Bind password
    boolean useSsl;         // Use SSL/TLS
    String userSearchBase;  // User search base DN
    String userSearchFilter;// User search filter
    String groupSearchBase; // Group search base DN
    String groupSearchFilter;// Group search filter
}
```

### LDAP User Model

```java
LdapUser {
    String dn;              // Distinguished Name
    String username;        // Username (uid/sAMAccountName)
    String cn;              // Common Name
    String email;           // Email address
    String firstName;       // First name
    String lastName;        // Last name
    String displayName;     // Display name
    String telephone;       // Phone number
    String department;      // Department
    String title;           // Job title
    boolean enabled;        // Account status
    List<String> groups;    // Group memberships
    Map<String, List<String>> attributes; // All attributes
}
```

### Configuration File Location

Connection configurations are stored at:
```
~/.ldap-manager/connections.json
```

### Log Files

Application logs are printed to console. To save logs:
```bash
java -jar ldap-manager-1.0.0.jar > app.log 2>&1
```

---

## Best Practices

### Security

1. **Don't commit passwords**: Keep connection configs out of version control
2. **Use service accounts**: Create dedicated LDAP service accounts with minimal permissions
3. **Enable SSL**: Always use SSL/TLS for production environments
4. **Rotate credentials**: Regularly update service account passwords
5. **Limit permissions**: Bind DN should have read-only access

### Performance

1. **Limit search results**: Use specific search filters
2. **Set appropriate search bases**: Narrow down search scope
3. **Use indexed attributes**: Search on `uid`, `sAMAccountName`, etc.
4. **Close connections**: Application handles this automatically

### Testing

1. **Test in dev first**: Use embedded server for development
2. **Use sample data**: Click "Add Sample Data" for quick tests
3. **Validate connections**: Always test before saving
4. **Document filters**: Keep notes on which filters work for your LDAP

### Data Management

1. **Backup connections**: Save `~/.ldap-manager/connections.json`
2. **Export important data**: Use Excel export for records
3. **Clear test data**: Use "Clear All Data" between test runs
4. **Version LDIF files**: Keep custom LDIF templates in version control

---

## Support

### Documentation

- **README.md**: Installation and quick start
- **GUIDE.md**: This comprehensive guide

### Getting Help

1. Check this guide first
2. Review console logs for error messages
3. Test with embedded server to isolate issues
4. Verify LDAP server connectivity independently

### Reporting Issues

When reporting issues, provide:
1. Error message from dialog and console
2. Connection configuration (without passwords)
3. LDAP server type (AD, OpenLDAP, etc.)
4. Steps to reproduce
5. Application version

---

## Appendix

### LDAP Filter Examples

```
# Find user by username (AD)
(sAMAccountName=jdoe)

# Find user by email
(mail=john.doe@example.com)

# Find all active users (AD)
(&(objectCategory=person)(objectClass=user)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))

# Find all groups
(objectClass=group)

# Find group members
(memberOf=cn=Developers,ou=groups,dc=example,dc=com)

# Multiple conditions (OR)
(|(uid=jdoe)(mail=john.doe@example.com))

# Multiple conditions (AND)
(&(objectClass=person)(department=IT))

# Wildcard search
(cn=John*)
```

### Common LDAP Attributes

| Attribute | Description | Example |
|-----------|-------------|---------|
| `dn` | Distinguished Name | `cn=John Doe,ou=users,dc=example,dc=com` |
| `cn` | Common Name | `John Doe` |
| `sn` | Surname | `Doe` |
| `givenName` | First Name | `John` |
| `uid` | User ID (OpenLDAP) | `jdoe` |
| `sAMAccountName` | Username (AD) | `jdoe` |
| `userPrincipalName` | UPN (AD) | `jdoe@example.com` |
| `mail` | Email | `john.doe@example.com` |
| `telephoneNumber` | Phone | `+1-555-0101` |
| `mobile` | Mobile Phone | `+1-555-0101` |
| `title` | Job Title | `Software Engineer` |
| `department` | Department | `IT` |
| `manager` | Manager DN | `cn=Jane Smith,ou=users,dc=example,dc=com` |
| `memberOf` | Group Membership | `cn=Developers,ou=groups,dc=example,dc=com` |
| `objectClass` | Entry Type | `person`, `inetOrgPerson` |

### Port Reference

| Port | Protocol | Description |
|------|----------|-------------|
| 389 | LDAP | Standard LDAP (unencrypted) |
| 636 | LDAPS | LDAP over SSL/TLS |
| 3268 | GC | AD Global Catalog |
| 3269 | GC SSL | AD Global Catalog over SSL |
| 10389 | LDAP | Embedded test server (default) |

---

**Version:** 1.0.0
**Last Updated:** 2025-12-02
**Author:** LDAP Manager Team
