package com.sshtools.maven.codeswitcher;

/* 
 * Heavily based on HSQLDB's CodeSwitcher utility. All remaining portions subject
 * to the below license. All other code, copyright 2011 SSHTools, license under the Apache 2 
 * license.
 * 
 * ----------------------------------------------------------------------
 * 
 * Copyrights and Licenses
 *
 * This product includes Hypersonic SQL.
 * Originally developed by Thomas Mueller and the Hypersonic SQL Group. 
 *
 * Copyright (c) 1995-2000 by the Hypersonic SQL Group. All rights reserved. 
 * Redistribution and use in source and binary forms, with or without modification, are permitted
 * provided that the following conditions are met: 
 *     -  Redistributions of source code must retain the above copyright notice, this list of conditions
 *         and the following disclaimer. 
 *     -  Redistributions in binary form must reproduce the above copyright notice, this list of
 *         conditions and the following disclaimer in the documentation and/or other materials
 *         provided with the distribution. 
 *     -  All advertising materials mentioning features or use of this software must display the
 *        following acknowledgment: "This product includes Hypersonic SQL." 
 *     -  Products derived from this software may not be called "Hypersonic SQL" nor may
 *        "Hypersonic SQL" appear in their names without prior written permission of the
 *         Hypersonic SQL Group. 
 *     -  Redistributions of any form whatsoever must retain the following acknowledgment: "This
 *          product includes Hypersonic SQL." 
 * This software is provided "as is" and any expressed or implied warranties, including, but
 * not limited to, the implied warranties of merchantability and fitness for a particular purpose are
 * disclaimed. In no event shall the Hypersonic SQL Group or its contributors be liable for any
 * direct, indirect, incidental, special, exemplary, or consequential damages (including, but
 * not limited to, procurement of substitute goods or services; loss of use, data, or profits;
 * or business interruption). However caused any on any theory of liability, whether in contract,
 * strict liability, or tort (including negligence or otherwise) arising in any way out of the use of this
 * software, even if advised of the possibility of such damage. 
 * This software consists of voluntary contributions made by many individuals on behalf of the
 * Hypersonic SQL Group.
 *
 *
 * For work added by the HSQL Development Group:
 *
 * Copyright (c) 2001-2002, The HSQL Development Group
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer, including earlier
 * license statements (above) and comply with all above license conditions.
 *
 * Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution, including earlier
 * license statements (above) and comply with all above license conditions.
 *
 * Neither the name of the HSQL Development Group nor the names of its
 * contributors may be used to endorse or promote products derived from this
 * software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL HSQL DEVELOPMENT GROUP, HSQLDB.ORG, 
 * OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, 
 * EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, 
 * PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

import java.io.*;
import java.text.ParseException;
import java.util.*;

/**
 * Modifies the source code to support different JDK or profile settings.
 */
public class CodeSwitcher {

	private static final String DEFAULT_LINE_SEPARATOR = System.getProperty("line.separator", "\n");
	private List<String> vList;
	private List<String> vSwitchOn;
	private List<String> vSwitchOff;
	private List<String> vSwitches;
	private boolean comment = true;
	private boolean failOnError = true;
	private Map<String, String> tokens;
	private String lineSeparator = DEFAULT_LINE_SEPARATOR;
	private boolean keepLastModified = true;

	/**
	 * Constructor declaration
	 * 
	 */
	public CodeSwitcher() {
		vList = new ArrayList<String>();
		vSwitchOn = new ArrayList<String>();
		vSwitchOff = new ArrayList<String>();
		vSwitches = new ArrayList<String>();
	}

	/**
	 * Set if processed files should have the same modification time as the
	 * original. Setting this to true would allow compiles to be more efficient,
	 * but might mean pre-processing state is wrong if the switches change.
	 * 
	 * @param keepLastModified keep last modified time
	 */
	public void setKeepLastModified(boolean keepLastModified) {
		this.keepLastModified = keepLastModified;
	}

