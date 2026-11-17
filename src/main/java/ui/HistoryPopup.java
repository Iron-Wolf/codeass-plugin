package ui;

import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

import javax.swing.*;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import com.intellij.openapi.application.ApplicationManager;
import com.intellij.openapi.application.ModalityState;
import com.intellij.openapi.project.Project;
import com.intellij.openapi.ui.popup.JBPopup;
import com.intellij.openapi.ui.popup.JBPopupFactory;
import com.intellij.ui.JBColor;
import com.intellij.ui.components.JBLabel;
import com.intellij.util.ui.JBUI;

import main.ApiResponse;

/**
 * Lightweight popup displaying the 3-month history with a mini horizontal bar chart.
 */
public final class HistoryPopup {

    private HistoryPopup() {

    }

    public static void showPopup(@Nullable Project project,
                                 @NotNull List<ApiResponse.HistoryCycle> history,
                                 @Nullable BigDecimal maxBudget) {

        Runnable showTask = () -> {
            var panel = createHistoryPanel(history, maxBudget);

            JBPopup popup = JBPopupFactory.getInstance()
                                          .createComponentPopupBuilder(panel, null)
                                          .setTitle("Spend History (Last 3 Months)")
                                          .setMovable(true)
                                          .setResizable(false)
                                          .setRequestFocus(true)
                                          .setCancelOnClickOutside(true)
                                          .createPopup();

            if (project != null && !project.isDisposed()) {
                popup.showCenteredInCurrentWindow(project);
            } else {
                popup.showInFocusCenter();
            }
        };

        var app = ApplicationManager.getApplication();
        if (app != null && !app.isDispatchThread()) {
            app.invokeLater(showTask, ModalityState.any());
        } else {
            showTask.run();
        }
    }

