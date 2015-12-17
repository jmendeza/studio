/*******************************************************************************
 * Crafter Studio Web-content authoring solution
 *     Copyright (C) 2007-2013 Crafter Software Corporation.
 * 
 *     This program is free software: you can redistribute it and/or modify
 *     it under the terms of the GNU General Public License as published by
 *     the Free Software Foundation, either version 3 of the License, or
 *     (at your option) any later version.
 * 
 *     This program is distributed in the hope that it will be useful,
 *     but WITHOUT ANY WARRANTY; without even the implied warranty of
 *     MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *     GNU General Public License for more details.
 * 
 *     You should have received a copy of the GNU General Public License
 *     along with this program.  If not, see <http://www.gnu.org/licenses/>.
 ******************************************************************************/
package org.craftercms.studio.api.v1.service.translation;

import java.io.PrintWriter;
import java.io.StringWriter;

public class ProviderException extends RuntimeException
{
	private static final long serialVersionUID = -8476500907489141914L;

	private boolean fatal;

	public ProviderException(String message, Exception cause, boolean fatal) {
		super(message, cause);
		this.fatal = fatal;
	}

	public boolean isFatal() {
		return fatal;
	}

	public String dumpStackTrace() {
		StringWriter buf = new StringWriter();
		PrintWriter out = new PrintWriter(buf);
		printStackTrace(out);
		out.flush();
		return buf.toString();
	}
}