	/**
	 * Get if processed files should have the same modification time as the
	 * original. Setting this to true would allow compiles to be more efficient,
	 * but might mean pre-processing state is wrong if the switches change.
	 * 
	 * @return keep last modified time
	 */
	public boolean getKeepLastModified() {
		return keepLastModified;
	}

	/**
	 * Set the output line separator. Defaults to the platform separator.
	 * 
	 * @param line separator
	 */
	public void setLineSeparator(String lineSeparator) {
		this.lineSeparator = lineSeparator;
	}

	/**
	 * Get the output line separator. Defaults to the platform separator.
	 * 
	 * @return line separator
	 */
	public String getLineSeparator() {
		return lineSeparator;
	}

	/**
	 * Set a map of tokens that will be used for simple string replacement.
	 * Every line will be checked for the strings contained in the keys. Any
	 * occurences will be replaced with the associated value.
	 * 
	 * @param tokens tokens
	 */
	public void setTokens(Map<String, String> tokens) {
		this.tokens = tokens;
	}

	/**
	 * Get a map of tokens that will be used for simple string replacement.
	 * Every line will be checked for the strings contained in the keys. Any
	 * occurences will be replaced with the associated value.
	 * 
	 * @return tokens
	 */
	public Map<String, String> getTokens() {
		return tokens;
	}

	/**
	 * Set whether an exception should be thrown when a parsing error occurs.
	 * 
	 * @param failOnError fail on error
	 */
	public void setFailOnError(boolean failOnError) {
		this.failOnError = failOnError;
	}

	/**
	 * Get whether an exception should be thrown when a parsing error occurs.
	 * 
	 * @return fail on error
	 */
	public boolean isFailOnError() {
		return failOnError;
	}

	/**
	 * Enabled code the is tagged with the provided symbol.
	 * 
	 * @param symbol symbol for code that should be enabled
	 */
	public void enableSymbol(String symbol) {
		vSwitchOn.add(symbol);
	}

	/**
	 * Disable code the is tagged with the provided symbol.
	 * 
	 * @param symbol symbol for code that should be disabled
	 */
	public void disableSymbol(String symbol) {
		vSwitchOff.add(symbol);
	}

	/**
	 * Set whether any removed code should be commented instead of stripped.
	 * 
	 * @param comment comment out source code instead of stripping it
	 */
	public void setComment(boolean comment) {
		this.comment = comment;
	}

	/**
	 * Get whether any removed code should be commented instead of stripped.
	 * 
	 * @return comment out source code instead of stripping it
	 */
	public boolean isComment() {
		return comment;
	}

	/**
	 * Process all files
	 * 
	 * @throws ParseException
	 */
	public void process() throws ParseException {
		for (String file : vList) {
			if (!processFile(file)) {
				printError(new File(file), 0, "in file " + file + " !");
			}
		}
		printMessage(null, 0, "");
	}

	/**
	 * Prints out all used labels
	 */
	void printSwitches() {
		printMessage(null, 0, "Used labels:");
		for (int i = 0; i < vSwitches.size(); i++) {
			printMessage(null, 0, vSwitches.get(i));
		}
	}

	/**
	 * Adds a directory to those that must be processed.
	 * 
	 * @param path
	 */
	void addDir(String path) {
		File f = new File(path);
		if (f.isFile() && path.endsWith(".java")) {
			vList.add(path);
		} else if (f.isDirectory()) {
			String list[] = f.list();
			for (int i = 0; i < list.length; i++) {
				addDir(path + File.separatorChar + list[i]);
			}
		}
	}

	class State {
		int state = 0; // 0=normal 1=inside_if 2=inside_else
		boolean switchoff = false;
		boolean working = false;
		int removeFrom = -1;
		int i = 0;
		public boolean delete;
	}

