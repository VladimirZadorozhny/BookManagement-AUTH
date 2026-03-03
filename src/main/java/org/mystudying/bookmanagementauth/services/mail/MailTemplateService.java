package org.mystudying.bookmanagementauth.services.mail;

import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;

@Service
public class MailTemplateService {

    private final SpringTemplateEngine templateEngine;

    public MailTemplateService(SpringTemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }

    public String buildRegistrationMail(String userName, String verificationLink) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("verificationLink", verificationLink);
        return templateEngine.process("mail/registration", context);
    }

    public String buildReminderMail(String userName, String bookTitle, OffsetDateTime dueDate, ReminderType reminderType) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("bookTitle", bookTitle);
        context.setVariable("dueDate", dueDate.format(DateTimeFormatter.ofPattern("MMM dd, yyyy")));
        context.setVariable("reminderType", reminderType);

        String templateName;
        switch (reminderType) {
            case THREE_DAYS_LEFT:
                templateName = "mail/reminder-due-soon";
                break;
            case DUE_TODAY:
                templateName = "mail/reminder-due-today";
                break;
            case OVERDUE:
                templateName = "mail/reminder-overdue";
                break;
            default:
                throw new IllegalArgumentException("Unknown reminder type: " + reminderType);
        }
        return templateEngine.process(templateName, context);
    }

    public String buildPasswordResetMail(String userName, String resetLink) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("resetLink", resetLink);
        return templateEngine.process("mail/password-reset", context);
    }

    public String buildLoginAlertMail(String userName, java.time.LocalDateTime loginTime) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("loginTime", loginTime.format(DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss")));
        return templateEngine.process("mail/login-alert", context);
    }
}
