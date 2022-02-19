package com.sshtools.maven.codeswitcher;

/**
 * A token for replacement
 * 
 */
public class Token {
	/**
	 * The token key, i.e. the text that is search for.
	 * 
	 * @parameter token
	 */
	public String key;
	/**
	 * The token value, i.e. the text that is inserted in place of the key.
	 * 
	 * @parameter token
	 */
	public String value;

}