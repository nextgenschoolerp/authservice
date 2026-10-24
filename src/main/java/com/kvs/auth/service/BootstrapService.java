package com.kvs.auth.service;

import com.kvs.auth.dto.BootstrapRequest;
import com.kvs.auth.dto.BootstrapResponse;
import com.kvs.auth.dto.BootstrapStatusResponse;
import com.kvs.auth.entity.*;
import com.kvs.auth.repo.*;
import com.kvs.core.exception.ConflictException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.UUID;

/**
 * Service orchestrating initial system bootstrap from Excel or direct onboarding requests
 * for zero-data environments.
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class BootstrapService {

    private final TenantRepository tenantRepository;
    private final UserRepository userRepository;
    private final UserCredentialRepository userCredentialRepository;
    private final UserRoleRepository userRoleRepository;
    private final BranchProvisioningRepository branchProvisioningRepository;
    private final PasswordEncoder passwordEncoder;

    /**
     * Inspect whether the database is uninitialized and requires initial bootstrap.
     */
    public BootstrapStatusResponse getBootstrapStatus() {
        long tenantCount = tenantRepository.count();
        long userCount = userRepository.count();
        boolean bootstrapRequired = (tenantCount == 0);

        return BootstrapStatusResponse.builder()
                .initialized(!bootstrapRequired)
                .bootstrapRequired(bootstrapRequired)
                .tenantCount(tenantCount)
                .userCount(userCount)
                .systemVersion("2.0.0-PROD")
                .build();
    }

    /**
     * Generate an Excel onboarding template with pre-styled headers and sample data.
     */
    public byte[] generateTemplate() throws IOException {
        try (Workbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {

            // Header Style
            CellStyle headerStyle = workbook.createCellStyle();
            Font font = workbook.createFont();
            font.setBold(true);
            font.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(font);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Sheet 1: Organization
            Sheet orgSheet = workbook.createSheet("1_Organization");
            Row orgHeader = orgSheet.createRow(0);
            String[] orgCols = {"Society / Group Name", "Society Code", "Domain Name", "Curriculum Board", "Contact Email", "Contact Phone"};
            for (int i = 0; i < orgCols.length; i++) {
                Cell cell = orgHeader.createCell(i);
                cell.setCellValue(orgCols[i]);
                cell.setCellStyle(headerStyle);
            }
            Row orgData = orgSheet.createRow(1);
            orgData.createCell(0).setCellValue("Delhi Public School Educational Society");
            orgData.createCell(1).setCellValue("DPS-HQ");
            orgData.createCell(2).setCellValue("dps.edu.in");
            orgData.createCell(3).setCellValue("CBSE");
            orgData.createCell(4).setCellValue("admin@dps.edu.in");
            orgData.createCell(5).setCellValue("+91-11-23456789");
            for (int i = 0; i < orgCols.length; i++) orgSheet.autoSizeColumn(i);

            // Sheet 2: Primary Campus
            Sheet branchSheet = workbook.createSheet("2_Primary_Campus");
            Row brHeader = branchSheet.createRow(0);
            String[] brCols = {"Branch / Campus Name", "Branch Code", "City", "State", "Address", "Pincode"};
            for (int i = 0; i < brCols.length; i++) {
                Cell cell = brHeader.createCell(i);
                cell.setCellValue(brCols[i]);
                cell.setCellStyle(headerStyle);
            }
            Row brData = branchSheet.createRow(1);
            brData.createCell(0).setCellValue("DPS North Delhi Campus");
            brData.createCell(1).setCellValue("DPS-DELHI-NORTH");
            brData.createCell(2).setCellValue("New Delhi");
            brData.createCell(3).setCellValue("Delhi");
            brData.createCell(4).setCellValue("Sector 14, Rohini");
            brData.createCell(5).setCellValue("110085");
            for (int i = 0; i < brCols.length; i++) branchSheet.autoSizeColumn(i);

            // Sheet 3: IT Administrator
            Sheet adminSheet = workbook.createSheet("3_IT_Administrator");
            Row admHeader = adminSheet.createRow(0);
            String[] admCols = {"Username", "First Name", "Last Name", "Official Email", "Phone Number", "Temporary Password (Optional)"};
            for (int i = 0; i < admCols.length; i++) {
                Cell cell = admHeader.createCell(i);
                cell.setCellValue(admCols[i]);
                cell.setCellStyle(headerStyle);
            }
            Row admData = adminSheet.createRow(1);
            admData.createCell(0).setCellValue("itadmin");
            admData.createCell(1).setCellValue("Rajesh");
            admData.createCell(2).setCellValue("Sharma");
            admData.createCell(3).setCellValue("rajesh.it@dps.edu.in");
            admData.createCell(4).setCellValue("+91-9876543210");
            admData.createCell(5).setCellValue("Welcome@2026!");
            for (int i = 0; i < admCols.length; i++) adminSheet.autoSizeColumn(i);

            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Parse an uploaded Excel (.xlsx) file and construct a BootstrapRequest.
     */
    public BootstrapRequest parseExcel(InputStream in) throws IOException {
        try (Workbook workbook = new XSSFWorkbook(in)) {
            // Sheet 1: Organization
            Sheet orgSheet = workbook.getSheetAt(0);
            Row orgRow = orgSheet.getRow(1);
            if (orgRow == null) throw new IllegalArgumentException("Sheet 1 (Organization) has no data row.");

            String societyName = getCellString(orgRow.getCell(0));
            String societyCode = getCellString(orgRow.getCell(1));
            String domainName = getCellString(orgRow.getCell(2));
            String curriculum = getCellString(orgRow.getCell(3));
            String contactEmail = getCellString(orgRow.getCell(4));
            String contactPhone = getCellString(orgRow.getCell(5));

            // Sheet 2: Primary Campus
            Sheet brSheet = workbook.getSheetAt(1);
            Row brRow = brSheet != null ? brSheet.getRow(1) : null;
            String branchName = brRow != null ? getCellString(brRow.getCell(0)) : "Main Campus";
            String branchCode = brRow != null ? getCellString(brRow.getCell(1)) : "MAIN";
            String city = brRow != null ? getCellString(brRow.getCell(2)) : "City";
            String state = brRow != null ? getCellString(brRow.getCell(3)) : "State";
            String address = brRow != null ? getCellString(brRow.getCell(4)) : "Campus Address";
            String pincode = brRow != null ? getCellString(brRow.getCell(5)) : "100001";

            // Sheet 3: IT Administrator
            Sheet admSheet = workbook.getSheetAt(2);
            Row admRow = admSheet != null ? admSheet.getRow(1) : null;
            if (admRow == null) throw new IllegalArgumentException("Sheet 3 (IT Administrator) has no data row.");

            String username = getCellString(admRow.getCell(0));
            String firstName = getCellString(admRow.getCell(1));
            String lastName = getCellString(admRow.getCell(2));
            String adminEmail = getCellString(admRow.getCell(3));
            String adminPhone = getCellString(admRow.getCell(4));
            String tempPassword = getCellString(admRow.getCell(5));

            return BootstrapRequest.builder()
                    .societyName(societyName)
                    .societyCode(societyCode)
                    .domainName(domainName)
                    .curriculumBoard(curriculum)
                    .contactEmail(contactEmail)
                    .contactPhone(contactPhone)
                    .branchName(branchName)
                    .branchCode(branchCode)
                    .city(city)
                    .state(state)
                    .address(address)
                    .pincode(pincode)
                    .adminUsername(username)
                    .adminFirstName(firstName)
                    .adminLastName(lastName)
                    .adminEmail(adminEmail)
                    .adminPhone(adminPhone)
                    .temporaryPassword(tempPassword)
                    .build();
        }
    }

    /**
     * Execute atomic bootstrap provisioning in database.
     */
    @Transactional
    public BootstrapResponse bootstrap(BootstrapRequest req) {
        log.info("Initiating system bootstrap for society: {} with admin: {}", req.getSocietyCode(), req.getAdminUsername());

        // 1. Guard check: only allow bootstrap if no tenants exist
        if (tenantRepository.count() > 0) {
            log.warn("Bootstrap rejected: system already initialized with {} tenant(s)", tenantRepository.count());
            throw new ConflictException("System is already initialized. Bootstrap is permanently locked.");
        }

        // 2. Resolve temporary password
        String tempPassword = (req.getTemporaryPassword() != null && !req.getTemporaryPassword().isBlank())
                ? req.getTemporaryPassword().trim()
                : generateSecurePassword();

        // 3. Create Root Tenant
        String tenantId = UUID.randomUUID().toString();
        Tenant tenant = new Tenant();
        tenant.setTenantId(tenantId);
        tenant.setSocietyCode(req.getSocietyCode().trim().toUpperCase());
        tenant.setSocietyName(req.getSocietyName().trim());
        tenant.setDomainName(req.getDomainName() != null ? req.getDomainName().trim() : null);
        tenant.setContactEmail(req.getContactEmail().trim().toLowerCase());
        tenant.setContactPhone(req.getContactPhone().trim());
        tenant.setStatus("ACTIVE");
        tenantRepository.save(tenant);

        // 4. Create Primary Campus Branch
        String branchCode = req.getBranchCode().trim().toUpperCase();
        BranchProvisioning branch = new BranchProvisioning();
        branch.setTenantId(tenantId);
        branch.setBranchCode(branchCode);
        branch.setBranchName(req.getBranchName().trim());
        branch.setAffiliationBoard(req.getCurriculumBoard() != null ? req.getCurriculumBoard() : "CBSE");
        branch.setSchemaName("dpsdata");
        branch.setStudentCapacity(1000);
        branch.setStaffCapacity(100);
        branchProvisioningRepository.save(branch);

        // 5. Create IT Administrator User Account
        String userId = UUID.randomUUID().toString();
        User adminUser = new User();
        adminUser.setUserId(userId);
        adminUser.setTenantId(tenantId);
        adminUser.setBranchId(branchCode);
        adminUser.setUsername(req.getAdminUsername().trim().toLowerCase());
        adminUser.setEmail(req.getAdminEmail().trim().toLowerCase());
        adminUser.setPhoneNumber(req.getAdminPhone() != null ? req.getAdminPhone().trim() : null);
        adminUser.setUserType("SUPER_ADMIN");
        adminUser.setFirstName(req.getAdminFirstName().trim());
        adminUser.setLastName(req.getAdminLastName().trim());
        adminUser.setActive(true);
        adminUser.setMfaEnabled(false);
        userRepository.save(adminUser);

        // 6. Create Credentials with MUST_CHANGE_PASSWORD = TRUE
        UserCredential credential = new UserCredential();
        credential.setUserId(userId);
        credential.setPasswordHash(passwordEncoder.encode(tempPassword));
        credential.setPasswordSalt(UUID.randomUUID().toString());
        credential.setPasswordChangedAt(Instant.now());
        credential.setFailedLoginAttempts(0);
        credential.setLockedUntil(null);
        credential.setMustChangePassword(true); // <-- Forces password change on first sign-in
        userCredentialRepository.save(credential);

        // 7. Assign Roles: Super Admin + IT Admin
        UserRole superAdminRole = new UserRole();
        superAdminRole.setUserId(userId);
        superAdminRole.setRoleCode("ROLE_SUPER_ADMIN");
        userRoleRepository.save(superAdminRole);

        UserRole itAdminRole = new UserRole();
        itAdminRole.setUserId(userId);
        itAdminRole.setRoleCode("ROLE_IT_ADMIN");
        userRoleRepository.save(itAdminRole);

        log.info("System bootstrap completed successfully: tenantId={}, userId={}, username={}",
                tenantId, userId, adminUser.getUsername());

        return BootstrapResponse.builder()
                .tenantId(tenantId)
                .societyCode(tenant.getSocietyCode())
                .societyName(tenant.getSocietyName())
                .branchCode(branchCode)
                .branchName(branch.getBranchName())
                .userId(userId)
                .username(adminUser.getUsername())
                .email(adminUser.getEmail())
                .temporaryPassword(tempPassword)
                .mustChangePassword(true)
                .message("Initial system setup completed. Please sign in using the IT Admin temporary credentials to set your permanent password.")
                .build();
    }

    /**
     * Bootstrap from uploaded multipart file.
     */
    public BootstrapResponse bootstrapFromUpload(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Uploaded file cannot be empty");
        }
        BootstrapRequest req = parseExcel(file.getInputStream());
        return bootstrap(req);
    }

    private String getCellString(Cell cell) {
        if (cell == null) return "";
        DataFormatter formatter = new DataFormatter();
        return formatter.formatCellValue(cell).trim();
    }

    private String generateSecurePassword() {
        String chars = "ABCDEFGHJKLMNPQRSTUVWXYZabcdefghijkmnpqrstuvwxyz23456789@#$!";
        SecureRandom random = new SecureRandom();
        StringBuilder sb = new StringBuilder("Init@2026");
        for (int i = 0; i < 4; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
}
