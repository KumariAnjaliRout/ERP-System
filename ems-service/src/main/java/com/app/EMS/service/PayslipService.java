
package com.app.EMS.service;

import com.app.EMS.client.NotificationFeignClient;
import com.app.EMS.config.CustomUserPrincipal;
import com.app.EMS.dto.NotificationRequestDto;
import com.app.EMS.dto.PayslipDTO;
import com.app.EMS.entity.*;
import com.app.EMS.exception.BadRequestException;
import com.app.EMS.exception.ForbiddenException;
import com.app.EMS.exception.ResourceNotFoundException;
import com.app.EMS.exception.UnauthorizedException;
import com.app.EMS.repository.*;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.io.*;
import java.util.List;
import java.util.Map;
import java.util.UUID;
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class PayslipService {

    private final EmployeeRepository employeeRepo;
    private final PayrollRepository payrollRepo;
    private final SalaryStructureRepository salaryRepo;
    private final EmployeePersonalDetailsRepository personalRepo;
    private final PayslipRepository payslipRepo;
    private final EmployeeDocumentRepository documentRepo;
    private final NotificationFeignClient notificationFeignClient;
    private void validateHierarchy(CustomUserPrincipal principal, Employee target){

        String loggedRole = principal.getRole();
        Roles targetRole = target.getRole();
        UUID loggedUserId = UUID.fromString(principal.getUserId());

        // SUPER ADMIN → full access
        if(loggedRole.equals("ROLE_SUPER_ADMIN")){
            return;
        }

        // ADMIN → HR, MANAGER, ACCOUNTANT + own
        if(loggedRole.equals("ROLE_ADMIN")){

            if(target.getUserId().equals(loggedUserId))
                return;

            if(targetRole == Roles.ROLE_HR ||
                    targetRole == Roles.ROLE_MANAGER ||
                    targetRole == Roles.ROLE_ACCOUNTANT ||
                    targetRole==Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("Admin cannot access this payroll");
        }

        // HR → EMPLOYEE + own
        if(loggedRole.equals("ROLE_HR")){

            if(target.getUserId().equals(loggedUserId))
                return;

            if(targetRole == Roles.ROLE_EMPLOYEE)
                return;

            throw new ForbiddenException("HR can access only employee payroll");
        }

    }

    private String getPanNumber(String empId) {
        return documentRepo
                .findByEmployeeEmployeeIdAndDocumentType(empId, DocumentType.PAN)
                .map(EmployeeDocument::getDocumentNumber)
                .filter(num -> num != null && !num.isBlank())
                .orElse("NA");
    }
    private String getMonthName(int month) {
        String name = java.time.Month.of(month).name();
        return name.substring(0,1) + name.substring(1).toLowerCase();
    }
    public ByteArrayInputStream generatePayslip(String empId,int month, int year, CustomUserPrincipal principal){
        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_EMPLOYEE") &&!principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_ADMIN") && !principal.getRole().equals("ROLE_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN") && !principal.getRole().equals("ROLE_MANAGER") ) {
            throw new ForbiddenException("Access denied");
        }
        String role=principal.getRole();
//        if (!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
//                && !principal.getRole().equals("ROLE_ACCOUNTANT")){
//            UUID userId = UUID.fromString(principal.getUserId());
//            Employee employe = employeeRepo.findByUserId(userId)
//                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
//            boolean inactive=employeeRepo.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
//            if (inactive)
//                throw new BadRequestException("You cannot get your profile.You are inactive employee");
//            Employee employeee = employeeRepo
//                    .findByEmployeeIdAndUserId(empId, userId)
//                    .orElseThrow(() -> new BadRequestException("User mismatch"));
//        }
        Employee employee = employeeRepo.findByEmployeeId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        UUID loggedUserId = UUID.fromString(principal.getUserId());

        boolean inactive = employeeRepo.existsByUserIdAndStatus(
                loggedUserId,
                EmployeeStatus.INACTIVE
        );

        if (inactive) {
            throw new BadRequestException("Inactive employee cannot access payslip");
        }

        Roles targetRole = employee.getRole();

        /* 🔥 ROLE HIERARCHY */
        validateHierarchy(principal,employee);
//        if(role.equals("ROLE_HR") && targetRole != Roles.ROLE_EMPLOYEE)
//            throw new BadRequestException("HR can generate payroll only for Employees");
//        if(role.equals("ROLE_ADMIN")
//                && !(targetRole == Roles.ROLE_HR
//                || targetRole == Roles.ROLE_MANAGER || targetRole == Roles.ROLE_ACCOUNTANT))
//            throw new BadRequestException("Admin can generate payroll only for HR,Accountant and Manager");

        Employee emp = employeeRepo.findByEmployeeId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
        EmployeePersonalDetails personal=personalRepo.findByEmployeeEmployeeId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Employee personal not found"));
        SalaryStructure salary = salaryRepo.findByEmployeeId(empId)
                .orElseThrow(() -> new ResourceNotFoundException("Salary structure not found"));
        Payroll payroll = payrollRepo
                .findByEmployee_EmployeeIdAndMonthAndYear(empId,month,year)
                .orElseThrow(() -> new ResourceNotFoundException("Payroll not generated"));

        if(payroll.getStatus() != PayrollStatus.PAID){
            throw new BadRequestException(
                    "Payslip can be generated only after salary is credicted"
            );
        }

        String panNumber = getPanNumber(empId);
        String monthName = getMonthName(month);
        /* ---------- HTML TEMPLATE ---------- */

        String html = buildHtml(emp,personal,payroll,salary,monthName,year,panNumber);
        try{
            ByteArrayOutputStream out = new ByteArrayOutputStream();

            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.withHtmlContent(html,null);
            builder.toStream(out);
            builder.run();

            byte[] pdfBytes = out.toByteArray();
            String folderPath = "uploads/payslips/";
            File folder = new File(folderPath);
            if (!folder.exists()) {
                folder.mkdirs();
            }

            String fileName = "Payslip_" + empId + "_" + monthName + "_" + year + ".pdf";
            String path = folderPath + fileName;
            try (FileOutputStream fos = new FileOutputStream(path)) {
                fos.write(pdfBytes);
            }
            Payslip payslip = payslipRepo
                    .findByPayroll_Id(payroll.getId())
                    .orElse(Payslip.builder().payroll(payroll).build());

            payslip.setEmployeeId(emp.getEmployeeId());
            payslip.setEmployeeName(emp.getFirstName()+" "+emp.getLastName());
            payslip.setDesignation(emp.getDesignation());
            payslip.setDepartment(emp.getDepartment());

            payslip.setBankName(personal.getBankName());
            payslip.setAccountNumber(personal.getAccountNumber());
            payslip.setAccountHolderName(personal.getAccountHolderName());
            payslip.setIfscCode(personal.getIfscCode());
            payslip.setPfAccountNumber(personal.getPfAccountNumber());

            payslip.setGrossSalary(payroll.getGrossSalary());
            payslip.setTotalDeductions(payroll.getTotalDeductions());
            payslip.setNetSalary(payroll.getNetSalary());

            payslip.setFilePath(path);
            payslip.setGeneratedDate(java.time.LocalDate.now());
            payslip.setGeneratedByRole(emp.getRole());

            payslipRepo.save(payslip);
            try {

                NotificationRequestDto notification = NotificationRequestDto.builder()
                        .category(NotificationCategory.PAYROLL)
                        .type(NotificationType.PAYSLIP_GENERATED)
                        .priority(NotificationPriority.NORMAL)
                        .organizationId(principal.getOrganizationId())
                        .targetUserId(emp.getUserId())
                        .targetRole(emp.getRole().name().replace("ROLE_", ""))
                        .metadata(Map.of(
                                "triggeredByRole", principal.getRole(),
                                "triggeredByUserId", principal.getUserId(),
                                "employeeId", emp.getEmployeeId(),
                                "month", month,
                                "year", year
                        ))
                        .build();

                notificationFeignClient.sendNotification(notification);

            } catch (Exception ex) {
                log.error("Notification failed for PAYSLIP_GENERATED", ex);
            }
            return new ByteArrayInputStream(pdfBytes);

        }catch(Exception e){
            throw new RuntimeException("PDF generation failed",e);
        }
    }

    private String buildHtml(Employee e,EmployeePersonalDetails emp, Payroll p, SalaryStructure s,String monthName, int year,String panNumber){

        return """
    <html xmlns="http://www.w3.org/1999/xhtml">
    <head>
        <style>
            body{
                font-family: Arial;
                font-size:12px;
                color:#000;
            }
            .header{
                text-align:center;
                margin-bottom:20px;
            }
            .title{
                font-size:18px;
                font-weight:bold;
            }
            .section-title{
                font-weight:bold;
                margin-top:20px;
                margin-bottom:5px;
                font-size:14px;
            }
            table{
                width:100%%;
                border-collapse:collapse;
            }
            td,th{
                border:1px solid #000;
                padding:6px;
                text-align:left;
            }
            .no-border td{
                border:none;
                padding:3px;
            }
            .right{text-align:right;}
            .bold{font-weight:bold;}
            .center{text-align:center;}
            .footer{
                margin-top:30px;
                text-align:center;
                font-size:11px;
            }
        </style>
    </head>

    <body>
        <div class="header">
                        <div class="title">Anasol Consultancy Services Private Limited</div>
                        <div style="font-size:11px; margin-top:5px;">
                            #1016, 11th Floor, Dsl Abacus IT Park, Uppal, Hyderabad, 500039
                        </div>
                        <div class="section-title" style="margin-top:8px;">
                         Salary Slip for %s %s
                        </div>
           </div>

        <div class="section-title">Employee Details</div>

        <table class="no-border">
            <tr>
                <td>Name:</td><td>%s</td>
                <td>Employee ID:</td><td>%s</td>
            </tr>
            <tr>
                <td>Designation:</td><td>%s</td>
                <td>Email:</td><td>%s</td>
            </tr>
        </table>
        <div class="section-title">Bank Details</div>

                <table>
                    <tr>
                        <td>Department:</td><td>%s</td>
                        <td>Bank Name:</td><td>%s</td>
                    </tr>
                    <tr>
                        <td>Account Holder:</td><td>%s</td>
                        <td>Account No:</td><td>%s</td>
                    </tr>
                    <tr>
                        <td>IFSC Code:</td><td>%s</td>
                        <td>PF Number:</td><td>%s</td>
                    </tr>
                     <tr>
                         <td>PAN Number:</td><td>%s</td>
                          <td>Phone Number:</td><td>%s</td>
                     </tr>
                </table>


        <div class="section-title">Salary Details</div>

        <table>
            <tr>
                <th>Earnings</th>
                <th class="right">Amount</th>
                <th>Deductions</th>
                <th class="right">Amount</th>
            </tr>

            <tr>
                <td>Basic</td><td class="right">%.2f</td>
                <td>PF</td><td class="right">%.2f</td>
            </tr>

            <tr>
                <td>HRA</td><td class="right">%.2f</td>
                <td>Professional Tax</td><td class="right">%.2f</td>
            </tr>

            <tr>
                <td>Travel Allowance</td><td class="right">%.2f</td>
                <td></td><td></td>
            </tr>

            <tr>
                <td>Medical Allowance</td><td class="right">%.2f</td>
                <td></td><td></td>
            </tr>

            <tr>
                <td>Shift Allowance</td><td class="right">%.2f</td>
                <td></td><td></td>
            </tr>

            <tr>
                <td>Other Allowance</td><td class="right">%.2f</td>
                <td></td><td></td>
            </tr>

            <tr class="bold">
                <td>Gross Salary</td><td class="right">%.2f</td>
                <td>Total Deductions</td><td class="right">%.2f</td>
            </tr>

            <tr class="bold">
                <td colspan="2"></td>
                <td>Net Salary</td>
                <td class="right">%.2f</td>
            </tr>
        </table>


        <div class="section-title">Attendance Summary</div>

        <table>
            <tr>
                <th>Total Days</th>
                <th>Present</th>
                <th>Half Days</th>
                <th>Absent</th>
            </tr>

            <tr class="center">
                <td>%d</td>
                <td>%d</td>
                <td>%d</td>
                <td>%d</td>
            </tr>
        </table>


        <div class="footer">
            This is system generated payslip and does not require signature.
        </div>

    </body>
    </html>
    """.formatted(
                monthName, year,
                e.getFirstName()+" "+e.getLastName(),
                e.getEmployeeId(),
                e.getDesignation(),
                e.getEmail(),
                e.getDepartment(),
                emp.getBankName(),
                emp.getAccountHolderName(),
                emp.getAccountNumber(),
                emp.getIfscCode(),
                emp.getPfAccountNumber(),
                panNumber,
                e.getPhone(),

                n(s.getBasic()),
                n(p.getPf()),
                n(s.getHra()),
                n(p.getProfessionalTax()),

                n(s.getTravelAllowance()),
                n(s.getMedicalAllowance()),
                n(s.getShiftAllowance()),
                n(s.getOtherAllowance()),

                n(p.getGrossSalary()),
                n(p.getTotalDeductions()),
                n(p.getNetSalary()),

                p.getTotalDays(),
                p.getPresentDays(),
                p.getHalfDays(),
                p.getAbsentDays()
        );
    }
    private double n(Double d){
        return d==null?0:d;
    }

    public List<PayslipDTO> getPayslipsByEmployee(String employeeId,
                                                  CustomUserPrincipal principal){

        if (principal == null) {
            throw new UnauthorizedException("Unauthorized");
        }
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ACCOUNTANT")
                && !principal.getRole().equals("ROLE_MANAGER")
                && !principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_ACCOUNTANT")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId = UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_EMPLOYEE") && !principal.getRole().equals("ROLE_HR")
                && !principal.getRole().equals("ROLE_MANAGER") && !principal.getRole().equals("ROLE_ACCOUNTANT")){

            Employee employee = employeeRepo.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("Employee not found"));
            boolean inactive=employeeRepo.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive)
                throw new BadRequestException("You cannot get your profile.You are inactive employee");
            Employee employeee = employeeRepo
                    .findByEmployeeIdAndUserId(employeeId, userId)
                    .orElseThrow(() -> new BadRequestException("Employee mismatch"));
        }
