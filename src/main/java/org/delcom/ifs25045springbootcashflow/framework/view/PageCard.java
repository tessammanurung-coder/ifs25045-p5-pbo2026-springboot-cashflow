package org.delcom.ifs25045springbootcashflow.framework.view;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.Paragraph;

/** Kartu putih berjudul; dipakai halaman login, registrasi, profil, dan daftar admin. */
public class PageCard extends Div {
    public enum HeadingLevel {
        PAGE,
        SECTION
    }

    public PageCard(String title, String subtitle) {
        this(title, subtitle, HeadingLevel.SECTION);
    }

    public PageCard(String title, String subtitle, HeadingLevel headingLevel) {
        addClassName("page-card");
        Component heading = headingLevel == HeadingLevel.PAGE ? new H1(title) : new H2(title);
        heading.addClassName("page-card-title");
        add(heading);
        if (subtitle != null && !subtitle.isBlank()) {
            Paragraph sub = new Paragraph(subtitle);
            sub.addClassName("page-card-subtitle");
            add(sub);
        }
    }

    public PageCard(String title) {
        this(title, null);
    }

    public PageCard with(Component... components) {
        add(components);
        return this;
    }
}
