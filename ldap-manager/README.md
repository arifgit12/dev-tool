# LDAP Manager

A Spring Boot desktop application for LDAP user validation, authentication, and management.

## Features

### Core Features
- **Connection Management**: Configure and manage multiple LDAP server connections
- **User Authentication**: Validate user credentials against LDAP servers
- **User Search**: Search for users by username, email, or other attributes
- **User Details**: View comprehensive user information including:
  - Basic profile information
  - Group memberships
  - All LDAP attributes
- **Export Functionality**: Export user data to Excel format
- **SSL/TLS Support**: Secure connections to LDAP servers

### Advanced Features
- Multi-connection support with easy switching
- Flexible search filters (supports Active Directory and OpenLDAP)
- Group membership visualization
- Copy user details to clipboard
- Custom search base and filter configuration

## Technology Stack

- **Java 17**
- **Spring Boot 3.2.0**
- **Spring LDAP**
- **Unboundid LDAP SDK**
- **Apache POI** (for Excel export)
- **Java Swing** (Desktop UI)

## Prerequisites

- Java 17 or higher
- Maven 3.6+
- Access to an LDAP server (Active Directory, OpenLDAP, etc.)

## Building the Application

```bash
cd ldap-manager
mvn clean package
```

## Running the Application

```bash
mvn spring-boot:run
```

Or run the generated JAR:

```bash
java -jar target/ldap-manager-1.0.0.jar
```

## Configuration

### LDAP Connection Parameters

When adding a new LDAP connection, you'll need to provide:

- **Name**: A friendly name for the connection
- **Host**: LDAP server hostname or IP address
- **Port**: LDAP port (default: 389 for LDAP, 636 for LDAPS)
- **Base DN**: The base distinguished name (e.g., `dc=example,dc=com`)
- **Bind Username/DN**: Username or full DN to bind with
  - For simple username: `admin` or `admin@example.com` (Active Directory)
  - For full DN: `cn=admin,dc=example,dc=com` (OpenLDAP)
- **Password**: Password for the bind user
- **Use SSL**: Enable for secure LDAPS connections
- **User Search Base**: Base DN for user searches (e.g., `ou=users,dc=example,dc=com`)
- **User Search Filter**: LDAP filter for finding users (e.g., `(uid={0})` or `(sAMAccountName={0})`)
  - `{0}` is replaced with the username being searched
- **Group Search Base**: Base DN for group searches (e.g., `ou=groups,dc=example,dc=com`)
- **Group Search Filter**: LDAP filter for finding groups (e.g., `(member={0})`)
  - `{0}` is replaced with the user's DN

### Example Configurations

#### Active Directory (with simple username)
```
Host: ad.example.com
Port: 389
Base DN: dc=example,dc=com
Bind Username/DN: administrator@example.com
  (or just: administrator)
Password: YourPassword123
User Search Base: ou=users,dc=example,dc=com
User Search Filter: (sAMAccountName={0})
Group Search Base: ou=groups,dc=example,dc=com
Group Search Filter: (member={0})
```

#### Active Directory (with full DN)
```
Host: ad.example.com
Port: 389
Base DN: dc=example,dc=com
Bind Username/DN: cn=ldap_reader,ou=service_accounts,dc=example,dc=com
Password: YourPassword123
User Search Base: ou=users,dc=example,dc=com
User Search Filter: (sAMAccountName={0})
Group Search Base: ou=groups,dc=example,dc=com
Group Search Filter: (member={0})
```

#### OpenLDAP
```
Host: ldap.example.com
Port: 389
Base DN: dc=example,dc=com
Bind Username/DN: cn=admin,dc=example,dc=com
Password: YourPassword123
User Search Base: ou=people,dc=example,dc=com
User Search Filter: (uid={0})
Group Search Base: ou=groups,dc=example,dc=com
Group Search Filter: (memberUid={0})
```

## Usage Guide

### 1. Managing Connections

1. Navigate to the **Connections** tab
2. Click **Add** to create a new LDAP connection
3. Fill in the connection details
4. Click **Test Connection** to verify the configuration
5. Save the connection

### 2. Searching Users

1. Navigate to the **User Search** tab
2. Select an active connection from the dropdown
3. Enter a search term (username, email, name, etc.)
4. Click **Search Users**
5. Results will appear in the table below

### 3. Authenticating Users

1. Navigate to the **User Search** tab
2. Select an active connection
3. Enter username and password in the Authentication section
4. Click **Authenticate**
5. A success or failure message will appear

### 4. Viewing User Details

1. After searching, click on a user in the results table
2. User details will appear in the lower panel showing:
   - Basic information
   - Group memberships
   - All LDAP attributes

### 5. Exporting User Data

1. Select a user to view their details
2. Click **Export to Excel**
3. Choose a location to save the Excel file
4. The file will contain three sheets:
   - Basic Info
   - Groups
   - All Attributes

## Stored Configuration

Connection configurations are stored in:
```
~/.ldap-manager/connections.json
```

This file contains encrypted connection details and can be backed up or transferred between installations.

## Security Considerations

- Connection passwords are stored in the configuration file
- Use appropriate file system permissions to protect the configuration
- Consider using read-only service accounts for LDAP bindings
- Enable SSL/TLS for production environments
- Test connections in a non-production environment first

## Troubleshooting

### Connection Failures
- Verify network connectivity to the LDAP server
- Check firewall rules
- Ensure the correct port is being used
- Verify SSL/TLS settings

### Authentication Failures
- Check that the bind DN and password are correct
- Verify the user search filter matches your LDAP schema
- Ensure the user exists in the specified search base

### Search Issues
- Verify the search base DN is correct
- Check that the search filter syntax is valid
- Ensure you have appropriate permissions to search

## License

This project is provided as-is for educational and internal use purposes.

## Support

For issues, questions, or contributions, please refer to the project repository.
