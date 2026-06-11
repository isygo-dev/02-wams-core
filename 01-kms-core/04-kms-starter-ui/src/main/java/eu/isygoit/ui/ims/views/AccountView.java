package eu.isygoit.ui.ims.views;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.avatar.Avatar;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.combobox.ComboBox;
import com.vaadin.flow.component.confirmdialog.ConfirmDialog;
import com.vaadin.flow.component.dialog.Dialog;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.GridVariant;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.notification.NotificationVariant;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.textfield.TextField;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.data.binder.Binder;
import com.vaadin.flow.data.binder.ValidationException;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import com.vaadin.flow.server.streams.DownloadHandler;
import com.vaadin.flow.server.streams.DownloadResponse;
import eu.isygoit.dto.AddressDto;
import eu.isygoit.dto.ContactDto;
import eu.isygoit.dto.data.AccountDetailsDto;
import eu.isygoit.dto.data.AccountDto;
import eu.isygoit.dto.data.MinAccountDto;
import eu.isygoit.enums.IEnumContact;
import eu.isygoit.enums.IEnumEnabledBinaryStatus;
import eu.isygoit.enums.IEnumLanguage;
import eu.isygoit.remote.ims.AccountDetailsService;
import eu.isygoit.remote.ims.AccountImageService;
import eu.isygoit.remote.ims.AccountService;
import eu.isygoit.ui.layout.ImsMainLayout;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

@Route(value = "ims/accounts", layout = ImsMainLayout.class)
@PageTitle("Account Management")
public class AccountView extends VerticalLayout {

    private final AccountService accountService;
    private final AccountDetailsService accountDetailsService;
    private final AccountImageService accountImageService;

    private final Grid<MinAccountDto> grid = new Grid<>(MinAccountDto.class);
    private final Button addButton = new Button("New Account", VaadinIcon.PLUS.create());
    private final Button refreshButton = new Button("Refresh", VaadinIcon.REFRESH.create());

    public AccountView(@Autowired AccountService accountService,
                       @Autowired AccountDetailsService accountDetailsService,
                       @Autowired AccountImageService accountImageService) {
        this.accountService = accountService;
        this.accountDetailsService = accountDetailsService;
        this.accountImageService = accountImageService;

        setSizeFull();
        setPadding(true);
        setSpacing(true);

        add(createHeader(), createGrid());
        refreshGrid();

        addButton.addClickListener(e -> openAccountDialog(null));
        refreshButton.addClickListener(e -> refreshGrid());
    }

    private HorizontalLayout createHeader() {
        HorizontalLayout header = new HorizontalLayout(
                new H2("Account Management"),
                addButton,
                refreshButton
        );
        header.setWidthFull();
        header.setJustifyContentMode(FlexComponent.JustifyContentMode.BETWEEN);
        header.setAlignItems(FlexComponent.Alignment.CENTER);
        return header;
    }

    private Component createGrid() {
        grid.removeAllColumns();
        grid.addColumn(MinAccountDto::getFullName).setHeader("Full Name").setSortable(true);
        grid.addColumn(MinAccountDto::getEmail).setHeader("Email").setSortable(true);
        grid.addColumn(MinAccountDto::getCode).setHeader("Username").setSortable(true);
        grid.addColumn(acc -> acc.getAdminStatus() != null ? acc.getAdminStatus().name() : "").setHeader("Status");
        grid.addComponentColumn(this::buildActionButtons).setHeader("Actions").setWidth("150px");

        grid.addThemeVariants(GridVariant.LUMO_ROW_STRIPES, GridVariant.LUMO_COMPACT);
        grid.setSizeFull();
        return grid;
    }

    private Component buildActionButtons(MinAccountDto account) {
        HorizontalLayout actions = new HorizontalLayout();
        Button editBtn = new Button(VaadinIcon.EDIT.create());
        editBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY);
        editBtn.addClickListener(e -> loadAndEditAccount(account.getId()));

        Button deleteBtn = new Button(VaadinIcon.TRASH.create());
        deleteBtn.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
        deleteBtn.addClickListener(e -> confirmDelete(account.getId()));

