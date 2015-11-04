package org.sqlite.android;

/**
 * Class to represent compiled SQLite3 statement.
 * 
 * Note, that all native methods of this class are not synchronized, i.e. it is up to the caller to
 * ensure that only one thread is in these methods at any one time.
 */
public class Stmt {
	/**
	 * Internal handle for the SQLite3 statement.
	 */
	private long handle = 0;

	/**
	 * Internal last error code for prepare()/step() methods.
	 */
	protected int error_code = 0;

	/**
	 * Prepare the next SQL statement for the Stmt instance.
	 * 
	 * @return true when the next piece of the SQL statement sequence has been prepared, false on end of
	 *         statement sequence.
	 */
	public native boolean prepare() throws org.sqlite.android.Exception;

	/**
	 * Perform one step of compiled SQLite3 statement.
	 * 
	 * Example:<BR>
	 * 
	 * <PRE>
	 *   ...
	 *   try {
	 *     Stmt s = db.prepare("select * from x; select * from y;");
	 *     s.bind(...);
	 *     ...
	 *     s.bind(...);
	 *     while (s.step(cb)) {
	 *       Object o = s.value(...);
	 *       ...
	 *     }
	 *     // s.reset() for re-execution or
	 *     // s.prepare() for the next piece of SQL
	 *     while (s.prepare()) {
	 *       s.bind(...);
	 *       ...
	 *       s.bind(...);
	 *       while (s.step(cb)) {
	 *         Object o = s.value(...);
	 *         ...
	 *       }
	 *     }
	 *   } catch (SQLite.Exception e) {
	 *     s.close();
	 *   }
	 * </PRE>
	 * 
	 * @return true when row data is available, false on end of result set.
	 */
	public native boolean step() throws org.sqlite.android.Exception;

	/**
	 * Close the compiled SQLite3 statement.
	 */
	public native void close() throws org.sqlite.android.Exception;

	/**
	 * Reset the compiled SQLite3 statement without clearing parameter bindings.
	 */
	public native void reset() throws org.sqlite.android.Exception;

	/**
	 * Clear all bound parameters of the compiled SQLite3 statement.
	 */
	public native void clear_bindings() throws org.sqlite.android.Exception;

	/**
	 * Bind positional integer value to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param value
	 *            value of parameter
	 */
	public native void bind(int pos, int value) throws org.sqlite.android.Exception;

	/**
	 * Bind positional long value to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param value
	 *            value of parameter
	 */
	public native void bind(int pos, long value) throws org.sqlite.android.Exception;

	/**
	 * Bind positional double value to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param value
	 *            value of parameter
	 */
	public native void bind(int pos, double value) throws org.sqlite.android.Exception;

	/**
	 * Bind positional byte array to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param value
	 *            value of parameter, may be null
	 */
	public native void bind(int pos, byte[] value) throws org.sqlite.android.Exception;

	/**
	 * Bind positional String to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param value
	 *            value of parameter, may be null
	 */
	public native void bind(int pos, String value) throws org.sqlite.android.Exception;

	/**
	 * Bind positional SQL null to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 */
	public native void bind(int pos) throws org.sqlite.android.Exception;

	/**
	 * Bind positional zero'ed blob to compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @param length
	 *            byte size of zero blob
	 */
	public native void bind_zeroblob(int pos, int length)
			throws org.sqlite.android.Exception;

	/**
	 * Return number of parameters in compiled SQLite3 statement.
	 * 
	 * @return int number of parameters
	 */
	public native int bind_parameter_count() throws org.sqlite.android.Exception;

	/**
	 * Return name of parameter in compiled SQLite3 statement.
	 * 
	 * @param pos
	 *            parameter index, 1-based
	 * @return String parameter name
	 */
	public native String bind_parameter_name(int pos) throws org.sqlite.android.Exception;

