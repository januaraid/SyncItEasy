package jp.januaraid.android.synciteasy.core;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

import android.accounts.Account;
import android.content.ContentProvider;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

public class DoContentProvider extends ContentProvider implements Consts {
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
	private static final UriMatcher sUriMatcher = new UriMatcher(0);
	private DoDbHelper mDoHelper;
	private SQLiteDatabase db;

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		ContentValues values = new ContentValues();
		values.put(Consts.DELETED, "true");
		return update(uri, values, selection, selectionArgs);
	}

	@Override
	public String getType(Uri uri) {
		return null;
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		int i = sUriMatcher.match(uri);
		if (i == -1) {
			Log.i(TAG, "there is no matched node.");
			return null;
		}
		Date date = new Date();
		long id;
		String syncflag = values.getAsString(SYNCED);
		if (syncflag == null || syncflag.isEmpty()) {
			Log.i(Consts.TAG, "syncflag is empty!");
			values.put(Consts.LOCAL_DATE, sdf.format(date));
			db = mDoHelper.getWritableDatabase();
			id = db.insert(TABLE_NAME[i], null, values);
			_requestSyuc();
		} else {
			Log.i(Consts.TAG, "Update : syncflag is not empty!");
			db = mDoHelper.getWritableDatabase();
			id = db.insert(TABLE_NAME[i], null, values);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return ContentUris.withAppendedId(uri, id);
	}

	@Override
	public boolean onCreate() {
		mDoHelper = new DoDbHelper(getContext());
		for (int i = 0; i < TABLE_NAME.length; i++) {
			sUriMatcher.addURI(Consts.AUTHORITY, TABLE_NAME[i], i);
		}
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder) {
		int i = sUriMatcher.match(uri);
		if (i == -1) {
			Log.i(Consts.TAG, "there is no matched node.");
			return null;
		}
		Log.d(this.getClass().getName(), "" + i);
		db = mDoHelper.getReadableDatabase();
		Cursor c = db.query(TABLE_NAME[i], projection, selection,
				selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	public Cursor query(Uri uri, String[] projection, String selection,
			String[] selectionArgs, String sortOrder, Boolean flag) {
		int i = sUriMatcher.match(uri);
		if (i == -1) {
			Log.i(Consts.TAG, "there is no matched node.");
			return null;
		}
		Log.d(this.getClass().getName(), "" + TABLE_NAME[i]);
		db = mDoHelper.getReadableDatabase();
		Cursor c = db.query(TABLE_NAME[i], projection, selection,
				selectionArgs, null, null, sortOrder);
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection,
			String[] selectionArgs) {
		int i = sUriMatcher.match(uri);
		if (i == -1) {
			Log.i(Consts.TAG, "there is no matched node.");
			return 0;
		}
		Date date = new Date();
		int count;
		String syncflag = values.getAsString(SYNCED);
		if (syncflag == null || syncflag.isEmpty()) {
			Log.i(Consts.TAG, "syncflag is empty!");
			values.put(Consts.LOCAL_DATE, sdf.format(date));
			values.put(SYNCED, "false");
			db = mDoHelper.getWritableDatabase();
			count = db.update(TABLE_NAME[i], values, selection, selectionArgs);
			_requestSyuc();
		} else {
			Log.i(Consts.TAG, "Update : syncflag is not empty! : " + syncflag
					+ selection + selectionArgs[0] + TABLE_NAME[i]);
			db = mDoHelper.getWritableDatabase();
			count = db.update(TABLE_NAME[i], values, selection, selectionArgs);
		}
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

	@Override
	public ContentProviderResult[] applyBatch(
			ArrayList<ContentProviderOperation> operations)
			throws OperationApplicationException {
		SQLiteDatabase db = mDoHelper.getWritableDatabase();

		db.beginTransaction();
		try {
			ContentProviderResult[] result = super.applyBatch(operations);
			db.setTransactionSuccessful();
			return result;
		} finally {
			db.endTransaction();
		}
	}
	
	private void _requestSyuc() {
		String accountName = getContext()
				.getSharedPreferences(
						Consts.PREF_KEY_CLOUD_BACKEND,
						Context.MODE_PRIVATE).getString(
						Consts.PREF_KEY_ACCOUNT_NAME, null);
		Account account = null;
		if (accountName == null) {
			return;
		} else {
			account = new Account(accountName, Consts.ACCOUNT_TYPE);
		}
		Bundle settingsBundle = new Bundle();
		settingsBundle.putBoolean(
				ContentResolver.SYNC_EXTRAS_MANUAL, true);
		settingsBundle.putBoolean(
				ContentResolver.SYNC_EXTRAS_EXPEDITED, true);
		settingsBundle.putBoolean("DataChange", true);
		ContentResolver.requestSync(account, Consts.AUTHORITY,
				settingsBundle);
	}

}
