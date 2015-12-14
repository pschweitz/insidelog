/*
 * Copyright 2015 Philippe Schweitzer.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.dbiservices.monitoring.tail.textconsole;

/**
 *
 * @author Philippe Schweitzer
 * @version 1.1
 * @since 16.11.2015
 */
import com.dbiservices.monitoring.tail.InformationObject;
import com.dbiservices.monitoring.tail.PatternColorConfiguration;
import com.dbiservices.tools.Logger;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Rectangle;
import java.awt.Shape;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.geom.AffineTransform;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.StringTokenizer;
import javafx.embed.swing.SwingNode;
import javafx.scene.layout.StackPane;
import javax.swing.JComponent;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.Painter;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.text.AbstractDocument;
import javax.swing.text.BadLocationException;
import javax.swing.text.BoxView;
import javax.swing.text.ComponentView;
import javax.swing.text.DefaultHighlighter;
import javax.swing.text.DefaultHighlighter.DefaultHighlightPainter;
import javax.swing.text.DefaultStyledDocument;
import javax.swing.text.Element;
import javax.swing.text.IconView;
import javax.swing.text.LabelView;
import javax.swing.text.ParagraphView;
import javax.swing.text.Position;
import javax.swing.text.Style;
import javax.swing.text.StyleConstants;
import javax.swing.text.StyleContext;
import javax.swing.text.StyledDocument;
import javax.swing.text.StyledEditorKit;
import javax.swing.text.View;
import javax.swing.text.ViewFactory;
import javax.swing.text.rtf.RTFEditorKit;

public class SwingConsole extends StackPane implements IOutputConsole {

    private static final Logger logger = Logger.getLogger(SwingConsole.class);

    private SwingNode swingNode = new SwingNode();

    private StyledDocument doc;

    private JTextPane textPane = new ExtendedJTextPane();

    private JScrollPane scrollPane = new JScrollPane(ScrollPaneConstants.VERTICAL_SCROLLBAR_AS_NEEDED, ScrollPaneConstants.HORIZONTAL_SCROLLBAR_AS_NEEDED);

    public static String lineSeparator = System.getProperty("line.separator");

    private boolean bufferOverflow = false;

    private int insertCount = 0;
    
    private InformationObject informationObject;

