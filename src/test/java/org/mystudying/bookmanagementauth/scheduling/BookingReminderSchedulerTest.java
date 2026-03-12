package org.mystudying.bookmanagementauth.scheduling;

import org.junit.jupiter.api.Test;
import org.mystudying.bookmanagementauth.domain.ReminderType;
import org.mystudying.bookmanagementauth.repositories.BookingRepository;
import org.mystudying.bookmanagementauth.services.BookingReminderService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@SpringBootTest
@ActiveProfiles("test")
public class BookingReminderSchedulerTest {

    @Autowired
    private BookingReminderScheduler scheduler;

    @MockBean
    private BookingRepository bookingRepository;

    @MockBean
    private BookingReminderService reminderService;

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

    @Test
    void shouldHandleEmptyListsGracefully() {
        when(bookingRepository.findBookingIdsDueInDays(any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.findBookingIdsDueToday(any(), any())).thenReturn(Collections.emptyList());
        when(bookingRepository.findBookingIdsOverdueByDays(any(), any())).thenReturn(Collections.emptyList());

        scheduler.sendReminders();

        verify(reminderService, never()).processReminder(anyLong(), any());
    }

    @Test
    void shouldIsolateFailuresBetweenBatches() {
        // GIVEN: First query fails
        when(bookingRepository.findBookingIdsDueInDays(any(), any())).thenThrow(new RuntimeException("DB Error"));
        // AND: Second query succeeds
        when(bookingRepository.findBookingIdsDueToday(any(), any())).thenReturn(List.of(2L));
        when(bookingRepository.findBookingIdsOverdueByDays(any(), any())).thenReturn(Collections.emptyList());

        // WHEN: Executing scheduler
        scheduler.sendReminders();

        // THEN: Verify second batch still processed
        verify(reminderService).processReminder(eq(2L), eq(ReminderType.DUE_TODAY));
    }
}
