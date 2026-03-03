package org.mystudying.bookmanagementauth.scheduling;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.services.BookingReminderService;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class BookingReminderSchedulerTest {


    private final BookingReminderScheduler scheduler;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private BookingReminderService reminderService;

    public BookingReminderSchedulerTest(BookingReminderScheduler scheduler) {
        this.scheduler = scheduler;
    }

    @Test
    void sendRemindersTriggerProcessForEachType() {
        when(bookingRepository.findBookingIdsDueInDays(any(), any())).thenReturn(List.of(1L));
        when(bookingRepository.findBookingIdsDueToday(any(), any())).thenReturn(List.of(2L));
        when(bookingRepository.findBookingIdsOverdueByDays(any(), any())).thenReturn(List.of(3L));

        scheduler.sendReminders();

        verify(reminderService).processReminder(eq(1L), eq(ReminderType.THREE_DAYS_LEFT));
        verify(reminderService).processReminder(eq(2L), eq(ReminderType.DUE_TODAY));
        verify(reminderService).processReminder(eq(3L), eq(ReminderType.OVERDUE));
    }
}