    public SwingConsole(InformationObject informationObject) {
        super();

        this.informationObject = informationObject;
        doc = new DefaultStyledDocument();
        textPane.setDocument(doc);

        textPane.setEditable(false);
        textPane.setBorder(null);

        Painter bgPainter = null;
        String laf = UIManager.getLookAndFeel().getName();
        if ("Nimbus".equals(laf)) {
            bgPainter = new Painter<JComponent>() {
                public void paint(Graphics2D g, JComponent c, int w, int h) {
                }
            };

            UIDefaults tdef = new UIDefaults();
            tdef.put("TextPane[Enabled].backgroundPainter", bgPainter);
            textPane.putClientProperty("Nimbus.Overrides", tdef);
        }

        logger.trace("BackgroundSwingColor: " + informationObject.getColorConfiguration().getBackgroundSwingColor());

        textPane.setBackground(informationObject.getColorConfiguration().getBackgroundSwingColor());

        logger.trace("SelectionSwingColor: " + informationObject.getColorConfiguration().getSelectionSwingColor());
        textPane.setSelectionColor(informationObject.getColorConfiguration().getSelectionSwingColor());

        scrollPane.setViewportView(textPane);

        scrollPane.setWheelScrollingEnabled(true);
        scrollPane.setAutoscrolls(true);
        scrollPane.setPreferredSize(new Dimension(150, 200));

        scrollPane.addComponentListener(new ComponentListener() {

            @Override
            public void componentResized(ComponentEvent e) {
                scrollPane.repaint();
            }

            @Override
            public void componentMoved(ComponentEvent e) {
            }

            @Override
            public void componentShown(ComponentEvent e) {
            }

            @Override
            public void componentHidden(ComponentEvent e) {
            }
        });

        zoomReset();

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                swingNode.setContent(scrollPane);
            }
        });

        getChildren().add(swingNode);
    }

    public void println(String message, Color color) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                doc = ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).getStyledDocument();

                try {

                    StyleContext sc = new StyleContext();
                    Style style = sc.addStyle(null, null);
                    StyleConstants.setForeground(style, color);
                    StyleConstants.setFontFamily(style, informationObject.getColorConfiguration().fontName);
                    StyleConstants.setFontSize(style, informationObject.getColorConfiguration().fontSize);

                    doc.insertString(doc.getLength(), message, style);

                } catch (BadLocationException ex) {
                    logger.error("Error inserting line", ex);
                }

                ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).setCaretPosition(doc.getLength());
            }
        });
    }

    public void println(String message) {
        println(message, Color.LIGHT_GRAY);
    }

    @Override
    public void appendText(String content, InformationObject informationObject) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                insertCount++;

                doc = ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).getStyledDocument();// .getDocument());

                javax.swing.undo.UndoManager undo = new javax.swing.undo.UndoManager();
                undo.setLimit(1);

                doc.addUndoableEditListener(undo);

                Element root = doc.getDefaultRootElement();
                int bufferSize = informationObject.getBufferSize();
                if (bufferSize <= 0) {
                    bufferSize = 100;
                }

                bufferSize = 1000 * bufferSize;
                int elementCount = informationObject.getElementCount();

                if (!bufferOverflow && root.getElementCount() + elementCount >= bufferSize) {
                    bufferOverflow = true;
                }

                if (bufferOverflow) {

                    int elementIndex = root.getElementCount() + elementCount - bufferSize;

                    try {
                        Element line = root.getElement(elementIndex);
                        int end = line.getEndOffset();
                        doc.remove(0, end);
                    } catch (BadLocationException ex) {
                    }
                }

                try {

                    Color color;
                    StyleContext sc = new StyleContext();
                    Style style = sc.addStyle(null, null);
                    StyleConstants.setFontFamily(style, informationObject.getColorConfiguration().fontName);
                    StyleConstants.setFontSize(style, informationObject.getColorConfiguration().fontSize);

                    if (informationObject.isDisplayColors()) {
                        StringTokenizer contentTokens = new StringTokenizer(content, System.lineSeparator());
                        while (contentTokens.hasMoreElements()) {
                            String token = contentTokens.nextToken();

                            ArrayList<PatternColorConfiguration> colorConfigurationList = informationObject.getColorConfiguration().getColorConfigurationList();

                            if (token != null && !token.equals("")) {
                                color = informationObject.getColorConfiguration().getDefaultSwingColor();

                                for (int i = 0; i < colorConfigurationList.size(); i++) {
                                    PatternColorConfiguration patternColorConfiguration = colorConfigurationList.get(i);
                                    if (patternColorConfiguration.caseSentitive) {
                                        if (token.contains(patternColorConfiguration.pattern)) {
                                            color = patternColorConfiguration.getSwingColor();

                                            break;
                                        }

                                    } else {
                                        if (token.toLowerCase().contains(patternColorConfiguration.pattern.toLowerCase())) {
                                            color = patternColorConfiguration.getSwingColor();

                                            break;
                                        }
                                    }
                                }

                                StyleConstants.setForeground(style, color);

                                int docLength = doc.getLength();
                                doc.insertString(docLength, token + System.lineSeparator(), style);
                                undo.discardAllEdits();
                                undo.die();
                            }
                        }
                    } else {
                        println(content);
                    }

                } catch (BadLocationException ex) {
                    logger.error("Error inserting line", ex);
                }

                ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).setCaretPosition(doc.getLength());
            }
        });                
    }

    @Override
    public void clear() {

        textPane.removeAll();
        scrollPane.removeAll();
        try {
            doc.remove(0, doc.getLength() - 1);
            System.gc();
        } catch (BadLocationException ex) {
            logger.error("Error clearing console line", ex);
        }
    }

    @Override
    public void insertLine(boolean displayColors) {

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {

                doc = ((DefaultStyledDocument) ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).getDocument());

                try {

                    Color color;
                    StyleContext sc = new StyleContext();
                    Style style = sc.addStyle(null, null);

                    String content = " ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////  " + System.lineSeparator() + System.lineSeparator();
                    if (displayColors) {
                        color = Color.MAGENTA;

                        StyleConstants.setForeground(style, color);
                        doc.insertString(doc.getLength(), content, style);

                    } else {
                        println(content);
                    }

                } catch (BadLocationException ex) {
                    logger.error("Error inserting line", ex);
                }

                ((JTextPane) ((JComponent) swingNode.getContent().getComponent(0)).getComponent(0)).setCaretPosition(doc.getLength());
            }
        });
    }

    @Override
    public void copyStyledContent() {

        Clipboard cb;
        RTFEditorKit rtfEditor = new RTFEditorKit();

        DataFlavor rtfFlavor = new DataFlavor("text/rtf", "Rich Text Format");
        DataFlavor flavors[] = {rtfFlavor};
        cb = Toolkit.getDefaultToolkit().getSystemClipboard();

        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        try {
            rtfEditor.write(out, doc, selectionStart, selectionEnd);
            logger.debug("Copy of styled content successful");

        } catch (IOException ex) {
            logger.error("Error copy styled content", ex);
        } catch (BadLocationException ex) {
            logger.error("Error copy styled content", ex);
        }
        String rtfString = out.toString();

        Object data[] = {new ByteArrayInputStream(rtfString.getBytes())};
        Transferable p = new SwingConsole.DataTransferClass(data, flavors);
        cb.setContents(p, null);
    }

    @Override
    public void saveStyledContent(String destinationFile) {

        try {

            RTFEditorKit rtfEditor = new RTFEditorKit();

            DataFlavor rtfFlavor = new DataFlavor("text/rtf", "Rich Text Format");
            DataFlavor flavors[] = {rtfFlavor};

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            FileOutputStream outF = null;

            try {
                outF = new FileOutputStream(destinationFile);
                rtfEditor.write(out, doc, 0, doc.getLength());
                out.close();

            } catch (IOException ex) {
                logger.error("Error saving styled content: " + destinationFile, ex);
            } catch (BadLocationException ex) {
                logger.error("Error saving styled content: " + destinationFile, ex);
            } finally {
                if (out != null) {
                    try {
                        out.close();
                    } catch (IOException ex) {

                    }
                }
            }

            String rtfString = out.toString();

            Color backgroudColor = informationObject.getColorConfiguration().getBackgroundSwingColor();

            int backgroudColorInt = backgroudColor.getRGB();

            int red = (backgroudColorInt >> 16) & 0xff;
            int green = (backgroudColorInt >> 8) & 0xff;
            int blue = (backgroudColorInt) & 0xff;

            backgroudColorInt = (blue * 65536) + (green * 256) + red;

            rtfString = rtfString.replace("{\\colortbl", "{\\*\\generator Msftedit 5.41.21.2510;}\\viewkind5\\uc1{\\*\\background{\\shp{\\*\\shpinst\n"
                    + "{\\sp{\\sn fillType}{\\sv 0}}\n"
                    + "{\\sp{\\sn fillColor}{\\sv " + backgroudColorInt + "}}\n"
                    + "{\\sp{\\sn fillBackColor}{\\sv 0}}\n"
                    + "{\\sp{\\sn fillFocus}{\\sv 0}}\n"
                    + "{\\sp{\\sn fillBlip}{\\sv {\\pict\\wmetafile0\\picscalex1\\picscaley1 \n"
                    + "}}}}}}\n"
                    + "{\\colortbl");

            outF.write(rtfString.getBytes());
            outF.close();
            logger.debug("Save of styled content successful: " + destinationFile);

        } catch (IOException ex) {
            logger.error("Error saving styled content: " + destinationFile, ex);
        }

    }

    @Override
    public void copyTextContent() {

        Clipboard cb;

        DataFlavor textFlavor = new DataFlavor("text/plain", "Text Format");
        DataFlavor flavors[] = {textFlavor};
        cb = Toolkit.getDefaultToolkit().getSystemClipboard();

        int selectionStart = textPane.getSelectionStart();
        int selectionEnd = textPane.getSelectionEnd();

        int textLength = doc.getLength();
        String content = "";
        try {

            int selectedLength = selectionEnd - selectionStart;
            if (selectedLength > 0) {
                content = doc.getText(selectionStart, selectedLength);
            } else {
                content = doc.getText(0, textLength);
            }
            logger.debug("Copy of text content successful");
        } catch (Exception e) {
            try {
                content = doc.getText(0, textLength);
            } catch (BadLocationException ex) {
                logger.error("Error copy text content", ex);
            }
        }

        Object data[] = {new ByteArrayInputStream(content.getBytes())};
        Transferable p = new SwingConsole.DataTransferClass(data, flavors);
        cb.setContents(p, null);
    }

    @Override
    public void saveTextContent(String destinationFile) {

        logger.debug("Save of text content successful: " + destinationFile);

        RTFEditorKit rtfEditor = new RTFEditorKit();

        DataFlavor rtfFlavor = new DataFlavor("text/plain", "Text Format");
        DataFlavor flavors[] = {rtfFlavor};
        FileOutputStream out = null;

        try {
            out = new FileOutputStream(destinationFile);
            out.write(doc.getText(0, doc.getLength()).getBytes());
            out.close();
            logger.debug("Save of text content successful: " + destinationFile);

        } catch (IOException ex) {
            logger.error("Error saving text content: " + destinationFile, ex);
        } catch (BadLocationException ex) {
            logger.error("Error saving text content: " + destinationFile, ex);
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    logger.error("Error saving text content: " + destinationFile, ex);
                }
            }
        }
    }

    @Override
 
    public void setInformationObject(InformationObject informationObject){
        this.informationObject = informationObject;
    }

    static class DataTransferClass implements Transferable {

        private Object datatrans[];
        private DataFlavor flavorstrans[];

        DataTransferClass(Object data[], DataFlavor flavors[]) {
            datatrans = data;
            flavorstrans = flavors;
        }

        public DataFlavor[] getTransferDataFlavors() {
            return flavorstrans;
        }

        public boolean isDataFlavorSupported(DataFlavor df) {
            if (df.getMimeType().contains("text/rtf") || df.getMimeType().contains("text/plain")) {
                return true;
            } else {
                return false;
            }
        }

        public Object getTransferData(DataFlavor flavor)
                throws UnsupportedFlavorException, IOException {
            if ((flavor.getMimeType().contains("text/rtf") || flavor.getMimeType().contains("text/plain"))) {
                return datatrans[0];
            } else {
                return null;
            }
        }
    }

    @Override
    public void search(String text, boolean caseSensitive) {

        logger.debug("Search: " + text);

        DefaultHighlighter highlighter = (DefaultHighlighter) textPane.getHighlighter();
        highlighter.removeAllHighlights();
        if (!text.equals("")) {
            DefaultHighlightPainter hPainter = new DefaultHighlightPainter(informationObject.getColorConfiguration().getSearchSwingColor());
            String contText = "";

            try {
                contText = doc.getText(0, doc.getLength());

                if (!caseSensitive) {
                    contText = contText.toLowerCase();
                    text = text.toLowerCase();
                }

            } catch (BadLocationException ex) {
                logger.error("Error searching content: " + text, ex);
            }

            int index = 0;

            while ((index = contText.indexOf(text, index)) > -1) {

                try {
                    highlighter.addHighlight(index, text.length() + index, hPainter);
                    index = index + text.length();
                } catch (BadLocationException ex) {
                    logger.error("Error searching content: " + text, ex);
                }
            }
        }

        textPane.repaint();
    }

    @Override
    public void zoomIn() {

        Double zoomFactor = getZoomFactor();

        textPane.getDocument().putProperty("ZOOM_FACTOR", zoomFactor * 1.1);
        logger.trace("zoom IN: factor:" + getZoomFactor());

        textPane.repaint();
        int newMaxV = (int) (scrollPane.getVerticalScrollBar().getMaximum() * 1.1);
        scrollPane.getVerticalScrollBar().setMaximum(newMaxV);

        scrollPane.repaint();
    }

    @Override
    public void zoomOut() {

        Double zoomFactor = getZoomFactor();

        textPane.getDocument().putProperty("ZOOM_FACTOR", zoomFactor * 1 / 1.1);

        int maxV = scrollPane.getHorizontalScrollBar().getMaximum();

        logger.trace("zoom OUT: factor:" + getZoomFactor());
        textPane.repaint();
        scrollPane.getHorizontalScrollBar().setMaximum((int) (maxV * 1 / 1.1));
        scrollPane.repaint();

    }

    @Override
    public void zoomReset() {
        textPane.getDocument().putProperty("ZOOM_FACTOR", null);

        Double zoomFactor = getZoomFactor();

        textPane.getDocument().putProperty("ZOOM_FACTOR", zoomFactor);
        textPane.getDocument().putProperty("i18n", Boolean.TRUE);

        logger.trace("zoom Reset: factor:" + getZoomFactor());
        textPane.repaint();
        scrollPane.repaint();

    }

    double getZoomFactor() {
        Double zoomFactor = (Double) textPane.getDocument().getProperty("ZOOM_FACTOR");

        if (zoomFactor == null) {
            zoomFactor = 1.0;
        }

        return zoomFactor;
    }

    private class ExtendedJTextPane extends JTextPane {

        public ExtendedJTextPane() {
            super();
            setEditorKit(new ExtendedScaledEditorKit());
        }

        @Override
        public void repaint(int x, int y, int width, int height) {
            super.repaint(0, 0, getWidth(), getHeight());
        }
    };

    private class ExtendedScaledEditorKit extends StyledEditorKit {

        public ViewFactory getViewFactory() {
            return new StyledViewFactory();
        }

        class StyledViewFactory implements ViewFactory {

            public View create(Element elem) {
                String kind = elem.getName();
                if (kind != null) {
                    if (kind.equals(AbstractDocument.ContentElementName)) {
                        return new LabelView(elem);
                    } else if (kind.equals(AbstractDocument.ParagraphElementName)) {
                        return new ParagraphView(elem);
                    } else if (kind.equals(AbstractDocument.SectionElementName)) {
                        return new ExtendedScaledView(elem, View.Y_AXIS);
                    } else if (kind.equals(StyleConstants.ComponentElementName)) {
                        return new ComponentView(elem);
                    } else if (kind.equals(StyleConstants.IconElementName)) {
                        return new IconView(elem);
                    }
                }
                return new LabelView(elem);
            }

        }

    }

    private class ExtendedScaledView extends BoxView {

        public ExtendedScaledView(Element elem, int axis) {
            super(elem, axis);
        }

        @Override
        public void paint(Graphics g, Shape allocation) {
            try {
                Graphics2D g2d = (Graphics2D) g;
                double zoomFactor = getZoomFactor();
                AffineTransform old = g2d.getTransform();
                g2d.scale(zoomFactor, zoomFactor);
                super.paint(g2d, allocation);
                g2d.setTransform(old);
            } catch (Exception e) {
            }
        }

        @Override
        public float getMinimumSpan(int axis) {
            float f = super.getMinimumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getMaximumSpan(int axis) {
            float f = super.getMaximumSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        public float getPreferredSpan(int axis) {
            float f = super.getPreferredSpan(axis);
            f *= getZoomFactor();
            return f;
        }

        @Override
        protected void layout(int width, int height) {
            super.layout(new Double(width / getZoomFactor()).intValue(),
                    new Double(height
                            * getZoomFactor()).intValue());
        }

        @Override
        public Shape modelToView(int pos, Shape a, Position.Bias b) throws BadLocationException {
            double zoomFactor = getZoomFactor();
            Rectangle alloc;
            alloc = a.getBounds();
            Shape s = super.modelToView(pos, alloc, b);
            alloc = s.getBounds();
            alloc.x *= zoomFactor;
            alloc.y *= zoomFactor;
            alloc.width *= zoomFactor;
            alloc.height *= zoomFactor;

            return alloc;
        }

        @Override
        public int viewToModel(float x, float y, Shape a,
                Position.Bias[] bias) {
            double zoomFactor = getZoomFactor();
            Rectangle alloc = a.getBounds();
            x /= zoomFactor;
            y /= zoomFactor;
            alloc.x /= zoomFactor;
            alloc.y /= zoomFactor;
            alloc.width /= zoomFactor;
            alloc.height /= zoomFactor;

            return super.viewToModel(x, y, alloc, bias);
        }
    }
}
