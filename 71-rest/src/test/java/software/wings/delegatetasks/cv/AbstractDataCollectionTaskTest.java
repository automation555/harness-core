package software.wings.delegatetasks.cv;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.inject.Injector;

import io.harness.CategoryTest;
import io.harness.category.element.UnitTests;
import io.harness.delegate.beans.ResponseData;
import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mockito.Mock;
import org.mockito.Mockito;
import software.wings.service.impl.analysis.DataCollectionInfoV2;
import software.wings.service.impl.analysis.DataCollectionTaskResult;
import software.wings.service.impl.analysis.DataCollectionTaskResult.DataCollectionTaskStatus;
import software.wings.sm.StateType;

import java.lang.reflect.InvocationTargetException;
import java.time.Duration;

public class AbstractDataCollectionTaskTest extends CategoryTest {
  @Mock private DataCollectorFactory dataCollectorFactory;
  @Mock private Injector injector;
  private AbstractDataCollectionTask<DataCollectionInfoV2> abstractDataCollectionTask;

  @Before
  public void setupTests() throws IllegalAccessException {
    initMocks(this);
    abstractDataCollectionTask = mock(AbstractDataCollectionTask.class, Mockito.CALLS_REAL_METHODS);
    FieldUtils.writeField(abstractDataCollectionTask, "dataCollectorFactory", dataCollectorFactory, true);
    FieldUtils.writeField(abstractDataCollectionTask, "injector", injector, true);
    AbstractDataCollectionTask.RETRY_SLEEP_DURATION = Duration.ofMillis(1); // to run retry based test faster.
  }
  @Test
  @Category(UnitTests.class)
  public void testCallToInitAndCollectAndSaveDataWithCorrectParams()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    abstractDataCollectionTask.run(dataCollectionInfo);
    verify(dataCollector, times(1)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(1)).collectAndSaveData(dataCollectionInfo);
  }

  @Test
  @Category(UnitTests.class)
  public void testCorrectTaskResultIfNoFailure()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertEquals(DataCollectionTaskStatus.SUCCESS, taskResult.getStatus());
    assertEquals(StateType.SPLUNKV2, taskResult.getStateType());
    assertNull(taskResult.getErrorMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testCorrectTaskResultIfLessThenRetryCountFailures()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).doNothing().when(dataCollector).init(any(), any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    verify(dataCollector, times(2)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(1)).collectAndSaveData(dataCollectionInfo);
    assertEquals(DataCollectionTaskStatus.SUCCESS, taskResult.getStatus());
    assertEquals(StateType.SPLUNKV2, taskResult.getStateType());
    assertEquals("error message from test", taskResult.getErrorMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testStatusFailureInCaseOfExceptionOnInitWithRetryCount()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).when(dataCollector).init(any(), any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    verify(dataCollector, times(4)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(0)).collectAndSaveData(dataCollectionInfo);
    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertEquals(DataCollectionTaskStatus.FAILURE, taskResult.getStatus());
    assertEquals(StateType.SPLUNKV2, taskResult.getStateType());
    assertEquals("error message from test", taskResult.getErrorMessage());
  }

  @Test
  @Category(UnitTests.class)
  public void testStatusFailureInCaseOfExceptionOnCollectAndSaveWithRetryCount()
      throws InvocationTargetException, NoSuchMethodException, InstantiationException, IllegalAccessException,
             DataCollectionException {
    DataCollectionInfoV2 dataCollectionInfo = createDataCollectionInfo();
    DataCollector<DataCollectionInfoV2> dataCollector = mock(DataCollector.class);
    doReturn(dataCollector).when(dataCollectorFactory).newInstance(any());
    doThrow(new RuntimeException("error message from test")).when(abstractDataCollectionTask).collectAndSaveData(any());
    ResponseData responseData = abstractDataCollectionTask.run(dataCollectionInfo);

    verify(dataCollector, times(4)).init(any(), eq(dataCollectionInfo));
    verify(abstractDataCollectionTask, times(4)).collectAndSaveData(dataCollectionInfo);
    DataCollectionTaskResult taskResult = (DataCollectionTaskResult) responseData;
    assertEquals(DataCollectionTaskStatus.FAILURE, taskResult.getStatus());
    assertEquals(StateType.SPLUNKV2, taskResult.getStateType());
    assertEquals("error message from test", taskResult.getErrorMessage());
  }

  public DataCollectionInfoV2 createDataCollectionInfo() {
    StateType stateType = StateType.SPLUNKV2;
    DataCollectionInfoV2 dataCollectionInfoV2 = mock(DataCollectionInfoV2.class);
    when(dataCollectionInfoV2.getStateType()).thenReturn(stateType);
    return dataCollectionInfoV2;
  }

  // TODO: write test for saving and create third party call logs.
}
