package jp.januaraid.android.synciteasy.core;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import jp.januaraid.android.synciteasy.backend.CloudBackend;
import jp.januaraid.android.synciteasy.backend.CloudEntity;
import jp.januaraid.android.synciteasy.backend.CloudQuery;
import jp.januaraid.android.synciteasy.backend.CloudQuery.Order;
import jp.januaraid.android.synciteasy.backend.CloudQuery.Scope;
import jp.januaraid.android.synciteasy.backend.Filter;
import jp.januaraid.android.synciteasy.gcm.GCMIntentService;

import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential;
import android.accounts.Account;
import android.app.Application;
import android.content.AbstractThreadedSyncAdapter;
import android.content.ContentProviderClient;
import android.content.ContentProviderOperation;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.OperationApplicationException;
import android.content.SyncResult;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.RemoteException;
import android.util.Log;

public class SyncAdapter extends AbstractThreadedSyncAdapter {
	private static final SimpleDateFormat sdf = new SimpleDateFormat(
			"yyyy-MM-dd HH:mm:ss.SSS", Locale.US);
	private String accountName;
	private ContentResolver mContentResolver;
	private ArrayList<ContentProviderOperation> operations;
	private static final Map<String, CloudQuery> continuousQueries = new HashMap<String, CloudQuery>();

	private GoogleAccountCredential mCredential;
	private CloudBackend mCloudBackend;
	private Application application;

	public SyncAdapter(Context context, boolean autoInitialize) {
		super(context, autoInitialize);
		Log.d(Consts.TAG, ":SyncAdapter");
		mContentResolver = context.getContentResolver();
		this.application = (Application) context;
	}

	public SyncAdapter(Context context, boolean autoInitialize,
			boolean allowParallelSyncs) {
		super(context, autoInitialize, allowParallelSyncs);
		Log.d(Consts.TAG, ":SyncAdapter");
		mContentResolver = context.getContentResolver();
	}

	@Override
	public void onPerformSync(Account account, Bundle extras, String authority,
			ContentProviderClient provider, SyncResult syncResult) {
		Log.d(Consts.TAG,
				":onPerformSync "
						+ account.toString()
						+ authority
						+ extras.getString("token")
						+ getContext().getSharedPreferences(
								Consts.PREF_KEY_CLOUD_BACKEND,
								Context.MODE_PRIVATE).getString(
								Consts.PREF_KEY_ACCOUNT_NAME, null));
		if (!Consts.IS_SYNC) {
			Log.d(Consts.TAG, "IS_SYNC is false");
			return;
		}

		mCloudBackend = new CloudBackend();
		if (this.application != null) {
			GCMIntentService.getRegistrationId(this.application);
		}
		mCredential = GoogleAccountCredential.usingAudience(getContext(),
				Consts.AUTH_AUDIENCE);
		mCloudBackend.setCredential(mCredential);

		if (Consts.IS_AUTH_ENABLED) {
			Log.d(Consts.TAG, "isAuth = true");
			accountName = getContext().getSharedPreferences(
					Consts.PREF_KEY_CLOUD_BACKEND, Context.MODE_PRIVATE)
					.getString(Consts.PREF_KEY_ACCOUNT_NAME, null);
			if (accountName == null) {
				Log.d(Consts.TAG, "accountName == null");
				return;
			} else {
				Log.d(Consts.TAG, accountName);
				mCredential.setSelectedAccountName(accountName);
			}
		} else {
			Log.d(Consts.TAG, "isAuth = false");
			return;
		}

		Log.d(Consts.TAG, "DataChange = " + extras.getBoolean("DataChange"));
		if (extras.getBoolean("DataChange")) {
			uploadEntity();
			return;
		} else {
			uploadEntity();
		}

		String token = extras.getString("token");
		if (token == null) {
			CloudQuery query = new CloudQuery(Consts.KIND_NAME);
			query.setRegId(GCMIntentService.getRegistrationId(application));
			query.setFilter(Filter.eq(CloudEntity.PROP_CREATED_BY, accountName));
			query.setScope(Scope.FUTURE_AND_PAST);
			query.setSort(CloudEntity.PROP_CREATED_BY, Order.ASC);
			query.setSubscriptionDurationSec(Consts.SUBSCRIPTION_DURATION_SEC);
			query.setLimit(Consts.QUERY_LIMIT);
			continuousQueries.put(query.getQueryId(), query);
			getEntity(query);
		} else {
			Log.i(Consts.TAG, "A message has been recieved of token: " + token);
			CloudQuery cq = continuousQueries.get(token);
			if (cq == null) {
				Log.i(Consts.TAG, "cq == null");
				cq = new CloudQuery(Consts.KIND_NAME);
				cq.setRegId(GCMIntentService.getRegistrationId(application));
				cq.setFilter(Filter
						.eq(CloudEntity.PROP_CREATED_BY, accountName));
				cq.setSort(CloudEntity.PROP_CREATED_BY, Order.ASC);
				cq.setSubscriptionDurationSec(Consts.SUBSCRIPTION_DURATION_SEC);
				cq.setLimit(Consts.QUERY_LIMIT);
				continuousQueries.put(cq.getQueryId(), cq);
			}
			cq.setScope(Scope.PAST);
			getEntity(cq);
		}

	}

