package edu.rit.se.crashavoidance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * Created by osvaldo on 21/05/17.
 */

public class DatabaseHelper extends SQLiteOpenHelper {
    public static final String TABLE_NAME_USERS = "usuarios";

    public static final String ID_USER = "id_usuario";
    public static final String CURP = "curp";
    public static final String NAME = "name";
    public static final String TYPE_USER = "type_user";
    public static final String MAC = "mac";
    public static final String COORDS = "coordenadas";
    public static final String RUTE = "ruta";

    static final String DB_NAME = "seeker.db";

    static final int DB_VERSION = 1;

    private static final String CREATE_TABLE_USERS = "create table "+
            TABLE_NAME_USERS  + " (" +
            ID_USER     + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            CURP        + " TEXT NOT NULL, " +
            NAME        + " TEXT NOT NULL, " +
            TYPE_USER   + " INTEGER NOT NULL, " +
            MAC         + " TEXT NOT NULL, " +
            COORDS      + " TEXT NOT NULL, " +
            RUTE        + " TEXT NOT NULL);";

    public DatabaseHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE_USERS);

        ContentValues values = new ContentValues();
        values.put(CURP, "TEST001122MMEMME96");
        values.put(NAME, "Name Lastn");
        values.put(TYPE_USER, UserType.MYSELF.getCode()); //Myself
        values.put(MAC, "00:00:00:00:00:00");
        values.put(COORDS, "");
        values.put(RUTE, "");
        db.insert(TABLE_NAME_USERS, null, values);

        addUser(db, UserType.USER.getCode(), "0"); // User
        addUser(db, UserType.USER.getCode(), "1");
        addUser(db, UserType.USER.getCode(), "2");

        addUser(db, UserType.CONTACT.getCode(), "3"); // Contact
        addUser(db, UserType.CONTACT.getCode(), "4");
        addUser(db, UserType.CONTACT.getCode(), "5");

        addUser(db, UserType.FIND.getCode(), "6"); // Find
        addUser(db, UserType.FIND.getCode(), "7");
        addUser(db, UserType.FIND.getCode(), "8");
    }

    public void addUser(SQLiteDatabase db, int type, String diff) {
        ContentValues values = new ContentValues();
        values.put(CURP, "TEST1234RRRAAA0" + diff);
        values.put(NAME, "Nombre Apellido " + diff);
        values.put(TYPE_USER, type);
        values.put(MAC, "");
        values.put(COORDS, "");
        values.put(RUTE, "");
        db.insert(TABLE_NAME_USERS, null, values);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME_USERS);
        onCreate(db);
    }
}
