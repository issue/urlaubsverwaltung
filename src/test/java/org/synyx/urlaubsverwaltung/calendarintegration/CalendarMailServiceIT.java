package org.synyx.urlaubsverwaltung.calendarintegration;


import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.jvnet.mock_javamail.Mailbox;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;
import org.synyx.urlaubsverwaltung.calendarintegration.absence.Absence;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.settings.MailSettings;
import org.synyx.urlaubsverwaltung.settings.Settings;
import org.synyx.urlaubsverwaltung.settings.SettingsDAO;
import org.synyx.urlaubsverwaltung.settings.SettingsService;

import javax.mail.Message;
import javax.mail.MessagingException;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.util.List;

import static java.time.ZoneOffset.UTC;
import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.testdatacreator.TestDataCreator.createPerson;

@SpringBootTest
@RunWith(SpringRunner.class)
@ActiveProfiles("dev")
public class CalendarMailServiceIT {

    @Autowired
    private CalendarMailService sut;

    @Autowired
    private SettingsService settingsService;
    @Autowired
    private SettingsDAO settingsDAO;

    @After
    public void setUp() {
        Mailbox.clearAll();
    }

    @Test
    public void ensureAdministratorGetsANotificationIfACalendarSyncErrorOccurred() throws MessagingException,
        IOException {

        activateMailSettings();

        final Person person = createPerson("user", "Lieschen", "Müller", "lieschen@firma.test");

        Absence absence = mock(Absence.class);
        when(absence.getPerson()).thenReturn(person);
        when(absence.getStartDate()).thenReturn(ZonedDateTime.now(UTC));
        when(absence.getEndDate()).thenReturn(ZonedDateTime.now(UTC));

        sut.sendCalendarSyncErrorNotification("Kalendername", absence, "Calendar sync failed");

        final String adminMail = getAdminMail();

        List<Message> inbox = Mailbox.get(adminMail);
        assertThat(inbox.size()).isOne();

        Message msg = inbox.get(0);

        assertThat(msg.getSubject()).isEqualTo("Fehler beim Synchronisieren des Kalenders");

        String content = (String) msg.getContent();
        assertThat(content).contains("Kalendername");
        assertThat(content).contains("Calendar sync failed");
        assertThat(content).contains(person.getNiceName());
    }

    private String getAdminMail() {
        final Settings settings = settingsService.getSettings();
        final MailSettings mailSettings = settings.getMailSettings();
        return mailSettings.getAdministrator();
    }

    private void activateMailSettings() {
        final Settings settings = settingsService.getSettings();
        final MailSettings mailSettings = settings.getMailSettings();
        mailSettings.setActive(true);
        settings.setMailSettings(mailSettings);
        settingsDAO.save(settings);
    }
}
