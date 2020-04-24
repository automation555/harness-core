package io.harness.ccm.config;

import com.google.inject.Inject;

import io.harness.ccm.config.GcpOrganization.GcpOrganizationKeys;
import io.harness.persistence.HPersistence;
import org.mongodb.morphia.query.Query;

import java.util.List;

public class GcpOrganizationDao {
  @Inject private HPersistence persistence;

  public String save(GcpOrganization organization) {
    return persistence.save(organization);
  }

  public GcpOrganization get(String uuid) {
    Query<GcpOrganization> query =
        persistence.createQuery(GcpOrganization.class).field(GcpOrganizationKeys.uuid).equal(uuid);
    return query.get();
  }

  public List<GcpOrganization> list(String accountId) {
    Query<GcpOrganization> query =
        persistence.createQuery(GcpOrganization.class).field(GcpOrganizationKeys.accountId).equal(accountId);
    return query.asList();
  }
}
