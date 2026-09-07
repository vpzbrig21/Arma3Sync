package fr.soe.a3s.ui.repository.workers;

import javax.swing.SwingUtilities;

/** Small boundary helper that keeps background service callbacks off Swing components. */
final class SwingUi {
    private SwingUi() { }

    static void run(Runnable action) {
        if (SwingUtilities.isEventDispatchThread()) action.run();
        else SwingUtilities.invokeLater(action);
    }
}