    public static JPanel createHistoryPanel(@NotNull List<ApiResponse.HistoryCycle> history,
                                            @Nullable BigDecimal maxBudget) {

        var mainPanel = new JPanel();
        mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS));
        mainPanel.setBorder(JBUI.Borders.empty(12, 16));

        if (history.isEmpty()) {
            mainPanel.add(new JBLabel("No history data available."));
            return mainPanel;
        }

        var maxSpend = history.stream()
                              .map(ApiResponse.HistoryCycle::spend)
                              .max(BigDecimal::compareTo)
                              .orElse(BigDecimal.ZERO);

        var reference = (maxBudget != null && maxBudget.compareTo(BigDecimal.ZERO) > 0)
                ? maxBudget
                : (maxSpend.compareTo(BigDecimal.ZERO) > 0 ? maxSpend : BigDecimal.ONE);

        var gridPanel = new JPanel(new GridBagLayout());
        gridPanel.setOpaque(false);
        gridPanel.setAlignmentX(0.0f);

        var gbc = new GridBagConstraints();
        gbc.anchor = GridBagConstraints.WEST;
        int gridy = 0;

        for (int i = 0; i < history.size(); i++) {
            var cycle = history.get(i);
            var ratio = cycle.spend().divide(reference, 4, RoundingMode.HALF_UP).doubleValue();

            // Month header row
            gbc.gridy = gridy++;
            gbc.gridwidth = 1;

            // Col 0: Month label
            gbc.gridx = 0;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = JBUI.insets(4, 0, 2, 8);
            var monthLabel = new JBLabel(formatCycle(cycle.cycle()));
            monthLabel.setFont(monthLabel.getFont().deriveFont(java.awt.Font.BOLD));
            gridPanel.add(monthLabel, gbc);

            // Col 1: Month progress bar
            gbc.gridx = 1;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = JBUI.insets(4, 4, 2, 8);
            var monthBar = new BarComponent(ratio, 12);
            gridPanel.add(monthBar, gbc);

            // Col 2: Month spend
            gbc.gridx = 2;
            gbc.weightx = 0.0;
            gbc.fill = GridBagConstraints.NONE;
            gbc.insets = JBUI.insets(4, 4, 2, 8);
            var spendLabel = new JBLabel("%s €".formatted(cycle.spendStr()));
            spendLabel.setFont(spendLabel.getFont().deriveFont(java.awt.Font.BOLD));
            gridPanel.add(spendLabel, gbc);

            // Col 3: Month usage details
            gbc.gridx = 3;
            gbc.weightx = 1.0;
            gbc.fill = GridBagConstraints.HORIZONTAL;
            gbc.insets = JBUI.insets(4, 4, 2, 0);
            var detailsText = "(%,d req • %s tokens)".formatted(
                    cycle.requests(),
                    PluginStatusBar.formatTokens(cycle.totalTokens())
            );
            var detailsLabel = new JBLabel(detailsText);
            detailsLabel.setForeground(JBColor.GRAY);
            gridPanel.add(detailsLabel, gbc);

            // Sub-rows: Models breakdown
            if (cycle.byModel() != null && !cycle.byModel().isEmpty()) {
                for (var m : cycle.byModel()) {
                    gbc.gridy = gridy++;

                    var modelRatio = (cycle.spend().compareTo(BigDecimal.ZERO) > 0 && m.spend().compareTo(BigDecimal.ZERO) > 0)
                            ? m.spend().divide(cycle.spend(), 4, RoundingMode.HALF_UP).doubleValue()
                            : 0.0;

                    // Col 0: Model name (bullet + name with exact 16px indent)
                    gbc.gridx = 0;
                    gbc.weightx = 0.0;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.insets = JBUI.insets(2, 16, 2, 8);
                    var modelNameLabel = new JBLabel("• " + m.model());
                    gridPanel.add(modelNameLabel, gbc);

                    // Col 1: Model progress bar within the month
                    gbc.gridx = 1;
                    gbc.weightx = 0.0;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.insets = JBUI.insets(2, 4, 2, 8);
                    var modelBar = new BarComponent(modelRatio, 6);
                    gridPanel.add(modelBar, gbc);

                    // Col 2: Model spend
                    gbc.gridx = 2;
                    gbc.weightx = 0.0;
                    gbc.fill = GridBagConstraints.NONE;
                    gbc.insets = JBUI.insets(2, 4, 2, 8);
                    var modelSpendLabel = new JBLabel("%s €".formatted(m.spendStr()));
                    gridPanel.add(modelSpendLabel, gbc);

                    // Col 3: Model percentage & usage details
                    gbc.gridx = 3;
                    gbc.weightx = 1.0;
                    gbc.fill = GridBagConstraints.HORIZONTAL;
                    gbc.insets = JBUI.insets(2, 4, 2, 0);
                    var modelDetailsLabel = new JBLabel(formatModelUsageDetails(m, cycle.spend()));
                    modelDetailsLabel.setForeground(JBColor.GRAY);
                    gridPanel.add(modelDetailsLabel, gbc);
                }
            } else {
                gbc.gridy = gridy++;
                gbc.gridx = 0;
                gbc.gridwidth = 4;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = JBUI.insets(2, 16, 2, 0);
                var emptyLabel = new JBLabel("• No model activity");
                emptyLabel.setFont(emptyLabel.getFont().deriveFont(java.awt.Font.ITALIC));
                emptyLabel.setForeground(JBColor.GRAY);
                gridPanel.add(emptyLabel, gbc);
            }

            // Separator between cycles
            if (i < history.size() - 1) {
                gbc.gridy = gridy++;
                gbc.gridx = 0;
                gbc.gridwidth = 4;
                gbc.weightx = 1.0;
                gbc.fill = GridBagConstraints.HORIZONTAL;
                gbc.insets = JBUI.insets(6, 0);
                var sep = new JSeparator();
                sep.setForeground(JBColor.namedColor("Component.borderColor", new JBColor(0xD0D0D0, 0x484B4D)));
                gridPanel.add(sep, gbc);
            }
        }

        mainPanel.add(gridPanel);

        if (maxBudget != null && maxBudget.compareTo(BigDecimal.ZERO) > 0) {
            mainPanel.add(Box.createVerticalStrut(JBUI.scale(10)));
            var footer = new JBLabel("Monthly budget reference: %s €".formatted(
                    maxBudget.setScale(2, RoundingMode.HALF_UP).toPlainString()));
            footer.setFont(footer.getFont().deriveFont(11f));
            footer.setForeground(JBColor.GRAY);
            footer.setAlignmentX(0.0f);
            mainPanel.add(footer);
        }

        return mainPanel;
    }

    static String formatModelUsageDetails(@NotNull ApiResponse.ModelUsage m, @NotNull BigDecimal cycleSpend) {

        String pctStr = "";
        if (cycleSpend.compareTo(BigDecimal.ZERO) > 0 && m.spend().compareTo(BigDecimal.ZERO) > 0) {
            var pct = m.spend()
                       .multiply(BigDecimal.valueOf(100))
                       .divide(cycleSpend, 1, RoundingMode.HALF_UP);
            pctStr = "(%s%%)  ".formatted(pct);
        }

        String usageDetails;
        if (m.totalTokens() > 0) {
            usageDetails = String.format(Locale.ROOT, "(%,d req • %s tokens)", m.requests(), PluginStatusBar.formatTokens(m.totalTokens()));
        } else {
            usageDetails = String.format(Locale.ROOT, "(%,d req)", m.requests());
        }

        return pctStr + usageDetails;
    }

    static String formatModelLine(@NotNull ApiResponse.ModelUsage m, @NotNull BigDecimal cycleSpend) {

        String pctStr = "";
        if (cycleSpend.compareTo(BigDecimal.ZERO) > 0 && m.spend().compareTo(BigDecimal.ZERO) > 0) {
            var pct = m.spend()
                       .multiply(BigDecimal.valueOf(100))
                       .divide(cycleSpend, 1, RoundingMode.HALF_UP);
            pctStr = " <b>(%s%%)</b>".formatted(pct);
        }

        String usageDetails;
        if (m.totalTokens() > 0) {
            usageDetails = String.format(Locale.ROOT, "%,d req &bull; %s tokens", m.requests(),
                                         PluginStatusBar.formatTokens(m.totalTokens()));
        } else {
            usageDetails = String.format(Locale.ROOT, "%,d req", m.requests());
        }

        return "<html>&bull; <b>%s:</b> %s &euro;%s <font color='gray'>(%s)</font></html>".formatted(
                m.model(),
                m.spendStr(),
                pctStr,
                usageDetails
        );
    }

    public static String formatCycle(@Nullable String cycle) {

        if (cycle == null || cycle.isBlank()) {
            return "Unknown";
        }
        var parts = cycle.split("-");
        if (parts.length == 2) {
            var year = Integer.parseInt(parts[0]);
            var month = Integer.parseInt(parts[1]);
            var ym = YearMonth.of(year, month);
            return ym.format(DateTimeFormatter.ofPattern("MMM yyyy", Locale.ENGLISH));
        }
        return "Unknown";
    }

    private static class BarComponent
            extends JComponent {

        private final double ratio;
        private final int barHeight;

        public BarComponent(double ratio) {

            this(ratio, 12);
        }

        public BarComponent(double ratio, int barHeight) {

            this.ratio = Math.clamp(ratio, 0.0, 1.0);
            this.barHeight = barHeight;
            var dim = new Dimension(JBUI.scale(130), JBUI.scale(barHeight + 4));
            setPreferredSize(dim);
            setMinimumSize(dim);
        }

        @Override
        protected void paintComponent(Graphics g) {

            super.paintComponent(g);
            var g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            int w = getWidth();
            int h = getHeight();
            int scaledH = JBUI.scale(barHeight);
            int y = (h - scaledH) / 2;
            int arc = Math.min(scaledH, JBUI.scale(6));

            // Track background
            g2.setColor(JBColor.namedColor("ProgressBar.trackColor", new JBColor(0xE5E5E5, 0x3C3F41)));
            g2.fillRoundRect(0, y, w, scaledH, arc, arc);

            // Progress bar
            int barWidth = (int) Math.round(w * ratio);
            if (barWidth > 0) {
                g2.setColor(JBColor.namedColor("ProgressBar.progressColor", new JBColor(0x3574F0, 0x3592C4)));
                g2.fillRoundRect(0, y, barWidth, scaledH, arc, arc);
            }
            g2.dispose();
        }
    }
}
