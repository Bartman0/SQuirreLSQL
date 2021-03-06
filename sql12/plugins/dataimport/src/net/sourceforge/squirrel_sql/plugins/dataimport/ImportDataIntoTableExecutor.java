package net.sourceforge.squirrel_sql.plugins.dataimport;
/*
 * Copyright (C) 2007 Thorsten Mürell
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.Date;
import java.util.List;

import javax.swing.JOptionPane;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.sql.ISQLConnection;
import net.sourceforge.squirrel_sql.fw.sql.ITableInfo;
import net.sourceforge.squirrel_sql.fw.sql.SQLUtilities;
import net.sourceforge.squirrel_sql.fw.sql.TableColumnInfo;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;
import net.sourceforge.squirrel_sql.plugins.dataimport.gui.ColumnMappingTableModel;
import net.sourceforge.squirrel_sql.plugins.dataimport.gui.ProgressBarDialog;
import net.sourceforge.squirrel_sql.plugins.dataimport.gui.SpecialColumnMapping;
import net.sourceforge.squirrel_sql.plugins.dataimport.importer.IFileImporter;
import net.sourceforge.squirrel_sql.plugins.dataimport.importer.UnsupportedFormatException;
import net.sourceforge.squirrel_sql.plugins.dataimport.prefs.DataImportPreferenceBean;
import net.sourceforge.squirrel_sql.plugins.dataimport.prefs.PreferencesManager;
import net.sourceforge.squirrel_sql.plugins.dataimport.util.DateUtils;

/**
 * This class does the main work for importing the file into the database.
 * 
 * @author Thorsten Mürell
 */
public class ImportDataIntoTableExecutor
{
	private final static ILogger log = LoggerController.createLogger(ImportDataIntoTableExecutor.class);

	/**
	 * Internationalized strings for this class.
	 */
	private static final StringManager stringMgr =
			StringManagerFactory.getStringManager(ImportDataIntoTableExecutor.class);

	/**
	 * the thread we do the work in
	 */
	private Thread execThread = null;

	private ISession session = null;
	private ITableInfo table = null;
	private TableColumnInfo[] columns = null;
	private ColumnMappingTableModel columnMapping = null;
	private IFileImporter importer = null;
	private List<String> importerColumns = null;
	private boolean skipHeader = false;

	private final boolean _singleTransaction;
	private final int _commitAfterEveryInserts;


	/**
	 * The standard constructor
	 *
	 * @param session                 The session
	 * @param table                   The table to import into
	 * @param columns                 The columns of the destination table
	 * @param importerColumns         The columns of the importer
	 * @param mapping                 The mapping of the columns
	 * @param importer                The file importer
	 * @param singleTransaction
	 * @param commitAfterEveryInserts
	 */
	public ImportDataIntoTableExecutor(ISession session,
												  ITableInfo table,
												  TableColumnInfo[] columns,
												  List<String> importerColumns,
												  ColumnMappingTableModel mapping,
												  IFileImporter importer,
												  boolean singleTransaction,
												  int commitAfterEveryInserts)
	{
		this.session = session;
		this.table = table;
		this.columns = columns;
		this.columnMapping = mapping;
		this.importer = importer;
		this.importerColumns = importerColumns;

		_singleTransaction = singleTransaction;
		_commitAfterEveryInserts = commitAfterEveryInserts;
	}

	/**
     * If the header should be skipped
     * 
     * @param skip
     */
    public void setSkipHeader(boolean skip) {
    	skipHeader = skip;
    }

	/**
    * Starts the thread that executes the insert operation.
    */
   public void execute()
	{
		Runnable runnable = new Runnable()
      {
         public void run()
         {
            _execute(_singleTransaction, _commitAfterEveryInserts);
         }
      };
		execThread = new Thread(runnable);
		execThread.setName("Dataimport Executor Thread");
		execThread.setUncaughtExceptionHandler(createUncaughtExceptionHandler());
		execThread.start();
	}

