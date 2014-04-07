package org.biomart.configurator.view.gui.dialogs;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Settings;
import org.biomart.common.utils.PartitionUtils;
import org.biomart.common.utils.XMLElements;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.component.MultiDatasourcePanel;
import org.biomart.configurator.controller.MartController;
import org.biomart.configurator.update.UpdateMart;
import org.biomart.configurator.utils.McGuiUtils;
import org.biomart.configurator.utils.McUtils;
import org.biomart.configurator.utils.Validation;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;
import org.biomart.objects.objects.PartitionTable;

public class DatasourceDialog extends JDialog implements ActionListener, ListSelectionListener {

    /**
	 * 
	 */
    private static final long serialVersionUID = 1L;
    private List<Mart> martList; // ordered
    private List<Integer> colList; // selected columns in partitiontable model
    private MultiDatasourcePanel tablePanel;
    private JButton addButton;
    private JButton ptTableButton;
    private JButton updateButton;
    private JMenuBar menuBar;
    private final boolean canAdd;

    /**
     * 
     * @param martList
     *            , ordered in martregistry.
     * @param cols
     * @param canAdd
     */
    public DatasourceDialog(List<Mart> martList, List<Integer> cols, boolean canAdd) {
        this.canAdd = canAdd;
        this.martList = martList;
        this.colList = cols;
        this.init();
        this.pack();
        this.setModal(true);
        this.setLocationRelativeTo(null);
        this.setVisible(true);
    }

    private void init() {
        menuBar = new JMenuBar();
        this.setJMenuBar(menuBar);
        JMenu fileMenu = new JMenu("File");
        JMenuItem partitionItem = fileMenu.add("partition");
        partitionItem.setActionCommand("partition");
        partitionItem.setAccelerator(KeyStroke.getKeyStroke('P', CTRL_DOWN_MASK));
        partitionItem.addActionListener(this);

        menuBar.add(fileMenu);
        menuBar.setVisible(false);

        this.tablePanel = new MultiDatasourcePanel(this.martList, this.colList);
        this.tablePanel.addSelectionListener(this);

        addButton = new JButton("Add");
        addButton.setActionCommand("adddatasource");
        addButton.addActionListener(this);

        ptTableButton = new JButton("View Partition Table");
        ptTableButton.setActionCommand("partitionTable");
        ptTableButton.addActionListener(this);
        if (this.martList.size() != 1) {
            ptTableButton.setEnabled(false);
        }
        ptTableButton.setVisible(Boolean.parseBoolean(Settings.getProperty("showadvancedmenu")));

        updateButton = new JButton("Update");
        updateButton.setActionCommand("update");
        updateButton.addActionListener(this);
        updateButton.setEnabled(false);

        JPanel buttonPanel = new JPanel();
        // FlowLayout layout = new FlowLayout();
        // layout.setAlignment(FlowLayout.LEFT);
        if (this.canAdd)
            buttonPanel.add(addButton);
        buttonPanel.add(updateButton);
        buttonPanel.add(ptTableButton);

        this.setLayout(new BorderLayout());
        this.add(tablePanel, BorderLayout.CENTER);
        this.add(buttonPanel, BorderLayout.SOUTH);
        this.setTitle("Data Source Management");
    }

