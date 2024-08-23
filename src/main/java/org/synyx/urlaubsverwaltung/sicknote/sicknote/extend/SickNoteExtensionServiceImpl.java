package org.synyx.urlaubsverwaltung.sicknote.sicknote.extend;

import org.slf4j.Logger;
import org.springframework.stereotype.Service;
import org.synyx.urlaubsverwaltung.absence.DateRange;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNote;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteService;
import org.synyx.urlaubsverwaltung.sicknote.sicknote.SickNoteStatus;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendar;
import org.synyx.urlaubsverwaltung.workingtime.WorkingTimeCalendarService;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.lang.invoke.MethodHandles.lookup;
import static org.slf4j.LoggerFactory.getLogger;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.extend.SickNoteExtensionStatus.ACCEPTED;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.extend.SickNoteExtensionStatus.SUBMITTED;
import static org.synyx.urlaubsverwaltung.sicknote.sicknote.extend.SickNoteExtensionStatus.SUPERSEDED;

@Service
class SickNoteExtensionServiceImpl implements SickNoteExtensionService {

    private static final Logger LOG = getLogger(lookup().lookupClass());

    private final SickNoteExtensionRepository repository;
    private final SickNoteService sickNoteService;
    private final WorkingTimeCalendarService workingTimeCalendarService;
    private final Clock clock;

    SickNoteExtensionServiceImpl(SickNoteExtensionRepository repository,
                                 SickNoteService sickNoteService,
                                 WorkingTimeCalendarService workingTimeCalendarService,
                                 Clock clock) {
        this.repository = repository;
        this.sickNoteService = sickNoteService;
        this.workingTimeCalendarService = workingTimeCalendarService;
        this.clock = clock;
    }

    @Override
    public Optional<SickNoteExtension> findSubmittedExtensionOfSickNote(SickNote sickNote) {
        return findMostRecentSubmittedExtensionEntity(sickNote)
            .map(entity -> toSickNoteExtension(entity, getAdditionalWorkingDays(sickNote, entity)));
    }

    @Override
    public void updateExtensionsForConvertedSickNote(SickNote sickNote) {
        if (sickNote.getStatus() == SickNoteStatus.CONVERTED_TO_VACATION) {
            final List<SickNoteExtensionEntity> toSave = repository.findAllBySickNoteIdOrderByCreatedAtDesc(sickNote.getId())
                .stream()
                .filter(e -> e.getStatus() == SUBMITTED)
                .map(e -> {
                    e.setStatus(SUPERSEDED);
                    return e;
                })
                .toList();

            if (toSave.isEmpty()) {
                LOG.info("No sickNoteExtensions available to update for sickNote={}", sickNote.getId());
            } else {
                LOG.info("Update sickNoteExtension status from SUBMITTED to SUPERSEDED of ids={}", toSave.stream().map(SickNoteExtensionEntity::getId).toList());
                repository.saveAll(toSave);
            }
        }
    }

    @Override
    public SickNoteExtension createSickNoteExtension(SickNote sickNote, LocalDate newEndDate) {

        // first update existing extension entities status to be SUPERSEDED now
        final List<SickNoteExtensionEntity> entities = repository.findAllBySickNoteIdOrderByCreatedAtDesc(sickNote.getId());
        for (SickNoteExtensionEntity entity : entities) {
            if (entity.getStatus() == SUBMITTED) {
                LOG.info("Update previous sickNoteExtension id={} status from SUBMITTED to SUPERSEDED.", entity.getId());
                entity.setStatus(SUPERSEDED);
            }
        }
        repository.saveAll(entities);

        // then create new submitted extension entity

        final SickNoteExtensionEntity extensionEntity = new SickNoteExtensionEntity();
        extensionEntity.setSickNoteId(sickNote.getId());
        extensionEntity.setNewEndDate(newEndDate);
        extensionEntity.setCreatedAt(Instant.now(clock));
        extensionEntity.setStatus(SUBMITTED);

        final SickNoteExtensionEntity saved = repository.save(extensionEntity);

        final BigDecimal additionalWorkdays = getWorkdays(sickNote.getPerson(), sickNote.getEndDate(), newEndDate);
        final SickNoteExtension submittedExtension = toSickNoteExtension(saved, additionalWorkdays);

        LOG.info("Created sickNoteExtension id={} of sickNote id={}", submittedExtension.id(), submittedExtension.sickNoteId());

        return submittedExtension;
    }

