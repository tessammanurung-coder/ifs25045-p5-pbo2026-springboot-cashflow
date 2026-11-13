package org.delcom.ifs25045springbootcashflow.framework.view;

import org.delcom.ifs25045springbootcashflow.adapter.presenter.FinancePresenter;
import org.delcom.ifs25045springbootcashflow.adapter.storage.FileStorage;
import org.delcom.ifs25045springbootcashflow.domain.entity.SortOption;
import org.delcom.ifs25045springbootcashflow.domain.entity.Transaction;
import org.delcom.ifs25045springbootcashflow.domain.entity.TransactionType;
import org.delcom.ifs25045springbootcashflow.domain.entity.User;
import org.delcom.ifs25045springbootcashflow.domain.exception.DomainException;
import org.delcom.ifs25045springbootcashflow.framework.auth.AuthSession;
import org.delcom.ifs25045springbootcashflow.usecase.FinanceUseCase;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.html.Div;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.progressbar.ProgressBar;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.value.ValueChangeMode;
import com.vaadin.flow.router.BeforeEnterEvent;
import com.vaadin.flow.router.BeforeEnterObserver;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.streams.UploadHandler;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

/** Dashboard cashflow milik pengguna yang sedang login (USER maupun ADMIN). */
@Route(value = "", layout = MainLayout.class)
@PageTitle("Cashflow")
public class FinanceView extends VerticalLayout implements BeforeEnterObserver {
    private final FinanceUseCase finance;
    private final FinancePresenter presenter;
    private final FileStorage storage;
    private final AuthSession session;

    private final SummaryCards summary;
    private final TextField search = new TextField();
    private final ComboBox<SortOption> sort = new ComboBox<>();
    private final TransactionGrid grid;
    private final Div empty = new Div(new Span("Belum ada transaksi. Tambahkan pemasukan atau pengeluaran pertama Anda."));
    private User user;

    public FinanceView(FinanceUseCase finance, FinancePresenter presenter, FileStorage storage,
                       AuthSession session) {
        this.finance = finance;
        this.presenter = presenter;
        this.storage = storage;
        this.session = session;
        addClassName("finance-view");
        setPadding(true);

        summary = new SummaryCards(presenter);
        grid = new TransactionGrid(presenter, this::confirmDelete);
        empty.addClassName("empty-state");

        search.setPlaceholder("Cari keterangan...");
        search.setPrefixComponent(VaadinIcon.SEARCH.create());
        search.setClearButtonVisible(true);
        search.setValueChangeMode(ValueChangeMode.LAZY);
        search.setValueChangeTimeout(300);
        search.addValueChangeListener(e -> refresh());
        search.addClassName("toolbar-search");

        sort.setItems(SortOption.values());
        sort.setItemLabelGenerator(presenter::sortLabel);
        sort.setValue(SortOption.NEWEST);
        sort.setAllowCustomValue(false);
        sort.setClearButtonVisible(false);
        sort.setAriaLabel("Urutkan");
        sort.addValueChangeListener(e -> refresh());

        Button reload = new Button("Muat ulang", new Icon(VaadinIcon.REFRESH), e -> refresh());
        Button addIncome = new Button("Pemasukan", new Icon(VaadinIcon.PLUS), e -> openForm(TransactionType.INCOME));
        addIncome.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_SUCCESS);
        Button addExpense = new Button("Pengeluaran", new Icon(VaadinIcon.MINUS), e -> openForm(TransactionType.EXPENSE));
        addExpense.addThemeVariants(ButtonVariant.LUMO_PRIMARY, ButtonVariant.LUMO_ERROR);

        HorizontalLayout toolbar = new HorizontalLayout(search, sort, reload, addIncome, addExpense);
        toolbar.setWidthFull();
        toolbar.setAlignItems(FlexComponent.Alignment.END);
        toolbar.getStyle().set("flex-wrap", "wrap");
        toolbar.addClassName("toolbar");

