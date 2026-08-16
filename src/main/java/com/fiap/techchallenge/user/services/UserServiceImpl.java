package com.fiap.techchallenge.user.services;

import com.fiap.techchallenge.user.api.UserService;
import com.fiap.techchallenge.user.api.commands.CreateUserCommand;
import com.fiap.techchallenge.user.api.commands.CreateWorkerCommand;
import com.fiap.techchallenge.user.api.commands.RegisterPhoneNumberCommand;
import com.fiap.techchallenge.user.api.commands.UpdateUserProfileCommand;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final WorkerRepository workerRepository;
    private final UserMapper userMapper;

    @Override
    @Transactional
    public UserInfo createCustomer(CreateUserCommand command) {
        User user = newUser(command);
        user.becomeCustomer();

        User saved = userRepository.save(user);
        log.info("Customer created userId={}", saved.getId());

        return userMapper.toInfo(saved);
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
        log.info("Worker created userId={} role={}", saved.getId(), command.role());

        return userMapper.toInfo(saved);
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
        log.info("Terminated worker userId={} on={}", userId, on);
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

        user.getPhoneNumbers().clear();
        for (RegisterPhoneNumberCommand phone : command.phoneNumbers()) {
            user.addPhoneNumber(PhoneNumber.builder()
                    .type(phone.type())
                    .phone(phone.phone())
                    .isPrimary(phone.isPrimary())
                    .build());
        }

        User saved = userRepository.save(user);
        log.info("Profile updated userId={}", userId);

        return userMapper.toInfo(saved);
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
        log.info("Customer deactivated userId={}", userId);
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
        log.info("Customer reactivated userId={}", userId);
    }

    @Override
    @Transactional
    public void markEmailVerified(UUID userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException(userId));

        user.verifyEmail();
        log.info("Email verified userId={}", userId);
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
        log.info("Email changed userId={}", userId);
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
