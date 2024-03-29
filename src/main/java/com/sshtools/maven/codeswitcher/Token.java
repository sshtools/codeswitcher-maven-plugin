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