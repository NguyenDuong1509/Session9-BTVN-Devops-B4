package com.quickbite.hr;

import com.quickbite.hr.model.Employee;
import com.quickbite.hr.repository.EmployeeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class EmployeeRepositoryTest {

    @Autowired
    private EmployeeRepository employeeRepository;

    @BeforeEach
    void setUp() {
        employeeRepository.deleteAll();
    }

    @Test
    @DisplayName("Test lưu và truy vấn nhân viên từ Database")
    void testSaveAndFindEmployee() {
        Employee emp = new Employee("Nguyen Van A", "nguyenvana@quickbite.com", "DevOps", 2500.0);
        Employee saved = employeeRepository.save(emp);

        assertNotNull(saved.getId(), "ID không được null sau khi lưu");
        Optional<Employee> found = employeeRepository.findById(saved.getId());
        assertTrue(found.isPresent(), "Phải tìm thấy nhân viên đã lưu");
        assertEquals("Nguyen Van A", found.get().getName());
        assertEquals("DevOps", found.get().getDepartment());
    }

    @Test
    @DisplayName("Test truy vấn danh sách nhân viên theo phòng ban")
    void testFindByDepartment() {
        employeeRepository.save(new Employee("Tran Thi B", "tranthib@quickbite.com", "Backend", 2000.0));
        employeeRepository.save(new Employee("Le Van C", "levanc@quickbite.com", "DevOps", 2200.0));

        List<Employee> devOpsEmployees = employeeRepository.findByDepartment("DevOps");
        assertEquals(1, devOpsEmployees.size(), "Số lượng nhân viên phòng DevOps phải là 1");
        assertEquals("Le Van C", devOpsEmployees.get(0).getName());
    }
}
