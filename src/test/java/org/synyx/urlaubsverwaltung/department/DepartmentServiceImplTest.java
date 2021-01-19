package org.synyx.urlaubsverwaltung.department;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.synyx.urlaubsverwaltung.TestDataCreator;
import org.synyx.urlaubsverwaltung.application.domain.Application;
import org.synyx.urlaubsverwaltung.application.service.ApplicationService;
import org.synyx.urlaubsverwaltung.person.Person;
import org.synyx.urlaubsverwaltung.person.Role;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static java.time.ZoneOffset.UTC;
import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.synyx.urlaubsverwaltung.TestDataCreator.createDepartment;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.ALLOWED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.ALLOWED_CANCELLATION_REQUESTED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.REJECTED;
import static org.synyx.urlaubsverwaltung.application.domain.ApplicationStatus.TEMPORARY_ALLOWED;
import static org.synyx.urlaubsverwaltung.person.Role.DEPARTMENT_HEAD;
import static org.synyx.urlaubsverwaltung.person.Role.OFFICE;
import static org.synyx.urlaubsverwaltung.person.Role.SECOND_STAGE_AUTHORITY;
import static org.synyx.urlaubsverwaltung.person.Role.USER;

@ExtendWith(MockitoExtension.class)
class DepartmentServiceImplTest {

    private DepartmentServiceImpl sut;

    @Mock
    private DepartmentRepository departmentRepository;
    @Mock
    private ApplicationService applicationService;

    @BeforeEach
    void setUp() {
        sut = new DepartmentServiceImpl(departmentRepository, applicationService, Clock.systemUTC());
    }

    @Test
    void ensureCallDepartmentDAOSave() {

        final Department department = createDepartment();
        sut.create(department);

        verify(departmentRepository).save(eq(department));
    }

    @Test
    void ensureCallDepartmentRepositoryFindById() {

        sut.getDepartmentById(42);
        verify(departmentRepository).findById(eq(42));
    }

    @Test
    void ensureUpdateCallDepartmentDAOUpdate() {

        final Department department = createDepartment();

        sut.update(department);

        verify(departmentRepository).save(eq(department));
    }

    @Test
    void ensureGetAllCallDepartmentDAOFindAll() {

        sut.getAllDepartments();

        verify(departmentRepository).findAll();
    }

    @Test
    void ensureGetManagedDepartmentsOfDepartmentHeadCallCorrectDAOMethod() {

        final Person person = new Person();

        sut.getManagedDepartmentsOfDepartmentHead(person);

        verify(departmentRepository).getManagedDepartments(person);
    }

    @Test
    void ensureGetManagedDepartmentsOfSecondStageAuthorityCallCorrectDAOMethod() {

        final Person person = new Person();

        sut.getManagedDepartmentsOfSecondStageAuthority(person);

        verify(departmentRepository).getDepartmentsForSecondStageAuthority(person);
    }

    @Test
    void ensureGetAssignedDepartmentsOfMemberCallCorrectDAOMethod() {

        final Person person = new Person();

        sut.getAssignedDepartmentsOfMember(person);

        verify(departmentRepository).getAssignedDepartments(person);
    }

    @Test
    void ensureDeletionIsNotExecutedIfDepartmentWithGivenIDDoesNotExist() {

        int id = 0;
        when(departmentRepository.findById(id)).thenReturn(Optional.empty());

        sut.delete(id);

        verify(departmentRepository, never()).deleteById(anyInt());
    }


    @Test
    void ensureDeleteCallFindOneAndDelete() {

        int id = 0;
        when(departmentRepository.findById(id)).thenReturn(Optional.of(createDepartment()));

        sut.delete(id);

        verify(departmentRepository).findById(eq(id));
        verify(departmentRepository).deleteById(eq(id));
    }

    @Test
    void ensureSetLastModificationOnUpdate() {

        final Department department = new Department();

        sut.update(department);
        assertThat(department.getLastModification()).isToday();
    }

