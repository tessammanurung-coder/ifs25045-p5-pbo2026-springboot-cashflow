package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.server.ServiceInitEvent;
import com.vaadin.flow.server.VaadinServiceInitListener;
import org.springframework.stereotype.Component;

/** Indikator loading global (bilah di atas halaman) yang muncul cepat saat server memproses. */
@Component
public class LoadingIndicatorInitListener implements VaadinServiceInitListener {
    private static final int FIRST_DELAY_MS = 150;
    private static final int SECOND_DELAY_MS = 1500;
    private static final int THIRD_DELAY_MS = 4000;

    @Override
    public void serviceInit(ServiceInitEvent event) {
        event.getSource().addUIInitListener(uiEvent -> {
            var config = uiEvent.getUI().getLoadingIndicatorConfiguration();
            config.setFirstDelay(FIRST_DELAY_MS);
            config.setSecondDelay(SECOND_DELAY_MS);
            config.setThirdDelay(THIRD_DELAY_MS);
        });
    }
}
