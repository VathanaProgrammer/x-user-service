package com.VyntraUserService.UserService.repository;

import com.VyntraUserService.UserService.model.Device;
import com.VyntraUserService.UserService.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface DeviceRepository extends JpaRepository<Device, Long> {
    List<Device> findByUser(User user);
}
