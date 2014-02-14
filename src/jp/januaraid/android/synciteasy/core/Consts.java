package jp.januaraid.android.synciteasy.core;

public interface Consts {
	/*
	 * Database version.
	 */
	public static final int DATABASE_VERSION = 1;
	
	public static final String DATABASE_NAME = "*** ENTER DATABASE NAME ***";
	
	public static final String[] TABLE_NAME = {"*** ENTER TABLE NAME ***"};
	
	public static final boolean[] TABLE_ISSYNC = {true,};
	
	public static final String[][] COLUMN_NAME = 
												{
													{"*** ENTER COLUMN NAME ***"}
												};
	
	public static final String[][] COLUMN_TYPE = 
												{
													{"*** ENTER DATABASE NAME ***"}
												};
	
	public static final String AUTHORITY = "*** ENTER YOUR AUTORITY NAME ***";
	
	public static final String URI = "content://" + AUTHORITY + "/";
	
	public static final String TAG = "Sync it Easy";
	
	public static final boolean IS_SYNC = true;
	
	/**
     * Set Project ID of your Google APIs Console Project.
     */
    public static final String PROJECT_ID = "*** ENTER YOUR PROJECT ID ***";

    /**
     * Set Project Number of your Google APIs Console Project.
     */
    public static final String PROJECT_NUMBER = "*** ENTER YOUR PROJECT NUMBER ***";

    /**
     * Set your Web Client ID for authentication at backend.
     */
    public static final String WEB_CLIENT_ID = "*** ENTER YOUR WEB CLIENT ID ***";

    /**
     * Set default user authentication enabled or disabled.
     */
    public static final boolean IS_AUTH_ENABLED = true;

    /**
     * Auth audience for authentication on backend.
     */
    public static final String AUTH_AUDIENCE = "server:client_id:" + WEB_CLIENT_ID;

    /**
     * Endpoint root URL
     */
    public static final String ENDPOINT_ROOT_URL = "https://" + PROJECT_ID
            + ".appspot.com/_ah/api/";

    /**
     * A flag to switch if the app should be run with local dev server or
     * production (cloud).
     */
    public static final boolean LOCAL_ANDROID_RUN = false;

    /**
     * SharedPreferences keys for CloudBackend.
     */
    public static final String PREF_KEY_CLOUD_BACKEND = "PREF_KEY_CLOUD_BACKEND";
    public static final String PREF_KEY_ACCOUNT_NAME = "PREF_KEY_ACCOUNT_NAME";
    
    /**
     * Continuous query duration second.
     * Default 30 minutes.
     */
    public static final int SUBSCRIPTION_DURATION_SEC = 1800;
    public static final int QUERY_LIMIT = 10000;
    
    public static final String KIND_NAME = "Sync_it_Easy";
    
    public static final String ID = "id";
    public static final String CREATED_AT = "createdAt";
    public static final String UPDATED_AT = "updatedAt";
    public static final String CREATED_BY = "createdBy";
    public static final String UPDATED_BY = "updatedBy";
    public static final String OWNER = "owner";
    public static final String SYNCED = "synced";
    public static final String DELETED = "deleted";
    public static final String LOCAL_DATE = "localDate";
    
    public static final String ACCOUNT_TYPE = "com.google";
    public static final String TABLE = "tablename";

}
