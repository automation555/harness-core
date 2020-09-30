package software.wings.beans;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AzureImageGallery {
  private String name;
  private String subscriptionId;
  private String resourceGroupName;
  private String regionName;
}