	/**
     * Performs the table copy operation.
	  * @param singleTransaction
	  * @param commitAfterEveryInserts
	  */
	 private void _execute(boolean singleTransaction, int commitAfterEveryInserts)
	 {
		 // Create column list
		 String columnList = createColumnList();
		 ISQLConnection conn = session.getSQLConnection();

		 StringBuffer insertSQL = new StringBuffer();
		 insertSQL.append("insert into ").append(table.getQualifiedName());
		 insertSQL.append(" (").append(columnList).append(") ");
		 insertSQL.append("VALUES ");
		 insertSQL.append(" (").append(getQuestionMarks(getColumnCount())).append(")");

		 PreparedStatement stmt = null;
		 int currentRow = 0;
		 boolean success = false;

		 boolean originalAutoCommit = getOriginalAutoCommit(conn);

		 try
		 {
			 conn.setAutoCommit(false);

			 DataImportPreferenceBean settings = PreferencesManager.getPreferences();
			 importer.open();
			 if (skipHeader)
				 importer.next();

			 if (settings.isUseTruncate())
			 {
				 String sql = "DELETE FROM " + table.getQualifiedName();
				 stmt = conn.prepareStatement(sql);
				 stmt.execute();
				 stmt.close();
			 }

			 stmt = conn.prepareStatement(insertSQL.toString());
			 //i18n[ImportDataIntoTableExecutor.importingDataInto=Importing data into {0}]
			 ProgressBarDialog.getDialog(session.getApplication().getMainFrame(), stringMgr.getString("ImportDataIntoTableExecutor.importingDataInto", table.getSimpleName()), false, null);
			 int inputLines = importer.getRows();
			 if (inputLines > 0)
			 {
				 ProgressBarDialog.setBarMinMax(0, inputLines == -1 ? 5000 : inputLines);
			 }
			 else
			 {
				 ProgressBarDialog.setIndeterminate();
			 }

			 while (importer.next())
			 {
				 currentRow++;
				 if (inputLines > 0)
				 {
					 ProgressBarDialog.incrementBar(1);
				 }
				 stmt.clearParameters();
				 int i = 1;
				 for (TableColumnInfo column : columns)
				 {
					 String mapping = getMapping(column);
					 try
					 {
						 if (SpecialColumnMapping.SKIP.getVisibleString().equals(mapping))
						 {
							 continue;
						 }
						 else if (SpecialColumnMapping.FIXED_VALUE.getVisibleString().equals(mapping))
						 {
							 bindFixedColumn(stmt, i++, column);
						 }
						 else if (SpecialColumnMapping.AUTO_INCREMENT.getVisibleString().equals(mapping))
						 {
							 bindAutoincrementColumn(stmt, i++, column, currentRow);
						 }
						 else if (SpecialColumnMapping.NULL.getVisibleString().equals(mapping))
						 {
							 stmt.setNull(i++, column.getDataType());
						 }
						 else
						 {
							 bindColumn(stmt, i++, column);
						 }
					 }
					 catch (UnsupportedFormatException ufe)
					 {
						 // i18n[ImportDataIntoTableExecutor.wrongFormat=Imported column has not the required format.\nLine is: {0}, column is: {1}]
						 showMessageDialogOnEDT(stringMgr.getString("ImportDataIntoTableExecutor.wrongFormat", ufe.getMessage(), currentRow, i - 1, column.getColumnName()));
						 throw ufe;
					 }
				 }
				 stmt.execute();

				 if (false == singleTransaction)
				 {
					 if(0 < currentRow && 0 == currentRow % commitAfterEveryInserts)
                {
                   conn.commit();
                }
				 }
			 }
			 importer.close();
			 success = true;
		 }
		 catch (SQLException sqle)
		 {
			 //i18n[ImportDataIntoTableExecutor.sqlException=A database error occurred while inserting data]
			 //i18n[ImportDataIntoTableExecutor.error=Error]
			 _showMessageDialogOnEDT(
					 stringMgr.getString("ImportDataIntoTableExecutor.sqlException", sqle.getMessage(), Integer.toString(currentRow)),
					 stringMgr.getString("ImportDataIntoTableExecutor.error"));


			 String query = stmt == null ? "null" : stmt.toString();
			 if (query.length() >= 1024000)
			 {   //with the safety switch off it's possible that this query is a few Mb long !
				 query = query.substring(0, 1024000) + "... (truncated)";
			 }
			 log.error("Failing query: " + query);
			 log.error("Failing line in CVS file: " + currentRow);
			 log.error("Database error", sqle);
		 }
		 catch (UnsupportedFormatException ufe)
		 {
			 log.error("Unsupported format.", ufe);
		 }
		 catch (IOException ioe)
		 {
			 //i18n[ImportDataIntoTableExecutor.ioException=An error occurred while reading the input file.]
			 _showMessageDialogOnEDT(
					 stringMgr.getString("ImportDataIntoTableExecutor.ioException", ioe.getMessage(), Integer.toString(currentRow)),
					 stringMgr.getString("ImportDataIntoTableExecutor.error"));

			 log.error("Error while reading file", ioe);
		 }
		 finally
		 {
			 try
			 {
				 finishTransaction(conn, success);
			 }
			 finally
			 {
				 setOriginalAutoCommit(conn, originalAutoCommit);
				 SQLUtilities.closeStatement(stmt);
				 ProgressBarDialog.dispose();
			 }
		 }

		 if (success)
		 {
			 //i18n[ImportDataIntoTableExecutor.success={0,choice,0#No records|1#One record|1<{0} records} successfully inserted.]
			 showMessageDialogOnEDT(stringMgr.getString("ImportDataIntoTableExecutor.success", currentRow));
		 }
	 }

