package net.sourceforge.squirrel_sql.plugins.syntax;

import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanel;
import net.sourceforge.squirrel_sql.client.session.ISQLEntryPanelFactory;
import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;
import net.sourceforge.squirrel_sql.plugins.syntax.rsyntax.RSyntaxSQLEntryAreaFactory;

import java.util.HashMap;


public class SQLEntryPanelFactoryProxy implements ISQLEntryPanelFactory
{
	private static final StringManager s_stringMgr =
		StringManagerFactory.getStringManager(SQLEntryPanelFactoryProxy.class);


   private SyntaxPlugin _syntaxPugin;

   /** The original Squirrel SQL CLient factory for creating SQL entry panels. */
   private ISQLEntryPanelFactory _originalFactory;

   private RSyntaxSQLEntryAreaFactory _rsyntaxFactory;


   SQLEntryPanelFactoryProxy(SyntaxPlugin syntaxPugin, ISQLEntryPanelFactory originalFactory)
   {
      _originalFactory = originalFactory;
      _rsyntaxFactory = new RSyntaxSQLEntryAreaFactory(syntaxPugin);
      _syntaxPugin = syntaxPugin;
   }

   public void sessionEnding(ISession session)
   {
      _rsyntaxFactory.sessionEnding(session);
   }

   public ISQLEntryPanel createSQLEntryPanel(final ISession session, HashMap<String, Object> props)
   {
      if (session == null)
      {
         throw new IllegalArgumentException("Null ISession passed");
      }

      SyntaxPreferences prefs = getPreferences(session);
      ISQLEntryPanel pnl = getPanel(session);


      ISQLEntryPanel newPnl;


      if (prefs.getUseRSyntaxTextArea())
      {
         newPnl = _rsyntaxFactory.createSQLEntryPanel(session, props);
      }
      else
      {
         newPnl = _originalFactory.createSQLEntryPanel(session, props);
      }

      new ToolsPopupHandler(_syntaxPugin).initToolsPopup(props, newPnl);

      new AutoCorrector(newPnl.getTextComponent(), _syntaxPugin);

      if(null == pnl || false == newPnl.getClass().equals(pnl.getClass()))
      {
         removePanel(session);
         savePanel(session, newPnl);
      }

      return newPnl;
   }


   private SyntaxPreferences getPreferences(ISession session)
   {
      return (SyntaxPreferences)session.getPluginObject(_syntaxPugin,
         IConstants.ISessionKeys.PREFS);
   }

   private void removePanel(ISession session)
   {
      session.removePluginObject(_syntaxPugin, IConstants.ISessionKeys.SQL_ENTRY_CONTROL);
   }

   private ISQLEntryPanel getPanel(ISession session)
   {
      return (ISQLEntryPanel)session.getPluginObject(_syntaxPugin, IConstants.ISessionKeys.SQL_ENTRY_CONTROL);
   }

   private void savePanel(ISession session, ISQLEntryPanel pnl)
   {
      session.putPluginObject(_syntaxPugin, IConstants.ISessionKeys.SQL_ENTRY_CONTROL, pnl);
   }



}