	/**
	 * Return index of named parameter in compiled SQLite3 statement.
	 * 
	 * @param name
	 *            of parameter
	 * @return int index of parameter, 1-based
	 */
	public native int bind_parameter_index(String name)
			throws org.sqlite.android.Exception;

	/**
	 * Retrieve integer column from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return int column value
	 */
	public native int column_int(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve long column from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return long column value
	 */
	public native long column_long(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve double column from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return double column value
	 */
	public native double column_double(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve blob column from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return byte[] column value
	 */
	public native byte[] column_bytes(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve string column from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String column value
	 */
	public native String column_string(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve column type from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return column type code, e.g. SQLite.Constants.SQLITE_INTEGER
	 */
	public native int column_type(int col) throws org.sqlite.android.Exception;

	/**
	 * Retrieve number of columns of exec'ed SQLite3 statement.
	 * 
	 * @return int number of columns
	 */
	public native int column_count() throws org.sqlite.android.Exception;

	/**
	 * Retrieve column data as object from exec'ed SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return Object or null
	 */
	public Object column(int col) throws org.sqlite.android.Exception {
		switch (column_type(col)) {
			case Constants.SQLITE_INTEGER:
				return new Long(column_long(col));
			case Constants.SQLITE_FLOAT:
				return new Double(column_double(col));
			case Constants.SQLITE_BLOB:
				return column_bytes(col);
			case Constants.SQLITE3_TEXT:
				return column_string(col);
		}
		return null;
	}

	/**
	 * Return name of column of SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String or null
	 */
	public native String column_name(int col) throws org.sqlite.android.Exception;

	/**
	 * Return table name of column of SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String or null
	 */
	public native String column_table_name(int col) throws org.sqlite.android.Exception;

	/**
	 * Return database name of column of SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String or null
	 */
	public native String column_database_name(int col) throws org.sqlite.android.Exception;

	/**
	 * Return declared column type of SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String or null
	 */
	public native String column_decltype(int col) throws org.sqlite.android.Exception;

	/**
	 * Return origin column name of column of SQLite3 statement.
	 * 
	 * @param col
	 *            column number, 0-based
	 * @return String or null
	 */
	public native String column_origin_name(int col) throws org.sqlite.android.Exception;

	/**
	 * Return statement status information.
	 * 
	 * @param op
	 *            which counter to report
	 * @param flg
	 *            reset flag
	 * @return counter
	 */
	public native int status(int op, boolean flg);

	/**
	 * Destructor for object.
	 */
	protected native void finalize();

	/**
	 * Internal native initializer.
	 */
	private static native void internal_init();

	static {
		internal_init();
	}

	protected String[] mColumns = null;

	public String[] column_names() {
		if (mColumns == null) {
			int colCount = 0;
			try {
				colCount = column_count();
			} catch (Exception e1) {
				// TODO Auto-generated catch block
				e1.printStackTrace();
			}
			mColumns = new String[colCount];
			for (int i = 0; i < colCount; i++) {
				try {
					mColumns[i] = column_name(i);
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
		return mColumns;
	}

	public int column_index(String col_name) {
		String[] names = column_names();
		int col_count = names.length;
		int i;
		for (i = 0; i < col_count; i++) {
			if (col_name.equals(names[i]))
				return i;
		}
		return -1;
	}

	// public MatrixCursor get_cursor(){
	// return get_cursor(20);
	// }
	//
	// public MatrixCursor get_cursor(int capacity){
	// MatrixCursor cursor = new MatrixCursor(column_names(), capacity);
	// try {
	// int colCount = column_count();
	// while(step()){
	// Object[] rowObject = new Object[colCount];
	// for(int i=0; i < colCount; i++){
	// rowObject[i] = column_string(i);
	// }
	// cursor.addRow(rowObject);
	// }
	// } catch (Exception e) {
	// // TODO Auto-generated catch block
	// e.printStackTrace();
	// }
	// return cursor;
	// }
}