	private boolean processFile(String name) throws ParseException {

		File f = new File(name);
		long lastModified = f.lastModified();
		File fnew = new File(name + ".new");
		State state = new State();

		try {
			List<String> newContents = getFileLines(f);
			List<String> originalContents = new ArrayList<String>(newContents.size());

			for (int i = 0; i < newContents.size(); i++) {
				originalContents.add(newContents.get(i));
			}

			for (; state.i < newContents.size() && !state.delete; state.i++) {
				String line = (String) newContents.get(state.i);

				if (line == null) {
					break;
				}
				
				String lineStripped = stripSpaces(line);

				if (state.working) {
					if (lineStripped.equals("/*") || lineStripped.equals("*/")) {
						newContents.remove(state.i--);
					} else if (lineStripped.startsWith("*")) {
						int idx = line.indexOf('*');
						newContents.set(state.i, line.substring(0, idx) + line.substring(idx + 1).trim());
					} else if (lineStripped.startsWith("//") && !lineStripped.startsWith("//#")) {
						int idx = line.indexOf("/");
						newContents.set(state.i, line.substring(0, idx) + line.substring(idx + 2).trim());
					}
				}

				if (lineStripped.indexOf("//#") != -1) {
					// Handle single line comment switches
					if (lineStripped.startsWith("//#[")) {
						if (!handleSingleLineComment(f, state, newContents, line, lineStripped)) {
							return false;
						}
					} else if (lineStripped.startsWith("//#ifdef")) {
						if (!handleIf(f, state, newContents, lineStripped)) {
							return false;
						}
					} else if (lineStripped.startsWith("//#else")) {
						if (!handleElse(f, state, newContents)) {
							return false;
						}
					} else if (lineStripped.startsWith("//#endif")) {
						if (!handleEndIf(f, state, newContents)) {
							return false;
						}
					} else if (lineStripped.startsWith("//#del")) {
						if (!handleDel(f, state, lineStripped)) {
							return false;
						}
					}
				}
			}

			if (state.delete) {
				if (!f.delete()) {
					printError(f, 0, "Failed to delete file " + f);
				}
			} else {

				if (state.state != 0) {
					printError(f, newContents.size(), "'#endif' missing");
					return false;
				}

				// Look for any tokens to replace
				for (int i = 0; i < newContents.size(); i++) {
					for (String token : tokens.keySet()) {
						String value = tokens.get(token);
						newContents.set(i, newContents.get(i).replace(token, value));
					}
				}

				// Determine if the file has changed at all
				boolean filechanged = false;
				for (int i = 0; i < newContents.size(); i++) {
					if (!originalContents.get(i).equals(newContents.get(i))) {
						filechanged = true;
						break;
					}
				}

				if (!filechanged) {
					return true;
				}

				writeFileLines(newContents, fnew, lineSeparator);

				File fbak = new File(name + ".bak");

				fbak.delete();
				f.renameTo(fbak);

				File fcopy = new File(name);

				fnew.renameTo(fcopy);
				fbak.delete();

				if (keepLastModified) {
					f.setLastModified(lastModified);
				}
			}

			return true;
		} catch (Exception e) {
			printError(null, 0, e.getMessage());

			return false;
		}
	}

	private boolean handleDel(File f, State state, String lineStripped) throws ParseException {
		String symbol = lineStripped.substring(6);
		if (symbol.length() == 0) {
			printError(f, state.i, "No symbol provide for #del statement");
			return false;
		}
		if (vSwitchOn.contains(symbol)) {
			state.delete = true;
			printMessage(f, state.i, "Will delete file " + f);
		}
		return true;
	}

	protected boolean handleElse(File f, State state, List<String> v) throws ParseException {
		if (state.state != 1) {
			printError(f, state.i, "'#else' without '#ifdef'");
			return false;
		}
		state.state = 2;
		if (state.working) {
			if (state.switchoff) {
				if (comment) {
					if (v.get(state.i - 1).equals("")) {
						v.add(state.i - 1, "*/");
						state.i++;
					} else {
						v.add(state.i++, "*/");
					}
				} else {
					removeMarkedLinesFrom(f, state, v);
				}
				state.switchoff = false;
			} else {
				if (comment) {
					v.add(++state.i, "/*");
				} else {
					state.removeFrom = state.i;
				}
				state.switchoff = true;
			}
		}
		return true;
	}

