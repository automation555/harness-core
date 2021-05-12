package io.harness.signup.notification;

import static io.harness.rule.OwnerRule.ZHUO;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.ng.core.user.UserInfo;
import io.harness.notification.channeldetails.EmailChannel;
import io.harness.notification.notificationclient.NotificationClient;
import io.harness.rest.RestResponse;
import io.harness.rule.Owner;
import io.harness.signup.SignupNotificationConfiguration;
import io.harness.user.remote.UserClient;

import com.google.common.collect.ImmutableMap;
import java.io.IOException;
import java.util.Map;
import java.util.Optional;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import retrofit2.Call;
import retrofit2.Response;

public class SignupNotificationHelperTest extends CategoryTest {
  @InjectMocks SignupNotificationHelper signupNotificationHelper;
  @Mock private UserClient userClient;
  @Mock private NotificationClient notificationClient;
  @Mock private SignupNotificationTemplateLoader catchLoader;
  @Mock private SignupNotificationConfiguration notificationConfiguration;

  private ArgumentCaptor<EmailChannel> emailChannelCaptor;
  private UserInfo userInfo;
  private static final String DEFAULT_TEMPLATE_ID = "default";
  private static final String EMAIL = "1@1";
  private static final String ID = "id";
  private static final String ACCOUNT_ID = "account";
  private static final String NAME = "1";
  private static final String URL = "/test";
  private static final String VERIFY_TEMPLATE_ID = "verify_email";
  private static final String CONFIRM_TEMPLATE_ID = "signup_confirmation";
  private static final String GCS_FILE_NAME = "gname";

  @Before
  public void setup() throws IOException {
    initMocks(this);
    userInfo = UserInfo.builder()
                   .uuid(ID)
                   .defaultAccountId(ACCOUNT_ID)
                   .email(DEFAULT_TEMPLATE_ID)
                   .name(NAME)
                   .email(EMAIL)
                   .build();

    Call<RestResponse<Optional<String>>> request = mock(Call.class);
    when(userClient.generateSignupNotificationUrl(any(), eq(userInfo))).thenReturn(request);
    when(request.execute()).thenReturn(Response.success(new RestResponse<>(Optional.of(URL))));
    EmailInfo veriyEmailInfo = EmailInfo.builder().templateId(VERIFY_TEMPLATE_ID).gcsFileName(GCS_FILE_NAME).build();
    EmailInfo confirmEmailInfo = EmailInfo.builder().templateId(CONFIRM_TEMPLATE_ID).gcsFileName(GCS_FILE_NAME).build();
    Map<EmailType, EmailInfo> templates = ImmutableMap.<EmailType, EmailInfo>builder()
                                              .put(EmailType.VERIFY, veriyEmailInfo)
                                              .put(EmailType.CONFIRM, confirmEmailInfo)
                                              .build();
    when(notificationConfiguration.getTemplates()).thenReturn(templates);

    emailChannelCaptor = ArgumentCaptor.forClass(EmailChannel.class);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSendVerifyNotification() {
    when(catchLoader.load(any())).thenReturn(true);
    signupNotificationHelper.sendSignupNotification(userInfo, EmailType.VERIFY, DEFAULT_TEMPLATE_ID);

    verify(notificationClient, times(1)).sendNotificationAsync(emailChannelCaptor.capture());
    EmailChannel value = emailChannelCaptor.getValue();
    assertThat(value.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(value.getTemplateData().get("name")).isEqualTo(NAME);
    assertThat(value.getTemplateData().get("url")).isEqualTo(URL);
    assertThat(value.getTemplateId()).isEqualTo(VERIFY_TEMPLATE_ID);
    assertThat(value.getRecipients()).contains(EMAIL);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSendConfirmNotification() {
    when(catchLoader.load(any())).thenReturn(true);

    signupNotificationHelper.sendSignupNotification(userInfo, EmailType.CONFIRM, DEFAULT_TEMPLATE_ID);

    verify(notificationClient, times(1)).sendNotificationAsync(emailChannelCaptor.capture());
    EmailChannel value = emailChannelCaptor.getValue();
    assertThat(value.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(value.getTemplateData().get("name")).isEqualTo(NAME);
    assertThat(value.getTemplateData().get("url")).isEqualTo(URL);
    assertThat(value.getTemplateId()).isEqualTo(CONFIRM_TEMPLATE_ID);
    assertThat(value.getRecipients()).contains(EMAIL);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSendVerifyNotificationFailover() {
    when(catchLoader.load(any())).thenReturn(false);

    signupNotificationHelper.sendSignupNotification(userInfo, EmailType.CONFIRM, DEFAULT_TEMPLATE_ID);

    verify(notificationClient, times(1)).sendNotificationAsync(emailChannelCaptor.capture());
    EmailChannel value = emailChannelCaptor.getValue();
    assertThat(value.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(value.getTemplateData().get("name")).isEqualTo(NAME);
    assertThat(value.getTemplateData().get("url")).isEqualTo(URL);
    assertThat(value.getTemplateId()).isEqualTo(DEFAULT_TEMPLATE_ID);
    assertThat(value.getRecipients()).contains(EMAIL);
  }

  @Test
  @Owner(developers = ZHUO)
  @Category(UnitTests.class)
  public void testSendConfirmNotificationFailover() {
    when(catchLoader.load(any())).thenReturn(false);

    signupNotificationHelper.sendSignupNotification(userInfo, EmailType.CONFIRM, DEFAULT_TEMPLATE_ID);

    verify(notificationClient, times(1)).sendNotificationAsync(emailChannelCaptor.capture());
    EmailChannel value = emailChannelCaptor.getValue();
    assertThat(value.getAccountId()).isEqualTo(ACCOUNT_ID);
    assertThat(value.getTemplateData().get("name")).isEqualTo(NAME);
    assertThat(value.getTemplateData().get("url")).isEqualTo(URL);
    assertThat(value.getTemplateId()).isEqualTo(DEFAULT_TEMPLATE_ID);
    assertThat(value.getRecipients()).contains(EMAIL);
  }
}
