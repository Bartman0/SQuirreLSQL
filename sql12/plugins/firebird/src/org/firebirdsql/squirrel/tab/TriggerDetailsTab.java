package org.firebirdsql.squirrel.tab;
/*
 * Copyright (C) 2002 Colin Bell
 * colbell@users.sourceforge.net
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
import java.sql.PreparedStatement;
import java.sql.SQLException;

import net.sourceforge.squirrel_sql.fw.sql.IDatabaseObjectInfo;
import net.sourceforge.squirrel_sql.fw.util.log.ILogger;
import net.sourceforge.squirrel_sql.fw.util.log.LoggerController;

import net.sourceforge.squirrel_sql.client.session.ISession;
/**
 * This class will display the details for an Oracle trigger.
 *
 * @author <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class TriggerDetailsTab extends BasePreparedStatementTab
{
	/**
	 * This interface defines locale specific strings. This should be
	 * replaced with a property file.
	 */
	private interface i18n
	{
		String TITLE = "Details";
		String HINT = "Display trigger details";
	}

	/** SQL that retrieves the data. */
	private static String SQL =
		"select rdb$trigger_name, " +
        "rdb$trigger_sequence, " +
        "case rdb$trigger_type " +
        "  when 1 then 'BEFORE INSERT' " +
        "  when 2 then 'AFTER INSERT' " +
        "  when 3 then 'BEFORE UPDATE' " +
        "  when 4 then 'AFTER UPDATE' " +
        "  when 5 then 'BEFORE DELETE' " +
        "  when 6 then 'AFTER DELETE' " +
        "  else 'UNKNOWN TYPE' || rdb$trigger_type " +
        "end as rdb$trigger_type, " +
        "case rdb$trigger_inactive " +
        "  when 0 then 'ACTIVE' " +
        "  when 1 then 'INACTIVE' " +
        "  else 'UNKNOWN' " +
        "end as rdb$trigger_active, " +
		"rdb$description " +
		"from rdb$triggers where " +
		"  rdb$trigger_name = ?";

	/** Logger for this class. */
	private final static ILogger s_log =
		LoggerController.createLogger(TriggerDetailsTab.class);

	public TriggerDetailsTab()
	{
		super(i18n.TITLE, i18n.HINT, true);
	}

	protected PreparedStatement createStatement() throws SQLException
	{
		ISession session = getSession();
		PreparedStatement pstmt = session.getSQLConnection().prepareStatement(SQL);
		IDatabaseObjectInfo doi = getDatabaseObjectInfo();
		pstmt.setString(1, doi.getSimpleName());
		return pstmt;
	}
}
