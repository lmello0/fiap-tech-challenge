package com.fiap.techchallenge.user.services;

import com.fiap.techchallenge.shared.audit.ActorResolver;
import com.fiap.techchallenge.shared.logging.LogContext;
import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
import com.fiap.techchallenge.user.api.events.CustomerDeactivatedEvent;
import com.fiap.techchallenge.user.api.events.CustomerReactivatedEvent;
import com.fiap.techchallenge.user.api.events.CustomerRegisteredEvent;
import com.fiap.techchallenge.user.api.events.UserEmailChangedEvent;
import com.fiap.techchallenge.user.api.events.UserEmailVerifiedEvent;
import com.fiap.techchallenge.user.api.events.UserProfileUpdatedEvent;
import com.fiap.techchallenge.user.api.events.WorkerRegisteredEvent;
import com.fiap.techchallenge.user.api.events.WorkerTerminatedEvent;
import com.fiap.techchallenge.user.api.queries.UserFilterQuery;
import com.fiap.techchallenge.user.api.representation.UserInfo;
import com.fiap.techchallenge.user.api.representation.UserPrincipal;
import com.fiap.techchallenge.user.entities.PhoneNumber;
import com.fiap.techchallenge.user.entities.User;
import com.fiap.techchallenge.user.entities.Worker;
import com.fiap.techchallenge.user.exceptions.*;
import com.fiap.techchallenge.user.mappers.UserMapper;
import com.fiap.techchallenge.user.repositories.UserRepository;
import com.fiap.techchallenge.user.repositories.WorkerRepository;
import com.fiap.techchallenge.user.repositories.specifications.UserSpecifications;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final UserMapper userMapper;
    private final ApplicationEventPublisher events;
    private final ActorResolver actorResolver;

    @Override
    @Transactional
    public UserInfo createCustomer(CreateUserCommand command) {
        User user = newUser(command);
        user.becomeCustomer();

        User saved = userRepository.save(user);
        LogContext.put("userId", saved.getId());

        UserInfo info = userMapper.toInfo(saved);
        events.publishEvent(new CustomerRegisteredEvent(saved.getId(), actorResolver.forSelf(saved.getId(), true), info));

        return info;
    }

    @Override
    @Transactional
    public UserInfo createWorker(CreateWorkerCommand command) {
        User user = newUser(command.user());
        user.becomeCustomer();

        if (command.startDate().isBefore(command.hireDate())) {
            throw new InvalidWorkerStartDateException(command.startDate(), command.hireDate());
        }

        user.becomeWorker(Worker.builder()
                .registration(generateRegistration())
                .role(command.role())
                .hireDate(command.hireDate())
                .startDate(command.startDate())
                .isActive(true)
                .build());

        User saved = userRepository.save(user);
        LogContext.put("userId", saved.getId());
        LogContext.put("role", command.role());

        UserInfo info = userMapper.toInfo(saved);
        events.publishEvent(new WorkerRegisteredEvent(
                saved.getId(), command.role(), actorResolver.forCurrentUser(false), info));

        return info;
    }

    @Override
    @Transactional
    public void terminateWorker(UUID userId, LocalDate on) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isWorker()) {
            throw new NotAWorkerException(userId);
        }

        user.getWorker().terminate(on);
        LogContext.put("userId", userId);
        LogContext.put("terminatedOn", on);

        events.publishEvent(new WorkerTerminatedEvent(
                userId, on, actorResolver.forCurrentUser(false), userMapper.toInfo(user)));
    }

    @Override
    @Transactional
    public UserInfo updateProfile(UUID userId, UpdateUserProfileCommand command) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        long primaryNumberCount = command.phoneNumbers().stream()
                .filter(RegisterPhoneNumberCommand::isPrimary)
                .count();

        int MAX_PRIMARY_PHONE_NUMBERS = 1;
        if (primaryNumberCount > MAX_PRIMARY_PHONE_NUMBERS) {
            throw new MultiplePrimaryPhoneNumberException();
        }

        user.setFirstName(command.firstName());
        user.setLastName(command.lastName());

        // Flushed separately: uk_phone_primary_per_user only allows one primary phone per user, and
        // PhoneNumber's IDENTITY id forces its insert to run immediately rather than queuing behind
        // the orphan-removal deletes below, so the old primary row has to be gone from the database
        // before a new one is added, not just scheduled for removal.
        user.getPhoneNumbers().clear();
        userRepository.saveAndFlush(user);

        for (RegisterPhoneNumberCommand phone : command.phoneNumbers()) {
            user.addPhoneNumber(PhoneNumber.builder()
                    .type(phone.type())
                    .phone(phone.phone())
                    .isPrimary(phone.isPrimary())
                    .build());
        }

        User saved = userRepository.save(user);
        LogContext.put("userId", userId);

        UserInfo info = userMapper.toInfo(saved);
        events.publishEvent(new UserProfileUpdatedEvent(userId, actorResolver.forCurrentUser(true), info));

        return info;
    }

    @Override
    @Transactional
    public void deactivateCustomer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isCustomer()) {
            throw new NotACustomerException(userId);
        }

        user.getCustomer().deactivate();
        LogContext.put("userId", userId);

        events.publishEvent(new CustomerDeactivatedEvent(userId, actorResolver.forCurrentUser(true), userMapper.toInfo(user)));
    }

    @Override
    @Transactional
    public void reactivateCustomer(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (!user.isCustomer()) {
            throw new NotACustomerException(userId);
        }

        user.getCustomer().reactivate();
        LogContext.put("userId", userId);

        events.publishEvent(new CustomerReactivatedEvent(userId, actorResolver.forCurrentUser(true), userMapper.toInfo(user)));
    }

    @Override
    @Transactional
    public void markEmailVerified(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.verifyEmail();
        LogContext.put("userId", userId);

        events.publishEvent(new UserEmailVerifiedEvent(userId, actorResolver.forSelf(userId, true), userMapper.toInfo(user)));
    }

    @Override
    @Transactional
    public void changeEmail(UUID userId, String newEmail) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        if (userRepository.existsByEmail(newEmail)) {
            throw new EmailAlreadyInUseException();
        }

        user.changeEmail(newEmail);
        LogContext.put("userId", userId);

        events.publishEvent(new UserEmailChangedEvent(userId, actorResolver.forSelf(userId, true), userMapper.toInfo(user)));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserInfo> listCustomers(UserFilterQuery filter, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecifications.isCustomer())
                .and(filterSpec(filter));

        return userRepository
                .findAll(spec, pageable)
                .map(userMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<UserInfo> listWorkers(UserFilterQuery filter, Pageable pageable) {
        Specification<User> spec = Specification
                .where(UserSpecifications.isWorker())
                .and(filterSpec(filter));

        return userRepository
                .findAll(spec, pageable)
                .map(userMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public UserInfo getById(UUID id) {
        return findById(id)
                .orElseThrow(() -> new UserNotFoundException(id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserInfo> findById(UUID id) {
        return userRepository
                .findById(id)
                .map(userMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserInfo> findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .map(userMapper::toInfo);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPrincipal> findPrincipalById(UUID id) {
        return userRepository.findPrincipalById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UserPrincipal> findPrincipalByEmail(String email) {
        return userRepository.findPrincipalByEmail(email);
    }

    private User newUser(CreateUserCommand command) {
        long primaryNumberCount = command.phoneNumbers().stream()
                .filter(RegisterPhoneNumberCommand::isPrimary)
                .count();

        int MAX_PRIMARY_PHONE_NUMBERS = 1;
        if (primaryNumberCount > MAX_PRIMARY_PHONE_NUMBERS) {
            throw new MultiplePrimaryPhoneNumberException();
        }

        if (!DocumentValidator.validate(command.documentType(), command.documentCode())) {
            throw new InvalidDocumentException(command.documentType());
        }

        if (userRepository.existsByEmail(command.email())) {
            throw new EmailAlreadyInUseException();
        }

        if (userRepository.existsByDocumentTypeAndDocumentCode(command.documentType(), command.documentCode())) {
            throw new DocumentAlreadyInUseException();
        }

        User user = User.builder()
                .firstName(command.firstName())
                .lastName(command.lastName())
                .email(command.email())
                .documentType(command.documentType())
                .documentCode(command.documentCode())
                .build();

        for (RegisterPhoneNumberCommand phone : command.phoneNumbers()) {
            user.addPhoneNumber(PhoneNumber.builder()
                    .type(phone.type())
                    .phone(phone.phone())
                    .isPrimary(phone.isPrimary())
                    .build());
        }

        return user;
    }

    private String generateRegistration() {
        String prefix = "ARS";
        Long nextNum = workerRepository.getNextRegistrationSeq();

        return prefix + "-%06d".formatted(nextNum);
    }

    private Specification<User> filterSpec(UserFilterQuery filter) {
        return Specification
                .where(UserSpecifications.nameContains(filter.name()))
                .and(UserSpecifications.emailContains(filter.email()))
                .and(UserSpecifications.documentContains(filter.document()))
                .and(UserSpecifications.phoneContains(filter.phone()));
    }
}
