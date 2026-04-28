package com.app.EMS.service;

import com.app.EMS.entity.Employee;
import com.app.EMS.entity.Payroll;
import org.springframework.stereotype.Component;
//
//@Component
//public class PayslipHtmlBuilder {
//
//    public String build(Payroll p) {
//
//        return String.format("""
//        <html>
//        <body>
//            <h2>Payslip</h2>
//
//            <p>Employee: %s</p>
//            <p>Month: %d</p>
//
//            <p>Gross Salary: %.2f</p>
//            <p>Deductions: %.2f</p>
//            <p>Net Salary: %.2f</p>
//
//        </body>
//        </html>
//        """,
//
//                p.getEmployee().getEmployeeId(),
//                p.getMonth(),
//                p.getGrossSalary(),
//                p.getTotalDeductions(),
//                p.getNetSalary()
//        );
//    }

@Component
public class PayslipHtmlBuilder {

    public String build(Payroll payroll) {

        Employee e = payroll.getEmployee();

        return """
        <html>
        <head>
        <style>
            body { font-family: Arial; padding:20px; }
            table { width:100%%; border-collapse:collapse; }
            td, th { border:1px solid #000; padding:8px; }
            .center { text-align:center; }
            .title { font-size:22px; font-weight:bold; }
        </style>
        </head>

        <body>

        <div class="center title">Company Name</div>
        <div class="center">Salary Slip for %d/%d</div>

        <br>

        <table>
            <tr>
                <td>Name</td>
                <td>%s %s</td>
                <td>Department</td>
                <td>%s</td>
            </tr>
            <tr>
                <td>Designation</td>
                <td>%s</td>
                <td>Bank Name</td>
                <td>%s</td>
            </tr>
            <tr>
                <td>Employee ID</td>
                <td>%s</td>
                <td>Account No</td>
                <td>%s</td>
            </tr>
        </table>

        <br>

        <table>
        <tr>
            <th>Earnings</th>
            <th>Amount</th>
            <th>Deductions</th>
            <th>Amount</th>
        </tr>

        <tr>
            <td>Basic</td>
            <td>%.2f</td>
            <td>Professional Tax</td>
            <td>%.2f</td>
        </tr>

        <tr>
            <td>HRA</td>
            <td>%.2f</td>
            <td>TDS</td>
            <td>%.2f</td>
        </tr>

        <tr>
            <td>Special Allowance</td>
            <td>%.2f</td>
            <td>EPF</td>
            <td>%.2f</td>
        </tr>

        <tr>
            <th>Gross Salary</th>
            <th>%.2f</th>
            <th>Total Deduction</th>
            <th>%.2f</th>
        </tr>

        <tr>
            <th colspan="3">Net Salary</th>
            <th>%.2f</th>
        </tr>

        </table>

        </body>
        </html>
        """.formatted(
                payroll.getMonth(),
                payroll.getYear(),
                e.getFirstName(),
                e.getLastName(),
                e.getDesignation(),
                e.getDesignation(),
                "HDFC Bank",
                e.getEmployeeId(),
                "XXXXXX1234",

                payroll.getGrossSalary() * 0.4,
                payroll.getGrossSalary() * 0.02,

                payroll.getGrossSalary() * 0.2,
                payroll.getGrossSalary() * 0.05,

                payroll.getGrossSalary() * 0.4,
                payroll.getGrossSalary() * 0.03,

                payroll.getGrossSalary(),
                payroll.getTotalDeductions(),
                payroll.getNetSalary()
        );
    }
}