//        Employee employeee = employeeRepo
//                .findByEmployeeIdAndUserId(employeeId, userId)
//                .orElseThrow(() -> new BadRequestException("Employee mismatch"));

        return payslipRepo.findByEmployeeId(employeeId).stream()
                .map(this::mapToDTO)
                .toList();
    }


    /* =========================================================
                    GET ALL PAYSLIPS
       ========================================================= */

    public List<PayslipDTO> getAllPayslips(CustomUserPrincipal principal){

        if(principal == null)
            throw new UnauthorizedException("Unauthorized");
        if (!principal.getRole().equals("ROLE_HR") && !principal.getRole().equals("ROLE_ADMIN")
                && !principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            throw new ForbiddenException("Access denied");
        }
        UUID userId=UUID.fromString(principal.getUserId());
        if(!principal.getRole().equals("ROLE_SUPER_ADMIN")) {
            Employee employeee = employeeRepo.findByUserId(userId)
                    .orElseThrow(() -> new ResourceNotFoundException("User not found"));
            boolean inactive_higher = employeeRepo.existsByUserIdAndStatus(userId, EmployeeStatus.INACTIVE);
            if (inactive_higher)
                throw new ResourceNotFoundException("Inactive User cannot perform this action");
        }
        String role = principal.getRole();
        String orgId=principal.getOrganizationId();

        if(role.equals("ROLE_SUPER_ADMIN"))
            return payslipRepo.findAll().stream().map(this::mapToDTO).toList();

        if(role.equals("ROLE_ADMIN")){

            List<Employee> list =
                    employeeRepo.findByOrganisationAndRoleIn(orgId,
                            List.of("ROLE_HR","ROLE_MANAGER","ROLE_ACCOUNTANT","ROLE_EMPLOYEE"));

            List<String> ids = list.stream().map(Employee::getEmployeeId).toList();

            return payslipRepo.findByEmployeeIdIn(ids)
                    .stream().map(this::mapToDTO).toList();
        }

        if(role.equals("ROLE_HR")){

            List<Employee> list =
                    employeeRepo.findByOrganisationAndRole(orgId,Roles.ROLE_EMPLOYEE);

            List<String> ids = list.stream().map(Employee::getEmployeeId).toList();

            return payslipRepo.findByEmployeeIdIn(ids)
                    .stream().map(this::mapToDTO).toList();
        }

        throw new ForbiddenException("Access denied");
    }


    private PayslipDTO mapToDTO(Payslip p) {

        return PayslipDTO.builder()
                .id(p.getId())
                .employeeId(p.getEmployeeId())
                .employeeName(p.getEmployeeName())
                .designation(p.getDesignation())
                .department(p.getDepartment())
                .bankName(p.getBankName())
                .accountNumber(p.getAccountNumber())
                .accountHolderName(p.getAccountHolderName())
                .ifscCode(p.getIfscCode())
                .pfAccountNumber(p.getPfAccountNumber())
                .grossSalary(p.getGrossSalary())
                .totalDeductions(p.getTotalDeductions())
                .netSalary(p.getNetSalary())
                .filePath(p.getFilePath())
                .generatedDate(p.getGeneratedDate())
                .month(p.getPayroll().getMonth())   // safe now
                .year(p.getPayroll().getYear())
                .generatedByRole(p.getPayroll().getGeneratedByRole())
                .build();
    }

}

