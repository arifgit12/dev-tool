# LDAP Manager - Quick Reference Card

## 🚀 Quick Start (30 seconds)

```bash
1. Start app: mvn spring-boot:run
2. Go to "Test Server" tab
3. Click "Start Server"
4. Click "Add Sample Data"
5. Click "Create Connection Config"
6. Go to "User Search" tab
7. Login: jdoe / password123
```

---

## 📋 Sample Test Accounts

| Username | Password | Name | Role |
|----------|----------|------|------|
| `jdoe` | `password123` | John Doe | Software Engineer |
| `jsmith` | `password123` | Jane Smith | Project Manager |
| `bwilson` | `password123` | Bob Wilson | Database Admin |
| `abrown` | `password123` | Alice Brown | Business Analyst |
| `cdavis` | `password123` | Charlie Davis | DevOps Engineer |

---

## 🔧 Add User (Copy-Paste Ready)

### For dc=local,dc=aam,dc=sa

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
title: Senior Developer
departmentNumber: IT
```

### For dc=example,dc=com

```ldif
dn: cn=Test User,ou=users,dc=example,dc=com
objectClass: top
objectClass: person
objectClass: organizationalPerson
objectClass: inetOrgPerson
cn: Test User
sn: User
givenName: Test
uid: tuser
mail: test.user@example.com
userPassword: test123
mobile: +1-555-9999
```

---

## 🔍 Common LDAP Filters

```bash
# Active Directory
User by username:       (sAMAccountName=jdoe)
User by email:          (mail=john@example.com)
All users:              (&(objectCategory=person)(objectClass=user))
All active users:       (&(objectCategory=person)(!(userAccountControl:1.2.840.113556.1.4.803:=2)))
All groups:             (objectClass=group)

# OpenLDAP
User by username:       (uid=jdoe)
User by email:          (mail=john@example.com)
All users:              (objectClass=inetOrgPerson)
All groups:             (objectClass=groupOfNames)

# Wildcards
Starts with:            (cn=John*)
Contains:               (cn=*doe*)
Multiple attributes:    (|(uid=jdoe)(mail=jdoe@example.com))
```

---

## 🌐 Connection Quick Config

### Active Directory
```
Host: ad.company.com
Port: 389
Base DN: dc=company,dc=com  (use Convert button!)
Username: admin@company.com
Filter: (sAMAccountName={0})
```

### OpenLDAP
```
Host: ldap.company.com
Port: 389
Base DN: dc=company,dc=com
Username: cn=admin,dc=company,dc=com
Filter: (uid={0})
```

### Embedded Server
```
Host: localhost
Port: 10389
Base DN: dc=example,dc=com
Username: cn=admin,dc=example,dc=com
Password: admin123
```

---

## 🎯 Common Tasks

### Connect to Real LDAP
```
Connections → Add → Fill form → Test → Save
```

### Search User
```
User Search → Select connection → Enter name → Search
```

### Authenticate User
```
User Search → Enter username/password → Authenticate
```

### Get User's Groups
```
User Search → Search user → Click result → View "Group Memberships"
```

### Export User Data
```
User Search → Select user → Export to Excel
```

### Test Groups
```
Advanced Testing → "Search Groups" → Enter group name → Execute
```

### Get Group Contacts
```
Advanced Testing → "Get Group Members with Mobiles" → Enter groups → Execute
```

---

## 🛠️ Embedded Server Commands

```bash
Start:              Click "Start Server"
Add test data:      Click "Add Sample Data"
Add custom user:    Paste LDIF → "Add Custom Entry"
Connect to it:      Click "Create Connection Config"
Clear data:         Click "Clear All Data"
Stop:               Click "Stop Server"
```

---

## 📁 File Locations

```
Connections:        ~/.ldap-manager/connections.json
Application:        ldap-manager/target/ldap-manager-1.0.0.jar
Guide:              ldap-manager/GUIDE.md
```

---

## 🐛 Troubleshooting Quick Fixes

| Problem | Solution |
|---------|----------|
| Connection failed | Check host, port, firewall |
| Invalid DN | Use format: `dc=example,dc=com` |
| Auth failed | Verify username format and password |
| No search results | Check User Search Base and Filter |
| Can't add user | Ensure `ou=users` exists (click "Add Sample Data" first) |
| Server won't start | Try different port or restart app |

---

## 🔑 Required LDIF Fields

**Minimum for user:**
```ldif
dn: cn=Name,ou=users,dc=example,dc=com
objectClass: inetOrgPerson
cn: Name
sn: Surname
```

**Recommended:**
```ldif
+ uid: username
+ mail: email@example.com
+ userPassword: password123
+ mobile: +1-555-1234
```

---

## 📞 Support Commands

```bash
# Check Java version
java -version

# Build application
mvn clean package -DskipTests

# Run with logs
java -jar target/ldap-manager-1.0.0.jar > app.log 2>&1

# Check port usage
netstat -an | grep 10389
```

---

## ⚡ Pro Tips

1. **Use "Convert" button** for Base DN - saves typing
2. **Test connection** before saving - catches errors early
3. **Start with embedded server** - no external setup needed
4. **Export to Excel** - easier data analysis
5. **Keep LDIF templates** - reuse for testing
6. **Check console logs** - detailed error messages
7. **Backup connections.json** - save your configs

---

## 🎓 Learning Path

```
Day 1: Use embedded server (Tab 4)
Day 2: Connect to real LDAP (Tab 1)
Day 3: Master search operations (Tab 2)
Day 4: Advanced testing (Tab 3)
Day 5: Custom LDIF and groups
```

---

**Need help?** Check GUIDE.md for detailed documentation.
mvn javafx:ru
