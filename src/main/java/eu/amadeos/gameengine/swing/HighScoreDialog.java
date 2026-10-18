package eu.amadeos.gameengine.swing;

import eu.amadeos.commons.i18n.Messages;
import eu.amadeos.commons.i18n.Mnemonics;
import eu.amadeos.commons.swing.EdtExecutor;
import eu.amadeos.gameengine.score.HighScores;
import eu.amadeos.gameengine.score.PlayerName;
import eu.amadeos.gameengine.score.Score;

import javax.swing.*;
import javax.swing.table.AbstractTableModel;
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Window;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.LongFunction;

/**
 * Shows the high scores of a game, one tab per table, and asks for the name after a good result.
 *
 * <pre>
 * HighScoreDialog.show(window, scores, tables, seconds -> texts.format("time.seconds", seconds), "beginner", place);
 * </pre>
 */
public final class HighScoreDialog {
    /**
     * A table as the dialog shows it.
     *
     * @param id    the table's file name in {@link HighScores}, e.g. {@code beginner}
     * @param title the tab title, e.g. {@code Beginner}
     */
    public record Table(String id, String title) {
        public Table {
            Objects.requireNonNull(id, "id");
            Objects.requireNonNull(title, "title");
        }
    }

    private HighScoreDialog() {
    }

    /**
     * Opens the modal dialog.
     *
     * @param format    how a value reads, e.g. {@code 42 s} or {@code 1,250}
     * @param selected  the table shown first, {@code null} for the first one
     * @param highlight the place in the selected table to highlight (a new entry), -1 for none
     */
    public static void show(Component parent, HighScores scores, List<Table> tables, LongFunction<String> format,
                            String selected, int highlight) {
        EdtExecutor.requireEdt();
        Messages texts = GameTexts.load();
        JDialog dialog = new JDialog(window(parent), texts.get("scores.title"), JDialog.ModalityType.APPLICATION_MODAL);
        dialog.setName("high-scores");
        dialog.setDefaultCloseOperation(WindowConstants.DISPOSE_ON_CLOSE);

        JTabbedPane tabs = new JTabbedPane();
        tabs.setName("score-tabs");
        for (Table table : tables) {
            ScoreModel model = new ScoreModel(texts, scores.list(table.id()), format);
            JTable view = new JTable(model);
            view.setName("scores-" + table.id());
            view.setFillsViewportHeight(true);
            view.setRowSelectionAllowed(true);
            view.getTableHeader().setReorderingAllowed(false);
            view.getColumnModel().getColumn(0).setMaxWidth(40);
            if (table.id().equals(selected) && highlight >= 0 && highlight < model.getRowCount()) {
                view.setRowSelectionInterval(highlight, highlight);
            }
            JScrollPane scroll = new JScrollPane(view);
            scroll.setPreferredSize(new Dimension(420, view.getRowHeight() * (HighScores.SIZE + 1) + 8));
            tabs.addTab(table.title(), scroll);
            if (table.id().equals(selected)) tabs.setSelectedComponent(scroll);
        }

        JButton reset = Mnemonics.apply(new JButton(), texts.get("scores.reset"));
        reset.setName("scores-reset");
        reset.addActionListener(e -> {
            int index = tabs.getSelectedIndex();
            if (index < 0) return;
            Table table = tables.get(index);
            int answer = JOptionPane.showConfirmDialog(dialog, texts.format("scores.reset.confirm", table.title()),
                    texts.get("scores.title"), JOptionPane.OK_CANCEL_OPTION, JOptionPane.WARNING_MESSAGE);
            if (answer != JOptionPane.OK_OPTION) return;
            scores.clear(table.id());
            JTable view = (JTable) ((JScrollPane) tabs.getComponentAt(index)).getViewport().getView();
            view.setModel(new ScoreModel(texts, List.of(), format));
            view.getColumnModel().getColumn(0).setMaxWidth(40);
        });
        JButton close = Mnemonics.apply(new JButton(), texts.get("scores.close"));
        close.setName("scores-close");
        close.addActionListener(e -> dialog.dispose());

        JPanel buttons = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttons.add(reset);
        buttons.add(close);
        JPanel content = new JPanel(new BorderLayout(0, 8));
        content.setBorder(BorderFactory.createEmptyBorder(10, 10, 6, 10));
        content.add(tabs, BorderLayout.CENTER);
        content.add(buttons, BorderLayout.SOUTH);
        dialog.setContentPane(content);
        dialog.getRootPane().setDefaultButton(close);
        dialog.getRootPane().registerKeyboardAction(e -> dialog.dispose(),
                KeyStroke.getKeyStroke("ESCAPE"), JComponent.WHEN_IN_FOCUSED_WINDOW);
        dialog.pack();
        dialog.setLocationRelativeTo(parent);
        dialog.setVisible(true);
    }

