package com.VyntraUserService.UserService.repository;

import com.VyntraUserService.UserService.model.StoreMember;
import com.VyntraUserService.UserService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface StoreMemberRepository extends JpaRepository<StoreMember, Long> {
    List<StoreMember> findByUser(User user);
}
