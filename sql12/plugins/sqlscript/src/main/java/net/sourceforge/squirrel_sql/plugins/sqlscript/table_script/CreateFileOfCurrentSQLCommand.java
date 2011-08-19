/*
 * Copyright (C) 2011 Stefan Willinger
 * wis775@users.sourceforge.net
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */
package net.sourceforge.squirrel_sql.plugins.sqlscript.table_script;

import java.awt.Frame;
import java.io.File;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.NumberFormat;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;

import net.sourceforge.squirrel_sql.client.gui.IAbortEventHandler;
import net.sourceforge.squirrel_sql.client.gui.ProgressAbortDialog;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.codereformat.CodeReformator;
import net.sourceforge.squirrel_sql.fw.codereformat.CommentSpec;
import net.sourceforge.squirrel_sql.fw.dialects.DialectFactory;
import net.sourceforge.squirrel_sql.fw.dialects.DialectType;
import net.sourceforge.squirrel_sql.fw.gui.action.ResultSetExportCommand;
import net.sourceforge.squirrel_sql.fw.gui.action.TableExportCsvDlg;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.ProgressAbortCallback;
import net.sourceforge.squirrel_sql.fw.sql.ProgressAbortFactoryCallback;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.StringUtilities;
import net.sourceforge.squirrel_sql.plugins.sqlscript.SQLScriptPlugin;

import org.apache.commons.lang.time.StopWatch;

/**
 * Command to export the result of the current SQL into a File.
 * With this command is the user able to export the result of the current SQL into a file using the {@link TableExportCsvDlg}.
 * The command will run on a separate thread and a separate connection to the database. It is monitored with a {@link ProgressAbortDialog} and can be canceled.
 * @see ResultSetExportCommand
 * @see ProgressAbortCallback
 * @author Stefan Willinger
 */
public class CreateFileOfCurrentSQLCommand extends AbstractDataScriptCommand {
	private static final StringManager s_stringMgr = StringManagerFactory
			.getStringManager(CreateFileOfCurrentSQLCommand.class);

	/**
	 * Command for exporting the data.
	 */
	private ResultSetExportCommand resultSetExportCommand;
	
	private Statement stmt = null;

	/**
	 * Progress dialog which supports the ability to cancel the task.
	 */
	private ProgressAbortCallback progressDialog;
	
	/**
	 * The current SQL in the SQL editor pane.
	 */
	private String currentSQL = null;

	/**
	 * Ctor specifying the current session.
	 */
	public CreateFileOfCurrentSQLCommand(ISession session, SQLScriptPlugin plugin) {
		super(session, plugin);
	}

	
	/**
	 * Does the job.
	 * @see net.sourceforge.squirrel_sql.fw.util.ICommand#execute()
	 */
	@Override
	public void execute() {
		
		this.currentSQL = getSelectedSelectStatement();
		
		getSession().getApplication().getThreadPool().addTask(new Runnable() {
			public void run() {
				doCreateFileOfCurrentSQL();
			}
		});
		 
	}



	/**
	 * Do the work.
	 */
	private void doCreateFileOfCurrentSQL() {
		try {
		
			ISQLConnection unmanagedConnection = null;
			try {
				unmanagedConnection = createUnmanagedConnection();
				
				// TODO maybe, we should use a SQLExecutorTask for taking advantage of some ExecutionListeners like the parameter replacement. But how to get the right Listeners?
				if(unmanagedConnection != null){
					
					stmt = createStatementForStreamingResults(unmanagedConnection.getConnection());
				}else{
					stmt = createStatementForStreamingResults(getSession().getSQLConnection().getConnection());
				}
				
				
				ProgressAbortFactoryCallback progressFactory = new ProgressAbortFactoryCallback() {
					@Override
					public ProgressAbortCallback create() {
						createProgressAbortDialog();
						return progressDialog;
					}
				};
				
				
				StopWatch stopWatch = new StopWatch();
				stopWatch.start();
				
				DialectType dialectType =
			            DialectFactory.getDialectType(getSession().getMetaData());
				resultSetExportCommand = new ResultSetExportCommand(stmt, currentSQL, dialectType, progressFactory);
				resultSetExportCommand.execute();
				
				stopWatch.stop();
				
				if (isAborted()) {
					return;
				}else if(resultSetExportCommand.getWrittenRows() >= 0){
					NumberFormat nf = NumberFormat.getIntegerInstance();
					
					String rows = nf.format(resultSetExportCommand.getWrittenRows());
					File targetFile = resultSetExportCommand.getTargetFile();
					String seconds = nf.format(stopWatch.getTime()/1000);
					String msg = s_stringMgr.getString("CreateFileOfCurrentSQLCommand.progress.sucessMessage",
							rows, 
							targetFile, 
							seconds);
					getSession().showMessage(msg);
				}
			} finally {
				SQLUtilities.closeStatement(stmt);
				if(unmanagedConnection != null){
					unmanagedConnection.close();
				}
			}
		}catch (Exception e) {
			if(e.getCause() != null){
				getSession().showErrorMessage(e.getCause());
			}
			getSession().showErrorMessage(e.getMessage());
		} finally {
			SwingUtilities.invokeLater(new Runnable() {
				public void run() {
					hideProgressMonitor();
				}
			});
		}
	}

