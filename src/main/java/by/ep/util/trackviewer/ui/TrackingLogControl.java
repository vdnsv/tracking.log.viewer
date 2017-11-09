package by.ep.util.trackviewer.ui;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.MessageBox;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.swt.widgets.Widget;

import by.ep.util.trackviewer.data.DumpItem;
import by.ep.util.trackviewer.data.TrackItem;
import by.ep.util.trackviewer.data.TrackItemFieldsProvider;
import by.ep.util.trackviewer.filter.Expression;
import by.ep.util.trackviewer.parser.TrackingLogLoader;

class TrackingLogControl extends Composite {

    private static final String PROPERTIES_FILE_NAME = "tracking.log.viewer.properties";
    private static final String DIR_PARAM = "directory";
    private static final String FILTER_PARAM = "filter";

    TrackingLogControl(Composite parent, FilesSelectControl filesSelectControl,
            UnboundSamplesControl unboundSamplesControl) {

        super(parent, SWT.NONE);
        this.setLayout(new GridLayout(1, false));
        FilterControl filterControl = new FilterControl(this);
        filterControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        PaginationControl paginationControl = new PaginationControl(this);
        paginationControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        SashForm sashForm = new SashForm(this, SWT.VERTICAL);
        sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        Tree tree = new Tree(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tree.setHeaderVisible(true);

        int[] columnsWidth = {400, 150, 100, 100, 80, 120, 200, 50, 250};
        String[] columnsTitle = {
                "name",
                "time",
                "duration",
                "sqlTime",
                "sqlCount",
                "typ",
                "thread",
                "id",
                "params"
        };

        for (int i = 0; i < columnsWidth.length; i++) {
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            column.setText(columnsTitle[i]);
            column.setWidth(columnsWidth[i]);
        }

        Text otherInfoText = new Text(sashForm, SWT.BORDER | SWT.V_SCROLL | SWT.WRAP);
        sashForm.setWeights(new int[]{3, 1});

        final TrackData trackData = new TrackData(tree, null, paginationControl, unboundSamplesControl, null);
        trackData.paginationControl = paginationControl;
        tree.setData(trackData);

        tree.addSelectionListener(new SelectionListener() {
            @Override
            public void widgetSelected(SelectionEvent e) {

                onItemSelected(e.item, otherInfoText, trackData);
            }

            @Override
            public void widgetDefaultSelected(SelectionEvent e) {

                onItemSelected(e.item, otherInfoText, trackData);
            }
        });

        filesSelectControl.setScanButtonClickFunc((String dirName) -> {
            otherInfoText.setText("");
            try {
                scanDir(dirName, trackData);
                saveParameters(DIR_PARAM, dirName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        });

        filterControl.setFilterButtonClickFunc((Expression expression, Boolean isDeep) -> {
            trackData.filterExpression = expression;
            trackData.isDeep = isDeep.booleanValue();
            if (trackData.logLoader != null) {
                generateTrackingLogTree(trackData);
            }
            saveParameters(FILTER_PARAM, filterControl.getFilterText());
        });

        paginationControl.setPageNumberChangedFunction(
                (PaginationControl pagesControl) -> generateTrackingLogTree(trackData));

        readParameters(filesSelectControl, filterControl);
    }

    private static void readParameters(FilesSelectControl filesSelectControl, FilterControl filterControl) {

        File f = new File(PROPERTIES_FILE_NAME);
        if (f.exists()) {
            try {
                try (FileInputStream fis = new FileInputStream(f)) {
                    Properties p = new Properties();
                    p.load(fis);
                    filesSelectControl.setDirectory(p.getProperty(DIR_PARAM, ""));
                    filterControl.setFilterText(p.getProperty(FILTER_PARAM, ""));
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private static void saveParameters(final String parameterName, final String parameterValue) {

        try {
            Properties p = new Properties();
            File f = new File(PROPERTIES_FILE_NAME);
            if (f.exists()) {
                try (BufferedInputStream bis = new BufferedInputStream(new FileInputStream(f))) {
                    p.load(bis);
                }
            }
            p.setProperty(parameterName, parameterValue);
            try (FileOutputStream fos = new FileOutputStream(f)) {
                p.store(fos, "");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void onItemSelected(Widget item, Text otherInfoText, TrackData trackData) {

        if (item instanceof TreeItem) {
            TreeItem treeItem = (TreeItem) item;
            if (treeItem.getData() instanceof DumpItem) {
                DumpItem dumpItem = (DumpItem) treeItem.getData();
                otherInfoText.setText(String.join("\n", dumpItem.stackTrace));
                if (treeItem.getItemCount() == 0 && !dumpItem.stackTrace.isEmpty()) {
                    treeItem.getDisplay().asyncExec(new Runnable() {
                        @Override
                        public void run() {

                            for (String dump : dumpItem.stackTrace) {
                                TreeItem dumpChild = new TreeItem(treeItem, SWT.NONE);
                                dumpChild.setText(dump);
                                if (dump.startsWith("\tcom.") && !dump.startsWith("\tcom.sun.")) {
                                    dumpChild.setFont(trackData.boldFont);
                                }
                            }
                        }
                    });
                }
            } else {
                String text = treeItem.getText(8);
                if (treeItem.getData() instanceof TrackItem) {
                    text += "\n\n" + ((TrackItem) treeItem.getData()).toString();
                }
                otherInfoText.setText(text);
            }
        }
    }


    private static void scanDir(final String dirName, TrackData trackData)
            throws IOException {

        trackData.logLoader = new TrackingLogLoader(dirName);
        trackData.logLoader.scan();
        trackData.tree.removeAll();
        trackData.paginationControl.setCurrentPage(1);
        generateTrackingLogTree(trackData);
        trackData.unboundSamplesControl.generateUnboundSamplesTree(trackData.logLoader.getUnboundSamples(), trackData.boldFont);
    }

    private static void setTreeItemText(TrackItem trackItem, TreeItem treeItem) {

        treeItem.setData(trackItem);
        treeItem.setText(new String[]{trackItem.name,
                trackItem.time,
                Integer.toString(trackItem.processTime),
                Integer.toString(trackItem.sqlTime),
                Integer.toString(trackItem.sqlCount),
                trackItem.typ,
                trackItem.thread,
                trackItem.id,
                trackItem.other
        });
    }

    private static void addTreeChildItems(List<TrackItem> children, TreeItem parentTreeItem, Font boldFont) {

        for (TrackItem trackItem : children) {
            TreeItem treeItem = new TreeItem(parentTreeItem, SWT.NONE);

            setTreeItemText(trackItem, treeItem);
            if (trackItem.children != null && !trackItem.children.isEmpty()) {
                addTreeChildItems(trackItem.children, treeItem, boldFont);
            }

            addDump(boldFont, trackItem, treeItem);
        }
    }

    private static void addDump(final Font boldFont, final TrackItem trackItem, final TreeItem treeItem) {

        if (trackItem.samplingItems != null && !trackItem.samplingItems.isEmpty()) {
            final TreeItem samplingInfoTreeItem = new TreeItem(treeItem, SWT.NONE);
            samplingInfoTreeItem.setText(
                    new String[]{"Sampling Info", "", "", "", "", "", "", "", ""});

            treeItem.getDisplay().asyncExec(() -> {

                        for (DumpItem dumpItem : trackItem.samplingItems) {
                            TreeItem dumpTreeItem = new TreeItem(samplingInfoTreeItem, SWT.NONE);
                            dumpTreeItem.setText(
                                    new String[]{"Dump", dumpItem.time, "", "", "", dumpItem.state, dumpItem.thread,
                                            dumpItem.id,
                                            dumpItem.isNative ? "native" : "not native"});
                            dumpTreeItem.setData(dumpItem);
                        }
                    }
            );
        }
    }

    private static List<TrackItem> filterItems(TrackData trackData) {

        try {
            TrackItemFieldsProvider trackItemFieldsProvider = new TrackItemFieldsProvider();

            List<TrackItem> filteredAndSortedItems = trackData.logLoader.getRootItems().stream()

                    .filter((TrackItem o) -> o.name != null
                            || (o.thread != null)
                            || (o.children != null && !o.children.isEmpty())
                            || (o.samplingItems != null && !o.samplingItems.isEmpty()))

                    .filter((TrackItem trackItem) -> filterTrackItem(trackItem, trackItemFieldsProvider,
                            trackData.filterExpression, trackData.isDeep))

                    .sorted((TrackItem o1, TrackItem o2) -> nvl(nvl(o1.startTime, o1.time), "")
                            .compareTo(nvl(o2.startTime, o2.time)))
                    .collect(
                            Collectors.toList());
            trackData.paginationControl.setRecordsCount(filteredAndSortedItems.size());

            int fromIndex = (trackData.paginationControl.getCurrentPage() - 1) * PaginationControl.ITEMS_PER_PAGE;
            int toIndex = fromIndex + PaginationControl.ITEMS_PER_PAGE;
            if (toIndex >= filteredAndSortedItems.size()) {
                toIndex = filteredAndSortedItems.size();
            }
            return filteredAndSortedItems.subList(fromIndex, toIndex);
        } catch (Exception ex) {
            MessageBox messageBox = new MessageBox(trackData.tree.getShell(), SWT.ICON_ERROR | SWT.OK);
            messageBox.setText("Error");
            messageBox.setMessage("Invalid filter expression! " + ex.getClass() + (ex.getMessage() == null ? "" :
                    "\n" + ex.getMessage()));
            messageBox.open();
            return new ArrayList<>();
        }
    }

    private static String nvl(final String value1, final String value2) {

        return (value1 == null) ? value2 : value1;
    }

    private static boolean filterTrackItem(TrackItem trackItem, TrackItemFieldsProvider trackItemFieldsProvider,
            Expression filterExpression, boolean isDeep) {

        if (filterExpression == null) {
            return true;
        }

        if ((Boolean) filterExpression.execute(trackItem, trackItemFieldsProvider)) {
            return true;
        }

        if (!isDeep) {
            return false;
        }
        if (trackItem.children != null && !trackItem.children.isEmpty()) {
            for (TrackItem childTrackItem : trackItem.children) {
                if (filterTrackItem(childTrackItem, trackItemFieldsProvider, filterExpression, isDeep)) {
                    return true;
                }
            }
        }

        return false;
    }

    private static void generateTrackingLogTree(TrackData trackData) {

        trackData.tree.removeAll();
        List<TrackItem> rootItems = filterItems(trackData);

        for (TrackItem rootTrackItem : rootItems) {
            TreeItem treeItem = new TreeItem(trackData.tree, SWT.NONE);
            setTreeItemText(rootTrackItem, treeItem);
            if (rootTrackItem.children != null && !rootTrackItem.children.isEmpty()) {
                addTreeChildItems(rootTrackItem.children, treeItem, trackData.boldFont);
            }
            addDump(trackData.boldFont, rootTrackItem, treeItem);
        }
    }

    static class TrackData {

        Tree tree;
        TrackingLogLoader logLoader;
        PaginationControl paginationControl;
        UnboundSamplesControl unboundSamplesControl;
        Expression filterExpression;
        boolean isDeep;
        Font boldFont;

        TrackData(Tree tree, TrackingLogLoader logLoader, PaginationControl paginationControl,
                UnboundSamplesControl unboundSamplesControl, Expression filterExpression) {

            this.tree = tree;
            this.logLoader = logLoader;
            this.paginationControl = paginationControl;
            this.unboundSamplesControl = unboundSamplesControl;
            this.filterExpression = filterExpression;

            FontData boldFontData = tree.getFont().getFontData()[0];
            boldFontData.setStyle(boldFontData.getStyle() | SWT.BOLD);

            boldFont = new Font(tree.getDisplay(), boldFontData);
        }
    }

}