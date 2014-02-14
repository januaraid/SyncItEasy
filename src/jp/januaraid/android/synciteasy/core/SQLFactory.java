package jp.januaraid.android.synciteasy.core;

public class SQLFactory {

	public static String[] create() {
		String[] sqls = new String[Consts.TABLE_NAME.length];
		StringBuffer sql;
		for (int i = 0; i < Consts.TABLE_NAME.length; i++) {
			sql = new StringBuffer();
			sql.append("CREATE TABLE " + Consts.TABLE_NAME[i] + "("
					+ "_id INTEGER PRIMARY KEY AUTOINCREMENT," +
					"id TEXT DEFAULT 'null'," +
					"createdAt TEXT DEFAULT 'null'," +
					"updatedAt TEXT DEFAULT 'null'," +
					"createdBy TEXT DEFAULT 'null'," +
					"updatedBy TEXT DEFAULT 'null'," +
					"owner TEXT DEFAULT 'null'," +
					"localDate TEXT DEFAULT '0000-00-00 00:00:00.000'," +
					"synced TEXT DEFAULT 'false'," +
					"deleted TEXT DEFAULT 'false',");
			for (int j = 0; j < Consts.COLUMN_NAME[i].length; j++) {
				sql.append(Consts.COLUMN_NAME[i][j] + " "
						+ Consts.COLUMN_TYPE[i][j]);
				if ((Consts.COLUMN_NAME[i].length - j) > 1) {
					sql.append(",");
				}
			}
			sql.append(");");
			sqls[i] = sql.toString();
		}
		return sqls;
	}
	
	public static String[] delete() {
		String[] sqls = new String[Consts.TABLE_NAME.length];
		StringBuffer sql = new StringBuffer();
		for (int i = 0; i < Consts.TABLE_NAME.length; i++) {
			sql.append("DROP TABLE IF EXISTS " + Consts.TABLE_NAME[i] + ";");
			sqls[i] = "DROP TABLE IF EXISTS " + Consts.TABLE_NAME[i] + ";";
		}
		return sqls;
	}
}