	private Thread.UncaughtExceptionHandler createUncaughtExceptionHandler()
	{
		return new Thread.UncaughtExceptionHandler()
		{
			@Override
			public void uncaughtException(Thread t, final Throwable e)
			{
				GUIUtils.processOnSwingEventThread(new Runnable()
				{
					@Override
					public void run()
					{
						throw new RuntimeException(e);
					}
				});
			}
		};
	}


	private void finishTransaction(ISQLConnection conn, boolean success)
	{
		try
		{
			if(success)
         {
            conn.commit();
         }
         else
         {
            conn.rollback();
         }
		}
		catch (SQLException e)
		{
			try
			{
				conn.rollback();
			}
			catch (SQLException e1)
			{
				log.error("Finally failed to rollback connection");
			}
			throw new RuntimeException(e);
		}
	}

	private void setOriginalAutoCommit(ISQLConnection conn, boolean originalAutoCommit)
	{
		try
		{
			conn.setAutoCommit(originalAutoCommit);
		}
		catch (SQLException e)
		{
			_showMessageDialogOnEDT(
					stringMgr.getString("ImportDataIntoTableExecutor.reestablish.autocommit.failed"),
					stringMgr.getString("ImportDataIntoTableExecutor.reestablish.autocommit.failed.title"),
					JOptionPane.ERROR_MESSAGE);

			throw new RuntimeException(e);
		}
	}

	private boolean getOriginalAutoCommit(ISQLConnection conn)
	{
		try
		{
			return conn.getAutoCommit();
		}
		catch (SQLException e)
		{
			throw new RuntimeException(e);
		}
	}

	private void _showMessageDialogOnEDT(String string, String title)
	{
		_showMessageDialogOnEDT(string, title, JOptionPane.ERROR_MESSAGE);
	}

	private void showMessageDialogOnEDT(final String message)
	{
		_showMessageDialogOnEDT(message, null, JOptionPane.DEFAULT_OPTION);
	}


	private void _showMessageDialogOnEDT(final String message, final String title, final int messageType)
	{
		GUIUtils.processOnSwingEventThread(new Runnable()
		{
			@Override
			public void run()
			{
				if (null == title)
				{
					JOptionPane.showMessageDialog(session.getApplication().getMainFrame(), message);
				}
				else
				{
					JOptionPane.showMessageDialog(session.getApplication().getMainFrame(), message, title, messageType);
				}
			}
		}, true);
	}


	private void bindAutoincrementColumn(PreparedStatement stmt, int index, TableColumnInfo column, int counter) throws SQLException, UnsupportedFormatException  {
    	long value = 0;
		String fixedValue = getFixedValue(column);
    	try {
			value = Long.parseLong(fixedValue);
    		value += counter;
    	} catch (NumberFormatException nfe) {
    		throw new UnsupportedFormatException("Could not interpret value for column " + column.getColumnName() + " as value of type long. The value is: " + fixedValue, nfe);
    	}
    	switch (column.getDataType()) {
    	case Types.BIGINT:
   			stmt.setLong(index, value);
    		break;
    	case Types.INTEGER:
    	case Types.NUMERIC:
   			stmt.setInt(index, (int)value);
    		break;
    	default:
    		throw new UnsupportedFormatException("Autoincrement column " + column.getColumnName() + "  is not numeric");
    	}
	}

