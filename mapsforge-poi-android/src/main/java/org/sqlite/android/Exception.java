package org.sqlite.android;

/**
 * Class for SQLite related exceptions.
 */
public class Exception extends java.lang.Exception {
	private static final long serialVersionUID = 1L;

	/**
	 * Construct a new SQLite exception.
	 * 
	 * @param string
	 *            error message
	 */
	public Exception(String string) {
		super(string);
	}
}
