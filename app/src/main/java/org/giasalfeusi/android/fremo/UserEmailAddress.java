package org.giasalfeusi.android.fremo;

/**
 * Created by salvy on 08/04/17.
 */

import android.accounts.Account;
import android.accounts.AccountManager;
import android.content.Context;

/**
 * This class uses the AccountManager to get the primary email address of the
 * current user.
 */
public class UserEmailAddress {
    static String getEmail(Context context) {
        AccountManager accountManager = AccountManager.get(context);
        Account account = getAccount(accountManager);

        if (account == null) {
            return null;
        } else {
            return account.name;
        }
    }

    private static Account getAccount(AccountManager accountManager) {

        Account account = null;

        try {
            Account[] accounts = accountManager.getAccountsByType("com.google");
            if (accounts.length > 0) {
                account = accounts[0];
            } else {
                account = null;
            }
        }
        catch (SecurityException e)
        {
            // No valid mail provided
        }

        return account;
    }
}
