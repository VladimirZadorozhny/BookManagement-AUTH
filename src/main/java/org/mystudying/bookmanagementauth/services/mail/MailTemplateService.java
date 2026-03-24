package org.mystudying.bookmanagementauth.services.mail;

import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.dto.AdminNotificationItem;
import org.mystudying.bookmanagementauth.dto.AdminNotificationType;
import org.springframework.stereotype.Service;
import org.thymeleaf.context.Context;
import org.thymeleaf.spring6.SpringTemplateEngine;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

@Service
public class MailTemplateService {

    private static final DateTimeFormatter DATE_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy");
    private static final DateTimeFormatter LOGIN_TIME_FORMAT = DateTimeFormatter.ofPattern("MMM dd, yyyy HH:mm:ss");

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

    public String buildReminderMail(String userName, String bookTitle, LocalDate dueDate, ReminderType reminderType) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("bookTitle", bookTitle);
        context.setVariable("dueDate", dueDate.format(DATE_FORMAT));

        String headline;
        String messagePrefix;
        String messageSuffix;
        String ctaText;
        switch (reminderType) {
            case THREE_DAYS_LEFT:
                headline = "Book Due Soon";
                messagePrefix = "This is a friendly reminder that your borrowed book";
                messageSuffix = "is due in 3 days, on";
                ctaText = "Please return it on time to avoid any late fees.";
                break;
            case DUE_TODAY:
                headline = "Book Due Today";
                messagePrefix = "Your borrowed book";
                messageSuffix = "is due today,";
                ctaText = "Kindly return it as soon as possible.";
                break;
            case OVERDUE:
                headline = "Book Overdue";
                messagePrefix = "This is an urgent reminder that your borrowed book";
                messageSuffix = "was due on";
                ctaText = "Please return the book immediately to minimize any accumulated fines.";
                break;
            default:
                throw new IllegalArgumentException("Unknown reminder type: " + reminderType);
        }

        context.setVariable("headline", headline);
        context.setVariable("messagePrefix", messagePrefix);
        context.setVariable("messageSuffix", messageSuffix);
        context.setVariable("ctaText", ctaText);

        return templateEngine.process("mail/reminder", context);
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
        context.setVariable("loginTime", loginTime.format(LOGIN_TIME_FORMAT));
        return templateEngine.process("mail/login-alert", context);
    }

    public String buildAdminNotificationMail(String userName,
                                             AdminNotificationType type,
                                             java.util.List<AdminNotificationItem> items,
                                             String summary) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("headline", getAdminNotificationHeadline(type));
        context.setVariable("intro", getAdminNotificationIntro(type));
        context.setVariable("summary", summary);
        context.setVariable("notificationType", type.name());
        context.setVariable("items", items);
        return templateEngine.process("mail/notification", context);
    }

    public String getAdminNotificationSubject(AdminNotificationType type) {
        switch (type) {
            case HEAVY_USERS:
                return "Library Notice: Multiple Active Borrowings";
            case OVERDUE:
                return "Action Required: Overdue Books";
            case UNPAID_FINES:
                return "Action Required: Unpaid Fines";
            default:
                return "Library Notification";
        }
    }

    public String formatDate(LocalDate date) {
        return date.format(DATE_FORMAT);
    }

    private String getAdminNotificationHeadline(AdminNotificationType type) {
        switch (type) {
            case HEAVY_USERS:
                return "Active Borrowings Summary";
            case OVERDUE:
                return "Overdue Books Notice";
            case UNPAID_FINES:
                return "Unpaid Fines Notice";
            default:
                return "Library Notification";
        }
    }

    private String getAdminNotificationIntro(AdminNotificationType type) {
        switch (type) {
            case HEAVY_USERS:
                return "We noticed you have multiple active borrowings.";
            case OVERDUE:
                return "Our records show overdue books on your account.";
            case UNPAID_FINES:
                return "Our records show unpaid or accruing fines for the following bookings.";
            default:
                return "Please review the details below.";
        }
    }
}
