package jp.januaraid.android.synciteasy.core;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.app.Fragment;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;

public class SignIn extends Fragment implements Consts {
	
	public static final String PROCESSING_FRAGMENT_TAG = "BACKEND_FRAGMENT";
	private static final int REQUEST_ACCOUNT_PICKER = 2;
	private GoogleAccountCredential mCredential;

	@Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Log.d(Consts.TAG, getClass().getSimpleName()+":onCreate");
        mCredential = GoogleAccountCredential.usingAudience(getActivity(), Consts.AUTH_AUDIENCE);
        signInAndSubscribe(false);
    }

	public void signInAndSubscribe(boolean overrideCurrent) {
		Log.d(Consts.TAG, getClass().getSimpleName()+":signInAndSubscribe");
		if (Consts.IS_AUTH_ENABLED) {
			String accountName = getSharedPreferences().getString(
					Consts.PREF_KEY_ACCOUNT_NAME, null);
			if (accountName == null || overrideCurrent) {
				super.startActivityForResult(
						mCredential.newChooseAccountIntent(),
						REQUEST_ACCOUNT_PICKER);
				return;
			} else {
				 Log.d(Consts.TAG, getClass().getSimpleName()+":"+accountName);
				mCredential.setSelectedAccountName(accountName);
				requestSync();
			}
		}
	}
	
	private void requestSync() {
		ContentResolver.setSyncAutomatically(getAccount(), Consts.AUTHORITY, false);
		Bundle settingsBundle = new Bundle();
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_MANUAL, true);
		settingsBundle.putBoolean(ContentResolver.SYNC_EXTRAS_EXPEDITED,
				true);
		ContentResolver.requestSync(getAccount(), Consts.AUTHORITY, settingsBundle);
	}

	@Override
    public final void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.d(Consts.TAG, getClass().getSimpleName()+":onActivityResult");

        Log.i("result from activity", "resultcode: " + resultCode);
        switch (requestCode) {
            case REQUEST_ACCOUNT_PICKER:
                if (data != null && data.getExtras() != null) {

                    // set the picked account name to the credential
                    String accountName = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
//                    mCredential.setSelectedAccountName(accountName);

                    // save account name to shared pref
                    SharedPreferences.Editor e = getSharedPreferences().edit();
                    e.putString(Consts.PREF_KEY_ACCOUNT_NAME, accountName);
                    e.commit();
                    requestSync();
                }
                break;
        }
    }

	public SharedPreferences getSharedPreferences() {
		Log.d(Consts.TAG, getClass().getSimpleName()+":getSharedPreferences");
		return getActivity().getSharedPreferences(Consts.PREF_KEY_CLOUD_BACKEND,
				Context.MODE_PRIVATE);
	}
	
	public Account getAccount() {
		Log.d(Consts.TAG, getClass().getSimpleName()+":getAccount");
		String accountName = getSharedPreferences().getString(
				Consts.PREF_KEY_ACCOUNT_NAME, null);
		if (accountName == null) {
			return null;
		} else {
			Account account = new Account(accountName, ACCOUNT_TYPE);
			return account;
		}
		
	}
}
