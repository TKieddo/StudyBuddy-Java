import java.awt.*;

/*
    Like FlowLayout, but wraps to the next row and keeps each card
    at its preferred size (does not stretch to fill the window).
*/
class WrapLayout extends FlowLayout {
    public WrapLayout(int align, int hgap, int vgap) {
        super(align, hgap, vgap);
    }

    public Dimension preferredLayoutSize(Container target) {
        return layoutSize(target, true);
    }

    public Dimension minimumLayoutSize(Container target) {
        return layoutSize(target, false);
    }

    private Dimension layoutSize(Container target, boolean preferred) {
        synchronized (target.getTreeLock()) {
            int width = target.getWidth();
            if (width == 0) {
                width = Integer.MAX_VALUE;
            }
            Insets insets = target.getInsets();
            int maxWidth = width - insets.left - insets.right - getHgap() * 2;
            int hgap = getHgap();
            int vgap = getVgap();
            Dimension dim = new Dimension(0, 0);
            int rowWidth = 0;
            int rowHeight = 0;
            int count = target.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = preferred ? m.getPreferredSize() : m.getMinimumSize();
                if (rowWidth + d.width > maxWidth && rowWidth > 0) {
                    dim.width = Math.max(dim.width, rowWidth);
                    dim.height = dim.height + rowHeight + vgap;
                    rowWidth = 0;
                    rowHeight = 0;
                }
                if (rowWidth > 0) {
                    rowWidth = rowWidth + hgap;
                }
                rowWidth = rowWidth + d.width;
                rowHeight = Math.max(rowHeight, d.height);
            }
            dim.width = Math.max(dim.width, rowWidth);
            dim.height = dim.height + rowHeight;
            dim.width = dim.width + insets.left + insets.right + hgap * 2;
            dim.height = dim.height + insets.top + insets.bottom + vgap * 2;
            return dim;
        }
    }

    public void layoutContainer(Container target) {
        synchronized (target.getTreeLock()) {
            Insets insets = target.getInsets();
            int maxWidth = target.getWidth() - insets.left - insets.right - getHgap() * 2;
            int x = insets.left + getHgap();
            int y = insets.top + getVgap();
            int rowHeight = 0;
            int hgap = getHgap();
            int vgap = getVgap();
            int count = target.getComponentCount();
            for (int i = 0; i < count; i++) {
                Component m = target.getComponent(i);
                if (!m.isVisible()) {
                    continue;
                }
                Dimension d = m.getPreferredSize();
                m.setSize(d.width, d.height);
                if (x + d.width > maxWidth + insets.left && x > insets.left + hgap) {
                    x = insets.left + hgap;
                    y = y + rowHeight + vgap;
                    rowHeight = 0;
                }
                m.setLocation(x, y);
                x = x + d.width + hgap;
                rowHeight = Math.max(rowHeight, d.height);
            }
        }
    }
}
