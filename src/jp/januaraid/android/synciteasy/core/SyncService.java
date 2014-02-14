package jp.januaraid.android.synciteasy.core;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class SyncService extends Service {
	private static SyncAdapter sSyncAdapter = null;
	private static final Object sSyncAdapterLock = new Object();
	
	@Override
	public void onCreate() {
		Log.d(Consts.TAG, "SyncService start");
		super.onCreate();
		synchronized (sSyncAdapterLock) {
            if (sSyncAdapter == null) {
                sSyncAdapter = new SyncAdapter(getApplicationContext(), true);
            }
        }
	}

	@Override
	public IBinder onBind(Intent intent) {
		return sSyncAdapter.getSyncAdapterBinder();
	}

}