    @Test
    void ensureReturnsAllMembersOfTheManagedDepartmentsOfTheDepartmentHead() {

        final Person departmentHead = new Person();
        final Person secondDepartmentHead = new Person();

        Person admin1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person admin2 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Person marketing1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person marketing2 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person marketing3 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, admin2, departmentHead, secondDepartmentHead));
        admins.setDepartmentHeads(asList(departmentHead, secondDepartmentHead));

        Department marketing = createDepartment("marketing");

        final Person secondStageAuth = new Person();
        secondStageAuth.setPermissions(List.of(USER, SECOND_STAGE_AUTHORITY));

        marketing.setMembers(asList(marketing1, marketing2, marketing3, departmentHead, secondStageAuth));
        marketing.setSecondStageAuthorities(singletonList(secondStageAuth));

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(asList(admins, marketing));

        List<Person> members = sut.getManagedMembersOfDepartmentHead(departmentHead);
        assertThat(members).hasSize(7);
    }

    @Test
    void ensureReturnsEmptyListIfPersonHasNoManagedDepartment() {

        final Person departmentHead = new Person();

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(emptyList());

        List<Person> members = sut.getManagedMembersOfDepartmentHead(departmentHead);
        assertThat(members).isEmpty();
    }

    @Test
    void ensureReturnsTrueIfIsDepartmentHeadOfTheGivenPerson() {

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(USER, DEPARTMENT_HEAD));

        Person admin1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person admin2 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, admin2, departmentHead));

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(admins));

        boolean isDepartmentHead = sut.isDepartmentHeadOfPerson(departmentHead, admin1);
        assertThat(isDepartmentHead).isTrue();
    }

    @Test
    void ensureReturnsFalseIfIsNotDepartmentHeadOfTheGivenPerson() {

        final Person departmentHead = new Person();
        departmentHead.setPermissions(List.of(USER, DEPARTMENT_HEAD));

        Person admin1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person admin2 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, admin2, departmentHead));

        Person marketing1 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(admins));

        boolean isDepartmentHead = sut.isDepartmentHeadOfPerson(departmentHead, marketing1);
        assertThat(isDepartmentHead).isFalse();
    }

    @Test
    void ensureReturnsFalseIfIsInTheSameDepartmentButHasNotDepartmentHeadRole() {

        final Person noDepartmentHead = new Person();
        noDepartmentHead.setPermissions(List.of(USER));

        Person admin1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person admin2 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, admin2, noDepartmentHead));

        boolean isDepartmentHead = sut.isDepartmentHeadOfPerson(noDepartmentHead, admin1);
        assertThat(isDepartmentHead).isFalse();
    }

    @Test
    void ensureReturnsEmptyListOfDepartmentApplicationsIfPersonIsNotAssignedToAnyDepartment() {

        final Person person = new Person();
        person.setPermissions(List.of(USER));

        final LocalDate date = LocalDate.now(UTC);

        when(departmentRepository.getAssignedDepartments(person)).thenReturn(emptyList());

        List<Application> applications = sut.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(person, date, date);
        assertThat(applications).isEmpty();

        verify(departmentRepository).getAssignedDepartments(person);
        verifyNoInteractions(applicationService);
    }

    @Test
    void ensureReturnsEmptyListOfDepartmentApplicationsIfNoMatchingApplicationsForLeave() {

        final Person person = new Person();
        person.setPermissions(List.of(USER));

        LocalDate date = LocalDate.now(UTC);

        Person admin1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person admin2 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Person marketing1 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person marketing2 = new Person("muster", "Muster", "Marlene", "muster@example.org");
        Person marketing3 = new Person("muster", "Muster", "Marlene", "muster@example.org");

        Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, admin2, person));

        Department marketing = createDepartment("marketing");
        marketing.setMembers(asList(marketing1, marketing2, marketing3, person));

        when(departmentRepository.getAssignedDepartments(person)).thenReturn(asList(admins, marketing));
        when(applicationService.getApplicationsForACertainPeriodAndPerson(any(LocalDate.class), any(LocalDate.class), any(Person.class)))
            .thenReturn(emptyList());

        List<Application> applications = sut.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(person, date, date);
        assertThat(applications).isEmpty();

        // Ensure fetches departments of person
        verify(departmentRepository).getAssignedDepartments(person);

        // Ensure fetches applications for leave for every department member
        verify(applicationService).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(admin1));
        verify(applicationService).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(admin2));
        verify(applicationService).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(marketing1));
        verify(applicationService).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(marketing2));
        verify(applicationService).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(marketing3));

        // Ensure does not fetch applications for leave for the given person
        verify(applicationService, never()).getApplicationsForACertainPeriodAndPerson(eq(date), eq(date), eq(person));
    }


    @Test
    void ensureReturnsOnlyWaitingAndAllowedAndCancellationRequestDepartmentApplicationsForLeave() {

        final Person person = new Person();
        person.setPermissions(List.of(USER));

        final LocalDate date = LocalDate.now(UTC);

        final Person admin1 = new Person("shane", "shane", "shane", "shane@example.org");
        final Person marketing1 = new Person("carl", "carl", "carl", "carl@example.org");

        final Department admins = createDepartment("admins");
        admins.setMembers(asList(admin1, person));

        final Department marketing = createDepartment("marketing");
        marketing.setMembers(asList(marketing1, person));

        final Application waitingApplication = new Application();
        waitingApplication.setStatus(TEMPORARY_ALLOWED);

        final Application allowedApplication = new Application();
        allowedApplication.setStatus(ALLOWED);

        final Application cancellationRequestApplication = new Application();
        cancellationRequestApplication.setStatus(ALLOWED_CANCELLATION_REQUESTED);

        final Application otherApplication = new Application();
        otherApplication.setStatus(REJECTED);

        when(departmentRepository.getAssignedDepartments(person)).thenReturn(asList(admins, marketing));

        when(applicationService.getApplicationsForACertainPeriodAndPerson(any(LocalDate.class), any(LocalDate.class), eq(admin1)))
            .thenReturn(asList(waitingApplication, otherApplication));

        when(applicationService.getApplicationsForACertainPeriodAndPerson(any(LocalDate.class), any(LocalDate.class), eq(marketing1)))
            .thenReturn(List.of(allowedApplication, cancellationRequestApplication));

        List<Application> applications = sut.getApplicationsForLeaveOfMembersInDepartmentsOfPerson(person, date, date);
        assertThat(applications)
            .hasSize(3)
            .contains(waitingApplication, allowedApplication, cancellationRequestApplication)
            .doesNotContain(otherApplication);
    }

    @Test
    void ensureSignedInOfficeUserCanAccessPersonData() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        person.setPermissions(singletonList(USER));

        final Person office = new Person("muster", "Muster", "Marlene", "muster@example.org");
        office.setId(2);
        office.setPermissions(asList(USER, OFFICE));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(office, person);
        assertThat(isAllowed).isTrue();
    }

    @Test
    void ensureSignedInBossUserCanAccessPersonData() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        person.setPermissions(singletonList(USER));

        final Person boss = new Person("muster", "Muster", "Marlene", "muster@example.org");
        boss.setId(2);
        boss.setPermissions(asList(USER, Role.BOSS));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(boss, person);
        assertThat(isAllowed).isTrue();
    }

    @Test
    void ensureSignedInDepartmentHeadOfPersonCanAccessPersonData() {

        final Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        person.setPermissions(singletonList(USER));

        final Person departmentHead = new Person("muster", "Muster", "Marlene", "muster@example.org");
        departmentHead.setId(2);
        departmentHead.setPermissions(asList(USER, Role.DEPARTMENT_HEAD));

        Department dep = createDepartment("dep");
        dep.setMembers(asList(person, departmentHead));

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(dep));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(departmentHead, person);
        assertThat(isAllowed).isTrue();
    }

    @Test
    void ensureSignedInDepartmentHeadThatIsNotDepartmentHeadOfPersonCanNotAccessPersonData() {

        Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        person.setPermissions(singletonList(USER));

        Person departmentHead = new Person("muster", "Muster", "Marlene", "muster@example.org");
        departmentHead.setId(2);
        departmentHead.setPermissions(asList(USER, Role.DEPARTMENT_HEAD));

        Department dep = createDepartment("dep");
        dep.setMembers(singletonList(departmentHead));

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(dep));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(departmentHead, person);
        assertThat(isAllowed).isFalse();
    }

    @Test
    void ensureSignedInDepartmentHeadCanNotAccessSecondStageAuthorityPersonData() {

        Person secondStageAuthority = new Person("muster", "Muster", "Marlene", "muster@example.org");
        secondStageAuthority.setId(1);
        secondStageAuthority.setPermissions(asList(USER, Role.SECOND_STAGE_AUTHORITY));

        Person departmentHead = new Person("muster", "Muster", "Marlene", "muster@example.org");
        departmentHead.setId(2);
        departmentHead.setPermissions(asList(USER, Role.DEPARTMENT_HEAD));

        Department dep = createDepartment("dep");
        dep.setMembers(asList(secondStageAuthority, departmentHead));
        dep.setSecondStageAuthorities(singletonList(secondStageAuthority));

        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(dep));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(departmentHead, secondStageAuthority);
        assertThat(isAllowed).isFalse();
    }

    @Test
    void ensureSignedInSecondStageAuthorityCanAccessDepartmentHeadPersonData() {

        Person secondStageAuthority = new Person("muster", "Muster", "Marlene", "muster@example.org");
        secondStageAuthority.setId(1);
        secondStageAuthority.setPermissions(asList(USER, Role.SECOND_STAGE_AUTHORITY, Role.DEPARTMENT_HEAD));

        Person departmentHead = new Person("muster", "Muster", "Marlene", "muster@example.org");
        departmentHead.setId(2);
        departmentHead.setPermissions(asList(USER, Role.DEPARTMENT_HEAD, Role.SECOND_STAGE_AUTHORITY));

        Department dep = createDepartment("dep");
        dep.setMembers(asList(secondStageAuthority, departmentHead));
        dep.setSecondStageAuthorities(singletonList(secondStageAuthority));
        dep.setDepartmentHeads(singletonList(departmentHead));

        when(departmentRepository.getDepartmentsForSecondStageAuthority(secondStageAuthority)).thenReturn(singletonList(dep));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(secondStageAuthority, departmentHead);
        assertThat(isAllowed).isTrue();
    }

    @Test
    void ensureNotPrivilegedUserCanNotAccessPersonData() {

        Person person = new Person("muster", "Muster", "Marlene", "muster@example.org");
        person.setId(1);
        person.setPermissions(singletonList(USER));

        Person user = new Person("muster", "Muster", "Marlene", "muster@example.org");
        user.setId(2);
        user.setPermissions(singletonList(USER));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(user, person);
        assertThat(isAllowed).isFalse();
    }

    @Test
    void ensureNotPrivilegedUserCanAccessOwnPersonData() {

        Person user = new Person("muster", "Muster", "Marlene", "muster@example.org");
        user.setId(1);
        user.setPermissions(singletonList(USER));

        boolean isAllowed = sut.isSignedInUserAllowedToAccessPersonData(user, user);
        assertThat(isAllowed).isTrue();
    }

    @Test
    void ensureBossHasAccessToAllDepartments() {
        Person boss = new Person("muster", "Muster", "Marlene", "muster@example.org");
        boss.setPermissions(asList(USER, Role.BOSS));

        Department dep = TestDataCreator.createDepartment("dep");
        when(departmentRepository.findAll()).thenReturn(singletonList(dep));

        var allowedDepartments = sut.getAllowedDepartmentsOfPerson(boss);
        assertThat(allowedDepartments).containsExactly(dep);
    }

    @Test
    void ensureOfficeHasAccessToAllDepartments() {
        Person office = new Person("muster", "Muster", "Marlene", "muster@example.org");
        office.setPermissions(asList(USER, OFFICE));

        Department dep = TestDataCreator.createDepartment("dep");
        when(departmentRepository.findAll()).thenReturn(singletonList(dep));

        var allowedDepartments = sut.getAllowedDepartmentsOfPerson(office);
        assertThat(allowedDepartments).containsExactly(dep);
    }

    @Test
    void ensureSecondStageAuthorityHasAccessToAllowedDepartments() {
        Person secondStageAuthority = new Person("muster", "Muster", "Marlene", "muster@example.org");
        secondStageAuthority.setPermissions(asList(USER, Role.SECOND_STAGE_AUTHORITY));

        Department dep = TestDataCreator.createDepartment("dep");
        when(departmentRepository.getDepartmentsForSecondStageAuthority(secondStageAuthority)).thenReturn(singletonList(dep));

        var allowedDepartments = sut.getAllowedDepartmentsOfPerson(secondStageAuthority);
        assertThat(allowedDepartments).containsExactly(dep);
    }

    @Test
    void ensureDepartmentHeadHasAccessToAllowedDepartments() {
        Person departmentHead = new Person("muster", "Muster", "Marlene", "muster@example.org");
        departmentHead.setPermissions(asList(USER, Role.DEPARTMENT_HEAD));

        Department dep = TestDataCreator.createDepartment("dep");
        when(departmentRepository.getManagedDepartments(departmentHead)).thenReturn(singletonList(dep));

        var allowedDepartments = sut.getAllowedDepartmentsOfPerson(departmentHead);
        assertThat(allowedDepartments).containsExactly(dep);
    }

    @Test
    void ensurePersonHasAccessToAssignedDepartments() {
        Person user = new Person("muster", "Muster", "Marlene", "muster@example.org");
        user.setPermissions(singletonList(USER));

        Department dep = TestDataCreator.createDepartment("dep");
        when(departmentRepository.getAssignedDepartments(user)).thenReturn(singletonList(dep));

        var allowedDepartments = sut.getAllowedDepartmentsOfPerson(user);
        assertThat(allowedDepartments).containsExactly(dep);
    }
}
