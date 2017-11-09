package by.ep.util.trackviewer.ui;

import java.util.List;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;

import by.ep.util.trackviewer.data.DumpItem;

public class UnboundSamplesControl extends Composite {

    private final PaginationControl paginationControl;
    private final Tree tree;

    private volatile List<DumpItem> unboundSamples;
    private Font boldFont;

    UnboundSamplesControl(Composite parent) {

        super(parent, SWT.NONE);
        this.setLayout(new GridLayout(1, false));

        paginationControl = new PaginationControl(this);
        paginationControl.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));

        tree = new Tree(this, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.FULL_SELECTION);
        tree.setHeaderVisible(true);
        tree.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 1));

        int[] columnsWidth = {600, 100, 200, 200};
        String[] columnsTitle = {
                "time",
                "id",
                "state",
                "thread",
                "native"
        };

        for (int i = 0; i < columnsWidth.length; i++) {
            TreeColumn column = new TreeColumn(tree, SWT.LEFT);
            column.setText(columnsTitle[i]);
            column.setWidth(columnsWidth[i]);
        }

        paginationControl.setPageNumberChangedFunction(paginationControl -> {
            generateTreePage(paginationControl.getCurrentPage(), boldFont);
        });


    }

    private void generateTreePage(int currentPage, Font boldFont) {

        tree.removeAll();
        if (unboundSamples != null) {

            int fromIndex = (currentPage - 1) * PaginationControl.ITEMS_PER_PAGE;
            int toIndex = fromIndex + PaginationControl.ITEMS_PER_PAGE;
            if (toIndex >= unboundSamples.size()) {
                toIndex = unboundSamples.size();
            }
            final int toIndexFinal = toIndex;

            final List<DumpItem> unboundSamplesCopy = unboundSamples;
            final List<DumpItem> unboundSamplesFiltered = unboundSamples.subList(fromIndex, toIndexFinal);

            this.getDisplay().asyncExec(new Runnable() {
                @Override
                public void run() {

                    for (DumpItem dumpItem : unboundSamplesFiltered) {
                        if (unboundSamplesCopy != unboundSamples) {
                            break;
                        }
                        TreeItem dumpTreeItem = new TreeItem(tree, SWT.NONE);
                        dumpTreeItem.setText(
                                new String[]{dumpItem.time, dumpItem.id, dumpItem.state, dumpItem.thread,
                                        dumpItem.isNative ? "native" : "not native"});
                        dumpTreeItem.setData(dumpItem);
                        for (String dump : dumpItem.stackTrace) {
                            TreeItem dumpChild = new TreeItem(dumpTreeItem, SWT.NONE);
                            dumpChild.setText(dump);
                            if (dump.startsWith("\tcom.") && !dump.startsWith("\tcom.sun.")) {
                                dumpChild.setFont(boldFont);
                            }
                        }
                    }
                }
            });
        }

    }

    public Tree getTree() {

        return tree;
    }

    void generateUnboundSamplesTree(List<DumpItem> unboundSamples, Font boldFont) {

        this.unboundSamples = unboundSamples;
        this.boldFont = boldFont;
        paginationControl.setRecordsCount(unboundSamples.size());
        paginationControl.setCurrentPage(1);
        generateTreePage(1, boldFont);
    }

}
