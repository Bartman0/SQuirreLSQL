package net.sourceforge.squirrel_sql.client.session.mainpanel.overview;


import net.sourceforge.squirrel_sql.client.IApplication;
import net.sourceforge.squirrel_sql.client.session.mainpanel.overview.datascale.DataScale;
import net.sourceforge.squirrel_sql.client.session.mainpanel.overview.datascale.DataScaleTable;
import net.sourceforge.squirrel_sql.fw.gui.GUIUtils;
import net.sourceforge.squirrel_sql.fw.util.StringManager;
import net.sourceforge.squirrel_sql.fw.util.StringManagerFactory;

import javax.swing.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class ChartConfigPanelTabController
{
   private static final StringManager s_stringMgr = StringManagerFactory.getStringManager(ChartConfigController.class);
   private IApplication _app;
   private ChartConfigPanelTabMode _chartConfigPanelTabMode;
   private ChartConfigPanelTab _chartConfigPanelTab;
   private DataScaleTable _dataScaleTable;


   public ChartConfigPanelTabController(IApplication app, ChartConfigPanelTabMode chartConfigPanelTabMode)
   {
      _app = app;
      _chartConfigPanelTabMode = chartConfigPanelTabMode;
      _chartConfigPanelTab = new ChartConfigPanelTab(chartConfigPanelTabMode);

      if(chartConfigPanelTabMode != ChartConfigPanelTabMode.XY_CHART)
      {
         _chartConfigPanelTab.cboCallDepth.setModel(new DefaultComboBoxModel(CallDepthComboModel.createModels()));
         _chartConfigPanelTab.cboCallDepth.setSelectedItem(CallDepthComboModel.getDefaultSelected());
      }

      if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.SINGLE_COLUMN)
      {
         _chartConfigPanelTab.cboXColumns.addItemListener(new ItemListener()
         {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
               onColumnSelected(e, ChartConfigPanelTabController.this._chartConfigPanelTab.cboXColumns);
            }
         });
      }
      else if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.TWO_COLUMN)
      {
         _chartConfigPanelTab.cboYColumns.addItemListener(new ItemListener()
         {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
               onColumnSelected(e, ChartConfigPanelTabController.this._chartConfigPanelTab.cboYColumns);
            }
         });
      }
      else if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.XY_CHART)
      {
         _chartConfigPanelTab.cboYColumns.addItemListener(new ItemListener()
         {
            @Override
            public void itemStateChanged(ItemEvent e)
            {
               onColumnSelected(e, ChartConfigPanelTabController.this._chartConfigPanelTab.cboYColumns);
            }
         });
      }

      _chartConfigPanelTab.btnChart.addActionListener(new ActionListener()
      {
         @Override
         public void actionPerformed(ActionEvent e)
         {
            onChart();
         }
      });

   }

   public void setDataScaleTable(DataScaleTable dataScaleTable)
   {
      _dataScaleTable = dataScaleTable;


      if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.SINGLE_COLUMN)
      {
         fillColumnCombo(_chartConfigPanelTab.cboXColumns, false);
      }
      else if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.TWO_COLUMN)
      {
         fillColumnCombo(_chartConfigPanelTab.cboXColumns, false);
         fillColumnCombo(_chartConfigPanelTab.cboYColumns, false);
      }
      else if (_chartConfigPanelTabMode == ChartConfigPanelTabMode.XY_CHART)
      {
         fillColumnCombo(_chartConfigPanelTab.cboXColumns, true);
         fillColumnCombo(_chartConfigPanelTab.cboYColumns, true);
      }


      onColumnSelected(null, _chartConfigPanelTab.cboXColumns);
   }

   private void fillColumnCombo(JComboBox cboColumns, boolean numbersOnly)
   {
      ColumnComboModel formerSelectedItem = (ColumnComboModel) cboColumns.getSelectedItem();
      DefaultComboBoxModel defaultComboBoxModel = new DefaultComboBoxModel(ColumnComboModel.createColumnComboModels(_dataScaleTable, numbersOnly));
      cboColumns.setModel(defaultComboBoxModel);
      cboColumns.setSelectedItem(formerSelectedItem);


      if(null == cboColumns.getSelectedItem() && 0 < cboColumns.getItemCount())
      {
         cboColumns.setSelectedIndex(0);
      }
   }


   private void onColumnSelected(ItemEvent e, JComboBox cboColumns)
   {
      if(_chartConfigPanelTabMode == ChartConfigPanelTabMode.XY_CHART)
      {
         return;
      }

      if (null == e || ItemEvent.SELECTED == e.getStateChange())
      {
         ColumnComboModel selectedColumn = (ColumnComboModel) cboColumns.getSelectedItem();
         _chartConfigPanelTab.cboYAxisKind.setModel(new DefaultComboBoxModel(ChartConfigMode.getAvailableValues(selectedColumn.getColumnDisplayDefinition(), _chartConfigPanelTabMode,s_stringMgr)));
      }
   }


   private void onChart()
   {
      ColumnComboModel selectedXAxisColumn = (ColumnComboModel) _chartConfigPanelTab.cboXColumns.getSelectedItem();
      DataScale xAxisDataScale = _dataScaleTable.getDataScaleTableModel().getDataScaleAt(selectedXAxisColumn.getColumnIndexInDataScale());

      DataScale yAxisDataScale = null;
      if(_chartConfigPanelTabMode == ChartConfigPanelTabMode.TWO_COLUMN || _chartConfigPanelTabMode == ChartConfigPanelTabMode.XY_CHART)
      {
         ColumnComboModel selectedYAxisColumn = (ColumnComboModel) _chartConfigPanelTab.cboYColumns.getSelectedItem();
         yAxisDataScale = _dataScaleTable.getDataScaleTableModel().getDataScaleAt(selectedYAxisColumn.getColumnIndexInDataScale());
      }

      Integer callDepth = null;
      ChartConfigMode chartConfigMode = null;

      if (_chartConfigPanelTabMode != ChartConfigPanelTabMode.XY_CHART)
      {
         CallDepthComboModel selItem = (CallDepthComboModel) _chartConfigPanelTab.cboCallDepth.getSelectedItem();
         callDepth = selItem.getCallDepth();
         chartConfigMode = (ChartConfigMode) _chartConfigPanelTab.cboYAxisKind.getSelectedItem();
      }

      ChartHandler.doChart(xAxisDataScale, yAxisDataScale, _dataScaleTable, callDepth, _chartConfigPanelTabMode, chartConfigMode, _app.getResources(), GUIUtils.getOwningFrame(_dataScaleTable));
   }


   public ChartConfigPanelTab getPanel()
   {
      return _chartConfigPanelTab;
   }
}