	private void bindFixedColumn(PreparedStatement stmt, int index, TableColumnInfo column) throws SQLException, IOException, UnsupportedFormatException {
    	String value = getFixedValue(column);
    	Date d = null;
    	switch (column.getDataType()) {
    	case Types.BIGINT:
    		try {
    			stmt.setLong(index, Long.parseLong(value));
    		} catch (NumberFormatException nfe) {
    			throw new UnsupportedFormatException(nfe);
			}
    		break;
    	case Types.INTEGER:
    	case Types.NUMERIC:
    		setIntOrUnsignedInt(stmt, index, column);
    		break;
    	case Types.DATE:
    		// Null values should be allowed
    		setDateOrNull(stmt, index, value);
    		break;
    	case Types.TIMESTAMP:
    		// Null values should be allowed
    		setTimeStampOrNull(stmt, index, value);
    		break;
    	case Types.TIME:    	
    		// Null values should be allowed
    		setTimeOrNull(stmt, index, value);
    		break;
    	default:
    		stmt.setString(index, value);
    	}
    }

	private void setDateOrNull(PreparedStatement stmt, int index,
			String value) throws UnsupportedFormatException, SQLException {
		if (null != value) {
			Date d = DateUtils.parseSQLFormats(value);
			if (d == null)
				throw new UnsupportedFormatException("Could not interpret value as date type. Value is: " + value);
			stmt.setDate(index, new java.sql.Date(d.getTime()));
		} else {
			stmt.setNull(index, Types.DATE);
		}
	}
	
	private void setTimeStampOrNull(PreparedStatement stmt, int index,
			String value) throws UnsupportedFormatException, SQLException {
		if (null != value) {
			Date d = DateUtils.parseSQLFormats(value);
			if (d == null)
				throw new UnsupportedFormatException("Could not interpret value as date type. Value is: " + value);
			stmt.setTimestamp(index, new java.sql.Timestamp(d.getTime()));
		} else {
			stmt.setNull(index, Types.TIMESTAMP);
		}
	}
	
	private void setTimeOrNull(PreparedStatement stmt, int index, String value)
			throws UnsupportedFormatException, SQLException {
		if (null != value) {
			Date d = DateUtils.parseSQLFormats(value);
			if (d == null)
				throw new UnsupportedFormatException("Could not interpret value as date type. Value is: " + value);
			stmt.setTime(index, new java.sql.Time(d.getTime()));
		} else {
			stmt.setNull(index, Types.TIME);
		}
	}
    
    private void bindColumn(PreparedStatement stmt, int index, TableColumnInfo column) throws SQLException, UnsupportedFormatException, IOException {
    	int mappedColumn = getMappedColumn(column);
		switch (column.getDataType()) {
    	case Types.BIGINT:    		
    		setLongOrNull(stmt, index, mappedColumn);    		
    		break;
    	case Types.INTEGER:
    	case Types.NUMERIC:
    		setIntOrUnsignedInt(stmt, index, column);
    		break;
    	case Types.DATE:
    		setDateOrNull(stmt, index, mappedColumn);
    		break;
    	case Types.TIMESTAMP:
    		setTimestampOrNull(stmt, index, mappedColumn);
    		break;
    	case Types.TIME:
    		setTimeOrNull(stmt, index, mappedColumn);
    		break;
    	default:
    		setStringOrNull(stmt, index, mappedColumn);
    	}
    }

	private void setStringOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws SQLException, IOException {
		String string = importer.getString(mappedColumn);
		if (null != string) {
			stmt.setString(index, string);
		} else {
			stmt.setNull(index, Types.VARCHAR);
		}
	}

	private void setTimeOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws SQLException, IOException,
			UnsupportedFormatException {
		Date date = importer.getDate(mappedColumn);
		if (null != date) {
			stmt.setTime(index, new java.sql.Time(date.getTime()));
		} else {
			stmt.setNull(index, Types.TIME);
		}
	}

