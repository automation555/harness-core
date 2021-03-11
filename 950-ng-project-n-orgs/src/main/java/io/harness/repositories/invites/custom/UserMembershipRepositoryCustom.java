package io.harness.repositories.invites.custom;

import io.harness.ng.core.invites.entities.UserMembership;

import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.query.Criteria;

public interface UserMembershipRepositoryCustom {
  List<UserMembership> findAll(Criteria criteria);

  Page<UserMembership> findAll(Criteria criteria, Pageable pageable);
}
