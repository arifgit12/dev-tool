package com.ldapmanager.test;

import javax.naming.Context;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.ldap.InitialLdapContext;
import javax.naming.ldap.LdapContext;
import java.util.Hashtable;

public class LdapAuthExample {

    public static void main(String[] args) {
        // --- CONFIGURE THESE ---
        String ldapUrl = "ldap://192.168.100.250:389";
        String baseDn = "CN=Users,DC=local,DC=cerebra,DC=sa";
        String samAccountName = "u01.linq2";
        String cnName = "linqq2-user01";
        String password = "App@0912";
        // -----------------------

        // build domain from baseDn (DC=local,DC=cerebra,DC=sa -> local.cerebra.sa)
        String domainFromBase = domainFromBaseDn(baseDn);

        // 1) Try UPN (userPrincipalName) bind: username@domain
        String upn = samAccountName + "@" + domainFromBase;
        System.out.println("Attempting bind using UPN: " + upn);
        boolean ok = false; // tryBind(ldapUrl, upn, password);
        if (ok) {
            System.out.println("Authenticated using UPN: " + upn);
            return;
        } else {
            System.out.println("UPN bind failed, trying DN-style bind...");
        }

        // 2) Try DN-style bind by composing DN under provided base (works if CN equals samAccountName)
        // Be careful: CN may not equal samAccountName in all AD setups.
        String guessedDn = "cn=" + samAccountName + "," + baseDn;
        //String guessedDn = "cn=" + cnName + "," + baseDn;
        System.out.println("Attempting bind using guessed DN: " + guessedDn);
        ok = tryBind(ldapUrl, guessedDn, password);
        if (ok) {
            System.out.println("Authenticated using guessed DN: " + guessedDn);
            return;
        } else {
            System.out.println("Guessed DN bind failed.");
        }

        System.out.println("Authentication failed with both UPN and guessed DN.");
        System.out.println("If both fail, you likely need to either:");
        System.out.println("- provide the correct full DN for the user, or");
        System.out.println("- perform a search (requires a service account or anonymous search enabled) to obtain the user's DN.");
    }

    private static boolean tryBind(String ldapUrl, String principal, String password) {
        Hashtable<String, String> env = new Hashtable<>();
        env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
        env.put(Context.PROVIDER_URL, ldapUrl);

        // recommended options
        env.put("com.sun.jndi.ldap.connect.timeout", "5000"); // ms
        env.put(Context.SECURITY_AUTHENTICATION, "simple");
        env.put(Context.SECURITY_PRINCIPAL, principal);
        env.put(Context.SECURITY_CREDENTIALS, password);

        // optional: follow referrals if your AD requires it
        env.put(Context.REFERRAL, "follow");

        LdapContext ctx = null;
        try {
            ctx = new InitialLdapContext(env, null);
            // success
            return true;
        } catch (NamingException e) {
            // authentication failed or other LDAP error
            System.err.println("Bind error for principal=" + principal + " : " + e.getMessage());
            return false;
        } finally {
            if (ctx != null) {
                try {
                    ctx.close();
                } catch (NamingException ignored) {}
            }
        }
    }

    private static String domainFromBaseDn(String baseDn) {
        // convert DC=local,DC=cerebra,DC=sa  ->  local.cerebra.sa
        String[] parts = baseDn.split(",");
        StringBuilder domain = new StringBuilder();
        boolean first = true;
        for (String p : parts) {
            p = p.trim();
            if (p.regionMatches(true, 0, "DC=", 0, 3)) {
                String dcVal = p.substring(3);
                if (!first) domain.append('.');
                domain.append(dcVal);
                first = false;
            }
        }
        // fallback
        if (domain.isEmpty()) return "domain.local";
        return domain.toString();
    }
}

