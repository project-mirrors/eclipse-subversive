/*******************************************************************************
 * Copyright (c) 2008, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.ant;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.tools.ant.BuildException;
import org.apache.tools.ant.Task;
import org.apache.tools.ant.taskdefs.Execute;
import org.apache.tools.ant.taskdefs.PumpStreamHandler;
import org.apache.tools.ant.types.Commandline;
import org.eclipse.core.runtime.Platform;

/**
 * Implementation of the SVN task for Ant
 * 
 * @author Alexander Gurov
 */
public class SVNTask extends Task {
	public static final String CMD_EXPORT = "export";

	public static final String CMD_CAT = "cat";

	protected String command;

	protected String url;

	protected String pegRev;

	protected String rev;

	protected String dest;

	protected String username;

	protected String password;

	protected boolean force;

	protected String SPACE_WRAP_CHAR = Platform.OS_WIN32.equals(Platform.getOS()) ? "\"" : "'";

	@Override
	public void execute() throws BuildException {
		String cmdLine = null;
		FileOutputStream oStream = null;
		if (SVNTask.CMD_CAT.equals(command)) {
			cmdLine = "svn cat " + SPACE_WRAP_CHAR + url;
			if (pegRev != null) {
				cmdLine += "@" + pegRev;
			}
			cmdLine += SPACE_WRAP_CHAR;
			if (rev != null) {
				cmdLine += " -r " + rev;
			}
			cmdLine += getCreadentialsPart();
			cmdLine += " --non-interactive";

			File folder = new File(dest).getParentFile();
			folder.mkdirs();

			try {
				oStream = new FileOutputStream(dest);
			} catch (FileNotFoundException e) {
				throw new BuildException(e);
			}
		} else if (SVNTask.CMD_EXPORT.equals(command)) {
			cmdLine = "svn export ";

			if (force) {
				cmdLine += " --force ";
			}

			if (rev != null) {
				cmdLine += " -r " + rev;
			}

			cmdLine += " " + SPACE_WRAP_CHAR + url;
			if (pegRev != null) {
				cmdLine += "@" + pegRev;
			}
			cmdLine += SPACE_WRAP_CHAR;
			cmdLine += " " + SPACE_WRAP_CHAR + dest + SPACE_WRAP_CHAR;
			cmdLine += getCreadentialsPart();
			cmdLine += " -q --non-interactive";
		}

		if (cmdLine != null) {
			try {
				Execute exe = new Execute(new PumpStreamHandler(oStream == null ? System.out : oStream, System.err));
				exe.setAntRun(getProject());
				exe.setCommandline(new Commandline(cmdLine).getCommandline());
				exe.execute();
			} catch (IOException e) {
				throw new BuildException(e);
			} finally {
				if (oStream != null) {
					try {
						oStream.close();
					} catch (IOException ex) {
					}
				}
			}
		}
	}

	public String getCommand() {
		return command;
	}

	public void setCommand(String command) {
		this.command = command;
	}

	public void setForce(boolean force) {
		this.force = force;
	}

	public boolean getForce() {
		return force;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public String getPegRev() {
		return pegRev;
	}

	public void setPegRev(String pegRev) {
		this.pegRev = pegRev;
	}

	public String getRev() {
		return rev;
	}

	public void setRev(String rev) {
		this.rev = rev;
	}

	public String getDest() {
		return dest;
	}

	public void setDest(String dest) {
		this.dest = dest;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	protected String getCreadentialsPart() {
		String cmdLine = " --no-auth-cache";
		if (username != null && username.length() != 0) {
			cmdLine += " --username " + SPACE_WRAP_CHAR + username + SPACE_WRAP_CHAR;
		}
		if (password != null && password.length() != 0) {
			cmdLine += " --password " + SPACE_WRAP_CHAR + password + SPACE_WRAP_CHAR;
		}
		return cmdLine;
	}

}
