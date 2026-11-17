package ui;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class HistoryPopupTest {

    @Test
    public void testFormatCycle() {

        assertEquals("Oct 2026", HistoryPopup.formatCycle("2026-10"));
        assertEquals("Sep 2026", HistoryPopup.formatCycle("2026-09"));
        assertEquals("Aug 2026", HistoryPopup.formatCycle("2026-08"));
        assertEquals("Jan 2027", HistoryPopup.formatCycle("2027-01"));
        assertEquals("Unknown", HistoryPopup.formatCycle(null));
        assertEquals("Unknown", HistoryPopup.formatCycle(""));
    }
}
