/*
 * Copyright (c) 2005, 2021, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.  Oracle designates this
 * particular file as subject to the "Classpath" exception as provided
 * by Oracle in the LICENSE file that accompanied this code.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */
//package sun.swing.table;
package com.raelity.jdk.sun.swing.table;

//import sun.swing.DefaultLookup;

import java.awt.Component;
import java.awt.Color;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.Serializable;
import java.util.function.BiFunction;

import javax.swing.*;
import javax.swing.plaf.UIResource;
import javax.swing.border.Border;
import javax.swing.table.*;

import com.raelity.jdk.sun.swing.icon.SortArrowIcon;

/**
 * Derived from jdk-21/java.desktop/sun/swing/table/DefaultTableCellHeaderRenderer 
 */
@SuppressWarnings("serial") // JDK-implementation class
public class TableSortHeaderRenderer extends DefaultTableCellRenderer
        implements UIResource {
    private boolean horizontalTextPositionSet;
    private Icon sortArrow;
    private EmptyIcon emptyIcon = new EmptyIcon();
    private BiFunction<Integer, Integer, String> toolTipGenerator
            = (sortIndex, column) -> (sortIndex == null ? null
                                      : ((sortIndex == 0 ? "primary"
                                      : sortIndex == 1 ? "secondary"
                                      : "tertiary") + " sort key"));
    
    static {
        // Establish developer default values, no change if key already exists.
        DefaultLookup.put("Table.ascendingSortIcon-1",
                          new SortArrowIcon(true, Color.green));
        DefaultLookup.put("Table.ascendingSortIcon-2",
                          new SortArrowIcon(true, Color.orange));
        DefaultLookup.put("Table.descendingSortIcon-1",
                          new SortArrowIcon(false, Color.green));
        DefaultLookup.put("Table.descendingSortIcon-2",
                          new SortArrowIcon(false, Color.orange));
        
        Icon natural = UIManager.getIcon("Table.naturalSortIcon");
        DefaultLookup.put("Table.naturalSortIcon-1", natural);
        DefaultLookup.put("Table.naturalSortIcon-2", natural);
        
        //DefaultLookup.put("TableHeader.rightAlignSortArrow", true);
    }

    /**
     * Creates a table cell renderer handling multi key sort.
     */
    public TableSortHeaderRenderer() {
        setHorizontalAlignment(JLabel.CENTER);
    }
    
    /**
     * Set the function used to generate a column header's toolTip. The function's
     * parameters are index and column respectively;
     * index is null if the column is not part of sort.
     * Note that even if index is null, column is valid.
     * The default function returns null unless the column is part of a sort;
     * when the column is part of the sort, somehing like
     * "secondary sort key" is returned; the default does not use the column
     * parameter.
     * <p>
     * If the column parameter is used by the function, the method
     * {@link JTable#convertColumnIndexToModel(int)} may be useful.
     * @param toolTipGenerator
     */
    public void setToolTipGenerator(BiFunction<Integer, Integer, String> toolTipGenerator) {
        this.toolTipGenerator = toolTipGenerator;
    }
    
    /** {@inheritDoc} */
    @Override
    public void setHorizontalTextPosition(int textPosition) {
        horizontalTextPositionSet = true;
        super.setHorizontalTextPosition(textPosition);
    }

    private record ColumnSortOrder(SortOrder sortOrder, Integer keyIndex) {};

    /** {@inheritDoc} */
    @Override
    public Component getTableCellRendererComponent(JTable table, Object value,
                boolean isSelected, boolean hasFocus, int row, int column)
    {
        Icon sortIcon = null;
        
        boolean isPaintingForPrint = false;
        
        Integer keyIndex = null;
        if (table != null) {
            JTableHeader header = table.getTableHeader();
            if (header != null) {
                Color fgColor = null;
                Color bgColor = null;
                if (hasFocus) {
                    fgColor = DefaultLookup.getColor(this, ui, "TableHeader.focusCellForeground");
                    bgColor = DefaultLookup.getColor(this, ui, "TableHeader.focusCellBackground");
                }
                if (fgColor == null) {
                    fgColor = header.getForeground();
                }
                if (bgColor == null) {
                    bgColor = header.getBackground();
                }
                setForeground(fgColor);
                setBackground(bgColor);
                
                setFont(header.getFont());
                
                isPaintingForPrint = header.isPaintingForPrint();
            }
            
            if (!isPaintingForPrint && table.getRowSorter() != null) {
                if (!horizontalTextPositionSet) {
                    // There is a row sorter, and the developer hasn't
                    // set a text position, change to leading.
                    setHorizontalTextPosition(JLabel.LEADING);
                }
                ColumnSortOrder result = getColumnSortOrder(table, column);
                SortOrder sortOrder = result.sortOrder();
                keyIndex = result.keyIndex();
                if (sortOrder != null) {
                    String iconLookupKey = switch(sortOrder) {
                    case ASCENDING -> "Table.ascendingSortIcon";
                    case DESCENDING -> "Table.descendingSortIcon";
                    case UNSORTED -> "Table.naturalSortIcon";
                    };
                    if (keyIndex != 0)
                        iconLookupKey += "-" + keyIndex;
                    sortIcon = DefaultLookup.getIcon(this, ui, iconLookupKey);
                }
            }
        }
        
        setToolTipText(toolTipGenerator.apply(keyIndex, column));
        setText(value == null ? "" : value.toString());
        setIcon(sortIcon);
        sortArrow = sortIcon;
        
        Border border = null;
        if (hasFocus) {
            border = DefaultLookup.getBorder(this, ui, "TableHeader.focusCellBorder");
        }
        if (border == null) {
            border = DefaultLookup.getBorder(this, ui, "TableHeader.cellBorder");
        }
        setBorder(border);
        
        return this;
    }

    /** return array { sortOrder, keyIndex } or null if none. */
    private static ColumnSortOrder getColumnSortOrder(JTable table, int column) {
        ColumnSortOrder rv = new ColumnSortOrder(null, null);
        if (table == null || table.getRowSorter() == null) {
            return rv;
        }
        java.util.List<? extends RowSorter.SortKey> sortKeys =
                table.getRowSorter().getSortKeys();
        for (int i = 0; i < sortKeys.size(); i++) {
            RowSorter.SortKey sortKey = sortKeys.get(i);
            if (i == 0 && sortKey.getSortOrder() == SortOrder.UNSORTED)
                return rv; // If first key is unsorted, then there's no sort.
            if (sortKey.getColumn() == table.convertColumnIndexToModel(column))
                return new ColumnSortOrder(sortKey.getSortOrder(), i);
        }
        return rv;
    }

    @Override
    public void paintComponent(Graphics g) {
        boolean b = DefaultLookup.getBoolean(this, ui,
                "TableHeader.rightAlignSortArrow", false);
        if (b && sortArrow != null) {
            //emptyIcon is used so that if the text in the header is right
            //aligned, or if the column is too narrow, then the text will
            //be sized appropriately to make room for the icon that is about
            //to be painted manually here.
            emptyIcon.width = sortArrow.getIconWidth();
            emptyIcon.height = sortArrow.getIconHeight();
            setIcon(emptyIcon);
            super.paintComponent(g);
            Point position = computeIconPosition(g);
            sortArrow.paintIcon(this, g, position.x, position.y);
        } else {
            super.paintComponent(g);
        }
    }

    private Point computeIconPosition(Graphics g) {
        FontMetrics fontMetrics = g.getFontMetrics();
        Rectangle viewR = new Rectangle();
        Rectangle textR = new Rectangle();
        Rectangle iconR = new Rectangle();
        Insets i = getInsets();
        viewR.x = i.left;
        viewR.y = i.top;
        viewR.width = getWidth() - (i.left + i.right);
        viewR.height = getHeight() - (i.top + i.bottom);
        SwingUtilities.layoutCompoundLabel(
            this,
            fontMetrics,
            getText(),
            sortArrow,
            getVerticalAlignment(),
            getHorizontalAlignment(),
            getVerticalTextPosition(),
            getHorizontalTextPosition(),
            viewR,
            iconR,
            textR,
            getIconTextGap());
        int x = getWidth() - i.right - sortArrow.getIconWidth();
        int y = iconR.y;
        return new Point(x, y);
    }

    @SuppressWarnings("serial") // JDK-implementation class
    private static class EmptyIcon implements Icon, Serializable {
        int width = 0;
        int height = 0;
        @Override public void paintIcon(Component c, Graphics g, int x, int y) {}
        @Override public int getIconWidth() { return width; }
        @Override public int getIconHeight() { return height; }
    }

    /** Using this avoids some source code modifications source code */
    private static class DefaultLookup {
        private DefaultLookup() { }
        
        static Color getColor(JComponent c, Object ui, String key)
        {
            Color color = UIManager.getColor(key);
            return color;
        }
        static Icon getIcon(JComponent c, Object ui, String key)
        {
            Icon icon = UIManager.getIcon(key);
            return icon;
        }
        static Border getBorder(JComponent c, Object ui, String key)
        {
            Border border = UIManager.getBorder(key);
            return border;
        }
        static boolean getBoolean(JComponent c, Object ui, String key, boolean defaultValue)
        {
            boolean flag = UIManager.getBoolean(key);
            return flag;
        }

        // Add something to the developer defaults, unless key is already set.
        static Object put(Object key, Object value)
        {
            if (UIManager.get(key) == null)
                UIManager.put(key, value);
            return UIManager.get(key);
        }
    }
}