    @Override
    public SickNote acceptSubmittedExtension(Long sickNoteId) {

        final SickNote sickNote = getSickNote(sickNoteId);
        final List<SickNoteExtensionEntity> extensionEntities = repository.findAllBySickNoteIdOrderByCreatedAtDesc(sickNoteId);

        // TODO nice error handling? while this should not happen when using the correct ui workflow...
        if (extensionEntities.isEmpty()) {
            throw new IllegalStateException("cannot accept submitted extension. no extensions could be found.");
        }
        if (extensionEntities.stream().noneMatch(e -> e.getStatus() == SUBMITTED)) {
            throw new IllegalStateException("cannot accept submitted extension. no extension with status SUBMITTED could be found.");
        }

        final LocalDate nextEndDate = extensionEntities.getFirst().getNewEndDate();

        // aub is not in scope currently
        // will be handled with https://github.com/urlaubsverwaltung/urlaubsverwaltung/issues/4551
        final SickNote updatedSickNote = sickNoteService.save(
            SickNote.builder(sickNote).endDate(nextEndDate).build()
        );

        for (SickNoteExtensionEntity entity : extensionEntities) {
            if (entity.getStatus() == SUBMITTED) {
                LOG.info("Update sickNoteExtension id={} status from SUBMITTED to ACCEPTED.", entity.getId());
                entity.setStatus(ACCEPTED);
            }
        }
        repository.saveAll(extensionEntities);

        LOG.info("Accepted sick note extension: {}", updatedSickNote);

        return updatedSickNote;
    }

    private Optional<SickNoteExtensionEntity> findMostRecentSubmittedExtensionEntity(SickNote sickNote) {
        final List<SickNoteExtensionEntity> extensionEntities = repository.findAllBySickNoteIdOrderByCreatedAtDesc(sickNote.getId());
        if (extensionEntities.isEmpty()) {
            LOG.debug("No extensions found for sickNote id={}", sickNote.getId());
            return Optional.empty();
        }
        if (extensionEntities.stream().noneMatch(e -> e.getStatus() == SUBMITTED)) {
            LOG.debug("No extension with status=SUBMITTED found for sickNote id={}", sickNote.getId());
            return Optional.empty();
        }
        final SickNoteExtensionEntity extensionEntity = extensionEntities.getFirst();
        LOG.debug("Found most recent submitted SickNoteExtensionEntity {}", extensionEntity);
        return Optional.of(extensionEntity);
    }

    private BigDecimal getAdditionalWorkingDays(SickNote sickNote, SickNoteExtensionEntity extensionEntity) {
        final DateRange dateRange = new DateRange(sickNote.getStartDate(), extensionEntity.getNewEndDate());
        final WorkingTimeCalendar workingTimeCalendar = getWorkingTimeCalendar(sickNote.getPerson(), dateRange);
        return workingTimeCalendar.workingTime(sickNote.getEndDate(), extensionEntity.getNewEndDate())
            // workingTime for the same date is 1. therefore we have to subtract 1 for "additional" days
            .subtract(BigDecimal.ONE);
    }

    private WorkingTimeCalendar getWorkingTimeCalendar(Person person, DateRange dateRange) {
        final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarService.getWorkingTimesByPersons(List.of(person), dateRange).get(person);
        if (workingTimeCalendar == null) {
            throw new IllegalStateException("expected workingTimeCalender of person=%s to exist for dateRange=%s".formatted(person.getId(), dateRange));
        }
        return workingTimeCalendar;
    }

    private BigDecimal getWorkdays(Person person, LocalDate start, LocalDate end) {
        final WorkingTimeCalendar workingTimeCalendar = workingTimeCalendarService.getWorkingTimesByPersons(List.of(person), new DateRange(start, end)).get(person);
        return workingTimeCalendar.workingTime(start, end);
    }

    private SickNote getSickNote(Long id) {
        return sickNoteService.getById(id)
            .orElseThrow(() -> new IllegalStateException("could not find sickNote with id=" + id));
    }

    private SickNoteExtension toSickNoteExtension(SickNoteExtensionEntity entity, BigDecimal additionalWorkdays) {
        return new SickNoteExtension(entity.getId(), entity.getSickNoteId(), entity.getNewEndDate(), entity.getStatus(), additionalWorkdays);
    }
}
