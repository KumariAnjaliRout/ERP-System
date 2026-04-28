package com.app.EMS.service;

import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.EmployeePersonalDetailsRequest;
import com.app.EMS.dto.EmployeePersonalDetailsResponse;
import com.app.EMS.entity.*;
import com.app.EMS.exception.*;
import com.app.EMS.repository.EmployeePersonalDetailsRepository;
import com.app.EMS.repository.EmployeeRepository;
import jakarta.persistence.Column;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Period;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional
public class EmployeePersonalDetailsService {

    private final EmployeeRepository employeeRepository;
    private final EmployeePersonalDetailsRepository personalDetailsRepository;

    public void saveOrUpdatePersonalDetails(
            CustomUserPrincipal principal,
            EmployeePersonalDetailsRequest request
    ) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        String role=principal.getRole();
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_EMPLOYEE")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT") && !principal.getRole().equals("ROLE_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userId = UUID.fromString(principal.getUserId());
        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found with employeeId: " + userId));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");
//        employee.setFirstName(employee.getFirstName());
//        employee.setLastName(employee.getLastName());
        employee.setEmail(employee.getEmail());
        EmployeePersonalDetails details =
                personalDetailsRepository.findByEmployeeUserId(userId)
                        .orElse(EmployeePersonalDetails.builder()
                                .employee(employee)
                                .build());

        if (request.getAccountHolderName() != null &&
                personalDetailsRepository
                        .existsByAccountHolderNameAndEmployeeUserIdNot(
                                request.getAccountHolderName(), userId)) {

            throw new AlreadyExistsResourceException(
                    request.getAccountHolderName() + " account holder name already exists");
        }

        if (request.getAccountNumber() != null &&
                personalDetailsRepository
                        .existsByAccountNumberAndEmployeeUserIdNot(
                                request.getAccountNumber(), userId)) {

            throw new AlreadyExistsResourceException(
                    request.getAccountNumber() + " account number already exists");
        }

        if (request.getIfscCode() != null &&
                personalDetailsRepository
                        .existsByIfscCodeAndEmployeeUserIdNot(
                                request.getIfscCode(), userId)) {

            throw new AlreadyExistsResourceException(
                    request.getIfscCode() + " IFSC code already exists");
        }

        if (request.getPfAccountNumber() != null &&
                personalDetailsRepository
                        .existsByPfAccountNumberAndEmployeeUserIdNot(
                                request.getPfAccountNumber(), userId)) {

            throw new AlreadyExistsResourceException(
                    request.getPfAccountNumber() + " PF account number already exists");
        }

        details.setDob(request.getDob());

        // age: either accept from request or calculate
        details.setAge(
                request.getAge() != null
                        ? request.getAge()
                        : calculateAge(request.getDob())
        );
        // String → Enum safe conversion
        if (request.getGender() != null) {
            details.setGender(Gender.valueOf(request.getGender().toUpperCase()));
        }
        employee.setFirstName(request.getFirstName());
        employee.setLastName(request.getLastName());
        employee.setPhone(request.getPhone());
        details.setAddress(request.getAddress());
        details.setCity(request.getCity());
        details.setState(request.getState());
        details.setPincode(request.getPincode());
        details.setBloodGroup(request.getBloodGroup());
        details.setAccountHolderName(request.getAccountHolderName());
        details.setBankName(request.getBankName());
        details.setAccountNumber(request.getAccountNumber());
        details.setIfscCode(request.getIfscCode());
        details.setPfAccountNumber(request.getPfAccountNumber());
        details.setRole(Roles.valueOf(role));

        personalDetailsRepository.save(details);
    }

    @Transactional
    public EmployeePersonalDetailsResponse getPersonalDetails(CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_EMPLOYEE")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT") && !principal.getRole().equals("ROLE_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userId = UUID.fromString(principal.getUserId());

        Employee employee = employeeRepository.findByUserId(userId)
                .orElseThrow(() ->
                        new ResourceNotFoundException("User not found"));
        boolean inactive=employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
        if (inactive)
            throw new BadRequestException("You cannot get your profile.You are inactive user");

        EmployeePersonalDetails details =
                personalDetailsRepository.findByEmployeeUserId(userId)
                        .orElse(null);

        return EmployeePersonalDetailsResponse.builder()
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phone(employee.getPhone())
                .email(employee.getEmail())

                .dob(details != null ? details.getDob() : null)
                .age(details != null ? details.getAge() : null)
                .gender(details != null && details.getGender()!=null
                        ? details.getGender().name()
                        : null)
                .address(details != null ? details.getAddress() : null)
                .city(details != null ? details.getCity() : null)
                .state(details != null ? details.getState() : null)
                .pincode(details != null ? details.getPincode() : null)
                .bloodGroup(details != null ? details.getBloodGroup() : null)
                .accountHolderName(details != null ? details.getAccountHolderName() : null)
                .bankName(details != null ? details.getBankName() : null)
                .accountNumber(details != null ? details.getAccountNumber() : null)
                .ifscCode(details != null ? details.getIfscCode() : null)
                .pfAccountNumber(details != null ? details.getPfAccountNumber() : null)
                .role(details != null ? details.getRole() : null)
                .build();
    }
    @Transactional
    public EmployeePersonalDetailsResponse getPersonalDetailsByHr(String EmployeeId,CustomUserPrincipal principal) {
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepository.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepository.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot see another user details");
        }
        String role=principal.getRole();
        Employee employee = employeeRepository.findByEmployeeId(EmployeeId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        Roles targetRole = employee.getRole();
        String loggedInOrg = principal.getOrganizationId();
        String targetOrg = employee.getOrganisation();

        // SUPER_ADMIN can access all orgs
        if (!principal.getRole().equals("ROLE_SUPER_ADMIN")) {

            if (loggedInOrg == null || targetOrg == null || !loggedInOrg.equals(targetOrg)) {
                throw new BadRequestException("Cannot get user personal details in another organization");
            }
        }

        if (role.equals("ROLE_ADMIN")) {

            // ADMIN can view HR, MANAGER, ACCOUNTANT
            if (targetRole != Roles.ROLE_HR &&
                    targetRole != Roles.ROLE_MANAGER &&
                    targetRole != Roles.ROLE_ACCOUNTANT &&
                    targetRole != Roles.ROLE_EMPLOYEE) {

                throw new BadRequestException(
                        "ADMIN can view only HR, MANAGER ,EMPLOYEE or ACCOUNTANT"
                );
            }
        }

        else if (role.equals("ROLE_HR")) {

            // HR can view only EMPLOYEE
            if (targetRole != Roles.ROLE_EMPLOYEE) {

                throw new BadRequestException(
                        "HR can view only EMPLOYEE details"
                );
            }
        }
        EmployeePersonalDetails details =
                personalDetailsRepository.findByEmployeeEmployeeId(EmployeeId)
                        .orElse(null);

        return EmployeePersonalDetailsResponse.builder()
                .employeeId(employee.getEmployeeId())
                .firstName(employee.getFirstName())
                .lastName(employee.getLastName())
                .phone(employee.getPhone())
                .email(employee.getEmail())

                .dob(details != null ? details.getDob() : null)
                .age(details != null ? details.getAge() : null)
                .gender(details != null && details.getGender()!=null
                        ? details.getGender().name()
                        : null)
                .address(details != null ? details.getAddress() : null)
                .city(details != null ? details.getCity() : null)
                .state(details != null ? details.getState() : null)
                .pincode(details != null ? details.getPincode() : null)
                .bloodGroup(details != null ? details.getBloodGroup() : null)
                .accountHolderName(details != null ? details.getAccountHolderName() : null)
                .bankName(details != null ? details.getBankName() : null)
                .accountNumber(details != null ? details.getAccountNumber() : null)
                .ifscCode(details != null ? details.getIfscCode() : null)
                .pfAccountNumber(details != null ? details.getPfAccountNumber() : null)
                .role(details != null ? details.getRole() : null)
                .build();
    }

    private int calculateAge(LocalDate dob) {
        return Period.between(dob, LocalDate.now()).getYears();
    }
}
