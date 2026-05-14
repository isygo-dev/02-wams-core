package eu.isygoit.ui.views;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.formlayout.FormLayout;
import com.vaadin.flow.component.textfield.TextArea;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import eu.isygoit.ui.MainLayout;

@Route(value = "crypto", layout = MainLayout.class)
@PageTitle("Cryptographic Operations")
public class CryptoOperationsView extends FormLayout {

    public CryptoOperationsView() {

        TextArea plaintext = new TextArea("Plaintext");
        TextArea ciphertext = new TextArea("Ciphertext");

        Button encrypt = new Button("Encrypt");
        Button decrypt = new Button("Decrypt");
        Button sign = new Button("Sign");
        Button verify = new Button("Verify");

        add(plaintext, ciphertext, encrypt, decrypt, sign, verify);
    }
}