        add(summary, toolbar, grid, empty);
    }

    @Override
    public void beforeEnter(BeforeEnterEvent event) {
        user = session.currentUser().orElse(null);
        if (user != null) {
            refresh();
        }
    }

    private void refresh() {
        if (user == null) {
            return;
        }
        try {
            List<Transaction> items = finance.search(user.getId(), search.getValue(), sort.getValue());
            grid.setItems(items);
            summary.update(finance.getSummary(user.getId()));
            empty.setVisible(items.isEmpty());
            grid.setVisible(!items.isEmpty());
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
    }

    private void confirmDelete(Transaction t) {
        Dialogs.confirm("Hapus transaksi", "Hapus " + presenter.formatRow(t) + "? Tindakan ini tidak dapat dibatalkan.",
                "Hapus", () -> delete(t));
    }

    private void delete(Transaction t) {
        try {
            Transaction removed = finance.delete(user.getId(), t.getId());
            if (removed.getProofPath() != null) {
                storage.delete(removed.getProofPath());
            }
            Notifier.success(presenter.deletedMessage(removed));
        } catch (DomainException ex) {
            Notifier.error(presenter.failureMessage(ex));
        }
        refresh();
    }

    private void openForm(TransactionType type) {
        boolean income = type == TransactionType.INCOME;
        Dialog dialog = Dialogs.create(income ? "Tambah pemasukan" : "Tambah pengeluaran");

        TextField description = new TextField("Keterangan");
        description.setWidthFull();
        description.setMaxLength(200);
        description.setPlaceholder(income ? "mis. Gaji bulanan" : "mis. Makan siang");
        TextField amount = new TextField("Jumlah (Rp)");
        amount.setWidthFull();
        amount.setAllowedCharPattern("[0-9]");
        amount.setPlaceholder("mis. 25000");
        amount.setHelperText("Angka bulat tanpa titik atau koma");

        AtomicReference<byte[]> fileData = new AtomicReference<>();
        AtomicReference<String> fileName = new AtomicReference<>();
        Upload upload = new Upload(UploadHandler.inMemory((metadata, bytes) -> {
            fileData.set(bytes);
            fileName.set(metadata.fileName());
        }));
        upload.setAcceptedFileTypes("image/png", "image/jpeg", "image/gif", "image/webp");
        upload.setMaxFiles(1);
        upload.setMaxFileSize(FileStorage.MAX_BYTES);
        upload.setWidthFull();
        upload.setUploadButton(new Button("Pilih bukti (opsional)", new Icon(VaadinIcon.UPLOAD)));
        upload.setDropLabel(new Span("atau tarik gambar ke sini (maks. 5 MB)"));
        upload.addFileRemovedListener(e -> {
            fileData.set(null);
            fileName.set(null);
        });

        ProgressBar progress = LoadingAction.newBar();
        upload.addStartedListener(e -> progress.setVisible(true));
        upload.addAllFinishedListener(e -> progress.setVisible(false));

        VerticalLayout form = new VerticalLayout(description, amount, upload, progress);
        form.setPadding(false);
        dialog.add(form);

        Button cancel = new Button("Batal", e -> dialog.close());
        Button save = new Button("Simpan");
        save.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        save.addClickListener(e -> LoadingAction.run(progress, () -> {
            if (save(dialog, type, description.getValue(), amount.getValue(), fileName.get(), fileData.get())) {
                dialog.close();
            }
        }, save, cancel));
        dialog.getFooter().add(cancel, save);
        dialog.open();
    }

    /** @return true bila transaksi tersimpan (dialog boleh ditutup). */
    private boolean save(Dialog dialog, TransactionType type, String description, String amount,
                         String fileName, byte[] fileData) {
        String proofPath = null;
        try {
            if (fileData != null) {
                proofPath = storage.store("proof", fileName, fileData);
            }
            Transaction saved = type == TransactionType.INCOME
                    ? finance.addIncome(user.getId(), description, amount, proofPath)
                    : finance.addExpense(user.getId(), description, amount, proofPath);
            Notifier.success(presenter.addedMessage(saved));
            refresh();
            return true;
        } catch (DomainException ex) {
            if (proofPath != null) {
                storage.delete(proofPath);
            }
            Notifier.error(presenter.failureMessage(ex));
            return false;
        }
    }
}
