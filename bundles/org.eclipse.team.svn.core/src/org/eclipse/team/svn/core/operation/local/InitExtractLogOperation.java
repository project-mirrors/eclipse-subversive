/*******************************************************************************
 * Copyright (c) 2005, 2023 Polarion Software and others.
 * 
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v. 2.0 which is available at
 * https://www.eclipse.org/legal/epl-2.0
 * 
 * SPDX-License-Identifier: EPL-2.0
 *
 * Contributors:
 *    Alexander Gurov (Polarion Software) - initial API and implementation
 *    Igor Burilo - Bug 245509: Improve extract log
 *    Alexander Fedorov (ArSysOp) - ongoing support
 *******************************************************************************/

package org.eclipse.team.svn.core.operation.local;

import java.io.FileWriter;
import java.io.IOException;
import java.text.DateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.svn.core.IStateFilter;
import org.eclipse.team.svn.core.SVNMessages;
import org.eclipse.team.svn.core.operation.AbstractActionOperation;

/**
 * Performs initialization of extract operations log file.
 * 
 * @author Alexander Gurov
 */
public class InitExtractLogOperation extends AbstractActionOperation {
	public static final String COMPLETE_LOG_NAME = "/changes.log"; //$NON-NLS-1$

	protected HashMap<String, List<String>> extractParticipants;

	protected String logPath;

	public InitExtractLogOperation(String logPath) {
		super("Operation_InitExtractLog", SVNMessages.class); //$NON-NLS-1$
		this.logPath = logPath;
		extractParticipants = new HashMap<>();
	}

	@Override
	protected void runImpl(IProgressMonitor monitor) throws Exception {
		DateFormat formatter = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM,
				Locale.getDefault());
		String date = formatter.format(new Date());
		logImpl(""); //$NON-NLS-1$
		logImpl(date);
		logImpl("==============================================================================="); //$NON-NLS-1$
	}

	public void log(String participant, String status) {
		String toPut = status.equals(IStateFilter.ST_NEW)
				? IStateFilter.ST_ADDED
				: status.equals(IStateFilter.ST_REPLACED) ? IStateFilter.ST_MODIFIED : status;
		if (extractParticipants.get(toPut) == null) {
			extractParticipants.put(toPut, new ArrayList<>());
		}
		extractParticipants.get(toPut).add(participant);
	}

	public void flushLog() {
		HashMap<String, List<String>> sortedParticipants = new HashMap<>();
		for (String status : extractParticipants.keySet()) {
			String[] participants = extractParticipants.get(status).toArray(new String[0]);
			Arrays.sort(participants);
			ArrayList<String> participantsToLog = new ArrayList<>();
			for (int i = 0; i < participants.length; i++) {
				if (status.equals(IStateFilter.ST_DELETED)) {
					boolean parentIsAlreadyLogged = false;
					for (String logged : participantsToLog) {
						if (participants[i].startsWith(logged + "\\")) { //$NON-NLS-1$
							parentIsAlreadyLogged = true;
							break;
						}
					}
					if (!parentIsAlreadyLogged) {
						participantsToLog.add(participants[i]);
					}
				} else if (i + 1 >= participants.length || !participants[i + 1].startsWith(participants[i] + "\\")) { //$NON-NLS-1$
					participantsToLog.add(participants[i]);
				}
			}
			sortedParticipants.put(status, participantsToLog);
		}

		//Sorting statuses
		List<String> statusesList = Arrays
				.asList(sortedParticipants.keySet().toArray(new String[extractParticipants.size()]));
		Collections.sort(statusesList, (o1, o2) -> {
			if (o1.equals(IStateFilter.ST_MODIFIED)) {
				return -1;
			}
			if (o2.equals(IStateFilter.ST_MODIFIED)) {
				return 1;
			}
			if (o1.equals(IStateFilter.ST_ADDED)) {
				return -1;
			}
			if (o2.equals(IStateFilter.ST_ADDED)) {
				return 1;
			}
			if (o1.equals(IStateFilter.ST_NEW)) {
				return -1;
			}
			if (o2.equals(IStateFilter.ST_NEW)) {
				return 1;
			}
			return 0;
		});

		for (String status : statusesList) {
			for (String participant : sortedParticipants.get(status)) {
				logImpl(SVNMessages.getString("Console_Status_" + status) + " " + participant); //$NON-NLS-1$ //$NON-NLS-2$
			}
			logImpl(""); //$NON-NLS-1$
		}
		extractParticipants.clear();
	}

	private void logImpl(String line) {
		FileWriter writer = null;
		try {
			writer = new FileWriter(logPath + InitExtractLogOperation.COMPLETE_LOG_NAME, true);
			writer.write(line);
			writer.write(System.lineSeparator());
		} catch (IOException ex) {
			//ignore
		} finally {
			if (writer != null) {
				try {
					writer.close();
				} catch (Exception ex) {
				}
			}
		}
	}

}
