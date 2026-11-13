package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.progressbar.ProgressBar;

/**
 * Menjalankan aksi dengan ProgressBar yang benar-benar terlihat. Perubahan UI hanya dikirim setelah
 * handler selesai, jadi aksi dijalankan pada round-trip kedua (setelah browser menerima ProgressBar
 * dan membalas), bukan pada klik yang sama.
 */
final class LoadingAction {
    private LoadingAction() {}

    static void run(ProgressBar bar, Runnable work, Button... buttons) {
        UI ui = UI.getCurrent();
        if (ui == null) {
            work.run();
            return;
        }
        bar.setVisible(true);
        for (Button button : buttons) {
            button.setEnabled(false);
        }
        ui.getPage().executeJs("return true;").then(Boolean.class, ready -> {
            try {
                work.run();
            } finally {
                bar.setVisible(false);
                for (Button button : buttons) {
                    button.setEnabled(true);
                }
            }
        });
    }

    static ProgressBar newBar() {
        ProgressBar bar = new ProgressBar();
        bar.setIndeterminate(true);
        bar.setVisible(false);
        return bar;
    }
}
