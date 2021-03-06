package net.sourceforge.squirrel_sql.plugins.graph.window;

import net.sourceforge.squirrel_sql.client.session.ISession;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.plugins.graph.GraphMainPanelTab;
import net.sourceforge.squirrel_sql.plugins.graph.GraphPanelController;
import net.sourceforge.squirrel_sql.plugins.graph.GraphPlugin;
import net.sourceforge.squirrel_sql.plugins.graph.LazyLoadListener;

import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

public class TabToWindowHandler
{
   private GraphMainPanelTab _graphMainPanelTab;
   private ISession _session;
   private GraphPlugin _plugin;
   private boolean _link;
   private GraphWindowController _graphWindowController;
   private LazyLoadListener _lazyLoadListener;

   public TabToWindowHandler(GraphPanelController panelController, ISession session, GraphPlugin plugin, boolean isLink)
   {
      _session = session;
      _plugin = plugin;
      _link = isLink;
      _graphMainPanelTab = new GraphMainPanelTab(panelController, plugin, _link);
      _graphMainPanelTab.getToWindowButton().addActionListener(new ActionListener()
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            toWindow();
         }
      });
   }

   private void toWindow()
   {
      _lazyLoadListener.lazyLoadTables();

      Dimension size = _graphMainPanelTab.getComponent().getSize();
      Point screenLoc = GUIUtils.getScreenLocationFor(_graphMainPanelTab.getComponent());

      Rectangle tabBoundsOnScreen = new Rectangle();
      tabBoundsOnScreen.x =screenLoc.x;
      tabBoundsOnScreen.y =screenLoc.y;
      tabBoundsOnScreen.width = size.width;
      tabBoundsOnScreen.height = size.height;


      toWindowAtBounds(tabBoundsOnScreen);
   }

   private void toWindowAtBounds(Rectangle tabBoundsOnScreen)
   {
      final int tabIdx = _session.getSessionSheet().removeMainTab(_graphMainPanelTab);

      GraphWindowControllerListener listener = new GraphWindowControllerListener()
      {
         @Override
         public void closing(int tabIdx)
         {
            onWindowClosing(tabIdx);
         }
      };

      _graphWindowController =
            new GraphWindowController(_session,
                                      _plugin,
                                      _graphMainPanelTab,
                                      tabIdx,
                                      tabBoundsOnScreen,
                                      listener,
                                      _link);
   }

   private void onWindowClosing(int tabIdx)
   {
      _graphWindowController = null;

      if(tabIdx <_session.getSessionSheet().getTabCount())
      {
         _session.getSessionSheet().insertMainTab(_graphMainPanelTab, tabIdx);
      }
      else
      {
         tabIdx = _session.getSessionSheet().addMainTab(_graphMainPanelTab);
         _session.getSessionSheet().selectMainTab(tabIdx);
      }

   }

   public void showGraph(LazyLoadListener lazyLoadListener, boolean selectTab)
   {
      _lazyLoadListener = lazyLoadListener;
      _graphMainPanelTab.setLazyLoadListener(_lazyLoadListener);
      int tabIdx = _session.getSessionSheet().addMainTab(_graphMainPanelTab);

      if (selectTab)
      {
         _session.getSessionSheet().selectMainTab(tabIdx);
      }
   }

   public void removeGraph()
   {
      if (null == _graphWindowController)
      {
         _session.getSessionSheet().removeMainTab(_graphMainPanelTab);
      }
      else
      {
         _graphWindowController.close();
         _graphWindowController = null;
      }
   }

   public void renameGraph(String newName)
   {
      _graphMainPanelTab.setTitle(newName);

      if (null == _graphWindowController)
      {
         int index = _session.getSessionSheet().removeMainTab(_graphMainPanelTab);
         _session.getSessionSheet().insertMainTab(_graphMainPanelTab, index);
      }
      else
      {
         _graphWindowController.rename(newName);
      }
   }

   public String getTitle()
   {
      return _graphMainPanelTab.getTitle();
   }

   public void setTitle(String title)
   {
      _graphMainPanelTab.setTitle(title);
   }

   public void showInWindowBesidesObjectTree()
   {
      _session.selectMainTab(ISession.IMainPanelTabIndexes.OBJECT_TREE_TAB);

      Component detailTabComp = _session.getObjectTreeAPIOfActiveSessionWindow().getDetailTabComp();
      Point locOnScreen = GUIUtils.getScreenLocationFor(detailTabComp);

      Rectangle bounds = new Rectangle(locOnScreen.x, locOnScreen.y, detailTabComp.getWidth(), detailTabComp.getHeight());

      toWindowAtBounds(bounds);
   }

   public void toggleWindowTab()
   {
      if(null == _graphWindowController)
      {
         toWindow();
      }
      else
      {
         _graphWindowController.returnToTab();
      }
   }

   public void changedFromLinkToLocalCopy()
   {
      _link = false;
      if(null == _graphWindowController)
      {
         _graphMainPanelTab.changedFromLinkToLocalCopy();
      }
      else
      {
         _graphWindowController.changedFromLinkToLocalCopy();
      }
   }

   public Component getComponent()
   {
      if(null == _graphWindowController)
      {
         return _graphMainPanelTab.getComponent();
      }
      else
      {
         return _graphWindowController.getComponent();
      }
   }

   public boolean isMyGraphMainPanelTab(GraphMainPanelTab graphMainPanelTab)
   {
      return _graphMainPanelTab == graphMainPanelTab;
   }
}
