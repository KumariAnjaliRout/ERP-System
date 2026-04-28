package com.app.EMS.dto;
//
//
import com.app.EMS.entity.Roles;
import lombok.*;
//import lombok.Data;
//@Data
//@Builder
//public class PayslipDTO {
//

import java.time.LocalDate;

////    private String companyName;
////    private String salaryMonth;
//
//    private String employeeName;
//    private String designation;
//    private String employeeId;
//    private String email;
//
//    /* earnings */
//    private double basic;
//    private double hra;
//    private double travelAllowance;
//    private double medicalAllowance;
//    private double shiftAllowance;
//    private double otherAllowance;
//
//    /* deductions */
//    private double pf;
//    private double professionalTax;
//
//    private double grossSalary;
//    private double totalDeduction;
//    private double netSalary;
//
//    /* attendance */
//    private int totalDays;
//    private int presentDays;
//    private int absentDays;
//    private int halfDays;
//    private String department;
//    private String bankName;
//    private String accountHolderName;
//    private String accountNumber;
//    private String ifscCode;
//    private String pfAccountNumber;
//
//
//}


@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PayslipDTO {

    private Long id;
    private String employeeId;
    private String employeeName;
    private String designation;
    private String department;

    private String bankName;
    private String accountNumber;
    private String accountHolderName;
    private String ifscCode;
    private String pfAccountNumber;

    private Double grossSalary;
    private Double totalDeductions;
    private Double netSalary;

    private String filePath;
    private LocalDate generatedDate;

    private Integer month;
    private Integer year;
    private Roles generatedByRole;
}