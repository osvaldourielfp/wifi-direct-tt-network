package edu.rit.se.crashavoidance.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import java.util.ArrayList;

/**
 * Created by osvaldo on 21/05/17.
 */

public class DBManager {
    private DatabaseHelper dbHelper;
    private Context context;
    private SQLiteDatabase database;

    public DBManager(Context c) {
        context = c;
    }

    public DBManager open() throws SQLException {
        dbHelper = new DatabaseHelper(context);
        database = dbHelper.getWritableDatabase();
        return this;
    }

    public void close() {
        dbHelper.close();
    }

    public void insert(User usuario) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.ID_USER, usuario.id_user);
        contentValue.put(DatabaseHelper.CURP, usuario.curp);
        contentValue.put(DatabaseHelper.NAME, usuario.name);
        contentValue.put(DatabaseHelper.TYPE_USER, usuario.type_user);
        contentValue.put(DatabaseHelper.MAC, usuario.mac);
        contentValue.put(DatabaseHelper.COORDS, usuario.coords);
        contentValue.put(DatabaseHelper.RUTE, usuario.rute);

        database.insert(DatabaseHelper.TABLE_NAME_USERS, null, contentValue);
    }

    public Cursor fetchUsuario(String[] selectionArgs) { // id_usuario
        String selection = DatabaseHelper.ID_USER + " = ? ";
        String[] columns = new String[] {
                DatabaseHelper.ID_USER,
                DatabaseHelper.CURP,
                DatabaseHelper.NAME,
                DatabaseHelper.TYPE_USER,
                DatabaseHelper.MAC,
                DatabaseHelper.COORDS,
                DatabaseHelper.RUTE
        };
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_NAME_USERS, columns, selection, selectionArgs,
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public Cursor fetchUsuarioName(String[] selectionArgs) { // name
        String selection = DatabaseHelper.ID_USER + " = ? ";
        String[] columns = new String[] {
                DatabaseHelper.ID_USER,
                DatabaseHelper.CURP,
                DatabaseHelper.NAME,
                DatabaseHelper.TYPE_USER,
                DatabaseHelper.MAC,
                DatabaseHelper.COORDS,
                DatabaseHelper.RUTE
        };
        Cursor cursor = database.query(
                DatabaseHelper.TABLE_NAME_USERS, columns, selection, selectionArgs,
                null, null, null, null);

        if (cursor != null) {
            cursor.moveToFirst();
        }
        return cursor;
    }

    public boolean existsUser(String[] selectionArgs) {
        String selection = DatabaseHelper.TYPE_USER + " = ?";
        String[] columns = new String[] {
                DatabaseHelper.ID_USER,
                DatabaseHelper.CURP,
                DatabaseHelper.NAME,
                DatabaseHelper.TYPE_USER,
                DatabaseHelper.MAC,
                DatabaseHelper.COORDS,
                DatabaseHelper.RUTE
        };

        Cursor cursor = database.query(
                DatabaseHelper.TABLE_NAME_USERS, columns, selection, selectionArgs,
                null, null, null, null);

        if (cursor == null) {
            return false;
        }

        cursor.moveToFirst(); //TODO: Verify how it works

        return !cursor.isAfterLast();
    }

    public ArrayList<User> fetchTypeUsers(String[] selectionArgs) { // type_user
        String selection = DatabaseHelper.TYPE_USER + " = ?";
        String[] columns = new String[] {
                DatabaseHelper.ID_USER,
                DatabaseHelper.CURP,
                DatabaseHelper.NAME,
                DatabaseHelper.TYPE_USER,
                DatabaseHelper.MAC,
                DatabaseHelper.COORDS,
                DatabaseHelper.RUTE
        };

        Cursor cursor = database.query(
                DatabaseHelper.TABLE_NAME_USERS, columns, selection, selectionArgs,
                null, null, null, null);

        if (cursor == null) {
            return null;
        }

        ArrayList<User> users = new ArrayList<>();
        User u;

        cursor.moveToFirst();
        while (cursor.isAfterLast() == false) {
            u = new User();

            u.id_user = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.ID_USER));
            u.curp = cursor.getString(cursor.getColumnIndex(DatabaseHelper.CURP));
            u.name = cursor.getString(cursor.getColumnIndex(DatabaseHelper.NAME));
            u.type_user = cursor.getInt(cursor.getColumnIndex(DatabaseHelper.TYPE_USER));
            u.mac = cursor.getString(cursor.getColumnIndex(DatabaseHelper.MAC));
            u.coords = cursor.getString(cursor.getColumnIndex(DatabaseHelper.COORDS));
            u.rute = cursor.getString(cursor.getColumnIndex(DatabaseHelper.RUTE));

            users.add(u);
            cursor.moveToNext();
        }

        return users;
    }

    public int update(User usuario) {
        ContentValues contentValue = new ContentValues();
        contentValue.put(DatabaseHelper.ID_USER, usuario.id_user);
        contentValue.put(DatabaseHelper.CURP, usuario.curp);
        contentValue.put(DatabaseHelper.NAME, usuario.name);
        contentValue.put(DatabaseHelper.TYPE_USER, usuario.type_user);
        contentValue.put(DatabaseHelper.MAC, usuario.mac);
        contentValue.put(DatabaseHelper.COORDS, usuario.coords);
        contentValue.put(DatabaseHelper.RUTE, usuario.rute);

        int i = database.update(DatabaseHelper.TABLE_NAME_USERS, contentValue,
                DatabaseHelper.ID_USER + " = " + usuario.id_user, null);
        return i;
    }

    public void delete(User usuario){
        database.delete(
                DatabaseHelper.TABLE_NAME_USERS,
                DatabaseHelper.ID_USER + " = " + usuario.id_user, null);
    }
}

