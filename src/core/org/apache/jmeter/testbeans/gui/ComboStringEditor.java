/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.jmeter.testbeans.gui;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyEditorSupport;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.text.JTextComponent;

import org.apache.jmeter.util.JMeterUtils;

/**
 * This class implements a property editor for possibly null String properties
 * that supports custom editing (i.e.: provides a GUI component) based on a
 * combo box.
 * <p>
 * The provided GUI is a combo box with:
 * <ul>
 * <li>An option for "undefined" (corresponding to the null value), unless the
 * <b>noUndefined</b> property is set.
 * <li>An option for each value in the <b>tags</b> property.
 * <li>The possibility to write your own value, unless the <b>noEdit</b>
 * property is set.
 * </ul>
 *
 */
class ComboStringEditor extends PropertyEditorSupport implements ItemListener {

    private static final String[] EMPTY_STRING_ARRAY = new String[0];

    /**
     * The list of options to be offered by this editor.
     */
    private final String[] tags;

    /**
     * The edited property's default value.
     */
    private String initialEditValue;

    private final JComboBox combo;

    private final DefaultComboBoxModel model;

    private boolean startingEdit = false;

    /*
     * True iif we're currently processing an event triggered by the user
     * selecting the "Edit" option. Used to prevent reverting the combo to
     * non-editable during processing of secondary events.
     */

    // TODO - do these behave properly during language change? Probably not.

    // Needs to be visible to test cases
    static final Object UNDEFINED = new UniqueObject(JMeterUtils.getResString("property_undefined")); //$NON-NLS-1$

    private static final Object EDIT = new UniqueObject(JMeterUtils.getResString("property_edit")); //$NON-NLS-1$

    @Deprecated // only for use from test code
    ComboStringEditor() {
        this(null, false, false);
    }

    ComboStringEditor(String []tags, boolean noEdit, boolean noUndefined) {
        // Create the combo box we will use to edit this property:

        this.tags = tags == null ? EMPTY_STRING_ARRAY : tags;

        model = new DefaultComboBoxModel();

        if (!noUndefined) {
            model.addElement(UNDEFINED);
        }
        for (String tag : this.tags) {
            model.addElement(tag);
        }
        if (!noEdit) {
            model.addElement(EDIT);
        }

        combo = new JComboBox(model);
        combo.addItemListener(this);
        combo.setEditable(false);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean supportsCustomEditor() {
        return true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Component getCustomEditor() {
        return combo;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object getValue() {
        return getAsText();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getAsText() {
        Object value = combo.getSelectedItem();

        if (value == UNDEFINED) {
            return null;
        }
        return (String) value; // TODO I10N
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setValue(Object value) {
        setAsText((String) value);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void setAsText(String value) {
        combo.setEditable(true);

        if (value == null) {
            combo.setSelectedItem(UNDEFINED);
        } else {
            combo.setSelectedItem(value); // TODO I10N
        }

        if (!startingEdit && combo.getSelectedIndex() >= 0) {
            combo.setEditable(false);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void itemStateChanged(ItemEvent e) {
        if (e.getStateChange() == ItemEvent.SELECTED) {
            if (e.getItem() == EDIT) {
                startingEdit = true;
                startEditing();
                startingEdit = false;
            } else {
                if (!startingEdit && combo.getSelectedIndex() >= 0) {
                    combo.setEditable(false);
                }

                firePropertyChange();
            }
        }
    }

    private void startEditing() {
        JTextComponent textField = (JTextComponent) combo.getEditor().getEditorComponent();

        combo.setEditable(true);

        textField.requestFocus();

        String text = initialEditValue; // TODO I10N
        if (initialEditValue == null) {
            text = ""; // will revert to last valid value if invalid
        }

        combo.setSelectedItem(text);

        int i = text.indexOf("${}");
        if (i != -1) {
            textField.setCaretPosition(i + 2);
        } else {
            textField.selectAll();
        }
    }

    public String getInitialEditValue() {
        return initialEditValue;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String[] getTags() {
        return tags;
    }

    /**
     * @param object
     */
    public void setInitialEditValue(String object) {
        initialEditValue = object;
    }

    /**
     * This is a funny hack: if you use a plain String, entering the text of the
     * string in the editor will make the combo revert to that option -- which
     * actually amounts to making that string 'reserved'. I preferred to avoid
     * this by using a different type having a controlled .toString().
     */
    private static class UniqueObject {
        private final String s;

        UniqueObject(String s) {
            this.s = s;
        }

        @Override
        public String toString() {
            return s;
        }
    }
}