	protected boolean handleIf(File f, State state, List<String> v, String lineStripped) throws ParseException {
		if (state.state != 0) {
			printError(f, state.i, "'#ifdef' not allowed inside '#ifdef'");
			return false;
		}
		state.state = 1;
		state.removeFrom = -1;
		String s = lineStripped.substring(8);
		if (vSwitchOn.indexOf(s) != -1) {
			printMessage(f, state.i, "Including " + s);
			state.working = true;
			state.switchoff = false;
			if (!comment) {
				// If not commenting, removing the ifdef itself
				v.remove(state.i--);
			}
		} else if (vSwitchOff.indexOf(s) != -1) {
			printMessage(f, state.i, "Excluding " + s);
			state.working = true;
			if (comment) {
				v.add(++state.i, "/*");
			} else {
				state.removeFrom = state.i;
			}
			state.switchoff = true;
		}
		if (vSwitches.indexOf(s) == -1) {
			vSwitches.add(s);
		}
		return true;
	}

	protected boolean handleEndIf(File f, State state, List<String> v) throws ParseException {
		if (state.state == 0) {
			printError(f, state.i, "'#endif' without '#ifdef'");
			return false;
		}
		state.state = 0;
		if (state.working && state.switchoff) {
			if (v.get(state.i - 1).equals("")) {
				if (comment) {
					v.add(state.i - 1, "*/");
				}
				state.i++;
			} else {
				if (comment) {
					v.add(state.i++, "*/");
				}
			}
			removeMarkedLinesFrom(f, state, v);
		}
		else if (!comment) {
			// If not commenting, removing the endif itself
			state.removeFrom = state.i;
			removeMarkedLinesFrom(f, state, v);
		}
		state.working = false;
		state.switchoff = false;
		return true;
	}

	protected boolean handleSingleLineComment(File f, State state, List<String> v, String line, String lineStripped)
			throws ParseException {
		int idx = line.indexOf("[");
		int eidx = line.indexOf(']');
		if (idx == -1) {
			printError(f, state.i, "unclosed simple switch");
			return false;
		} else {
			String symbol = line.substring(idx + 1, eidx);
			if (vSwitchOn.contains(symbol)) {
				v.set(state.i, line.substring(0, line.indexOf('/')) + line.substring(eidx + 1).trim());
			} else if (vSwitchOff.contains(symbol)) {
				state.removeFrom = state.i;
				removeMarkedLinesFrom(f, state, v);
			}
		}
		return true;
	}

	private void removeMarkedLinesFrom(File f, State state, List<String> v) {
		if (state.removeFrom != -1) {
			printMessage(f, state.removeFrom, "Removing " + (state.i - state.removeFrom) + " lines");
			for(int i = state.removeFrom; i <= state.i; i++) {
				printMessage(f, state.removeFrom, "Removed '" + v.remove(state.removeFrom) + "'");
			}
			state.i = state.removeFrom - 1;
			state.removeFrom = -1;
		}
	}

	static List<String> getFileLines(File f) throws IOException {
		LineNumberReader read = null;
		List<String> v = new ArrayList<String>();
		try {
			read = new LineNumberReader(new FileReader(f));
			for (;;) {
				String line = read.readLine();
				if (line == null) {
					break;
				}
				v.add(line);
			}
		} finally {
			if (read != null) {
				read.close();
			}
		}
		return v;
	}

	static String trimBoth(String text) {
		text = text.trim();
		int s = text.length();
		char ch;
		int i;
		for (i = 0; i < s && Character.isWhitespace((ch = text.charAt(i))); i++)
			;
		return i < s ? text.substring(i) : text;
	}

	static String stripSpaces(String text) {
		StringBuffer buf = new StringBuffer();
		int s = text.length();
		char ch;
		for (int i = 0; i < s; i++) {
			ch = text.charAt(i);
			if (!Character.isWhitespace(ch)) {
				buf.append(ch);
			}
		}
		return buf.toString();
	}