	private void uploadEntity() {
		operations = new ArrayList<ContentProviderOperation>();
		int idx;
		String selection;
		String[] selectionArgs = new String[1];
		for (int i = 0; i < Consts.TABLE_NAME.length; i++) {
			if (Consts.TABLE_ISSYNC[i] == true) {
				Uri uri = Uri.parse(Consts.URI + Consts.TABLE_NAME[i]);
				Log.d(Consts.TAG, uri.toString());
				selection = Consts.SYNCED + " = ?";
				selectionArgs[0] = "false";
				Cursor c = mContentResolver.query(uri, null, selection,
						selectionArgs, null);
				try {
					boolean isEof = c.moveToFirst();
					Log.d(Consts.TAG, "ColumnCount : " + c.getCount()
							+ "c.moveToFirst() : " + isEof);
					CloudEntity newPost, result;
					while (isEof) {
						newPost = new CloudEntity(Consts.KIND_NAME);
						result = null;
						idx = c.getColumnIndexOrThrow(Consts.ID);
						if (c.getString(idx).equals("null")) {
							Log.d(Consts.TAG, "Insert to Cloud");
							newPost.put(Consts.TABLE, Consts.TABLE_NAME[i]);
							idx = c.getColumnIndexOrThrow(Consts.LOCAL_DATE);
							newPost.put(Consts.LOCAL_DATE, c.getString(idx));
							idx = c.getColumnIndexOrThrow(Consts.DELETED);
							newPost.put(Consts.DELETED, c.getString(idx));
							for (int j = 0; j < Consts.COLUMN_NAME[i].length; j++) {
								idx = c.getColumnIndex(Consts.COLUMN_NAME[i][j]);
								if (idx == -1) {
									continue;
								}
								Log.d(Consts.TAG, c.getString(idx));
								newPost.put(Consts.COLUMN_NAME[i][j],
										c.getString(idx));
							}
							try {
								result = mCloudBackend.insert(newPost);
							} catch (IOException e) {
								e.printStackTrace();
							}

						} else {
							Log.d(Consts.TAG, "Update to Cloud");
							try {
								newPost.put(Consts.TABLE, Consts.TABLE_NAME[i]);
								newPost.setId(c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.CREATED_AT);
								newPost.setCreatedAt(sdf.parse(c.getString(idx)));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.UPDATED_AT);
								newPost.setUpdatedAt(sdf.parse(c.getString(idx)));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.CREATED_BY);
								newPost.setCreatedBy(c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.UPDATED_BY);
								newPost.setUpdatedBy(c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.OWNER);
								newPost.setOwner(c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.LOCAL_DATE);
								newPost.put(Consts.LOCAL_DATE, c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.SYNCED);
								Log.d(Consts.TAG, c.getString(idx));
								idx = c.getColumnIndexOrThrow(Consts.DELETED);
								newPost.put(Consts.DELETED, c.getString(idx));
								Log.d(Consts.TAG, c.getString(idx));
								for (int j = 0; j < Consts.COLUMN_NAME[i].length; j++) {
									idx = c.getColumnIndex(Consts.COLUMN_NAME[i][j]);
									if (idx == -1) {
										continue;
									}
									newPost.put(Consts.COLUMN_NAME[i][j],
											c.getString(idx));
									Log.d(Consts.TAG, Consts.COLUMN_NAME[i][j]
											+ " : " + c.getString(idx));
								}
								try {
									result = mCloudBackend.update(newPost);
								} catch (IOException e) {
									e.printStackTrace();
								}
							} catch (IllegalArgumentException e) {
								e.printStackTrace();
							} catch (ParseException e) {
								e.printStackTrace();
							}
						}
						if (result != null) {
							Log.d(Consts.TAG, "result != null");
							Log.d(Consts.TAG, result.toString());
							selection = "_id = ?";
							idx = c.getColumnIndexOrThrow("_id");
							selectionArgs[0] = c.getString(idx);
							ContentValues values = new ContentValues();
							values.put(Consts.ID, result.getId());
							values.put(Consts.CREATED_AT,
									sdf.format(result.getCreatedAt()));
							values.put(Consts.UPDATED_AT,
									sdf.format(result.getUpdatedAt()));
							values.put(Consts.CREATED_BY, result.getCreatedBy());
							values.put(Consts.UPDATED_BY, result.getUpdatedBy());
							values.put(Consts.OWNER, result.getOwner());
							values.put(Consts.LOCAL_DATE,
									(String) result.get(Consts.LOCAL_DATE));
							values.put(Consts.SYNCED, "true");
							values.put(Consts.DELETED,
									(String) result.get(Consts.DELETED));
							for (int j = 0; j < Consts.COLUMN_NAME[i].length; j++) {
								values.put(Consts.COLUMN_NAME[i][j],
										(String) result
												.get(Consts.COLUMN_NAME[i][j]));
							}
							operations.add(ContentProviderOperation
									.newUpdate(uri)
									.withSelection(selection, selectionArgs)
									.withValues(values).build());
						}
						isEof = c.moveToNext();
					}
				} finally {
					c.close();
				}

			}
		}
		try {
			mContentResolver.applyBatch(Consts.AUTHORITY, operations);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}

	}

	private void getEntity(CloudQuery query) {
		operations = new ArrayList<ContentProviderOperation>();
		int idx;
		String selection;
		String[] selectionArgs = new String[1];
		List<CloudEntity> coList = new LinkedList<CloudEntity>();
		try {
			coList = mCloudBackend.list(query);
		} catch (IOException e) {
			e.printStackTrace();
		}

		if (!coList.isEmpty()) {
			Log.d(Consts.TAG, "coList.isEmpty() == false");
			for (CloudEntity result : coList) {
				Log.d(Consts.TAG, result.toString()
						+ result.getCreatedAt().toString());

				for (int i = 0; i < Consts.TABLE_NAME.length; i++) {
					if (Consts.TABLE_NAME[i].equals(result.get(Consts.TABLE))) {
						ContentValues values = new ContentValues();
						values.put(Consts.ID, result.getId());
						values.put(Consts.CREATED_AT,
								sdf.format(result.getCreatedAt()));
						values.put(Consts.UPDATED_AT,
								sdf.format(result.getUpdatedAt()));
						values.put(Consts.CREATED_BY, result.getCreatedBy());
						values.put(Consts.UPDATED_BY, result.getUpdatedBy());
						values.put(Consts.OWNER, result.getOwner());
						values.put(Consts.SYNCED, "true");
						if (result.get(Consts.DELETED) != null) {
							values.put(Consts.DELETED,
									(String) result.get(Consts.DELETED));
						}
						if (result.get(Consts.LOCAL_DATE) != null) {
							values.put(Consts.LOCAL_DATE,
									(String) result.get(Consts.LOCAL_DATE));
						}
						for (int j = 0; j < Consts.COLUMN_NAME[i].length; j++) {
							values.put(Consts.COLUMN_NAME[i][j],
									(String) result
											.get(Consts.COLUMN_NAME[i][j]));
						}

						Uri uri = Uri.parse(Consts.URI + Consts.TABLE_NAME[i]);
						selection = Consts.ID + " LIKE ?";
						selectionArgs[0] = result.getId();

						Cursor c = mContentResolver.query(uri, null, selection,
								selectionArgs, null);
						try {
							c.moveToFirst();
							if (c.getCount() < 1) {
								Log.d(Consts.TAG, "Insert from Cloud");
								operations.add(ContentProviderOperation
										.newInsert(uri).withValues(values)
										.build());
							} else {
								Log.d(Consts.TAG, "Update from Cloud");
								try {
									idx = c.getColumnIndexOrThrow(Consts.LOCAL_DATE);
									java.util.Date localDate = sdf.parse(c
											.getString(idx));
									java.util.Date remoteDate = sdf
											.parse("0000-00-00 00:00:00.000");
									if (result.get(Consts.LOCAL_DATE) != null) {
										remoteDate = sdf.parse((String) result
												.get(Consts.LOCAL_DATE));
									}
									if (localDate.compareTo(remoteDate) == 0) {
										Log.d(Consts.TAG,
												sdf.format(localDate)
														+ " == "
														+ sdf.format(remoteDate));
									} else if (localDate.compareTo(remoteDate) < 0) {
										Log.d(Consts.TAG,
												sdf.format(localDate)
														+ " < "
														+ sdf.format(remoteDate));
										operations.add(ContentProviderOperation
												.newUpdate(uri)
												.withValues(values)
												.withSelection(selection,
														selectionArgs).build());
										Log.d(Consts.TAG, selection
												+ selectionArgs[0]);
									} else {
										Log.d(Consts.TAG,
												sdf.format(localDate)
														+ " > "
														+ sdf.format(remoteDate));
										values = new ContentValues();
										values.put(Consts.ID, result.getId());
										operations.add(ContentProviderOperation
												.newUpdate(uri)
												.withValues(values)
												.withSelection(selection,
														selectionArgs).build());
									}
								} catch (IllegalArgumentException e) {
									e.printStackTrace();
								} catch (ParseException e) {
									e.printStackTrace();
								}
							}
						} finally {
							c.close();
						}
					}
				}
			}
		} else {
			Log.d(Consts.TAG, "coList.isEmpty() == true");
			return;
		}
		try {
			mContentResolver.applyBatch(Consts.AUTHORITY, operations);
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (OperationApplicationException e) {
			e.printStackTrace();
		}

	}

}
