package org.sqlite.android;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * SQLite SQL restore utility.
 */
public class SQLRestore {
    BufferedReader is;
    Database db;

    public SQLRestore(InputStream is, Database db) {
        this.is = new BufferedReader(new InputStreamReader(is));
        this.db = db;
    }

    public void restore() throws org.sqlite.android.Exception {
        String line = null, sql = null;
        while (true) {
            try {
                line = is.readLine();
            } catch (EOFException e) {
                line = null;
            } catch (IOException e) {
                throw new org.sqlite.android.Exception("I/O error");
            }
            if (line == null) {
                break;
            }
            if (sql == null) {
                sql = line;
            } else {
                sql = sql + " " + line;
            }
            if (Database.complete(sql)) {
                db.exec(sql, null);
                sql = null;
            }
        }
        if (sql != null) {
            throw new org.sqlite.android.Exception("Incomplete SQL: " + sql);
        }
    }
}