	static void writeFileLines(List<String> v, File f, String ls) throws IOException {

		FileWriter write = null;
		try {
			write = new FileWriter(f);
			for (String line : v) {
				write.write(line);
				write.write(ls);
			}

			write.flush();
		} finally {
			if (write != null) {
				write.close();

			}
		}
	}

	/**
	 * Print an error message. May be overridden to integrate with logging
	 * frameworks. If <code>failOnError</code> is set, an exception will also be
	 * thrown.
	 * 
	 * @param f file message occurred on
	 * @param line line number
	 * @param error message
	 * @throws ParseException if failOnError is set
	 */
	protected void printError(File f, int line, String error) throws ParseException {
		if (f == null) {
			System.out.println("ERROR: " + error);
			if (failOnError) {
				throw new ParseException("Failed to codeswitch.", 0);
			}
		} else {
			System.out.println("ERROR: " + f.getName() + "[" + line + "] " + error);
			if (failOnError) {
				throw new ParseException("Failed to parse " + f.getPath() + ". " + error, line);
			}
		}
	}

	/**
	 * Print a message. May be overridden to integrate with logging frameworks.
	 * 
	 * @param f file message occured on
	 * @param line line number
	 * @param message message
	 */
	protected void printMessage(File f, int line, String message) {
		if (f == null) {
			System.out.println("MSG: " + message);
		} else {
			System.out.println("MSG: " + f.getName() + "[" + line + "] " + message);
		}
	}

	/**
	 * Command line entry point,
	 * 
	 * <pre>
	 * Usage: java CodeSwitcher [paths] [labels] [+][-]
	 * If no labels are specified then all used
	 * labels in the source code are shown.
	 * Use +MODE to switch on the things labeld MODE
	 * Use -MODE to switch off the things labeld MODE
	 * Path: Any number of path or files may be
	 * specified. Use . for the current directory
	 * (including sub-directories).
	 * Example: java CodeSwitcher +JAVA2 .
	 * This example switches on code labeled JAVA2
	 * in all *.java files in the current directory
	 * and all subdirectories.
	 * </pre>
	 * 
	 * @param arguments.
	 * @throws ParseException
	 */
	public static void main(String a[]) throws ParseException {

		CodeSwitcher s = new CodeSwitcher();

		if (a.length == 0) {
			showUsage();

			return;
		}

		boolean path = false;

		for (int i = 0; i < a.length; i++) {
			String p = a[i];
			if (p.startsWith("/")) {
				String opt = p.substring(1);
				if (opt.equalsIgnoreCase("strip")) {
					s.comment = false;
				}
			} else if (p.startsWith("+")) {
				s.enableSymbol(p.substring(1));
			} else if (p.startsWith("-")) {
				s.disableSymbol(p.substring(1));
			} else {
				s.addDir(p);
				path = true;
			}
		}

		if (!path) {
			s.printError(null, 0, "no path specified");
			showUsage();
		} else {
			if (s.vSwitchOff.size() == 0 && s.vSwitchOn.size() == 0) {
				s.printSwitches();
			} else {
				s.process();
			}
		}
	}

	/**
	 * Method declaration
	 * 
	 */
	static void showUsage() {
		System.out.print("Usage: java CodeSwitcher [paths] [labels] [+][-]\n" + "If no labels are specified then all used\n"
			+ "labels in the source code are shown.\n" + "Use +MODE to switch on the things labeld MODE\n"
			+ "Use -MODE to switch off the things labeld MODE\n" + "Path: Any number of path or files may be\n"
			+ "specified. Use . for the current directory\n" + "(including sub-directories).\n"
			+ "Example: java CodeSwitcher +JAVA2 .\n" + "This example switches on code labeled JAVA2\n"
			+ "in all *.java files in the current directory\n" + "and all subdirectories.\n");
	}
}
