package software.wings.licensing;

import static io.harness.data.structure.EmptyPredicate.isEmpty;
import static io.harness.data.structure.EmptyPredicate.isNotEmpty;
import static io.harness.validation.Validator.notNullCheck;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;
import com.google.inject.Singleton;

import io.harness.ccm.license.CeLicenseInfo;
import io.harness.ccm.license.CeLicenseType;
import io.harness.event.handler.impl.EventPublishHelper;
import io.harness.exception.InvalidRequestException;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.validator.constraints.NotEmpty;
import org.mongodb.morphia.query.UpdateOperations;
import software.wings.app.MainConfiguration;
import software.wings.beans.Account;
import software.wings.beans.Account.AccountKeys;
import software.wings.beans.AccountStatus;
import software.wings.beans.AccountType;
import software.wings.beans.DefaultSalesContacts;
import software.wings.beans.DefaultSalesContacts.AccountTypeDefault;
import software.wings.beans.License;
import software.wings.beans.LicenseInfo;
import software.wings.beans.User;
import software.wings.beans.security.UserGroup;
import software.wings.dl.GenericDbCache;
import software.wings.dl.WingsPersistence;
import software.wings.helpers.ext.mail.EmailData;
import software.wings.service.impl.AccountDao;
import software.wings.service.impl.LicenseUtils;
import software.wings.service.intfc.AccountService;
import software.wings.service.intfc.EmailNotificationService;
import software.wings.service.intfc.UserGroupService;
import software.wings.service.intfc.UserService;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;

/**
 *
 * @author rktummala on 11/10/18
 */
@Singleton
@Slf4j
public class LicenseServiceImpl implements LicenseService {
  private static final String EMAIL_SUBJECT_ACCOUNT_EXPIRED = "Harness License Expired!";
  private static final String EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE = "Harness License about to Expire!";

  private static final String EMAIL_BODY_ACCOUNT_EXPIRED = "Customer License has Expired";
  private static final String EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE = "Customer License is about to Expire";

  private static final String TRIAL_EXPIRATION_DAY_0_TEMPLATE = "trial_expiration_day0";
  private static final String TRIAL_EXPIRATION_DAY_4_TEMPLATE = "trial_expiration_day4";
  private static final String TRIAL_EXPIRATION_DAY_23_TEMPLATE = "trial_expiration_day23";
  private static final String TRIAL_EXPIRATION_DAY_29_TEMPLATE = "trial_expiration_day29";
  private static final String TRIAL_EXPIRATION_DAY_30_TEMPLATE = "trial_expiration_day30";

  private final AccountService accountService;
  private final WingsPersistence wingsPersistence;
  private final GenericDbCache dbCache;
  private final ExecutorService executorService;
  private final LicenseProvider licenseProvider;
  private final EmailNotificationService emailNotificationService;
  private final EventPublishHelper eventPublishHelper;
  private final UserService userService;
  private final UserGroupService userGroupService;
  private final AccountDao accountDao;
  private List<String> trialDefaultContacts;
  private List<String> paidDefaultContacts;

  @Inject
  public LicenseServiceImpl(AccountService accountService, AccountDao accountDao, WingsPersistence wingsPersistence,
      GenericDbCache dbCache, ExecutorService executorService, LicenseProvider licenseProvider,
      EmailNotificationService emailNotificationService, EventPublishHelper eventPublishHelper,
      MainConfiguration mainConfiguration, UserService userService, UserGroupService userGroupService) {
    this.accountService = accountService;
    this.accountDao = accountDao;
    this.wingsPersistence = wingsPersistence;
    this.dbCache = dbCache;
    this.executorService = executorService;
    this.licenseProvider = licenseProvider;
    this.emailNotificationService = emailNotificationService;
    this.eventPublishHelper = eventPublishHelper;
    this.userService = userService;
    this.userGroupService = userGroupService;

    DefaultSalesContacts defaultSalesContacts = mainConfiguration.getDefaultSalesContacts();
    if (defaultSalesContacts != null && defaultSalesContacts.isEnabled()) {
      List<AccountTypeDefault> accountTypeDefaults = defaultSalesContacts.getAccountTypeDefaults();

      if (isNotEmpty(accountTypeDefaults)) {
        for (AccountTypeDefault accountTypeDefault : accountTypeDefaults) {
          switch (accountTypeDefault.getAccountType()) {
            case AccountType.ESSENTIALS:
            case AccountType.PAID:
              paidDefaultContacts = getEmailIds(accountTypeDefault.getEmailIds());
              break;
            case AccountType.TRIAL:
              trialDefaultContacts = getEmailIds(accountTypeDefault.getEmailIds());
              break;
            default:
              break;
          }
        }
      }
    }
  }

