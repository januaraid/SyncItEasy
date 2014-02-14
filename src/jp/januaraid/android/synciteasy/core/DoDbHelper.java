package jp.januaraid.android.synciteasy.core;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

public class DoDbHelper extends SQLiteOpenHelper {

	public DoDbHelper(Context context) {
		super(context, Consts.DATABASE_NAME, null, Consts.DATABASE_VERSION);
	}

	@Override
	public void onCreate(SQLiteDatabase db) {
		db.beginTransaction();
		try {
			String[] sqls = SQLFactory.create();
			for (int i = 0; i < sqls.length; i++) {
				db.execSQL(sqls[i]);
			}
			db.setTransactionSuccessful();
			Log.d(getDatabaseName(), "" + SQLFactory.create());
		} finally {
			db.endTransaction();
		}
	}

	@Override
	public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
		db.beginTransaction();
		try {
			String[] sqls = SQLFactory.delete();
			for (int i = 0; i < sqls.length; i++) {
				db.execSQL(sqls[i]);
			}
			db.setTransactionSuccessful();
			Log.d(getDatabaseName(), "" + SQLFactory.delete());
		} finally {
			db.endTransaction();
		}
        onCreate(db);
	}

}
