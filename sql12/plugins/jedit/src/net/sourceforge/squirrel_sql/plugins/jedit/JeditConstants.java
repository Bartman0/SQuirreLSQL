package net.sourceforge.squirrel_sql.plugins.jedit;
/*
 * Copyright (C) 2001-2002 Colin Bell
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

/**
 * Plugin constants.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
interface JeditConstants
{
	/** Name of file to store user prefs in. */
	static final String USER_PREFS_FILE_NAME = "JeditPrefs.xml";

	/** Default font used in the text control. */
//	static final FontInfo DEFAULT_FONT_INFO = new FontInfo(new Font(
//				"Monospaced", Font.PLAIN, 14));

	/** Keys to objects stored in session. */
	interface ISessionKeys
	{
		/** The sessions <TT>JeditPreferences</TT> object. */
		String PREFS = "prefs";
		String JEDIT_SQL_ENTRY_CONTROL = "sqlentry";
	}
}