	/**
	 * Create a {@link Statement} that will stream the result instead of loading into the memory.
	 * @param connection the connection to use
	 * @return A Statement, that will stream the result.
	 * @throws SQLException 
	 * @see http://javaquirks.blogspot.com/2007/12/mysql-streaming-result-set.html
	 * @see http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
	 */
	private Statement createStatementForStreamingResults(Connection connection) throws SQLException {
		Statement stmt;
		DialectType dialectType =
	            DialectFactory.getDialectType(getSession().getMetaData());
		if(DialectType.MYSQL5 == dialectType){
			/*
			 * MYSQL will load the whole result into memory. To avoid this, we must use the streaming mode.
			 * 
			 * http://javaquirks.blogspot.com/2007/12/mysql-streaming-result-set.html
			 * http://dev.mysql.com/doc/refman/5.0/en/connector-j-reference-implementation-notes.html
			 */
			stmt = connection.createStatement(java.sql.ResultSet.TYPE_FORWARD_ONLY, java.sql.ResultSet.CONCUR_READ_ONLY);
			stmt.setFetchSize(Integer.MIN_VALUE);
		}else{
			stmt = connection.createStatement();
		}
		return stmt;

	}


	/**
	 * Create a new unmanaged connection, , which is not associated with the current session.
	 * @return a new unmanaged connection or null, if no connection can be created.
	 * @throws SQLException 
	 */
	private ISQLConnection createUnmanagedConnection() throws SQLException {
		ISQLConnection unmanagedConnection = getSession().createUnmanagedConnection();
		
		if(unmanagedConnection == null){
			int option = JOptionPane.showConfirmDialog(null, "Unable to open a new connection. The current connection will be used instead.", "Unable to open a new Connection", JOptionPane.OK_CANCEL_OPTION);
			if(option == JOptionPane.CANCEL_OPTION){
				return null;
			}
		}else{
			// we didn't want a autocommit
			unmanagedConnection.setAutoCommit(false);
		}
		return unmanagedConnection;
	}


	/**
	 * Create and show a new  progress monitor with the ability to cancel the task.
	 */
	protected void createProgressAbortDialog() {
		try {
			SwingUtilities.invokeAndWait(new Runnable() {
				
				@Override
				public void run() {
					/*
					 *  Copied from FormatSQLCommand.
					 *  Is there a better way to get the CommentSpec[] ?
					 */
			
			CommentSpec[] commentSpecs =
			  new CommentSpec[]
			  {
				  new CommentSpec("/*", "*/"),
				  new CommentSpec("--", StringUtilities.getEolStr())
			  };

			String statementSep = getSession().getQueryTokenizer().getSQLStatementSeparator();
			
			CodeReformator cr = new CodeReformator(statementSep, commentSpecs);

			String reformatedSQL = cr.reformat(resultSetExportCommand.getSql());
			
			String targetFile = resultSetExportCommand.getTargetFile().getAbsolutePath();
			
			// i18n[CreateFileOfCurrentSQLCommand.progress.title=Exporting to a file.]
			String title = s_stringMgr.getString("CreateFileOfCurrentSQLCommand.progress.title", targetFile);
			progressDialog = new SQL2FileProgressAbortDialog((Frame)null, title, targetFile, reformatedSQL , new IAbortEventHandler() {
				@Override
				public void cancel() {
					/* 
					 * We need to cancel the statement at this point for the case, that we are waiting for the first rows.
					 */
					if(stmt != null){
						try {
							stmt.cancel();
						} catch (SQLException e1) {
							// nothing todo
						}
					}
				}
			});
				}
			});
		} catch (Exception e) {
			throw new RuntimeException("Could not create the Progress Monitor.", e);
		}
		
		
	}

	/**
	 * Hide the progress monitor.
	 * The progress monitor will not be destroyed.
	 */
	protected void hideProgressMonitor() {
		if(progressDialog != null){
			progressDialog.setVisible(false);
			progressDialog.dispose();
		}
	}
	
	/**
	 * Check, if the user has canceled the task.
	 * @return true, if the user has canceled the task, otherwise false.
	 */
	protected boolean isAborted() {
		if(progressDialog != null && progressDialog.isStop()){
			return true;
		}
		return false;
		
	}
}
