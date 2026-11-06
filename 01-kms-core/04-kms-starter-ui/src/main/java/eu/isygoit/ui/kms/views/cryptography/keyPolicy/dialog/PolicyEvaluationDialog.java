package eu.isygoit.ui.kms.views.cryptography.keyPolicy.dialog;

import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.button.ButtonVariant;
import com.vaadin.flow.component.html.Pre;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.icon.Icon;
import com.vaadin.flow.component.icon.VaadinIcon;
import com.vaadin.flow.component.orderedlayout.FlexComponent;
import com.vaadin.flow.component.orderedlayout.HorizontalLayout;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.theme.lumo.LumoUtility;
import eu.isygoit.i18n.I18n;
import eu.isygoit.ui.common.dialog.NoActionDialog;

public final class PolicyEvaluationDialog extends NoActionDialog {

    public PolicyEvaluationDialog(
            String action,
            String decision,
            String reason,
            String sid,
            String principal,
            String selectedKeyId,
            String keyArn,
            String matchedStatement) {
        super(I18n.t("kms.policy.eval.title", action));
        addClassName("kms-dialog");
        setWidth("600px");
        setResizable(true);
        Button closeButton = new Button(I18n.t("kms.policy.eval.close"), event -> close());
        closeButton.addThemeVariants(ButtonVariant.LUMO_PRIMARY);
        getFooter().removeAll();
        getFooter().add(closeButton);

        VerticalLayout content = new VerticalLayout();
        content.setSpacing(true);
        content.setPadding(true);

        String decisionText;
        String decisionModifierClass;
        Icon decisionIcon;
        if ("ALLOWED".equals(decision)) {
            decisionIcon = VaadinIcon.CHECK_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.allowed");
            decisionModifierClass = "policy-eval-decision-span--allowed";
        } else if ("DENIED".equals(decision)) {
            decisionIcon = VaadinIcon.CLOSE_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.denied");
            decisionModifierClass = "policy-eval-decision-span--denied";
        } else {
            decisionIcon = VaadinIcon.EXCLAMATION_CIRCLE.create();
            decisionText = I18n.t("kms.policy.eval.decision.uncertain");
            decisionModifierClass = "policy-eval-decision-span--uncertain";
        }
        decisionIcon.addClassName(decisionModifierClass);

        Span decisionSpan = new Span(decisionText);
        decisionSpan.addClassName("policy-eval-decision-span");
        decisionSpan.addClassName(decisionModifierClass);

        HorizontalLayout decisionLayout = new HorizontalLayout(decisionIcon, decisionSpan);
        decisionLayout.setAlignItems(FlexComponent.Alignment.CENTER);
        decisionLayout.setSpacing(true);
        content.add(decisionLayout);

        Span reasonSpan = new Span(I18n.t("kms.policy.eval.reason", reason));
        reasonSpan.addClassName("policy-eval-reason-span");
        content.add(reasonSpan);

        if (sid != null && !sid.isEmpty()) {
            HorizontalLayout sidLayout = new HorizontalLayout();
            sidLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            sidLayout.setSpacing(true);
            Span sidLabel = new Span("🆔 " + I18n.t("kms.policy.eval.sid"));
            sidLabel.addClassName(LumoUtility.FontWeight.BOLD);
            sidLayout.add(sidLabel, new Span(sid));
            content.add(sidLayout);
        }

        if (principal != null && !principal.isEmpty()) {
            HorizontalLayout principalLayout = new HorizontalLayout();
            principalLayout.setAlignItems(FlexComponent.Alignment.CENTER);
            principalLayout.setSpacing(true);
            Span principalLabel = new Span("👤 " + I18n.t("kms.policy.eval.principal"));
            principalLabel.addClassName(LumoUtility.FontWeight.BOLD);
            principalLayout.add(principalLabel, new Span(principal));
            content.add(principalLayout);
        }

        if (selectedKeyId == null) {
            Span warningSpan = new Span(I18n.t("kms.policy.eval.no.key.selected"));
            warningSpan.addClassName("policy-eval-warning-span");
            content.add(warningSpan);
        } else {
            Span resourceSpan = new Span(I18n.t("kms.policy.eval.key.arn", keyArn));
            resourceSpan.addClassName("policy-eval-muted-span");
            content.add(resourceSpan);
        }

        if (matchedStatement != null && !matchedStatement.isEmpty()) {
            Span matchedHeader = new Span(I18n.t("kms.policy.eval.matched.statement"));
            matchedHeader.addClassName("policy-eval-matched-header");
            content.add(matchedHeader);

            Pre pre = new Pre();
            pre.setText(matchedStatement);
            pre.addClassName("policy-eval-matched-pre");
            pre.setWidthFull();
            content.add(pre);
        }

        Span helpSpan = new Span(I18n.t("kms.policy.eval.tip"));
        helpSpan.addClassName("policy-eval-help-span");
        content.add(helpSpan);
        add(content);
    }
}