        actions.add(editBtn, deleteBtn);
        return actions;
    }

    private void confirmDelete(Long id) {
        ConfirmDialog dialog = new ConfirmDialog();
        dialog.setHeader("Delete Account");
        dialog.setText("Are you sure you want to delete this account? This action cannot be undone.");
        dialog.setConfirmText("Delete");
        dialog.setCancelText("Cancel");
        dialog.addConfirmListener(event -> deleteAccount(id));
        dialog.open();
    }

    private void refreshGrid() {
        ResponseEntity<List<MinAccountDto>> response = accountService.accountsByTenant();
        List<MinAccountDto> accounts = response.getBody();
        if (accounts == null) accounts = new ArrayList<>();
        grid.setItems(accounts);
    }

    private void loadAndEditAccount(Long id) {
        try {
            ResponseEntity<AccountDto> response = accountService.findById(id);
            AccountDto fullAccount = response.getBody();
            if (fullAccount != null) {
                if (fullAccount.getAccountDetails() == null) {
                    fullAccount.setAccountDetails(new AccountDetailsDto());
                }
                openAccountDialog(fullAccount);
            } else {
                showNotification("Account not found", NotificationVariant.LUMO_ERROR);
            }
        } catch (Exception e) {
            showNotification("Error loading account: " + e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void deleteAccount(Long id) {
        try {
            accountService.delete(id);
            showNotification("Account deleted", NotificationVariant.LUMO_SUCCESS);
            refreshGrid();
        } catch (Exception e) {
            showNotification("Delete failed: " + e.getMessage(), NotificationVariant.LUMO_ERROR);
        }
    }

    private void openAccountDialog(AccountDto existingAccount) {
        Dialog dialog = new Dialog();
        dialog.setHeaderTitle(existingAccount == null ? "Create Account" : "Edit Account");
        dialog.setWidth("950px");
        dialog.setModal(true);
        dialog.setDraggable(true);

        Binder<AccountDto> binder = new Binder<>(AccountDto.class);
        AccountDto accountToEdit = existingAccount != null ? existingAccount : new AccountDto();
        if (accountToEdit.getAccountDetails() == null) {
            accountToEdit.setAccountDetails(new AccountDetailsDto());
        }

        // --- Form fields ---
        FormLayout form = new FormLayout();
        form.setResponsiveSteps(new FormLayout.ResponsiveStep("0", 2));

        TextField codeField = new TextField("Username");
        TextField emailField = new TextField("Email");
        TextField fullNameField = new TextField("Full Name (computed)");
        fullNameField.setReadOnly(true);
        ComboBox<IEnumLanguage.Types> languageBox = new ComboBox<>("Language");
        languageBox.setItems(IEnumLanguage.Types.values());
        ComboBox<IEnumEnabledBinaryStatus.Types> statusBox = new ComboBox<>("Admin Status");
        statusBox.setItems(IEnumEnabledBinaryStatus.Types.values());

        // Bind simple fields
        binder.forField(codeField).bind(AccountDto::getCode, AccountDto::setCode);
        binder.forField(emailField).bind(AccountDto::getEmail, AccountDto::setEmail);
        binder.forField(languageBox).bind(AccountDto::getLanguage, AccountDto::setLanguage);
        binder.forField(statusBox).bind(AccountDto::getAdminStatus, AccountDto::setAdminStatus);

        // AccountDetails fields
        TextField firstNameField = new TextField("First Name");
        TextField lastNameField = new TextField("Last Name");
        TextField countryField = new TextField("Country");
        TextField streetField = new TextField("Street");
        TextField cityField = new TextField("City");
        TextField zipField = new TextField("Zip Code");

        // Contacts grid
        Grid<ContactDto> contactsGrid = new Grid<>(ContactDto.class);
        contactsGrid.setHeight("200px");
        contactsGrid.addColumn(ContactDto::getType).setHeader("Type");
        contactsGrid.addColumn(ContactDto::getValue).setHeader("Value");
        contactsGrid.addComponentColumn(contact -> {
            Button remove = new Button(VaadinIcon.TRASH.create());
            remove.addThemeVariants(ButtonVariant.LUMO_TERTIARY, ButtonVariant.LUMO_ERROR);
            remove.addClickListener(e -> {
                if (accountToEdit.getAccountDetails().getContacts() != null) {
                    accountToEdit.getAccountDetails().getContacts().remove(contact);
                    contactsGrid.setItems(accountToEdit.getAccountDetails().getContacts());
                }
            });
            return remove;
        }).setHeader("Actions").setWidth("80px");

        Button addContactBtn = new Button("Add Contact", VaadinIcon.PLUS_CIRCLE.create());
        addContactBtn.addClickListener(e -> {
            Dialog contactDialog = new Dialog();
            contactDialog.setHeaderTitle("New Contact");
            TextField typeField = new TextField("Type (e.g., Phone, Email)");
            TextField valueField = new TextField("Value");
            Button save = new Button("Save", ev -> {
                ContactDto contact = new ContactDto();
                contact.setType(IEnumContact.Types.valueOf(typeField.getValue()));
                contact.setValue(valueField.getValue());
                if (accountToEdit.getAccountDetails().getContacts() == null) {
                    accountToEdit.getAccountDetails().setContacts(new ArrayList<>());
                }
                accountToEdit.getAccountDetails().getContacts().add(contact);
                contactsGrid.setItems(accountToEdit.getAccountDetails().getContacts());
                contactDialog.close();
            });
            contactDialog.add(new VerticalLayout(typeField, valueField, save));
            contactDialog.open();
        });

        // --- Avatar upload ---
        Avatar avatarPreview = new Avatar();
        avatarPreview.setWidth("80px");
        avatarPreview.setHeight("80px");
        if (existingAccount != null && existingAccount.getId() != null) {
            loadAvatarImage(existingAccount.getId(), avatarPreview);
        }

        final byte[][] tempAvatarHolder = new byte[1][];
        UI ui = UI.getCurrent();

        Upload upload = new Upload(event -> {
            try {
                byte[] imageData = event.getInputStream().readAllBytes();
                if (accountToEdit.getId() != null) {
                    // Existing account: upload immediately
                    MultipartFile mpf = new InMemoryMultipartFile(event.getFileName(), imageData);
                    accountImageService.uploadImage(accountToEdit.getId(), mpf);
                    ui.access(() -> {
                        updateAvatarPreview(avatarPreview, imageData);
                        showNotification("Avatar uploaded", NotificationVariant.LUMO_SUCCESS);
                    });
                } else {
                    // New account: store temporarily
                    tempAvatarHolder[0] = imageData;
                }
            } catch (IOException ex) {
                ui.access(() -> showNotification("Upload failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR));
            }
        });
        upload.setAcceptedFileTypes("image/jpeg", "image/png", "image/jpg");
        upload.setMaxFileSize(5 * 1024 * 1024);
        upload.setMaxFiles(1);
        upload.setUploadButton(new Button("Upload Avatar"));

        // Layout arrangement
        form.add(codeField, emailField, fullNameField, languageBox, statusBox,
                firstNameField, lastNameField, countryField,
                streetField, cityField, zipField);
        form.setColspan(fullNameField, 2);

        VerticalLayout contactsLayout = new VerticalLayout(contactsGrid, addContactBtn);
        contactsLayout.setSpacing(true);
        contactsLayout.setPadding(false);

        VerticalLayout leftColumn = new VerticalLayout(form, contactsLayout);
        leftColumn.setSpacing(true);
        leftColumn.setPadding(false);

        VerticalLayout rightColumn = new VerticalLayout(upload, avatarPreview);
        rightColumn.setAlignItems(FlexComponent.Alignment.CENTER);
        rightColumn.setSpacing(true);

        HorizontalLayout mainLayout = new HorizontalLayout(leftColumn, rightColumn);
        mainLayout.setWidthFull();
        mainLayout.setSpacing(true);

        dialog.add(mainLayout);

        // Pre-fill data for editing
        if (existingAccount != null) {
            AccountDetailsDto details = existingAccount.getAccountDetails();
            firstNameField.setValue(details.getFirstName() != null ? details.getFirstName() : "");
            lastNameField.setValue(details.getLastName() != null ? details.getLastName() : "");
            countryField.setValue(details.getCountry() != null ? details.getCountry() : "");
            if (details.getAddress() != null) {
                streetField.setValue(details.getAddress().getStreet() != null ? details.getAddress().getStreet() : "");
                cityField.setValue(details.getAddress().getCity() != null ? details.getAddress().getCity() : "");
                zipField.setValue(details.getAddress().getZipCode() != null ? details.getAddress().getZipCode() : "");
            }
            if (details.getContacts() != null) {
                contactsGrid.setItems(details.getContacts());
            }
            binder.readBean(existingAccount);
        }

        // Auto-update full name
        firstNameField.addValueChangeListener(e -> updateFullName(firstNameField, lastNameField, fullNameField));
        lastNameField.addValueChangeListener(e -> updateFullName(firstNameField, lastNameField, fullNameField));
        updateFullName(firstNameField, lastNameField, fullNameField);

        // Save button
        Button saveBtn = new Button("Save", e -> {
            try {
                binder.writeBean(accountToEdit);

                // Build AccountDetails
                AccountDetailsDto details = accountToEdit.getAccountDetails();
                details.setFirstName(firstNameField.getValue());
                details.setLastName(lastNameField.getValue());
                details.setCountry(countryField.getValue());

                // Build Address if any field is filled
                if (!streetField.getValue().isEmpty() || !cityField.getValue().isEmpty() || !zipField.getValue().isEmpty()) {
                    AddressDto address = AddressDto.builder()
                            .street(streetField.getValue())
                            .city(cityField.getValue())
                            .zipCode(zipField.getValue())
                            .build();
                    details.setAddress(address);
                } else {
                    details.setAddress(null);
                }

                accountToEdit.setAccountDetails(details);

                AccountDto saved;
                if (accountToEdit.getId() == null) {
                    // Create account
                    saved = accountService.create(accountToEdit).getBody();
                    if (saved == null) throw new RuntimeException("Create returned null");
                    // Upload avatar if present
                    if (tempAvatarHolder[0] != null) {
                        MultipartFile mpf = new InMemoryMultipartFile("avatar.jpg", tempAvatarHolder[0]);
                        accountImageService.uploadImage(saved.getId(), mpf);
                    }
                    showNotification("Account created", NotificationVariant.LUMO_SUCCESS);
                } else {
                    saved = accountService.update(accountToEdit.getId(), accountToEdit).getBody();
                    showNotification("Account updated", NotificationVariant.LUMO_SUCCESS);
                }
                dialog.close();
                refreshGrid();
            } catch (ValidationException ex) {
                showNotification("Validation error: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            } catch (Exception ex) {
                showNotification("Save failed: " + ex.getMessage(), NotificationVariant.LUMO_ERROR);
            }
        });
        saveBtn.addThemeVariants(ButtonVariant.LUMO_PRIMARY);

        Button cancelBtn = new Button("Cancel", e -> dialog.close());
        dialog.getFooter().add(saveBtn, cancelBtn);

        dialog.open();
    }

    private void updateFullName(TextField firstName, TextField lastName, TextField fullName) {
        String fn = firstName.getValue();
        String ln = lastName.getValue();
        String combined = (fn != null ? fn : "") + (ln != null ? " " + ln : "");
        fullName.setValue(combined.trim());
    }

    private void loadAvatarImage(Long accountId, Avatar avatar) {
        try {
            var response = accountImageService.downloadImage(accountId);
            if (response.getBody() != null) {
                InputStream inputStream = response.getBody().getInputStream();
                byte[] imageData = inputStream.readAllBytes();
                updateAvatarPreview(avatar, imageData);
            } else {
                avatar.setImage(null);
                avatar.setName("User");
            }
        } catch (Exception e) {
            // No avatar or error – keep default
        }
    }

    private void updateAvatarPreview(Avatar avatar, byte[] imageData) {
        avatar.setImageHandler(
                DownloadHandler.fromInputStream(
                        downloadEvent -> new DownloadResponse(new ByteArrayInputStream(imageData), "avatar", "image/jpeg", -1)
                )
        );
    }

    private void showNotification(String message, NotificationVariant variant) {
        Notification.show(message, 3000, Notification.Position.MIDDLE)
                .addThemeVariants(variant);
    }

    // Helper MultipartFile implementation for byte array
    private static class InMemoryMultipartFile implements MultipartFile {
        private final String name;
        private final byte[] content;
        private final String contentType;

        public InMemoryMultipartFile(String name, byte[] content) {
            this.name = name;
            this.content = content;
            String ext = name != null && name.contains(".") ? name.substring(name.lastIndexOf(".") + 1).toLowerCase() : "";
            this.contentType = switch (ext) {
                case "png" -> "image/png";
                case "jpg", "jpeg" -> "image/jpeg";
                default -> "application/octet-stream";
            };
        }

        @Override
        public String getName() { return name; }

        @Override
        public String getOriginalFilename() { return name; }

        @Override
        public String getContentType() { return contentType; }

        @Override
        public boolean isEmpty() { return content == null || content.length == 0; }

        @Override
        public long getSize() { return content.length; }

        @Override
        public byte[] getBytes() { return content; }

        @Override
        public InputStream getInputStream() { return new ByteArrayInputStream(content); }

        @Override
        public void transferTo(java.io.File dest) throws IOException {
            java.nio.file.Files.write(dest.toPath(), content);
        }
    }
}