  @Override
  public void checkForLicenseExpiry(Account account) {
    try {
      account = accountService.get(account.getUuid());
      LicenseInfo licenseInfo = account.getLicenseInfo();

      if (licenseInfo == null) {
        return;
      }

      String accountStatus = licenseInfo.getAccountStatus();
      String accountType = licenseInfo.getAccountType();
      if (isEmpty(accountType)) {
        return;
      }

      if (accountType.equals(AccountType.COMMUNITY)) {
        return;
      }

      long expiryTime = licenseInfo.getExpiryTime();
      long currentTime = System.currentTimeMillis();
      if (currentTime < expiryTime) {
        if (accountType.equals(AccountType.PAID) || accountType.equals(AccountType.ESSENTIALS)) {
          if (!account.isEmailSentToSales() && ((expiryTime - currentTime) <= Duration.ofDays(30).toMillis())) {
            sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, paidDefaultContacts);
          }
        } else if (accountType.equals(AccountType.TRIAL)) {
          boolean lessThan7DaysLeftForLicenseExpiry = (expiryTime - currentTime) <= Duration.ofDays(7).toMillis();
          if (!account.isEmailSentToSales() && lessThan7DaysLeftForLicenseExpiry) {
            sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_ABOUT_TO_EXPIRE,
                EMAIL_BODY_ACCOUNT_ABOUT_TO_EXPIRE, trialDefaultContacts);
          }
        }
      } else {
        if (AccountStatus.ACTIVE.equals(accountStatus)) {
          expireLicense(account.getUuid(), licenseInfo);
          sendEmailToSales(account, expiryTime, accountType, EMAIL_SUBJECT_ACCOUNT_EXPIRED, EMAIL_BODY_ACCOUNT_EXPIRED,
              accountType.equals(AccountType.PAID) ? paidDefaultContacts : trialDefaultContacts);
        }
      }
    } catch (Exception e) {
      logger.warn("Failed to check license info", e);
    }
  }

  @VisibleForTesting
  String getEmailTemplateName(Account account, long currentTime, long expiryTime) {
    long lastLicenseExpiryReminderSentAt = account.getLastLicenseExpiryReminderSentAt();
    long expiredSinceDays = TimeUnit.MILLISECONDS.toDays(currentTime - expiryTime);
    long lastReminderSentSinceDays = TimeUnit.MILLISECONDS.toDays(currentTime - lastLicenseExpiryReminderSentAt);
    String templateName = null;

    if (lastReminderSentSinceDays > 0) {
      if (expiredSinceDays <= 1) {
        templateName = TRIAL_EXPIRATION_DAY_0_TEMPLATE;
      } else if (expiredSinceDays == 4) {
        templateName = TRIAL_EXPIRATION_DAY_4_TEMPLATE;
      } else if (expiredSinceDays == 23) {
        templateName = TRIAL_EXPIRATION_DAY_23_TEMPLATE;
      } else if (expiredSinceDays == 29) {
        templateName = TRIAL_EXPIRATION_DAY_29_TEMPLATE;
      } else if (expiredSinceDays == 30) {
        templateName = TRIAL_EXPIRATION_DAY_30_TEMPLATE;
      }
    }
    return templateName;
  }

  @VisibleForTesting
  void updateLastLicenseExpiryReminderSentAt(String accountId, long time) {
    wingsPersistence.updateField(Account.class, accountId, AccountKeys.lastLicenseExpiryReminderSentAt, time);
    dbCache.invalidate(Account.class, accountId);
  }

  private List<String> getEmailIds(String emailIdsStr) {
    if (isEmpty(emailIdsStr)) {
      return null;
    }

    String[] emailIdArr = emailIdsStr.split(",");

    if (isEmpty(emailIdArr)) {
      return null;
    }

    List<String> emailIds = new ArrayList<>();

    for (String emailId : emailIdArr) {
      emailIds.add(emailId.trim());
    }

    return emailIds;
  }

  private void sendEmailToSales(
      Account account, long expiryTime, String accountType, String subject, String body, List<String> defaultContacts) {
    if (isEmpty(account.getSalesContacts()) && isEmpty(defaultContacts)) {
      logger.info("Skipping the sending of email since no sales contacts were configured");
      return;
    }

    List<String> mailingList = isEmpty(account.getSalesContacts()) ? defaultContacts : account.getSalesContacts();

    Date expiryDate = new Date(expiryTime);
    Map<String, String> templateModel = new HashMap<>();
    templateModel.put("emailSubject", subject);
    templateModel.put("emailBody", body);
    templateModel.put("accountName", account.getAccountName());
    templateModel.put("companyName", account.getCompanyName());
    templateModel.put("accountType", accountType);
    templateModel.put("expiry", expiryDate.toString());

    EmailData emailData = EmailData.builder()
                              .system(true)
                              .to(mailingList)
                              .templateName("send_email_to_sales")
                              .templateModel(templateModel)
                              .accountId(account.getUuid())
                              .build();
    emailData.setCc(Collections.emptyList());
    emailData.setRetries(3);
    boolean sent = emailNotificationService.send(emailData);
    if (sent) {
      updateEmailSentToSales(account.getUuid(), true);
    } else {
      updateEmailSentToSales(account.getUuid(), false);
      logger.warn("Couldn't send email to sales for account {}", account.getUuid());
    }
  }

  /**
   * Send email to the members of account's admin user group. If email is sent successfully to any one member of the
   * group, then return true.
   * @param accountId
   * @param templateName
   * @return
   */
  @VisibleForTesting
  boolean sendEmailToAccountAdmin(String accountId, String templateName) {
    UserGroup adminUserGroup = userGroupService.getAdminUserGroup(accountId);
    boolean emailSent = false;

    for (String memberId : adminUserGroup.getMemberIds()) {
      User user = userService.get(memberId);
      String name = !user.getName().isEmpty() ? user.getName() : "there";
      Map<String, String> templateModel = new HashMap<>();
      templateModel.put("name", name);
      EmailData emailData = EmailData.builder()
                                .to(Arrays.asList(user.getEmail()))
                                .templateName(templateName)
                                .templateModel(templateModel)
                                .accountId(accountId)
                                .build();
      emailData.setCc(Collections.emptyList());
      emailData.setRetries(3);
      emailSent = emailSent || emailNotificationService.send(emailData);
    }
    logger.info("Trial account expiration email with template name {} sent successfully {}", templateName, emailSent);
    return emailSent;
  }

  @Override
  public boolean updateAccountLicense(@NotEmpty String accountId, LicenseInfo licenseInfo) {
    Account accountInDB = accountService.get(accountId);
    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    LicenseInfo oldLicenseInfo = accountInDB.getLicenseInfo();
    String oldAccountType = null;
    if (oldLicenseInfo != null) {
      oldAccountType = oldLicenseInfo.getAccountType();
    }

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    byte[] encryptedLicenseInfo = LicenseUtils.getEncryptedLicenseInfoForUpdate(oldLicenseInfo, licenseInfo);

    updateOperations.set("encryptedLicenseInfo", encryptedLicenseInfo);
    updateOperations.set(AccountKeys.licenseInfo, licenseInfo);

    wingsPersistence.update(accountInDB, updateOperations);
    updateEmailSentToSales(accountId, false);
    dbCache.invalidate(Account.class, accountId);
    Account updatedAccount = wingsPersistence.get(Account.class, accountId);
    LicenseUtils.decryptLicenseInfo(updatedAccount, false);

    eventPublishHelper.publishLicenseChangeEvent(accountId, oldAccountType, licenseInfo.getAccountType());
    return true;
  }

  @Override
  public boolean startCeLimitedTrial(@NotEmpty String accountId) {
    Account account = accountDao.get(accountId);
    Preconditions.checkNotNull(account);

    CeLicenseInfo currCeLicenseInfo = account.getCeLicenseInfo();
    if (currCeLicenseInfo != null) {
      throw new InvalidRequestException("CE Limited Trial license has already started");
    }

    CeLicenseInfo ceLicenseInfo = CeLicenseInfo.builder()
                                      .licenseType(CeLicenseType.LIMITED_TRIAL)
                                      .expiryTime(CeLicenseType.LIMITED_TRIAL.getDefaultExpiryTime())
                                      .build();
    updateCeLicense(accountId, ceLicenseInfo);
    return true;
  }

  @Override
  public boolean updateCeLicense(@NotEmpty String accountId, CeLicenseInfo ceLicenseInfo) {
    accountDao.updateCeLicense(accountId, ceLicenseInfo);
    if (Instant.now().toEpochMilli() < ceLicenseInfo.getExpiryTime()) {
      accountService.updateCloudCostEnabled(accountId, true);
    } else {
      accountService.updateCloudCostEnabled(accountId, false);
    }
    return true;
  }

  @Override
  public Account updateAccountSalesContacts(@NotEmpty String accountId, List<String> salesContacts) {
    Account accountInDB = accountService.get(accountId);

    notNullCheck("Invalid Account for the given Id: " + accountId, accountInDB);

    UpdateOperations<Account> updateOperations = wingsPersistence.createUpdateOperations(Account.class);

    if (isNotEmpty(salesContacts)) {
      updateOperations.set("salesContacts", salesContacts);
    } else {
      updateOperations.unset("salesContacts");
    }

    wingsPersistence.update(accountInDB, updateOperations);
    updateEmailSentToSales(accountId, false);
    dbCache.invalidate(Account.class, accountId);
    Account updatedAccount = wingsPersistence.get(Account.class, accountId);
    LicenseUtils.decryptLicenseInfo(updatedAccount, false);
    return updatedAccount;
  }

  @Override
  public void updateAccountLicenseForOnPrem(String encryptedLicenseInfoBase64String) {
    try {
      if (isEmpty(encryptedLicenseInfoBase64String)) {
        String msg = "Couldn't find license info";
        throw new InvalidRequestException(msg);
      }

      List<Account> accountList = accountService.listAllAccounts();
      if (accountList == null) {
        String msg = "Couldn't find any accounts in DB";
        throw new InvalidRequestException(msg);
      }

      accountList.forEach(account -> {
        if (!account.getAccountName().equalsIgnoreCase("Global")) {
          byte[] encryptedLicenseInfo = Base64.getDecoder().decode(encryptedLicenseInfoBase64String.getBytes());
          byte[] encryptedLicenseInfoFromDB = account.getEncryptedLicenseInfo();

          boolean noLicenseInfoInDB = isEmpty(encryptedLicenseInfoFromDB);

          if (noLicenseInfoInDB || !Arrays.equals(encryptedLicenseInfo, encryptedLicenseInfoFromDB)) {
            account.setEncryptedLicenseInfo(encryptedLicenseInfo);
            LicenseUtils.decryptLicenseInfo(account, true);
            LicenseInfo licenseInfo = account.getLicenseInfo();
            if (licenseInfo != null) {
              updateAccountLicense(account.getUuid(), licenseInfo);
            } else {
              throw new InvalidRequestException("No license info could be extracted from the encrypted license info");
            }
          }
        }
      });
    } catch (Exception ex) {
      throw new InvalidRequestException("Error while updating account license for on-prem", ex);
    }
  }

  @Override
  public boolean isAccountDeleted(String accountId) {
    // We get the account status from local cache even though its eventually consistent,
    // Since this is called by delegate service frequently, we are referring to cache.
    return AccountStatus.DELETED.equals(accountService.getAccountStatus(accountId));
  }

  @Override
  public boolean isAccountExpired(String accountId) {
    // TODO when we have distributed cache, account should be cached and referred.
    Account account = dbCache.get(Account.class, accountId);
    notNullCheck("Invalid account with id: " + accountId, account);

    LicenseInfo licenseInfo = account.getLicenseInfo();

    if (licenseInfo == null) {
      return false;
    }

    String accountType = licenseInfo.getAccountType();
    String accountStatus = licenseInfo.getAccountStatus();

    if (isEmpty(accountType)) {
      throw new InvalidRequestException("Account type is null for account :" + accountId);
    }

    if (isEmpty(accountStatus)) {
      throw new InvalidRequestException("Account status is null for account :" + accountId);
    }

    if (AccountType.COMMUNITY.equals(accountType)) {
      return false;
    }

    if (AccountStatus.EXPIRED.equals(accountStatus) || AccountStatus.DELETED.equals(accountStatus)
        || AccountStatus.MARKED_FOR_DELETION.equals(accountStatus)) {
      return true;
    }

    if (System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      executorService.submit(() -> expireLicense(accountId, licenseInfo));
      return true;
    }

    return false;
  }

  private void expireLicense(String accountId, LicenseInfo licenseInfo) {
    licenseInfo.setAccountStatus(AccountStatus.EXPIRED);
    updateAccountLicense(accountId, licenseInfo);
  }

  private void updateEmailSentToSales(String accountId, boolean status) {
    wingsPersistence.updateField(Account.class, accountId, "emailSentToSales", status);
  }

  @Override
  public void validateLicense(String accountId, String operation) {
    Account account = accountService.get(accountId);
    LicenseInfo licenseInfo = account.getLicenseInfo();
    if (licenseInfo == null) {
      throw new InvalidRequestException("license Info not present");
    }
    if (licenseInfo.getExpiryTime() > 0 && System.currentTimeMillis() > licenseInfo.getExpiryTime()) {
      licenseProvider.get(account.getLicenseId());
    } else {
      throw new InvalidRequestException("Invalid expiry time");
    }
  }

  @Override
  public void setLicense(Account account) {
    List<License> licenseList = licenseProvider.getActiveLicenses();
    account.setLicenseId(licenseList.get(0).getUuid());
    if (account.getLicenseInfo() == null) {
      account.setLicenseInfo(new LicenseInfo());
    }

    account.getLicenseInfo().setExpiryTime(System.currentTimeMillis() + licenseList.get(0).getExpiryDuration());
  }
}
