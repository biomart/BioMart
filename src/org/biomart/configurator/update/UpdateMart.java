package org.biomart.configurator.update;

import java.util.List;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.SwingWorker;
import org.biomart.common.exceptions.MartBuilderException;
import org.biomart.common.resources.Resources;
import org.biomart.common.view.gui.dialogs.ProgressDialog;
import org.biomart.common.view.gui.dialogs.StackTrace;
import org.biomart.configurator.model.object.DataLinkInfo;
import org.biomart.configurator.utils.type.DataLinkType;
import org.biomart.objects.objects.Dataset;
import org.biomart.objects.objects.Mart;

public class UpdateMart {

    private ProgressDialog progressMonitor;
    private UpdateMartModel updateModel;

    public UpdateMartModel getUpdateModel() {
        return updateModel;
    }

    /*
     * the dataset may from db (0.7/0.8), url (0.7/0.8), need to handle all the cases
     */
    public UpdateMart(JDialog parent, ProgressDialog pd) {
        if (pd == null) {
            this.progressMonitor = ProgressDialog.getInstance(parent);
        } else {
            this.progressMonitor = pd;
        }
        this.updateModel = new UpdateMartModel();
    }

    public boolean updateDataset(List<Dataset> dslist, boolean updateOption) throws MartBuilderException {
        for (Dataset ds : dslist) {
            this.progressMonitor.setStatus("updating dataset: " + ds.getDisplayName());
            this.updateModel.updateDataset(ds, updateOption);
        }
        Mart mart = dslist.get(0).getParentMart();
        if (updateOption)
            this.updateModel.updateOptions(mart, dslist);
        return true;
    }

    public void updateMartWithProgressBar(final Mart mart) {

        final SwingWorker<Void, Void> worker = new SwingWorker<Void, Void>() {

            @Override
            protected Void doInBackground() throws Exception {
                try {
                    progressMonitor.setStatus("updating mart ...");
                    updateModel.updateMart(mart);
                    // update options
                    int n = JOptionPane.showConfirmDialog(null, "update options in mart " + mart.getName() + " ?",
                            "Question", JOptionPane.YES_NO_OPTION);

                    if (n == 0) {
                        progressMonitor.setStatus("updating options from mart " + mart.getName());
                        updateModel.updateOptions(mart, mart.getDatasetList());
                    }
                    // add naive attribute/filter for orphan column?
                    boolean checkOrphanColumns = false;
                    for (Dataset ds : mart.getDatasetList()) {
                        // check if it is hidden for the master
                        DataLinkInfo dlink = ds.getDataLinkInfoNonFlip();
                        if (dlink.getDataLinkType() == DataLinkType.SOURCE
                                || dlink.getDataLinkType() == DataLinkType.TARGET) {
                            if (ds.hideOnConfig(mart.getMasterConfig()))
                                continue;
                            checkOrphanColumns = true;
                            break;
                        }
                    }
                    /*
                     * if(checkOrphanColumns) { progressMonitor.setStatus("checking orphan column");
                     * MartController.getInstance().createNaiveForOrphanColumn(mart); }
                     */
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

            @Override
            protected void done() {
                progressMonitor.setVisible(false);
            }
        };

        worker.execute();
        progressMonitor.start(Resources.get("DEFAULTPRECESSMESSAGE"));

    }

}
