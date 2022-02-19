/**
 * SSHTOOLS Limited licenses this file to you under the Apache
 * License, Version 2.0 (the "License"); you may not use this
 * file except in compliance with the License.
 *
 * You may obtain a copy of the License at
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.sshtools.maven.codeswitcher;

import java.io.File;
import java.io.IOException;
import java.text.ParseException;
import java.util.HashMap;
import java.util.Map;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.util.FileUtils;

/**
 * Pre-process source
 * 
 * @goal pre-process
 */
public class CodeswitcherMojo extends AbstractMojo {
	/**
	 * The temporary directory to write the pre-processed files to when
	 * runOnCopy is set to true (the default).
	 * 
	 * @parameter default-value="target/preprocessed"
	 */
	private String temporaryDirectory = "target/preprocessed";

	/**
	 * When set, the goal will run on a copy of the source.
	 * 
	 * @parameter default-value="true"
	 */
	private boolean workOnCopy = true;

	/**
	 * When set, code will be commented out rather than stripped
	 * 
	 * @parameter default-value="false"
	 */
	private boolean comment;

	/**
	 * When set, the new source code will be set as the main source directory
	 * for the built. <code>workOnCopy</code> must also be <code>true</code> and
	 * a <code>temporaryDirectory</code> must be set.
	 * 
	 * @parameter default-value="false"
	 */
	private boolean changeBuildSourceDirectory;

	/**
	 * The list of symbols whose code will be enabled.
	 * 
	 * @parameter
	 */
	private String[] enable;

	/**
	 * The list of symbols whose code will be disabled/
	 * 
	 * @parameter
	 */
	private String[] disable;

	/**
	 * The output line separator. If not set, defaults to platform settings.
	 * Otherwise may be one of cr,crlf or lf
	 * 
	 * @parameter
	 */
	private String lineSeparator;

	/**
	 * Special token that gets replaced with the current timestamp
	 * 
	 * @parameter
	 */
	private String timestampToken;

	/**
	 * @parameter expression="${project}"
	 * @required
	 * @readonly
	 * @since 1.0
	 */
	private MavenProject project;

	/**
	 * Listof tokens that will simply be replaced in any pre-processed source.
	 * 
	 * @parameter
	 */
	private Token[] tokens;

	public void execute() throws MojoExecutionException {
		// Copy original source if configured to do so
		String pathToWorkOn = project.getBuild().getSourceDirectory();

		File targetDir = new File(project.getBasedir(), temporaryDirectory);

		File sourceDirectory = new File(project.getBuild().getSourceDirectory());
		if (!sourceDirectory.exists()) {
			getLog().warn("No or missing source directory to pre-process.");
		} else {
			if (workOnCopy) {
				copySource(sourceDirectory, targetDir);
				pathToWorkOn = targetDir.getAbsolutePath();
			}
			getLog().info("Pre-proccessing to " + pathToWorkOn);

			// Create switch, capture errors and messages and put them in the
			// maven
			// log
			CodeSwitcher switcher = new CodeSwitcher() {
				@Override
				protected void printError(File file, int line, String message) throws ParseException {
					if (file == null) {
						getLog().error(message);
					} else {
						getLog().error(file.getName() + "[" + line + "] " + message);
					}
					if (isFailOnError()) {
						throw new ParseException("Failed to codeswitch.", 0);
					}
				}

				@Override
				protected void printMessage(File file, int line, String message) {
					if (file == null) {
						getLog().info(message);
					} else {
						getLog().info(file.getName() + "[" + line + "] " + message);
					}
				}
			};

			// Line separators
			if ("cr".equalsIgnoreCase(lineSeparator)) {
				switcher.setLineSeparator("\r");
			} else if ("crlf".equalsIgnoreCase(lineSeparator)) {
				switcher.setLineSeparator("\r\n");
			} else if ("lf".equalsIgnoreCase(lineSeparator)) {
				switcher.setLineSeparator("\n");
			}

			// Options
			Map<String, String> t = new HashMap<String, String>();
			if (tokens != null) {
				for (Token token : tokens) {
					t.put(token.key, token.value);
				}
			}
			if (timestampToken != null) {
				t.put(timestampToken, System.currentTimeMillis() + "L");
			}
			switcher.setTokens(t);
			switcher.setComment(comment);

			// Configure symbols
			if (enable != null) {
				for (String symbol : enable) {
					switcher.enableSymbol(symbol);
				}
			}
			if (disable != null) {
				for (String symbol : disable) {
					switcher.disableSymbol(symbol);
				}
			}

			// Directory to process
			switcher.addDir(pathToWorkOn);

			// Start processing
			try {
				switcher.process();
			} catch (ParseException e) {
				throw new MojoExecutionException("Failed to process file.", e);
			}

			// Change the project source directory to the temporary directory
			// for
			// compilation
			if (workOnCopy && changeBuildSourceDirectory) {
				getLog().info("Changing build source directory");
				// TODO this currently works well enough for compiling. Need to
				// test
				// other common goals (javadoc, source jar, reports)
				
				/* TODO this make eclipse update the source directories incorrectly, need to find a way to exclude from
				 * this 
				 */
				for(Object key : getPluginContext().keySet()) {
					getLog().info("Key " + key + " = " + getPluginContext().get(key));
				}
				
				project.getCompileSourceRoots().remove(project.getBuild().getSourceDirectory());
				project.addCompileSourceRoot(pathToWorkOn);
			}
		}
	}

	private void copySource(File sourceDirectory, File targetDirectory) throws MojoExecutionException {
		try {
			FileUtils.copyDirectoryStructure(sourceDirectory, targetDirectory);
		} catch (IOException e) {
			throw new MojoExecutionException("Failed to copy source to temporary directory.", e);
		}
	}
}