    @Override
    public void actionPerformed(ActionEvent e) {

        // TODO Auto-generated method stub
        if (e.getActionCommand().equals("partition")) {
            if (martList.size() > 0 && martList.get(0).getPartitionTableList().size() > 0) {
                PartitionTable pt = martList.get(0).getPartitionTableList().get(0);
                if (pt != null)
                    new PartitionTableDialog(this, pt, 0, -1);
            }
        } else if (e.getActionCommand().equals("adddatasource")) {
            AddDatasetDialog addDialog = new AddDatasetDialog(this, "", "", "", "", "", "", "", false, false);
            List<String> newDs = addDialog.getResult();
            if (McUtils.isCollectionEmpty(newDs))
                return;
            List<String> newRow = new ArrayList<String>();
            // FIXME don't hardcode the column
            List<String> displayList = new ArrayList<String>();
            if (newDs.get(0).equals(AddDatasetDialog.DBPANEL)) {
                newRow.add(newDs.get(1)); // connection
                newRow.add(newDs.get(2)); // database
                newRow.add(newDs.get(3)); // schema
                newRow.add(newDs.get(4)); // user name
                newRow.add(newDs.get(5)); // passowrd
                newRow.add(newDs.get(6)); // dataset name
                newRow.add(XMLElements.FALSE_VALUE.toString()); // hide
                newRow.add(newDs.get(7)); // displayname

                displayList.add(newDs.get(6)); // dataset name
                displayList.add(newDs.get(7)); // displayname
                displayList.add(newDs.get(1)); // connection
                displayList.add(newDs.get(2)); // database
                displayList.add(newDs.get(3)); // schema
                displayList.add(XMLElements.FALSE_VALUE.toString());
                displayList.add("");
            } else {
                newRow.add(newDs.get(1)); // connection
                newRow.add(newDs.get(2)); // port
                newRow.add(newDs.get(3)); // host
                newRow.add(newDs.get(4)); //
                newRow.add(newDs.get(5));
                newRow.add(newDs.get(6)); // dataset name
                newRow.add(XMLElements.FALSE_VALUE.toString()); // hide
                newRow.add(newDs.get(7)); // displayname

                displayList.add(newDs.get(6)); // dataset name
                displayList.add(newDs.get(7)); // displayname
                displayList.add(newDs.get(1)); // connection
                displayList.add(newDs.get(2)); // port
                displayList.add(newDs.get(3)); // path
                displayList.add(XMLElements.FALSE_VALUE.toString());
                displayList.add("");

            }

            String commonName = newRow.get(PartitionUtils.DATASETNAME);
            ;
            for (Mart mart : martList) {
                PartitionTable pt = mart.getSchemaPartitionTable();
                // change the dataset name by adding prefix
                String dsName = pt.getValue(0, PartitionUtils.DATASETNAME);
                String tmpName = commonName;
                int index = dsName.lastIndexOf("_");
                if (index >= 0) {
                    String prefix = dsName.substring(0, index);
                    tmpName = prefix + "_" + commonName;
                    newRow.set(PartitionUtils.DATASETNAME, tmpName);
                    // don't change it for multiple mart, keep the common name
                    if (martList.size() == 1)
                        displayList.set(0, tmpName);
                }
                pt.addNewRow(newRow);
            }

            List<Mart> allMart = new ArrayList<Mart>();
            allMart.addAll(this.martList);
            this.tablePanel.addRow(displayList, allMart);
            this.pack();
            // fix partitiontable
            MartController.getInstance().fixPartitionTable();
        } else if (e.getActionCommand().equals("partitionTable")) {

            // get the first partition table
            if (this.martList.size() > 0 && this.martList.get(0).getPartitionTableList().size() > 0) {
                PartitionTable pt = this.martList.get(0).getPartitionTableList().get(0);
                // hardcode for now, from BC url, only create 9 fixed columns
                if (pt.getTotalColumns() < 15) {
                    int count = pt.getTotalColumns();
                    for (int i = count; i < 15; i++) {
                        pt.addColumn("");
                    }
                }
                if (pt != null)
                    new PartitionTableDialog(this, pt, 0, -1);
            }

        } else if (e.getActionCommand().equals("update")) {
            if (JOptionPane
                    .showConfirmDialog(this, "update datasets?", "Data source update", JOptionPane.YES_NO_OPTION) != JOptionPane.YES_OPTION)
                return;
            // update with progressbar
            final ProgressDialog progressMonitor = ProgressDialog.getInstance(this);

            final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {
                @Override
                protected void done() {
                    // Close the progress dialog.
                    progressMonitor.setVisible(false);
                    progressMonitor.stop();
                }

                @Override
                protected Void doInBackground() throws Exception {
                    try {
                        progressMonitor.setStatus("updating...");
                        update(progressMonitor);
                        Validation.validate(McGuiUtils.INSTANCE.getRegistryObject(), false);
                    } catch (final Throwable t) {
                        SwingUtilities.invokeLater(new Runnable() {
                            public void run() {
                                StackTrace.showStackTrace(t);
                            }
                        });
                    } finally {
                        progressMonitor.setVisible(false);
                    }
                    return null;
                }
            };

            worker.execute();
            progressMonitor.start("processing ...");
        }

    }

    private void update(ProgressDialog pd) throws MartBuilderException {
        int n = 0;
        int[] selectedRows = this.tablePanel.getTable().getSelectedRows();
        for (int row : selectedRows) {
            Collection<Mart> marts = this.tablePanel.getMartsFromRow(row);
            if (marts == null)
                continue;
            boolean multiMart = (marts.size() > 1);
            for (Mart mart : marts) {
                List<Dataset> dsList = new ArrayList<Dataset>();
                String commonDsName = (String) this.tablePanel.getTable().getValueAt(row,
                        PartitionUtils.DSM_DATASETNAME);
                String dsName = null;
                if (multiMart) {
                    // add prefix
                    String fullName = mart.getSchemaPartitionTable().getValue(0, PartitionUtils.DATASETNAME);
                    int index = fullName.lastIndexOf("_");
                    String prefix = "";
                    if (index >= 0) {
                        prefix = fullName.substring(0, index) + "_";
                    }
                    dsName = prefix + commonDsName;
                } else
                    dsName = commonDsName;
                Dataset ds = mart.getDatasetByName(dsName);
                if (ds == null) {
                    // try to find dataset by suffix
                    ds = mart.getDatasetBySuffix(dsName);
                    if (ds == null)
                        continue;
                }
                if (ds.hideOnMaster())
                    continue;
                dsList.add(ds);
                if ((n == 0 || n == 2) && !dsList.isEmpty()) {
                    Object[] options = { "Yes", "Yes, Dont warn me anymore", "No", "No, Dont warn me anymore" };

                    n = JOptionPane.showOptionDialog(this,
                            "Do you want to update the options for mart " + mart.getName() + ". Proceed?", "Warning",
                            JOptionPane.DEFAULT_OPTION, JOptionPane.WARNING_MESSAGE, null, options, options[0]);
                }
                UpdateMart um = new UpdateMart(this, pd);
                um.updateDataset(dsList, n < 2
                        ? true : false);
            }
        }
    }

    @Override
    public void valueChanged(ListSelectionEvent event) {
        if (!event.getValueIsAdjusting()) {
            this.updateButton.setEnabled(event.getFirstIndex() >= 0);

        }
    }

}
