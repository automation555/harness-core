package io.harness.utils;

import static io.harness.annotations.dev.HarnessTeam.PL;

import io.harness.annotations.dev.OwnedBy;
import io.harness.exception.InvalidRequestException;
import io.harness.exception.UnexpectedException;
import io.harness.ng.core.dto.ResponseDTO;

import com.google.inject.Singleton;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import retrofit2.Call;
import retrofit2.Response;

@Singleton
@Slf4j
@OwnedBy(PL)
public class RestCallToNGManagerClientUtils {
  public static <T> T execute(Call<ResponseDTO<T>> request) {
    try {
      Response<ResponseDTO<T>> response = request.execute();
      if (response.isSuccessful()) {
        log.info("response raw {}, body {}", response.raw(), response.body().getData());
        return response.body().getData();
      } else {
        log.error("response {}, request {}, request body {}", response.body(), request.request(),
            request.request().body().toString());
        // todo @Nikhil Add the error message here, currently the error message is not stored in responsedto ?
        throw new InvalidRequestException("Error occurred while performing this operation");
      }
    } catch (IOException ex) {
      log.error("IO error while connecting to manager", ex);
      throw new UnexpectedException("Unable to connect, please try again.");
    }
  }
}