	private void setTimestampOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws SQLException, IOException,
			UnsupportedFormatException {
		Date date = importer.getDate(mappedColumn);
		if (null != date) {
			stmt.setTimestamp(index, new java.sql.Timestamp(date.getTime()));
		} else {
			stmt.setNull(index, Types.TIMESTAMP);
		}
	}

	private void setDateOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws SQLException, IOException,
			UnsupportedFormatException {
		Date date = importer.getDate(mappedColumn);
		if (null != date) {
			stmt.setDate(index, new java.sql.Date(date.getTime()));
		} else {
			stmt.setNull(index, Types.DATE);
		}

	}
    
	/*
	 * 1968807: Unsigned INT problem with IMPORT FILE functionality
	 * 
	 * If we are working with a signed integer, then it should be ok to store in
	 * a Java integer which is always signed. However, if we are working with an
	 * unsigned integer type, Java doesn't have this so use a long instead.
	 */    
    private void setIntOrUnsignedInt(PreparedStatement stmt, int index, TableColumnInfo column) 
    	throws SQLException, UnsupportedFormatException, IOException 
    {
    	int mappedColumn = getMappedColumn(column);
  		String columnTypeName = column.getTypeName(); 
 		if (columnTypeName != null 
 				&& (columnTypeName.endsWith("UNSIGNED") || columnTypeName.endsWith("unsigned"))) 
 		{
 			setLongOrNull(stmt, index, mappedColumn);			
 		}
 		
 		try {
 			setIntOrNull(stmt, index, mappedColumn);			
 		} catch (UnsupportedFormatException e) {
 			//
 			// Too much logs slow down the system in case of 
 			// large imports ( > 10000)
 			//
 			// log.error("bindColumn: integer storage overflowed.  Exception was "+e.getMessage()+
 			//			 ".  Re-trying as a long.", e);
 			/* try it as a long in case the database driver didn't correctly identify an unsigned field */
 			setLongOrNull(stmt, index, mappedColumn);			
 		}
    }

	private void setLongOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws IOException, UnsupportedFormatException,
			SQLException {
		Long long1 = importer.getLong(mappedColumn);
		if (null == long1) {
			stmt.setNull(index, Types.INTEGER);
		} else {
			stmt.setLong(index, long1);
		}
	}

	private void setIntOrNull(PreparedStatement stmt, int index,
			int mappedColumn) throws IOException, UnsupportedFormatException,
			SQLException {
		Integer int1 = importer.getInt(mappedColumn);
		if (null == int1) {
			stmt.setNull(index, Types.INTEGER);
		} else {
			stmt.setInt(index, int1);
		}
	}
    
    private int getMappedColumn(TableColumnInfo column) {
    	return importerColumns.indexOf(getMapping(column));
    }
    
    private String getMapping(TableColumnInfo column) {
		int pos = columnMapping.findTableColumn(column.getColumnName());
		return columnMapping.getValueAt(pos, 1).toString();
    }
    
    private String getFixedValue(TableColumnInfo column) {
		int pos = columnMapping.findTableColumn(column.getColumnName());
		return columnMapping.getValueAt(pos, 2).toString();
    }
    
    private String createColumnList() {
    	StringBuffer columnsList = new StringBuffer();
    	for (TableColumnInfo column : columns) {
    		String mapping = getMapping(column);
    		if (SpecialColumnMapping.SKIP.getVisibleString().equals(mapping)) continue;
    		
    		if (columnsList.length() != 0) {
    			columnsList.append(", ");
    		}
    		columnsList.append(column.getColumnName());
    	}
    	return columnsList.toString();
    }
    
    private int getColumnCount() {
    	int count = 0;
    	for (TableColumnInfo column : columns) {
    		int pos = columnMapping.findTableColumn(column.getColumnName());
    		String mapping = columnMapping.getValueAt(pos, 1).toString();
    		if (!SpecialColumnMapping.SKIP.getVisibleString().equals(mapping)) {
    			count++;
    		}
    	}
    	return count;
    }
    
    private String getQuestionMarks(int count) {
        StringBuffer result = new StringBuffer();
        for (int i = 0; i < count; i++) {
            result.append("?");
            if (i < count-1) {
                result.append(", ");
            }
        }
        return result.toString();
    }    

}
