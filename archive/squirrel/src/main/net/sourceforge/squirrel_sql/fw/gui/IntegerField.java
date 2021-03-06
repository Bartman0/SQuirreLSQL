package net.sourceforge.squirrel_sql.fw.gui;
/*
 * Copyright (C) 2001 Colin Bell
 * colbell@users.sourceforge.net
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
import java.awt.Toolkit;

import javax.swing.JTextField;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

/**
 * This class is a <CODE>TextField</CODE> that only allows integral
 * values to be entered into it.
 *
 * @author  <A HREF="mailto:colbell@users.sourceforge.net">Colin Bell</A>
 */
public class IntegerField extends JTextField {
    /**
     * Default ctor.
     */
    public IntegerField() {
        super();
    }

    /**
     * Ctor specifying the field width.
     */
    public IntegerField(int cols) {
        super(cols);
    }

    public int getInt() {
        final String text = getText();
        if (text == null || text.length() == 0) {
            return 0;
        }
        return Integer.parseInt(text);
    }

    public void setInt(int value) {
        setText("" + value);
    }

    protected Document createDefaultModel() {
        return new IntegerDocument();
    }

    static class IntegerDocument extends PlainDocument {

        public void insertString(int offs, String str, AttributeSet a)
                throws BadLocationException {
            if (str != null) {
                try {
                    Integer.decode(str);
                    super.insertString(offs, str, a);
                } catch (NumberFormatException ex) {
                    Toolkit.getDefaultToolkit().beep();
                }
            }
        }
    }
}