    /**
     * Asks for the player's name after a result that enters the table, offering the name used last time.
     *
     * @param message e.g. "You cleared Beginner in 42 seconds."
     * @return the name, also remembered for next time; empty if the player cancelled
     */
    public static Optional<String> askName(Component parent, String message, PlayerName playerName) {
        EdtExecutor.requireEdt();
        Messages texts = GameTexts.load();
        JTextField field = new JTextField(playerName.get(), 20);
        field.setName("player-name");
        JLabel label = Mnemonics.apply(new JLabel(), texts.get("scores.name"));
        label.setLabelFor(field);
        JPanel panel = new JPanel(new GridLayout(0, 1, 0, 4));
        panel.add(new JLabel(message));
        panel.add(label);
        panel.add(field);
        field.selectAll();
        // Focus the field once the option pane is shown.
        field.addAncestorListener(new javax.swing.event.AncestorListener() {
            @Override
            public void ancestorAdded(javax.swing.event.AncestorEvent e) {
                SwingUtilities.invokeLater(field::requestFocusInWindow);
            }

            @Override
            public void ancestorRemoved(javax.swing.event.AncestorEvent e) {
            }

            @Override
            public void ancestorMoved(javax.swing.event.AncestorEvent e) {
            }
        });
        int answer = JOptionPane.showConfirmDialog(parent, panel, texts.get("scores.new.title"),
                JOptionPane.OK_CANCEL_OPTION, JOptionPane.PLAIN_MESSAGE);
        String name = Score.clean(field.getText());
        if (answer != JOptionPane.OK_OPTION || name.isEmpty()) return Optional.empty();
        playerName.set(name);
        return Optional.of(name);
    }

    private static Window window(Component parent) {
        if (parent instanceof Window w) return w;
        return parent == null ? null : SwingUtilities.getWindowAncestor(parent);
    }

    /** Place, name, value and date of each entry. */
    static final class ScoreModel extends AbstractTableModel {
        private final List<Score> scores;
        private final LongFunction<String> format;
        private final String[] columns;
        private final DateTimeFormatter dates = DateTimeFormatter.ofLocalizedDateTime(FormatStyle.SHORT)
                .withZone(ZoneId.systemDefault());

        ScoreModel(Messages texts, List<Score> scores, LongFunction<String> format) {
            this.scores = scores;
            this.format = format;
            this.columns = new String[]{texts.get("scores.column.place"), texts.get("scores.column.name"),
                    texts.get("scores.column.score"), texts.get("scores.column.date")};
        }

        @Override
        public int getRowCount() {
            return scores.size();
        }

        @Override
        public int getColumnCount() {
            return columns.length;
        }

        @Override
        public String getColumnName(int column) {
            return columns[column];
        }

        @Override
        public Object getValueAt(int row, int column) {
            Score s = scores.get(row);
            return switch (column) {
                case 0 -> (row + 1) + ".";
                case 1 -> s.name();
                case 2 -> format.apply(s.value());
                default -> dates.format(s.date());
            };
        }

        @Override
        public boolean isCellEditable(int row, int column) {
            return false;
        }
    }
}
